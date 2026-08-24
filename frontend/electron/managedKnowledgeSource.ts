import { lstat, realpath, stat } from 'node:fs/promises'
import path from 'node:path'

/** 受管原文操作的稳定失败分类；不得包含本地路径或内部 token。 */
export const MANAGED_SOURCE_CODES = {
  INVALID_DOCUMENT_ID: 'INVALID_DOCUMENT_ID',
  INVALID_PATH: 'INVALID_PATH',
  SOURCE_NOT_FOUND: 'SOURCE_NOT_FOUND',
  SOURCE_NOT_FILE: 'SOURCE_NOT_FILE',
  SOURCE_NOT_AVAILABLE: 'SOURCE_NOT_AVAILABLE',
  SOURCE_MISSING: 'SOURCE_MISSING',
  BACKEND_UNAVAILABLE: 'BACKEND_UNAVAILABLE',
  OPEN_FAILED: 'OPEN_FAILED',
  REVEAL_FAILED: 'REVEAL_FAILED',
} as const

export type ManagedSourceResult =
  | { ok: true }
  | { ok: false; code: string; message: string }

export interface ManagedSourceFs {
  realpath(p: string): Promise<string>
  stat(p: string): Promise<{ isFile(): boolean }>
  lstat(p: string): Promise<{ isSymbolicLink(): boolean }>
}

/** 纯校验：只接受正整数 documentId。 */
export function isValidDocumentId(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}

function fail(code: string, message: string): { ok: false; code: string; message: string } {
  return { ok: false, code, message }
}

/**
 * 纯函数：把后端返回的相对路径解析到 dataDir/knowledge/sources 下。
 * 拒绝 traversal、绝对路径越界、目录与符号链接逃逸（realpath 后再校验真实路径仍在根内）。
 */
export async function resolveManagedSourcePath(
  dataDir: string,
  relativePath: string,
  fsImpl: ManagedSourceFs = { lstat, realpath, stat },
): Promise<{ ok: true; absolute: string } | { ok: false; code: string; message: string }> {
  if (typeof relativePath !== 'string' || relativePath.trim() === '') {
    return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '受管路径无效')
  }
  const sourcesRoot = path.resolve(dataDir, 'knowledge', 'sources')
  const candidate = path.resolve(dataDir, relativePath)
  // 候选必须在 sources 根目录内（含用户子目录），拒绝 ../ 或绝对路径逃逸
  if (candidate !== sourcesRoot && !candidate.startsWith(sourcesRoot + path.sep)) {
    return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '受管路径越界')
  }
  try {
    const linkInfo = await fsImpl.lstat(candidate)
    if (linkInfo.isSymbolicLink()) {
      return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '受管目标不能是符号链接')
    }
    const info = await fsImpl.stat(candidate)
    if (!info.isFile()) {
      return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '受管目标不是普通文件')
    }
    const real = await fsImpl.realpath(candidate)
    const realRoot = await fsImpl.realpath(sourcesRoot)
    if (real !== realRoot && !real.startsWith(realRoot + path.sep)) {
      return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '真实路径越界')
    }
    return { ok: true, absolute: real }
  } catch {
    return fail(MANAGED_SOURCE_CODES.INVALID_PATH, '受管文件不可用')
  }
}

export interface ManagedSourceContext {
  backendOrigin: string
  workspaceToken: string
  internalToken: string
  dataDir: string
  shell: {
    openPath(p: string): Promise<string>
    showItemInFolder(p: string): void
  }
  fetchImpl?: typeof fetch
}

/**
 * main 进程组合：校验 documentId → 用 workspace + internal token 调内部端点 →
 * realpath 边界校验 → shell.openPath / showItemInFolder。
 * 响应与日志不包含本地路径、hash 或内部 token。
 */
export async function openManagedKnowledgeSource(
  documentId: unknown,
  action: 'open' | 'reveal',
  ctx: ManagedSourceContext,
): Promise<ManagedSourceResult> {
  if (!isValidDocumentId(documentId)) {
    return { ok: false, code: MANAGED_SOURCE_CODES.INVALID_DOCUMENT_ID, message: '文档标识无效' }
  }
  let response: Response
  try {
    const headers = new Headers()
    headers.set('X-Workspace-Token', ctx.workspaceToken)
    headers.set('X-ResumeGo-Internal', ctx.internalToken)
    response = await (ctx.fetchImpl ?? fetch)(
      `${ctx.backendOrigin}/api/v2/internal/knowledge/documents/${documentId}/managed-source`,
      { headers },
    )
  } catch {
    return { ok: false, code: MANAGED_SOURCE_CODES.BACKEND_UNAVAILABLE, message: '本地服务不可用' }
  }
  let body: { success: boolean; data?: { relativePath?: string }; message?: string | null }
  try {
    body = await response.json() as typeof body
  } catch {
    return { ok: false, code: MANAGED_SOURCE_CODES.SOURCE_NOT_FOUND, message: '无法取得受管原文' }
  }
  if (response.status === 403) {
    return { ok: false, code: MANAGED_SOURCE_CODES.SOURCE_NOT_FOUND, message: '无法取得受管原文' }
  }
  if (!response.ok || !body.success) {
    const allowedBackendCodes = new Set<string>([
      MANAGED_SOURCE_CODES.SOURCE_NOT_FOUND,
      MANAGED_SOURCE_CODES.SOURCE_NOT_FILE,
      MANAGED_SOURCE_CODES.SOURCE_NOT_AVAILABLE,
      MANAGED_SOURCE_CODES.SOURCE_MISSING,
    ])
    const code = typeof body.message === 'string' && allowedBackendCodes.has(body.message)
      ? body.message
      : MANAGED_SOURCE_CODES.SOURCE_NOT_FOUND
    return { ok: false, code, message: '无法打开受管原文' }
  }
  const resolved = await resolveManagedSourcePath(ctx.dataDir, body.data?.relativePath ?? '')
  if (!resolved.ok) {
    return resolved
  }
  try {
    if (action === 'open') {
      const error = await ctx.shell.openPath(resolved.absolute)
      if (error) {
        return { ok: false, code: MANAGED_SOURCE_CODES.OPEN_FAILED, message: '无法打开文件' }
      }
      return { ok: true }
    }
    ctx.shell.showItemInFolder(resolved.absolute)
    return { ok: true }
  } catch {
    return {
      ok: false,
      code: action === 'open' ? MANAGED_SOURCE_CODES.OPEN_FAILED : MANAGED_SOURCE_CODES.REVEAL_FAILED,
      message: '无法打开文件',
    }
  }
}
