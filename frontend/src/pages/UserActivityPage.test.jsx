import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import UserActivityPage from './UserActivityPage'
import { AuthContext } from '../context/AuthContext'
import * as adminApi from '../api/adminApi'

const adminUser = { id: 1, username: 'chief', fullName: 'The Chief', district: 'NORTH', role: 'ADMIN' }

function renderPage(userId = '5') {
  return render(
    <MemoryRouter initialEntries={[`/users/${userId}/activity`]}>
      <AuthContext.Provider value={{ user: adminUser, token: 'jwt-token', logout: vi.fn() }}>
        <Routes>
          <Route path="/users/:id/activity" element={<UserActivityPage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('UserActivityPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the activity log for the given user id', async () => {
    vi.spyOn(adminApi, 'fetchUserActivity').mockResolvedValue([
      { id: 1, actorUsername: 'kolsen', actorFullName: 'Kari Olsen', action: 'PASSWORD_CHANGED', details: null, occurredAt: '2026-09-04T09:00:00Z' },
    ])

    renderPage('5')

    expect(await screen.findByText('PASSWORD_CHANGED')).toBeInTheDocument()
    expect(screen.getByText('Kari Olsen')).toBeInTheDocument()
    expect(adminApi.fetchUserActivity).toHaveBeenCalledWith('jwt-token', '5')
  })

  it('shows an empty state when there is no activity', async () => {
    vi.spyOn(adminApi, 'fetchUserActivity').mockResolvedValue([])

    renderPage('5')

    expect(await screen.findByText(/no activity/i)).toBeInTheDocument()
  })

  it('polls for new activity', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.spyOn(adminApi, 'fetchUserActivity').mockResolvedValue([])

    renderPage('5')
    await screen.findByText(/no activity/i)

    adminApi.fetchUserActivity.mockClear()
    await vi.advanceTimersByTimeAsync(8000)

    expect(adminApi.fetchUserActivity).toHaveBeenCalledWith('jwt-token', '5')
    vi.useRealTimers()
  })
})
