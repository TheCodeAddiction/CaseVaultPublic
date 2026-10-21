import { useCallback, useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchUserActivity } from '../api/adminApi'
import { usePolling } from '../hooks/usePolling'
import AppShell from '../components/AppShell'
import ActivityTimeline from '../components/ActivityTimeline'
import './UserActivityPage.css'

export default function UserActivityPage() {
  const { id } = useParams()
  const { user, token, logout } = useAuth()
  const [activity, setActivity] = useState(null)

  const loadActivity = useCallback(() => {
    fetchUserActivity(token, id).then(setActivity)
  }, [token, id])

  useEffect(() => {
    loadActivity()
  }, [loadActivity])

  usePolling(loadActivity, 8000)

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="user-activity">
        <Link to="/users" className="user-activity__back">← Back to manage users</Link>
        <h1>User activity</h1>

        <div className="card">
          <ActivityTimeline entries={activity} />
        </div>
      </div>
    </AppShell>
  )
}
