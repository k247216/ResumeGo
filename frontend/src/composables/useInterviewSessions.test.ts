import { describe, expect, it } from 'vitest'
import type { InterviewStatusResponse } from '../types/interview'
import { useInterviewSessions } from './useInterviewSessions'

function session(sessionId: number, status = 'READY'): InterviewStatusResponse {
  return {
    sessionId,
    status,
    currentQuestionIndex: 1,
    totalQuestions: 5,
    currentQuestion: null,
    completed: false,
    perQuestionScores: [],
    personaName: `面试官 ${sessionId}`,
    personaTitle: '模拟面试官',
  }
}

describe('useInterviewSessions', () => {
  it('upserts sessions without duplicating an existing session', () => {
    const workspace = useInterviewSessions()

    workspace.upsertSession(session(1))
    workspace.upsertSession({ ...session(1, 'WAITING_ANSWER'), currentQuestionIndex: 2 })

    expect(workspace.sessions.value).toHaveLength(1)
    expect(workspace.sessions.value[0]).toMatchObject({ status: 'WAITING_ANSWER', currentQuestionIndex: 2 })
  })

  it('keeps answer drafts isolated by active session', () => {
    const workspace = useInterviewSessions()
    workspace.sessions.value = [session(1), session(2)]

    workspace.activateSession(1).answerDraft = '第一轮回答'
    workspace.activateSession(2).answerDraft = '第二轮回答'

    expect(workspace.activeState.value.answerDraft).toBe('第二轮回答')
    expect(workspace.getOrCreateSessionState(1).answerDraft).toBe('第一轮回答')
  })

  it('applies partial status updates only to the selected session', () => {
    const workspace = useInterviewSessions()
    workspace.sessions.value = [session(1), session(2)]

    workspace.updateSession(2, { status: 'COMPLETED', completed: true })

    expect(workspace.sessions.value[0].status).toBe('READY')
    expect(workspace.sessions.value[1]).toMatchObject({ status: 'COMPLETED', completed: true })
  })
})
