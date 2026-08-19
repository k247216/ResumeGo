import { describe, expect, it } from 'vitest'
import { resolveWorkspaceLaunchState } from './workspaceLaunchState'

describe('resolveWorkspaceLaunchState', () => {
  it('maps loading, failure, empty, library, and target workspaces explicitly', () => {
    expect(resolveWorkspaceLaunchState({ loading: true, hasError: false, resumeCount: 0, targetCount: 0 }))
      .toBe('loading')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: true, resumeCount: 0, targetCount: 0 }))
      .toBe('error')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, targetCount: 0 }))
      .toBe('first-run')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 2, targetCount: 0 }))
      .toBe('library')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, targetCount: 1 }))
      .toBe('target')
  })
})
