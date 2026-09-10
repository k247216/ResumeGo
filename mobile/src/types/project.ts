export type JobProjectStatus = 'active' | 'archived'

export type TargetStage =
  | 'applied' | 'exam' | 'interview' | 'hr' | 'offer'
  | 'pool' | 'screened_out' | 'rejected' | 'closed'

/** 结果标记与流程阶段分离：阶段表示当前推进位置，结果表示为什么结束。 */
export type TargetOutcome =
  | 'pool'
  | 'exam_failed'
  | 'interview_failed'
  | 'hr_failed'
  | 'resume_failed'
  | 'rejected'
  | 'closed'

/** 流程主线顺序 */
export const TARGET_STAGE_ORDER: readonly TargetStage[] = [
  'applied', 'exam', 'interview', 'hr', 'offer',
] as const

/** 结果态：进入即锁定 */
export const TARGET_OUTCOME_ORDER: readonly TargetStage[] = [
  'offer', 'pool', 'screened_out', 'rejected', 'closed',
] as const

export const TARGET_STAGE_LABELS: Record<TargetStage, string> = {
  applied: '投递中',
  exam: '笔试',
  interview: '面试',
  hr: 'HR面',
  offer: '已拿 Offer',
  pool: '泡池子',
  screened_out: '被筛下',
  rejected: '被拒',
  closed: '已放弃',
}

const FLOW_RANK: Record<string, number> = { applied: 1, exam: 2, interview: 3, hr: 4, offer: 5 }
export const TERMINAL_STAGES: readonly TargetStage[] = ['offer', 'pool', 'screened_out', 'rejected', 'closed']

export const TARGET_OUTCOME_LABELS: Record<TargetOutcome, string> = {
  pool: '泡池子',
  exam_failed: '笔试未通过',
  interview_failed: '面试第 N 轮未通过',
  hr_failed: 'HR 面未通过',
  resume_failed: '简历未通过',
  rejected: '已拒绝',
  closed: '已放弃',
}

export function stageFlowRank(stage: TargetStage): number {
  return FLOW_RANK[stage] ?? 0
}

export function isTerminalStage(stage: TargetStage): boolean {
  return TERMINAL_STAGES.includes(stage)
}

export function normalizeTargetStage(value: unknown): TargetStage {
  return [...TARGET_STAGE_ORDER, ...TARGET_OUTCOME_ORDER].includes(value as TargetStage)
    ? (value as TargetStage)
    : 'applied'
}

/** 各阶段的展示色（节点 / 徽章共用） */
export const TARGET_STAGE_COLORS: Record<TargetStage, string> = {
  applied: '#4C6FFF',
  exam: '#F77234',
  interview: '#168B68',
  hr: '#722ED1',
  offer: '#D48806',
  pool: '#989893',
  screened_out: '#D46B08',
  rejected: '#B53C32',
  closed: '#667085',
}

export interface JobProject {
  id: number
  name: string
  status: JobProjectStatus
  stage: TargetStage
  jobDescriptionId: number | null
  resumeVersionId: number | null
  archivedAt: string | null
  stageUpdatedAt: string | null
  industry: string | null
  targetRole: string | null
  location: string | null
  notes: string | null
  /** 每个岗位可独立设置面试轮次；旧数据缺失时按 2 轮兼容。 */
  interviewRounds?: number
  /** 当前进行到第几面；仅当 stage === interview 时有效。 */
  interviewRound?: number
  /** 结束原因，与流程阶段分开保存，避免丢失“第几面挂”等信息。 */
  outcome?: TargetOutcome | null
  outcomeRound?: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateJobProjectRequest {
  name: string
  jobDescriptionId?: number | null
  resumeVersionId?: number | null
}

export interface UpdateJobProjectLinksRequest {
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

export interface UpdateJobProjectStageRequest {
  stage: TargetStage
}

export interface UpdateJobProjectApplicationRequest {
  industry?: string | null
  role?: string | null
  location?: string | null
  notes?: string | null
}

export interface StageEvent {
  id: number
  stage: TargetStage
  occurredAt: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}
