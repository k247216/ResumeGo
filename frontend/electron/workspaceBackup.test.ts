// @vitest-environment node

import { mkdtemp, mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import { createColdWorkspaceBackup } from './workspaceBackup.js'

describe('createColdWorkspaceBackup', () => {
  it('copies the database and attachments into a separate backup directory', async () => {
    const root = await mkdtemp(path.join(os.tmpdir(), 'resumego-backup-'))
    const dataDir = path.join(root, 'workspace')
    await mkdir(path.join(dataDir, 'attachments'), { recursive: true })
    await writeFile(path.join(dataDir, 'resumego.mv.db'), 'database-v1')
    await writeFile(path.join(dataDir, 'attachments', 'resume.md'), '# Resume')

    const result = await createColdWorkspaceBackup(dataDir, new Date('2026-08-19T10:20:30.000Z'))

    expect(result?.backupDir).toContain(path.join('backups', '2026-08-19T10-20-30-000Z'))
    expect(await readFile(path.join(result!.backupDir, 'resumego.mv.db'), 'utf8')).toBe('database-v1')
    expect(await readFile(path.join(result!.backupDir, 'attachments', 'resume.md'), 'utf8')).toBe('# Resume')
  })

  it('does nothing before the first workspace database exists', async () => {
    const dataDir = await mkdtemp(path.join(os.tmpdir(), 'resumego-empty-'))
    await expect(createColdWorkspaceBackup(dataDir)).resolves.toBeNull()
  })

  it('retains only the five newest cold backups', async () => {
    const root = await mkdtemp(path.join(os.tmpdir(), 'resumego-retention-'))
    await writeFile(path.join(root, 'resumego.mv.db'), 'database')
    for (let day = 1; day <= 6; day += 1) {
      await mkdir(path.join(root, 'backups', `2026-08-0${day}T00-00-00-000Z`), { recursive: true })
    }

    await createColdWorkspaceBackup(root, new Date('2026-08-19T00:00:00.000Z'))

    expect(await readdir(path.join(root, 'backups'))).toHaveLength(5)
  })
})
