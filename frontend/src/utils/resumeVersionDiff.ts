import type { ResumeContent } from '../types/resume'

export interface ResumeChapterChange {
  chapterKey: string
  chapterLabel: string
  changeType: 'added' | 'modified' | 'removed'
}

/** 章节清单：与编辑台一致的展示顺序 */
const CHAPTERS: Array<{ key: keyof ResumeContent; label: string }> = [
  { key: 'basicInfo', label: '基本信息' },
  { key: 'summary', label: '个人简介' },
  { key: 'workExperience', label: '工作经历' },
  { key: 'education', label: '教育经历' },
  { key: 'projects', label: '项目经历' },
  { key: 'skills', label: '技能' },
  { key: 'skillCategories', label: '技能分类' },
  { key: 'certifications', label: '证书' },
  { key: 'languages', label: '语言' },
  { key: 'githubProjects', label: '开源项目' },
  { key: 'customSections', label: '自定义章节' },
]

function isEmptyChapter(value: unknown): boolean {
  if (value == null) return true
  if (typeof value === 'string') return value.trim() === ''
  if (Array.isArray(value)) return value.length === 0
  if (typeof value === 'object') return Object.keys(value).length === 0
  return false
}

function stableKey(value: unknown): string {
  try {
    return JSON.stringify(value) ?? ''
  } catch {
    return String(value)
  }
}

/**
 * 确定性章节级差异：只比较两个真实 ResumeContent 的章节存在性与内容，
 * 不调用 AI、不猜测词级变化。新增=绿色、修改=琥珀、删除=低饱和红（由调用方着色）。
 */
export function diffResumeContent(prev: ResumeContent, next: ResumeContent): ResumeChapterChange[] {
  const changes: ResumeChapterChange[] = []
  for (const chapter of CHAPTERS) {
    const prevValue = prev[chapter.key]
    const nextValue = next[chapter.key]
    const prevEmpty = isEmptyChapter(prevValue)
    const nextEmpty = isEmptyChapter(nextValue)
    if (prevEmpty && nextEmpty) continue
    if (prevEmpty && !nextEmpty) {
      changes.push({ chapterKey: chapter.key as string, chapterLabel: chapter.label, changeType: 'added' })
      continue
    }
    if (!prevEmpty && nextEmpty) {
      changes.push({ chapterKey: chapter.key as string, chapterLabel: chapter.label, changeType: 'removed' })
      continue
    }
    if (stableKey(prevValue) !== stableKey(nextValue)) {
      changes.push({ chapterKey: chapter.key as string, chapterLabel: chapter.label, changeType: 'modified' })
    }
  }
  return changes
}
