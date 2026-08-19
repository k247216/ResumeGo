import type {
  ApiResponse,
  CapabilityEvidence,
  CreateCapabilityEvidenceRequest,
} from '../types/evidence'
import { apiFetch } from './http'

const BASE = '/api/v1/evidences'

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

export async function listEvidences(): Promise<ApiResponse<CapabilityEvidence[]>> {
  const res = await apiFetch(BASE)
  return parseResponse<CapabilityEvidence[]>(res, '获取能力证据失败')
}

export async function getEvidence(id: number): Promise<ApiResponse<CapabilityEvidence>> {
  const res = await apiFetch(`${BASE}/${id}`)
  return parseResponse<CapabilityEvidence>(res, '获取能力证据详情失败')
}

export async function createEvidence(
  req: CreateCapabilityEvidenceRequest,
): Promise<ApiResponse<CapabilityEvidence>> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<CapabilityEvidence>(res, '创建能力证据失败')
}
