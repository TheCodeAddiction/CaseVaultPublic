import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthProvider, useAuth } from './AuthContext'
import * as authApi from '../api/authApi'

function Probe() {
  const { user, token, login, logout, isLoading } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="username">{user?.username ?? 'none'}</span>
      <span data-testid="token">{token ?? 'none'}</span>
      <button onClick={() => login({ username: 'mibsen', password: 'secret' })}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('starts with no user when there is no stored token', async () => {
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('username')).toHaveTextContent('none')
  })

  it('hydrates the user from /api/me when a token is already stored', async () => {
    localStorage.setItem('casevault.token', 'stored-token')
    vi.spyOn(authApi, 'fetchCurrentUser').mockResolvedValue({
      id: 1, username: 'kolsen', fullName: 'Kari Olsen', district: 'SOUTH', role: 'OFFICER',
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )

    await waitFor(() => expect(screen.getByTestId('username')).toHaveTextContent('kolsen'))
    expect(authApi.fetchCurrentUser).toHaveBeenCalledWith('stored-token')
  })

  it('login stores the token and user, and persists the token to localStorage', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'new-token',
      expiresAt: '2026-09-03T14:30:00Z',
      user: { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' },
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))

    await userEvent.click(screen.getByText('login'))

    await waitFor(() => expect(screen.getByTestId('username')).toHaveTextContent('mibsen'))
    expect(screen.getByTestId('token')).toHaveTextContent('new-token')
    expect(localStorage.getItem('casevault.token')).toBe('new-token')
  })

  it('logout clears user, token, and localStorage', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'new-token',
      expiresAt: '2026-09-03T14:30:00Z',
      user: { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' },
    })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    await userEvent.click(screen.getByText('login'))
    await waitFor(() => expect(screen.getByTestId('username')).toHaveTextContent('mibsen'))

    await userEvent.click(screen.getByText('logout'))

    expect(screen.getByTestId('username')).toHaveTextContent('none')
    expect(localStorage.getItem('casevault.token')).toBeNull()
  })
})
