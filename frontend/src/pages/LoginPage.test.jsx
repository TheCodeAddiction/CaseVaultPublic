import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from './LoginPage'
import { AuthProvider } from '../context/AuthContext'
import * as authApi from '../api/authApi'
import { ApiError } from '../api/authApi'

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    localStorage.clear()
  })

  it('logs in and redirects to the dashboard on success', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt-token',
      expiresAt: '2026-09-03T14:30:00Z',
      user: { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' },
    })

    renderPage()
    await user.type(screen.getByLabelText(/username/i), 'mibsen')
    await user.type(screen.getByLabelText(/password/i), 'correct horse battery staple')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(authApi.login).toHaveBeenCalledWith({ username: 'mibsen', password: 'correct horse battery staple' })
    expect(navigateMock).toHaveBeenCalledWith('/')
  })

  it('shows a generic error message on invalid credentials', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'login').mockRejectedValue(
      new ApiError('INVALID_CREDENTIALS', 'Invalid username or password')
    )

    renderPage()
    await user.type(screen.getByLabelText(/username/i), 'mibsen')
    await user.type(screen.getByLabelText(/password/i), 'wrong')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Invalid username or password')).toBeInTheDocument()
    expect(navigateMock).not.toHaveBeenCalled()
  })

  it('links to the forgot-password page', () => {
    renderPage()

    expect(screen.getByRole('link', { name: /forgot password/i })).toHaveAttribute('href', '/forgot-password')
  })
})
