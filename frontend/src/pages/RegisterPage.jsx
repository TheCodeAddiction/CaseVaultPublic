import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/authApi'
import AuthLayout from '../components/AuthLayout'
import '../components/AuthForm.css'

const DISTRICTS = ['NORTH', 'EAST', 'SOUTH', 'WEST']

export default function RegisterPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [district, setDistrict] = useState(DISTRICTS[0])
  const [formError, setFormError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)
    setFieldErrors({})
    setIsSubmitting(true)

    try {
      await register({ fullName, username, email, password, confirmPassword, district })
      navigate('/login', { state: { registered: true } })
    } catch (error) {
      if (error.fields) {
        setFieldErrors(error.fields)
      }
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      <form className="auth-form card" onSubmit={handleSubmit} noValidate>
        <h1>Create an account</h1>

        {formError && <p className="auth-form__error" role="alert">{formError}</p>}

        <label htmlFor="fullName">Full name</label>
        <input
          id="fullName"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          required
        />
        {fieldErrors.fullName && <p className="auth-form__field-error">{fieldErrors.fullName}</p>}

        <label htmlFor="username">Username</label>
        <input
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        {fieldErrors.username && <p className="auth-form__field-error">{fieldErrors.username}</p>}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        {fieldErrors.email && <p className="auth-form__field-error">{fieldErrors.email}</p>}

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {fieldErrors.password && <p className="auth-form__field-error">{fieldErrors.password}</p>}

        <label htmlFor="confirmPassword">Confirm password</label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
        />
        {fieldErrors.confirmPassword && <p className="auth-form__field-error">{fieldErrors.confirmPassword}</p>}

        <label htmlFor="district">District</label>
        <select id="district" value={district} onChange={(e) => setDistrict(e.target.value)}>
          {DISTRICTS.map((d) => (
            <option key={d} value={d}>{d}</option>
          ))}
        </select>

        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>Register</button>

        <p className="auth-form__footer">Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </AuthLayout>
  )
}
