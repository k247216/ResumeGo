import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InterviewLobbyHero from './InterviewLobbyHero.vue'

describe('InterviewLobbyHero', () => {
  it('explains locked target practice and emits the two primary intents', async () => {
    const wrapper = mount(InterviewLobbyHero, {
      props: {
        fromTarget: true,
        recordCount: 3,
        completedCount: 2,
        inProgressCount: 1,
        growthLoading: false,
      },
      global: {
        stubs: {
          ElIcon: { template: '<span><slot /></span>' },
          ArrowRight: true,
          Loading: true,
          Trophy: true,
        },
      },
    })

    expect(wrapper.text()).toContain('用当前目标验证这版简历')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('2')

    await wrapper.get('[data-test="create-interview"]').trigger('click')
    await wrapper.get('[data-test="view-growth"]').trigger('click')

    expect(wrapper.emitted('create')).toHaveLength(1)
    expect(wrapper.emitted('view-growth')).toHaveLength(1)
  })
})
