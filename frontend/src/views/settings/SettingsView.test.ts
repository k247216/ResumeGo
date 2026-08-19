// @vitest-environment happy-dom

import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SettingsView from './SettingsView.vue'

const { listAiProviders, testUnsavedAiProvider, fetchAiProviderModels } = vi.hoisted(() => ({
  listAiProviders: vi.fn(),
  testUnsavedAiProvider: vi.fn(),
  fetchAiProviderModels: vi.fn(),
}))

vi.mock('../../api/aiProviders', () => ({
  listAiProviders,
  createAiProvider: vi.fn(),
  updateAiProvider: vi.fn(),
  setDefaultAiProvider: vi.fn(),
  deleteAiProvider: vi.fn(),
  applyWebSessionKey: vi.fn(),
  testAiProvider: vi.fn(),
  testUnsavedAiProvider,
  fetchAiProviderModels,
}))

vi.mock('element-plus', async (importOriginal) => {
  const original = await importOriginal<typeof import('element-plus')>()
  return { ...original, ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() } }
})

describe('SettingsView', () => {
  beforeEach(() => {
    listAiProviders.mockResolvedValue([])
    delete window.resumeGoDesktop
  })

  it('keeps local editing available while model service is unconfigured', async () => {
    const wrapper = mount(SettingsView, {
      global: { directives: { loading: () => undefined } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('本地数据')
    expect(wrapper.text()).toContain('尚未配置')
    expect(wrapper.text()).toContain('本地编辑功能仍可正常使用')
  })

  it('offers the common provider presets', async () => {
    const wrapper = mount(SettingsView, {
      global: { directives: { loading: () => undefined } },
    })
    await flushPromises()

    const options = wrapper.findAll('option').map((option) => option.text())
    expect(options).toEqual(expect.arrayContaining([
      'OpenAI', 'Anthropic', 'Google Gemini', 'DeepSeek', '智谱 GLM', '通义千问', 'Moonshot',
    ]))
  })

  it('tests and discovers models before a profile is saved', async () => {
    testUnsavedAiProvider.mockResolvedValue({ success: true, message: '连接成功', models: [] })
    fetchAiProviderModels.mockResolvedValue({ success: true, message: '已获取 2 个模型', models: ['model-a', 'model-b'] })
    const wrapper = mount(SettingsView, { global: { directives: { loading: () => undefined } } })
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs.at(-1)!.setValue('temporary-key')

    await wrapper.findAll('button').find((button) => button.text() === '测试连接')!.trigger('click')
    await flushPromises()
    expect(testUnsavedAiProvider).toHaveBeenCalledWith(expect.objectContaining({ apiKey: 'temporary-key' }))

    await wrapper.findAll('button').find((button) => button.text() === '获取模型')!.trigger('click')
    await flushPromises()
    expect(fetchAiProviderModels).toHaveBeenCalled()
    expect((wrapper.find('input[list="provider-models"]').element as HTMLInputElement).value).toBe('model-a')
  })
})
