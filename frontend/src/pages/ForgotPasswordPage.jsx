import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { forgotPassword, resetPassword } from '../api/authApi'
import AuthLayout from '../components/AuthLayout'
import '../components/AuthForm.css'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [codeRequested, setCodeRequested] = useState(false)
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [formError, setFormError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleRequestCode(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      await forgotPassword(username)
      setCodeRequested(true)
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      await resetPassword(username, code, newPassword)
      navigate('/login', { state: { passwordReset: true } })
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      {!codeRequested && (
        <form className="auth-form card" onSubmit={handleRequestCode} noValidate>
          <h1>Forgot password</h1>
          <p>Enter your username and we'll send a reset code to the email on file.</p>

          {formError && <p className="auth-form__error" role="alert">{formError}</p>}

          <label htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} required />

          <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>Send reset code</button>

          <p className="auth-form__footer">Remembered it? <Link to="/login">Sign in</Link></p>
        </form>
      )}

      {codeRequested && (
        <form className="auth-form card" onSubmit={handleResetPassword} noValidate>
          <h1>Enter reset code</h1>
          <p>Enter the code sent to the email on file for {username}, and choose a new password.</p>

          {formError && <p className="auth-form__error" role="alert">{formError}</p>}

          <label htmlFor="code">Reset code</label>
          <input id="code" value={code} onChange={(e) => setCode(e.target.value)} required />

          <label htmlFor="newPassword">New password</label>
          <input
            id="newPassword"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
          />

          <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>Reset password</button>
        </form>
      )}
    </AuthLayout>
  )
}
