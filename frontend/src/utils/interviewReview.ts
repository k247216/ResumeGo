import type {
  MultiSessionSummaryResponse,
  PerQuestionScore,
  ScoreDetail,
} from '../types/interview'

export type ScoreDimensionKey = 'clarity' | 'relevance' | 'depth' | 'structure' | 'evidence'

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
  { key: 'structure', label: '回答结构', color: '#8b5cf6' },
  { key: 'evidence', label: '证据具体性', color: '#ef8f35' },
]

function scoreValue(score: PerQuestionScore | ScoreDetail, key: ScoreDimensionKey): number {
  if (key === 'evidence') return Number(score.evidence || score.accuracy || 0)
  return Number(score[key] || 0)
}

export function summarizeQuestionScores(scores: PerQuestionScore[]): ScoreSummary | null {
  if (!scores.length) return null
  const summaries = dimensions.flatMap((dimension) => {
    const values = scores.map((score) => scoreValue(score, dimension.key)).filter((value) => value > 0)
    if (!values.length) return []
    return [{
      ...dimension,
      value: roundToOneDecimal(values.reduce((sum, value) => sum + value, 0) / values.length),
    }]
  })
  if (!summaries.length) return null
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
  const values = dimensions.map((dimension) => scoreValue(score, dimension.key)).filter((value) => value > 0)
  if (!values.length) return '—'
  return roundToOneDecimal(values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(1)
}

export function questionEvaluationCopy(score: ScoreDetail): string {
  const weakest = dimensions
    .map((dimension) => ({ label: dimension.label, value: scoreValue(score, dimension.key) }))
    .filter((dimension) => dimension.value > 0)
    .sort((left, right) => left.value - right.value)[0]
  return weakest ? `当前最需要加强：${weakest.label}` : '当前还没有足够的维度评分'
}

export function trainingHintForDimension(key: ScoreDimensionKey): string {
  return {
    clarity: '建议练习“背景—动作—结果”三段式表达，把回答控制在 60-90 秒内。',
    relevance: '建议先复述岗位关键词，再把项目经历对齐到岗位要求，避免泛泛介绍。',
    depth: '建议补充技术取舍、故障定位、边界条件和复盘，减少只讲功能实现。',
    structure: '建议按背景—动作—结果组织回答，先给结论再补关键细节。',
    evidence: '建议补充可核实的个人动作、结果指标和真实项目证据。',
  }[key]
}

function roundToOneDecimal(value: number): number {
  return Math.round(value * 10) / 10
}
