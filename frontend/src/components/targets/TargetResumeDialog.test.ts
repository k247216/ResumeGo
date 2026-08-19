import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetResumeDialog from './TargetResumeDialog.vue'

const resumes = [{ id: 2, title: '后端简历', currentVersion: { id: 7, resumeId: 2, versionNo: 3, content: {}, createdByType: 'user', createdAt: '2026-08-19' } }]

describe('TargetResumeDialog', () => {
  it('selects an existing current resume version', async () => {
    const wrapper = mount(TargetResumeDialog, { props: { open: true, resumes } })
    await wrapper.get('[data-test="resume-version"]').setValue('7')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('select')).toEqual([[7]])
  })

  it('explains when there is no local resume to select', () => {
    const wrapper = mount(TargetResumeDialog, { props: { open: true, resumes: [] } })
    expect(wrapper.text()).toContain('先创建一份本地简历')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})
