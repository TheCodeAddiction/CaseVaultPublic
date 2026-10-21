const INLINE_WRAPS = {
  bold: { marker: '**', placeholder: 'bold text' },
  italic: { marker: '_', placeholder: 'italic text' },
  code: { marker: '`', placeholder: 'code' },
}

const LINE_PREFIXES = {
  heading: '## ',
  bullet: '- ',
  numbered: '1. ',
  quote: '> ',
}

const LINK_PLACEHOLDER_TEXT = 'link text'
const LINK_PLACEHOLDER_URL = 'https://'

function applyInlineWrap({ value, selectionStart, selectionEnd }, { marker, placeholder }) {
  const before = value.slice(0, selectionStart)
  const selected = value.slice(selectionStart, selectionEnd)
  const after = value.slice(selectionEnd)

  if (before.endsWith(marker) && after.startsWith(marker)) {
    return {
      value: before.slice(0, -marker.length) + selected + after.slice(marker.length),
      selectionStart: selectionStart - marker.length,
      selectionEnd: selectionEnd - marker.length,
    }
  }

  const text = selected || placeholder
  return {
    value: `${before}${marker}${text}${marker}${after}`,
    selectionStart: selectionStart + marker.length,
    selectionEnd: selectionStart + marker.length + text.length,
  }
}

function applyLinePrefix({ value, selectionStart, selectionEnd }, prefix) {
  const blockStart = value.lastIndexOf('\n', selectionStart - 1) + 1
  const nextBreak = value.indexOf('\n', selectionEnd)
  const blockEnd = nextBreak === -1 ? value.length : nextBreak

  const lines = value.slice(blockStart, blockEnd).split('\n')
  const allPrefixed = lines.every((line) => line.startsWith(prefix))
  const updated = lines
    .map((line) => (allPrefixed ? line.slice(prefix.length) : `${prefix}${line}`))
    .join('\n')

  return {
    value: value.slice(0, blockStart) + updated + value.slice(blockEnd),
    selectionStart: blockStart,
    selectionEnd: blockStart + updated.length,
  }
}

function applyLink({ value, selectionStart, selectionEnd }) {
  const before = value.slice(0, selectionStart)
  const selected = value.slice(selectionStart, selectionEnd)
  const after = value.slice(selectionEnd)
  const text = selected || LINK_PLACEHOLDER_TEXT

  // Put the caret on the URL, since that's the part the author still has to fill in.
  const urlStart = selectionStart + text.length + 3

  return {
    value: `${before}[${text}](${LINK_PLACEHOLDER_URL})${after}`,
    selectionStart: urlStart,
    selectionEnd: urlStart + LINK_PLACEHOLDER_URL.length,
  }
}

/**
 * Applies a formatting command to a textarea's current value/selection.
 * Pure: takes and returns `{value, selectionStart, selectionEnd}`, so the
 * caller owns both the state update and restoring the selection afterwards.
 */
export function applyCommand(state, command) {
  if (command === 'link') {
    return applyLink(state)
  }
  if (INLINE_WRAPS[command]) {
    return applyInlineWrap(state, INLINE_WRAPS[command])
  }
  if (LINE_PREFIXES[command]) {
    return applyLinePrefix(state, LINE_PREFIXES[command])
  }
  return state
}
