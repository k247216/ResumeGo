import type { ResumeContent } from './resume'

export interface LayoutProposalRequest {
  resumeVersionId?: number | null
  draftContent: ResumeContent
  targetJobDescriptionId?: number | null
  targetJob?: LayoutProposalTargetJob | null
  templateKey?: string | null
  goal?: string | null
}

export interface LayoutProposalTargetJob {
  jobTitle?: string
  companyName?: string
  requiredSkills?: string[]
  preferredSkills?: string[]
  responsibilities?: string[]
  experienceRequirements?: string[]
  educationRequirements?: string[]
}

export interface LayoutProposalChange {
  id: string
  sectionId: string
  fieldKey: string
  label: string
  before: string
  after: string
  reason: string
  riskLevel?: 'low' | 'medium'
}

export interface LayoutProposalResponse {
  proposalId: string
  model: string
  promptVersion: string
  changes: LayoutProposalChange[]
  templateKey?: string | null
  hiddenSectionIds: string[]
  warnings: string[]
}
