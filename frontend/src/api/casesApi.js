import { requestJson, authHeader, ApiError } from './client'

export function fetchCaseStats(token) {
  return requestJson('/api/cases/stats', {
    headers: authHeader(token),
  })
}

export function fetchCases(token, { q, status, page, size } = {}) {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  if (status) params.set('status', status)
  if (page !== undefined) params.set('page', page)
  if (size !== undefined) params.set('size', size)

  const query = params.toString()
  return requestJson(`/api/cases${query ? `?${query}` : ''}`, {
    headers: authHeader(token),
  })
}

export function createCase(token, { title, description }) {
  return requestJson('/api/cases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ title, description }),
  })
}

export function fetchCase(token, caseId) {
  return requestJson(`/api/cases/${caseId}`, {
    headers: authHeader(token),
  })
}

export function fetchCaseActivity(token, caseId) {
  return requestJson(`/api/cases/${caseId}/activity`, {
    headers: authHeader(token),
  })
}

export function fetchCaseDocuments(token, caseId) {
  return requestJson(`/api/cases/${caseId}/documents`, {
    headers: authHeader(token),
  })
}

export function addDocument(token, caseId, { type, title, content, file }) {
  const formData = new FormData()
  formData.set('type', type)
  if (title) formData.set('title', title)
  if (content) formData.set('content', content)
  if (file) formData.set('file', file)

  return requestJson(`/api/cases/${caseId}/documents`, {
    method: 'POST',
    headers: authHeader(token),
    body: formData,
  })
}

export function deleteDocument(token, caseId, documentId) {
  return requestJson(`/api/cases/${caseId}/documents/${documentId}`, {
    method: 'DELETE',
    headers: authHeader(token),
  })
}

export function recordActivity(token, { entityType, entityId, action, details }) {
  return requestJson('/api/activity-log', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ entityType, entityId, action, details }),
  })
}

export function updateCaseStatus(token, caseId, status) {
  return requestJson(`/api/cases/${caseId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ status }),
  })
}

export function assignCase(token, caseId, officerUsername) {
  return requestJson(`/api/cases/${caseId}/assignee`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ officerUsername }),
  })
}

export function shareCase(token, caseId, { district, username } = {}) {
  return requestJson(`/api/cases/${caseId}/shares`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ district: district ?? null, username: username ?? null }),
  })
}

export function fetchCaseShares(token, caseId) {
  return requestJson(`/api/cases/${caseId}/shares`, {
    headers: authHeader(token),
  })
}

export function revokeCaseShare(token, caseId, shareId) {
  return requestJson(`/api/cases/${caseId}/shares/${shareId}`, {
    method: 'DELETE',
    headers: authHeader(token),
  })
}

export function fetchDistrictOfficers(token) {
  return requestJson('/api/admin/users', {
    headers: authHeader(token),
  })
}

export async function downloadDocumentFile(token, caseId, documentId) {
  const response = await fetch(`/api/cases/${caseId}/documents/${documentId}/file`, {
    headers: authHeader(token),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(body.error ?? 'UNKNOWN_ERROR', body.message ?? 'Something went wrong', body.fields)
  }

  return response.blob()
}

export async function exportCasesCsv(token, { q, status } = {}) {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  if (status) params.set('status', status)

  const query = params.toString()
  const response = await fetch(`/api/cases/export${query ? `?${query}` : ''}`, {
    headers: authHeader(token),
  })
  return response.text()
}
