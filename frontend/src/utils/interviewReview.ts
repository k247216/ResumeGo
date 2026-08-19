import type {
  MultiSessionSummaryResponse,
  PerQuestionScore,
  ScoreDetail,
} from '../types/interview'

export type ScoreDimensionKey = 'clarity' | 'relevance' | 'depth' | 'accuracy'

export interface ScoreDimensionSummary {
  key: ScoreDimensionKey
  label: string
  value: number
  color: string
}

export interface ScoreSummary {
  average: number
  displayAverage: string
  dimensions: ScoreDimensionSummary[]
  strongest: ScoreDimensionSummary
  weakest: ScoreDimensionSummary
}

export interface RoundReviewSummary {
  sessionId: number
  personaName: string
  personaTitle: string
  order: number
  completed: boolean
  questionCount: number
  summary: ScoreSummary | null
}

export interface InterviewPlanReviewSummary {
  plan: {
    jobLabel: string
    resumeLabel: string
  }
  rounds: RoundReviewSummary[]
  completedRounds: number
  totalRounds: number
  overall: ScoreSummary | null
  cachedSummary: MultiSessionSummaryResponse | null
}

const dimensions: Array<Omit<ScoreDimensionSummary, 'value'>> = [
  { key: 'clarity', label: '表达清晰度', color: '#3b82f6' },
  { key: 'relevance', label: '岗位相关性', color: '#10b981' },
  { key: 'depth', label: '技术深度', color: '#f59e0b' },
  { key: 'accuracy', label: '回答准确性', color: '#8b5cf6' },
]

export function summarizeQuestionScores(scores: PerQuestionScore[]): ScoreSummary | null {
  if (!scores.length) return null
  const totals = scores.reduce<Record<ScoreDimensionKey, number>>(
    (result, score) => ({
      clarity: result.clarity + Number(score.clarity || 0),
      relevance: result.relevance + Number(score.relevance || 0),
      depth: result.depth + Number(score.depth || 0),
      accuracy: result.accuracy + Number(score.accuracy || 0),
    }),
    { clarity: 0, relevance: 0, depth: 0, accuracy: 0 },
  )
  const summaries = dimensions.map((dimension) => ({
    ...dimension,
    value: roundToOneDecimal(totals[dimension.key] / scores.length),
  }))
  const sorted = [...summaries].sort((left, right) => left.value - right.value)
  const average = roundToOneDecimal(
    summaries.reduce((sum, dimension) => sum + dimension.value, 0) / summaries.length,
  )

  return {
    average,
    displayAverage: average.toFixed(1),
    dimensions: summaries,
    strongest: sorted[sorted.length - 1],
    weakest: sorted[0],
  }
}

export function questionEvaluationAverage(score: ScoreDetail): string {
  return roundToOneDecimal(
    (score.clarity + score.relevance + score.depth + score.accuracy) / 4,
  ).toFixed(1)
}

export function questionEvaluationCopy(score: ScoreDetail): string {
  const weakest = dimensions
    .map((dimension) => ({ label: dimension.label, value: score[dimension.key] }))
    .sort((left, right) => left.value - right.value)[0]
  return `当前最需要加强：${weakest.label}`
}

export function trainingHintForDimension(key: ScoreDimensionKey): string {
  return {
    clarity: '建议练习“背景—动作—结果”三段式表达，把回答控制在 60-90 秒内。',
    relevance: '建议先复述岗位关键词，再把项目经历对齐到岗位要求，避免泛泛介绍。',
    depth: '建议补充技术取舍、故障定位、边界条件和复盘，减少只讲功能实现。',
    accuracy: '建议核实技术名词、指标和个人职责，避免模糊或夸大的表述。',
  }[key]
}

function roundToOneDecimal(value: number): number {
  return Math.round(value * 10) / 10
}
