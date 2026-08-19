import type { AiProtocol } from '../api/aiProviders'

export interface AiProviderPreset {
  id: string
  label: string
  protocolType: AiProtocol
  baseUrl: string
  model: string
}

export const aiProviderPresets: AiProviderPreset[] = [
  { id: 'openai', label: 'OpenAI', protocolType: 'openai-compatible', baseUrl: 'https://api.openai.com/v1', model: 'gpt-4.1-mini' },
  { id: 'anthropic', label: 'Anthropic', protocolType: 'anthropic', baseUrl: 'https://api.anthropic.com/v1', model: 'claude-sonnet-4-20250514' },
  { id: 'gemini', label: 'Google Gemini', protocolType: 'gemini', baseUrl: 'https://generativelanguage.googleapis.com/v1beta', model: 'gemini-2.5-flash' },
  { id: 'deepseek', label: 'DeepSeek', protocolType: 'openai-compatible', baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  { id: 'glm', label: '智谱 GLM', protocolType: 'openai-compatible', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', model: 'glm-4-flash' },
  { id: 'qwen', label: '通义千问', protocolType: 'openai-compatible', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
  { id: 'moonshot', label: 'Moonshot', protocolType: 'openai-compatible', baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' },
  { id: 'custom', label: '自定义兼容服务', protocolType: 'openai-compatible', baseUrl: '', model: '' },
]
