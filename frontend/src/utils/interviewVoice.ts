type SpeechRuntime = {
  SpeechRecognition?: unknown
  webkitSpeechRecognition?: unknown
  speechSynthesis?: { speak: (utterance: any) => void; cancel: () => void }
  SpeechSynthesisUtterance?: new (text: string) => any
}

/** 浏览器原生语音转写只作为文字输入的增强能力，不影响键盘输入。 */
export function canUseSpeechRecognition(runtime: SpeechRuntime = typeof window === 'undefined' ? {} : window): boolean {
  return Boolean(runtime.SpeechRecognition || runtime.webkitSpeechRecognition)
}

/** 朗读当前真实面经题目；不可用时由调用方保留文字阅读路径。 */
export function speakInterviewQuestion(text: string, runtime: SpeechRuntime = typeof window === 'undefined' ? {} : window): boolean {
  const value = text.trim()
  const synthesis = runtime.speechSynthesis
  const Utterance = runtime.SpeechSynthesisUtterance
  if (!value || !synthesis || !Utterance) return false
  synthesis.cancel()
  const utterance = new Utterance(value)
  utterance.lang = 'zh-CN'
  utterance.rate = 0.98
  synthesis.speak(utterance)
  return true
}

export function stopInterviewQuestionSpeech(runtime: SpeechRuntime = typeof window === 'undefined' ? {} : window): void {
  runtime.speechSynthesis?.cancel()
}
