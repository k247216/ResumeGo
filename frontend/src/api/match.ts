import type { BatchMatchResponse, CreateJobMatchRequest, JobMatch } from '../types/match'
import { apiFetch } from './http'

async function parseRawResponse<T>(res: Response, fallbackMessage: string): Promise<T> {
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.message || body?.error?.message || fallbackMessage)
  }
  return body
}

export async function createJobMatch(
  req: CreateJobMatchRequest,
): Promise<JobMatch> {
  const res = await apiFetch(`/api/resume-versions/${req.resumeVersionId}/job-matches`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jobDescriptionId: req.jobDescriptionId,
    }),
  })
  return parseRawResponse<JobMatch>(res, '生成岗位匹配失败')
}

export async function batchMatch(
  versionId: number,
  topN: number = 5,
): Promise<BatchMatchResponse> {
  const res = await apiFetch(`/api/resume-versions/${versionId}/batch-matches`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ topN }),
  })
  return parseRawResponse<BatchMatchResponse>(res, '批量岗位匹配失败')
}
