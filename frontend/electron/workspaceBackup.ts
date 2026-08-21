import { access, cp, mkdir, readdir, rename, rm, stat } from 'node:fs/promises'
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


export interface WorkspaceBackupInfo {
  id: string
  createdAt: string
  sizeBytes: number
}

const BACKUP_NAME_PATTERN = /^\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}-\d{3}Z$/

async function listBackupDirs(dataDir: string): Promise<string[]> {
  const backupRoot = path.join(dataDir, 'backups')
  if (!(await exists(backupRoot))) return []
  const entries = (await readdir(backupRoot, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory() && BACKUP_NAME_PATTERN.test(entry.name))
    .map((entry) => entry.name)
    .sort()
    .reverse()
  return entries
}

export async function listWorkspaceBackups(dataDir: string): Promise<WorkspaceBackupInfo[]> {
  const names = await listBackupDirs(dataDir)
  const infos: WorkspaceBackupInfo[] = []
  for (const name of names) {
    const backupDir = path.join(dataDir, 'backups', name)
    try {
      const info = await stat(path.join(backupDir, 'resumego.mv.db'))
      infos.push({
        id: name,
        createdAt: name.replace(/^(\d{4}-\d{2}-\d{2})T(\d{2})-(\d{2})-(\d{2})-(\d{3})Z$/, '$1T$2:$3:$4.$5Z'),
        sizeBytes: info.size,
      })
    } catch {
      // A backup missing its database file is treated as invalid and skipped.
    }
  }
  return infos
}

export async function restoreWorkspaceBackup(
  dataDir: string,
  backupId: string,
): Promise<{ restored: boolean; backupDir: string }> {
  if (!BACKUP_NAME_PATTERN.test(backupId)) {
    throw new Error('无效的备份标识')
  }
  const databaseFile = path.join(dataDir, 'resumego.mv.db')
  if (!(await exists(databaseFile))) {
    throw new Error('当前工作区没有可恢复的数据')
  }
  const backupDir = path.join(dataDir, 'backups', backupId)
  const backupDb = path.join(backupDir, 'resumego.mv.db')
  if (!(await exists(backupDb))) {
    throw new Error('备份数据不完整')
  }
  const crashDir = path.join(
    dataDir,
    'crash-' + new Date().toISOString().replaceAll(':', '-').replace('.', '-'),
  )
  await mkdir(crashDir, { recursive: true })
  await rename(databaseFile, path.join(crashDir, 'resumego.mv.db'))
  await cp(backupDb, databaseFile)
  return { restored: true, backupDir }
}

export async function exportWorkspaceBackup(
  dataDir: string,
  backupId: string | null,
  targetDir: string,
): Promise<{ exportedTo: string }> {
  const sourceDir = backupId && BACKUP_NAME_PATTERN.test(backupId)
    ? path.join(dataDir, 'backups', backupId)
    : dataDir
  const stamp = new Date().toISOString().replaceAll(':', '-').replace('.', '-')
  const target = path.join(targetDir, `resumego-workspace-${stamp}`)
  await mkdir(target, { recursive: true })
  if (backupId) {
    await cp(sourceDir, target, { recursive: true })
  } else {
    await cp(path.join(dataDir, 'resumego.mv.db'), path.join(target, 'resumego.mv.db'))
    const attachments = path.join(dataDir, 'attachments')
    if (await exists(attachments)) {
      await cp(attachments, path.join(target, 'attachments'), { recursive: true })
    }
  }
  return { exportedTo: target }
}
