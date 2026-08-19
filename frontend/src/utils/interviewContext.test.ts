import { describe, expect, it } from 'vitest'
import { filterTargetInterviewRecords } from './interviewContext'

describe('filterTargetInterviewRecords', () => {
  const records = [
    { id: 'current', jobDescriptionId: 6, resumeVersionId: 21 },
    { id: 'other-version', jobDescriptionId: 6, resumeVersionId: 22 },
    { id: 'other-job', jobDescriptionId: 7, resumeVersionId: 21 },
    { id: 'legacy', jobDescriptionId: null, resumeVersionId: null },
  ]

  it('keeps only records created for the exact target materials', () => {
    expect(filterTargetInterviewRecords(records, 6, 21).map((record) => record.id)).toEqual(['current'])
  })

  it('returns no records when the target context is incomplete', () => {
    expect(filterTargetInterviewRecords(records, null, 21)).toEqual([])
  })
})
