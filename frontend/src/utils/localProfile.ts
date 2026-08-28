export type UsageSource = 'local' | 'demo'

export interface LocalProfile {
  name: string
  /** 默认头像是短文本；上传头像会保存为本地 data URL。 */
  avatar: string
  createdAt: string
  lastUsedAt: string
  /** 兼容旧版本的本地使用日期列表。 */
  usageDates: string[]
  /** 每个自然日累计的本地使用分钟数。 */
  usageMinutesByDate: Record<string, number>
  /** 桌面应用启动次数；网页端不会展示，但仍与本地资料一起兼容保存。 */
  launchCount: number
}

export interface DailyUsage {
  key: string
  minutes: number
  isDemo: boolean
  hasLocalRecord: boolean
  level: 0 | 1 | 2 | 3 | 4
}

export interface MonthUsageCell extends DailyUsage {
  day: number
  isCurrentMonth: boolean
}

const STORAGE_KEY = 'resumego:local-profile'
const DEFAULT_AVATAR = '林'

/** 用本地日历计算日期，避免 toISOString 在午夜附近造成日期偏移。 */
export function formatDateKey(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function createDefaultProfile(now = new Date()): LocalProfile {
  return {
    name: '本地用户',
    avatar: DEFAULT_AVATAR,
    createdAt: now.toISOString(),
    lastUsedAt: now.toISOString(),
    usageDates: [],
    usageMinutesByDate: {},
    launchCount: 0,
  }
}

function isUsageMinutes(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

export function readLocalProfile(): LocalProfile {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return createDefaultProfile()
    const parsed = JSON.parse(raw) as Partial<LocalProfile>
    const fallback = createDefaultProfile()
    const usageMinutesByDate = parsed.usageMinutesByDate && typeof parsed.usageMinutesByDate === 'object'
      ? Object.fromEntries(
        Object.entries(parsed.usageMinutesByDate)
          .filter(([, value]) => isUsageMinutes(value))
          .map(([key, value]) => [key, Math.round(value as number)]),
      )
      : fallback.usageMinutesByDate
    return {
      name: typeof parsed.name === 'string' && parsed.name.trim() ? parsed.name.trim() : fallback.name,
      avatar: typeof parsed.avatar === 'string' && parsed.avatar.trim() ? parsed.avatar.trim() : fallback.avatar,
      createdAt: typeof parsed.createdAt === 'string' ? parsed.createdAt : fallback.createdAt,
      lastUsedAt: typeof parsed.lastUsedAt === 'string' ? parsed.lastUsedAt : fallback.lastUsedAt,
      usageDates: Array.isArray(parsed.usageDates)
        ? Array.from(new Set(parsed.usageDates.filter((value): value is string => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value))))
        : fallback.usageDates,
      usageMinutesByDate,
      launchCount: typeof parsed.launchCount === 'number' && Number.isFinite(parsed.launchCount) && parsed.launchCount >= 0
        ? Math.round(parsed.launchCount)
        : fallback.launchCount,
    }
  } catch {
    return createDefaultProfile()
  }
}

export function saveLocalProfile(profile: LocalProfile): LocalProfile {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(profile)) } catch { /* local-only storage may be unavailable */ }
  return profile
}

/** 每次打开应用记录一个真实本地使用分钟；不记录页面内容或行为明细。 */
export function touchLocalProfile(now = new Date()): LocalProfile {
  const profile = recordLocalUsage(1, now)
  return saveLocalProfile({ ...profile, launchCount: profile.launchCount + 1 })
}

/** 将本次会话经过的分钟数累加到本地自然日。 */
export function recordLocalUsage(minutes = 1, now = new Date()): LocalProfile {
  const profile = readLocalProfile()
  const key = formatDateKey(now)
  const increment = Math.max(1, Math.round(minutes))
  const usageDates = Array.from(new Set([...profile.usageDates, key])).sort()
  const usageMinutesByDate = {
    ...profile.usageMinutesByDate,
    [key]: (profile.usageMinutesByDate[key] ?? 0) + increment,
  }
  return saveLocalProfile({ ...profile, lastUsedAt: now.toISOString(), usageDates, usageMinutesByDate })
}

export function updateLocalProfile(patch: Partial<Pick<LocalProfile, 'name' | 'avatar'>>): LocalProfile {
  const profile = readLocalProfile()
  return saveLocalProfile({
    ...profile,
    name: patch.name?.trim() || profile.name,
    avatar: patch.avatar?.trim() || profile.avatar,
  })
}

/** 没有真实使用记录的日期仅用于让空状态热力图可预览，界面会明确标注为演示记录。 */
function demoMinutesForDate(key: string) {
  const day = Number(key.slice(-2))
  const month = Number(key.slice(5, 7))
  if (!Number.isFinite(day) || !Number.isFinite(month)) return 0
  if ((day + month) % 17 === 0) return 75
  if ((day + month) % 11 === 0) return 45
  if ((day + month) % 7 === 0) return 25
  return 0
}

function usageLevel(minutes: number): DailyUsage['level'] {
  if (minutes >= 60) return 4
  if (minutes >= 45) return 3
  if (minutes >= 25) return 2
  if (minutes > 0) return 1
  return 0
}

export function getDailyUsage(profile: LocalProfile, key: string): DailyUsage {
  const storedMinutes = profile.usageMinutesByDate[key]
  const hasLocalRecord = profile.usageDates.includes(key) || (isUsageMinutes(storedMinutes) && storedMinutes > 0)
  const minutes = hasLocalRecord ? Math.max(0, storedMinutes ?? 1) : demoMinutesForDate(key)
  return { key, minutes, isDemo: !hasLocalRecord && minutes > 0, hasLocalRecord, level: usageLevel(minutes) }
}

/** 生成按周一开始的月份日历；首尾的非当月格子用于保持网格对齐。 */
export function buildMonthUsageCells(year: number, monthIndex: number, profile: LocalProfile): MonthUsageCell[] {
  const firstDay = new Date(year, monthIndex, 1)
  const offset = (firstDay.getDay() + 6) % 7
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const cellCount = Math.ceil((offset + daysInMonth) / 7) * 7
  return Array.from({ length: cellCount }, (_, index) => {
    const date = new Date(year, monthIndex, index - offset + 1)
    const key = formatDateKey(date)
    const usage = getDailyUsage(profile, key)
    return { ...usage, day: date.getDate(), isCurrentMonth: date.getMonth() === monthIndex }
  })
}

export function formatUsageDuration(minutes: number) {
  if (!minutes) return '未记录'
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (!hours) return `${rest} 分钟`
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}

export function localProfileStorageKey() {
  return STORAGE_KEY
}
