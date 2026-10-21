import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { createCase } from '../api/casesApi'
import AppShell from '../components/AppShell'
import '../components/AuthForm.css'

export default function CreateCasePage() {
  const navigate = useNavigate()
  const { user, token, logout } = useAuth()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [formError, setFormError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)
    setFieldErrors({})
    setIsSubmitting(true)

    try {
      const created = await createCase(token, { title, description })
      navigate(`/cases/${created.id}`)
    } catch (error) {
      if (error.fields) setFieldErrors(error.fields)
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AppShell user={user} onSignOut={logout}>
      <div style={{ padding: '2rem 1.5rem' }}>
        <form className="auth-form card" onSubmit={handleSubmit} noValidate style={{ maxWidth: 480 }}>
          <h1>Create a case</h1>

          {formError && <p className="auth-form__error" role="alert">{formError}</p>}

          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          {fieldErrors.title && <p className="auth-form__field-error">{fieldErrors.title}</p>}

          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            rows={6}
          />
          {fieldErrors.description && <p className="auth-form__field-error">{fieldErrors.description}</p>}

          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>Create case</button>
        </form>
      </div>
    </AppShell>
  )
}
