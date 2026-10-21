import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import CreateCasePage from './CreateCasePage'
import { AuthContext } from '../context/AuthContext'
import * as casesApi from '../api/casesApi'
import { ApiError } from '../api/client'

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

const user = { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user, token: 'jwt-token' }}>
        <CreateCasePage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('CreateCasePage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
  })

  it('submits the form and navigates to the new case', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'createCase').mockResolvedValue({ id: 9, title: 'Stolen bicycle' })

    renderPage()
    await u.type(screen.getByLabelText(/title/i), 'Stolen bicycle')
    await u.type(screen.getByLabelText(/description/i), 'Missing from the rack outside the library.')
    await u.click(screen.getByRole('button', { name: /create case/i }))

    expect(casesApi.createCase).toHaveBeenCalledWith('jwt-token', {
      title: 'Stolen bicycle',
      description: 'Missing from the rack outside the library.',
    })
    expect(navigateMock).toHaveBeenCalledWith('/cases/9')
  })

  it('shows a server validation error', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'createCase').mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'One or more fields are invalid', { title: 'Title is too short' })
    )

    renderPage()
    await u.type(screen.getByLabelText(/title/i), 'x')
    await u.type(screen.getByLabelText(/description/i), 'Missing from the rack outside the library.')
    await u.click(screen.getByRole('button', { name: /create case/i }))

    expect(await screen.findByText('Title is too short')).toBeInTheDocument()
  })
})
