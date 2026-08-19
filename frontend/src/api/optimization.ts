import type { ApiResponse } from '../types/resume'
import type {
  GenerateSuggestionsResponse,
  SuggestionFollowUpRequest,
  SuggestionFollowUpResponse,
} from '../types/optimization'
import { apiFetch } from './http'

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

export async function generateSuggestions(
  matchId: number,
): Promise<ApiResponse<GenerateSuggestionsResponse>> {
  const res = await apiFetch(`/api/v1/job-matches/${matchId}/suggestions`, {
    method: 'POST',
  })
  return parseResponse<GenerateSuggestionsResponse>(res, '生成 AI 建议失败')
}

export async function generateSuggestionsWithAssessment(
  matchId: number,
): Promise<ApiResponse<GenerateSuggestionsResponse>> {
  return generateSuggestions(matchId)
}

export async function getSuggestions(
  matchId: number,
): Promise<ApiResponse<GenerateSuggestionsResponse>> {
  const res = await apiFetch(`/api/v1/job-matches/${matchId}/suggestions`)
  return parseResponse<GenerateSuggestionsResponse>(res, '获取 AI 建议失败')
}

export async function generateSuggestionFollowUp(
  suggestionId: number,
  request: SuggestionFollowUpRequest,
): Promise<ApiResponse<SuggestionFollowUpResponse>> {
  const res = await apiFetch(`/api/v1/suggestions/${suggestionId}/follow-up`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return parseResponse<SuggestionFollowUpResponse>(res, '生成最终建议失败')
}

export async function acceptSuggestion(suggestionId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`/api/v1/suggestions/${suggestionId}/accept`, {
    method: 'POST',
  })
  return parseResponse<null>(res, '采纳建议失败')
}

export async function rejectSuggestion(suggestionId: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`/api/v1/suggestions/${suggestionId}/reject`, {
    method: 'POST',
  })
  return parseResponse<null>(res, '拒绝建议失败')
}
