export type ResumeMarkName =
  | 'profile'
  | 'engineering'
  | 'academic'
  | 'product'
  | 'research'
  | 'creative'
  | 'timeline'
  | 'general'

export const RESUME_MARKS: readonly ResumeMarkName[] = [
  'profile', 'engineering', 'academic', 'product',
  'research', 'creative', 'timeline', 'general',
]

/** 新建简历默认分配一个稳定的身份图标，避免把文件格式当作视觉身份。 */
export function resumeMarkOf(id: number): ResumeMarkName {
  const index = Math.abs(Math.trunc(id)) % RESUME_MARKS.length
  return RESUME_MARKS[index]
}
