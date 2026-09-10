import { describe, expect, it } from 'vitest'
import { buildIcsEvent } from './calendar'

describe('手机日历事件桥接', () => {
  it('生成手机日历可识别的 ICS 事件，并保留标题、时间和备注', () => {
    const ics = buildIcsEvent({
      id: 42,
      title: '腾讯技术一面',
      startTime: '2026-09-11T10:00:00.000Z',
      endTime: '2026-09-11T11:30:00.000Z',
      notes: '线上会议; 准备 Redis',
    })
    expect(ics).toContain('BEGIN:VCALENDAR')
    expect(ics).toContain('UID:zhida-42@career-os')
    expect(ics).toContain('SUMMARY:腾讯技术一面')
    expect(ics).toContain('DTSTART:20260911T100000Z')
    expect(ics).toContain('DTEND:20260911T113000Z')
    expect(ics).toContain('DESCRIPTION:线上会议\\; 准备 Redis')
    expect(ics).toContain('END:VCALENDAR')
  })
})
