import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchUnreadCount } from '../api/messagesApi'
import { usePolling } from '../hooks/usePolling'
import TopBar from './TopBar'
import Sidebar from './Sidebar'
import './AppShell.css'

export default function AppShell({ user, onSignOut, children }) {
  const { token } = useAuth()
  const [unreadCount, setUnreadCount] = useState(0)

  function reload() {
    if (token) {
      fetchUnreadCount(token).then((result) => setUnreadCount(result.unreadCount)).catch(() => {})
    }
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  usePolling(reload, 10000)

  return (
    <div className="app-shell">
      <TopBar user={user} onSignOut={onSignOut} unreadCount={unreadCount} />
      <div className="app-shell__body">
        <Sidebar user={user} unreadCount={unreadCount} />
        <main className="app-shell__main">{children}</main>
      </div>
    </div>
  )
}
