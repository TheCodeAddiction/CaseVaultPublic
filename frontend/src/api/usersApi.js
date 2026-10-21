import { requestJson, authHeader } from './client'

export function searchUsers(token, q) {
  const params = new URLSearchParams({ q })
  return requestJson(`/api/users/search?${params.toString()}`, {
    headers: authHeader(token),
  })
}
