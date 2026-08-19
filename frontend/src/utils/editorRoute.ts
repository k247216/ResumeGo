import type { RouteLocationRaw } from 'vue-router'

interface ResumeEditorContext {
  resumeId?: number | null
  versionId?: number | null
  targetId?: number | null
  mode?: 'blank'
}

export function buildResumeEditorLocation(context: ResumeEditorContext): RouteLocationRaw {
  const query: Record<string, string> = {}
  addPositiveId(query, 'resumeId', context.resumeId)
  addPositiveId(query, 'versionId', context.versionId)
  addPositiveId(query, 'targetId', context.targetId)
  if (context.mode === 'blank') query.mode = 'blank'
  return { name: 'resume-editor', query }
}

function addPositiveId(query: Record<string, string>, key: string, value?: number | null) {
  if (Number.isSafeInteger(value) && Number(value) > 0) query[key] = String(value)
}
