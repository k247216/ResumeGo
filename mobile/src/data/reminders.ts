import { eventsWithReminders } from './store'
import {
  exactAlarmPermissionState, notificationPermissionState, pendingNotificationCount,
  scheduleReminder, type ReminderOutcome,
} from './notifications'

/** 一次批量重排的落点统计。scheduled 才是真的躺在系统通知里的条数。 */
export interface ReminderReport {
  scheduled: number
  skippedPast: number
  denied: number
  web: number
  failed: number
  total: number
}

let lastReport: ReminderReport | null = null

export function lastReminderReport(): ReminderReport | null { return lastReport }

function emptyReport(total: number): ReminderReport {
  return { scheduled: 0, skippedPast: 0, denied: 0, web: 0, failed: 0, total }
}

function count(report: ReminderReport, outcome: ReminderOutcome) {
  if (outcome === 'scheduled') report.scheduled += 1
  else if (outcome === 'skipped-past') report.skippedPast += 1
  else if (outcome === 'denied') report.denied += 1
  else if (outcome === 'web') report.web += 1
  else report.failed += 1
}

/**
 * 把库里所有「已设置提醒」的日程重新排进系统。
 * 系统通知在恢复备份、清理数据、部分 ROM 重启后会丢，所以 reminders 才是真相源，
 * 每次冷启动和每次恢复备份都要跑一遍，而不是只在用户拨过提醒选择器时排一次。
 */
export async function armAllReminders(): Promise<ReminderReport> {
  const items = eventsWithReminders()
  const report = emptyReport(items.length)
  for (const { event, minutes } of items) count(report, await scheduleReminder(event, minutes))
  lastReport = report
  return report
}

export interface ReminderDiagnostics {
  permission: 'granted' | 'denied' | 'unsupported'
  exactAlarm: 'granted' | 'denied' | 'unsupported'
  /** 库里设置了提醒的日程条数（用户的真实意图）。 */
  intended: number
  /** 系统当前确实挂着的待触发通知数；无法核实时为 null。 */
  systemArmed: number | null
  report: ReminderReport | null
}

/**
 * 汇总提醒链路的健康状况。
 * intended > systemArmed 就是国产 ROM 后台清理偷偷吃掉通知的直接证据，只有这个差值能让用户知道该去放行。
 */
export async function collectReminderDiagnostics(): Promise<ReminderDiagnostics> {
  const items = eventsWithReminders()
  const [permission, exactAlarm, systemArmed] = await Promise.all([
    notificationPermissionState(),
    exactAlarmPermissionState(),
    pendingNotificationCount(),
  ])
  return { permission, exactAlarm, intended: items.length, systemArmed, report: lastReport }
}
