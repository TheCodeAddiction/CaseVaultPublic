import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import ProfilePage from './ProfilePage'
import { AuthContext } from '../context/AuthContext'
import * as adminApi from '../api/adminApi'
import { ApiError } from '../api/client'

const user = { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

function renderPage(logoutMock = vi.fn()) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user, token: 'jwt-token', logout: logoutMock }}>
        <ProfilePage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    navigateMock.mockReset()
  })

  it('renders the caller\'s own profile details', async () => {
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER',
    })

    renderPage()

    expect(await screen.findByText('Maria Ibsen')).toBeInTheDocument()
    expect(screen.getByText('mibsen')).toBeInTheDocument()
    expect(screen.getByText('NORTH')).toBeInTheDocument()
    expect(screen.getByText('OFFICER')).toBeInTheDocument()
    expect(adminApi.fetchMyProfile).toHaveBeenCalledWith('jwt-token')
  })

  it('shows a default avatar when there is no profile picture', async () => {
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })

    renderPage()
    await screen.findByText('Maria Ibsen')

    expect(screen.getAllByText('MI').length).toBeGreaterThan(0)
  })

  it('changes password and shows a success message', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })
    vi.spyOn(adminApi, 'changeMyPassword').mockResolvedValue({ username: 'mibsen' })

    renderPage()
    await screen.findByText('Maria Ibsen')

    await u.type(screen.getByLabelText(/current password/i), 'old-pass')
    await u.type(screen.getByLabelText(/new password/i), 'a brand new password')
    await u.click(screen.getByRole('button', { name: /change password/i }))

    expect(adminApi.changeMyPassword).toHaveBeenCalledWith('jwt-token', 'old-pass', 'a brand new password')
    expect(await screen.findByText(/password updated/i)).toBeInTheDocument()
  })

  it('shows an error when the current password is wrong', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })
    vi.spyOn(adminApi, 'changeMyPassword').mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 'Invalid username or password'))

    renderPage()
    await screen.findByText('Maria Ibsen')

    await u.type(screen.getByLabelText(/current password/i), 'wrong')
    await u.type(screen.getByLabelText(/new password/i), 'a brand new password')
    await u.click(screen.getByRole('button', { name: /change password/i }))

    expect(await screen.findByText('Invalid username or password')).toBeInTheDocument()
  })

  it('uploads a profile picture', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })
    vi.spyOn(adminApi, 'uploadMyProfilePicture').mockResolvedValue({ hasProfilePicture: true })
    const file = new File(['fake bytes'], 'avatar.jpg', { type: 'image/jpeg' })

    renderPage()
    await screen.findByText('Maria Ibsen')

    await u.upload(screen.getByLabelText(/profile picture/i), file)
    await u.click(screen.getByRole('button', { name: /upload picture/i }))

    expect(adminApi.uploadMyProfilePicture).toHaveBeenCalledWith('jwt-token', file)
    expect(await screen.findByText(/picture updated/i)).toBeInTheDocument()
  })

  it('deletes the own account and signs out', async () => {
    const u = userEvent.setup()
    const logoutMock = vi.fn()
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })
    vi.spyOn(adminApi, 'deleteOwnAccount').mockResolvedValue({ message: 'Account deleted.' })

    renderPage(logoutMock)
    await screen.findByText('Maria Ibsen')

    await u.type(screen.getByLabelText(/confirm your password/i), 'correct horse battery staple')
    await u.click(screen.getByRole('button', { name: /delete my account/i }))

    expect(adminApi.deleteOwnAccount).toHaveBeenCalledWith('jwt-token', 'correct horse battery staple')
    expect(logoutMock).toHaveBeenCalled()
    expect(navigateMock).toHaveBeenCalledWith('/login')
  })

  it('shows an error when account deletion fails', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchMyProfile').mockResolvedValue({
      id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER', hasProfilePicture: false,
    })
    vi.spyOn(adminApi, 'deleteOwnAccount').mockRejectedValue(
      new ApiError('CANNOT_DELETE_USER', 'Cannot delete a user with existing case, document, message, or activity history. Deactivate the account instead.')
    )

    renderPage()
    await screen.findByText('Maria Ibsen')

    await u.type(screen.getByLabelText(/confirm your password/i), 'correct horse battery staple')
    await u.click(screen.getByRole('button', { name: /delete my account/i }))

    expect(await screen.findByText(/deactivate the account instead/i)).toBeInTheDocument()
  })
})
