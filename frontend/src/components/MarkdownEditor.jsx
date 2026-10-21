import { useEffect, useRef, useState } from 'react'
import MarkdownContent from './MarkdownContent'
import { applyCommand } from './markdownCommands'
import './MarkdownEditor.css'

const TOOLBAR_BUTTONS = [
  { command: 'bold', label: 'Bold', hint: 'Bold (Ctrl+B)', face: 'B', faceClass: 'is-bold' },
  { command: 'italic', label: 'Italic', hint: 'Italic (Ctrl+I)', face: 'I', faceClass: 'is-italic' },
  { command: 'heading', label: 'Heading', hint: 'Heading', face: 'H' },
  { command: 'bullet', label: 'Bulleted list', hint: 'Bulleted list', face: '• —' },
  { command: 'numbered', label: 'Numbered list', hint: 'Numbered list', face: '1. —' },
  { command: 'quote', label: 'Quote', hint: 'Quote', face: '❝' },
  { command: 'code', label: 'Code', hint: 'Code', face: '</>' },
  { command: 'link', label: 'Link', hint: 'Link', face: '🔗' },
]

const SHORTCUTS = { b: 'bold', i: 'italic' }

export default function MarkdownEditor({
  id,
  label,
  value,
  onChange,
  rows = 6,
  required = false,
  maxLength,
}) {
  const textareaRef = useRef(null)
  const pendingSelection = useRef(null)
  const [isPreviewing, setIsPreviewing] = useState(false)

  useEffect(() => {
    const textarea = textareaRef.current
    if (!pendingSelection.current || !textarea) {
      return
    }
    const [start, end] = pendingSelection.current
    pendingSelection.current = null
    textarea.focus()
    textarea.setSelectionRange(start, end)
  }, [value])

  function runCommand(command) {
    const textarea = textareaRef.current
    if (!textarea) {
      return
    }

    const next = applyCommand(
      { value, selectionStart: textarea.selectionStart, selectionEnd: textarea.selectionEnd },
      command,
    )
    pendingSelection.current = [next.selectionStart, next.selectionEnd]
    onChange(next.value)
  }

  function handleKeyDown(event) {
    if (!event.ctrlKey && !event.metaKey) {
      return
    }
    const command = SHORTCUTS[event.key.toLowerCase()]
    if (command) {
      event.preventDefault()
      runCommand(command)
    }
  }

  return (
    <div className="markdown-editor">
      <label htmlFor={id}>{label}</label>

      <div className="markdown-editor__toolbar" role="toolbar" aria-label="Formatting" aria-controls={id}>
        {TOOLBAR_BUTTONS.map((button) => (
          <button
            key={button.command}
            type="button"
            className={`markdown-editor__tool${button.faceClass ? ` ${button.faceClass}` : ''}`}
            aria-label={button.label}
            title={button.hint}
            disabled={isPreviewing}
            onClick={() => runCommand(button.command)}
          >
            {button.face}
          </button>
        ))}

        <button
          type="button"
          className="markdown-editor__preview-toggle"
          aria-pressed={isPreviewing}
          onClick={() => setIsPreviewing((previewing) => !previewing)}
        >
          {isPreviewing ? 'Write' : 'Preview'}
        </button>
      </div>

      {isPreviewing ? (
        <div className="markdown-editor__preview">
          {value.trim()
            ? <MarkdownContent content={value} />
            : <p className="markdown-editor__preview-empty">Nothing to preview yet.</p>}
        </div>
      ) : (
        <textarea
          id={id}
          ref={textareaRef}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={handleKeyDown}
          rows={rows}
          required={required}
          maxLength={maxLength}
        />
      )}

      <p className="markdown-editor__hint">
        Markdown supported — <code>**bold**</code>, <code>_italic_</code>, <code>## heading</code>, <code>- list</code>.
      </p>
    </div>
  )
}
