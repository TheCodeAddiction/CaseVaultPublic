import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import TopBar from './TopBar'
import { AuthContext } from '../context/AuthContext'

function renderTopBar(props) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: props.user, token: 'jwt-token', logout: vi.fn() }}>
        <TopBar {...props} />
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('TopBar', () => {
  it('links the wordmark/seal back to the dashboard', () => {
    renderTopBar({})

    const link = screen.getByRole('link', { name: /casevault/i })
    expect(link).toHaveAttribute('href', '/')
  })

  it('shows identity and sign-out only when a user is provided', () => {
    renderTopBar({ user: { fullName: 'Maria Ibsen', role: 'OFFICER', district: 'NORTH' }, onSignOut: vi.fn() })

    expect(screen.getByText(/Maria Ibsen/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument()
  })

  it('shows an unread badge only when there are unread messages', () => {
    const { rerender } = render(
      <MemoryRouter>
        <AuthContext.Provider value={{ user: null, token: 'jwt-token', logout: vi.fn() }}>
          <TopBar user={{ fullName: 'Maria Ibsen', role: 'OFFICER', district: 'NORTH' }} onSignOut={vi.fn()} unreadCount={0} />
        </AuthContext.Provider>
      </MemoryRouter>
    )
    expect(screen.queryByText('3')).not.toBeInTheDocument()

    rerender(
      <MemoryRouter>
        <AuthContext.Provider value={{ user: null, token: 'jwt-token', logout: vi.fn() }}>
          <TopBar user={{ fullName: 'Maria Ibsen', role: 'OFFICER', district: 'NORTH' }} onSignOut={vi.fn()} unreadCount={3} />
        </AuthContext.Provider>
      </MemoryRouter>
    )
    expect(screen.getByText('3')).toBeInTheDocument()
  })
})
