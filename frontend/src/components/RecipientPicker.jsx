import { useEffect, useState } from 'react'
import { searchUsers } from '../api/usersApi'
import './RecipientPicker.css'

export default function RecipientPicker({ token, onSelect, selectedUsername = '', id = 'recipientSearch', label = 'Recipient' }) {
  const [query, setQuery] = useState(selectedUsername)
  const [results, setResults] = useState([])

  useEffect(() => {
    setQuery(selectedUsername)
  }, [selectedUsername])

  useEffect(() => {
    if (!query || query === selectedUsername) {
      setResults([])
      return
    }

    let cancelled = false
    searchUsers(token, query).then((matches) => {
      if (!cancelled) {
        setResults(matches)
      }
    })

    return () => {
      cancelled = true
    }
  }, [token, query, selectedUsername])

  function handleSelect(user) {
    setResults([])
    onSelect(user)
  }

  return (
    <div className="recipient-picker">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoComplete="off"
        required
      />
      {results.length > 0 && (
        <ul className="recipient-picker__results">
          {results.map((user) => (
            <li key={user.id}>
              <button type="button" className="recipient-picker__result" onClick={() => handleSelect(user)}>
                <span className="recipient-picker__name">{user.fullName}</span>
                <span className="recipient-picker__meta">
                  {user.username} · {user.role} · {user.district}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
