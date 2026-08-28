import type { InterviewMode } from '../types/interview'

export const INTERVIEW_ENGINE_STATES = ['home', 'setup', 'room', 'review'] as const

export type InterviewEngineState = (typeof INTERVIEW_ENGINE_STATES)[number]

export interface InterviewEngineRouteQuery {
  view?: string | string[]
  sessionId?: string | string[]
  mode?: string | string[]
}

export interface InterviewEngineLocation {
  state: InterviewEngineState
  sessionId: number | null
  mode: InterviewMode | null
}

/**
 * The interview engine has four work states. A session id is required for
 * room/review so a refresh cannot silently open an unrelated conversation.
 */
export function interviewEngineStateFromQuery(query: InterviewEngineRouteQuery): InterviewEngineLocation {
  const view = firstQueryValue(query.view)
  const sessionId = positiveQueryId(query.sessionId)
  if ((view === 'room' || view === 'review') && sessionId != null) {
    return { state: view, sessionId, mode: null }
  }
  if (view === 'setup') {
    const mode = parseInterviewMode(query.mode)
    return mode ? { state: 'setup', sessionId: null, mode } : { state: 'home', sessionId: null, mode: null }
  }
  return { state: 'home', sessionId: null, mode: null }
}

export function interviewEngineStateToQuery(
  state: InterviewEngineState,
  sessionId: number | null = null,
  mode: InterviewMode | null = null,
): Record<string, string> {
  if ((state === 'room' || state === 'review') && sessionId != null) {
    return { view: state, sessionId: String(sessionId) }
  }
  if (state === 'setup' && mode != null) {
    return { view: state, mode }
  }
  return { view: state }
}

function firstQueryValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value
}

function positiveQueryId(value: string | string[] | undefined): number | null {
  const parsed = Number(firstQueryValue(value))
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
}

function parseInterviewMode(value: string | string[] | undefined): InterviewMode | null {
  const mode = firstQueryValue(value)
  return mode === 'ROLE_BASED' || mode === 'KNOWLEDGE_TRAINING' || mode === 'EXPERIENCE_SIMULATION'
    ? mode
    : null
}
