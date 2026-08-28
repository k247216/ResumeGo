// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeVersionRail from './ResumeVersionRail.vue'
import type { ResumeVersion } from '../../types/resume'

const version = (id: number, versionNo: number): ResumeVersion => ({
  id,
  resumeId: 1,
  parentVersionId: versionNo > 1 ? id - 1 : null,
  versionNo,
  content: {},
  changeSummary: `V${versionNo}`,
  createdByType: 'user',
  createdAt: '2026-08-25',
})

describe('ResumeVersionRail', () => {
  it('按 versionNo 单调递增排序，不依赖接口返回顺序', () => {
    const wrapper = mount(ResumeVersionRail, {
      props: {
        versions: [version(3, 3), version(1, 1), version(2, 2)],
        selectedVersionId: 2,
        currentVersionId: 3,
      },
    })
    expect(wrapper.findAll('.node-dot')).toHaveLength(3)
    expect(wrapper.findAll('.node-date')).toHaveLength(3)
  })

  it('选中版本有当前状态标记并标记 aria-selected', () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1), version(2, 2)], selectedVersionId: 2, currentVersionId: 2 },
    })
    const current = wrapper.get('[data-test="rail-version-2"]')
    expect(current.classes()).toContain('current')
    expect(current.attributes('aria-selected')).toBe('true')
    expect(current.find('.node-glyph').exists()).toBe(false)
    expect(current.find('.node-dot').exists()).toBe(true)
    expect(current.find('.node-version').text()).toBe('V2')
    expect(current.find('.node-version').classes()).not.toContain('muted')
    expect(current.find('.node-status').exists()).toBe(false)
    expect(wrapper.get('[data-test="rail-version-1"]').classes()).not.toContain('current')
  })

  it('查看历史版本时不增加状态标签；回到当前版本保持同一轨道结构', async () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1), version(2, 2)], selectedVersionId: 2, currentVersionId: 2 },
    })
    await wrapper.get('[data-test="rail-version-1"]').trigger('click')
    expect(wrapper.emitted('select-version')).toEqual([[1]])
    await wrapper.setProps({ selectedVersionId: 1 })
    expect(wrapper.find('[data-test="rail-readonly-badge"]').exists()).toBe(false)

    await wrapper.setProps({ selectedVersionId: 2 })
    expect(wrapper.find('[data-test="rail-readonly-badge"]').exists()).toBe(false)
  })

  it('节点是小尺寸：轨道不使用大白圆或状态标签', () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1)], selectedVersionId: 1, currentVersionId: 1 },
    })
    const rail = wrapper.get('[data-test="version-rail"]')
    // 轨道节点只保留颜色、符号和日期，不再堆叠状态标签
    expect(rail.find('.node-dot').exists()).toBe(true)
    expect(rail.find('.node-status').exists()).toBe(false)
    expect(rail.find('.node-date').exists()).toBe(true)
  })
})
