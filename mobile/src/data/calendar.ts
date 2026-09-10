import { Capacitor } from '@capacitor/core'
import type { ScheduleEvent } from '../types/schedule'

export interface CalendarEventLike {
  id: number
  title: string
  startTime: string
  endTime: string | null
  notes: string | null
}

function escapeIcs(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n')
}

function icsDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) throw new Error('日程时间无效')
  return date.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')
}

export function buildIcsEvent(event: CalendarEventLike): string {
  const start = icsDate(event.startTime)
  const end = event.endTime ? icsDate(event.endTime) : icsDate(new Date(new Date(event.startTime).getTime() + 60 * 60_000).toISOString())
  const description = event.notes ? `DESCRIPTION:${escapeIcs(event.notes)}\r\n` : ''
  return [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Zhida Career OS//Mobile//CN',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:zhida-${event.id}@career-os`,
    `DTSTAMP:${icsDate(new Date().toISOString())}`,
    `DTSTART:${start}`,
    `DTEND:${end}`,
    `SUMMARY:${escapeIcs(event.title)}`,
    description.trimEnd(),
    'END:VEVENT',
    'END:VCALENDAR',
    '',
  ].filter(Boolean).join('\r\n')
}

function toBase64(text: string): string {
  const bytes = new TextEncoder().encode(text)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary)
}

/**
 * 将本地日程交给手机系统日历：原生端打开系统分享面板，网页端下载 .ics。
 * 这不会读取用户已有日历，也不会把日程上传到服务器。
 */
export async function addToDeviceCalendar(event: ScheduleEvent): Promise<'shared' | 'downloaded'> {
  const ics = buildIcsEvent(event)
  const safeName = event.title.replace(/[^\p{L}\p{N}_-]+/gu, '-').replace(/^-|-$/g, '') || 'zhida-event'
  const fileName = `${safeName}-${event.id}.ics`

  if (Capacitor.isNativePlatform()) {
    const { Filesystem, Directory } = await import('@capacitor/filesystem')
    const { Share } = await import('@capacitor/share')
    const written = await Filesystem.writeFile({
      path: fileName,
      data: toBase64(ics),
      directory: Directory.Cache,
    })
    await Share.share({ title: event.title, url: written.uri, dialogTitle: '添加到手机日历' })
    return 'shared'
  }

  const url = URL.createObjectURL(new Blob([ics], { type: 'text/calendar;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
  return 'downloaded'
}
