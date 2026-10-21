import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AuthLayout from '../components/AuthLayout'
import '../components/AuthForm.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [formError, setFormError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      await login({ username, password })
      navigate('/')
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      <form className="auth-form card" onSubmit={handleSubmit} noValidate>
        <h1>Sign in</h1>

        {formError && <p className="auth-form__error" role="alert">{formError}</p>}

        <label htmlFor="username">Username</label>
        <input
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>Sign in</button>

        <p className="auth-form__footer">
          <Link to="/forgot-password">Forgot password?</Link>
        </p>
        <p className="auth-form__footer">Need an account? <Link to="/register">Register</Link></p>
      </form>
    </AuthLayout>
  )
}
