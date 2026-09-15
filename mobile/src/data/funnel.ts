import type { JobProject, StageEvent, TargetStage } from '../types/project'
import type { ScheduleEvent } from '../types/schedule'
import { isTerminalStage, normalizeTargetStage, stageFlowRank, TARGET_STAGE_LABELS, TARGET_STAGE_ORDER, TARGET_OUTCOME_ORDER } from '../types/project'

export interface FunnelRow {
  stage: TargetStage
  label: string
  /** 到达过该阶段的目标数（含后来被拒/放弃的） */
  reached: number
  /** 相对上一主线阶段的转化率（%）；首行没有上游，为 null */
  rate: number | null
}
export interface FunnelStats {
  total: number
  /** 未归档的目标数（与「我的」页的「进行中」同口径） */
  active: number
  /** 当前已处于终态（offer/泡池/筛下/拒/放弃）的目标数 */
  settled: number
  archived: number
  rows: FunnelRow[]
  outcomes: Array<{ stage: TargetStage; label: string; count: number }>
}

/**
 * 求职漏斗。「到达某阶段」的证据有三个来源：
 * ① 阶段记录；② 当前阶段兜底（覆盖没有阶段记录的老数据）；
 * ③ 关联日程的类型（有笔试日程就算到过笔试）——用户完全可能约了笔试却没推阶段，
 * 只看阶段记录会把上游低估、算出 133% 这种荒谬转化率。
 */
export function computeFunnel(
  targets: readonly JobProject[],
  stageEvents: readonly (StageEvent & { targetId: number })[],
  schedules: readonly ScheduleEvent[] = [],
): FunnelStats {
  const reached = new Map<TargetStage, Set<number>>(TARGET_STAGE_ORDER.map((s) => [s, new Set<number>()]))
  for (const t of targets) {
    const cur = normalizeTargetStage(t.stage)
    const curRank = stageFlowRank(cur)
    for (const stage of TARGET_STAGE_ORDER) {
      const rank = stageFlowRank(stage)
      // 创建即投递：applied 人人到达；其余主线阶段按「当前阶段的位次」兜底。
      if (rank === 1 || (curRank > 0 && rank <= curRank)) reached.get(stage)!.add(t.id)
    }
  }
  for (const ev of stageEvents) {
    reached.get(normalizeTargetStage(ev.stage))?.add(ev.targetId)
  }
  for (const ev of schedules) {
    if (ev.jobProjectId == null) continue
    // 日程类型到阶段证据的映射：笔试日程→笔试；面试日程→面试，标题含「HR 面」的算 HR 面。跟进/其他不进漏斗。
    if (ev.eventType === 'exam') reached.get('exam')?.add(ev.jobProjectId)
    else if (ev.eventType === 'interview') {
      reached.get('interview')?.add(ev.jobProjectId)
      if (/HR\s*面/i.test(ev.title)) reached.get('hr')?.add(ev.jobProjectId)
    }
  }

  const rows: FunnelRow[] = []
  let prev = 0
  for (const stage of TARGET_STAGE_ORDER) {
    const count = reached.get(stage)!.size
    rows.push({ stage, label: TARGET_STAGE_LABELS[stage], reached: count, rate: prev > 0 ? Math.round((count / prev) * 100) : null })
    prev = count
  }

  return {
    total: targets.length,
    active: targets.filter((t) => t.status === 'active').length,
    settled: targets.filter((t) => isTerminalStage(normalizeTargetStage(t.stage))).length,
    archived: targets.filter((t) => t.status === 'archived').length,
    rows,
    outcomes: TARGET_OUTCOME_ORDER.map((stage) => ({
      stage,
      label: TARGET_STAGE_LABELS[stage],
      count: targets.filter((t) => normalizeTargetStage(t.stage) === stage).length,
    })),
  }
}
