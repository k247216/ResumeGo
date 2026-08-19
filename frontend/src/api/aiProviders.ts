import { apiFetch } from './http'

export type AiProtocol = 'openai-compatible' | 'anthropic' | 'gemini'

export interface AiProviderProfile {
  id: number
  displayName: string
  protocolType: AiProtocol
  baseUrl: string
  defaultModel: string
  defaultProfile: boolean
  apiKeyConfigured: boolean
  lastTestedAt: string | null
  lastTestStatus: 'success' | 'failed' | null
  lastTestMessage: string | null
}

export interface AiProviderProfileInput {
  displayName: string
  protocolType: AiProtocol
  baseUrl: string
  defaultModel: string
}

export interface AiProviderProbeInput extends AiProviderProfileInput {
  apiKey: string
}

export interface AiProviderProbeResult {
  success: boolean
  message: string
  models: string[]
}

interface Envelope<T> {
  success: boolean
  data: T
  message: string | null
}

async function unwrap<T>(response: Response): Promise<T> {
  const body = await response.json() as Envelope<T>
  if (!response.ok || !body.success) throw new Error(body.message || '模型配置请求失败')
  return body.data
}

export async function listAiProviders(): Promise<AiProviderProfile[]> {
  return unwrap(await apiFetch('/api/ai/providers'))
}

export async function createAiProvider(input: AiProviderProfileInput): Promise<AiProviderProfile> {
  return unwrap(await apiFetch('/api/ai/providers', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  }))
}

export async function updateAiProvider(id: number, input: AiProviderProfileInput): Promise<AiProviderProfile> {
  return unwrap(await apiFetch(`/api/ai/providers/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  }))
}

export async function setDefaultAiProvider(id: number): Promise<AiProviderProfile> {
  return unwrap(await apiFetch(`/api/ai/providers/${id}/default`, { method: 'PUT' }))
}

export async function deleteAiProvider(id: number): Promise<void> {
  await unwrap(await apiFetch(`/api/ai/providers/${id}`, { method: 'DELETE' }))
}

export async function applyWebSessionKey(profileId: number, apiKey: string): Promise<AiProviderProfile> {
  return unwrap(await apiFetch('/api/ai/runtime/session', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ profileId, apiKey }),
  }))
}

export async function testAiProvider(id: number): Promise<AiProviderProfile> {
  return unwrap(await apiFetch(`/api/ai/providers/${id}/test`, { method: 'POST' }))
}

export async function testUnsavedAiProvider(input: AiProviderProbeInput): Promise<AiProviderProbeResult> {
  return unwrap(await apiFetch('/api/ai/providers/test', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  }))
}

export async function fetchAiProviderModels(input: AiProviderProbeInput): Promise<AiProviderProbeResult> {
  return unwrap(await apiFetch('/api/ai/providers/models', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input),
  }))
}
