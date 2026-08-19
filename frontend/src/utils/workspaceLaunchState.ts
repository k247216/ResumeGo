export type WorkspaceLaunchState = 'first-run' | 'workspace'

export type WorkspaceLaunchInput = {
  loading: boolean
  hasError: boolean
  resumeCount: number
  jobCount: number
}

export function resolveWorkspaceLaunchState(input: WorkspaceLaunchInput): WorkspaceLaunchState {
  if (!input.loading && !input.hasError && input.resumeCount === 0 && input.jobCount === 0) {
    return 'first-run'
  }
  return 'workspace'
}
