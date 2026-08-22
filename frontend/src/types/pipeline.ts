/** 对应后端 PipelineLifecycle */
export type PipelineLifecycle = 'ACTIVE' | 'PAUSED' | 'CLOSED' | 'ARCHIVED'

/** 对应后端 PipelineOutcome */
export type PipelineOutcome = 'OFFER' | 'REJECTED' | 'WITHDRAWN' | 'OTHER'

/** 对应后端 PipelineStageState */
export type PipelineStageState = 'PENDING' | 'CURRENT' | 'COMPLETED' | 'SKIPPED'

/** 对应后端 PipelineStageResponse */
export interface PipelineStage {
  id: number
  name: string
  position: number
  state: PipelineStageState
}

/** 对应后端 CareerPipelineResponse */
export interface CareerPipeline {
  id: number
  name: string
  companyName: string
  roleTitle: string
  jobDescriptionId: number | null
  resumeVersionId: number | null
  lifecycle: PipelineLifecycle
  outcome: PipelineOutcome | null
  currentStageId: number | null
  stages: PipelineStage[]
  scheduleEventIds: number[]
  interviewPlanIds: number[]
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

/** 对应后端 CreateCareerPipelineRequest */
export interface CreatePipelineRequest {
  name: string
  companyName: string
  roleTitle: string
  jobDescriptionId?: number | null
  resumeVersionId?: number | null
  stages?: string[]
}

/** 对应后端 AddPipelineStageRequest */
export interface AddPipelineStageRequest {
  name: string
}

/** 对应后端 RenamePipelineStageRequest */
export interface RenamePipelineStageRequest {
  name: string
}

/** 对应后端 ReorderPipelineStagesRequest */
export interface ReorderPipelineStagesRequest {
  stageIds: number[]
}

/** 对应后端 TransitionPipelineStageRequest */
export interface TransitionPipelineStageRequest {
  targetStageId: number
  note?: string | null
}

/** 与现有 client 约定一致的成功包装（后端 ApiResponse） */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}
