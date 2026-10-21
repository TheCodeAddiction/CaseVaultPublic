import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import ProfilePicture from './ProfilePicture'
import { AuthContext } from '../context/AuthContext'
import * as adminApi from '../api/adminApi'

function renderPicture(props) {
  return render(
    <AuthContext.Provider value={{ user: { id: 1 }, token: 'jwt-token', logout: vi.fn() }}>
      <ProfilePicture {...props} />
    </AuthContext.Provider>
  )
}

describe('ProfilePicture', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    global.URL.createObjectURL = vi.fn(() => 'blob:fake-url')
    global.URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    delete global.URL.createObjectURL
    delete global.URL.revokeObjectURL
  })

  it('shows initials as a default avatar when there is no picture', () => {
    renderPicture({ userId: 1, fullName: 'Maria Ibsen', hasProfilePicture: false })

    expect(screen.getByText('MI')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('fetches and displays the picture when one is set', async () => {
    const blob = new Blob(['fake bytes'], { type: 'image/jpeg' })
    vi.spyOn(adminApi, 'fetchProfilePicture').mockResolvedValue(blob)

    renderPicture({ userId: 1, fullName: 'Maria Ibsen', hasProfilePicture: true })

    const img = await screen.findByRole('img', { name: /maria ibsen/i })
    expect(img).toHaveAttribute('src', 'blob:fake-url')
    expect(adminApi.fetchProfilePicture).toHaveBeenCalledWith('jwt-token', 1)
  })

  it('falls back to initials if fetching the picture fails', async () => {
    vi.spyOn(adminApi, 'fetchProfilePicture').mockRejectedValue(new Error('nope'))

    renderPicture({ userId: 1, fullName: 'Maria Ibsen', hasProfilePicture: true })

    expect(await screen.findByText('MI')).toBeInTheDocument()
  })
})
