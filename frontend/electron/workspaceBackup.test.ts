import { mkdtemp, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  createColdWorkspaceBackup,
  exportWorkspaceBackup,
  listWorkspaceBackups,
  restoreWorkspaceBackup,
} from './workspaceBackup.js'

describe('workspaceBackup', () => {
  let dataDir = ''

  beforeEach(async () => {
    dataDir = await mkdtemp(path.join(tmpdir(), 'resumego-backup-test-'))
    await writeFile(path.join(dataDir, 'resumego.mv.db'), 'current-db-content')
  })

  afterEach(async () => {
    await rm(dataDir, { recursive: true, force: true })
  })

  it('creates a cold backup directory with a timestamp name', async () => {
    const result = await createColdWorkspaceBackup(dataDir, new Date('2026-08-21T10:00:00.000Z'))
    expect(result).not.toBeNull()
    expect(result!.backupDir).toContain('backups')
    const backups = await readdir(path.join(dataDir, 'backups'))
    expect(backups).toHaveLength(1)
  })

  it('keeps at most 5 backups and prunes older ones', async () => {
    for (let i = 0; i < 7; i++) {
      await createColdWorkspaceBackup(dataDir, new Date(2026, 7, 21, 10, 0, i))
    }
    const backups = await readdir(path.join(dataDir, 'backups'))
    expect(backups).toHaveLength(5)
  })

  it('lists backups with size metadata', async () => {
    await createColdWorkspaceBackup(dataDir, new Date('2026-08-21T10:00:00.000Z'))
    const infos = await listWorkspaceBackups(dataDir)
    expect(infos).toHaveLength(1)
    expect(infos[0].sizeBytes).toBeGreaterThan(0)
    expect(infos[0].createdAt).toBeTruthy()
  })

  it('restores a backup into a fresh crash directory', async () => {
    await createColdWorkspaceBackup(dataDir, new Date('2026-08-21T10:00:00.000Z'))
    const infos = await listWorkspaceBackups(dataDir)
    expect(infos).toHaveLength(1)
    const result = await restoreWorkspaceBackup(dataDir, infos[0].id)
    expect(result.restored).toBe(true)
    // crash dir should exist with the previous db
    const dirs = (await readdir(dataDir)).filter((name) => name.startsWith('crash-'))
    expect(dirs).toHaveLength(1)
  })

  it('exports the current workspace to a target directory', async () => {
    const target = await mkdtemp(path.join(tmpdir(), 'resumego-backup-target-'))
    try {
      const result = await exportWorkspaceBackup(dataDir, null, target)
      expect(result.exportedTo).toBeTruthy()
      const files = await readdir(result.exportedTo)
      expect(files).toContain('resumego.mv.db')
    } finally {
      await rm(target, { recursive: true, force: true })
    }
  })

  it('rejects an invalid backup id on restore', async () => {
    await expect(restoreWorkspaceBackup(dataDir, '..%2Fevil')).rejects.toThrow('无效的备份标识')
  })
})
