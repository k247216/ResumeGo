import { describe, expect, it } from 'vitest'
import {
  INTERVIEW_ENGINE_STATES,
  interviewEngineStateFromQuery,
  interviewEngineStateToQuery,
  type InterviewEngineState,
} from './interviewEngineState'

describe('interview engine state contract', () => {
  it('keeps the four work states explicit and stable', () => {
    expect(INTERVIEW_ENGINE_STATES).toEqual(['home', 'setup', 'room', 'review'])
  })

  it.each<InterviewEngineState>(['home', 'room', 'review'])('round trips %s', (state) => {
    const query = interviewEngineStateToQuery(state, state === 'room' || state === 'review' ? 42 : null)
    expect(interviewEngineStateFromQuery(query)).toEqual({ state, sessionId: state === 'room' || state === 'review' ? 42 : null, mode: null })
  })

  it('round trips setup only when a valid mode is present', () => {
    const query = interviewEngineStateToQuery('setup', null, 'ROLE_BASED')
    expect(interviewEngineStateFromQuery(query)).toEqual({ state: 'setup', sessionId: null, mode: 'ROLE_BASED' })
  })

  it('falls back to home for unknown or incomplete query values', () => {
    expect(interviewEngineStateFromQuery({ view: 'unknown', sessionId: '42' })).toEqual({ state: 'home', sessionId: null, mode: null })
    expect(interviewEngineStateFromQuery({ view: 'room' })).toEqual({ state: 'home', sessionId: null, mode: null })
    expect(interviewEngineStateFromQuery({ view: 'review', sessionId: 'nope' })).toEqual({ state: 'home', sessionId: null, mode: null })
  })

  it('does not carry a session id into home or setup', () => {
    expect(interviewEngineStateToQuery('home', 42)).toEqual({ view: 'home' })
    expect(interviewEngineStateToQuery('setup', 42)).toEqual({ view: 'setup' })
  })

  it('round trips a setup mode without leaking it into other states', () => {
    expect(interviewEngineStateFromQuery({ view: 'setup', mode: 'KNOWLEDGE_TRAINING' })).toEqual({
      state: 'setup',
      sessionId: null,
      mode: 'KNOWLEDGE_TRAINING',
    })
    expect(interviewEngineStateFromQuery({ view: 'setup', mode: 'unknown' })).toEqual({
      state: 'home',
      sessionId: null,
      mode: null,
    })
    expect(interviewEngineStateToQuery('setup', null, 'EXPERIENCE_SIMULATION')).toEqual({
      view: 'setup',
      mode: 'EXPERIENCE_SIMULATION',
    })
    expect(interviewEngineStateToQuery('home', null, 'EXPERIENCE_SIMULATION')).toEqual({ view: 'home' })
  })
})
