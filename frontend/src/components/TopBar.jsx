import { Link } from 'react-router-dom'
import SealBadge from './SealBadge'
import ProfilePicture from './ProfilePicture'
import './TopBar.css'

export default function TopBar({ user, onSignOut, unreadCount = 0 }) {
  return (
    <header className="top-bar">
      <Link to="/" className="top-bar__brand">
        <SealBadge size={28} />
        <span className="top-bar__wordmark">CASEVAULT</span>
      </Link>

      {user && (
        <div className="top-bar__identity">
          <Link to="/messages" className="top-bar__inbox" aria-label="Messages">
            Messages
            {unreadCount > 0 && <span className="top-bar__badge">{unreadCount}</span>}
          </Link>
          <ProfilePicture
            userId={user.id}
            fullName={user.fullName}
            hasProfilePicture={user.hasProfilePicture}
            size={28}
          />
          <span className="top-bar__user">
            {user.fullName} · {user.role} · {user.district}
          </span>
          <button className="btn btn-ghost" onClick={onSignOut}>Sign out</button>
        </div>
      )}
    </header>
  )
}
