import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FirstRunEmptyState from './FirstRunEmptyState.vue'

describe('FirstRunEmptyState', () => {
  it('explains that a new workspace starts without personal data', () => {
    const wrapper = mount(FirstRunEmptyState)

    expect(wrapper.text()).toContain('从一个真实目标开始')
    expect(wrapper.text()).toContain('不会预置任何个人简历、岗位或面试记录')
    expect(wrapper.get('[data-test="create-first-workspace"]').text()).toBe('录入第一个岗位')
    // 4 步自然顺序：目标岗位 → 简历 → 日程 → 针对性模拟
    const steps = wrapper.findAll('.first-run-state__steps li')
    expect(steps).toHaveLength(4)
    expect(steps[0].text()).toContain('录入一个目标岗位')
    expect(steps[1].text()).toContain('关联或创建简历')
    expect(steps[2].text()).toContain('添加面试/笔试日程')
    expect(steps[3].text()).toContain('开始针对性模拟')
  })

  it('emits create when the primary action is selected', async () => {
    const wrapper = mount(FirstRunEmptyState)

    await wrapper.get('[data-test="create-first-workspace"]').trigger('click')

    expect(wrapper.emitted('create')).toHaveLength(1)
  })
})
