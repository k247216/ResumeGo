/** Knowledge 模块类型：与后端 Knowledge 契约一一对应。 */

export type KnowledgeSourceType = 'NOTE' | 'FILE'

export type KnowledgeProcessingStatus = 'NOT_STARTED' | 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/** 与现有 client 约定一致的成功包装（后端 ApiResponse） */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

/** 对应后端 KnowledgeDocumentResponse */
export interface KnowledgeDocument {
  id: number
  title: string
  sourceType: KnowledgeSourceType
  processingStatus: KnowledgeProcessingStatus
  sourceFile: string | null
  createdAt: string
  updatedAt: string
}

/** 对应后端 CreateKnowledgeDocumentRequest（本片仅 NOTE） */
export interface CreateKnowledgeNoteRequest {
  title: string
  sourceType: 'NOTE'
}

/** 对应后端 KnowledgeImportResponse（冻结 IO-01 契约） */
export interface KnowledgeImportResponse {
  documentId: number
  sourceType: KnowledgeSourceType
  processingStatus: KnowledgeProcessingStatus
  duplicate: boolean
  errorCode: string | null
}

/** 对应后端 KnowledgeContentResponse：正文只通过该端点返回 */
export interface KnowledgeContentResponse {
  documentId: number
  content: string
}

/** 对应后端 KnowledgeCategoryResponse */
export interface KnowledgeCategory {
  id: number
  name: string
  normalizedName: string
  createdAt: string
  updatedAt: string
}

/** 对应后端 KnowledgeTagResponse */
export interface KnowledgeTag {
  id: number
  name: string
  normalizedName: string
  createdAt: string
  updatedAt: string
}

/** 对应后端 CreateKnowledgeNameRequest（分类/标签共用） */
export interface CreateKnowledgeNameRequest {
  name: string
}

/** 对应后端 KnowledgeDocumentClassificationResponse：所选文档的现有关联 */
export interface KnowledgeDocumentClassification {
  category: KnowledgeCategory | null
  tags: KnowledgeTag[]
}

/** 对应后端 KnowledgeSearchItemResponse */
export interface KnowledgeSearchItem {
  document: KnowledgeDocument
  matchedField: 'TITLE' | 'CONTENT'
  snippet: string
  lineNumber: number | null
}

/** 搜索过滤条件 */
export interface KnowledgeSearchFilter {
  categoryId: number | null
  tagId: number | null
}
