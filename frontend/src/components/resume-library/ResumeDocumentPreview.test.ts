// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeDocumentPreview from './ResumeDocumentPreview.vue'
import ResumeCompareToolbar from './ResumeCompareToolbar.vue'

const content = {
  basicInfo: { name: '小林', targetRole: '后端开发' },
  summary: '专注于可靠的后端系统。',
}

describe('ResumeDocumentPreview 工作区比例', () => {
  it('uses the readable paper scale without exposing a zoom label', () => {
    const wrapper = mount(ResumeDocumentPreview, { props: { content, scale: 0.7 } })
    expect(wrapper.find('.paper-holder').attributes('style')).toContain('555.8px')
    expect(wrapper.find('.studio-preview').classes()).toContain('scrollable')
  })
})

describe('ResumeCompareToolbar', () => {
  it('does not render a misleading zoom control', () => {
    const wrapper = mount(ResumeCompareToolbar, {
      props: { selectedVersionNo: 2, viewingCurrent: true, comparing: true },
    })
    expect(wrapper.find('[data-test="zoom-controls"]').exists()).toBe(false)
  })
})
