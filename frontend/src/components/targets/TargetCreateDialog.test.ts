import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetCreateDialog from './TargetCreateDialog.vue'

const resumes = [{
  id: 8,
  title: '通用简历',
  currentVersion: {
    id: 11,
    resumeId: 8,
    versionNo: 2,
    content: {},
    createdByType: 'user',
    createdAt: '2026-08-19T10:00:00',
  },
}]

describe('TargetCreateDialog', () => {
  it('does not submit a blank target name', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes } })
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toBeUndefined()
    expect(wrapper.text()).toContain('请输入求职目标名称')
  })

  it('submits the name and optional current resume version', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes } })
    await wrapper.get('[data-test="target-name"]').setValue('腾讯 · Java 后端实习')
    await wrapper.get('[data-test="target-resume"]').setValue('11')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toEqual([[{ name: '腾讯 · Java 后端实习', resumeVersionId: 11 }]])
  })

  it('keeps form content when creation fails', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes, errorMessage: '' } })
    await wrapper.get('[data-test="target-name"]').setValue('保留的目标')
    await wrapper.setProps({ errorMessage: '创建失败，请重试' })
    expect((wrapper.get('[data-test="target-name"]').element as HTMLInputElement).value).toBe('保留的目标')
    expect(wrapper.text()).toContain('创建失败，请重试')
  })
})
