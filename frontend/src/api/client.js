export class ApiError extends Error {
  constructor(code, message, fields) {
    super(message)
    this.code = code
    this.fields = fields
  }
}

export async function requestJson(url, options = {}) {
  const response = await fetch(url, options)

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(body.error ?? 'UNKNOWN_ERROR', body.message ?? 'Something went wrong', body.fields)
  }

  return response.json()
}

export function authHeader(token) {
  return { Authorization: `Bearer ${token}` }
}
