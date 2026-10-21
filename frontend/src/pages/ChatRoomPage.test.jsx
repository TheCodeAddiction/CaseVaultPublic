import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ChatRoomPage from './ChatRoomPage'
import { AuthContext } from '../context/AuthContext'
import * as messagesApi from '../api/messagesApi'

vi.mock('../api/messagesApi')

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/messages/conv-1']}>
      <AuthContext.Provider value={{ user: { fullName: 'Maria Ibsen', username: 'mibsen' }, token: 'jwt-token', logout: vi.fn() }}>
        <Routes>
          <Route path="/messages/:conversationId" element={<ChatRoomPage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

const unreadMessages = [
  { id: 'msg-1', conversationId: 'conv-1', senderUsername: 'kolsen', recipientUsername: 'mibsen', body: 'Hello', sentAt: '2026-09-04T09:00:00Z', readAt: null },
]

describe('ChatRoomPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.clearAllMocks()
    messagesApi.fetchUnreadCount.mockResolvedValue({ unreadCount: 0 })
    messagesApi.fetchConversationMessages.mockResolvedValue(unreadMessages)
    messagesApi.markConversationRead.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the message thread', async () => {
    renderPage()

    expect(await screen.findByText('Hello')).toBeInTheDocument()
  })

  it('limits the reply body to 4000 characters', async () => {
    renderPage()
    await screen.findByText('Hello')

    expect(screen.getByLabelText('Message')).toHaveAttribute('maxLength', '4000')
  })

  it('automatically marks unread messages as read on open, with no button', async () => {
    renderPage()

    await screen.findByText('Hello')
    await waitFor(() => expect(messagesApi.markConversationRead).toHaveBeenCalledWith('jwt-token', 'conv-1'))
    expect(screen.queryByRole('button', { name: /mark as read/i })).not.toBeInTheDocument()
  })

  it('does not call mark-read again when there is nothing unread', async () => {
    messagesApi.fetchConversationMessages.mockResolvedValue([{ ...unreadMessages[0], readAt: '2026-09-04T09:05:00Z' }])

    renderPage()
    await screen.findByText('Hello')

    expect(messagesApi.markConversationRead).not.toHaveBeenCalled()
  })

  it('sends a reply and appends it to the thread', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    messagesApi.sendToConversation.mockResolvedValue({
      id: 'msg-2', conversationId: 'conv-1', senderUsername: 'mibsen', recipientUsername: 'kolsen', body: 'On my way', sentAt: '2026-09-04T09:10:00Z', readAt: null,
    })

    renderPage()
    await screen.findByText('Hello')

    await user.type(screen.getByLabelText('Message'), 'On my way')
    await user.click(screen.getByRole('button', { name: /send/i }))

    expect(await screen.findByText('On my way')).toBeInTheDocument()
  })

  it('polls for new messages', async () => {
    renderPage()
    await screen.findByText('Hello')

    messagesApi.fetchConversationMessages.mockClear()
    await vi.advanceTimersByTimeAsync(4000)

    await waitFor(() => expect(messagesApi.fetchConversationMessages).toHaveBeenCalledWith('jwt-token', 'conv-1'))
  })

  it('sends a reply with an attachment', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    messagesApi.sendToConversation.mockResolvedValue({
      id: 'msg-2', conversationId: 'conv-1', senderUsername: 'mibsen', recipientUsername: 'kolsen', body: 'See attached', sentAt: '2026-09-04T09:10:00Z', readAt: null,
    })

    renderPage()
    await screen.findByText('Hello')

    const file = new File(['fake bytes'], 'notes.txt', { type: 'text/plain' })
    await user.type(screen.getByLabelText('Message'), 'See attached')
    await user.upload(screen.getByLabelText(/attachment/i), file)
    await user.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => expect(messagesApi.sendToConversation).toHaveBeenCalledWith('jwt-token', 'conv-1', {
      body: 'See attached',
      attachment: file,
    }))
    expect(await screen.findByText('See attached')).toBeInTheDocument()
  })

  it('shows an inline error with a link back to the conversation list when the initial load fails', async () => {
    messagesApi.fetchConversationMessages.mockReset()
    messagesApi.fetchConversationMessages.mockRejectedValue(new Error('Only participants may access this conversation'))

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(/couldn't load this conversation/i)
    expect(screen.queryByText('Loading…')).not.toBeInTheDocument()
    const backLink = screen.getByRole('link', { name: /back to messages/i })
    expect(backLink).toHaveAttribute('href', '/messages')
  })

  it('does not surface an error for a transient poll failure after a successful load', async () => {
    renderPage()
    await screen.findByText('Hello')

    messagesApi.fetchConversationMessages.mockRejectedValueOnce(new Error('transient blip'))
    await vi.advanceTimersByTimeAsync(4000)

    expect(screen.getByText('Hello')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
