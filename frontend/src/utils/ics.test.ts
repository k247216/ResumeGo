import { describe, expect, it } from 'vitest'
import { parseIcs } from './ics'

describe('parseIcs', () => {
  it('parses basic timed events with summary and uid', () => {
    const raw = [
      'BEGIN:VCALENDAR',
      'BEGIN:VEVENT',
      'UID:abc-123',
      'SUMMARY:腾讯技术面',
      'DTSTART:20260825T140000',
      'DTEND:20260825T153000',
      'END:VEVENT',
      'END:VCALENDAR',
    ].join('\r\n')

    const result = parseIcs(raw)
    expect(result.skippedRecurring).toBe(0)
    expect(result.events).toEqual([
      { uid: 'abc-123', title: '腾讯技术面', startTime: '2026-08-25T14:00:00', endTime: '2026-08-25T15:30:00', allDay: false },
    ])
  })

  it('unfolds continuation lines and escapes text', () => {
    const raw = [
      'BEGIN:VEVENT',
      'SUMMARY:字节\\n三面（含',
      ' 续行）',
      'DTSTART;TZID=Asia/Shanghai:20260901T093000',
      'END:VEVENT',
    ].join('\r\n')

    const result = parseIcs(raw)
    expect(result.events[0].title).toBe('字节\n三面（含续行）')
    // TZID 值为该时区墙上时间：换算成时刻后按本地展示，断言与公式保持一致（不写死运行环境时区）
    const asIfUtc = Date.UTC(2026, 8, 1, 9, 30, 0)
    const offsetMs = timeZoneOffsetOfShanghaiAt(asIfUtc)
    const expected = new Date(asIfUtc - offsetMs)
    const pad = (value: number) => String(value).padStart(2, '0')
    expect(result.events[0].startTime).toBe(
      `${expected.getFullYear()}-${pad(expected.getMonth() + 1)}-${pad(expected.getDate())}T${pad(expected.getHours())}:${pad(expected.getMinutes())}:00`,
    )
  })

  function timeZoneOffsetOfShanghaiAt(dateMs: number): number {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Shanghai',
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
    }).formatToParts(new Date(dateMs))
    const get = (type: string) => Number(parts.find((part) => part.type === type)?.value ?? '0')
    return Date.UTC(get('year'), get('month') - 1, get('day'), get('hour'), get('minute'), get('second')) - dateMs
  }

  it('converts UTC values to local time', () => {
    const raw = [
      'BEGIN:VEVENT',
      'SUMMARY:UTC 面试',
      'DTSTART:20260825T060000Z',
      'DTEND:20260825T070000Z',
      'END:VEVENT',
    ].join('\n')

    const result = parseIcs(raw)
    // 本地时区换算：只断言与 Date 换算一致，不写死时区
    const expected = new Date(Date.UTC(2026, 7, 25, 6, 0, 0))
    const pad = (value: number) => String(value).padStart(2, '0')
    expect(result.events[0].startTime).toBe(
      `${expected.getFullYear()}-${pad(expected.getMonth() + 1)}-${pad(expected.getDate())}T${pad(expected.getHours())}:${pad(expected.getMinutes())}:00`,
    )
  })

  it('marks VALUE=DATE as all-day events', () => {
    const raw = [
      'BEGIN:VEVENT',
      'SUMMARY:入职日',
      'DTSTART;VALUE=DATE:20261001',
      'DTEND;VALUE=DATE:20261002',
      'END:VEVENT',
    ].join('\n')

    const result = parseIcs(raw)
    expect(result.events[0].allDay).toBe(true)
    expect(result.events[0].startTime).toBe('2026-10-01T00:00:00')
  })

  it('skips recurring and cancelled events with counters', () => {
    const raw = [
      'BEGIN:VEVENT',
      'SUMMARY:每周例会',
      'RRULE:FREQ=WEEKLY',
      'DTSTART:20260825T100000',
      'END:VEVENT',
      'BEGIN:VEVENT',
      'SUMMARY:已取消面试',
      'STATUS:CANCELLED',
      'DTSTART:20260826T100000',
      'END:VEVENT',
      'BEGIN:VEVENT',
      'SUMMARY:正常事件',
      'DTSTART:20260827T100000',
      'END:VEVENT',
    ].join('\n')

    const result = parseIcs(raw)
    expect(result.skippedRecurring).toBe(1)
    expect(result.skippedCancelled).toBe(1)
    expect(result.events.map((event) => event.title)).toEqual(['正常事件'])
  })

  it('ignores nested VALARM properties', () => {
    const raw = [
      'BEGIN:VEVENT',
      'SUMMARY:带提醒的事件',
      'DTSTART:20260828T090000',
      'BEGIN:VALARM',
      'ACTION:DISPLAY',
      'TRIGGER:-PT15M',
      'END:VALARM',
      'END:VEVENT',
    ].join('\n')

    const result = parseIcs(raw)
    expect(result.events).toHaveLength(1)
    expect(result.events[0].title).toBe('带提醒的事件')
    expect(result.events[0].startTime).toBe('2026-08-28T09:00:00')
  })

  it('falls back to 未命名日程 when summary missing', () => {
    const raw = ['BEGIN:VEVENT', 'DTSTART:20260829T120000', 'END:VEVENT'].join('\n')
    expect(parseIcs(raw).events[0].title).toBe('未命名日程')
  })
})
