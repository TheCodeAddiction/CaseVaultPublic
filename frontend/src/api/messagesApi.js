import { requestJson, authHeader, ApiError } from './client'

function buildFormData({ body, relatedCaseId, attachment }) {
  const formData = new FormData()
  formData.set('body', body)
  if (relatedCaseId) {
    formData.set('relatedCaseId', relatedCaseId)
  }
  if (attachment) {
    formData.set('attachment', attachment)
  }
  return formData
}

export function sendNewMessage(token, { recipientUsername, body, relatedCaseId, attachment }) {
  const formData = buildFormData({ body, relatedCaseId, attachment })
  formData.set('recipientUsername', recipientUsername)

  return requestJson('/api/messages', {
    method: 'POST',
    headers: authHeader(token),
    body: formData,
  })
}

export function fetchConversations(token) {
  return requestJson('/api/conversations', {
    headers: authHeader(token),
  })
}

export function fetchConversationMessages(token, conversationId) {
  return requestJson(`/api/conversations/${conversationId}/messages`, {
    headers: authHeader(token),
  })
}

export function sendToConversation(token, conversationId, { body, relatedCaseId, attachment }) {
  const formData = buildFormData({ body, relatedCaseId, attachment })

  return requestJson(`/api/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: authHeader(token),
    body: formData,
  })
}

export function markConversationRead(token, conversationId) {
  return requestJson(`/api/conversations/${conversationId}/read`, {
    method: 'POST',
    headers: authHeader(token),
  })
}

export function fetchUnreadCount(token) {
  return requestJson('/api/messages/unread-count', {
    headers: authHeader(token),
  })
}

export async function downloadMessageAttachment(token, messageId) {
  const response = await fetch(`/api/messages/${messageId}/attachment`, {
    headers: authHeader(token),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(body.error ?? 'UNKNOWN_ERROR', body.message ?? 'Something went wrong', body.fields)
  }

  return response.blob()
}
