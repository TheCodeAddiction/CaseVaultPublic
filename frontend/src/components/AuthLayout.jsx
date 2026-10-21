import SealBadge from './SealBadge'
import TopBar from './TopBar'
import './AuthLayout.css'

export default function AuthLayout({ children }) {
  return (
    <div className="auth-shell">
      <TopBar />

      <div className="auth-split">
        <div className="auth-branding">
          <div className="auth-branding__watermark">
            <SealBadge size={220} faded />
          </div>
          <div className="auth-branding__content">
            <SealBadge size={48} />
            <h1 className="auth-branding__title">CaseVault</h1>
            <p className="auth-branding__motto">Case &amp; evidence management for every district.</p>
          </div>
        </div>

        <div className="auth-form-panel">
          {children}
        </div>
      </div>
    </div>
  )
}
