import { describe, expect, it } from 'vitest'
import { applyCommand } from './markdownCommands'

function state(value, selectionStart = value.length, selectionEnd = selectionStart) {
  return { value, selectionStart, selectionEnd }
}

describe('applyCommand', () => {
  it('wraps the selection in bold markers and keeps the text selected', () => {
    const result = applyCommand(state('call the witness', 9, 16), 'bold')

    expect(result.value).toBe('call the **witness**')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('witness')
  })

  it('inserts a placeholder and selects it when nothing is selected', () => {
    const result = applyCommand(state(''), 'italic')

    expect(result.value).toBe('_italic text_')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('italic text')
  })

  it('unwraps an already-wrapped selection', () => {
    const result = applyCommand(state('**witness**', 2, 9), 'bold')

    expect(result.value).toBe('witness')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('witness')
  })

  it('prefixes every line of a multi-line selection', () => {
    const result = applyCommand(state('one\ntwo\nthree', 0, 13), 'bullet')

    expect(result.value).toBe('- one\n- two\n- three')
  })

  it('removes the prefix when every selected line already has it', () => {
    const result = applyCommand(state('- one\n- two', 0, 11), 'bullet')

    expect(result.value).toBe('one\ntwo')
  })

  it('prefixes only the current line when there is no selection', () => {
    const result = applyCommand(state('one\ntwo', 5), 'heading')

    expect(result.value).toBe('one\n## two')
  })

  it('builds a link around the selection and puts the caret on the URL', () => {
    const result = applyCommand(state('see the report', 8, 14), 'link')

    expect(result.value).toBe('see the [report](https://)')
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe('https://')
  })

  it('leaves the state untouched for an unknown command', () => {
    const original = state('unchanged')

    expect(applyCommand(original, 'nope')).toEqual(original)
  })
})
