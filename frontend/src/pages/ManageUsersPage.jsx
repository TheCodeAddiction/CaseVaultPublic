import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchUsers, createUser, changeUserRole, changeUserDistrict, deactivateUser, reactivateUser, deleteUser } from '../api/adminApi'
import AppShell from '../components/AppShell'
import '../components/AuthForm.css'
import './ManageUsersPage.css'

const ROLES = ['OFFICER', 'SUPERVISOR', 'ADMIN']
const DISTRICTS = ['NORTH', 'EAST', 'SOUTH', 'WEST']

export default function ManageUsersPage() {
  const { user, token, logout } = useAuth()
  const [users, setUsers] = useState(null)

  const [fullName, setFullName] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState(ROLES[0])
  const [district, setDistrict] = useState(DISTRICTS[0])
  const [formError, setFormError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [rowError, setRowError] = useState(null)

  useEffect(() => {
    fetchUsers(token).then(setUsers)
  }, [token])

  function replaceUser(updated) {
    setUsers((current) => current.map((u) => (u.id === updated.id ? updated : u)))
  }

  async function handleRoleChange(userId, newRole) {
    const updated = await changeUserRole(token, userId, newRole)
    replaceUser(updated)
  }

  async function handleDistrictChange(userId, newDistrict) {
    const updated = await changeUserDistrict(token, userId, newDistrict)
    replaceUser(updated)
  }

  async function handleDeactivate(userId) {
    const updated = await deactivateUser(token, userId)
    replaceUser(updated)
  }

  async function handleReactivate(userId) {
    const updated = await reactivateUser(token, userId)
    replaceUser(updated)
  }

  async function handleDelete(userId) {
    setRowError(null)
    try {
      await deleteUser(token, userId)
      setUsers((current) => current.filter((u) => u.id !== userId))
    } catch (error) {
      setRowError(error.message)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)
    setIsSubmitting(true)

    try {
      const created = await createUser(token, { fullName, username, email, password, role, district })
      setUsers((current) => [...current, created])
      setFullName('')
      setUsername('')
      setEmail('')
      setPassword('')
      setRole(ROLES[0])
      setDistrict(DISTRICTS[0])
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="manage-users">
        <h1>Manage users</h1>

        <div className="card manage-users__table-card">
          {rowError && <p className="auth-form__error" role="alert">{rowError}</p>}
          {users === null && <p className="manage-users__empty">Loading users…</p>}
          {users !== null && users.length === 0 && <p className="manage-users__empty">No users to show.</p>}
          {users !== null && users.length > 0 && (
            <table className="case-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Full name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>District</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="case-table__mono">{u.username}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>
                      <select
                        aria-label={`Role for ${u.username}`}
                        value={u.role}
                        disabled={!u.active}
                        onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      >
                        {ROLES.map((r) => (
                          <option key={r} value={r}>{r}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        aria-label={`District for ${u.username}`}
                        value={u.district}
                        disabled={!u.active}
                        onChange={(e) => handleDistrictChange(u.id, e.target.value)}
                      >
                        {DISTRICTS.map((d) => (
                          <option key={d} value={d}>{d}</option>
                        ))}
                      </select>
                    </td>
                    <td>{u.active ? 'Active' : 'Inactive'}</td>
                    <td className="manage-users__actions">
                      {u.active && (
                        <button
                          type="button"
                          className="btn btn-ghost-dark"
                          aria-label={`Deactivate ${u.username}`}
                          onClick={() => handleDeactivate(u.id)}
                        >
                          Deactivate
                        </button>
                      )}
                      {!u.active && (
                        <button
                          type="button"
                          className="btn btn-ghost-dark"
                          aria-label={`Reactivate ${u.username}`}
                          onClick={() => handleReactivate(u.id)}
                        >
                          Reactivate
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn btn-ghost-dark"
                        aria-label={`Delete ${u.username}`}
                        onClick={() => handleDelete(u.id)}
                      >
                        Delete
                      </button>
                      <Link to={`/users/${u.id}/activity`} className="btn btn-ghost-dark">View activity</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <form onSubmit={handleSubmit} className="auth-form card manage-users__form">
          <h2>Create user</h2>
          {formError && <p className="auth-form__error" role="alert">{formError}</p>}

          <label htmlFor="fullName">Full name</label>
          <input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} required />

          <label htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} required />

          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <label htmlFor="role">Role</label>
          <select id="role" value={role} onChange={(e) => setRole(e.target.value)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>

          <label htmlFor="district">District</label>
          <select id="district" value={district} onChange={(e) => setDistrict(e.target.value)}>
            {DISTRICTS.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>

          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>Create user</button>
        </form>
      </div>
    </AppShell>
  )
}
