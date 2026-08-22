import type {
  ApiResponse,
  KnowledgeCategory,
  KnowledgeCategoryNode,
  KnowledgeContentResponse,
  KnowledgeDeletionImpact,
  KnowledgeDocument,
  KnowledgeDocumentClassification,
  KnowledgeImportResponse,
  KnowledgeSearchItem,
  KnowledgeTag,
  UpdateKnowledgeCategoryRequest,
} from '../types/knowledge'
import { apiFetch } from './http'

const KNOWLEDGE_BASE = '/api/v2/knowledge'

/** 带 HTTP 状态码的错误：用于区分 409（仍在处理）与 404（不可用）。 */
export class KnowledgeHttpError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'KnowledgeHttpError'
    this.status = status
  }
}

async function parseResponse<T>(res: Response, fallbackMessage: string): Promise<ApiResponse<T>> {
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new KnowledgeHttpError(res.status, body?.message || body?.error?.message || fallbackMessage)
  }
  if (!body?.success) {
    throw new KnowledgeHttpError(res.status, body?.message || fallbackMessage)
  }
  return body
}

/** 浏览列表：可选 categoryId/tagId/includeDescendants 过滤；无参数返回当前用户全部资料。 */
export async function listKnowledgeDocuments(
  categoryId?: number | null,
  tagId?: number | null,
  includeDescendants = false,
): Promise<ApiResponse<KnowledgeDocument[]>> {
  const params = new URLSearchParams()
  if (categoryId != null) params.set('categoryId', String(categoryId))
  if (tagId != null) params.set('tagId', String(tagId))
  if (categoryId != null) params.set('includeDescendants', String(includeDescendants))
  const query = params.toString()
  const res = await apiFetch(query ? `${KNOWLEDGE_BASE}/documents?${query}` : `${KNOWLEDGE_BASE}/documents`)
  return parseResponse<KnowledgeDocument[]>(res, '获取知识库列表失败')
}

export async function getKnowledgeDocument(documentId: number): Promise<ApiResponse<KnowledgeDocument>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}`)
  return parseResponse<KnowledgeDocument>(res, '获取知识文档失败')
}

export async function createKnowledgeNote(title: string): Promise<ApiResponse<KnowledgeDocument>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, sourceType: 'NOTE' }),
  })
  return parseResponse<KnowledgeDocument>(res, '创建笔记失败')
}

/** 只发送 File 字节的 multipart（单字段 file），绝不发送系统路径。 */
export async function importKnowledgeFile(file: File): Promise<ApiResponse<KnowledgeImportResponse>> {
  const form = new FormData()
  form.append('file', file)
  const res = await apiFetch(`${KNOWLEDGE_BASE}/imports`, { method: 'POST', body: form })
  return parseResponse<KnowledgeImportResponse>(res, '导入文件失败')
}

/** 未完成返回 409，缺失/他人返回 404；均由后端决定，前端只展示真实状态。 */
export async function getKnowledgeContent(documentId: number): Promise<ApiResponse<KnowledgeContentResponse>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/content`)
  return parseResponse<KnowledgeContentResponse>(res, '读取提取内容失败')
}

export async function listKnowledgeCategories(): Promise<ApiResponse<KnowledgeCategory[]>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/categories`)
  return parseResponse<KnowledgeCategory[]>(res, '获取分类列表失败')
}

export async function listKnowledgeTags(): Promise<ApiResponse<KnowledgeTag[]>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/tags`)
  return parseResponse<KnowledgeTag[]>(res, '获取标签列表失败')
}

export async function createKnowledgeTag(name: string): Promise<ApiResponse<KnowledgeTag>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return parseResponse<KnowledgeTag>(res, '创建标签失败')
}

/** 所选文档的现有关联（category 可为 null，tags 为空数组）。 */
export async function getDocumentClassification(documentId: number): Promise<ApiResponse<KnowledgeDocumentClassification>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/classification`)
  return parseResponse<KnowledgeDocumentClassification>(res, '读取文档关联失败')
}

export async function setDocumentCategory(documentId: number, categoryId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/category/${categoryId}`, {
    method: 'PUT',
  })
  return parseResponse<null>(res, '设置分类失败')
}

export async function removeDocumentCategory(documentId: number, categoryId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/category/${categoryId}`, {
    method: 'DELETE',
  })
  return parseResponse<null>(res, '移除分类失败')
}

export async function addDocumentTag(documentId: number, tagId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/tags/${tagId}`, {
    method: 'PUT',
  })
  return parseResponse<null>(res, '添加标签失败')
}

export async function removeDocumentTag(documentId: number, tagId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/tags/${tagId}`, {
    method: 'DELETE',
  })
  return parseResponse<null>(res, '移除标签失败')
}

/** 关键词搜索：q trim 1-100，可选 categoryId/tagId；结果含 matchedField/snippet/lineNumber。 */
export async function searchKnowledge(
  q: string,
  categoryId?: number | null,
  tagId?: number | null,
  includeDescendants = true,
): Promise<ApiResponse<KnowledgeSearchItem[]>> {
  const params = new URLSearchParams()
  params.set('q', q)
  if (categoryId != null) params.set('categoryId', String(categoryId))
  if (tagId != null) params.set('tagId', String(tagId))
  if (categoryId != null) params.set('includeDescendants', String(includeDescendants))
  const res = await apiFetch(`${KNOWLEDGE_BASE}/search?${params.toString()}`)
  return parseResponse<KnowledgeSearchItem[]>(res, '搜索失败')
}

export async function retryKnowledgeDocument(documentId: number): Promise<ApiResponse<KnowledgeDocument>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/retry`, { method: 'POST' })
  return parseResponse<KnowledgeDocument>(res, '重试解析失败')
}

export async function getKnowledgeDeletionImpact(documentId: number): Promise<ApiResponse<KnowledgeDeletionImpact>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/deletion-impact`)
  return parseResponse<KnowledgeDeletionImpact>(res, '读取删除影响失败')
}

export async function deleteKnowledgeDocument(documentId: number, confirmationToken: string): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ confirmationToken }),
  })
  return parseResponse<null>(res, '删除资料失败')
}

/** 层级分类树（BE-04：扁平节点 + parentId/depth/counts） */
export async function listKnowledgeCategoryTree(): Promise<ApiResponse<KnowledgeCategoryNode[]>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/categories`)
  return parseResponse<KnowledgeCategoryNode[]>(res, '获取资料库分类失败')
}

export async function createKnowledgeCategory(name: string, parentId: number | null = null): Promise<ApiResponse<KnowledgeCategory>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/categories`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, parentId }),
  })
  return parseResponse<KnowledgeCategory>(res, '创建分类失败')
}

export async function updateKnowledgeCategory(id: number, request: UpdateKnowledgeCategoryRequest): Promise<ApiResponse<KnowledgeCategory>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/categories/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return parseResponse<KnowledgeCategory>(res, '更新分类失败')
}

export async function deleteKnowledgeCategory(id: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/categories/${id}`, { method: 'DELETE' })
  return parseResponse<null>(res, '删除分类失败')
}

/** NOTE 正文保存（BE-05）：原样 UTF-8，允许空串，≤1 MiB */
export async function saveKnowledgeNoteContent(documentId: number, content: string): Promise<ApiResponse<KnowledgeContentResponse>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents/${documentId}/content`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })
  return parseResponse<KnowledgeContentResponse>(res, '保存笔记正文失败')
}
