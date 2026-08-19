export type WorkspaceLaunchState = 'loading' | 'error' | 'first-run' | 'library' | 'target'

export type WorkspaceLaunchInput = {
  loading: boolean
  hasError: boolean
  resumeCount: number
  targetCount: number
}

export function resolveWorkspaceLaunchState(input: WorkspaceLaunchInput): WorkspaceLaunchState {
  if (input.loading) return 'loading'
  if (input.hasError) return 'error'
  if (input.targetCount > 0) return 'target'
  if (input.resumeCount > 0) return 'library'
  return 'first-run'
}
