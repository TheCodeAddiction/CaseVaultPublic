import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import ForgotPasswordPage from './ForgotPasswordPage'
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
      <ForgotPasswordPage />
    </MemoryRouter>
  )
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
  })

  it('requests a reset code and then shows the code + new password form', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'forgotPassword').mockResolvedValue({ message: 'If that account exists, a reset code has been sent.' })

    renderPage()

    await user.type(screen.getByLabelText(/username/i), 'mibsen')
    await user.click(screen.getByRole('button', { name: /send reset code/i }))

    expect(authApi.forgotPassword).toHaveBeenCalledWith('mibsen')
    expect(await screen.findByLabelText(/reset code/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/new password/i)).toBeInTheDocument()
  })

  it('submits the code and new password, then redirects to login', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'forgotPassword').mockResolvedValue({ message: 'sent' })
    vi.spyOn(authApi, 'resetPassword').mockResolvedValue({ message: 'Password updated.' })

    renderPage()
    await user.type(screen.getByLabelText(/username/i), 'mibsen')
    await user.click(screen.getByRole('button', { name: /send reset code/i }))
    await screen.findByLabelText(/reset code/i)

    await user.type(screen.getByLabelText(/reset code/i), '1234')
    await user.type(screen.getByLabelText(/new password/i), 'a brand new password')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(authApi.resetPassword).toHaveBeenCalledWith('mibsen', '1234', 'a brand new password')
    expect(navigateMock).toHaveBeenCalledWith('/login', expect.objectContaining({
      state: expect.objectContaining({ passwordReset: true }),
    }))
  })

  it('shows an error when the reset code is wrong', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'forgotPassword').mockResolvedValue({ message: 'sent' })
    vi.spyOn(authApi, 'resetPassword').mockRejectedValue(new ApiError('INVALID_RESET_CODE', 'Invalid or expired reset code'))

    renderPage()
    await user.type(screen.getByLabelText(/username/i), 'mibsen')
    await user.click(screen.getByRole('button', { name: /send reset code/i }))
    await screen.findByLabelText(/reset code/i)

    await user.type(screen.getByLabelText(/reset code/i), '0000')
    await user.type(screen.getByLabelText(/new password/i), 'a brand new password')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(await screen.findByText('Invalid or expired reset code')).toBeInTheDocument()
    expect(navigateMock).not.toHaveBeenCalled()
  })
})
