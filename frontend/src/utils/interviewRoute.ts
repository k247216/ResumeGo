import type { RouteLocationRaw } from 'vue-router'

interface TargetInterviewContext {
  targetId?: number | null
  versionId?: number | null
  jobId?: number | null
}

export function buildTargetInterviewLocation(context: TargetInterviewContext): RouteLocationRaw {
  if (![context.targetId, context.versionId, context.jobId].every(isPositiveId)) {
    throw new Error('开始面试需要完整的求职目标上下文')
  }

  return {
    name: 'interview',
    query: {
      from: 'target',
      targetId: String(context.targetId),
      versionId: String(context.versionId),
      jobId: String(context.jobId),
    },
  }
}

function isPositiveId(value?: number | null): value is number {
  return Number.isSafeInteger(value) && Number(value) > 0
}
