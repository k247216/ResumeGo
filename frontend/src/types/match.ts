export interface CreateJobMatchRequest {
  resumeVersionId: number
  jobDescriptionId: number
}

export interface MatchDetails {
  requiredCoverage?: number
  preferredCoverage?: number
  experienceCoverage?: number
  educationMatch?: boolean
  responsibilityCoverage?: number
  matchedItems?: string[]
  missingItems?: string[]
  unknownItems?: string[]
  dimensionScores?: Record<string, number>
  matchLevel?: string
  [key: string]: unknown
}

export interface JobMatch {
  id?: number
  resumeVersionId?: number
  jobDescriptionId?: number
  algorithmVersion?: string
  matchScore: number
  details: MatchDetails
  inputFingerprint?: string
  createdAt?: string
}

export interface BatchMatchResult {
  jobDescriptionId: number
  matchScore: number
  matchLevel: string
}

export interface BatchMatchResponse {
  matches: BatchMatchResult[]
  totalCompared: number
}
