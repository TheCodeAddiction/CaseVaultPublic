import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchCases, fetchCaseStats, exportCasesCsv } from '../api/casesApi'
import AppShell from '../components/AppShell'
import StatusPill from '../components/StatusPill'
import './DashboardPage.css'

const STATUSES = ['OPEN', 'IN_PROGRESS', 'CLOSED']

export default function DashboardPage() {
  const { user, token, logout } = useAuth()
  const [pageResult, setPageResult] = useState(null)
  const [q, setQ] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [stats, setStats] = useState(null)

  useEffect(() => {
    fetchCases(token, { q: q || undefined, status: status || undefined, page }).then(setPageResult)
  }, [token, q, status, page])

  useEffect(() => {
    fetchCaseStats(token).then(setStats).catch(() => {})
  }, [token])

  function handleSearchSubmit(event) {
    event.preventDefault()
    setPage(0)
    setQ(searchInput)
  }

  function handleStatusChange(event) {
    setPage(0)
    setStatus(event.target.value)
  }

  async function handleExport() {
    const csv = await exportCasesCsv(token, { q: q || undefined, status: status || undefined })
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'cases-export.csv'
    link.click()
    URL.revokeObjectURL(url)
  }

  const cases = pageResult?.content ?? null

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="dashboard">
        <div className="dashboard__header">
          <h1>Cases</h1>
          <Link to="/cases/new" className="btn btn-primary">New case</Link>
        </div>

        {stats !== null && (
          <div className="dashboard__stats">
            <div className="card dashboard__stat">
              <span className="dashboard__stat-label">Total</span>
              <span className="dashboard__stat-value">{stats.totalCases}</span>
            </div>
            <div className="card dashboard__stat">
              <span className="dashboard__stat-label">Open</span>
              <span className="dashboard__stat-value">{stats.openCases}</span>
            </div>
            <div className="card dashboard__stat">
              <span className="dashboard__stat-label">In progress</span>
              <span className="dashboard__stat-value">{stats.inProgressCases}</span>
            </div>
            <div className="card dashboard__stat">
              <span className="dashboard__stat-label">Closed</span>
              <span className="dashboard__stat-value">{stats.closedCases}</span>
            </div>
            {stats.recentCases.length > 0 && (
              <div className="card dashboard__recent">
                <span className="dashboard__stat-label">Recently created</span>
                <ul className="dashboard__recent-list">
                  {stats.recentCases.map((c) => (
                    <li key={c.id}>
                      <Link to={`/cases/${c.id}`}>{c.caseNumber}</Link> — <span>{c.title}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        <div className="dashboard__toolbar">
          <form onSubmit={handleSearchSubmit} className="dashboard__search">
            <input
              type="search"
              placeholder="Search cases…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            <button type="submit" className="btn btn-ghost-dark">Search</button>
          </form>

          <label htmlFor="statusFilter" className="dashboard__status-label">Status</label>
          <select id="statusFilter" value={status} onChange={handleStatusChange}>
            <option value="">All</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </select>

          <button type="button" className="btn btn-ghost-dark" onClick={handleExport}>Export CSV</button>
        </div>

        <div className="card">
          {cases === null && <p className="dashboard__empty">Loading cases…</p>}
          {cases !== null && cases.length === 0 && <p className="dashboard__empty">No cases to show.</p>}
          {cases !== null && cases.length > 0 && (
            <table className="case-table">
              <thead>
                <tr>
                  <th>Case number</th>
                  <th>Title</th>
                  <th>Status</th>
                  <th>District</th>
                </tr>
              </thead>
              <tbody>
                {cases.map((c) => (
                  <tr key={c.id}>
                    <td className="case-table__mono">
                      <Link to={`/cases/${c.id}`}>{c.caseNumber}</Link>
                    </td>
                    <td>{c.title}</td>
                    <td><StatusPill value={c.status} /></td>
                    <td>{c.district}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {pageResult !== null && pageResult.totalPages > 1 && (
          <div className="dashboard__pagination">
            <button
              type="button"
              className="btn btn-ghost-dark"
              onClick={() => setPage((p) => p - 1)}
              disabled={pageResult.page <= 0}
            >
              Previous
            </button>
            <span className="dashboard__pagination-label">
              Page {pageResult.page + 1} of {pageResult.totalPages}
            </span>
            <button
              type="button"
              className="btn btn-ghost-dark"
              onClick={() => setPage((p) => p + 1)}
              disabled={pageResult.page >= pageResult.totalPages - 1}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </AppShell>
  )
}
