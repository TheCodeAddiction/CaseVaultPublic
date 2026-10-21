import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import RegisterPage from './RegisterPage'
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
      <RegisterPage />
    </MemoryRouter>
  )
}

async function fillAndSubmit(user, overrides = {}) {
  const values = {
    fullName: 'Maria Ibsen',
    username: 'mibsen',
    email: 'mibsen@example.test',
    password: 'correct horse battery staple',
    confirmPassword: 'correct horse battery staple',
    district: 'NORTH',
    ...overrides,
  }

  await user.type(screen.getByLabelText(/full name/i), values.fullName)
  await user.type(screen.getByLabelText(/^username/i), values.username)
  await user.type(screen.getByLabelText(/^email/i), values.email)
  await user.type(screen.getByLabelText(/^password/i), values.password)
  await user.type(screen.getByLabelText(/confirm password/i), values.confirmPassword)
  await user.selectOptions(screen.getByLabelText(/district/i), values.district)
  await user.click(screen.getByRole('button', { name: /register/i }))
}

describe('RegisterPage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
  })

  it('submits the form and redirects to login on success', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'register').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER',
    })

    renderPage()
    await fillAndSubmit(user)

    expect(authApi.register).toHaveBeenCalledWith({
      fullName: 'Maria Ibsen',
      username: 'mibsen',
      email: 'mibsen@example.test',
      password: 'correct horse battery staple',
      confirmPassword: 'correct horse battery staple',
      district: 'NORTH',
    })
    expect(navigateMock).toHaveBeenCalledWith('/login', expect.objectContaining({
      state: expect.objectContaining({ registered: true }),
    }))
  })

  it('shows the server error message when the username is taken', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'register').mockRejectedValue(
      new ApiError('USERNAME_TAKEN', 'Username is already taken: mibsen')
    )

    renderPage()
    await fillAndSubmit(user)

    expect(await screen.findByText('Username is already taken: mibsen')).toBeInTheDocument()
    expect(navigateMock).not.toHaveBeenCalled()
  })

  it('shows field-level validation errors from the server', async () => {
    const user = userEvent.setup()
    vi.spyOn(authApi, 'register').mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'One or more fields are invalid', { username: 'Username is invalid' })
    )

    renderPage()
    await fillAndSubmit(user)

    expect(await screen.findByText('Username is invalid')).toBeInTheDocument()
  })
})
