import { useState } from 'react'
import { Link } from 'react-router-dom'
import './Sidebar.css'

export default function Sidebar({ user, unreadCount = 0 }) {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <nav className={`sidebar${collapsed ? ' sidebar--collapsed' : ''}`}>
      <button
        type="button"
        className="sidebar__toggle"
        onClick={() => setCollapsed((c) => !c)}
        aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
      >
        {collapsed ? '»' : '«'}
      </button>

      <Link to="/" className="sidebar__link" aria-label="Dashboard">
        <span aria-hidden="true">⌂</span>
        {!collapsed && <span className="sidebar__label">Dashboard</span>}
      </Link>

      <Link to="/cases/new" className="sidebar__link" aria-label="New case">
        <span aria-hidden="true">+</span>
        {!collapsed && <span className="sidebar__label">New case</span>}
      </Link>

      <Link to="/messages" className="sidebar__link" aria-label="Messages">
        <span aria-hidden="true">✉</span>
        {!collapsed && (
          <span className="sidebar__label">
            Messages{unreadCount > 0 && ` (${unreadCount})`}
          </span>
        )}
      </Link>

      <Link to="/profile" className="sidebar__link" aria-label="Profile">
        <span aria-hidden="true">☺</span>
        {!collapsed && <span className="sidebar__label">Profile</span>}
      </Link>

      {(user?.role === 'SUPERVISOR' || user?.role === 'ADMIN') && (
        <Link to="/district-overview" className="sidebar__link" aria-label="District overview">
          <span aria-hidden="true">▤</span>
          {!collapsed && <span className="sidebar__label">District overview</span>}
        </Link>
      )}

      {user?.role === 'ADMIN' && (
        <Link to="/users" className="sidebar__link" aria-label="Manage users">
          <span aria-hidden="true">⚏</span>
          {!collapsed && <span className="sidebar__label">Manage users</span>}
        </Link>
      )}
    </nav>
  )
}
