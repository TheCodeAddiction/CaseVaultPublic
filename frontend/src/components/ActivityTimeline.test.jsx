import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import ActivityTimeline from './ActivityTimeline'

describe('ActivityTimeline', () => {
  it('shows a loading state when entries is null', () => {
    render(<ActivityTimeline entries={null} />)

    expect(screen.getByText(/loading/i)).toBeInTheDocument()
  })

  it('shows an empty state when there are no entries', () => {
    render(<ActivityTimeline entries={[]} />)

    expect(screen.getByText(/no activity/i)).toBeInTheDocument()
  })

  it('renders each entry\'s action, actor, timestamp, and details', () => {
    render(
      <ActivityTimeline
        entries={[
          { id: 1, actorUsername: 'mibsen', actorFullName: 'Maria Ibsen', action: 'CASE_CREATED', details: 'Case created', occurredAt: '2026-09-01T09:00:00Z' },
          { id: 2, actorUsername: 'kolsen', actorFullName: 'Kari Olsen', action: 'STATUS_CHANGED', details: null, occurredAt: '2026-09-02T09:00:00Z' },
        ]}
      />
    )

    expect(screen.getByText('CASE_CREATED')).toBeInTheDocument()
    expect(screen.getByText('Maria Ibsen')).toBeInTheDocument()
    expect(screen.getByText('Case created')).toBeInTheDocument()
    expect(screen.getByText('STATUS_CHANGED')).toBeInTheDocument()
    expect(screen.getByText('Kari Olsen')).toBeInTheDocument()
  })
})
