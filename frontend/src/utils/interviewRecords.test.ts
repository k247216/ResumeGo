import { describe, expect, it } from 'vitest'
import type { JobDescription } from '../types/job'
import type { InterviewStatusResponse } from '../types/interview'
import {
  buildInterviewRecords,
  interviewRecordProgress,
  interviewRecordStatus,
  interviewRoundStatus,
  type InterviewPlanContext,
} from './interviewRecords'

function session(
  sessionId: number,
  status: string,
  personaName: string,
): InterviewStatusResponse {
  return {
    sessionId,
    status,
    currentQuestionIndex: status === 'COMPLETED' ? 5 : 2,
    totalQuestions: 5,
    currentQuestion: null,
    completed: status === 'COMPLETED',
    perQuestionScores: [],
    personaName,
    personaTitle: '模拟面试官',
  }
}

const job: JobDescription = {
  id: 8,
  jobTitle: '后端工程师',
  companyName: '示例公司',
  rawText: '岗位说明',
  parsed: null,
  parseStatus: 'succeeded',
  promptVersion: null,
  sourceMeta: null,
  jobType: null,
  createdAt: '2026-08-19T00:00:00Z',
  updatedAt: '2026-08-19T00:00:00Z',
}

function plan(sessionId: number, currentPersonaIndex: number): InterviewPlanContext {
  return {
    planId: '42',
    sessionId,
    resumeVersionId: 12,
    resumeLabel: '主简历 v3',
    jobDescriptionId: 8,
    jobLabel: '示例公司 · 后端工程师',
    personaIds: [4, 5],
    personaNames: ['技术负责人', 'HR'],
    currentPersonaIndex,
  }
}

describe('buildInterviewRecords', () => {
  it('groups plan rounds in plan order and derives a completed record', () => {
    const records = buildInterviewRecords({
      sessions: [session(102, 'COMPLETED', 'HR'), session(101, 'COMPLETED', '技术负责人')],
      plansBySessionId: {
        101: plan(101, 0),
        102: plan(102, 1),
      },
      jobs: [job],
      deletedSessionIds: new Set(),
    })

    expect(records).toHaveLength(1)
    expect(records[0]).toMatchObject({
      id: '42',
      title: '示例公司 · 后端工程师',
      subtitle: '技术负责人 / HR',
      resumeLabel: '主简历 v3',
      completedCount: 2,
      totalCount: 2,
      isCompleted: true,
      isInProgress: false,
      job,
    })
    expect(records[0].sessions.map((item) => item.sessionId)).toEqual([101, 102])
  })

  it('omits locally deleted sessions and keeps legacy sessions separate', () => {
    const records = buildInterviewRecords({
      sessions: [session(3, 'WAITING_ANSWER', '面试官甲'), session(4, 'COMPLETED', '面试官乙')],
      plansBySessionId: {},
      jobs: [],
      deletedSessionIds: new Set([4]),
    })

    expect(records).toHaveLength(1)
    expect(records[0]).toMatchObject({
      id: 'legacy_3',
      title: '本次多轮面试',
      completedCount: 0,
      totalCount: 1,
      isCompleted: false,
      isInProgress: true,
    })
  })
})

describe('interview record presentation state', () => {
  it('distinguishes completed, failed, cancelled, and active rounds', () => {
    expect(interviewRoundStatus(session(1, 'COMPLETED', 'A'))).toBe('completed')
    expect(interviewRoundStatus(session(2, 'FAILED', 'B'))).toBe('failed')
    expect(interviewRoundStatus(session(3, 'CANCELLED', 'C'))).toBe('cancelled')
    expect(interviewRoundStatus(session(4, 'WAITING_ANSWER', 'D'))).toBe('active')
  })

  it('prioritizes failure over cancellation and clamps progress', () => {
    const [record] = buildInterviewRecords({
      sessions: [session(10, 'FAILED', 'A'), session(11, 'CANCELLED', 'B')],
      plansBySessionId: {
        10: plan(10, 0),
        11: plan(11, 1),
      },
      jobs: [job],
      deletedSessionIds: new Set(),
    })

    expect(interviewRecordStatus(record)).toBe('failed')
    expect(interviewRecordProgress({ ...record, completedCount: 9, totalCount: 2 })).toBe(100)
    expect(interviewRecordProgress({ ...record, completedCount: 0, totalCount: 0 })).toBe(0)
  })
})
