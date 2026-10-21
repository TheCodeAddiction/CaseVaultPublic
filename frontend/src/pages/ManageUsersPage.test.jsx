import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import ManageUsersPage from './ManageUsersPage'
import { AuthContext } from '../context/AuthContext'
import * as adminApi from '../api/adminApi'
import { ApiError } from '../api/client'

const adminUser = { id: 1, username: 'chief', fullName: 'The Chief', district: 'NORTH', role: 'ADMIN' }

function officerRow(overrides = {}) {
  return {
    id: 2, username: 'officerA', email: 'officera@example.test', fullName: 'Officer A', district: 'EAST', role: 'OFFICER', active: true,
    ...overrides,
  }
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: adminUser, token: 'jwt-token', logout: vi.fn() }}>
        <ManageUsersPage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ManageUsersPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('lists all users', async () => {
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([
      { id: 1, username: 'chief', fullName: 'The Chief', district: 'NORTH', role: 'ADMIN' },
      { id: 2, username: 'officerA', fullName: 'Officer A', district: 'EAST', role: 'OFFICER' },
    ])

    renderPage()

    expect(await screen.findByText('officerA')).toBeInTheDocument()
    expect(screen.getByText('chief')).toBeInTheDocument()
    expect(adminApi.fetchUsers).toHaveBeenCalledWith('jwt-token')
  })

  it('links to a user\'s activity page', async () => {
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])

    renderPage()
    await screen.findByText('officerA')

    expect(screen.getByRole('link', { name: /view activity/i })).toHaveAttribute('href', '/users/2/activity')
  })

  it('creates a new user and adds it to the list', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([
      { id: 1, username: 'chief', fullName: 'The Chief', district: 'NORTH', role: 'ADMIN' },
    ])
    vi.spyOn(adminApi, 'createUser').mockResolvedValue({
      id: 2, username: 'newsupervisor', email: 'newsupervisor@example.test', fullName: 'New Supervisor', district: 'SOUTH', role: 'SUPERVISOR',
    })

    renderPage()
    await screen.findByText('chief')

    await u.type(screen.getByLabelText(/full name/i), 'New Supervisor')
    await u.type(screen.getByLabelText(/username/i), 'newsupervisor')
    await u.type(screen.getByLabelText(/email/i), 'newsupervisor@example.test')
    await u.type(screen.getByLabelText(/password/i), 'a brand new password')
    await u.selectOptions(screen.getByLabelText(/^role$/i), 'SUPERVISOR')
    await u.selectOptions(screen.getByLabelText(/^district$/i), 'SOUTH')
    await u.click(screen.getByRole('button', { name: /create user/i }))

    expect(adminApi.createUser).toHaveBeenCalledWith('jwt-token', {
      fullName: 'New Supervisor',
      username: 'newsupervisor',
      email: 'newsupervisor@example.test',
      password: 'a brand new password',
      role: 'SUPERVISOR',
      district: 'SOUTH',
    })
    expect(await screen.findByText('newsupervisor')).toBeInTheDocument()
  })

  it('shows an error when creation fails', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([])
    vi.spyOn(adminApi, 'createUser').mockRejectedValue(new ApiError('USERNAME_TAKEN', 'Username is already taken: chief'))

    renderPage()
    await screen.findByText(/no users/i)

    await u.type(screen.getByLabelText(/full name/i), 'Dup')
    await u.type(screen.getByLabelText(/username/i), 'chief')
    await u.type(screen.getByLabelText(/email/i), 'dup@example.test')
    await u.type(screen.getByLabelText(/password/i), 'a brand new password')
    await u.click(screen.getByRole('button', { name: /create user/i }))

    expect(await screen.findByText('Username is already taken: chief')).toBeInTheDocument()
  })

  it('changes a user\'s role', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])
    vi.spyOn(adminApi, 'changeUserRole').mockResolvedValue(officerRow({ role: 'SUPERVISOR' }))

    renderPage()
    await screen.findByText('officerA')

    await u.selectOptions(screen.getByLabelText(/role for officera/i), 'SUPERVISOR')

    expect(adminApi.changeUserRole).toHaveBeenCalledWith('jwt-token', 2, 'SUPERVISOR')
    expect(await screen.findByDisplayValue('SUPERVISOR')).toBeInTheDocument()
  })

  it('changes a user\'s district', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])
    vi.spyOn(adminApi, 'changeUserDistrict').mockResolvedValue(officerRow({ district: 'WEST' }))

    renderPage()
    await screen.findByText('officerA')

    await u.selectOptions(screen.getByLabelText(/district for officera/i), 'WEST')

    expect(adminApi.changeUserDistrict).toHaveBeenCalledWith('jwt-token', 2, 'WEST')
    expect(await screen.findByDisplayValue('WEST')).toBeInTheDocument()
  })

  it('deactivates a user', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])
    vi.spyOn(adminApi, 'deactivateUser').mockResolvedValue(officerRow({ active: false }))

    renderPage()
    await screen.findByText('officerA')

    await u.click(screen.getByRole('button', { name: /deactivate officera/i }))

    expect(adminApi.deactivateUser).toHaveBeenCalledWith('jwt-token', 2)
    expect(await screen.findByText(/inactive/i)).toBeInTheDocument()
  })

  it('reactivates a deactivated user', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow({ active: false })])
    vi.spyOn(adminApi, 'reactivateUser').mockResolvedValue(officerRow({ active: true }))

    renderPage()
    await screen.findByText('officerA')

    await u.click(screen.getByRole('button', { name: /reactivate officera/i }))

    expect(adminApi.reactivateUser).toHaveBeenCalledWith('jwt-token', 2)
    expect(await screen.findByText(/^active$/i)).toBeInTheDocument()
  })

  it('deletes a user with no history', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])
    vi.spyOn(adminApi, 'deleteUser').mockResolvedValue({ message: 'User deleted.' })

    renderPage()
    await screen.findByText('officerA')

    await u.click(screen.getByRole('button', { name: /delete officera/i }))

    expect(adminApi.deleteUser).toHaveBeenCalledWith('jwt-token', 2)
    expect(screen.queryByText('officerA')).not.toBeInTheDocument()
  })

  it('shows an error when deletion is blocked by existing history', async () => {
    const u = userEvent.setup()
    vi.spyOn(adminApi, 'fetchUsers').mockResolvedValue([officerRow()])
    vi.spyOn(adminApi, 'deleteUser').mockRejectedValue(
      new ApiError('CANNOT_DELETE_USER', 'Cannot delete a user with existing case, document, message, or activity history. Deactivate the account instead.')
    )

    renderPage()
    await screen.findByText('officerA')

    await u.click(screen.getByRole('button', { name: /delete officera/i }))

    expect(await screen.findByText(/deactivate the account instead/i)).toBeInTheDocument()
    expect(screen.getByText('officerA')).toBeInTheDocument()
  })
})
