import { randomBytes } from 'node:crypto'
import { spawn, type ChildProcess } from 'node:child_process'
import { closeSync, createReadStream, openSync, readFileSync } from 'node:fs'
import { mkdir, stat } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import net from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { app, BrowserWindow, dialog, ipcMain, safeStorage } from 'electron'
import { buildBackendLaunchSpec } from './backendProcess.js'
import { DesktopKeyStore } from './keyStore.js'
import { isTrustedRendererUrl } from './security.js'
import { createBeforeQuitHandler, terminateChildProcess } from './processLifecycle.js'
import {
  createColdWorkspaceBackup,
  exportWorkspaceBackup,
  listWorkspaceBackups,
  restoreWorkspaceBackup,
} from './workspaceBackup.js'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
app.setName('ResumeGo')
let backendProcess: ChildProcess | null = null
let frontendServer: Server | null = null
let runtimeConfig = { backendOrigin: '', workspaceToken: '' }
let internalToken = ''
let keyStore: DesktopKeyStore | null = null
let trustedFrontendOrigin = ''
let dataDir = ''
let backendLogFd: number | null = null

function findOpenPort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.unref()
    server.on('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        server.close(() => reject(new Error('无法分配本地端口')))
        return
      }
      server.close(() => resolve(address.port))
    })
  })
}

async function waitForBackend(origin: string, child: ChildProcess): Promise<void> {
  const deadline = Date.now() + 45_000
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(
        '本地服务提前退出（退出码 ' + child.exitCode + '）。请查看日志文件 backend.log 了解详情；原始本地数据和启动前备份均未删除。',
      )
    }
    try {
      const response = await fetch(`${origin}/actuator/health`)
      if (response.ok) return
    } catch {
      // The process is still starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error('本地服务启动超时（45 秒）。请查看日志文件 backend.log 了解详情。')
}

function contentType(filePath: string): string {
  return ({
    '.css': 'text/css; charset=utf-8',
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
  } as Record<string, string>)[path.extname(filePath)] ?? 'application/octet-stream'
}

async function startFrontendServer(distDir: string): Promise<string> {
  const port = await findOpenPort()
  const resolvedDist = path.resolve(distDir)
  frontendServer = createServer(async (request, response) => {
    try {
      const pathname = decodeURIComponent(new URL(request.url ?? '/', 'http://127.0.0.1').pathname)
      const requested = pathname === '/' ? 'index.html' : pathname.slice(1)
      let filePath = path.resolve(resolvedDist, requested)
      if (!filePath.startsWith(`${resolvedDist}${path.sep}`) && filePath !== resolvedDist) {
        response.writeHead(403).end()
        return
      }
      try {
        const info = await stat(filePath)
        if (!info.isFile()) throw new Error('not a file')
      } catch {
        filePath = path.join(resolvedDist, 'index.html')
      }
      response.setHeader('Content-Type', contentType(filePath))
      response.setHeader('Cache-Control', 'no-store')
      createReadStream(filePath).pipe(response)
    } catch {
      response.writeHead(500).end('ResumeGo failed to load')
    }
  })
  await new Promise<void>((resolve, reject) => {
    frontendServer!.once('error', reject)
    frontendServer!.listen(port, '127.0.0.1', resolve)
  })
  return `http://127.0.0.1:${port}`
}

ipcMain.on('resumego:runtime-config', (event) => {
  assertTrustedIpc(event)
  event.returnValue = runtimeConfig
})
ipcMain.handle('resumego:key-save', async (event, profileId: number, apiKey: string) => {
  assertTrustedIpc(event)
  if (!keyStore) throw new Error('安全存储尚未就绪')
  await keyStore.save(profileId, apiKey)
  await applyStoredProvider(profileId)
  return true
})
ipcMain.handle('resumego:key-delete', async (event, profileId: number) => {
  assertTrustedIpc(event)
  if (!keyStore) throw new Error('安全存储尚未就绪')
  await keyStore.delete(profileId)
  await providerRequest(`/api/ai/runtime/${profileId}`, { method: 'DELETE' })
  return true
})
ipcMain.handle('resumego:key-has', async (event, profileId: number) => {
  assertTrustedIpc(event)
  return keyStore?.has(profileId) ?? false
})
ipcMain.handle('resumego:key-apply', async (event, profileId: number) => {
  assertTrustedIpc(event)
  return applyStoredProvider(profileId)
})
ipcMain.handle('resumego:key-storage-mode', async (event) => {
  assertTrustedIpc(event)
  return keyStore?.mode() ?? 'session'
})

ipcMain.handle('resumego:backup-list', async (event) => {
  assertTrustedIpc(event)
  return listWorkspaceBackups(dataDir)
})
ipcMain.handle('resumego:backup-create', async (event) => {
  assertTrustedIpc(event)
  return createColdWorkspaceBackup(dataDir)
})
ipcMain.handle('resumego:backup-restore', async (event, backupId: string) => {
  assertTrustedIpc(event)
  const result = await restoreWorkspaceBackup(dataDir, backupId)
  if (result.restored) {
    // A restored database requires the local backend to reload its file; simplest
    // safe path is to tell the renderer to restart the workspace via app reload.
    return result
  }
  return result
})
ipcMain.handle('resumego:backup-export', async (event, backupId: string | null) => {
  assertTrustedIpc(event)
  const choice = await dialog.showOpenDialog({
    properties: ['openDirectory', 'createDirectory'],
    title: '选择备份保存位置',
  })
  if (choice.canceled || choice.filePaths.length === 0) {
    return { canceled: true }
  }
  const result = await exportWorkspaceBackup(dataDir, backupId, choice.filePaths[0])
  return { canceled: false, ...result }
})


async function startApplication(): Promise<void> {
  const projectRoot = path.resolve(currentDir, '..', '..')
  dataDir = path.join(app.getPath('userData'), 'workspace')
  await mkdir(dataDir, { recursive: true })
  await createColdWorkspaceBackup(dataDir)

  const backendPort = await findOpenPort()
  const workspaceToken = randomBytes(32).toString('hex')
  internalToken = randomBytes(32).toString('hex')
  keyStore = new DesktopKeyStore(path.join(app.getPath('userData'), 'secure'), {
    isAvailable: () => safeStorage.isEncryptionAvailable()
      && (process.platform !== 'linux' || safeStorage.getSelectedStorageBackend() !== 'basic_text'),
    encrypt: (value) => safeStorage.encryptString(value),
    decrypt: (value) => safeStorage.decryptString(value),
  })
  const spec = buildBackendLaunchSpec({
    isPackaged: app.isPackaged,
    resourcesPath: process.resourcesPath,
    projectRoot,
    dataDir,
    port: backendPort,
    workspaceToken,
    internalToken,
    platform: process.platform,
  })
  backendLogFd = openSync(path.join(app.getPath('userData'), 'backend.log'), 'a')
  backendProcess = spawn(spec.command, spec.args, {
    cwd: app.isPackaged ? process.resourcesPath : projectRoot,
    env: spec.env,
    stdio: ['ignore', backendLogFd, backendLogFd],
    windowsHide: true,
  })
  runtimeConfig = {
    backendOrigin: `http://127.0.0.1:${backendPort}`,
    workspaceToken,
  }
  await waitForBackend(runtimeConfig.backendOrigin, backendProcess)
  await restoreDefaultProvider()

  const distDir = app.isPackaged
    ? path.join(process.resourcesPath, 'frontend')
    : path.join(projectRoot, 'frontend', 'dist')
  const frontendOrigin = await startFrontendServer(distDir)
  trustedFrontendOrigin = frontendOrigin
  const mainWindow = new BrowserWindow({
    width: 1440,
    height: 960,
    minWidth: 1080,
    minHeight: 720,
    show: false,
    backgroundColor: '#f5f3ee',
    webPreferences: {
      preload: path.join(currentDir, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  })
  mainWindow.webContents.on('will-navigate', (event, navigationUrl) => {
    if (!isTrustedRendererUrl(navigationUrl, trustedFrontendOrigin)) event.preventDefault()
  })
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
  mainWindow.once('ready-to-show', () => mainWindow.show())
  await mainWindow.loadURL(frontendOrigin)
}

async function providerRequest(pathname: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  headers.set('X-Workspace-Token', runtimeConfig.workspaceToken)
  headers.set('Content-Type', 'application/json')
  return fetch(`${runtimeConfig.backendOrigin}${pathname}`, { ...init, headers })
}

async function applyStoredProvider(profileId: number): Promise<boolean> {
  const apiKey = await keyStore?.getForMain(profileId)
  if (!apiKey) return false
  const response = await providerRequest('/api/ai/runtime/apply', {
    method: 'POST',
    headers: { 'X-ResumeGo-Internal': internalToken },
    body: JSON.stringify({ profileId, apiKey }),
  })
  if (!response.ok) throw new Error('无法装载已保存的模型配置')
  return true
}

async function restoreDefaultProvider(): Promise<void> {
  try {
    const response = await providerRequest('/api/ai/providers')
    if (!response.ok) return
    const body = await response.json() as { data?: Array<{ id: number; defaultProfile: boolean }> }
    const selected = body.data?.find((profile) => profile.defaultProfile)
    if (selected) await applyStoredProvider(selected.id)
  } catch {
    // AI remains optional; startup must not fail because a provider cannot be restored.
  }
}

async function stopChildren(): Promise<void> {
  const server = frontendServer
  frontendServer = null
  const child = backendProcess
  backendProcess = null
  if (server) {
    server.closeAllConnections()
    await new Promise<void>((resolve) => server.close(() => resolve()))
  }
  if (child) await terminateChildProcess(child)
  if (backendLogFd !== null) {
    try { closeSync(backendLogFd) } catch { /* already closed */ }
    backendLogFd = null
  }
}

function assertTrustedIpc(event: Electron.IpcMainEvent | Electron.IpcMainInvokeEvent): void {
  if (!isTrustedRendererUrl(event.senderFrame?.url ?? '', trustedFrontendOrigin)) {
    throw new Error('拒绝来自非应用页面的请求')
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit()
} else {
  app.whenReady().then(startApplication).catch((error: unknown) => {
    const message = error instanceof Error ? error.message : '未知错误'
    let logTail = ''
    const logPath = path.join(app.getPath('userData'), 'backend.log')
    try {
      const content = readFileSync(logPath, 'utf-8')
      logTail = content.split('\n').slice(-25).join('\n')
    } catch {
      // No log yet; the backend may have failed before writing anything.
    }
    dialog.showErrorBox(
      'ResumeGo 无法启动',
      `${message}\n\n原始本地数据和启动前备份均未删除。\n\n===== 后端日志（最后 25 行） =====\n${logTail || '（暂无日志）'}\n\n日志文件位置：${logPath}`,
    )
    app.quit()
  })
}

app.on('before-quit', createBeforeQuitHandler(stopChildren, () => app.quit()))
app.on('window-all-closed', () => app.quit())
