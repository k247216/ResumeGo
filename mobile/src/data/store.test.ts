import { beforeEach, describe, expect, it } from 'vitest'
import {
  createSchedule, createTarget, currentVersionOf, deleteResume, deleteSchedule, deleteTarget, getReminder, getResume, importBackup, importResume,
  interviewRoundOf, interviewRoundsOf, listResumes, listSchedules, listTargets, reopenTarget, resetWorkspace,
  setInterviewRound, setInterviewRounds,
  setReminder, setStage, setTargetOutcome, setVersionNote, stageEventsOf, linkResume,
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

  it('拿到 Offer 后锁定面试轮次和结果标记', () => {
    const t = createTarget('Offer 终态锁定测试')
    expect(setStage(t.id, 'offer').ok).toBe(true)

    setInterviewRounds(t.id, 5)
    setInterviewRound(t.id, 4)
    setTargetOutcome(t.id, 'pool')

    expect(interviewRoundsOf(t)).toBe(2)
    expect(interviewRoundOf(t)).toBe(1)
    expect(t.stage).toBe('offer')
    expect(t.outcome).toBeNull()
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

describe('简历版本备注', () => {
  it('导入时不再预填占位备注', async () => {
    const resume = await importResume('备注初值', new File(['a'], 'a.pdf', { type: 'application/pdf' }))
    expect(currentVersionOf(resume.id)?.note).toBeNull()
  })

  it('写入会去掉首尾空白，全空白等同于清空', async () => {
    const resume = await importResume('备注编辑', new File(['a'], 'a.pdf', { type: 'application/pdf' }))
    const ver = currentVersionOf(resume.id)!
    setVersionNote(resume.id, ver.id, '  投字节后端 2 面版  ')
    expect(currentVersionOf(resume.id)?.note).toBe('投字节后端 2 面版')
    setVersionNote(resume.id, ver.id, '   ')
    expect(currentVersionOf(resume.id)?.note).toBeNull()
  })

  it('不接受别的简历的版本号，避免串改他人备注', async () => {
    const mine = await importResume('甲', new File(['a'], 'a.pdf', { type: 'application/pdf' }))
    const other = await importResume('乙', new File(['b'], 'b.pdf', { type: 'application/pdf' }))
    setVersionNote(mine.id, other.currentVersionId!, '越界写入')
    expect(currentVersionOf(mine.id)?.note).toBeNull()
    expect(currentVersionOf(other.id)?.note).toBeNull()
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

function futureSchedule(title: string, jobProjectId: number | null) {
  return createSchedule({
    title, eventType: 'interview',
    startTime: new Date(Date.now() + 5400_000).toISOString(),
    endTime: null, notes: null, jobDescriptionId: null, jobProjectId,
  })
}

describe('删除与锁定的连带影响', () => {
  it('删除求职目标会连带移除它的日程和提醒', () => {
    const target = createTarget('级联删除测试')
    const event = futureSchedule('级联删除测试 一面', target.id)
    setReminder(event.id, 30)

    const { removedScheduleIds } = deleteTarget(target.id)

    expect(removedScheduleIds).toEqual([event.id])
    expect(listSchedules().some((e) => e.id === event.id)).toBe(false)
    expect(getReminder(event.id)).toBe(0)
  })

  it('终态锁定后无法再改结果标记，但解锁可以', () => {
    const target = createTarget('解锁测试')
    setStage(target.id, 'interview')
    setStage(target.id, 'offer')

    expect(setTargetOutcome(target.id, 'pool').ok).toBe(false)

    const reopened = reopenTarget(target.id)
    expect(reopened.ok).toBe(true)
    expect(reopened.stage).toBe('interview')
    expect(target.stage).toBe('interview')
    expect(setStage(target.id, 'hr').ok).toBe(true)
  })
})

describe('清空与恢复', () => {
  it('清空工作区不留任何记录（不再回落到演示数据）', async () => {
    createTarget('待清空目标')
    futureSchedule('待清空日程', null)
    await importResume('待清空简历', new File(['x'], 'x.md', { type: 'text/markdown' }))

    expect(await resetWorkspace()).toMatchObject({ ok: true })

    expect(listTargets()).toHaveLength(0)
    expect(listSchedules()).toHaveLength(0)
    expect(listResumes()).toHaveLength(0)
  })

  it('非法备份被拒绝且不动现有数据', () => {
    createTarget('保留下来的目标')
    const before = listTargets().length

    expect(importBackup('{"targets":"不是数组","schedules":[]}').ok).toBe(false)
    expect(listTargets()).toHaveLength(before)
  })

  it('恢复低 seq 的备份后新建记录不会撞上已有 id', () => {
    const existing = createTarget('已有目标')
    const snapshot = {
      targets: [{ ...existing }], stageEvents: [], schedules: [], resumes: [], versions: [], reminders: {}, seq: 1,
    }

    expect(importBackup(JSON.stringify(snapshot)).ok).toBe(true)
    const created = createTarget('恢复后新建')
    expect(created.id).toBeGreaterThan(existing.id)
  })
})
