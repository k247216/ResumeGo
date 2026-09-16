import type { StageEvent, TargetStage } from '../types/project'
import { isTerminalStage, normalizeTargetStage, TARGET_STAGE_LABELS } from '../types/project'

/**
 * 求职季报告的节奏维度：数量之外，还有时间。
 * 数据全来自 stageEvents——建目标即记 applied，每次推进都有事件，历史是完整的。
 */

const DAY_MS = 86_400_000

export interface StageStall {
  targetId: number
  targetName: string
  stage: TargetStage
  days: number
}

export interface StagePace {
  /** 投递 → 首次进面的平均天数；没有走完这段路的目标时为 null。 */
  appliedToInterviewDays: number | null
  /** 参与平均的目标数。 */
  sampleCount: number
  /** 进行中的目标里，当前阶段停留最久的一个；没有活跃目标或都未满 1 天时为 null。 */
  longestStall: StageStall | null
}

type StageEventWithTarget = StageEvent & { targetId: number }
interface Span { stage: TargetStage; time: number }

export function stagePaceSummary(
  stageEvents: StageEventWithTarget[],
  targets: Array<{ id: number; name: string; status: string; stage: TargetStage }>,
  nowMs: number,
): StagePace {
  // 按目标分组；occurredAt 可能是脏数据，NaN 的时间直接丢弃，不能让一条坏记录毁掉整段计算。
  const byTarget = new Map<number, Span[]>()
  for (const ev of stageEvents) {
    const time = new Date(ev.occurredAt).getTime()
    if (!Number.isFinite(time)) continue
    const list = byTarget.get(ev.targetId) ?? []
    list.push({ stage: ev.stage, time })
    byTarget.set(ev.targetId, list)
  }
  for (const list of byTarget.values()) list.sort((a, b) => a.time - b.time)

  // 投递 → 首次进面：同一目标内取最早的 applied 和最早的 interview 之差。
  const samples: number[] = []
  for (const spans of byTarget.values()) {
    const applied = spans.find((s) => s.stage === 'applied')
    const interview = spans.find((s) => s.stage === 'interview')
    if (!applied || !interview) continue
    const span = interview.time - applied.time
    if (span < 0) continue
    samples.push(span / DAY_MS)
  }
  const avg = samples.length
    ? Math.round(samples.reduce((sum, d) => sum + d, 0) / samples.length)
    : null

  // 卡得最久：只在「进行中」的目标里找（结束的叫结果，不叫卡）。当前阶段从 target.stage 取真相。
  let longestStall: StageStall | null = null
  for (const target of targets) {
    if (target.status !== 'active') continue
    const stage = normalizeTargetStage(target.stage)
    if (isTerminalStage(stage)) continue
    const spans = byTarget.get(target.id)
    if (!spans?.length) continue
    const last = spans[spans.length - 1]
    const days = Math.floor((nowMs - last.time) / DAY_MS)
    if (days < 1) continue
    if (!longestStall || days > longestStall.days) {
      longestStall = { targetId: target.id, targetName: target.name, stage, days }
    }
  }

  return { appliedToInterviewDays: avg, sampleCount: samples.length, longestStall }
}

/** 报告里的一句话节奏文案；没有可说的就返回空串（模板按空渲染跳过）。 */
export function stagePaceLine(pace: StagePace): string {
  const parts: string[] = []
  if (pace.appliedToInterviewDays != null) {
    parts.push(`从投递到进面平均 ${pace.appliedToInterviewDays} 天`)
  }
  if (pace.longestStall) {
    parts.push(`${pace.longestStall.targetName} 已在${TARGET_STAGE_LABELS[pace.longestStall.stage]}阶段停了 ${pace.longestStall.days} 天`)
  }
  return parts.join('；')
}
