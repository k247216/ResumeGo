const workspaceToken = import.meta.env.VITE_LOCAL_WORKSPACE_TOKEN || 'resumego-local-workspace'

export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  if (workspaceToken) {
    headers.set('X-Workspace-Token', workspaceToken)
  }
  return fetch(input, {
    ...init,
    headers,
  })
}
