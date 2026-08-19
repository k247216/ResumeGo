import { describe, expect, it } from 'vitest'
import { buildResumeEditorLocation } from './editorRoute'

describe('buildResumeEditorLocation', () => {
  it('serializes positive context ids', () => {
    expect(buildResumeEditorLocation({ resumeId: 2, versionId: 3, targetId: 4 })).toEqual({
      name: 'resume-editor',
      query: { resumeId: '2', versionId: '3', targetId: '4' },
    })
  })

  it('omits invalid ids and unsupported modes', () => {
    expect(buildResumeEditorLocation({ resumeId: 0, versionId: -1, targetId: null, mode: 'import' as never })).toEqual({
      name: 'resume-editor',
      query: {},
    })
  })

  it('supports blank resume mode', () => {
    expect(buildResumeEditorLocation({ mode: 'blank' })).toEqual({
      name: 'resume-editor',
      query: { mode: 'blank' },
    })
  })
})
