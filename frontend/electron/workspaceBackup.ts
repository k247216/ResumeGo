import { access, cp, mkdir, readdir, rm } from 'node:fs/promises'
import path from 'node:path'

export interface WorkspaceBackupResult {
  backupDir: string
}

function timestamp(date: Date): string {
  return date.toISOString().replaceAll(':', '-').replace('.', '-')
}

async function exists(filePath: string): Promise<boolean> {
  try {
    await access(filePath)
    return true
  } catch {
    return false
  }
}

export async function createColdWorkspaceBackup(
  dataDir: string,
  now = new Date(),
): Promise<WorkspaceBackupResult | null> {
  const databaseFile = path.join(dataDir, 'resumego.mv.db')
  if (!(await exists(databaseFile))) {
    return null
  }

  const backupDir = path.join(dataDir, 'backups', timestamp(now))
  await mkdir(backupDir, { recursive: true })
  await cp(databaseFile, path.join(backupDir, 'resumego.mv.db'))

  const attachments = path.join(dataDir, 'attachments')
  if (await exists(attachments)) {
    await cp(attachments, path.join(backupDir, 'attachments'), { recursive: true })
  }

  const backupRoot = path.join(dataDir, 'backups')
  const backups = (await readdir(backupRoot, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory() && /^\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}-\d{3}Z$/.test(entry.name))
    .map((entry) => entry.name)
    .sort()
    .reverse()
  for (const expired of backups.slice(5)) {
    await rm(path.join(backupRoot, expired), { recursive: true })
  }

  return { backupDir }
}
