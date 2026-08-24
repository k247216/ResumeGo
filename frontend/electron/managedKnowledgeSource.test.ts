// @vitest-environment node

import { describe, expect, it, vi } from 'vitest'
import {
  isValidDocumentId,
  MANAGED_SOURCE_CODES,
  openManagedKnowledgeSource,
  resolveManagedSourcePath,
} from './managedKnowledgeSource'
import type { ManagedSourceFs } from './managedKnowledgeSource'
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'

async function tempWorkspace(): Promise<string> {
  const dir = await mkdtemp(path.join(tmpdir(), 'io02-'))
  const target = path.join(dir, 'knowledge', 'sources', '1', 'x.md')
  await mkdir(path.dirname(target), { recursive: true })
  await writeFile(target, 'content')
  return dir
}

const realFs: ManagedSourceFs = {
  realpath: (p: string) => Promise.resolve(p),
  stat: async (p: string) => ({ isFile: () => true }),
  lstat: async () => ({ isSymbolicLink: () => false }),
}

describe('managed knowledge source', () => {
  it('rejects non-positive document ids before any backend call', async () => {
    for (const bad of [0, -1, 1.5, '7', {}, null, undefined]) {
      const result = await openManagedKnowledgeSource(bad, 'open', {
        backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir: '/d',
        shell: { openPath: vi.fn().mockResolvedValue(''), showItemInFolder: vi.fn() },
      })
      expect(result).toEqual({ ok: false, code: MANAGED_SOURCE_CODES.INVALID_DOCUMENT_ID, message: '文档标识无效' })
    }
  })

  it('rejects traversal, absolute, directory, and out-of-root real paths', async () => {
    const base = '/data/workspace'
    const results = await Promise.all([
      resolveManagedSourcePath(base, '../secret.md', realFs),
      resolveManagedSourcePath(base, '/etc/passwd', realFs),
      resolveManagedSourcePath(base, 'knowledge/other/1/x.md', realFs),
      resolveManagedSourcePath(base, 'knowledge/sources', { ...realFs, stat: async () => ({ isFile: () => false }) }),
    ])
    for (const r of results) {
      expect(r.ok).toBe(false)
      if (!r.ok) expect(r.code).toBe(MANAGED_SOURCE_CODES.INVALID_PATH)
    }
  })

  it('rejects symlink escape via realpath outside the sources root', async () => {
    const base = '/data/workspace'
    const symlinkFs: ManagedSourceFs = {
      stat: async () => ({ isFile: () => true }),
      lstat: async () => ({ isSymbolicLink: () => false }),
      realpath: async (p: string) => (p.endsWith('x.md') ? '/outside/real.md' : p),
    }
    const result = await resolveManagedSourcePath(base, 'knowledge/sources/1/x.md', symlinkFs)
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.code).toBe(MANAGED_SOURCE_CODES.INVALID_PATH)
  })

  it('rejects a final symlink even when it resolves inside the managed root', async () => {
    const base = '/data/workspace'
    const symlinkFs: ManagedSourceFs = {
      stat: async () => ({ isFile: () => true }),
      lstat: async () => ({ isSymbolicLink: () => true }),
      realpath: async (p: string) => p,
    }
    const result = await resolveManagedSourcePath(base, 'knowledge/sources/1/x.md', symlinkFs)
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.code).toBe(MANAGED_SOURCE_CODES.INVALID_PATH)
  })

  it('accepts an in-root regular file and returns its real path', async () => {
    const base = '/data/workspace'
    const result = await resolveManagedSourcePath(base, 'knowledge/sources/1/x.md', realFs)
    expect(result.ok).toBe(true)
    if (result.ok) expect(result.absolute).toContain('knowledge/sources/1/x.md')
  })

  it('maps backend stable failure codes and never leaks paths', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: false, data: null, message: 'SOURCE_MISSING' }),
      { status: 404, headers: { 'Content-Type': 'application/json' } },
    ))
    const shell = { openPath: vi.fn(), showItemInFolder: vi.fn() }
    const result = await openManagedKnowledgeSource(7, 'open', {
      backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir: '/d',
      shell, fetchImpl,
    })
    expect(result).toEqual({ ok: false, code: 'SOURCE_MISSING', message: '无法打开受管原文' })
    expect(shell.openPath).not.toHaveBeenCalled()
  })

  it('does not return an unexpected backend error message to the renderer', async () => {
    const leakedPath = '/Users/example/private/secret.md'
    const fetchImpl = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: false, data: null, message: leakedPath }),
      { status: 500, headers: { 'Content-Type': 'application/json' } },
    ))
    const result = await openManagedKnowledgeSource(7, 'open', {
      backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir: '/d',
      shell: { openPath: vi.fn(), showItemInFolder: vi.fn() }, fetchImpl,
    })
    expect(result).toEqual({ ok: false, code: 'SOURCE_NOT_FOUND', message: '无法打开受管原文' })
    expect(JSON.stringify(result)).not.toContain(leakedPath)
  })

  it('opens the resolved file via shell.openPath and normalizes system errors', async () => {
    const dataDir = await tempWorkspace()
    const fetchImpl = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: true, data: { relativePath: 'knowledge/sources/1/x.md' }, message: null }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    const shell = { openPath: vi.fn().mockResolvedValue('系统错误: ' + dataDir + '/knowledge/sources/1/x.md'), showItemInFolder: vi.fn() }
    const result = await openManagedKnowledgeSource(7, 'open', {
      backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir,
      shell, fetchImpl,
    })
    expect(result).toEqual({ ok: false, code: MANAGED_SOURCE_CODES.OPEN_FAILED, message: '无法打开文件' })
    // 系统错误中的路径绝不进入响应
    expect(JSON.stringify(result)).not.toContain(dataDir)
    expect(shell.openPath).toHaveBeenCalledTimes(1)
  })

  it('reveals via showItemInFolder on success', async () => {
    const dataDir = await tempWorkspace()
    const fetchImpl = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: true, data: { relativePath: 'knowledge/sources/1/x.md' }, message: null }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    const shell = { openPath: vi.fn(), showItemInFolder: vi.fn() }
    const result = await openManagedKnowledgeSource(7, 'reveal', {
      backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir,
      shell, fetchImpl,
    })
    expect(result).toEqual({ ok: true })
    expect(shell.showItemInFolder).toHaveBeenCalledTimes(1)
  })

  it('returns backend unavailable on fetch failure', async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new Error('ECONNREFUSED'))
    const result = await openManagedKnowledgeSource(7, 'open', {
      backendOrigin: 'http://x', workspaceToken: 'w', internalToken: 'i', dataDir: '/d',
      shell: { openPath: vi.fn(), showItemInFolder: vi.fn() }, fetchImpl,
    })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.code).toBe(MANAGED_SOURCE_CODES.BACKEND_UNAVAILABLE)
  })
})
