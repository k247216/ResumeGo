import type {
  ApiResponse,
  CreateResumeRequest,
  CreateResumeVersionRequest,
  Resume,
  ResumeVersion,
  UpdateResumeTargetJobRequest,
} from '../types/resume'
import { apiFetch } from './http'

const RESUME_BASE = '/api/v1/resumes'
const VERSION_BASE = '/api/v1/resume-versions'

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

export async function listResumes(): Promise<ApiResponse<Resume[]>> {
  const res = await apiFetch(RESUME_BASE)
  return parseResponse<Resume[]>(res, '获取简历列表失败')
}

export async function deleteResume(resumeId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${RESUME_BASE}/${resumeId}`, {
    method: 'DELETE',
  })
  return parseResponse<null>(res, '删除简历失败')
}

export async function createResume(req: CreateResumeRequest): Promise<ApiResponse<Resume>> {
  const res = await apiFetch(RESUME_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<Resume>(res, '创建简历失败')
}

export async function updateResumeTargetJob(
  resumeId: number,
  req: UpdateResumeTargetJobRequest,
): Promise<ApiResponse<Resume>> {
  const res = await apiFetch(`${RESUME_BASE}/${resumeId}/target-job`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<Resume>(res, '更新简历目标岗位失败')
}

export async function getResumeVersion(versionId: number): Promise<ApiResponse<ResumeVersion>> {
  const res = await apiFetch(`${VERSION_BASE}/${versionId}`)
  return parseResponse<ResumeVersion>(res, '获取简历版本失败')
}

export async function getResumeVersions(resumeId: number): Promise<ApiResponse<ResumeVersion[]>> {
  const res = await apiFetch(`${RESUME_BASE}/${resumeId}/versions`)
  return parseResponse<ResumeVersion[]>(res, '获取版本列表失败')
}

export async function createResumeVersion(
  resumeId: number,
  req: CreateResumeVersionRequest,
): Promise<ApiResponse<ResumeVersion>> {
  const res = await apiFetch(`${RESUME_BASE}/${resumeId}/versions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<ResumeVersion>(res, '保存简历新版本失败')
}

