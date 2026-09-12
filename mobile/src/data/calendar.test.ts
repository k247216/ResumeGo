import { describe, expect, it } from 'vitest'
import type { ScheduleEvent } from '../types/schedule'
import type { CalendarHandoffDeps } from './calendar'
import { addToDeviceCalendar, buildIcsEvent, foldIcsLine } from './calendar'
import type { CreateEventOptions } from './calendarIntent'

const encoder = new TextEncoder()
const octets = (value: string): number => encoder.encode(value).length

function scheduleEvent(overrides: Partial<ScheduleEvent> = {}): ScheduleEvent {
  return {
    id: 42,
    title: '腾讯技术一面',
    eventType: 'interview',
    startTime: '2026-09-11T10:00:00.000Z',
    endTime: '2026-09-11T11:30:00.000Z',
    notes: null,
    jobDescriptionId: null,
    jobProjectId: null,
    createdAt: '2026-09-01T00:00:00.000Z',
    updatedAt: '2026-09-01T00:00:00.000Z',
    ...overrides,
  }
}

function deps(overrides: Partial<CalendarHandoffDeps> = {}): CalendarHandoffDeps {
  return {
    isNative: overrides.isNative ?? (() => true),
    nativeReady: overrides.nativeReady ?? (() => true),
    openNative: overrides.openNative ?? (async () => undefined),
    share: overrides.share ?? (async () => 'com.android.calendar'),
  }
}

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

  it('无结束时间时按一小时收口，且文件以 CRLF 结尾', () => {
    const ics = buildIcsEvent({
      id: 8,
      title: '笔试',
      startTime: '2026-09-11T10:00:00.000Z',
      endTime: null,
      notes: null,
    })
    expect(ics).toContain('DTEND:20260911T110000Z')
    expect(ics.endsWith('\r\n')).toBe(true)
    expect(ics).not.toContain('DESCRIPTION')
  })

  it('长备注折行后每条物理行都不超过 75 octet，且 unfold 回来仍是完整内容', () => {
    const notes =
      '线上会议 https://meeting.example.com/j/1234567890?pwd=abcd; 请提前测试摄像头与麦克风，并准备好 Redis、MySQL 索引与项目复盘的追问材料'
    const ics = buildIcsEvent({ id: 7, title: '字节跳动后端二面', startTime: '2026-09-11T10:00:00.000Z', endTime: null, notes })
    for (const line of ics.trimEnd().split('\r\n')) expect(octets(line)).toBeLessThanOrEqual(75)
    const unfolded = ics.replace(/\r\n /g, '')
    expect(unfolded).toContain(`DESCRIPTION:${notes.replace(/;/g, '\\;').replace(/,/g, '\\,')}`)
  })
})

describe('foldIcsLine', () => {
  it('未超预算的整行原样返回', () => {
    expect(foldIcsLine('SUMMARY:短标题')).toBe('SUMMARY:短标题')
    expect(foldIcsLine('')).toBe('')
  })

  it('按 UTF-8 字节数吃满 75 octet 才断行，续行以一个空格开头', () => {
    const lines = foldIcsLine('あ'.repeat(40)).split('\r\n')
    expect(octets(lines[0])).toBe(75)
    expect(lines[1]).toBe(` ${'あ'.repeat(15)}`)
  })

  it('从不拆裂多字节字符与代理对', () => {
    const folded = foldIcsLine(`${'a'.repeat(73)}😀`)
    expect(folded).toBe(`${'a'.repeat(73)}\r\n 😀`)
    expect(folded.replace(/\r\n /g, '')).toBe(`${'a'.repeat(73)}😀`)
  })
})

describe('addToDeviceCalendar 的落点判别', () => {
  it('原生插件可用时走 ACTION_INSERT，并把 UTC 毫秒交给日历', async () => {
    const sent: CreateEventOptions[] = []
    let shared = 0
    const handoff = await addToDeviceCalendar(
      scheduleEvent(),
      deps({
        openNative: async (options) => {
          sent.push(options)
        },
        share: async () => {
          shared += 1
          return 'com.android.calendar'
        },
      }),
    )
    expect(handoff).toBe('native')
    expect(shared).toBe(0)
    expect(sent[0]).toMatchObject({
      title: '腾讯技术一面',
      description: null,
      beginTime: Date.parse('2026-09-11T10:00:00.000Z'),
      endTime: Date.parse('2026-09-11T11:30:00.000Z'),
    })
  })

  it('原生报 NO_CALENDAR_APP 时降级为分享 .ics，文件名带扩展名', async () => {
    const payloads: string[] = []
    const handoff = await addToDeviceCalendar(
      scheduleEvent({ title: '笔试 / 线上' }),
      deps({
        openNative: async () => {
          throw new Error('NO_CALENDAR_APP')
        },
        share: async (payload) => {
          payloads.push(payload.fileName)
          return 'com.android.filemanager'
        },
      }),
    )
    expect(handoff).toBe('shared')
    expect(payloads).toEqual(['笔试-线上-42.ics'])
  })

  it('原生插件没注册上时直接走分享通路', async () => {
    let opened = 0
    const handoff = await addToDeviceCalendar(
      scheduleEvent(),
      deps({
        nativeReady: () => false,
        openNative: async () => {
          opened += 1
        },
      }),
    )
    expect(handoff).toBe('shared')
    expect(opened).toBe(0)
  })

  it('用户在分享面板里返回算 unavailable，不算失败也不谎称已加入日历', async () => {
    const handoff = await addToDeviceCalendar(scheduleEvent(), deps({ nativeReady: () => false, share: async () => null }))
    expect(handoff).toBe('unavailable')
  })

  it('写盘/分享抛错时收口成 unavailable 而不是把异常甩给调用方', async () => {
    const handoff = await addToDeviceCalendar(
      scheduleEvent(),
      deps({
        nativeReady: () => false,
        share: async () => {
          throw new Error('FILE_NOTCREATED')
        },
      }),
    )
    expect(handoff).toBe('unavailable')
  })

  it('浏览器分支只下载文件并返回 downloaded', async () => {
    const fileNames: string[] = []
    const handoff = await addToDeviceCalendar(
      scheduleEvent(),
      deps({
        isNative: () => false,
        share: async (payload) => {
          fileNames.push(payload.fileName)
          return null
        },
      }),
    )
    expect(handoff).toBe('downloaded')
    expect(fileNames).toEqual(['腾讯技术一面-42.ics'])
  })
})
