import { describe, expect, it } from 'vitest'
import { buildTargetInterviewLocation } from './interviewRoute'

describe('buildTargetInterviewLocation', () => {
  it('serializes a complete target interview context', () => {
    expect(buildTargetInterviewLocation({ targetId: 2, versionId: 3, jobId: 4 })).toEqual({
      name: 'interview',
      query: { from: 'target', targetId: '2', versionId: '3', jobId: '4' },
    })
  })

  it('rejects incomplete or invalid context', () => {
    expect(() => buildTargetInterviewLocation({ targetId: 2, versionId: 0, jobId: 4 })).toThrow('完整的求职目标上下文')
    expect(() => buildTargetInterviewLocation({ targetId: null, versionId: 3, jobId: 4 })).toThrow('完整的求职目标上下文')
  })
})
