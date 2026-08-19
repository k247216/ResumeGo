import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FirstRunEmptyState from './FirstRunEmptyState.vue'

describe('FirstRunEmptyState', () => {
  it('explains that a new workspace starts without personal data', () => {
    const wrapper = mount(FirstRunEmptyState)

    expect(wrapper.text()).toContain('从一个真实目标开始')
    expect(wrapper.text()).toContain('不会预置任何个人简历、岗位或面试记录')
    expect(wrapper.get('[data-test="create-first-workspace"]').text()).toBe('开始创建')
  })

  it('emits create when the primary action is selected', async () => {
    const wrapper = mount(FirstRunEmptyState)

    await wrapper.get('[data-test="create-first-workspace"]').trigger('click')

    expect(wrapper.emitted('create')).toHaveLength(1)
  })
})
