import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import MarkdownContent from './MarkdownContent'

describe('MarkdownContent', () => {
  it('renders markdown formatting as HTML', () => {
    const { container } = render(
      <MarkdownContent content={'## Summary\n\nSuspect was **not** on site.'} />,
    )

    expect(screen.getByRole('heading', { name: 'Summary' })).toBeInTheDocument()
    expect(container.querySelector('strong')).toHaveTextContent('not')
  })

  it('renders lists', () => {
    const { container } = render(<MarkdownContent content={'- first\n- second'} />)

    expect(container.querySelectorAll('li')).toHaveLength(2)
  })

  it('opens links in a new tab without leaking the referrer', () => {
    render(<MarkdownContent content="[case notes](https://example.test/notes)" />)

    const link = screen.getByRole('link', { name: 'case notes' })
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('keeps single line breaks, the way a report author types them', () => {
    const { container } = render(<MarkdownContent content={'first line\nsecond line'} />)

    expect(container.querySelectorAll('br')).toHaveLength(1)
    expect(container.textContent).toContain('second line')
  })

  it('renders nothing when there is no content', () => {
    const { container } = render(<MarkdownContent content={undefined} />)

    expect(container).toBeEmptyDOMElement()
  })
})
