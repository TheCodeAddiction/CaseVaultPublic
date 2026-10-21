import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import RecipientPicker from './RecipientPicker'
import * as usersApi from './../api/usersApi'

describe('RecipientPicker', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('searches as the user types and lists matches', async () => {
    const u = userEvent.setup()
    vi.spyOn(usersApi, 'searchUsers').mockResolvedValue([
      { id: 2, username: 'kolsen', fullName: 'Kari Olsen', district: 'NORTH', role: 'OFFICER' },
    ])
    const onSelect = vi.fn()

    render(<RecipientPicker token="jwt-token" onSelect={onSelect} />)

    await u.type(screen.getByLabelText(/recipient/i), 'kari')

    expect(await screen.findByText('Kari Olsen')).toBeInTheDocument()
    expect(usersApi.searchUsers).toHaveBeenCalledWith('jwt-token', 'kari')
  })

  it('calls onSelect with the chosen user and clears the results', async () => {
    const u = userEvent.setup()
    vi.spyOn(usersApi, 'searchUsers').mockResolvedValue([
      { id: 2, username: 'kolsen', fullName: 'Kari Olsen', district: 'NORTH', role: 'OFFICER' },
    ])
    const onSelect = vi.fn()

    render(<RecipientPicker token="jwt-token" onSelect={onSelect} />)
    await u.type(screen.getByLabelText(/recipient/i), 'kari')
    await screen.findByText('Kari Olsen')

    await u.click(screen.getByText('Kari Olsen'))

    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ username: 'kolsen' }))
    expect(screen.queryByText('Kari Olsen')).not.toBeInTheDocument()
  })

  it('shows the currently selected recipient', () => {
    render(<RecipientPicker token="jwt-token" onSelect={vi.fn()} selectedUsername="kolsen" />)

    expect(screen.getByDisplayValue('kolsen')).toBeInTheDocument()
  })
})
