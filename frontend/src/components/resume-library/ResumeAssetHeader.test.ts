// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import ResumeAssetHeader from './ResumeAssetHeader.vue'
import type { Resume } from '../../types/resume'

const resume: Resume = {
  id: 11,
  title: '后端基础简历',
  kind: 'GENERAL',
  currentVersion: null,
}

describe('ResumeAssetHeader 收藏', () => {
  beforeEach(() => localStorage.clear())

  it('persists favorite by resume asset id', async () => {
    const wrapper = mount(ResumeAssetHeader, { props: { resume } })
    await wrapper.get('[data-test="toggle-favorite"]').trigger('click')
    expect(localStorage.getItem(`resumego:resume-favorite:${resume.id}`)).toBe('true')
  })

  it('restores favorite when switching back to an asset', async () => {
    localStorage.setItem(`resumego:resume-favorite:${resume.id}`, 'true')
    const wrapper = mount(ResumeAssetHeader, { props: { resume } })
    expect(wrapper.get('[data-test="toggle-favorite"]').attributes('aria-pressed')).toBe('true')
  })

  it('opens actionable asset operations instead of a placeholder toast', async () => {
    const wrapper = mount(ResumeAssetHeader, { props: { resume } })
    await wrapper.get('[data-test="asset-more"]').trigger('click')
    expect(wrapper.get('[data-test="asset-more-menu"]').text()).toContain('创建岗位版本')
    expect(wrapper.get('[data-test="more-delete"]').text()).toContain('归档到回收站')
    await wrapper.get('[data-test="more-create-job-version"]').trigger('click')
    expect(wrapper.emitted('fork')).toHaveLength(1)
  })
})
