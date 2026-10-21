import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import CaseDetailPage from './CaseDetailPage'
import { AuthContext } from '../context/AuthContext'
import * as casesApi from '../api/casesApi'
import * as messagesApi from '../api/messagesApi'
import * as usersApi from '../api/usersApi'
import { ApiError } from '../api/client'

const officer = { id: 1, username: 'mibsen', fullName: 'Maria Ibsen', district: 'NORTH', role: 'OFFICER' }
const supervisor = { id: 2, username: 'boss1', fullName: 'Boss One', district: 'NORTH', role: 'SUPERVISOR' }

function renderPage(caseId = '7', currentUser = officer) {
  return render(
    <MemoryRouter initialEntries={[`/cases/${caseId}`]}>
      <AuthContext.Provider value={{ user: currentUser, token: 'jwt-token' }}>
        <Routes>
          <Route path="/cases/:id" element={<CaseDetailPage />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('CaseDetailPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(casesApi, 'fetchCase').mockResolvedValue({
      id: 7, caseNumber: 'NORTH-2026-0007', title: 'Stolen bicycle',
      description: 'Missing from the rack.', status: 'OPEN',
      district: 'NORTH', assignedOfficerUsername: 'mibsen', createdAt: '2026-09-01T09:00:00Z',
    })
    vi.spyOn(casesApi, 'fetchCaseActivity').mockResolvedValue([
      { id: 1, actorUsername: 'mibsen', actorFullName: 'Maria Ibsen', action: 'CASE_CREATED', details: 'Case created', occurredAt: '2026-09-01T09:00:00Z' },
    ])
    vi.spyOn(casesApi, 'fetchCaseDocuments').mockResolvedValue([
      { id: 1, type: 'MESSAGE', title: 'Update', content: 'No new leads.', authorUsername: 'mibsen', authorFullName: 'Maria Ibsen', createdAt: '2026-09-01T10:00:00Z' },
    ])
    vi.spyOn(casesApi, 'fetchCaseShares').mockResolvedValue([])
    vi.spyOn(casesApi, 'fetchDistrictOfficers').mockResolvedValue([
      { id: 3, username: 'officer2', fullName: 'Ola Nord', district: 'NORTH', role: 'OFFICER' },
    ])
  })

  it('renders case details, documents, and activity', async () => {
    renderPage()

    expect(await screen.findByText('Stolen bicycle')).toBeInTheDocument()
    expect(screen.getByText('NORTH-2026-0007')).toBeInTheDocument()
    expect(screen.getByText('No new leads.')).toBeInTheDocument()
    expect(screen.getByText('CASE_CREATED')).toBeInTheDocument()
    expect(screen.getByText('NORTH')).toBeInTheDocument()
  })

  it('shows an error instead of hanging on "Loading case..." when the case is inaccessible', async () => {
    vi.spyOn(casesApi, 'fetchCase').mockRejectedValue(new ApiError('CASE_NOT_FOUND', 'No case found with id: 7'))

    renderPage()

    expect(await screen.findByText(/case not found or you don't have access/i)).toBeInTheDocument()
    expect(screen.queryByText(/loading case/i)).not.toBeInTheDocument()
  })

  it('submits a new comment and refreshes the list', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'addDocument').mockResolvedValue({ id: 2, type: 'MESSAGE' })

    renderPage()
    await screen.findByText('Stolen bicycle')

    await u.type(screen.getByLabelText(/content/i), 'Follow-up call scheduled for tomorrow.')
    await u.click(screen.getByRole('button', { name: /add comment/i }))

    expect(casesApi.addDocument).toHaveBeenCalledWith('jwt-token', '7', {
      type: 'MESSAGE',
      title: undefined,
      content: 'Follow-up call scheduled for tomorrow.',
      file: undefined,
    })
    expect(casesApi.fetchCaseDocuments).toHaveBeenCalledTimes(2)
  })

  it('submits a new report with a title', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'addDocument').mockResolvedValue({ id: 3, type: 'REPORT' })

    renderPage()
    await screen.findByText('Stolen bicycle')

    await u.selectOptions(screen.getByLabelText(/document type/i), 'REPORT')
    await u.type(screen.getByLabelText(/title/i), 'Incident summary')
    await u.type(screen.getByLabelText(/content/i), 'Full narrative of the incident.')
    await u.click(screen.getByRole('button', { name: /add report/i }))

    expect(casesApi.addDocument).toHaveBeenCalledWith('jwt-token', '7', {
      type: 'REPORT',
      title: 'Incident summary',
      content: 'Full narrative of the incident.',
      file: undefined,
    })
  })

  it('submits a file upload', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'addDocument').mockResolvedValue({ id: 4, type: 'FILE' })
    const file = new File(['fake bytes'], 'evidence.jpg', { type: 'image/jpeg' })

    renderPage()
    await screen.findByText('Stolen bicycle')

    await u.selectOptions(screen.getByLabelText(/document type/i), 'FILE')
    await u.upload(screen.getByLabelText(/^file/i), file)
    await u.click(screen.getByRole('button', { name: /add file/i }))

    expect(casesApi.addDocument).toHaveBeenCalledWith('jwt-token', '7', {
      type: 'FILE',
      title: undefined,
      content: undefined,
      file,
    })
  })

  it('downloads a file document using an authenticated fetch, not a plain link', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'fetchCaseDocuments').mockResolvedValue([
      { id: 5, type: 'FILE', title: 'Scene photo', originalFilename: 'evidence.jpg', authorUsername: 'mibsen', authorFullName: 'Maria Ibsen', createdAt: '2026-09-01T10:00:00Z' },
    ])
    const fakeBlob = new Blob(['fake bytes'])
    vi.spyOn(casesApi, 'downloadDocumentFile').mockResolvedValue(fakeBlob)
    global.URL.createObjectURL = vi.fn(() => 'blob:mock')
    global.URL.revokeObjectURL = vi.fn()

    renderPage()
    await screen.findByText('Stolen bicycle')

    await u.click(screen.getByRole('button', { name: /evidence\.jpg/i }))

    expect(casesApi.downloadDocumentFile).toHaveBeenCalledWith('jwt-token', '7', 5)
  })

  it('lets any role change the case status', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'updateCaseStatus').mockResolvedValue({ id: 7, status: 'IN_PROGRESS' })

    renderPage('7', officer)
    await screen.findByText('Stolen bicycle')

    await u.selectOptions(screen.getByLabelText(/^status/i), 'IN_PROGRESS')
    await u.click(screen.getByRole('button', { name: /update status/i }))

    expect(casesApi.updateCaseStatus).toHaveBeenCalledWith('jwt-token', '7', 'IN_PROGRESS')
  })

  it('hides assignment and sharing controls from officers', async () => {
    renderPage('7', officer)
    await screen.findByText('Stolen bicycle')

    expect(screen.queryByRole('button', { name: /^assign$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^share$/i })).not.toBeInTheDocument()
  })

  it('lets a supervisor assign the case to an officer', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'assignCase').mockResolvedValue({ id: 7, assignedOfficerUsername: 'officer2' });

    renderPage('7', supervisor)
    await screen.findByText('Stolen bicycle')

    await u.selectOptions(await screen.findByLabelText(/assign to/i), 'officer2')
    await u.click(screen.getByRole('button', { name: /^assign$/i }))

    expect(casesApi.assignCase).toHaveBeenCalledWith('jwt-token', '7', 'officer2')
  })

  it('lets a supervisor share the case with another district', async () => {
    const u = userEvent.setup()
    vi.spyOn(casesApi, 'shareCase').mockResolvedValue({ id: 1, sharedWithDistrict: 'EAST' })

    renderPage('7', supervisor)
    await screen.findByText('Stolen bicycle')

    await u.selectOptions(screen.getByLabelText(/share with district/i), 'EAST')
    await u.click(screen.getByRole('button', { name: /^share district$/i }))

    expect(casesApi.shareCase).toHaveBeenCalledWith('jwt-token', '7', { district: 'EAST' })
  })

  it('lets a supervisor share the case with a specific user via a recipient lookup, not a raw input', async () => {
    const u = userEvent.setup()
    vi.spyOn(usersApi, 'searchUsers').mockResolvedValue([
      { id: 4, username: 'eastofficer', fullName: 'East Officer', district: 'EAST', role: 'OFFICER' },
    ])
    vi.spyOn(casesApi, 'shareCase').mockResolvedValue({ id: 2, sharedWithUsername: 'eastofficer' })

    renderPage('7', supervisor)
    await screen.findByText('Stolen bicycle')

    await u.type(screen.getByLabelText(/share with user/i), 'east')
    await u.click(await screen.findByText('East Officer'))
    await u.click(screen.getByRole('button', { name: /^share user$/i }))

    expect(casesApi.shareCase).toHaveBeenCalledWith('jwt-token', '7', { username: 'eastofficer' })
  })

  it('lets a supervisor remove an existing share', async () => {
    const u = userEvent.setup()
    casesApi.fetchCaseShares.mockResolvedValue([
      { id: 9, sharedWithDistrict: 'EAST', sharedWithUsername: null, sharedByUsername: 'boss1', sharedAt: '2026-09-01T09:00:00Z' },
    ])
    vi.spyOn(casesApi, 'revokeCaseShare').mockResolvedValue({ message: 'Share removed.' })

    renderPage('7', supervisor)
    await screen.findByText('Stolen bicycle')

    await u.click(await screen.findByRole('button', { name: /remove/i }))

    expect(casesApi.revokeCaseShare).toHaveBeenCalledWith('jwt-token', '7', 9)
  })

  it('sends this case to a colleague via message', async () => {
    const u = userEvent.setup()
    vi.spyOn(usersApi, 'searchUsers').mockResolvedValue([
      { id: 3, username: 'officer2', fullName: 'Ola Nord', district: 'NORTH', role: 'OFFICER' },
    ])
    vi.spyOn(messagesApi, 'sendNewMessage').mockResolvedValue({ id: 9 })

    renderPage('7', officer)
    await screen.findByText('Stolen bicycle')

    await u.type(screen.getByLabelText(/recipient/i), 'ola')
    await u.click(await screen.findByText('Ola Nord'))
    await u.click(screen.getByRole('button', { name: /send case/i }))

    expect(messagesApi.sendNewMessage).toHaveBeenCalledWith('jwt-token', expect.objectContaining({
      recipientUsername: 'officer2',
      relatedCaseId: 7,
    }))
    expect(await screen.findByText(/case sent/i)).toBeInTheDocument()
  })
})
