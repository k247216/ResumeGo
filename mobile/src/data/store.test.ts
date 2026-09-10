import { beforeEach, describe, expect, it } from 'vitest'
import {
  createSchedule, createTarget, currentVersionOf, deleteResume, deleteSchedule, getReminder, getResume, importBackup, importResume,
  interviewRoundOf, interviewRoundsOf, listTargets, setInterviewRound, setInterviewRounds,
  setReminder, setStage, setTargetOutcome, stageEventsOf, linkResume,
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

  it('每个求职目标可以独立设置面试轮次', () => {
    const t = createTarget('轮次配置测试')
    expect(interviewRoundsOf(t)).toBe(2)
    setInterviewRounds(t.id, 4)
    expect(interviewRoundsOf(t)).toBe(4)
    setInterviewRounds(t.id, 99)
    expect(interviewRoundsOf(t)).toBe(8)
  })

  it('设置面试轮次后可以记录当前第几面', () => {
    const t = createTarget('当前面试轮次测试')
    setInterviewRounds(t.id, 4)
    setInterviewRound(t.id, 3)

    expect(interviewRoundOf(t)).toBe(3)
  })

  it('求职目标可以记录具体结果标记', () => {
    const t = createTarget('结果标记测试')
    setTargetOutcome(t.id, 'interview_failed', 2)

    expect(t.outcome).toBe('interview_failed')
    expect(t.outcomeRound).toBe(2)
    expect(t.stage).toBe('screened_out')
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

describe('简历资产删除', () => {
  it('删除简历时同步解除求职目标绑定', async () => {
    const target = createTarget('简历删除绑定测试')
    const resume = await importResume('待删除简历', new File(['resume'], 'resume.pdf', { type: 'application/pdf' }))
    const version = currentVersionOf(resume.id)
    expect(version).not.toBeNull()
    linkResume(target.id, version!.id)

    await deleteResume(resume.id)

    expect(getResume(resume.id)).toBeUndefined()
    expect(listTargets().find((item) => item.id === target.id)?.resumeVersionId).toBeNull()
  })
})
