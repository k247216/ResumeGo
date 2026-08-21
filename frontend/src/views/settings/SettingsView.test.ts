// @vitest-environment happy-dom

import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SettingsView from './SettingsView.vue'

const { listAiProviders, fetchAiProviderModels } = vi.hoisted(() => ({
  listAiProviders: vi.fn(),
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
  testUnsavedAiProvider: vi.fn(),
  fetchAiProviderModels,
}))

vi.mock('element-plus', async (importOriginal) => {
  const original = await importOriginal<typeof import('element-plus')>()
  return { ...original, ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), info: vi.fn() } }
})

describe('SettingsView', () => {
  beforeEach(() => {
    listAiProviders.mockResolvedValue([])
    delete window.resumeGoDesktop
  })

  it('shows a setup workspace instead of a pre-filled form while no service is configured', async () => {
    const wrapper = mount(SettingsView, {
      global: { directives: { loading: () => undefined } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('本地数据')
    expect(wrapper.text()).toContain('尚未配置')
    const setup = wrapper.get('[data-test="setup-empty"]')
    expect(setup.text()).toContain('尚未配置任何服务')
    expect(setup.text()).toContain('验证后从服务商获取模型')
    // 空态下不出现任何已填好的模型字段
    expect(wrapper.find('input[value="DeepSeek"]').exists()).toBe(false)
    expect(wrapper.find('input[list="provider-models"]').exists()).toBe(false)
  })

  it('offers the common provider presets in the connect flow', async () => {
    const wrapper = mount(SettingsView, { global: { directives: { loading: () => undefined } } })
    await flushPromises()

    await wrapper.get('[data-test="add-ai-service"]').trigger('click')
    const options = wrapper.findAll('select')[0]!.findAll('option').map((option) => option.text())
    expect(options).toEqual(expect.arrayContaining([
      'OpenAI', 'Anthropic', 'Google Gemini', 'DeepSeek', '智谱 GLM', '通义千问', 'Moonshot',
    ]))
  })

  it('verifies the provider and fetches models before any model is pre-filled', async () => {
    fetchAiProviderModels.mockResolvedValue({ success: true, message: '已获取 2 个模型', models: ['model-a', 'model-b'] })
    const wrapper = mount(SettingsView, { global: { directives: { loading: () => undefined } } })
    await flushPromises()

    await wrapper.get('[data-test="add-ai-service"]').trigger('click')
    await wrapper.findAll('select')[0]!.setValue('deepseek')
    const password = wrapper.findAll('input').find((input) => input.attributes('type') === 'password')!
    await password.setValue('temporary-key')

    await wrapper.get('[data-test="verify-and-continue"]').trigger('click')
    await flushPromises()

    expect(fetchAiProviderModels).toHaveBeenCalledWith(expect.objectContaining({
      apiKey: 'temporary-key',
      defaultModel: '',
    }))
    expect(wrapper.get('[data-test="connected-banner"]').text()).toContain('DeepSeek 已连接')
    // 模型不被自动填满：由用户从服务商返回的列表中选择
    const modelInput = wrapper.find('input[list="provider-models"]')
    expect(modelInput.exists()).toBe(true)
    expect((modelInput.element as HTMLInputElement).value).toBe('')
    const datalistValues = wrapper.findAll('datalist#provider-models option').map((option) => option.attributes('value'))
    expect(datalistValues).toEqual(expect.arrayContaining(['model-a', 'model-b']))
    expect(wrapper.get('[data-test="save-config"]').text()).toBe('保存配置')
  })

  it('reveals the advanced model-name fallback when a provider returns no model list', async () => {
    fetchAiProviderModels.mockResolvedValue({ success: true, message: '已获取 0 个模型', models: [] })
    const wrapper = mount(SettingsView, { global: { directives: { loading: () => undefined } } })
    await flushPromises()

    await wrapper.get('[data-test="add-ai-service"]').trigger('click')
    await wrapper.findAll('select')[0]!.setValue('deepseek')
    await wrapper.findAll('input').find((input) => input.attributes('type') === 'password')!.setValue('temporary-key')
    await wrapper.get('[data-test="verify-and-continue"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="connected-banner"]').text()).toContain('DeepSeek 已连接')
    // 空模型列表：高级设置自动展开，模型名称输入框直接可见可填
    expect(wrapper.get('[data-test="advanced-toggle"]').text()).toContain('收起')
    expect(wrapper.find('input[list="provider-models"]').exists()).toBe(true)
    expect(wrapper.find('datalist#provider-models').text()).not.toContain('model-')
  })

  it('keeps the empty two-column layout after cancelling a connect flow', async () => {
    const wrapper = mount(SettingsView, { global: { directives: { loading: () => undefined } } })
    await flushPromises()

    await wrapper.get('[data-test="add-ai-service"]').trigger('click')
    expect(wrapper.find('[data-test="connect-form"]').exists()).toBe(true)
    await wrapper.findAll('button').find((button) => button.text() === '取消')!.trigger('click')
    expect(wrapper.find('[data-test="setup-empty"]').exists()).toBe(true)
    expect(wrapper.find('input[value="DeepSeek"]').exists()).toBe(false)
  })
})
