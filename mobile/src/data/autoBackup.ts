import { Capacitor } from '@capacitor/core'
import { exportBackup } from './store'

/**
 * 冷启动自动备份：把整库 JSON 每天落一份到 App 专属外部目录（Android/data/<pkg>/files/backup/）。
 *
 * 为什么是 Directory.External：公共目录（Downloads/Documents）在 Android 13+ 因
 * WRITE_EXTERNAL_STORAGE 权限被移除而写不进去（share.ts 里有完整论证），External 是
 * 免权限的应用专属目录，用户用数据线连电脑仍可拷走。
 *
 * 边界必须对用户说清楚（MeView 展示）：卸载或「清除数据」会连它一起清掉，
 * 它防的是 WebView 本地存储损坏/被禁用，**不能替代换机前的手动导出**。
 */

const AUTO_BACKUP_AT_KEY = 'zhida-auto-backup-at'
const AUTO_BACKUP_ERR_KEY = 'zhida-auto-backup-error'
const AUTO_BACKUP_COUNT_KEY = 'zhida-auto-backup-count'
const BACKUP_DIR = 'backup'
const KEEP = 7

/** 当天备份文件名：一天一份、重写覆盖，文件名即日期（字典序=时间序）。 */
export function backupFileNameFor(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `zhida-auto-${y}-${m}-${d}.json`
}

/**
 * 纯函数：目录里现有的备份名 + 今天的名字，算出超出保留数量的该删文件。
 * 返回按日期升序（最旧的排最前，删的顺序即此）。只认 zhida-auto-* 命名，
 * 用户手动放进同目录的任何文件都不碰。
 */
export function staleBackupNames(names: string[], todayName: string, keep = KEEP): string[] {
  const own = names.filter((n) => /^zhida-auto-\d{4}-\d{2}-\d{2}\.json$/.test(n))
  const all = own.includes(todayName) ? own : [...own, todayName]
  const removeCount = all.length - keep
  return removeCount > 0 ? all.sort().slice(0, removeCount) : []
}

function readLS(key: string): string | null {
  try { return localStorage.getItem(key) } catch { return null }
}
function writeLS(key: string, value: string) {
  try { localStorage.setItem(key, value) } catch { /* 状态记录失败不影响备份本身 */ }
}

export interface AutoBackupStatus {
  /** 最近一次成功落盘的时刻（ISO），null = 还没成功过。 */
  at: string | null
  /** 最近一次失败的现场信息；成功后清空。 */
  error: string | null
  /** 目录里保留的份数；未知为 null。 */
  kept: number | null
}

/** Web 预览环境没有文件系统，返回 null 让 UI 整行隐藏，而不是编一个状态。 */
export function autoBackupStatus(): AutoBackupStatus | null {
  if (!Capacitor.isNativePlatform()) return null
  const keptRaw = readLS(AUTO_BACKUP_COUNT_KEY)
  return {
    at: readLS(AUTO_BACKUP_AT_KEY),
    error: readLS(AUTO_BACKUP_ERR_KEY) || null,
    kept: keptRaw && /^\d+$/.test(keptRaw) ? Number(keptRaw) : null,
  }
}

export type AutoBackupResult =
  | { status: 'skipped'; reason: 'web' | 'empty' }
  | { status: 'written'; kept: number }
  | { status: 'failed'; message: string }

/**
 * 冷启动调用。绝不抛错、绝不阻塞启动——备份是守护动作，
 * 失败把现场记下来给「我的」页展示，绝不把失败说成成功。
 */
export async function runAutoBackup(): Promise<AutoBackupResult> {
  if (!Capacitor.isNativePlatform()) return { status: 'skipped', reason: 'web' }
  try {
    const { Filesystem, Directory, Encoding } = await import('@capacitor/filesystem')
    const json = exportBackup()
    const parsed = JSON.parse(json) as { targets?: unknown[]; schedules?: unknown[]; resumes?: unknown[]; interviewLogs?: unknown[] }
    const recordCount = (parsed.targets?.length ?? 0) + (parsed.schedules?.length ?? 0) + (parsed.resumes?.length ?? 0) + (parsed.interviewLogs?.length ?? 0)
    // 空库不落盘：备份一个空 JSON 只会让「保留 7 份」全是空壳，还可能覆盖损坏前的最后一份好数据。
    if (!recordCount) return { status: 'skipped', reason: 'empty' }
    try { await Filesystem.mkdir({ path: BACKUP_DIR, directory: Directory.External, recursive: true }) } catch { /* 已存在 */ }
    const name = backupFileNameFor(new Date())
    await Filesystem.writeFile({
      path: `${BACKUP_DIR}/${name}`,
      data: json,
      directory: Directory.External,
      encoding: Encoding.UTF8,
    })
    const listing = await Filesystem.readdir({ path: BACKUP_DIR, directory: Directory.External })
    const stale = staleBackupNames(listing.files.map((f) => f.name), name)
    for (const old of stale) {
      try { await Filesystem.deleteFile({ path: `${BACKUP_DIR}/${old}`, directory: Directory.External }) } catch { /* 删除失败不影响本轮落盘 */ }
    }
    const kept = Math.max(0, listing.files.length - stale.length)
    writeLS(AUTO_BACKUP_AT_KEY, new Date().toISOString())
    writeLS(AUTO_BACKUP_ERR_KEY, '')
    writeLS(AUTO_BACKUP_COUNT_KEY, String(kept))
    return { status: 'written', kept }
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    writeLS(AUTO_BACKUP_ERR_KEY, message)
    return { status: 'failed', message }
  }
}
