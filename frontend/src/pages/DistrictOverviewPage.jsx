import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchDistrictOverview } from '../api/reportsApi'
import AppShell from '../components/AppShell'
import './DistrictOverviewPage.css'

export default function DistrictOverviewPage() {
  const { user, token, logout } = useAuth()
  const [overview, setOverview] = useState(null)

  useEffect(() => {
    fetchDistrictOverview(token, {}).then(setOverview)
  }, [token])

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="district-overview">
        <h1>District overview</h1>

        {overview === null && <p className="district-overview__empty">Loading…</p>}
        {overview !== null && (
          <>
            <div className="district-overview__stats">
              <div className="card district-overview__stat">
                <span className="district-overview__stat-label">District</span>
                <span className="district-overview__stat-value">{overview.district}</span>
              </div>
              <div className="card district-overview__stat">
                <span className="district-overview__stat-label">Total cases</span>
                <span className="district-overview__stat-value">{overview.totalCases}</span>
              </div>
              <div className="card district-overview__stat">
                <span className="district-overview__stat-label">Open</span>
                <span className="district-overview__stat-value">{overview.openCases}</span>
              </div>
              <div className="card district-overview__stat">
                <span className="district-overview__stat-label">In progress</span>
                <span className="district-overview__stat-value">{overview.inProgressCases}</span>
              </div>
              <div className="card district-overview__stat">
                <span className="district-overview__stat-label">Closed</span>
                <span className="district-overview__stat-value">{overview.closedCases}</span>
              </div>
            </div>

            <h2>Officer workload</h2>
            <div className="card">
              {overview.officerWorkload.length === 0 && (
                <p className="district-overview__empty">No officers in this district.</p>
              )}
              {overview.officerWorkload.length > 0 && (
                <table className="case-table">
                  <thead>
                    <tr>
                      <th>Username</th>
                      <th>Full name</th>
                      <th>Assigned cases</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overview.officerWorkload.map((o) => (
                      <tr key={o.username}>
                        <td className="case-table__mono">{o.username}</td>
                        <td>{o.fullName}</td>
                        <td>{o.assignedCaseCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </AppShell>
  )
}
