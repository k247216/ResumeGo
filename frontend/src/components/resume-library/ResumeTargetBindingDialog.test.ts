// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeTargetBindingDialog from './ResumeTargetBindingDialog.vue'

const targets = [
  { targetId: 7, label: '腾讯 Java 后端', stageLabel: '技术面', resumeVersionId: null },
  { targetId: 8, label: '字节 后端开发', stageLabel: '笔试', resumeVersionId: 31 },
]

describe('ResumeTargetBindingDialog', () => {
  it('emits a selected target from the card dialog', async () => {
    const wrapper = mount(ResumeTargetBindingDialog, { props: { open: true, targets } })
    expect(wrapper.text()).toContain('已绑定简历版本')
    expect(wrapper.text()).not.toContain('V31')
    await wrapper.get('[data-test="target-option-7"]').trigger('click')
    await wrapper.get('[data-test="confirm-binding"]').trigger('click')
    expect(wrapper.emitted('confirm')?.[0]).toEqual([7])
  })
})
