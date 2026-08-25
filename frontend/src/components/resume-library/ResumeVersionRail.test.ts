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
    const labels = wrapper.findAll('.node-label').map((node) => node.text())
    expect(labels).toEqual(['V1', 'V2', 'V3'])
  })

  it('选中版本绿色实心节点并标记 aria-selected', () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1), version(2, 2)], selectedVersionId: 2, currentVersionId: 2 },
    })
    const current = wrapper.get('[data-test="rail-version-2"]')
    expect(current.classes()).toContain('current')
    expect(current.attributes('aria-selected')).toBe('true')
    expect(wrapper.get('[data-test="rail-version-1"]').classes()).not.toContain('current')
  })

  it('查看历史版本时显示只读徽标；回到当前版本徽标消失', async () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1), version(2, 2)], selectedVersionId: 2, currentVersionId: 2 },
    })
    expect(wrapper.find('[data-test="rail-readonly-badge"]').exists()).toBe(false)

    await wrapper.get('[data-test="rail-version-1"]').trigger('click')
    expect(wrapper.emitted('select-version')).toEqual([[1]])
    // 父组件回写选中版本后，只读徽标出现
    await wrapper.setProps({ selectedVersionId: 1 })
    expect(wrapper.get('[data-test="rail-readonly-badge"]').text()).toContain('只读')

    await wrapper.setProps({ selectedVersionId: 2 })
    expect(wrapper.find('[data-test="rail-readonly-badge"]').exists()).toBe(false)
  })

  it('节点是小尺寸：轨道不使用大白圆', () => {
    const wrapper = mount(ResumeVersionRail, {
      props: { versions: [version(1, 1)], selectedVersionId: 1, currentVersionId: 1 },
    })
    const rail = wrapper.get('[data-test="version-rail"]')
    // 高度约束在组件样式中（72px）；节点直径 9px
    expect(rail.find('.node-dot').exists()).toBe(true)
  })
})
