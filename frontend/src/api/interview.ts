import type {
  InterviewApiResponse,
  CreateInterviewPlanRequest,
  InterviewPlanResponse,
  GrowthReport,
  InterviewerPersona,
  InterviewStatusResponse,
  MultiSessionSummaryRequest,
  MultiSessionSummaryResponse,
  SessionHistoryResponse,
  StartInterviewRequest,
  SubmitAnswerRequest,
  SubmitAnswerResponse,
} from '../types/interview'
import { apiFetch } from './http'

const BASE = '/api/v1/interviews'
const PLAN_BASE = '/api/v1/interview-plans'

async function parseResponse<T>(
  res: Response,
  fallbackMessage: string,
): Promise<InterviewApiResponse<T>> {
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.message || body?.error?.message || fallbackMessage)
  }
  if (!body?.success) {
    throw new Error(body?.message || fallbackMessage)
  }
  return body
}

export async function createInterview(
  req: StartInterviewRequest,
): Promise<InterviewApiResponse<InterviewStatusResponse>> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<InterviewStatusResponse>(res, '创建面试会话失败')
}

export async function createInterviewPlan(
  req: CreateInterviewPlanRequest,
): Promise<InterviewApiResponse<InterviewPlanResponse>> {
  const res = await apiFetch(PLAN_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<InterviewPlanResponse>(res, '创建多轮面试计划失败')
}

export async function listMyInterviewPlans(): Promise<InterviewApiResponse<InterviewPlanResponse[]>> {
  const res = await apiFetch(`${PLAN_BASE}/my`)
  return parseResponse<InterviewPlanResponse[]>(res, '获取多轮面试计划失败')
}

export async function getInterviewPlan(planId: number): Promise<InterviewApiResponse<InterviewPlanResponse>> {
  const res = await apiFetch(`${PLAN_BASE}/${planId}`)
  return parseResponse<InterviewPlanResponse>(res, '获取多轮面试计划详情失败')
}

export async function generateInterviewPlanSummary(
  planId: number,
): Promise<InterviewApiResponse<MultiSessionSummaryResponse>> {
  const res = await apiFetch(`${PLAN_BASE}/${planId}/summary`, {
    method: 'POST',
  })
  return parseResponse<MultiSessionSummaryResponse>(res, '生成整次多轮面试总结失败')
}

export async function deleteInterviewPlan(planId: number): Promise<InterviewApiResponse<null>> {
  const res = await apiFetch(`${PLAN_BASE}/${planId}`, {
    method: 'DELETE',
  })
  return parseResponse<null>(res, '删除多轮面试计划失败')
}

export async function getInterviewGrowthReport(
  resumeId: number,
  jobDescriptionId: number,
): Promise<InterviewApiResponse<GrowthReport>> {
  const params = new URLSearchParams({
    resumeId: String(resumeId),
    jobDescriptionId: String(jobDescriptionId),
  })
  const res = await apiFetch(`/api/v1/interview-growth?${params.toString()}`)
  return parseResponse<GrowthReport>(res, '获取成长趋势失败')
}

export async function startInterview(
  sessionId: number,
): Promise<InterviewApiResponse<InterviewStatusResponse>> {
  const res = await apiFetch(`${BASE}/${sessionId}/start`, {
    method: 'POST',
  })
  return parseResponse<InterviewStatusResponse>(res, '开始面试失败')
}

export async function getInterviewStatus(
  sessionId: number,
): Promise<InterviewApiResponse<InterviewStatusResponse>> {
  const res = await apiFetch(`${BASE}/${sessionId}/status`)
  return parseResponse<InterviewStatusResponse>(res, '获取面试状态失败')
}

export async function submitInterviewAnswer(
  sessionId: number,
  req: SubmitAnswerRequest,
): Promise<InterviewApiResponse<SubmitAnswerResponse>> {
  const res = await apiFetch(`${BASE}/${sessionId}/answers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<SubmitAnswerResponse>(res, '提交面试回答失败')
}

export async function listInterviewerPersonas(): Promise<InterviewApiResponse<InterviewerPersona[]>> {
  const res = await apiFetch('/api/v1/interviewer-personas')
  return parseResponse<InterviewerPersona[]>(res, '获取面试官人设列表失败')
}

export async function createInterviewerPersona(
  req: { name: string; title: string; style: string },
): Promise<InterviewApiResponse<InterviewerPersona>> {
  const res = await apiFetch('/api/v1/interviewer-personas', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<InterviewerPersona>(res, '创建面试官人设失败')
}

export async function deleteInterviewerPersona(id: number): Promise<InterviewApiResponse<null>> {
  const res = await apiFetch(`/api/v1/interviewer-personas/${id}`, {
    method: 'DELETE',
  })
  return parseResponse<null>(res, '删除面试官人设失败')
}

export async function listMyInterviews(): Promise<InterviewApiResponse<InterviewStatusResponse[]>> {
  const res = await apiFetch(`${BASE}/my`)
  return parseResponse<InterviewStatusResponse[]>(res, '获取面试会话列表失败')
}

export async function getSessionHistory(
  sessionId: number,
): Promise<InterviewApiResponse<SessionHistoryResponse>> {
  const res = await apiFetch(`${BASE}/${sessionId}/history`)
  return parseResponse<SessionHistoryResponse>(res, '获取会话历史失败')
}

export async function generateMultiSessionSummary(
  request: MultiSessionSummaryRequest,
): Promise<InterviewApiResponse<MultiSessionSummaryResponse>> {
  const res = await apiFetch(`${BASE}/summary/multi`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return parseResponse<MultiSessionSummaryResponse>(res, '生成跨会话总结失败')
}
