// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AiSetupGuide from './AiSetupGuide.vue'

describe('AiSetupGuide', () => {
  it('offers configuration and dismissal after an unconfigured AI call', async () => {
    const wrapper = mount(AiSetupGuide, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })

    window.dispatchEvent(new CustomEvent('resumego:ai-not-configured'))
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('去配置')
    expect(wrapper.text()).toContain('暂不使用')
  })
})
