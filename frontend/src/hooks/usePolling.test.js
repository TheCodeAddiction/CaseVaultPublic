import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { usePolling } from './usePolling'

describe('usePolling', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('calls the callback on every interval tick', () => {
    const callback = vi.fn()
    renderHook(() => usePolling(callback, 1000))

    expect(callback).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1000)
    expect(callback).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(2000)
    expect(callback).toHaveBeenCalledTimes(3)
  })

  it('always calls the latest callback without restarting the interval', () => {
    const firstCallback = vi.fn()
    const { rerender } = renderHook(({ cb }) => usePolling(cb, 1000), {
      initialProps: { cb: firstCallback },
    })

    const secondCallback = vi.fn()
    rerender({ cb: secondCallback })

    vi.advanceTimersByTime(1000)
    expect(firstCallback).not.toHaveBeenCalled()
    expect(secondCallback).toHaveBeenCalledTimes(1)
  })

  it('stops polling on unmount', () => {
    const callback = vi.fn()
    const { unmount } = renderHook(() => usePolling(callback, 1000))

    unmount()
    vi.advanceTimersByTime(5000)
    expect(callback).not.toHaveBeenCalled()
  })
})
