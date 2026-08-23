import type { KnowledgeProcessingStatus } from '../../types/knowledge'

/** 状态文案全部来自真实 processingStatus，不伪造成功。 */
export const KNOWLEDGE_STATUS_LABEL: Record<KnowledgeProcessingStatus, string> = {
  NOT_STARTED: '未处理',
  PENDING: '处理中',
  RUNNING: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败',
  METADATA_ONLY: '仅收录',
}

export function knowledgeStatusLabel(status: KnowledgeProcessingStatus): string {
  return KNOWLEDGE_STATUS_LABEL[status] ?? status
}
