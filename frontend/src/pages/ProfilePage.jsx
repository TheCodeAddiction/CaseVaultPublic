import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchMyProfile, updateMyAccount, changeMyPassword, uploadMyProfilePicture, deleteOwnAccount } from '../api/adminApi'
import AppShell from '../components/AppShell'
import ProfilePicture from '../components/ProfilePicture'
import '../components/AuthForm.css'
import './ProfilePage.css'

export default function ProfilePage() {
  const navigate = useNavigate()
  const { user, token, logout } = useAuth()
  const [profile, setProfile] = useState(null)

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [accountError, setAccountError] = useState(null)
  const [accountSuccess, setAccountSuccess] = useState(null)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordError, setPasswordError] = useState(null)
  const [passwordSuccess, setPasswordSuccess] = useState(null)

  const [pictureFile, setPictureFile] = useState(null)
  const [pictureError, setPictureError] = useState(null)
  const [pictureSuccess, setPictureSuccess] = useState(null)

  const [deletePassword, setDeletePassword] = useState('')
  const [deleteError, setDeleteError] = useState(null)

  useEffect(() => {
    fetchMyProfile(token).then((p) => {
      setProfile(p)
      setFullName(p.fullName)
      setEmail(p.email)
    })
  }, [token])

  async function handleUpdateAccount(event) {
    event.preventDefault()
    setAccountError(null)
    setAccountSuccess(null)

    try {
      const updated = await updateMyAccount(token, { fullName, email })
      setProfile(updated)
      setAccountSuccess('Account updated.')
    } catch (error) {
      setAccountError(error.message)
    }
  }

  async function handleChangePassword(event) {
    event.preventDefault()
    setPasswordError(null)
    setPasswordSuccess(null)

    try {
      await changeMyPassword(token, currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setPasswordSuccess('Password updated.')
    } catch (error) {
      setPasswordError(error.message)
    }
  }

  async function handleUploadPicture(event) {
    event.preventDefault()
    setPictureError(null)
    setPictureSuccess(null)

    if (!pictureFile) {
      setPictureError('Choose a picture to upload.')
      return
    }

    try {
      const updated = await uploadMyProfilePicture(token, pictureFile)
      setProfile((current) => ({ ...current, hasProfilePicture: updated.hasProfilePicture }))
      setPictureFile(null)
      setPictureSuccess('Picture updated.')
    } catch (error) {
      setPictureError(error.message)
    }
  }

  async function handleDeleteAccount(event) {
    event.preventDefault()
    setDeleteError(null)

    try {
      await deleteOwnAccount(token, deletePassword)
      logout()
      navigate('/login')
    } catch (error) {
      setDeleteError(error.message)
    }
  }

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="profile">
        <h1>My profile</h1>

        {profile === null && <p>Loading…</p>}
        {profile !== null && (
          <>
            <ProfilePicture
              userId={profile.id}
              fullName={profile.fullName}
              hasProfilePicture={profile.hasProfilePicture}
              size={96}
            />

            <dl className="card profile__details">
              <dt>Full name</dt>
              <dd>{profile.fullName}</dd>

              <dt>Username</dt>
              <dd>{profile.username}</dd>

              <dt>Email</dt>
              <dd>{profile.email}</dd>

              <dt>District</dt>
              <dd>{profile.district}</dd>

              <dt>Role</dt>
              <dd>{profile.role}</dd>

              <dt>Profile picture</dt>
              <dd>{profile.hasProfilePicture ? 'Set' : 'Not set'}</dd>
            </dl>

            <form onSubmit={handleUpdateAccount} className="auth-form card profile__form">
              <h2>Account details</h2>
              {accountError && <p className="auth-form__error" role="alert">{accountError}</p>}
              {accountSuccess && <p>{accountSuccess}</p>}

              <label htmlFor="fullName">Full name</label>
              <input
                id="fullName"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />

              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />

              <button type="submit" className="btn btn-primary">Save</button>
            </form>

            <form onSubmit={handleChangePassword} className="auth-form card profile__form">
              <h2>Change password</h2>
              {passwordError && <p className="auth-form__error" role="alert">{passwordError}</p>}
              {passwordSuccess && <p>{passwordSuccess}</p>}

              <label htmlFor="currentPassword">Current password</label>
              <input
                id="currentPassword"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />

              <label htmlFor="newPassword">New password</label>
              <input
                id="newPassword"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />

              <button type="submit" className="btn btn-primary">Change password</button>
            </form>

            <form onSubmit={handleUploadPicture} className="auth-form card profile__form">
              <h2>Profile picture</h2>
              {pictureError && <p className="auth-form__error" role="alert">{pictureError}</p>}
              {pictureSuccess && <p>{pictureSuccess}</p>}

              <label htmlFor="profilePicture">Profile picture</label>
              <input
                id="profilePicture"
                type="file"
                accept=".jpg,.jpeg,.png"
                onChange={(e) => setPictureFile(e.target.files[0] ?? null)}
              />

              <button type="submit" className="btn btn-primary">Upload picture</button>
            </form>

            <form onSubmit={handleDeleteAccount} className="auth-form card profile__form profile__danger">
              <h2>Delete account</h2>
              <p>This permanently deletes your account. Accounts with existing case, document,
                message, or activity history can't be deleted — deactivate instead.</p>
              {deleteError && <p className="auth-form__error" role="alert">{deleteError}</p>}

              <label htmlFor="deletePassword">Confirm your password</label>
              <input
                id="deletePassword"
                type="password"
                value={deletePassword}
                onChange={(e) => setDeletePassword(e.target.value)}
                required
              />

              <button type="submit" className="btn btn-ghost-dark">Delete my account</button>
            </form>
          </>
        )}
      </div>
    </AppShell>
  )
}
