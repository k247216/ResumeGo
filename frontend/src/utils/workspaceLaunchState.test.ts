import { describe, expect, it } from 'vitest'
import { resolveWorkspaceLaunchState } from './workspaceLaunchState'

describe('resolveWorkspaceLaunchState', () => {
  it('returns first-run only after empty data loads successfully', () => {
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, jobCount: 0 }))
      .toBe('first-run')
  })

  it('does not hide loading, errors, or existing workspace data behind onboarding', () => {
    expect(resolveWorkspaceLaunchState({ loading: true, hasError: false, resumeCount: 0, jobCount: 0 }))
      .toBe('workspace')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: true, resumeCount: 0, jobCount: 0 }))
      .toBe('workspace')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 1, jobCount: 0 }))
      .toBe('workspace')
    expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, jobCount: 1 }))
      .toBe('workspace')
  })
})
