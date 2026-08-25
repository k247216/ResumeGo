import type { ApiResponse } from './resume'

export type InterviewStatus =
  | 'READY'
  | 'ASKING'
  | 'WAITING_ANSWER'
  | 'EVALUATING'
  | 'NEXT_QUESTION'
  | 'SUMMARIZING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | string

export interface InterviewerPersona {
  id: number
  name: string
  title: string
  style: string
  avatar: string
  type: 'preset' | 'custom'
  userId?: number | null
  sortOrder?: number
  createdAt?: string
}

export interface StartInterviewRequest {
  resumeVersionId: number
  jobDescriptionId: number
  questionCount: number
  personaId: number
}

/** 训练模式：三种且仅三种；创建后不可修改 */
export type InterviewMode = 'ROLE_BASED' | 'KNOWLEDGE_TRAINING' | 'EXPERIENCE_SIMULATION'

/** 岗位模拟：Pipeline（jobProjectId）、Resume Version 与 persona 均为用户显式选择 */
export interface RoleBasedPlanRequest {
  mode: 'ROLE_BASED'
  jobProjectId: number
  resumeVersionId: number
  questionCount: number
  personaIds: number[]
  focusTags?: string[]
  supplement?: string
}

/** 知识训练：只要求当前用户的 Knowledge Document，不强制岗位/简历 */
export interface KnowledgeTrainingPlanRequest {
  mode: 'KNOWLEDGE_TRAINING'
  knowledgeDocumentIds: number[]
  difficulty?: string
  questionCount: number
  focusTags?: string[]
  supplement?: string
}

/** 面经模拟：只使用本地题集；AI 追问单独标源 */
export interface ExperienceSimulationPlanRequest {
  mode: 'EXPERIENCE_SIMULATION'
  questionSetId: number
  personaIds?: number[]
  followUpIntensity?: string
  questionCount: number
  focusTags?: string[]
  supplement?: string
}

/** 按 mode 键控的判别联合：不允许全可选大请求 */
export type CreateInterviewPlanRequest =
  | RoleBasedPlanRequest
  | KnowledgeTrainingPlanRequest
  | ExperienceSimulationPlanRequest

export type QuestionSetSourceType = 'USER_MANUAL' | 'IMPORTED_EXPERIENCE' | 'GENERATED_PRACTICE'

export interface InterviewQuestionSetItem {
  positionIndex: number
  questionText: string
}

export interface InterviewQuestionSetResponse {
  id: number
  title: string
  sourceType: QuestionSetSourceType
  sourceNote?: string | null
  archived: boolean
  archivedAt?: string | null
  createdAt?: string
  updatedAt?: string
  /** 列表模式为 null；详情模式返回有序题目 */
  items?: InterviewQuestionSetItem[] | null
}

export interface InterviewQuestionSetRequest {
  title: string
  sourceType: QuestionSetSourceType
  sourceNote?: string
  questions: string[]
}

export interface InterviewPlanRound {
  sessionId: number
  personaId: number
  personaName: string
  personaTitle: string
  roundOrder: number
  status: InterviewStatus
  currentQuestionIndex: number
  totalQuestions: number
  completed: boolean
}

export interface InterviewPlanResponse {
  planId: number
  /** 训练模式（创建后不可变）；历史数据回填 ROLE_BASED */
  mode?: InterviewMode
  contextContractVersion?: string
  /** 不可变开始上下文快照：历史回放展示其中的名称与版本号，不用当前数据覆盖 */
  startContextSnapshot?: Record<string, unknown> | null
  resumeVersionId: number
  jobDescriptionId: number
  title: string
  questionCount: number
  focusTags: string[]
  supplement?: string | null
  summary?: MultiSessionSummaryResponse | null
  summaryGeneratedAt?: string | null
  rounds: InterviewPlanRound[]
  completed: boolean
  createdAt?: string
  updatedAt?: string
}

export interface InterviewQuestion {
  questionIndex: number
  questionText: string
  questionType?: string | null
}

export interface PerQuestionScore {
  questionIndex: number
  questionText: string
  clarity: number
  relevance: number
  depth: number
  accuracy: number
}

export interface InterviewStatusResponse {
  sessionId: number
  status: InterviewStatus
  currentQuestionIndex: number
  totalQuestions: number
  currentQuestion?: InterviewQuestion | null
  summaryJson?: string | null
  completed: boolean
  perQuestionScores?: PerQuestionScore[] | null
  personaName?: string | null
  personaTitle?: string | null
}

export interface SubmitAnswerRequest {
  answerText: string
}

export interface EvaluationSummary {
  strengths?: string[] | null
  weaknesses?: string[] | null
  suggestions?: string[] | null
  referenceAnswer?: string | null
  score?: ScoreDetail | null
}

export interface ScoreDetail {
  clarity: number
  relevance: number
  depth: number
  accuracy: number
}

export interface SubmitAnswerResponse {
  sessionId: number
  status: InterviewStatus
  currentQuestionIndex: number
  totalQuestions: number
  nextQuestion?: InterviewQuestion | null
  evaluation?: EvaluationSummary | null
  completed: boolean
  retryable: boolean
}

export type InterviewApiResponse<T> = ApiResponse<T>

export interface SessionHistoryResponse {
  sessionId: number
  items: SessionHistoryItem[]
}

export interface MultiSessionSummaryRequest {
  sessionIds: number[]
}

export interface MultiSessionSummaryResponse {
  overallSummary: string
  overallScore: number
  crossStrengths: string[]
  crossWeaknesses: string[]
  suggestions: string[]
  sessions: SessionBrief[]
}

export interface SessionBrief {
  sessionId: number
  personaName: string
  personaTitle: string
  totalQuestions: number
}

export interface SessionHistoryItem {
  questionIndex: number
  questionText: string
  questionType: string
  answerText: string
  evaluation: EvaluationSummary | null
}

// ========== 成长趋势 ==========

export interface GrowthSnapshot {
  resumeVersionId: number
  versionLabel: string
  representativePlanId: number
  completedAt: string
  interviewCount: number
  dimensions: GrowthDimensions
  summary: string
}

export interface GrowthDimensions {
  clarity: number
  relevance: number
  depth: number
  accuracy: number
}

export interface GrowthReport {
  resumeId: number
  jobDescriptionId: number
  jobTitle: string
  companyName: string
  snapshots: GrowthSnapshot[]
  changes: GrowthDimensions
}
