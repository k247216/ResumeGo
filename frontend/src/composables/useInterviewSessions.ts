import { computed, ref } from 'vue'
import type {
  InterviewStatusResponse,
  PerQuestionScore,
  SessionHistoryItem,
} from '../types/interview'

export interface InterviewSessionState {
  history: SessionHistoryItem[]
  answerDraft: string
  pendingAnswer: string
  retryable: boolean
  lastSubmitAnswer: string
  perQuestionScores: PerQuestionScore[]
  viewingHistoryIndex: number | null
}

export function createInterviewSessionState(): InterviewSessionState {
  return {
    history: [],
    answerDraft: '',
    pendingAnswer: '',
    retryable: false,
    lastSubmitAnswer: '',
    perQuestionScores: [],
    viewingHistoryIndex: null,
  }
}

export function useInterviewSessions() {
  const sessions = ref<InterviewStatusResponse[]>([])
  const activeSessionId = ref<number | null>(null)
  const sessionStates = ref<Record<number, InterviewSessionState>>({})

  const activeSession = computed(() => sessions.value.find(
    (session) => session.sessionId === activeSessionId.value,
  ) ?? null)
  const activeState = computed(() => activeSessionId.value
    ? sessionStates.value[activeSessionId.value] ?? createInterviewSessionState()
    : createInterviewSessionState())

  function getOrCreateSessionState(sessionId: number): InterviewSessionState {
    if (!sessionStates.value[sessionId]) {
      sessionStates.value[sessionId] = createInterviewSessionState()
    }
    return sessionStates.value[sessionId]
  }

  function activateSession(sessionId: number): InterviewSessionState {
    activeSessionId.value = sessionId
    return getOrCreateSessionState(sessionId)
  }

  function updateSession(sessionId: number, partial: Partial<InterviewStatusResponse>) {
    const index = sessions.value.findIndex((session) => session.sessionId === sessionId)
    if (index >= 0) sessions.value[index] = { ...sessions.value[index], ...partial }
  }

  function upsertSession(session: InterviewStatusResponse) {
    const index = sessions.value.findIndex((item) => item.sessionId === session.sessionId)
    if (index >= 0) {
      sessions.value[index] = { ...sessions.value[index], ...session }
      return
    }
    sessions.value = [session, ...sessions.value]
  }

  function removeSessionState(sessionId: number) {
    delete sessionStates.value[sessionId]
  }

  return {
    sessions,
    activeSessionId,
    sessionStates,
    activeSession,
    activeState,
    getOrCreateSessionState,
    activateSession,
    updateSession,
    upsertSession,
    removeSessionState,
  }
}
