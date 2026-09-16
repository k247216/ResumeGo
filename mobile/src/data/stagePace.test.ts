import { describe, expect, it } from 'vitest'
import { stagePaceLine, stagePaceSummary } from './stagePace'
import type { StageEvent, TargetStage } from '../types/project'

function ev(targetId: number, id: number, stage: TargetStage, occurredAt: string): StageEvent & { targetId: number } {
  return { id, targetId, stage, occurredAt }
}

const NOW = new Date('2026-09-17T00:00:00.000Z').getTime()

describe('stagePaceSummary', () => {
  it('投递到进面的平均天数：两个样本取平均', () => {
    const events = [
      ev(1, 1, 'applied', '2026-09-01T00:00:00.000Z'),
      ev(1, 2, 'interview', '2026-09-06T00:00:00.000Z'), // 5 天
      ev(2, 3, 'applied', '2026-09-01T00:00:00.000Z'),
      ev(2, 4, 'interview', '2026-09-11T00:00:00.000Z'), // 10 天
    ]
    const pace = stagePaceSummary(events, [], NOW)
    expect(pace.appliedToInterviewDays).toBe(8) // (5+10)/2 = 7.5 → 8
    expect(pace.sampleCount).toBe(2)
  })

  it('没走到面试的目标不进样本；只有面试没有投递记录的也不进', () => {
    const events = [
      ev(1, 1, 'applied', '2026-09-01T00:00:00.000Z'),
      ev(2, 2, 'interview', '2026-09-05T00:00:00.000Z'),
    ]
    expect(stagePaceSummary(events, [], NOW).appliedToInterviewDays).toBeNull()
  })

  it('卡得最久：只看进行中的目标，结束的不算卡', () => {
    const targets = [
      { id: 1, name: '字节跳动', status: 'active', stage: 'interview' as TargetStage },
      { id: 2, name: '美团', status: 'active', stage: 'rejected' as TargetStage },
      { id: 3, name: '腾讯', status: 'archived', stage: 'interview' as TargetStage },
    ]
    const events = [
      ev(1, 1, 'applied', '2026-09-01T00:00:00.000Z'),
      ev(1, 2, 'interview', '2026-09-10T00:00:00.000Z'), // 停了 7 天
      ev(2, 3, 'applied', '2026-08-01T00:00:00.000Z'),  // 结束态，停再久也不算
      ev(3, 4, 'interview', '2026-08-10T00:00:00.000Z'), // 归档，不算
    ]
    const pace = stagePaceSummary(events, targets, NOW)
    expect(pace.longestStall?.targetName).toBe('字节跳动')
    expect(pace.longestStall?.days).toBe(7)
    expect(pace.longestStall?.stage).toBe('interview')
  })

  it('当前阶段以 target.stage 为准（事件缺失时跳过该目标），停留不足 1 天不算卡', () => {
    const targets = [{ id: 1, name: '网易', status: 'active', stage: 'hr' as TargetStage }]
    const events = [ev(1, 1, 'applied', '2026-09-16T23:00:00.000Z')]
    expect(stagePaceSummary(events, targets, NOW).longestStall).toBeNull()
  })

  it('坏时间戳被丢弃，不毁掉整段计算', () => {
    const events = [
      ev(1, 1, 'applied', '不是日期'),
      ev(1, 2, 'interview', '2026-09-06T00:00:00.000Z'),
    ]
    expect(stagePaceSummary(events, [], NOW).appliedToInterviewDays).toBeNull()
  })
})

describe('stagePaceLine', () => {
  it('两段信息拼接成一句话', () => {
    const line = stagePaceLine({
      appliedToInterviewDays: 8, sampleCount: 2,
      longestStall: { targetId: 1, targetName: '字节跳动', stage: 'interview', days: 7 },
    })
    expect(line).toBe('从投递到进面平均 8 天；字节跳动 已在面试阶段停了 7 天')
  })

  it('没有任何可说的时候返回空串', () => {
    expect(stagePaceLine({ appliedToInterviewDays: null, sampleCount: 0, longestStall: null })).toBe('')
  })
})
