import { describe, expect, it } from 'vitest'
import { computeFunnel } from './funnel'
import type { JobProject, StageEvent, TargetStage } from '../types/project'
import type { ScheduleEvent, ScheduleEventType } from '../types/schedule'

function target(id: number, stage: TargetStage, status: 'active' | 'archived' = 'active'): JobProject {
  return {
    id, name: `目标${id}`, status, stage, jobDescriptionId: null, resumeVersionId: null,
    archivedAt: status === 'archived' ? '2026-01-02T00:00:00.000Z' : null,
    stageUpdatedAt: '2026-01-01T00:00:00.000Z', industry: null, targetRole: null, location: null, notes: null,
    createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z',
  }
}
function ev(id: number, targetId: number, stage: TargetStage): StageEvent & { targetId: number } {
  return { id, targetId, stage, occurredAt: '2026-01-01T00:00:00.000Z' }
}

describe('求职漏斗统计', () => {
  it('按阶段记录统计到达数，转化率相对上一主线阶段', () => {
    // t1 只投递；t2 走到面试；t3 走到笔试；t4 被拒（到达过 applied）
    const targets = [target(1, 'applied'), target(2, 'interview'), target(3, 'exam'), target(4, 'rejected')]
    const events = [
      ev(1, 1, 'applied'),
      ev(2, 2, 'applied'), ev(3, 2, 'exam'), ev(4, 2, 'interview'),
      ev(5, 3, 'applied'), ev(6, 3, 'exam'),
      ev(7, 4, 'applied'), ev(8, 4, 'rejected'),
    ]
    const stats = computeFunnel(targets, events)
    expect(stats.total).toBe(4)

    const reached = Object.fromEntries(stats.rows.map((r) => [r.stage, r.reached]))
    expect(reached.applied).toBe(4)
    expect(reached.exam).toBe(2)
    expect(reached.interview).toBe(1)
    expect(reached.hr).toBe(0)
    expect(reached.offer).toBe(0)

    const rates = Object.fromEntries(stats.rows.map((r) => [r.stage, r.rate]))
    expect(rates.applied).toBeNull()
    expect(rates.exam).toBe(50)
    expect(rates.interview).toBe(50)
    expect(rates.hr).toBe(0)
  })

  it('当前阶段兜底：没有阶段记录的目标也算到达了当前及之前的主线阶段', () => {
    const stats = computeFunnel([target(9, 'hr')], [])
    expect(stats.rows.find((r) => r.stage === 'applied')!.reached).toBe(1)
    expect(stats.rows.find((r) => r.stage === 'hr')!.reached).toBe(1)
    expect(stats.rows.find((r) => r.stage === 'offer')!.reached).toBe(0)
  })

  it('终态目标至少算到过投递，即使一条阶段记录都没有', () => {
    const stats = computeFunnel([target(5, 'screened_out')], [])
    expect(stats.rows.find((r) => r.stage === 'applied')!.reached).toBe(1)
  })

  it('结果分布按终态阶段计数；归档与终态互不混淆', () => {
    const stats = computeFunnel(
      [target(1, 'offer'), target(2, 'rejected'), target(3, 'applied'), target(4, 'applied', 'archived')],
      [],
    )
    expect(stats.settled).toBe(2)
    expect(stats.archived).toBe(1)
    expect(stats.active).toBe(3)
    expect(stats.outcomes.find((o) => o.stage === 'offer')!.count).toBe(1)
    expect(stats.outcomes.find((o) => o.stage === 'rejected')!.count).toBe(1)
    expect(stats.outcomes.find((o) => o.stage === 'pool')!.count).toBe(0)
  })

  it('空工作区不报错，转化率没有除零', () => {
    const stats = computeFunnel([], [])
    expect(stats.total).toBe(0)
    expect(stats.rows[0].reached).toBe(0)
    expect(stats.rows[0].rate).toBeNull()
  })

  it('日程类型是到达证据：约了笔试/面试但没推阶段也算到达，避免上游低估算出 >100%', () => {
    // t6 只有 applied 阶段记录，但有笔试和面试日程（面试标题含 HR 面 → 同时算 HR 面）
    const schedule = (id: number, targetId: number, eventType: ScheduleEventType, title: string): ScheduleEvent => ({
      id, title, eventType, startTime: '2026-08-01T02:00:00.000Z', endTime: null, notes: null,
      jobDescriptionId: null, jobProjectId: targetId, createdAt: '', updatedAt: '',
    })
    const schedules = [
      schedule(1, 6, 'exam', '华为 笔试'),
      schedule(2, 6, 'interview', '华为 一面'),
      schedule(3, 6, 'interview', '华为 HR面'),
      schedule(4, 7, 'followup', '跟进一下'), // 跟进不进漏斗
    ]
    const stats = computeFunnel([target(6, 'applied'), target(7, 'applied')], [ev(1, 6, 'applied')], schedules)
    const reached = Object.fromEntries(stats.rows.map((r) => [r.stage, r.reached]))
    expect(reached.exam).toBe(1)
    expect(reached.interview).toBe(1)
    expect(reached.hr).toBe(1)
    // 上游 <= 下游，转化率不会超过 100
    for (const row of stats.rows) expect(row.rate == null || row.rate <= 100 || reached[row.stage] > reached.applied).toBe(true)
  })
})
