import { Capacitor } from '@capacitor/core'
import { LocalNotifications } from '@capacitor/local-notifications'
import type { ScheduleEvent } from '../types/schedule'

const pendingTimers = new Map<number, ReturnType<typeof setTimeout>>()

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

function fireWebNotification(title: string, body: string) {
  try {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, { body })
    }
  } catch { /* 忽略 */ }
}

/** 为日程排一条本地提醒；原生走系统通知，浏览器会话内用定时器降级。 */
export async function scheduleReminder(event: ScheduleEvent, minutesBefore: number): Promise<void> {
  cancelReminder(event.id)
  const start = new Date(event.startTime).getTime()
  const fireAt = start - minutesBefore * 60_000
  if (Number.isNaN(start) || fireAt <= Date.now()) return

  if (Capacitor.isNativePlatform()) {
    try {
      await LocalNotifications.schedule({
        notifications: [{
          id: event.id,
          title: `即将开始：${event.title}`,
          body: minutesBefore > 0 ? `${minutesBefore} 分钟后开始` : '现在开始',
          schedule: { at: new Date(fireAt), allowWhileIdle: true },
        }],
      })
      return
    } catch { /* 降级到 Web */ }
  }
  const delay = fireAt - Date.now()
  const timer = setTimeout(() => fireWebNotification(`即将开始：${event.title}`, `${event.title} 将在 ${minutesBefore} 分钟后开始`), delay)
  pendingTimers.set(event.id, timer)
}

export function cancelReminder(eventId: number): void {
  const timer = pendingTimers.get(eventId)
  if (timer) { clearTimeout(timer); pendingTimers.delete(eventId) }
  if (Capacitor.isNativePlatform()) {
    void LocalNotifications.cancel({ notifications: [{ id: eventId }] }).catch(() => undefined)
  }
}

/** 立即触发一条提醒（用于演示 / 测试）。 */
export function previewReminder(event: ScheduleEvent): void {
  if (Capacitor.isNativePlatform()) {
    void LocalNotifications.schedule({
      notifications: [{ id: event.id + 100000, title: `即将开始：${event.title}`, body: '提醒预览', schedule: { at: new Date(Date.now() + 2000) } }],
    }).catch(() => fireWebNotification(`即将开始：${event.title}`, '提醒预览'))
  } else {
    fireWebNotification(`即将开始：${event.title}`, '提醒预览')
  }
}
