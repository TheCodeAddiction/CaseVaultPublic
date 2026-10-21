import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchConversations, sendNewMessage } from '../api/messagesApi'
import { usePolling } from '../hooks/usePolling'
import AppShell from '../components/AppShell'
import RecipientPicker from '../components/RecipientPicker'
import '../components/AuthForm.css'
import './ConversationListPage.css'

export default function ConversationListPage() {
  const { user, token, logout } = useAuth()
  const navigate = useNavigate()
  const [conversations, setConversations] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [isComposing, setIsComposing] = useState(false)
  const [recipientUsername, setRecipientUsername] = useState('')
  const [body, setBody] = useState('')
  const [attachment, setAttachment] = useState(null)
  const [formError, setFormError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function reload() {
    const isInitialLoad = conversations === null
    fetchConversations(token)
      .then((fetched) => {
        setConversations(fetched)
        setLoadError(null)
      })
      .catch((error) => {
        if (isInitialLoad) {
          setLoadError(error)
        }
      })
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  usePolling(reload, 8000)

  async function handleSend(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      const created = await sendNewMessage(token, { recipientUsername, body, attachment })
      navigate(`/messages/${created.conversationId}`)
    } catch (error) {
      setFormError(error.message)
      setIsSubmitting(false)
    }
  }

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="conversation-list">
        <div className="conversation-list__header">
          <h1>Messages</h1>
          <button type="button" className="btn btn-primary" onClick={() => setIsComposing((current) => !current)}>
            New message
          </button>
        </div>

        {isComposing && (
          <form onSubmit={handleSend} className="auth-form card conversation-list__compose">
            {formError && <p className="auth-form__error" role="alert">{formError}</p>}

            <RecipientPicker
              token={token}
              selectedUsername={recipientUsername}
              onSelect={(selected) => setRecipientUsername(selected.username)}
            />

            <label htmlFor="body">Message</label>
            <textarea
              id="body"
              className="conversation-list__textarea"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              maxLength={4000}
              required
            />

            <label htmlFor="attachment">Attachment</label>
            <input
              id="attachment"
              type="file"
              accept=".jpg,.jpeg,.png,.pdf,.txt"
              onChange={(e) => setAttachment(e.target.files[0] ?? null)}
            />

            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>Send</button>
          </form>
        )}

        <div className="card">
          {conversations === null && loadError && (
            <p className="conversation-list__empty" role="alert">
              Couldn't load your conversations. {loadError.message}
            </p>
          )}
          {conversations === null && !loadError && <p className="conversation-list__empty">Loading…</p>}
          {conversations !== null && conversations.length === 0 && (
            <p className="conversation-list__empty">No conversations yet.</p>
          )}
          {conversations !== null && conversations.length > 0 && (
            <ul className="conversation-list__list">
              {conversations.map((c) => (
                <li key={c.id} className="conversation-list__item">
                  <Link to={`/messages/${c.id}`} className="conversation-list__link">
                    <div className="conversation-list__row">
                      <span className="conversation-list__name">{c.otherParticipantFullName}</span>
                      <span className="conversation-list__date">{new Date(c.lastMessageSentAt).toLocaleString()}</span>
                    </div>
                    <div className="conversation-list__row">
                      <p className="conversation-list__preview">{c.lastMessageBody}</p>
                      {c.unreadCount > 0 && <span className="conversation-list__badge">{c.unreadCount}</span>}
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </AppShell>
  )
}
