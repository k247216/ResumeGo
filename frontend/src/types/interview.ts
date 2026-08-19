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

export interface CreateInterviewPlanRequest {
  resumeVersionId: number
  jobDescriptionId: number
  questionCount: number
  personaIds: number[]
  focusTags?: string[]
  supplement?: string
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
