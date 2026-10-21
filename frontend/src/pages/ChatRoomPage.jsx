import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchConversationMessages, sendToConversation, markConversationRead, downloadMessageAttachment } from '../api/messagesApi'
import { usePolling } from '../hooks/usePolling'
import AppShell from '../components/AppShell'
import '../components/AuthForm.css'
import './ChatRoomPage.css'

function otherParticipantName(messages, username) {
  const first = messages?.[0]
  if (!first) {
    return null
  }
  return first.senderUsername === username ? first.recipientFullName : first.senderFullName
}

export default function ChatRoomPage() {
  const { user, token, logout } = useAuth()
  const { conversationId } = useParams()
  const [messages, setMessages] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [body, setBody] = useState('')
  const [attachment, setAttachment] = useState(null)
  const [formError, setFormError] = useState(null)
  const [downloadError, setDownloadError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function reload() {
    const isInitialLoad = messages === null
    try {
      const fetched = await fetchConversationMessages(token, conversationId)
      setMessages(fetched)
      setLoadError(null)

      const hasUnread = fetched.some((m) => m.recipientUsername === user.username && !m.readAt)
      if (hasUnread) {
        await markConversationRead(token, conversationId)
      }
    } catch (error) {
      if (isInitialLoad) {
        setLoadError(error)
      }
    }
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, conversationId])

  usePolling(reload, 4000)

  async function handleSend(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      const created = await sendToConversation(token, conversationId, { body, attachment })
      setMessages((current) => [...(current ?? []), created])
      setBody('')
      setAttachment(null)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDownloadAttachment(message) {
    setDownloadError(null)
    try {
      const blob = await downloadMessageAttachment(token, message.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = message.attachmentFilename
      link.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setDownloadError(error.message)
    }
  }

  const otherName = otherParticipantName(messages, user.username)

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="chat-room">
        <div className="chat-room__header">
          <Link to="/messages" className="btn btn-ghost-dark">← Messages</Link>
          {otherName && <h1 className="chat-room__title">{otherName}</h1>}
        </div>

        <div className="card chat-room__thread">
          {messages === null && loadError && (
            <div className="chat-room__error">
              <p role="alert">Couldn't load this conversation. {loadError.message}</p>
              <Link to="/messages">← Back to Messages</Link>
            </div>
          )}
          {messages === null && !loadError && <p className="chat-room__empty">Loading…</p>}
          {messages !== null && messages.map((m) => (
            <div key={m.id} className={`chat-room__bubble${m.senderUsername === user.username ? ' chat-room__bubble--mine' : ''}`}>
              <div className="chat-room__bubble-meta">
                <span>{m.senderUsername}</span>
                <span>{new Date(m.sentAt).toLocaleString()}</span>
              </div>
              <p>{m.body}</p>
              {m.relatedCaseId && (
                <p className="chat-room__bubble-case">
                  Regarding case:{' '}
                  <Link to={`/cases/${m.relatedCaseId}`}>{m.relatedCaseNumber} — {m.relatedCaseTitle}</Link>
                </p>
              )}
              {m.attachmentFilename && (
                <button type="button" className="btn btn-ghost-dark" onClick={() => handleDownloadAttachment(m)}>
                  📎 {m.attachmentFilename}
                </button>
              )}
            </div>
          ))}
        </div>

        {messages !== null && (
          <form onSubmit={handleSend} className="auth-form card chat-room__compose">
            {formError && <p className="auth-form__error" role="alert">{formError}</p>}
            {downloadError && <p className="auth-form__error" role="alert">{downloadError}</p>}
            <label htmlFor="body">Message</label>
            <textarea
              id="body"
              className="chat-room__textarea"
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
      </div>
    </AppShell>
  )
}
