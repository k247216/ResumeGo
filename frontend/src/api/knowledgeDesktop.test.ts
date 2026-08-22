// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { openKnowledgeSource, revealKnowledgeSource } from './knowledgeDesktop'

describe('knowledgeDesktop adapter', () => {
  beforeEach(() => {
    delete (window as { resumeGoDesktop?: unknown }).resumeGoDesktop
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns DESKTOP_REQUIRED in browser development mode without pretending success', async () => {
    expect(await openKnowledgeSource(7)).toEqual({
      ok: false, code: 'DESKTOP_REQUIRED', message: '该功能仅桌面端可用',
    })
    expect(await revealKnowledgeSource(7)).toMatchObject({ ok: false, code: 'DESKTOP_REQUIRED' })
  })

  it('forwards only the positive document id to the preload bridge', async () => {
    const open = vi.fn().mockResolvedValue({ ok: true })
    const reveal = vi.fn().mockResolvedValue({ ok: false, code: 'SOURCE_MISSING', message: '无法打开受管原文' })
    ;(window as { resumeGoDesktop?: unknown }).resumeGoDesktop = {
      openKnowledgeSource: open,
      revealKnowledgeSource: reveal,
    }

    expect(await openKnowledgeSource(7)).toEqual({ ok: true })
    expect(open).toHaveBeenCalledWith(7)

    const result = await revealKnowledgeSource(9)
    expect(result).toMatchObject({ ok: false, code: 'SOURCE_MISSING' })
    expect(reveal).toHaveBeenCalledWith(9)
  })
})
