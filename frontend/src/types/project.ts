export type JobProjectStatus = 'active' | 'archived'

export interface JobProject {
  id: number
  name: string
  status: JobProjectStatus
  jobDescriptionId: number | null
  resumeVersionId: number | null
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateJobProjectRequest {
  name: string
  jobDescriptionId?: number | null
  resumeVersionId?: number | null
}

export interface UpdateJobProjectLinksRequest {
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}
