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
  it('does not submit when company, role and name are all blank', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes } })
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toBeUndefined()
    expect(wrapper.text()).toContain('请至少填写公司或岗位')
  })

  it('auto-fills the target name from company and role, then submits with JD payload', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes } })
    await wrapper.get('[data-test="target-company"]').setValue('腾讯')
    await wrapper.get('[data-test="target-job-title"]').setValue('Java 后端实习')
    // 自动命名跟随公司 · 岗位
    expect((wrapper.get('[data-test="target-name"]').element as HTMLInputElement).value).toBe('腾讯 · Java 后端实习')
    await wrapper.get('[data-test="target-jd"]').setValue('负责后端服务开发')
    await wrapper.get('[data-test="target-resume"]').setValue('11')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toEqual([[
      {
        name: '腾讯 · Java 后端实习',
        companyName: '腾讯',
        jobTitle: 'Java 后端实习',
        jdText: '负责后端服务开发',
        resumeVersionId: 11,
      },
    ]])
  })

  it('keeps form content when creation fails', async () => {
    const wrapper = mount(TargetCreateDialog, { props: { open: true, resumes, errorMessage: '' } })
    await wrapper.get('[data-test="target-company"]').setValue('保留的公司')
    await wrapper.setProps({ errorMessage: '创建失败，请重试' })
    expect((wrapper.get('[data-test="target-company"]').element as HTMLInputElement).value).toBe('保留的公司')
    expect(wrapper.text()).toContain('创建失败，请重试')
  })
})
