// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useExternalCalendars } from './useExternalCalendars'

const validIcs = [
  'BEGIN:VEVENT',
  'SUMMARY:飞书面试',
  'DTSTART:20260825T140000',
  'DTEND:20260825T150000',
  'END:VEVENT',
].join('\r\n')

describe('useExternalCalendars', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('imports a source and exposes flattened readonly events', () => {
    const store = useExternalCalendars()
    expect(store.externalEvents.value).toEqual([])

    const result = store.importFromText('飞书日历', validIcs)

    expect(result.count).toBe(1)
    expect(store.sources.value).toHaveLength(1)
    expect(store.sources.value[0].name).toBe('飞书日历')
    expect(store.externalEvents.value[0]).toMatchObject({
      title: '飞书面试',
      kind: 'external',
      allDay: false,
      startTime: '2026-08-25T14:00:00',
      endTime: '2026-08-25T15:00:00',
    })
    expect(store.externalEvents.value[0].sourceName).toBe('飞书日历')
    // 持久化到 localStorage
    expect(JSON.parse(localStorage.getItem('resumego:external-calendars:v1') ?? '[]')).toHaveLength(1)
  })

  it('rejects empty or non-ics content with a readable error', () => {
    const store = useExternalCalendars()
    expect(() => store.importFromText('空白', '')).toThrow('文件内容为空')
    expect(() => store.importFromText('乱内容', 'hello world')).toThrow('.ics / iCal 格式')
    expect(store.sources.value).toHaveLength(0)
  })

  it('explains when only RRULE events exist', () => {
    const store = useExternalCalendars()
    const rruleOnly = ['BEGIN:VEVENT', 'SUMMARY:周会', 'RRULE:FREQ=WEEKLY', 'DTSTART:20260825T100000', 'END:VEVENT'].join('\n')
    expect(() => store.importFromText('重复事件', rruleOnly)).toThrow('重复规则（RRULE）')
  })

  it('keeps at most five most-recent sources and removes by id', () => {
    const store = useExternalCalendars()
    for (let index = 0; index < 7; index += 1) {
      store.importFromText(`来源 ${index}`, `BEGIN:VEVENT\nSUMMARY:第 ${index} 场\nDTSTART:2030010${(index % 9) + 1}T100000\nEND:VEVENT`)
    }
    expect(store.sources.value).toHaveLength(5)
    expect(store.sources.value[0].name).toBe('来源 6')

    store.remove(store.sources.value[0].id)
    expect(store.sources.value.some((source) => source.name === '来源 6')).toBe(false)
  })

  it('hides events older than the 90-day horizon', () => {
    vi.useFakeTimers({ now: new Date(2026, 7, 23) })
    try {
      const store = useExternalCalendars()
      const old = ['BEGIN:VEVENT', 'SUMMARY:很早以前', 'DTSTART:20250101T100000', 'END:VEVENT'].join('\n')
      store.importFromText('旧日历', old)
      expect(store.externalEvents.value).toEqual([])
    } finally {
      vi.useRealTimers()
    }
  })
})
