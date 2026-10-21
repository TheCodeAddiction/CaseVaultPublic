import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import ConversationListPage from './ConversationListPage'
import { AuthContext } from '../context/AuthContext'
import * as messagesApi from '../api/messagesApi'
import * as usersApi from '../api/usersApi'

vi.mock('../api/messagesApi')

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: { fullName: 'Maria Ibsen' }, token: 'jwt-token', logout: vi.fn() }}>
        <ConversationListPage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ConversationListPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    messagesApi.fetchUnreadCount.mockResolvedValue({ unreadCount: 0 })
    messagesApi.fetchConversations.mockResolvedValue([
      {
        id: 'conv-1',
        otherParticipantUsername: 'kolsen',
        otherParticipantFullName: 'Kari Olsen',
        lastMessageBody: 'Can you cover my shift?',
        lastMessageSentAt: '2026-09-04T09:00:00Z',
        lastMessageSenderUsername: 'kolsen',
        unreadCount: 2,
      },
    ])
  })

  afterEach(() => {
    vi.useRealTimers()
    navigateMock.mockReset()
  })

  it('shows each conversation with a preview and unread indicator', async () => {
    renderPage()

    expect(await screen.findByText('Kari Olsen')).toBeInTheDocument()
    expect(screen.getByText('Can you cover my shift?')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('links each conversation row to its chat room', async () => {
    renderPage()

    const link = await screen.findByRole('link', { name: /Kari Olsen/ })
    expect(link).toHaveAttribute('href', '/messages/conv-1')
  })

  it('polls for new conversations', async () => {
    renderPage()
    await screen.findByText('Kari Olsen')

    messagesApi.fetchConversations.mockClear()
    await vi.advanceTimersByTimeAsync(8000)

    await waitFor(() => expect(messagesApi.fetchConversations).toHaveBeenCalled())
  })

  it('opens the recipient picker and starts a new conversation', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    vi.spyOn(usersApi, 'searchUsers').mockResolvedValue([
      { id: 3, username: 'officer2', fullName: 'Ola Nord', district: 'NORTH', role: 'OFFICER' },
    ])
    messagesApi.sendNewMessage.mockResolvedValue({ id: 'msg-1', conversationId: 'conv-9' })

    renderPage()
    await screen.findByText('Kari Olsen')

    await user.click(screen.getByRole('button', { name: /new message/i }))
    expect(screen.getByLabelText(/recipient/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Message')).toHaveAttribute('maxLength', '4000')

    await user.type(screen.getByLabelText(/recipient/i), 'ola')
    await user.click(await screen.findByText('Ola Nord'))
    await user.type(screen.getByLabelText('Message'), 'Got a minute?')
    await user.click(screen.getByRole('button', { name: /^send$/i }))

    await waitFor(() => expect(messagesApi.sendNewMessage).toHaveBeenCalledWith('jwt-token', expect.objectContaining({
      recipientUsername: 'officer2',
      body: 'Got a minute?',
    })))
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/messages/conv-9'))
  })

  it('shows an inline error on a failed initial load, but stays silent on a failed poll', async () => {
    messagesApi.fetchConversations.mockReset()
    messagesApi.fetchConversations.mockRejectedValueOnce(new Error('Network error'))

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(/couldn't load your conversations/i)

    messagesApi.fetchConversations.mockResolvedValue([
      {
        id: 'conv-1',
        otherParticipantUsername: 'kolsen',
        otherParticipantFullName: 'Kari Olsen',
        lastMessageBody: 'Can you cover my shift?',
        lastMessageSentAt: '2026-09-04T09:00:00Z',
        lastMessageSenderUsername: 'kolsen',
        unreadCount: 2,
      },
    ])

    await vi.advanceTimersByTimeAsync(8000)
    expect(await screen.findByText('Kari Olsen')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    messagesApi.fetchConversations.mockRejectedValueOnce(new Error('transient blip'))
    await vi.advanceTimersByTimeAsync(8000)

    expect(screen.getByText('Kari Olsen')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
