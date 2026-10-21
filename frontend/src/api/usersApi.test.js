import { describe, expect, it, vi, beforeEach } from 'vitest'
import { searchUsers } from './usersApi'

describe('usersApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('searches users by query', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 2, username: 'kolsen', fullName: 'Kari Olsen', district: 'NORTH', role: 'OFFICER' }],
    })

    const result = await searchUsers('jwt-token', 'kari')

    expect(fetch).toHaveBeenCalledWith('/api/users/search?q=kari', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].username).toBe('kolsen')
  })
})
