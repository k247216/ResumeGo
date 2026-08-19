import type {
  CreateJobDescriptionRequest,
  CompanyProfile,
  JobDescription,
  ApiResponse,
} from '../types/job'
import { apiFetch } from './http'

const BASE = '/api/v1/job-descriptions'

/**
 * 创建原始 JD（仅保存，不解析）
 */
export async function createJobDescription(
  req: CreateJobDescriptionRequest,
): Promise<ApiResponse<JobDescription>> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error?.message || err.message || '创建 JD 失败')
  }
  return res.json()
}

/**
 * 查询当前本地用户的所有 JD
 */
export async function listJobDescriptions(): Promise<ApiResponse<JobDescription[]>> {
  const res = await apiFetch(BASE)
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error?.message || err.message || '获取 JD 列表失败')
  }
  return res.json()
}

/**
 * 查询单条 JD
 */
export async function getJobDescription(
  id: number,
): Promise<ApiResponse<JobDescription>> {
  const res = await apiFetch(`${BASE}/${id}`)
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error?.message || err.message || '获取 JD 失败')
  }
  return res.json()
}

/**
 * 调用 AI 解析已保存的 JD
 */
export async function parseJobDescription(
  id: number,
): Promise<ApiResponse<JobDescription>> {
  const res = await apiFetch(`${BASE}/${id}/parse`, {
    method: 'POST',
  })
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error?.message || err.message || '解析 JD 失败')
  }
  return res.json()
}

/**
 * 删除 JD
 */
export async function deleteJobDescription(id: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message || '删除 JD 失败')
  }
  return res.json()
}

/**
 * 根据公司名解析公司偏好 Profile。未命中时 data 为空对象。
 */
export async function resolveCompanyProfile(
  companyName?: string | null,
): Promise<ApiResponse<CompanyProfile>> {
  const params = new URLSearchParams()
  if (companyName) params.set('companyName', companyName)
  const res = await apiFetch(`/api/v1/company-profiles/resolve?${params.toString()}`)
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error?.message || err.message || '获取公司偏好失败')
  }
  return res.json()
}
