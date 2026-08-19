import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetDeleteDialog from './TargetDeleteDialog.vue'

const target = { id: 3, name: '腾讯后端目标', status: 'active' as const, jobDescriptionId: 4, resumeVersionId: 5, archivedAt: null, createdAt: '', updatedAt: '' }

describe('TargetDeleteDialog', () => {
  it('explains that linked local materials are preserved', () => {
    const wrapper = mount(TargetDeleteDialog, { props: { open: true, target } })
    expect(wrapper.text()).toContain('腾讯后端目标')
    expect(wrapper.text()).toContain('不会删除关联的简历版本和岗位材料')
  })

  it('requires the target name before confirming deletion', async () => {
    const wrapper = mount(TargetDeleteDialog, { props: { open: true, target } })
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('confirm')).toBeUndefined()
    await wrapper.get('[data-test="delete-confirm-name"]').setValue('腾讯后端目标')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })
})
