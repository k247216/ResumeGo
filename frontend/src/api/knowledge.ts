import type {
  ApiResponse,
  KnowledgeContentResponse,
  KnowledgeDocument,
  KnowledgeImportResponse,
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

export async function listKnowledgeDocuments(): Promise<ApiResponse<KnowledgeDocument[]>> {
  const res = await apiFetch(`${KNOWLEDGE_BASE}/documents`)
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
