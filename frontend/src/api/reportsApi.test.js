import { describe, expect, it, vi, beforeEach } from 'vitest'
import { fetchDistrictOverview } from './reportsApi'

describe('reportsApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('fetches the district overview for the caller\'s own district', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ district: 'NORTH', totalCases: 3, openCases: 1, inProgressCases: 1, closedCases: 1, officerWorkload: [] }),
    })

    const result = await fetchDistrictOverview('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/reports/district-overview', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.district).toBe('NORTH')
  })

  it('fetches the district overview for a given district', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ district: 'WEST', totalCases: 0, openCases: 0, inProgressCases: 0, closedCases: 0, officerWorkload: [] }),
    })

    const result = await fetchDistrictOverview('jwt-token', { district: 'WEST' })

    expect(fetch).toHaveBeenCalledWith('/api/reports/district-overview?district=WEST', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.district).toBe('WEST')
  })
})
