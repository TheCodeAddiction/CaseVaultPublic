import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DistrictOverviewPage from './DistrictOverviewPage'
import { AuthContext } from '../context/AuthContext'
import * as reportsApi from '../api/reportsApi'

const supervisorUser = { id: 1, username: 'sup', fullName: 'The Supervisor', district: 'NORTH', role: 'SUPERVISOR' }

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: supervisorUser, token: 'jwt-token', logout: vi.fn() }}>
        <DistrictOverviewPage />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('DistrictOverviewPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows case counts and officer workload', async () => {
    vi.spyOn(reportsApi, 'fetchDistrictOverview').mockResolvedValue({
      district: 'NORTH',
      totalCases: 5,
      openCases: 2,
      inProgressCases: 1,
      closedCases: 2,
      officerWorkload: [
        { username: 'officerA', fullName: 'Officer A', assignedCaseCount: 3 },
        { username: 'officerB', fullName: 'Officer B', assignedCaseCount: 0 },
      ],
    })

    renderPage()

    expect(await screen.findByText('NORTH')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByText('officerA')).toBeInTheDocument()
    expect(screen.getByText('officerB')).toBeInTheDocument()
    expect(reportsApi.fetchDistrictOverview).toHaveBeenCalledWith('jwt-token', {})
  })
})
