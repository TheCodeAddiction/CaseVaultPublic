import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Sidebar from './Sidebar'

function renderSidebar(user) {
  return render(
    <MemoryRouter>
      <Sidebar user={user} />
    </MemoryRouter>
  )
}

describe('Sidebar', () => {
  it('shows navigation links with labels by default', () => {
    renderSidebar()

    expect(screen.getByRole('link', { name: /dashboard/i })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: /new case/i })).toHaveAttribute('href', '/cases/new')
    expect(screen.getByRole('link', { name: /profile/i })).toHaveAttribute('href', '/profile')
  })

  it('does not show manage users link for non-admins', () => {
    renderSidebar({ role: 'OFFICER' })

    expect(screen.queryByRole('link', { name: /manage users/i })).not.toBeInTheDocument()
  })

  it('shows manage users link for admins', () => {
    renderSidebar({ role: 'ADMIN' })

    expect(screen.getByRole('link', { name: /manage users/i })).toHaveAttribute('href', '/users')
  })

  it('does not show district overview link for officers', () => {
    renderSidebar({ role: 'OFFICER' })

    expect(screen.queryByRole('link', { name: /district overview/i })).not.toBeInTheDocument()
  })

  it('shows district overview link for supervisors and admins', () => {
    renderSidebar({ role: 'SUPERVISOR' })
    expect(screen.getByRole('link', { name: /district overview/i })).toHaveAttribute('href', '/district-overview')
  })

  it('collapses to hide labels and expands back', async () => {
    const u = userEvent.setup()
    renderSidebar()

    const toggle = screen.getByRole('button', { name: /collapse/i })
    await u.click(toggle)

    expect(screen.queryByText(/^dashboard$/i)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /dashboard/i })).toBeInTheDocument()

    await u.click(screen.getByRole('button', { name: /expand/i }))
    expect(screen.getByText(/^dashboard$/i)).toBeInTheDocument()
  })
})
