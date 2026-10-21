import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  fetchCases, createCase, fetchCase, fetchCaseActivity, fetchCaseDocuments, addDocument,
  updateCaseStatus, assignCase, shareCase, fetchCaseShares, fetchDistrictOfficers, exportCasesCsv,
  downloadDocumentFile, fetchCaseStats,
} from './casesApi'

describe('casesApi', () => {
  beforeEach(() => {
    global.fetch = vi.fn()
  })

  it('fetches a page of cases with the bearer token', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
          { id: 1, caseNumber: 'NORTH-2026-0001', title: 'Break-in', status: 'OPEN', district: 'NORTH', createdAt: '2026-08-20T09:00:00Z' },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    })

    const result = await fetchCases('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/cases', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.content).toHaveLength(1)
    expect(result.content[0].caseNumber).toBe('NORTH-2026-0001')
    expect(result.totalElements).toBe(1)
  })

  it('fetches cases with search/filter/pagination query params', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 }) })

    await fetchCases('jwt-token', { q: 'bicycle', status: 'OPEN', page: 1, size: 10 })

    expect(fetch).toHaveBeenCalledWith('/api/cases?q=bicycle&status=OPEN&page=1&size=10', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
  })

  it('creates a case with title and description', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 5, caseNumber: 'SOUTH-2026-0004', title: 'Stolen bicycle' }),
    })

    const result = await createCase('jwt-token', { title: 'Stolen bicycle', description: 'Missing from the rack.' })

    expect(fetch).toHaveBeenCalledWith('/api/cases', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ title: 'Stolen bicycle', description: 'Missing from the rack.' }),
    }))
    expect(result.caseNumber).toBe('SOUTH-2026-0004')
  })

  it('fetches a single case by id', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 5, title: 'Stolen bicycle' }) })

    const result = await fetchCase('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/cases/5', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.title).toBe('Stolen bicycle')
  })

  it('fetches case activity', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ([{ id: 1, action: 'CASE_CREATED' }]) })

    const result = await fetchCaseActivity('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/activity', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].action).toBe('CASE_CREATED')
  })

  it('fetches case documents', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ([{ id: 1, type: 'MESSAGE' }]) })

    const result = await fetchCaseDocuments('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/documents', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].type).toBe('MESSAGE')
  })

  it('adds a document as multipart form data', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 2, type: 'MESSAGE' }) })

    await addDocument('jwt-token', 5, { type: 'MESSAGE', title: 'Update', content: 'No new leads.' })

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/documents', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    const callArgs = fetch.mock.calls[0][1]
    expect(callArgs.body).toBeInstanceOf(FormData)
    expect(callArgs.body.get('type')).toBe('MESSAGE')
    expect(callArgs.body.get('title')).toBe('Update')
    expect(callArgs.body.get('content')).toBe('No new leads.')
  })

  it('updates case status', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 5, status: 'IN_PROGRESS' }) })

    const result = await updateCaseStatus('jwt-token', 5, 'IN_PROGRESS')

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/status', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ status: 'IN_PROGRESS' }),
    }))
    expect(result.status).toBe('IN_PROGRESS')
  })

  it('assigns a case to an officer', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ id: 5, assignedOfficerUsername: 'mibsen' }) })

    const result = await assignCase('jwt-token', 5, 'mibsen')

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/assignee', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ officerUsername: 'mibsen' }),
    }))
    expect(result.assignedOfficerUsername).toBe('mibsen')
  })

  it('shares a case with a district', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 1, sharedWithDistrict: 'EAST' }) })

    const result = await shareCase('jwt-token', 5, { district: 'EAST' })

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/shares', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ district: 'EAST', username: null }),
    }))
    expect(result.sharedWithDistrict).toBe('EAST')
  })

  it('shares a case with a specific user', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 2, sharedWithUsername: 'mibsen' }) })

    const result = await shareCase('jwt-token', 5, { username: 'mibsen' })

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/shares', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-token' }),
      body: JSON.stringify({ district: null, username: 'mibsen' }),
    }))
    expect(result.sharedWithUsername).toBe('mibsen')
  })

  it('fetches case shares', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ([{ id: 1, sharedWithDistrict: 'EAST' }]) })

    const result = await fetchCaseShares('jwt-token', 5)

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/shares', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].sharedWithDistrict).toBe('EAST')
  })

  it('fetches officers in the caller\'s district for assignment', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ([{ username: 'mibsen' }]) })

    const result = await fetchDistrictOfficers('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/admin/users', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result[0].username).toBe('mibsen')
  })

  it('exports cases as CSV text', async () => {
    fetch.mockResolvedValueOnce({ ok: true, status: 200, text: async () => 'Case Number,Title\nNORTH-2026-0001,Break-in\n' })

    const csv = await exportCasesCsv('jwt-token', { q: 'break' })

    expect(fetch).toHaveBeenCalledWith('/api/cases/export?q=break', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(csv).toContain('NORTH-2026-0001')
  })

  it('downloads a document file as a blob', async () => {
    const fakeBlob = new Blob(['fake bytes'])
    fetch.mockResolvedValueOnce({ ok: true, status: 200, blob: async () => fakeBlob })

    const blob = await downloadDocumentFile('jwt-token', 5, 2)

    expect(fetch).toHaveBeenCalledWith('/api/cases/5/documents/2/file', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(blob).toBe(fakeBlob)
  })

  it('throws an ApiError when the document download fails', async () => {
    fetch.mockResolvedValueOnce({ ok: false, status: 404, json: async () => ({ error: 'CASE_NOT_FOUND', message: 'No case found with id: 5' }) })

    await expect(downloadDocumentFile('jwt-token', 5, 2)).rejects.toMatchObject({ code: 'CASE_NOT_FOUND' })
  })

  it('fetches case stats', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ totalCases: 3, openCases: 1, inProgressCases: 1, closedCases: 1, recentCases: [] }),
    })

    const result = await fetchCaseStats('jwt-token')

    expect(fetch).toHaveBeenCalledWith('/api/cases/stats', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
    expect(result.totalCases).toBe(3)
  })
})
