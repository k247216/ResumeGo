import { describe, expect, it } from 'vitest'
import { RESUME_MARKS, resumeMarkOf } from './resumeMark'

describe('简历身份图标', () => {
  it('提供同一视觉系列的八种稳定变体', () => {
    expect(RESUME_MARKS).toHaveLength(8)
    expect(new Set(RESUME_MARKS).size).toBe(8)
    expect(resumeMarkOf(12)).toBe(resumeMarkOf(12))
    expect(RESUME_MARKS).toContain(resumeMarkOf(99))
  })
})
