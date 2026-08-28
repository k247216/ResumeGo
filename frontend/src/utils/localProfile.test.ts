import { beforeEach, describe, expect, it } from 'vitest'
import {
  buildMonthUsageCells,
  formatUsageDuration,
  getDailyUsage,
  localProfileStorageKey,
  readLocalProfile,
  recordLocalUsage,
  touchLocalProfile,
  updateLocalProfile,
} from './localProfile'

describe('local profile usage model', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('builds a Monday-first month grid with blank leading cells', () => {
    const profile = readLocalProfile()
    const cells = buildMonthUsageCells(2026, 1, profile)

    expect(cells.length).toBe(35)
    expect(cells.slice(0, 6).every((cell) => !cell.isCurrentMonth)).toBe(true)
    expect(cells[6]).toMatchObject({ key: '2026-02-01', day: 1, isCurrentMonth: true })
    expect(cells.filter((cell) => cell.isCurrentMonth).at(-1)).toMatchObject({ key: '2026-02-28', day: 28, isCurrentMonth: true })
  })

  it('returns real minutes for a recorded day and marks fallback values as demo data', () => {
    const profile = readLocalProfile()
    const realKey = '2026-02-12'
    profile.usageMinutesByDate[realKey] = 47
    profile.usageDates.push(realKey)

    expect(getDailyUsage(profile, realKey)).toMatchObject({ minutes: 47, isDemo: false })
    expect(getDailyUsage(profile, '2026-02-05').isDemo).toBe(true)
  })

  it('records local usage against the local calendar date and persists it', () => {
    const recordedAt = new Date(2026, 1, 12, 9, 30)
    const profile = recordLocalUsage(12, recordedAt)

    expect(profile.usageDates).toContain('2026-02-12')
    expect(profile.usageMinutesByDate['2026-02-12']).toBe(12)
    expect(profile.lastUsedAt).toBe(recordedAt.toISOString())
    expect(JSON.parse(localStorage.getItem(localProfileStorageKey()) ?? '{}').usageMinutesByDate['2026-02-12']).toBe(12)
  })

  it('counts application launches separately from elapsed local usage', () => {
    const openedAt = new Date(2026, 1, 12, 9, 30)
    const first = touchLocalProfile(openedAt)
    const elapsed = recordLocalUsage(12, new Date(2026, 1, 12, 9, 42))
    const second = touchLocalProfile(new Date(2026, 1, 13, 8, 0))

    expect(first.launchCount).toBe(1)
    expect(elapsed.launchCount).toBe(1)
    expect(second.launchCount).toBe(2)
    expect(second.usageMinutesByDate['2026-02-12']).toBe(13)
  })

  it('formats daily minutes for the profile detail without losing short sessions', () => {
    expect(formatUsageDuration(0)).toBe('未记录')
    expect(formatUsageDuration(7)).toBe('7 分钟')
    expect(formatUsageDuration(75)).toBe('1 小时 15 分钟')
  })

  it('persists an uploaded avatar data URL through the profile update path', () => {
    const avatar = 'data:image/png;base64,ZmFrZQ=='
    const profile = updateLocalProfile({ avatar })

    expect(profile.avatar).toBe(avatar)
    expect(readLocalProfile().avatar).toBe(avatar)
  })
})
