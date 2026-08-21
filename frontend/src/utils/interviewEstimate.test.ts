import { describe, expect, it } from 'vitest'
import { estimateSessionMinutes, estimateSessionMinutesLabel } from './interviewEstimate'

describe('estimateSessionMinutes', () => {
  it('estimates 3–4 minutes per question', () => {
    expect(estimateSessionMinutes(5)).toEqual({ minMinutes: 15, maxMinutes: 20 })
    expect(estimateSessionMinutes(3)).toEqual({ minMinutes: 9, maxMinutes: 12 })
    expect(estimateSessionMinutes(10)).toEqual({ minMinutes: 30, maxMinutes: 40 })
  })

  it('guards against non-positive input', () => {
    expect(estimateSessionMinutes(0)).toEqual({ minMinutes: 3, maxMinutes: 4 })
  })

  it('formats the estimate label', () => {
    expect(estimateSessionMinutesLabel(5)).toBe('预计 15–20 分钟')
  })
})
