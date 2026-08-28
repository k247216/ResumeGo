import { defaultResumeTemplateKey, isValidResumeTemplateKey, resumeTemplateOptions } from '../constants/resumeTemplates'
import type { ResumeContent } from '../types/resume'

export { defaultResumeTemplateKey, isValidResumeTemplateKey, resumeTemplateOptions }

export type ResumeTemplateKey =
  | 'classic' | 'blue' | 'minimal' | 'emerald' | 'graphite'
  | 'sidebar' | 'compact' | 'elegant' | 'warm' | 'terminal'
  | 'royal' | 'steel' | 'wine' | 'navy' | 'forest'
  | 'slate' | 'rose' | 'ocean' | 'amber' | 'nord'
  | 'editorial' | 'timeline' | 'mono' | 'folio' | 'paper'

const LEGACY_GLOBAL_KEY = 'resumego:selectedResumeTemplate'
const ASSET_KEY_PREFIX = 'resumego:resume-template:'

export function normalizeResumeTemplateKey(value: unknown): ResumeTemplateKey {
  if (typeof value === 'string' && isValidResumeTemplateKey(value)) return value as ResumeTemplateKey
  return defaultResumeTemplateKey as ResumeTemplateKey
}

export function resumeTemplateStorageKey(resumeId: number): string {
  return `${ASSET_KEY_PREFIX}${resumeId}`
}

export function readResumeTemplate(resumeId: number | null | undefined): ResumeTemplateKey {
  try {
    const scoped = resumeId != null ? localStorage.getItem(resumeTemplateStorageKey(resumeId)) : null
    if (scoped) return normalizeResumeTemplateKey(scoped)
    return normalizeResumeTemplateKey(localStorage.getItem(LEGACY_GLOBAL_KEY))
  } catch {
    return defaultResumeTemplateKey as ResumeTemplateKey
  }
}

export function writeResumeTemplate(resumeId: number | null | undefined, value: string): ResumeTemplateKey {
  const normalized = normalizeResumeTemplateKey(value)
  try {
    if (resumeId != null) localStorage.setItem(resumeTemplateStorageKey(resumeId), normalized)
    localStorage.setItem(LEGACY_GLOBAL_KEY, normalized)
  } catch {
    // localStorage unavailable: rendering still uses normalized value in memory.
  }
  return normalized
}

/**
 * 创建岗位副本时复制一次来源外观；副本随后使用自己的资产级键，不再跟随来源简历。
 */
export function cloneResumeTemplate(
  sourceResumeId: number | null | undefined,
  targetResumeId: number | null | undefined,
  sourceTemplate?: string,
): ResumeTemplateKey {
  const template = sourceTemplate ?? readResumeTemplate(sourceResumeId)
  return writeResumeTemplate(targetResumeId, template)
}

export interface ResumeTitleSource {
  title: string
  currentVersion?: { content?: ResumeContent | null } | null
}

/**
 * 资产列表展示岗位语义，不把同一用户的姓名重复写在每一行。
 * 原始标题仍保存在资产中，正文预览继续展示完整个人信息。
 */
export function getResumeDisplayTitle(resume: ResumeTitleSource): string {
  const rawTitle = resume.title.trim()
  const contentName = resume.currentVersion?.content?.basicInfo?.name?.trim() ?? ''
  const targetRole = resume.currentVersion?.content?.basicInfo?.targetRole?.trim() ?? ''
  const withoutName = contentName
    ? rawTitle
      // 资产标题可能来自旧版本的“姓名 + 岗位简历”，兼容分隔符与中文“的”。
      .replace(new RegExp(`^${escapeRegExp(contentName)}\\s*(?:[-·|｜/:：]|的)?\\s*`), '')
      .replace(new RegExp(`\\s*(?:[-·|｜/:：]|的)?\\s*${escapeRegExp(contentName)}$`), '')
      .trim()
    : rawTitle
  if (withoutName && withoutName !== '简历' && withoutName !== '新简历') return withoutName
  if (targetRole) return `${targetRole}简历`
  return withoutName || rawTitle || '未命名简历'
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
