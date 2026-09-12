import { describe, expect, it } from 'vitest'
import { DEFAULT_EVENT_DURATION_MIN, eventEndsAt, isEventFinished } from './schedule'

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
