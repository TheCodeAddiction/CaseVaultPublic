import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  fetchMyProfile, updateMyAccount, changeMyPassword, uploadMyProfilePicture, fetchUsers, createUser,
  changeUserRole, changeUserDistrict, deactivateUser, reactivateUser, deleteUser, deleteOwnAccount,
  fetchProfilePicture, fetchUserActivity,
} from './adminApi'

describe('adminApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('fetches the caller\'s own profile', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }),
    })

    const result = await fetchMyProfile('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/me', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.username).toBe('mibsen')
  })

  it('updates the caller\'s own full name and email', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ fullName: 'New Name' }) })

    const result = await updateMyAccount('jwt-token', { fullName: 'New Name', email: 'new@example.test' })

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/me', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body).toBeInstanceOf(FormData)
    expect(callArgs.body.get('fullName')).toBe('New Name')
    expect(callArgs.body.get('email')).toBe('new@example.test')
    expect(result.fullName).toBe('New Name')
  })

  it('changes the caller\'s own password', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ username: 'mibsen' }) })

    const result = await changeMyPassword('jwt-token', 'old-pass', 'new-pass')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/me', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body).toBeInstanceOf(FormData)
    expect(callArgs.body.get('currentPassword')).toBe('old-pass')
    expect(callArgs.body.get('newPassword')).toBe('new-pass')
    expect(result.username).toBe('mibsen')
  })

  it('uploads a profile picture', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ hasProfilePicture: true }) })
    const file = new File(['fake bytes'], 'avatar.jpg', { type: 'image/jpeg' })

    const result = await uploadMyProfilePicture('jwt-token', file)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/me', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body).toBeInstanceOf(FormData)
    expect(callArgs.body.get('file')).toBe(file)
    expect(result.hasProfilePicture).toBe(true)
  })

  it('fetches the user list', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, username: 'mibsen' }],
    })

    const result = await fetchUsers('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result).toEqual([{ id: 1, username: 'mibsen' }])
  })

  it('creates a user with a given role and district', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 2, username: 'newsupervisor', role: 'SUPERVISOR', district: 'SOUTH' }),
    })

    const result = await createUser('jwt-token', {
      fullName: 'New Supervisor',
      username: 'newsupervisor',
      email: 'newsupervisor@example.test',
      password: 'a brand new password',
      role: 'SUPERVISOR',
      district: 'SOUTH',
    })

    expect(fetch).toHaveBeenCalledWith('/api/admin/users', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({
        fullName: 'New Supervisor',
        username: 'newsupervisor',
        email: 'newsupervisor@example.test',
        password: 'a brand new password',
        role: 'SUPERVISOR',
        district: 'SOUTH',
      }),
    }))
    expect(result.username).toBe('newsupervisor')
  })

  it('changes a user\'s role', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 2, role: 'SUPERVISOR' }) })

    const result = await changeUserRole('jwt-token', 2, 'SUPERVISOR')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/2/role', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ role: 'SUPERVISOR' }),
    }))
    expect(result.role).toBe('SUPERVISOR')
  })

  it('changes a user\'s district', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 2, district: 'WEST' }) })

    const result = await changeUserDistrict('jwt-token', 2, 'WEST')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/2/district', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ district: 'WEST' }),
    }))
    expect(result.district).toBe('WEST')
  })

  it('deactivates a user', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 2, active: false }) })

    const result = await deactivateUser('jwt-token', 2)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/2/deactivate', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.active).toBe(false)
  })

  it('reactivates a user', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 2, active: true }) })

    const result = await reactivateUser('jwt-token', 2)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/2/reactivate', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.active).toBe(true)
  })

  it('deletes a user', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ message: 'User deleted.' }) })

    const result = await deleteUser('jwt-token', 2)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/2', expect.objectContaining({
      method: 'DELETE',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.message).toBe('User deleted.')
  })

  it('deletes the caller\'s own account', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ message: 'Account deleted.' }) })

    const result = await deleteOwnAccount('jwt-token', 'my-current-password')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/me', expect.objectContaining({
      method: 'DELETE',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ currentPassword: 'my-current-password' }),
    }))
    expect(result.message).toBe('Account deleted.')
  })

  it('fetches a profile picture as a blob', async () => {
    const blob = new Blob(['fake bytes'], { type: 'image/jpeg' })
    fetch.mockResolvedValueOnce({ ok: true, status: 200, blob: async () => blob })

    const result = await fetchProfilePicture('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/5/picture', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result).toBe(blob)
  })

  it('throws ApiError when there is no profile picture', async () => {
    fetch.mockResolvedValueOnce({
      ok: false,
      status: 404,
      json: async () => ({ error: 'PROFILE_PICTURE_NOT_FOUND', message: 'This user has no profile picture set' }),
    })

    await expect(fetchProfilePicture('jwt-token', 5)).rejects.toMatchObject({
      code: 'PROFILE_PICTURE_NOT_FOUND',
    })
  })

  it('fetches a user\'s activity log', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, actorUsername: 'kolsen', actorFullName: 'Kari Olsen', action: 'PASSWORD_CHANGED', details: null, occurredAt: '2026-09-04T09:00:00Z' }],
    })

    const result = await fetchUserActivity('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/admin/users/5/activity', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].action).toBe('PASSWORD_CHANGED')
  })
})
