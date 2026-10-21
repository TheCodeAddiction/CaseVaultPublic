import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AppShell from './AppShell'
import { AuthContext } from '../context/AuthContext'
import * as messagesApi from '../api/messagesApi'

vi.mock('../api/messagesApi')

function renderShell() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: { fullName: 'Maria Ibsen', role: 'OFFICER', district: 'NORTH' }, token: 'jwt-token', logout: vi.fn() }}>
        <AppShell user={{ fullName: 'Maria Ibsen', role: 'OFFICER', district: 'NORTH' }} onSignOut={vi.fn()}>
          <p>content</p>
        </AppShell>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('AppShell', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    messagesApi.fetchUnreadCount.mockResolvedValue({ unreadCount: 1 })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('polls the unread count on an interval', async () => {
    renderShell()
    await waitFor(() => expect(messagesApi.fetchUnreadCount).toHaveBeenCalledTimes(1))

    await vi.advanceTimersByTimeAsync(10000)
    await waitFor(() => expect(messagesApi.fetchUnreadCount).toHaveBeenCalledTimes(2))
  })
})
