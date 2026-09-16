import { describe, expect, it } from 'vitest'
import { shouldReArm } from './autoRearm'
import type { ReminderReport } from './reminders'

describe('shouldReArm', () => {
  const report = (denied: number): ReminderReport => ({
    scheduled: 3, skippedPast: 1, denied, web: 0, failed: 0, total: 4 + denied,
  })

  it('权限没给时永不重排（避免每次回前台空跑）', () => {
    expect(shouldReArm(report(5), 'denied')).toBe(false)
    expect(shouldReArm(null, 'denied')).toBe(false)
    expect(shouldReArm(null, 'unsupported')).toBe(false)
  })

  it('从未排过 + 权限已给 → 补排', () => {
    expect(shouldReArm(null, 'granted')).toBe(true)
  })

  it('上一轮有 denied（首装冷启动常态）+ 权限已给 → 补排', () => {
    expect(shouldReArm(report(4), 'granted')).toBe(true)
  })

  it('上一轮全部排入成功 → 不重排', () => {
    expect(shouldReArm(report(0), 'granted')).toBe(false)
  })
})
