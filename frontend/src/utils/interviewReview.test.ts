import { describe, expect, it } from 'vitest'
import type { PerQuestionScore, ScoreDetail } from '../types/interview'
import {
  questionEvaluationAverage,
  questionEvaluationCopy,
  summarizeQuestionScores,
  trainingHintForDimension,
} from './interviewReview'

describe('summarizeQuestionScores', () => {
  it('averages each dimension and identifies strongest and weakest dimensions', () => {
    const scores: PerQuestionScore[] = [
      { questionIndex: 1, questionText: 'Q1', clarity: 8, relevance: 7, depth: 5, accuracy: 9 },
      { questionIndex: 2, questionText: 'Q2', clarity: 6, relevance: 9, depth: 7, accuracy: 8 },
    ]

    expect(summarizeQuestionScores(scores)).toMatchObject({
      average: 7.4,
      displayAverage: '7.4',
      strongest: { key: 'evidence', value: 8.5 },
      weakest: { key: 'depth', value: 6 },
    })
  })

  it('returns null when no evaluated answers exist', () => {
    expect(summarizeQuestionScores([])).toBeNull()
  })
})

describe('question review copy', () => {
  const score: ScoreDetail = { clarity: 8, relevance: 7, depth: 4, accuracy: 9 }

  it('calculates a stable one-decimal answer score', () => {
    expect(questionEvaluationAverage(score)).toBe('7.0')
  })

  it('points to the weakest dimension and returns its training hint', () => {
    expect(questionEvaluationCopy(score)).toBe('当前最需要加强：技术深度')
    expect(trainingHintForDimension('depth')).toContain('技术取舍')
  })
})
