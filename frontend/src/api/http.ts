interface DesktopRuntimeConfig {
  backendOrigin: string
  workspaceToken: string
}

export interface DesktopBackupInfo {
  id: string
  createdAt: string
  sizeBytes: number
}

export interface DesktopExportResult {
  canceled?: boolean
  exportedTo?: string
}

declare global {
  interface Window {
    resumeGoDesktop?: {
      runtime: () => DesktopRuntimeConfig
      saveApiKey: (profileId: number, apiKey: string) => Promise<boolean>
      deleteApiKey: (profileId: number) => Promise<boolean>
      hasApiKey: (profileId: number) => Promise<boolean>
      applyApiKey: (profileId: number) => Promise<boolean>
      keyStorageMode: () => Promise<'secure' | 'session'>
      listBackups: () => Promise<DesktopBackupInfo[]>
      createBackup: () => Promise<{ backupDir: string } | null>
      restoreBackup: (backupId: string) => Promise<{ restored: boolean; backupDir: string }>
      exportBackup: (backupId: string | null) => Promise<DesktopExportResult>
    }
  }
}

function runtimeConfig(): DesktopRuntimeConfig | undefined {
  return typeof window === 'undefined' ? undefined : window.resumeGoDesktop?.runtime()
}

export function resolveApiUrl(input: RequestInfo | URL): RequestInfo | URL {
  const config = runtimeConfig()
  if (!config || typeof input !== 'string' || !input.startsWith('/')) {
    return input
  }
  return `${config.backendOrigin}${input}`
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const config = runtimeConfig()
  const workspaceToken = config?.workspaceToken
    || import.meta.env.VITE_LOCAL_WORKSPACE_TOKEN
    || 'resumego-local-workspace'
  const headers = new Headers(init.headers)
  if (workspaceToken) {
    headers.set('X-Workspace-Token', workspaceToken)
  }
  const response = await fetch(resolveApiUrl(input), {
    ...init,
    headers,
  })
  if (typeof window !== 'undefined') {
    void response.clone().text().then((body) => {
      if (body.includes('NOT_CONFIGURED') || body.includes('尚未配置 AI 模型服务')) {
        window.dispatchEvent(new CustomEvent('resumego:ai-not-configured'))
      }
    }).catch(() => undefined)
  }
  return response
}
