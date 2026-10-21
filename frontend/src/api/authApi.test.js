import { describe, expect, it, vi, beforeEach } from 'vitest'
import { register, login, fetchCurrentUser, forgotPassword, resetPassword, ApiError } from './authApi'

describe('authApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('register posts to /api/auth/register and returns the created user', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }),
    })

    const result = await register({
      fullName: 'Maria Ibsen',
      username: 'mibsen',
      password: 'correct horse battery staple',
      confirmPassword: 'correct horse battery staple',
      district: 'NORTH',
    })

    expect(fetch).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
    }))
    expect(result.username).toBe('mibsen')
  })

  it('register throws ApiError with server message on failure', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 409,
      json: async () => ({ error: 'USERNAME_TAKEN', message: 'Username is already taken: mibsen' }),
    })

    await expect(register({
      fullName: 'Maria Ibsen',
      username: 'mibsen',
      password: 'x',
      confirmPassword: 'x',
      district: 'NORTH',
    })).rejects.toMatchObject({
      code: 'USERNAME_TAKEN',
      message: 'Username is already taken: mibsen',
    })
  })

  it('login posts credentials and returns token + user', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        token: 'jwt-token',
        expiresAt: '2026-09-03T14:30:00Z',
        user: { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' },
      }),
    })

    const result = await login({ username: 'mibsen', password: 'secret' })

    expect(fetch).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(result.token).toBe('jwt-token')
    expect(result.user.username).toBe('mibsen')
  })

  it('login throws ApiError on invalid credentials', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ error: 'INVALID_CREDENTIALS', message: 'Invalid username or password' }),
    })

    await expect(login({ username: 'mibsen', password: 'wrong' }))
      .rejects.toBeInstanceOf(ApiError)
  })

  it('fetchCurrentUser sends the bearer token and returns the profile', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }),
    })

    const result = await fetchCurrentUser('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/me', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.username).toBe('mibsen')
  })

  it('forgotPassword posts the username', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ message: 'If that account exists, a reset code has been sent.' }),
    })

    const result = await forgotPassword('mibsen')

    expect(fetch).toHaveBeenCalledWith('/api/auth/forgot-password', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ username: 'mibsen' }),
    }))
    expect(result.message).toMatch(/reset code/i)
  })

  it('resetPassword posts username, code, and new password', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ message: 'Password updated.' }),
    })

    const result = await resetPassword('mibsen', '1234', 'a brand new password')

    expect(fetch).toHaveBeenCalledWith('/api/auth/reset-password', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ username: 'mibsen', code: '1234', newPassword: 'a brand new password' }),
    }))
    expect(result.message).toBe('Password updated.')
  })

  it('resetPassword throws ApiError on an invalid code', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({ error: 'INVALID_RESET_CODE', message: 'Invalid or expired reset code' }),
    })

    await expect(resetPassword('mibsen', '0000', 'a brand new password'))
      .rejects.toBeInstanceOf(ApiError)
  })
})
