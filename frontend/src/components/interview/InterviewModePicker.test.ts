// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InterviewModePicker from './InterviewModePicker.vue'

describe('InterviewModePicker', () => {
  it('renders three equal mode entries without placeholder copy', () => {
    const wrapper = mount(InterviewModePicker, { props: { modelValue: null } })
    expect(wrapper.findAll('[data-test^="mode-"]')).toHaveLength(3)
    expect(wrapper.get('[data-test="mode-ROLE_BASED"]').text()).toContain('岗位模拟')
    expect(wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').text()).toContain('知识训练')
    expect(wrapper.get('[data-test="mode-EXPERIENCE_SIMULATION"]').text()).toContain('真题演练')
    const text = wrapper.text()
    expect(text).not.toContain('规划中')
    expect(text).not.toContain('敬请期待')
  })

  it('marks the selected mode and emits update on click', async () => {
    const wrapper = mount(InterviewModePicker, { props: { modelValue: 'ROLE_BASED' } })
    expect(wrapper.get('[data-test="mode-ROLE_BASED"]').classes()).toContain('selected')

    await wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([['KNOWLEDGE_TRAINING']])
  })
})
