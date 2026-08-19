export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string | null
}

export type EvidenceType = 'project' | 'internship' | 'competition' | 'skill' | 'other'

export interface CapabilityEvidence {
  id: number
  userId: number
  evidenceType: EvidenceType
  title: string
  situation?: string | null
  actionText: string
  resultText?: string | null
  skillTags: string[]
  sourceNote?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateCapabilityEvidenceRequest {
  evidenceType: EvidenceType
  title: string
  situation?: string
  actionText: string
  resultText?: string
  skillTags: string[]
  sourceNote?: string
}
