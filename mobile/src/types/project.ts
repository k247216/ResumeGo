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

/**
 * 面经：别人家公司的真实面试记录，粘贴进来重新排版成好读的纸面文章。
 * rounds / questionCount 在解析时算好存下来，列表页不必重新解析原文。
 */
export interface InterviewLog {
  id: number
  title: string
  /** 关联的求职目标；面经常常先于目标存在，允许不关联。 */
  targetId: number | null
  /** 解析出的轮次数（一面/二面/hr面…的段落数）。 */
  rounds: number
  /** 解析出的面试问题数。 */
  questionCount: number
  /** 清洗排版后的正文（自产 HTML，纯文本已转义，不含外部标签）。 */
  contentHtml: string
  /** 解析出的问题原文，供战前速览直接引用。 */
  questions: string[]
  createdAt: string
  updatedAt: string
}

/**
 * 里程碑：一条投递线上值得留证的瞬间——约面邮件、笔试通过、offer 电话截图。
 * 截图本体存 IndexedDB（fileStore），记录里只存 key；JSON 备份不含图（与简历文件同理）。
 */
export type MilestoneKind = 'invite' | 'exam' | 'interview' | 'offer' | 'reject' | 'moment'

export const MILESTONE_KINDS: Record<MilestoneKind, { label: string; color: string }> = {
  invite: { label: '约面通知', color: '#4C6FFF' },
  exam: { label: '笔试', color: '#F77234' },
  interview: { label: '面试记录', color: '#168B68' },
  offer: { label: 'Offer', color: '#D48806' },
  reject: { label: '结果通知', color: '#B53C32' },
  moment: { label: '其他时刻', color: '#989893' },
}

export function normalizeMilestoneKind(value: unknown): MilestoneKind {
  return typeof value === 'string' && value in MILESTONE_KINDS ? (value as MilestoneKind) : 'moment'
}

export interface Milestone {
  id: number
  targetId: number
  kind: MilestoneKind
  title: string
  note: string
  /** 截图在 fileStore 里的 key 列表。 */
  images: string[]
  occurredAt: string
  createdAt: string
}
