export type JobProjectStatus = 'active' | 'archived'

export type TargetStage =
  | 'applied' | 'exam' | 'interview' | 'hr' | 'offer'
  | 'pool' | 'screened_out' | 'rejected' | 'closed'

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
