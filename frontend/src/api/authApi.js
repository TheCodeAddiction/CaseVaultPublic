import { requestJson, authHeader, ApiError } from './client'

export { ApiError }

export function register(payload) {
  return requestJson('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function login(payload) {
  return requestJson('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function fetchCurrentUser(token) {
  return requestJson('/api/me', {
    headers: authHeader(token),
  })
}

export function logout() {
  return requestJson('/api/auth/logout', { method: 'POST' })
}

export function forgotPassword(username) {
  return requestJson('/api/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username }),
  })
}

export function resetPassword(username, code, newPassword) {
  return requestJson('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, code, newPassword }),
  })
}
