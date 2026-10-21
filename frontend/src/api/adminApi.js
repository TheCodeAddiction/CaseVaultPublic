import { requestJson, authHeader, ApiError } from './client'

export function fetchMyProfile(token) {
  return requestJson('/api/admin/users/me', {
    headers: authHeader(token),
  })
}

function updateMyAccountRequest(token, fields) {
  const formData = new FormData()
  for (const [key, value] of Object.entries(fields)) {
    if (value !== undefined && value !== null) {
      formData.set(key, value)
    }
  }

  return requestJson('/api/admin/users/me', {
    method: 'PUT',
    headers: authHeader(token),
    body: formData,
  })
}

export function updateMyAccount(token, { fullName, email }) {
  return updateMyAccountRequest(token, { fullName, email })
}

export function changeMyPassword(token, currentPassword, newPassword) {
  return updateMyAccountRequest(token, { currentPassword, newPassword })
}

export function uploadMyProfilePicture(token, file) {
  return updateMyAccountRequest(token, { file })
}

export function fetchUsers(token) {
  return requestJson('/api/admin/users', {
    headers: authHeader(token),
  })
}

export function createUser(token, { fullName, username, email, password, role, district }) {
  return requestJson('/api/admin/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ fullName, username, email, password, role, district }),
  })
}

export function changeUserRole(token, userId, role) {
  return requestJson(`/api/admin/users/${userId}/role`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ role }),
  })
}

export function changeUserDistrict(token, userId, district) {
  return requestJson(`/api/admin/users/${userId}/district`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ district }),
  })
}

export function deactivateUser(token, userId) {
  return requestJson(`/api/admin/users/${userId}/deactivate`, {
    method: 'POST',
    headers: authHeader(token),
  })
}

export function reactivateUser(token, userId) {
  return requestJson(`/api/admin/users/${userId}/reactivate`, {
    method: 'POST',
    headers: authHeader(token),
  })
}

export function deleteUser(token, userId) {
  return requestJson(`/api/admin/users/${userId}`, {
    method: 'DELETE',
    headers: authHeader(token),
  })
}

export function deleteOwnAccount(token, currentPassword) {
  return requestJson('/api/admin/users/me', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', ...authHeader(token) },
    body: JSON.stringify({ currentPassword }),
  })
}

export function fetchUserActivity(token, userId) {
  return requestJson(`/api/admin/users/${userId}/activity`, {
    headers: authHeader(token),
  })
}

export async function fetchProfilePicture(token, userId) {
  const response = await fetch(`/api/admin/users/${userId}/picture`, {
    headers: authHeader(token),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(body.error ?? 'UNKNOWN_ERROR', body.message ?? 'Something went wrong', body.fields)
  }

  return response.blob()
}
