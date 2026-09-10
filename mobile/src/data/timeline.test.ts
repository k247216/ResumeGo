import { describe, expect, it } from 'vitest'
import { eventsOnDate, timelineDates } from './timeline'

describe('日程时间轴', () => {
  it('从锚点生成连续的七个本地日期', () => {
    const days = timelineDates(new Date(2026, 8, 10, 15, 30), 7)

    expect(days).toHaveLength(7)
    expect(days[0].toDateString()).toBe(new Date(2026, 8, 10).toDateString())
    expect(days[6].toDateString()).toBe(new Date(2026, 8, 16).toDateString())
    expect(days.every((day) => day.getHours() === 0 && day.getMinutes() === 0)).toBe(true)
  })

  it('默认可以生成至少一个月的可滑动日期窗口', () => {
    const days = timelineDates(new Date(2026, 8, 10, 15, 30), 31)

    expect(days).toHaveLength(31)
    expect(days[30].toDateString()).toBe(new Date(2026, 9, 10).toDateString())
  })

  it('按本地年月日筛选当天的日程', () => {
    const events = [
      { id: 1, startTime: new Date(2026, 8, 10, 14, 0).toISOString() },
      { id: 2, startTime: new Date(2026, 8, 11, 10, 0).toISOString() },
    ]

    expect(eventsOnDate(events, new Date(2026, 8, 10)).map((event) => event.id)).toEqual([1])
  })
})
