import { describe, expect, it } from 'vitest'
import { isCalendarIntentAvailable, openCreateEvent } from './calendarIntent'

describe('CalendarIntent 原生桥', () => {
  it('浏览器里没有原生插件，可用性判定必须是 false 且不抛', () => {
    expect(isCalendarIntentAvailable()).toBe(false)
  })

  it('没有实现时调用以 reject 收口，不会同步抛出打断调用方', async () => {
    await expect(openCreateEvent({ title: '腾讯技术一面', beginTime: 0, endTime: 60_000 })).rejects.toThrow(/not implemented/)
  })
})
