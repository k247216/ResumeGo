import { beforeEach, describe, expect, it } from 'vitest'
import {
  createSchedule, createTarget, deleteSchedule, getReminder, importBackup,
  listTargets, setReminder, setStage, stageEventsOf,
} from './store'

describe('求职目标阶段规则', () => {
  it('创建目标默认进入「投递中」并留下阶段记录', () => {
    const t = createTarget('测试公司 · 前端')
    expect(t.stage).toBe('applied')
    expect(stageEventsOf(t.id).some((e) => e.stage === 'applied')).toBe(true)
  })

  it('阶段只能向前推进', () => {
    const t = createTarget('前进测试')
    expect(setStage(t.id, 'interview').ok).toBe(true)
    const back = setStage(t.id, 'applied')
    expect(back.ok).toBe(false)
    expect(back.message).toContain('不能回退')
  })

  it('进入结果态后锁定，不可再改阶段', () => {
    const t = createTarget('锁定测试')
    expect(setStage(t.id, 'rejected').ok).toBe(true)
    const after = setStage(t.id, 'interview')
    expect(after.ok).toBe(false)
    expect(after.message).toContain('锁定')
  })
})

describe('日程提醒', () => {
  it('写入与读取提醒分钟数', () => {
    const ev = createSchedule({
      title: '提醒测试', eventType: 'interview',
      startTime: new Date(Date.now() + 3600_000).toISOString(),
      endTime: null, notes: null, jobDescriptionId: null, jobProjectId: null,
    })
    setReminder(ev.id, 30)
    expect(getReminder(ev.id)).toBe(30)
  })

  it('删除日程同时清除提醒', () => {
    const ev = createSchedule({
      title: '删除测试', eventType: 'exam',
      startTime: new Date(Date.now() + 7200_000).toISOString(),
      endTime: null, notes: null, jobDescriptionId: null, jobProjectId: null,
    })
    setReminder(ev.id, 60)
    deleteSchedule(ev.id)
    expect(getReminder(ev.id)).toBe(0)
  })
})

describe('备份导入校验', () => {
  beforeEach(() => { /* 单例 store，跨用例共享，无需重置 */ })

  it('非法 JSON 返回失败', () => {
    expect(importBackup('not-json').ok).toBe(false)
  })

  it('缺少必需集合返回失败', () => {
    expect(importBackup(JSON.stringify({ foo: 1 })).ok).toBe(false)
  })

  it('合法备份可恢复', () => {
    const snapshot = { targets: listTargets(), schedules: [], reminders: {}, stageEvents: [], resumes: [], versions: [], seq: 1 }
    expect(importBackup(JSON.stringify(snapshot)).ok).toBe(true)
  })
})
