import { describe, expect, it } from 'vitest'
import { DEFAULT_EVENT_DURATION_MIN, eventEndsAt, eventStatus, isEventFinished, scheduleTimeError } from './schedule'

const START = '2026-03-05T09:00:00.000Z'

describe('日程是否已经结束', () => {
  it('没填结束时间时按一小时估长度', () => {
    expect(eventEndsAt({ startTime: START, endTime: null }))
      .toBe(new Date(START).getTime() + DEFAULT_EVENT_DURATION_MIN * 60_000)
  })

  it('结束时间不晚于开始时间也按一小时估，避免日程永远停在「进行中」', () => {
    const ends = eventEndsAt({ startTime: START, endTime: '2026-03-05T08:00:00.000Z' })
    expect(ends).toBe(new Date(START).getTime() + DEFAULT_EVENT_DURATION_MIN * 60_000)
  })

  it('时间字段非法时退回开始时间，不会算出 NaN', () => {
    expect(eventEndsAt({ startTime: '不是时间', endTime: null })).toBe(0)
    expect(isEventFinished({ startTime: '不是时间', endTime: null }, Date.now())).toBe(true)
  })

  it('恰好到点的日程算已结束，写心得的入口在这一刻出现', () => {
    const ends = eventEndsAt({ startTime: START, endTime: '2026-03-05T10:00:00.000Z' })
    expect(isEventFinished({ startTime: START, endTime: '2026-03-05T10:00:00.000Z' }, ends)).toBe(true)
    expect(isEventFinished({ startTime: START, endTime: '2026-03-05T10:00:00.000Z' }, ends - 1)).toBe(false)
  })
})

describe('日程三态判定', () => {
  const NOW = new Date('2026-09-14T06:00:00.000Z').getTime()

  it('开始前是 upcoming，开始到结束之间是 ongoing，结束即 finished', () => {
    const ev = { startTime: '2026-09-14T05:00:00.000Z', endTime: '2026-09-14T07:00:00.000Z' }
    expect(eventStatus(ev, NOW - 2 * 3600_000)).toBe('upcoming')
    expect(eventStatus(ev, NOW)).toBe('ongoing')
    expect(eventStatus(ev, NOW + 3600_000)).toBe('finished')
  })

  it('没填结束时间按一小时估：开始半小时后算进行中，两小时后算已结束', () => {
    const ev = { startTime: '2026-09-14T05:00:00.000Z', endTime: null }
    expect(eventStatus(ev, NOW - 1800_000)).toBe('ongoing')
    expect(eventStatus(ev, NOW + 2 * 3600_000)).toBe('finished')
  })

  it('时间字段非法按 finished 兜底，复盘入口不被锁死', () => {
    expect(eventStatus({ startTime: '不是时间', endTime: null }, NOW)).toBe('finished')
  })
})

describe('日程表单时间校验', () => {
  it('没填或填坏开始时间要拦下', () => {
    expect(scheduleTimeError(null, null)).toContain('开始时间')
    expect(scheduleTimeError('不是日期', null)).toContain('开始时间')
  })

  it('结束早于或等于开始都要拦下，不能靠 eventEndsAt 静默兜底', () => {
    expect(scheduleTimeError(START, '2026-03-05T08:00:00.000Z')).toContain('晚于')
    expect(scheduleTimeError(START, START)).toContain('晚于')
  })

  it('结束时间填坏也要拦下', () => {
    expect(scheduleTimeError(START, 'bad')).toContain('结束时间')
  })

  it('只填开始时间、或结束晚于开始，都通过', () => {
    expect(scheduleTimeError(START, null)).toBeNull()
    expect(scheduleTimeError(START, '2026-03-05T10:00:00.000Z')).toBeNull()
  })
})
