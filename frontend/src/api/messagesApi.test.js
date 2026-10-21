import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  sendNewMessage,
  fetchConversations,
  fetchConversationMessages,
  sendToConversation,
  markConversationRead,
  fetchUnreadCount,
  downloadMessageAttachment,
} from './messagesApi'
import { ApiError } from './client'

describe('messagesApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('sends a new message', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 'msg-1', conversationId: 'conv-1', senderUsername: 'mibsen', recipientUsername: 'kolsen', body: 'hi' }),
    })

    const result = await sendNewMessage('jwt-token', { recipientUsername: 'kolsen', body: 'hi' })

    expect(fetch).toHaveBeenCalledWith('/api/messages', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body).toBeInstanceOf(FormData)
    expect(callArgs.body.get('recipientUsername')).toBe('kolsen')
    expect(callArgs.body.get('body')).toBe('hi')
    expect(result.conversationId).toBe('conv-1')
  })

  it('sends a new message with a related case and an attachment', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 'msg-2', conversationId: 'conv-2' }) })
    const file = new File(['fake bytes'], 'notes.txt', { type: 'text/plain' })

    await sendNewMessage('jwt-token', { recipientUsername: 'kolsen', body: 'see case', relatedCaseId: 7, attachment: file })

    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body.get('relatedCaseId')).toBe('7')
    expect(callArgs.body.get('attachment')).toBe(file)
  })

  it('fetches conversations', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ id: 'conv-1', unreadCount: 2 }] })

    const result = await fetchConversations('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/conversations', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result).toEqual([{ id: 'conv-1', unreadCount: 2 }])
  })

  it('fetches messages for a conversation', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ id: 'msg-1' }] })

    const result = await fetchConversationMessages('jwt-token', 'conv-1')

    expect(fetch).toHaveBeenCalledWith('/api/conversations/conv-1/messages', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result).toEqual([{ id: 'msg-1' }])
  })

  it('sends a reply into an existing conversation', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 'msg-2', conversationId: 'conv-1' }) })

    const result = await sendToConversation('jwt-token', 'conv-1', { body: 'reply' })

    expect(fetch).toHaveBeenCalledWith('/api/conversations/conv-1/messages', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body.get('body')).toBe('reply')
    expect(result.id).toBe('msg-2')
  })

  it('marks a conversation as read', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => [{ id: 'msg-1', readAt: '2026-09-04T00:00:00Z' }] })

    const result = await markConversationRead('jwt-token', 'conv-1')

    expect(fetch).toHaveBeenCalledWith('/api/conversations/conv-1/read', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].readAt).toBeTruthy()
  })

  it('fetches the unread count', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ unreadCount: 3 }) })

    const result = await fetchUnreadCount('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/messages/unread-count', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.unreadCount).toBe(3)
  })

  it('downloads a message attachment as a blob', async () => {
    const blob = new Blob(['hello'], { type: 'text/plain' })
    fetch.mockResolvedValueOnce({ ok: true, status: 200, blob: async () => blob })

    const result = await downloadMessageAttachment('jwt-token', 'msg-1')

    expect(fetch).toHaveBeenCalledWith('/api/messages/msg-1/attachment', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result).toBe(blob)
  })

  it('throws ApiError when there is no attachment', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 404,
      json: async () => ({ error: 'MESSAGE_NOT_FOUND', message: 'No message found with id: msg-1' }),
    })

    await expect(downloadMessageAttachment('jwt-token', 'msg-1')).rejects.toBeInstanceOf(ApiError)
  })
})
