import { Capacitor } from '@capacitor/core'
import type { ScheduleEvent } from '../types/schedule'
import type { CreateEventOptions } from './calendarIntent'
import { isCalendarIntentAvailable, openCreateEvent } from './calendarIntent'
import type { SharePayload } from './share'
import { shareFile } from './share'

export interface CalendarEventLike {
  id: number
  title: string
  startTime: string
  endTime: string | null
  notes: string | null
}

/**
 * 日程交到手机日历的真实落点，调用方必须按它区分文案：
 * 'native' 才是系统日历的「新建事件」预填页；'shared' 只是把 .ics 丢进了分享面板；
 * 'downloaded' 是浏览器下载；'unavailable' 表示这条链路在本机走不通（用户在面板里返回也算这里，那不是故障）。
 */
export type CalendarHandoff = 'native' | 'shared' | 'downloaded' | 'unavailable'

/** 供单测注入的原生/分享通路；生产调用不传即可。 */
export interface CalendarHandoffDeps {
  isNative: () => boolean
  nativeReady: () => boolean
  openNative: (options: CreateEventOptions) => Promise<void>
  share: (payload: SharePayload) => Promise<string | null>
}

const ICS_MIME = 'text/calendar;charset=utf-8'
const DEFAULT_MINUTES = 60

/** 起止时间只在这里算一次：UTC 毫秒给原生，UTC ISO 给 .ics，两条路不会是两套时间。 */
function eventRange(event: CalendarEventLike): { beginTime: number; endTime: number } {
  const beginTime = new Date(event.startTime).getTime()
  if (Number.isNaN(beginTime)) throw new Error('日程时间无效')
  const end = event.endTime ? new Date(event.endTime).getTime() : NaN
  // 结束时间缺失或不晚于开始时间时按 1 小时兜底，否则日历里会落一个看不见的事件。
  const endTime = Number.isNaN(end) || end <= beginTime ? beginTime + DEFAULT_MINUTES * 60_000 : end
  return { beginTime, endTime }
}

function escapeIcs(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n')
}

/**
 * RFC 5545 的折行：物理行不得超过 75 octet，续行以一个空格开头。
 * 按 UTF-8 字节数计数（备注多为中文，按字符数会算少），且从不拆裂码点；
 * 长备注不折行会产出非法 VEVENT，日历 App 直接整个文件解析失败。
 */
export function foldIcsLine(line: string, limit = 75): string {
  const encoder = new TextEncoder()
  const parts: string[] = []
  let buf = ''
  let octets = 0
  for (const char of line) {
    const size = encoder.encode(char).length
    if (octets + size > limit) {
      parts.push(buf)
      buf = ' '
      octets = 1
    }
    buf += char
    octets += size
  }
  parts.push(buf)
  return parts.join('\r\n')
}

// 折行必须在拼上属性名之后做：`DESCRIPTION:` 自身也占 75 octet 的预算。
function icsLine(name: string, value: string): string {
  return foldIcsLine(`${name}:${escapeIcs(value)}`)
}

function icsDate(value: string | number): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) throw new Error('日程时间无效')
  return date.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')
}

export function buildIcsEvent(event: CalendarEventLike): string {
  const { beginTime, endTime } = eventRange(event)
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Zhida Career OS//Mobile//CN',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:zhida-${event.id}@career-os`,
    `DTSTAMP:${icsDate(Date.now())}`,
    `DTSTART:${icsDate(beginTime)}`,
    `DTEND:${icsDate(endTime)}`,
    icsLine('SUMMARY', event.title),
    ...(event.notes ? [icsLine('DESCRIPTION', event.notes)] : []),
    'END:VEVENT',
    'END:VCALENDAR',
  ]
  return `${lines.join('\r\n')}\r\n`
}

function fileNameOf(title: string, id: number): string {
  const safeName = title.replace(/[^\p{L}\p{N}_-]+/gu, '-').replace(/^-|-$/g, '') || 'zhida-event'
  return `${safeName}-${id}.ics`
}

const defaultDeps: CalendarHandoffDeps = {
  isNative: () => Capacitor.isNativePlatform(),
  nativeReady: isCalendarIntentAvailable,
  openNative: openCreateEvent,
  share: shareFile,
}

/**
 * 把本地日程交给手机系统日历：优先用 ACTION_INSERT 拉起日历的「新建事件」页，
 * 由日历 App 自己写入（分享 .ics 那条路走不通，日历 App 根本不接收 text/calendar）。
 * 预填页仍需用户按一次保存，因此不承诺「已经存进日历」，也不承诺日历会另发一次提醒。
 * 这不会读取用户已有日历，也不会把日程上传到服务器。
 */
export async function addToDeviceCalendar(event: ScheduleEvent, deps: CalendarHandoffDeps = defaultDeps): Promise<CalendarHandoff> {
  const { beginTime, endTime } = eventRange(event)
  const payload = (): SharePayload => ({
    fileName: fileNameOf(event.title, event.id),
    blob: new Blob([buildIcsEvent(event)], { type: ICS_MIME }),
    subject: event.title,
    dialogTitle: '添加到手机日历',
  })

  if (!deps.isNative()) {
    // 浏览器里没有日历可拉起，shareFile 的 Web 分支就是 blob + <a download>。
    try {
      await deps.share(payload())
      return 'downloaded'
    } catch {
      return 'unavailable'
    }
  }

  if (deps.nativeReady()) {
    try {
      await deps.openNative({ title: event.title, description: event.notes, beginTime, endTime })
      return 'native'
    } catch {
      // NO_CALENDAR_APP / CALENDAR_INTENT_FAILED / 未注册，一律继续降级。
    }
  }

  try {
    return (await deps.share(payload())) ? 'shared' : 'unavailable'
  } catch {
    return 'unavailable'
  }
}
