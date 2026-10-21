import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from './DashboardPage'
import { AuthContext } from '../context/AuthContext'
import * as casesApi from '../api/casesApi'

const user = { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }

function renderWithAuth() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user, token: 'jwt-token', logout: vi.fn() }}>
        <DashboardPage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(casesApi, 'fetchCaseStats').mockResolvedValue({
      totalCases: 0, openCases: 0, inProgressCases: 0, closedCases: 0, recentCases: [],
    })
  })

  it('renders the case list fetched for the current user', async () => {
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({
      content: [
        { id: 1, caseNumber: 'NORTH-2026-0001', title: 'Break-in at Elm Street warehouse', status: 'OPEN', district: 'NORTH', createdAt: '2026-08-20T09:00:00Z' },
        { id: 2, caseNumber: 'NORTH-2026-0002', title: 'Vehicle theft', status: 'IN_PROGRESS', district: 'NORTH', createdAt: '2026-08-25T09:00:00Z' },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    renderWithAuth()

    expect(await screen.findByText('NORTH-2026-0001')).toBeInTheDocument()
    expect(screen.getByText('Break-in at Elm Street warehouse')).toBeInTheDocument()
    expect(screen.getByText('NORTH-2026-0002')).toBeInTheDocument()
    expect(casesApi.fetchCases).toHaveBeenCalledWith('jwt-token', expect.objectContaining({}))
  })

  it('shows an empty state when there are no cases', async () => {
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })

    renderWithAuth()

    expect(await screen.findByText(/no cases/i)).toBeInTheDocument()
  })

  it('searches by keyword', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })

    renderWithAuth()
    await screen.findByText(/no cases/i)

    await u.type(screen.getByPlaceholderText(/search/i), 'bicycle')
    await u.click(screen.getByRole('button', { name: /search/i }))

    expect(casesApi.fetchCases).toHaveBeenLastCalledWith('jwt-token', expect.objectContaining({ q: 'bicycle' }))
  })

  it('filters by status', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })

    renderWithAuth()
    await screen.findByText(/no cases/i)

    await u.selectOptions(screen.getByLabelText(/status/i), 'CLOSED')

    expect(casesApi.fetchCases).toHaveBeenLastCalledWith('jwt-token', expect.objectContaining({ status: 'CLOSED' }))
  })

  it('shows case counts from the stats endpoint', async () => {
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    vi.spyOn(casesApi, 'fetchCaseStats').mockResolvedValue({
      totalCases: 6, openCases: 3, inProgressCases: 2, closedCases: 1,
      recentCases: [{ id: 'abc-123', caseNumber: 'NORTH-2026-0009', title: 'Recent case', status: 'OPEN', district: 'NORTH' }],
    })

    renderWithAuth()

    expect(await screen.findByText('6')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('Recent case')).toBeInTheDocument()
  })

  it('shows page controls and advances to the next page', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({
      content: [{ id: 1, caseNumber: 'NORTH-2026-0001', title: 'Case one', status: 'OPEN', district: 'NORTH' }],
      page: 0,
      size: 20,
      totalElements: 21,
      totalPages: 2,
    })

    renderWithAuth()
    await screen.findByText('Case one')

    expect(screen.getByText(/page 1 of 2/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled()

    await u.click(screen.getByRole('button', { name: /^next$/i }))

    expect(casesApi.fetchCases).toHaveBeenLastCalledWith('jwt-token', expect.objectContaining({ page: 1 }))
  })

  it('resets to page 0 when the search changes', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({
      content: [{ id: 1, caseNumber: 'NORTH-2026-0001', title: 'Case one', status: 'OPEN', district: 'NORTH' }],
      page: 0,
      size: 20,
      totalElements: 21,
      totalPages: 2,
    })

    renderWithAuth()
    await screen.findByText('Case one')

    await u.click(screen.getByRole('button', { name: /^next$/i }))
    await u.type(screen.getByPlaceholderText(/search/i), 'bicycle')
    await u.click(screen.getByRole('button', { name: /^search$/i }))

    expect(casesApi.fetchCases).toHaveBeenLastCalledWith('jwt-token', expect.objectContaining({ q: 'bicycle', page: 0 }))
  })

  it('exports cases as a CSV download', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCases').mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    vi.spyOn(casesApi, 'exportCasesCsv').mockResolvedValue('Case Number,Title\n')
    global.URL.createObjectURL = vi.fn(() => 'blob:mock')
    global.URL.revokeObjectURL = vi.fn()

    renderWithAuth()
    await screen.findByText(/no cases/i)

    await u.click(screen.getByRole('button', { name: /export/i }))

    expect(casesApi.exportCasesCsv).toHaveBeenCalledWith('jwt-token', expect.objectContaining({}))
  })
})
