import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetJobDialog from './TargetJobDialog.vue'

describe('TargetJobDialog', () => {
  it('requires a title and a meaningful job description', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toBeUndefined()
    expect(wrapper.text()).toContain('请输入岗位名称')

    await wrapper.get('[data-test="job-title"]').setValue('Java 后端实习')
    await wrapper.get('[data-test="job-raw-text"]').setValue('内容过短')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('至少 20 个字符')
  })

  it('emits only user-entered target job data', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    await wrapper.get('[data-test="job-title"]').setValue('Java 后端实习')
    await wrapper.get('[data-test="job-company"]').setValue('示例科技')
    await wrapper.get('[data-test="job-raw-text"]').setValue('负责服务端接口开发、数据库设计以及相关系统维护工作。')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('create')).toEqual([[
      { jobTitle: 'Java 后端实习', companyName: '示例科技', rawText: '负责服务端接口开发、数据库设计以及相关系统维护工作。' },
    ]])
  })
})
