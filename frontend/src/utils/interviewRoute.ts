import type { RouteLocationRaw } from 'vue-router'

interface TargetInterviewContext {
  targetId?: number | null
  versionId?: number | null
  jobId?: number | null
}

export function buildTargetInterviewLocation(context: TargetInterviewContext): RouteLocationRaw {
  // 部分上下文也合法：只把存在的 id 写入 query；大厅按 targetId 从目标实体回推绑定，
  // 缺 JD 或简历时进入「目标已选、补充绑定」态，而不是丢掉目标上下文裸进大厅。
  const query: Record<string, string> = { from: 'target' }
  if (isPositiveId(context.targetId)) query.targetId = String(context.targetId)
  if (isPositiveId(context.versionId)) query.versionId = String(context.versionId)
  if (isPositiveId(context.jobId)) query.jobId = String(context.jobId)
  return { name: 'interview', query }
}

function isPositiveId(value?: number | null): value is number {
  return Number.isSafeInteger(value) && Number(value) > 0
}
