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

  it('fills the company field from a hot company chip', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    const tencent = wrapper.get('[data-test="company-chips"]').findAll('button').find((b) => b.text() === '腾讯')
    expect(tencent).toBeTruthy()
    await tencent!.trigger('click')
    expect((wrapper.get('[data-test="job-company"]').element as HTMLInputElement).value).toBe('腾讯')
  })

  it('fills the title field from a hot job chip', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    const java = wrapper.get('[data-test="job-title-chips"]').findAll('button').find((b) => b.text() === 'Java 后端开发')
    expect(java).toBeTruthy()
    await java!.trigger('click')
    expect((wrapper.get('[data-test="job-title"]').element as HTMLInputElement).value).toBe('Java 后端开发')
  })

  it('filters hot companies by the typed keyword and hides on no match', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    await wrapper.get('[data-test="job-company"]').setValue('腾')
    expect(wrapper.findAll('[data-test="company-chips"] button').map((b) => b.text())).toEqual(['腾讯'])
    await wrapper.get('[data-test="job-company"]').setValue('tencent')
    expect(wrapper.findAll('[data-test="company-chips"] button').map((b) => b.text())).toEqual(['腾讯'])
    await wrapper.get('[data-test="job-company"]').setValue('不存在')
    expect(wrapper.find('[data-test="company-chips"]').exists()).toBe(false)
  })

  it('filters hot job titles by the typed keyword', async () => {
    const wrapper = mount(TargetJobDialog, { props: { open: true } })
    await wrapper.get('[data-test="job-title"]').setValue('前端')
    expect(wrapper.findAll('[data-test="job-title-chips"] button').map((b) => b.text())).toEqual(['前端开发'])
  })
})
