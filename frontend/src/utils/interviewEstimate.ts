/**
 * 单场模拟面试时长估算：以每题 3–4 分钟计。
 * 这是文档化的估算公式（用于大厅「预计 N–M 分钟」），不是真实计时数据。
 */
export interface SessionMinutesEstimate {
  minMinutes: number
  maxMinutes: number
}

export function estimateSessionMinutes(questionCount: number): SessionMinutesEstimate {
  const count = Math.max(1, Math.floor(questionCount))
  return { minMinutes: count * 3, maxMinutes: count * 4 }
}

export function estimateSessionMinutesLabel(questionCount: number): string {
  const { minMinutes, maxMinutes } = estimateSessionMinutes(questionCount)
  return `预计 ${minMinutes}–${maxMinutes} 分钟`
}
