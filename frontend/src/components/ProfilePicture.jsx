import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { fetchProfilePicture } from '../api/adminApi'
import './ProfilePicture.css'

function initialsOf(fullName) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}

export default function ProfilePicture({ userId, fullName, hasProfilePicture, size = 64 }) {
  const { token } = useAuth()
  const [imageUrl, setImageUrl] = useState(null)

  useEffect(() => {
    if (!hasProfilePicture || !userId) {
      setImageUrl(null)
      return
    }

    let objectUrl = null
    fetchProfilePicture(token, userId)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setImageUrl(objectUrl)
      })
      .catch(() => setImageUrl(null))

    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [token, userId, hasProfilePicture])

  const dimension = `${size}px`

  if (imageUrl) {
    return (
      <img
        className="profile-picture"
        src={imageUrl}
        alt={fullName}
        style={{ width: dimension, height: dimension }}
      />
    )
  }

  return (
    <span
      className="profile-picture profile-picture--placeholder"
      style={{ width: dimension, height: dimension }}
      aria-label={fullName}
    >
      {initialsOf(fullName)}
    </span>
  )
}
