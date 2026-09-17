import { beforeEach, describe, expect, it } from 'vitest'
import {
  backupSummaryOf, createInterviewLog, createSchedule, createTarget, currentVersionOf, deleteInterviewLog, deleteResume, deleteSchedule, deleteTarget, exportBackup, flushPersist, getReminder, getResume, importBackup, importResume,
  interviewRoundOf, interviewRoundsOf, lastBackupAgeDays, listInterviewLogs, listResumes, listReviews, listReviewTags, listSchedules, listTargets, markBackupNow, reopenTarget, resetWorkspace, restoreTarget,
  setInterviewRound, setInterviewRounds,
  setReminder, setReviewTags, setScheduleReview, setStage, setTargetOutcome, setVersionNote, snapshotTarget, stageEventsOf, updateInterviewLog, updateSchedule, linkResume,
  recordScheduleResult,
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

function scheduleOf(title: string, startTime: string, jobProjectId: number | null = null) {
  return createSchedule({
    title, eventType: 'interview', startTime,
    endTime: null, notes: null, jobDescriptionId: null, jobProjectId,
  })
}

describe('复盘心得', () => {
  it('心得与备注各存各的，写心得不会覆盖赛前备注', () => {
    const ev = scheduleOf('心得与备注分离', '2026-03-01T02:00:00.000Z')
    updateSchedule(ev.id, { notes: '面试官：王工' })

    setScheduleReview(ev.id, '算法题没答上来')

    const saved = listSchedules().find((item) => item.id === ev.id)
    expect(saved?.notes).toBe('面试官：王工')
    expect(saved?.review).toBe('算法题没答上来')
  })

  it('清空或只留空格时心得字段回到 null，不会存一条假心得', () => {
    const ev = scheduleOf('清空心得', '2026-03-02T02:00:00.000Z')
    setScheduleReview(ev.id, '写了两句')
    setScheduleReview(ev.id, '   ')

    expect(listSchedules().find((item) => item.id === ev.id)?.review).toBeNull()
    expect(listReviews().some((item) => item.id === ev.id)).toBe(false)
  })

  it('复盘列表只收写过心得的日程，并按最近一场排在最前', () => {
    const t = createTarget('复盘归组公司')
    const early = scheduleOf('早的一场', '2026-03-03T02:00:00.000Z', t.id)
    const late = scheduleOf('晚的一场', '2026-03-20T02:00:00.000Z', t.id)
    const unwritten = scheduleOf('没写心得的一场', '2026-03-25T02:00:00.000Z', t.id)
    setScheduleReview(early.id, '一面基础题')
    setScheduleReview(late.id, '三面系统设计')

    const ids = listReviews().map((item) => item.id)
    expect(ids).not.toContain(unwritten.id)
    expect(ids.indexOf(late.id)).toBeLessThan(ids.indexOf(early.id))
  })
})

describe('复盘标签', () => {
  it('setReviewTags 去重、裁剪空串、上限 12 个', () => {
    const ev = scheduleOf('标签去重', '2026-04-01T02:00:00.000Z')
    setReviewTags(ev.id, ['算法', '算法', '  ', '项目深挖'])
    const saved = listSchedules().find((item) => item.id === ev.id)
    expect(saved?.reviewTags).toEqual(['算法', '项目深挖'])
    setReviewTags(ev.id, Array.from({ length: 15 }, (_, i) => `t${i}`))
    expect(listSchedules().find((item) => item.id === ev.id)?.reviewTags?.length).toBe(12)
    // 清掉这次测试留下的标签，避免污染后续用例的全局标签汇总
    setReviewTags(ev.id, [])
  })

  it('listReviewTags 汇总所有心得标签并按热度倒序', () => {
    const a = scheduleOf('热度A', '2026-04-02T02:00:00.000Z')
    const b = scheduleOf('热度B', '2026-04-03T02:00:00.000Z')
    setReviewTags(a.id, ['算法', '挂了'])
    setReviewTags(b.id, ['算法', '拿offer'])
    const tags = listReviewTags()
    expect(tags[0]).toBe('算法')
    expect(new Set(tags)).toEqual(new Set(['算法', '挂了', '拿offer']))
    expect(tags).toHaveLength(3)
  })

  it('标签与正文各自独立，清空正文不影响已有标签', () => {
    const ev = scheduleOf('标签独立', '2026-04-04T02:00:00.000Z')
    setScheduleReview(ev.id, '二面凉了')
    setReviewTags(ev.id, ['挂了'])
    setScheduleReview(ev.id, '   ')
    expect(listSchedules().find((item) => item.id === ev.id)?.reviewTags).toEqual(['挂了'])
  })
})

describe('便签底色', () => {
  it('setScheduleReview 可以保存调色板内的底色，不传则不动现有值', () => {
    const a = scheduleOf('底色保存', '2026-04-05T02:00:00.000Z')
    setScheduleReview(a.id, '<p>带底色的心得</p>', '#5fb894')
    const saved = listSchedules().find((item) => item.id === a.id)
    expect(saved?.reviewColor).toBe('#5fb894')
    setScheduleReview(a.id, '<p>改了正文</p>')
    expect(listSchedules().find((item) => item.id === a.id)?.reviewColor).toBe('#5fb894')
  })

  it('显式传 null 回到「跟随类型色」，调色板之外的值一律丢弃', () => {
    const a = scheduleOf('底色清洗', '2026-04-06T02:00:00.000Z')
    setScheduleReview(a.id, '<p>x</p>', '#5fb894')
    setScheduleReview(a.id, '<p>xy</p>', null)
    expect(listSchedules().find((item) => item.id === a.id)?.reviewColor).toBeNull()
    setScheduleReview(a.id, '<p>xyz</p>', 'javascript:alert(1)')
    expect(listSchedules().find((item) => item.id === a.id)?.reviewColor).toBeNull()
  })
})

describe('复盘联动推进', () => {
  it('面试通过且还有下一面：只推进轮次，不进下一阶段', () => {
    const t = createTarget('轮次推进测试')
    setStage(t.id, 'interview')
    setInterviewRounds(t.id, 3)
    const ev = scheduleOf('轮次推进测试 一面', '2026-03-01T02:00:00.000Z', t.id)

    const res = recordScheduleResult(ev.id, true)

    expect(res.ok).toBe(true)
    expect(res.stage).toBe('interview')
    expect(res.round).toBe(2)
    expect(interviewRoundOf(t)).toBe(2)
    expect(t.stage).toBe('interview')
  })

  it('最后一面通过：推进到 HR 面', () => {
    const t = createTarget('终面推进测试')
    setStage(t.id, 'interview')
    setInterviewRounds(t.id, 2)
    setInterviewRound(t.id, 2)
    const ev = scheduleOf('终面推进测试 二面', '2026-03-02T02:00:00.000Z', t.id)

    const res = recordScheduleResult(ev.id, true)

    expect(res.ok).toBe(true)
    expect(res.stage).toBe('hr')
    expect(t.stage).toBe('hr')
  })

  it('笔试通过会把落后的状态追平：目标还在投递中也要直接进面试', () => {
    const t = createTarget('状态追平测试')
    const ev = scheduleOf('状态追平测试 笔试', '2026-03-03T02:00:00.000Z', t.id)
    updateSchedule(ev.id, { eventType: 'exam' })

    const res = recordScheduleResult(ev.id, true)

    expect(res.ok).toBe(true)
    expect(res.stage).toBe('interview')
    expect(t.stage).toBe('interview')
  })

  it('挂了：按日程类型标记结果并进入终态锁定', () => {
    const t = createTarget('挂科标记测试')
    setInterviewRounds(t.id, 3)
    setStage(t.id, 'interview')
    const ev = scheduleOf('挂科标记测试 二面', '2026-03-04T02:00:00.000Z', t.id)

    const res = recordScheduleResult(ev.id, false)

    expect(res.ok).toBe(true)
    expect(t.stage).toBe('screened_out')
    expect(t.outcome).toBe('interview_failed')
    expect(t.outcomeRound).toBe(1)
    // 终态锁定：之后任何阶段改动都被拒
    expect(setStage(t.id, 'offer').ok).toBe(false)
  })

  it('未关联目标的日程如实拒绝，不悄悄吞掉', () => {
    const ev = scheduleOf('无主日程', '2026-03-05T02:00:00.000Z')
    const res = recordScheduleResult(ev.id, true)
    expect(res.ok).toBe(false)
    expect(res.message).toContain('未关联')
  })

  it('问询标记一旦写入就持久，旧数据缺省视为没问过', () => {
    const ev = scheduleOf('问询标记', '2026-03-06T02:00:00.000Z')
    expect(listSchedules().find((item) => item.id === ev.id)?.outcomePrompted).toBe(false)
    updateSchedule(ev.id, { outcomePrompted: true })
    expect(listSchedules().find((item) => item.id === ev.id)?.outcomePrompted).toBe(true)
  })
})

describe('备份内容预览与备份时间', () => {
  it('backupSummaryOf 念出备份里有什么：目标/日程/心得/简历/提醒各自计数', () => {
    const t = createTarget('预览计数目标')
    const ev = scheduleOf('预览计数日程', '2026-03-07T02:00:00.000Z', t.id)
    setScheduleReview(ev.id, '写一篇心得用于计数')
    setReminder(ev.id, 30)
    const json = exportBackup()

    const summary = backupSummaryOf(json)

    expect(summary.ok).toBe(true)
    expect(summary.targets).toBe(listTargets().length)
    expect(summary.schedules).toBe(listSchedules().length)
    expect(summary.reviews).toBeGreaterThanOrEqual(1)
    expect(summary.reminders).toBeGreaterThanOrEqual(1)
  })

  it('坏 JSON 与缺关键清单的文件如实拒绝，不给出误导性计数', () => {
    expect(backupSummaryOf('不是JSON').ok).toBe(false)
    expect(backupSummaryOf('{"foo":1}').ok).toBe(false)
  })

  it('备份时间：从未备份返回 null，标记后按天计数', () => {
    resetWorkspace()
    expect(lastBackupAgeDays()).toBeNull()
    markBackupNow()
    expect(lastBackupAgeDays()).toBe(0)
  })
})

describe('阶段误触回退', () => {
  it('退回更早阶段默认被拒，显式允许时可以退回', () => {
    const t = createTarget('回退测试')
    setStage(t.id, 'interview')

    expect(setStage(t.id, 'applied').ok).toBe(false)
    expect(setStage(t.id, 'applied', { allowBackward: true }).ok).toBe(true)
    expect(t.stage).toBe('applied')
  })

  it('快照能撤销误触：阶段、面试轮次与阶段时间轴一起回到改动前', () => {
    const t = createTarget('撤销快照')
    setStage(t.id, 'interview')
    setInterviewRound(t.id, 2)
    const snap = snapshotTarget(t.id)
    expect(snap).not.toBeNull()
    const eventsBefore = stageEventsOf(t.id).length

    setStage(t.id, 'offer')

    expect(restoreTarget(snap!)).toBe(true)
    expect(t.stage).toBe('interview')
    expect(interviewRoundOf(t)).toBe(2)
    expect(stageEventsOf(t.id)).toHaveLength(eventsBefore)
  })

  it('计划已被删除时撤销如实返回失败', () => {
    const t = createTarget('撤销前被删')
    const snap = snapshotTarget(t.id)!
    deleteTarget(t.id)

    expect(restoreTarget(snap)).toBe(false)
  })
})

describe('备份导入的深度卫生处理', () => {
  it('剔除无 id / 重复 id / 坏日期的条目，reminders 只留正的有限分钟数', () => {
    const snapshot = {
      targets: [
        { id: 1, name: '正常目标', status: 'active', stage: 'applied', createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z' },
        { name: '没有 id 的目标', status: 'active', stage: 'applied' },
        { id: 2, name: '重复占位一' },
        { id: 2, name: '重复占位二' },
      ],
      schedules: [
        { id: 10, title: '正常日程', eventType: 'interview', startTime: '2026-02-01T02:00:00.000Z', endTime: null, notes: null, jobDescriptionId: null, jobProjectId: null, createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-01T00:00:00.000Z' },
        { id: 11, title: '开始时间坏掉的日程', startTime: '不是日期' },
      ],
      stageEvents: [
        { id: 12, stage: 'applied', occurredAt: '2026-01-01T00:00:00.000Z', targetId: 1 },
        { id: 13, stage: 'exam', occurredAt: 'bad-date', targetId: 1 },
      ],
      resumes: [],
      versions: [
        { id: 14, resumeId: 1, versionNo: 1, fileName: 'a.pdf', mime: 'application/pdf', size: 1, fileKey: 'k', note: null, createdAt: '2026-01-01T00:00:00.000Z' },
        { id: 15, resumeId: '不行', versionNo: 1, fileName: 'b.pdf', mime: 'application/pdf', size: 1, fileKey: 'k2', note: null, createdAt: '2026-01-01T00:00:00.000Z' },
      ],
      reminders: { 10: 30, 'not-a-number': 5, 11: -3, 12: 1.5, 13: '三十' },
      seq: 1,
    }

    expect(importBackup(JSON.stringify(snapshot)).ok).toBe(true)

    const targetIds = listTargets().map((t) => t.id)
    expect(targetIds).toContain(1)
    // 重复 id 去重：id=2 只留第一条，第二条「重复占位二」被剔除
    expect(targetIds).toContain(2)
    expect(listTargets().filter((t) => t.name === '重复占位二')).toHaveLength(0)
    expect(listSchedules().map((e) => e.id)).toEqual([10])
    expect(getReminder(10)).toBe(30)
    expect(getReminder(11)).toBe(0)
  })

  it('防抖持久化：flushPersist 之后本地存储立刻是最新库', () => {
    const t = createTarget('防抖冲刷目标')
    flushPersist()
    const parsed = JSON.parse(localStorage.getItem('zhida-mobile-db-v2')!)
    expect(parsed.targets.some((x: { id: number }) => x.id === t.id)).toBe(true)
  })

  it('清空工作区后 flush，本地存储里也是空库', async () => {
    await resetWorkspace()
    flushPersist()
    const parsed = JSON.parse(localStorage.getItem('zhida-mobile-db-v2')!)
    expect(parsed.targets).toHaveLength(0)
  })
})

describe('面经库', () => {
  it('创建后可列出（新的在前），更新与删除生效', () => {
    const a = createInterviewLog('字节一面面经', null, '<section class="iv-sec"><h3>一面</h3><p>x</p></section>', 1, ['q1', 'q2', 'q3'])
    const b = createInterviewLog('腾讯面经', null, '<p>y</p>', 1, [])
    const list = listInterviewLogs()
    expect(list[0].id).toBe(b.id)
    expect(list.find((l) => l.id === a.id)?.questionCount).toBe(3)

    updateInterviewLog(a.id, { title: '字节跳动一面面经', targetId: null })
    expect(listInterviewLogs().find((l) => l.id === a.id)?.title).toBe('字节跳动一面面经')

    deleteInterviewLog(b.id)
    expect(listInterviewLogs().some((l) => l.id === b.id)).toBe(false)
    deleteInterviewLog(a.id)
  })

  it('面经计入备份预览，导出导入可完整往返', () => {
    const log = createInterviewLog('往返测试面经', null, '<p>内容</p>', 1, ['一个问题？'])
    const json = exportBackup()
    expect(backupSummaryOf(json).logs).toBeGreaterThanOrEqual(1)

    // 导入自己的导出，面经不丢
    flushPersist()
    const r = importBackup(json)
    expect(r.ok).toBe(true)
    expect(listInterviewLogs().some((l) => l.id === log.id)).toBe(true)
    deleteInterviewLog(log.id)
  })

  it('备份里的坏面经（无标题/无正文）导入时被剔除，不影响其他数据', () => {
    const json = JSON.stringify({
      targets: [], schedules: [], resumes: [], versions: [], reminders: {},
      interviewLogs: [
        { id: 1, title: '正常面经', contentHtml: '<p>x</p>', createdAt: '2026-09-17T00:00:00.000Z', updatedAt: '2026-09-17T00:00:00.000Z' },
        { id: 2, contentHtml: '<p>没标题</p>' },
        { id: 3, title: '没正文' },
      ],
      seq: 3,
    })
    const r = importBackup(json)
    expect(r.ok).toBe(true)
    const list = listInterviewLogs()
    expect(list.some((l) => l.id === 1)).toBe(true)
    expect(list.some((l) => l.id === 2)).toBe(false)
    expect(list.some((l) => l.id === 3)).toBe(false)
    // 清场，不污染其他用例
    for (const l of list) deleteInterviewLog(l.id)
  })
})
