import { describe, expect, it } from 'vitest'
import { buildTargetInterviewLocation } from './interviewRoute'

describe('buildTargetInterviewLocation', () => {
  it('serializes a complete target interview context', () => {
    expect(buildTargetInterviewLocation({ targetId: 2, versionId: 3, jobId: 4 })).toEqual({
      name: 'interview',
      query: { from: 'target', targetId: '2', versionId: '3', jobId: '4' },
    })
  })

  it('omits missing ids so partial context stays bound to the target', () => {
    expect(buildTargetInterviewLocation({ targetId: 2, versionId: 0, jobId: 4 })).toEqual({
      name: 'interview',
      query: { from: 'target', targetId: '2', jobId: '4' },
    })
    expect(buildTargetInterviewLocation({ targetId: null, versionId: 3, jobId: 4 })).toEqual({
      name: 'interview',
      query: { from: 'target', versionId: '3', jobId: '4' },
    })
    expect(buildTargetInterviewLocation({})).toEqual({
      name: 'interview',
      query: { from: 'target' },
    })
  })
})
