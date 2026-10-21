import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import MarkdownEditor from './MarkdownEditor'

function Harness({ initialValue = '' }) {
  const [value, setValue] = useState(initialValue)
  return <MarkdownEditor id="content" label="Content" value={value} onChange={setValue} />
}

describe('MarkdownEditor', () => {
  it('associates the label with the textarea', () => {
    render(<Harness />)

    expect(screen.getByLabelText('Content')).toBeInTheDocument()
  })

  it('wraps the selected text when a toolbar button is clicked', async () => {
    const u = userEvent.setup()
    render(<Harness initialValue="call the witness" />)

    const textarea = screen.getByLabelText('Content')
    textarea.setSelectionRange(9, 16)
    await u.click(screen.getByRole('button', { name: 'Bold' }))

    expect(textarea).toHaveValue('call the **witness**')
  })

  it('applies bold via the keyboard shortcut', async () => {
    const u = userEvent.setup()
    render(<Harness initialValue="urgent" />)

    const textarea = screen.getByLabelText('Content')
    textarea.focus()
    textarea.setSelectionRange(0, 6)
    await u.keyboard('{Control>}b{/Control}')

    expect(textarea).toHaveValue('**urgent**')
  })

  it('turns the selected lines into a bulleted list', async () => {
    const u = userEvent.setup()
    render(<Harness initialValue={'one\ntwo'} />)

    const textarea = screen.getByLabelText('Content')
    textarea.setSelectionRange(0, 7)
    await u.click(screen.getByRole('button', { name: 'Bulleted list' }))

    expect(textarea).toHaveValue('- one\n- two')
  })

  it('swaps the textarea for a rendered preview and back', async () => {
    const u = userEvent.setup()
    render(<Harness initialValue="**bold**" />)

    await u.click(screen.getByRole('button', { name: 'Preview' }))

    expect(screen.queryByLabelText('Content')).not.toBeInTheDocument()
    expect(screen.getByText('bold').tagName).toBe('STRONG')

    await u.click(screen.getByRole('button', { name: 'Write' }))

    expect(screen.getByLabelText('Content')).toHaveValue('**bold**')
  })

  it('disables the formatting buttons while previewing', async () => {
    const u = userEvent.setup()
    render(<Harness initialValue="text" />)

    await u.click(screen.getByRole('button', { name: 'Preview' }))

    expect(screen.getByRole('button', { name: 'Bold' })).toBeDisabled()
  })

  it('tells the author when there is nothing to preview', async () => {
    const u = userEvent.setup()
    render(<Harness />)

    await u.click(screen.getByRole('button', { name: 'Preview' }))

    expect(screen.getByText('Nothing to preview yet.')).toBeInTheDocument()
  })

  it('reports typed edits to the parent', async () => {
    const u = userEvent.setup()
    const onChange = vi.fn()
    render(<MarkdownEditor id="content" label="Content" value="" onChange={onChange} />)

    await u.type(screen.getByLabelText('Content'), 'x')

    expect(onChange).toHaveBeenCalledWith('x')
  })
})
