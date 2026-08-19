import type { ApiResponse } from '../types/resume'
import type { LayoutProposalRequest, LayoutProposalResponse } from '../types/layout'
import { apiFetch } from './http'

async function parseResponse<T>(res: Response, fallbackMessage: string): Promise<ApiResponse<T>> {
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.message || body?.error?.message || fallbackMessage)
  }
  if (!body?.success) {
    throw new Error(body?.message || fallbackMessage)
  }
  return body
}

export async function createLayoutProposal(
  req: LayoutProposalRequest,
): Promise<ApiResponse<LayoutProposalResponse>> {
  const res = await apiFetch('/api/v1/resume-layout/proposals', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<LayoutProposalResponse>(res, '生成 AI 排版提案失败')
}
