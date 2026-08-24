import type {
  AddPipelineStageRequest,
  ApiResponse,
  CareerPipeline,
  CreatePipelineRequest,
  PipelineStageTransition,
  RenamePipelineStageRequest,
  ReorderPipelineStagesRequest,
  TransitionPipelineStageRequest,
  UpdatePipelineRequest,
} from '../types/pipeline'
import { apiFetch } from './http'

const PIPELINE_BASE = '/api/v2/pipelines'

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

export async function listPipelines(): Promise<ApiResponse<CareerPipeline[]>> {
  const res = await apiFetch(PIPELINE_BASE)
  return parseResponse<CareerPipeline[]>(res, '获取求职管线列表失败')
}

export async function getPipeline(pipelineId: number): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}`)
  return parseResponse<CareerPipeline>(res, '获取求职管线失败')
}

export async function createPipeline(req: CreatePipelineRequest): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(PIPELINE_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: req.name,
      companyName: req.companyName,
      roleTitle: req.roleTitle,
      jobDescriptionId: req.jobDescriptionId ?? null,
      resumeVersionId: req.resumeVersionId ?? null,
      stages: req.stages ?? [],
    }),
  })
  return parseResponse<CareerPipeline>(res, '创建求职管线失败')
}

export async function updatePipeline(
  pipelineId: number,
  req: UpdatePipelineRequest,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    // 五个字段全部显式发送，含两个可空的 null
    body: JSON.stringify({
      name: req.name,
      companyName: req.companyName,
      roleTitle: req.roleTitle,
      jobDescriptionId: req.jobDescriptionId,
      resumeVersionId: req.resumeVersionId,
    }),
  })
  return parseResponse<CareerPipeline>(res, '更新求职管线失败')
}

export async function listPipelineTransitions(
  pipelineId: number,
): Promise<ApiResponse<PipelineStageTransition[]>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/transitions`)
  return parseResponse<PipelineStageTransition[]>(res, '读取阶段历史失败')
}

export async function addPipelineStage(
  pipelineId: number,
  req: AddPipelineStageRequest,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/stages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<CareerPipeline>(res, '添加阶段失败')
}

export async function renamePipelineStage(
  pipelineId: number,
  stageId: number,
  req: RenamePipelineStageRequest,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/stages/${stageId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<CareerPipeline>(res, '重命名阶段失败')
}

export async function reorderPipelineStages(
  pipelineId: number,
  req: ReorderPipelineStagesRequest,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/stages/order`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<CareerPipeline>(res, '调整阶段顺序失败')
}

export async function transitionPipelineStage(
  pipelineId: number,
  req: TransitionPipelineStageRequest,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/transitions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  return parseResponse<CareerPipeline>(res, '推进阶段失败')
}

export async function archivePipeline(pipelineId: number): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/archive`, {
    method: 'POST',
  })
  return parseResponse<CareerPipeline>(res, '归档求职管线失败')
}

export async function restorePipeline(pipelineId: number): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/restore`, {
    method: 'POST',
  })
  return parseResponse<CareerPipeline>(res, '恢复求职管线失败')
}

export async function linkScheduleEvent(
  pipelineId: number,
  eventId: number,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/schedule-events/${eventId}`, {
    method: 'PUT',
  })
  return parseResponse<CareerPipeline>(res, '关联日程失败')
}

export async function unlinkScheduleEvent(
  pipelineId: number,
  eventId: number,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/schedule-events/${eventId}`, {
    method: 'DELETE',
  })
  return parseResponse<CareerPipeline>(res, '解除日程关联失败')
}

export async function linkInterviewPlan(
  pipelineId: number,
  planId: number,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/interview-plans/${planId}`, {
    method: 'PUT',
  })
  return parseResponse<CareerPipeline>(res, '关联模拟面试计划失败')
}

export async function unlinkInterviewPlan(
  pipelineId: number,
  planId: number,
): Promise<ApiResponse<CareerPipeline>> {
  const res = await apiFetch(`${PIPELINE_BASE}/${pipelineId}/interview-plans/${planId}`, {
    method: 'DELETE',
  })
  return parseResponse<CareerPipeline>(res, '解除模拟面试计划关联失败')
}
