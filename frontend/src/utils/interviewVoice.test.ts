// @vitest-environment happy-dom
import { describe, expect, it, vi } from 'vitest'
import { canUseSpeechRecognition, speakInterviewQuestion, stopInterviewQuestionSpeech } from './interviewVoice'

describe('interviewVoice', () => {
  it('detects browser speech recognition without requiring it for text fallback', () => {
    expect(canUseSpeechRecognition({ SpeechRecognition: function SpeechRecognition() {} })).toBe(true)
    expect(canUseSpeechRecognition({})).toBe(false)
  })

  it('reads a true-interview question aloud and can stop it', () => {
    const speak = vi.fn()
    const cancel = vi.fn()
    const Utterance = vi.fn(function (this: { text: string; lang?: string; rate?: number }, text: string) {
      this.text = text
    }) as unknown as new (text: string) => { text: string; lang?: string; rate?: number }
    const runtime = { speechSynthesis: { speak, cancel }, SpeechSynthesisUtterance: Utterance }

    expect(speakInterviewQuestion('请介绍一次缓存故障排查。', runtime)).toBe(true)
    expect(cancel).toHaveBeenCalledOnce()
    expect(Utterance).toHaveBeenCalledWith('请介绍一次缓存故障排查。')
    expect(speak).toHaveBeenCalledOnce()

    stopInterviewQuestionSpeech(runtime)
    expect(cancel).toHaveBeenCalledTimes(2)
  })

  it('returns false when speech synthesis is unavailable', () => {
    expect(speakInterviewQuestion('问题', {})).toBe(false)
  })
})
