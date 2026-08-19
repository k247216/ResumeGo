export interface OptimizationSuggestion {
  id: number
  jobMatchId: number
  resumeVersionId: number
  evidenceId?: number | null
  sectionKey: string
  originalText: string
  suggestedText?: string | null
  reasonText: string
  targetRequirement: string
  status: 'pending' | 'accepted' | 'rejected' | 'evidence_required' | 'high_risk' | string
  riskLevel?: string | null
  createdAt: string
}

export interface GenerateSuggestionsResponse {
  suggestions: OptimizationSuggestion[]
}

export interface SuggestionFollowUpRequest {
  userSupplement: string
}

export interface SuggestionFollowUpResponse {
  finalAdvice: string
  nextSteps: string[]
  promptVersion: string
}
