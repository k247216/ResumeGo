import { Capacitor } from '@capacitor/core'
import { LocalNotifications } from '@capacitor/local-notifications'
import type { ScheduleEvent } from '../types/schedule'
import { eventEndsAt } from '../types/schedule'

const pendingTimers = new Map<number, ReturnType<typeof setTimeout>>()

/**
 * 一条提醒的最终去向。调用方必须区分它们：
 * 'scheduled' 才是系统里真正躺着的一条通知，'web' 只在页面活着时有效，不能对用户承诺。
 */
export type ReminderOutcome = 'scheduled' | 'skipped-past' | 'denied' | 'web' | 'failed'

export const REMINDER_OUTCOME_MESSAGES: Record<ReminderOutcome, string> = {
  scheduled: '提醒已排入系统',
  'skipped-past': '提醒时间已过，本次未排',
  denied: '通知权限未开启，提醒无法送达',
  web: '当前为浏览器预览，提醒只在页面打开时有效',
  failed: '系统未接受提醒，请重试',
}

/**
 * 提醒必须走自己的渠道：插件建的 default 渠道写死了 IMPORTANCE_DEFAULT，
 * 而 Android 8+ 只有 IMPORTANCE_HIGH 才会弹出横幅（heads-up）并响铃——
 * 面试提醒安静地躺在下拉栏里，等于没有提醒。
 */
export const REMINDER_CHANNEL = {
  id: 'zhida-reminder',
  name: '日程提醒',
  description: '面试、笔试开始前的准点提醒',
} as const

let channelReady: Promise<boolean> | null = null

/** 渠道只需建一次；Android 8 以下插件回 unavailable，那条路径上系统本来就没有渠道概念。 */
export function ensureReminderChannel(): Promise<boolean> {
  if (!Capacitor.isNativePlatform()) return Promise.resolve(false)
  channelReady ??= LocalNotifications.createChannel({
    id: REMINDER_CHANNEL.id,
    name: REMINDER_CHANNEL.name,
    description: REMINDER_CHANNEL.description,
    importance: 4,
    visibility: 1,
    vibration: true,
    lights: true,
    sound: 'zhida_reminder.wav',
  })
    .then(() => true)
    .catch(() => false)
  return channelReady
}

export async function requestNotificationPermission(): Promise<boolean> {
  try {
    if (Capacitor.isNativePlatform()) {
      const res = await LocalNotifications.requestPermissions()
      return res.display === 'granted'
    }
    if ('Notification' in window) {
      const res = await Notification.requestPermission()
      return res === 'granted'
    }
  } catch { /* 无通知能力时静默降级 */ }
  return false
}

/** 只查询不弹窗：用于提醒诊断展示当前权限状态。 */
export async function notificationPermissionState(): Promise<'granted' | 'denied' | 'unsupported'> {
  try {
    if (Capacitor.isNativePlatform()) {
      const res = await LocalNotifications.checkPermissions()
      return res.display === 'granted' ? 'granted' : 'denied'
    }
    if ('Notification' in window) {
      return Notification.permission === 'granted' ? 'granted' : Notification.permission === 'denied' ? 'denied' : 'unsupported'
    }
  } catch { return 'unsupported' }
  return 'unsupported'
}

/**
 * Android 12+ 没有「精确闹钟」授权时，系统会把提醒推迟到大致时间——面试提醒晚十分钟就等于没提醒。
 */
export async function exactAlarmPermissionState(): Promise<'granted' | 'denied' | 'unsupported'> {
  if (!Capacitor.isNativePlatform()) return 'unsupported'
  try {
    const res = await LocalNotifications.checkExactNotificationSetting()
    return res.exact_alarm === 'granted' ? 'granted' : 'denied'
  } catch { return 'unsupported' }
}

/** 跳转到系统「精确闹钟」设置页让用户授权（Android < 12 直接返回 granted）。 */
export async function requestExactAlarmPermission(): Promise<'granted' | 'denied' | 'unsupported'> {
  if (!Capacitor.isNativePlatform()) return 'unsupported'
  try {
    const res = await LocalNotifications.changeExactNotificationSetting()
    return res.exact_alarm === 'granted' ? 'granted' : 'denied'
  } catch { return 'unsupported' }
}

/** 系统当前真正挂着的待触发通知条数；浏览器环境下无法核实，返回 null。 */
export async function pendingNotificationCount(): Promise<number | null> {
  if (!Capacitor.isNativePlatform()) return null
  try {
    const res = await LocalNotifications.getPending()
    return res.notifications.length
  } catch { return null }
}

/**
 * 系统当前挂着的待触发通知 id 集合——通知 id 就是日程 id，
 * 所以能精确回答「这一场的提醒还在不在系统里」。
 * 浏览器环境无法核实，返回 null，调用方相应隐藏状态戳而不是乱报。
 */
export async function pendingNotificationIds(): Promise<Set<number> | null> {
  if (!Capacitor.isNativePlatform()) return null
  try {
    const res = await LocalNotifications.getPending()
    return new Set(res.notifications.map((n) => n.id))
  } catch { return null }
}

function fireWebNotification(title: string, body: string) {
  try {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, { body })
    }
  } catch { /* 忽略 */ }
}

/** 为日程排一条本地提醒；返回真实落点，绝不把失败说成成功。 */
export async function scheduleReminder(event: ScheduleEvent, minutesBefore: number): Promise<ReminderOutcome> {
  cancelReminder(event.id)
  const start = new Date(event.startTime).getTime()
  if (Number.isNaN(start)) return 'failed'
  const fireAt = start - minutesBefore * 60_000
  if (fireAt <= Date.now()) return 'skipped-past'

  if (Capacitor.isNativePlatform()) {
    if (await notificationPermissionState() !== 'granted') return 'denied'
    const channel = (await ensureReminderChannel()) ? REMINDER_CHANNEL.id : undefined
    try {
      await LocalNotifications.schedule({
        notifications: [{
          id: event.id,
          title: `即将开始：${event.title}`,
          body: minutesBefore > 0 ? `${minutesBefore} 分钟后开始` : '现在开始',
          schedule: { at: new Date(fireAt), allowWhileIdle: true },
          ...(channel ? { channelId: channel } : {}),
        }],
      })
      return 'scheduled'
    } catch {
      // 排期失败时不再悄悄改用前台定时器：那会让用户以为提醒已生效。
      return 'failed'
    }
  }

  const delay = fireAt - Date.now()
  const timer = setTimeout(() => fireWebNotification(`即将开始：${event.title}`, `${event.title} 将在 ${minutesBefore} 分钟后开始`), delay)
  pendingTimers.set(event.id, timer)
  return 'web'
}

export function cancelReminder(eventId: number): void {
  const timer = pendingTimers.get(eventId)
  if (timer) { clearTimeout(timer); pendingTimers.delete(eventId) }
  if (Capacitor.isNativePlatform()) {
    void LocalNotifications.cancel({ notifications: [{ id: eventId }] }).catch(() => undefined)
  }
}

/** 立即触发一条提醒（用于「测试提醒」）。 */
export async function previewReminder(event: ScheduleEvent): Promise<ReminderOutcome> {
  const fireAt = new Date(Date.now() + 3000)
  if (Capacitor.isNativePlatform()) {
    if (await notificationPermissionState() !== 'granted') return 'denied'
    const channel = (await ensureReminderChannel()) ? REMINDER_CHANNEL.id : undefined
    try {
      await LocalNotifications.schedule({
        notifications: [{
          id: event.id + 100000,
          title: `测试提醒：${event.title}`,
          body: '如果你看到这条，提醒链路是通的',
          schedule: { at: fireAt, allowWhileIdle: true },
          ...(channel ? { channelId: channel } : {}),
        }],
      })
      return 'scheduled'
    } catch { return 'failed' }
  }
  fireWebNotification(`测试提醒：${event.title}`, '如果你看到这条，提醒链路是通的')
  return 'web'
}

/**
 * 取消系统里所有已排入的提醒，返回真正撤掉的条数。
 *
 * 「清空本机记录」和「恢复备份」都必须先跑这一遍：库里的意图被清空或整份替换之后，
 * 系统里那批通知不会自己消失，会在预定时间弹出来，指向一条已经不存在的日程。
 * 而 armAllReminders 只负责把新库里的提醒排进去，管不到新库里已经没有的旧通知。
 */
export async function cancelAllReminders(): Promise<number> {
  if (!Capacitor.isNativePlatform()) return 0
  try {
    const pending = await LocalNotifications.getPending()
    const ids = pending.notifications.map((item) => item.id)
    if (!ids.length) return 0
    await LocalNotifications.cancel({ notifications: ids.map((id) => ({ id })) })
    return ids.length
  } catch {
    // 拿不到待触发列表时不能假装成功：调用方据此决定要不要告诉用户「已取消 N 条」。
    return 0
  }
}

// ── 面后复盘提醒：提醒链的最后一环——面后 2 小时推一把「该写复盘了」，写完即撤 ──

/** 复盘提醒的独立通知 id 段：日程 id + 200000（测试提醒占 100000 段），互不踩。 */
export const NUDGE_ID_OFFSET = 200_000

const NUDGE_KEY = 'zhida-review-nudge'
/** 复盘提醒开关，默认开。记在本机不进库：换机恢复的是数据，习惯设置留在设备上。 */
export function reviewNudgeEnabled(): boolean {
  try { return localStorage.getItem(NUDGE_KEY) !== 'off' } catch { return true }
}
export function setReviewNudgeEnabled(on: boolean) {
  try { localStorage.setItem(NUDGE_KEY, on ? 'on' : 'off') } catch { /* 存不了就按默认开 */ }
}

/** 撤掉某场日程的面后复盘提醒（写完心得、删日程时都要撤）。 */
export function cancelReviewNudge(eventId: number): void {
  const nudgeId = eventId + NUDGE_ID_OFFSET
  const timer = pendingTimers.get(nudgeId)
  if (timer) { clearTimeout(timer); pendingTimers.delete(nudgeId) }
  if (Capacitor.isNativePlatform()) {
    void LocalNotifications.cancel({ notifications: [{ id: nudgeId }] }).catch(() => undefined)
  }
}

/**
 * 给一场已排期的笔试/面试挂「面后写复盘」提醒：结束后 2 小时触发。
 * 与赛前提醒共用渠道；结束时刻按 endTime 算，没填则按开始 + 1 小时估。
 * 调用方负责确认：开关已开、事件在未来、还没写过心得。
 */
export async function scheduleReviewNudge(event: ScheduleEvent): Promise<ReminderOutcome> {
  cancelReviewNudge(event.id)
  const fireAt = eventEndsAt(event) + 2 * 3600_000
  if (fireAt <= Date.now()) return 'skipped-past'

  if (Capacitor.isNativePlatform()) {
    if (await notificationPermissionState() !== 'granted') return 'denied'
    const channel = (await ensureReminderChannel()) ? REMINDER_CHANNEL.id : undefined
    try {
      await LocalNotifications.schedule({
        notifications: [{
          id: event.id + NUDGE_ID_OFFSET,
          title: '该写复盘了',
          body: `${event.title} 已结束，趁记忆新鲜记两句`,
          schedule: { at: new Date(fireAt), allowWhileIdle: true },
          ...(channel ? { channelId: channel } : {}),
        }],
      })
      return 'scheduled'
    } catch {
      return 'failed'
    }
  }

  const timer = setTimeout(() => fireWebNotification('该写复盘了', `${event.title} 已结束，趁记忆新鲜记两句`), fireAt - Date.now())
  pendingTimers.set(event.id + NUDGE_ID_OFFSET, timer)
  return 'web'
}
