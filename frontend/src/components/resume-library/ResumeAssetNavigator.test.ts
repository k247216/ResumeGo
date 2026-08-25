// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeAssetNavigator from './ResumeAssetNavigator.vue'
import type { Resume } from '../../types/resume'

const resume = (id: number, kind: Resume['kind'] = 'GENERAL'): Resume => ({
  id,
  title: `简历 ${id}`,
  kind,
  forkedFromVersionId: kind === 'JOB_EXPRESSION' ? 9 : null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: { id: id * 10, resumeId: id, parentVersionId: null, versionNo: 1, content: {}, createdByType: 'user', createdAt: new Date().toISOString() },
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
})

function mountNavigator(props: Record<string, unknown> = {}) {
  return mount(ResumeAssetNavigator, {
    props: {
      items: [resume(1), resume(2, 'JOB_EXPRESSION')],
      selectedId: 1,
      loading: false,
      error: '',
      filter: 'all',
      archivedCount: 3,
      ...props,
    },
  })
}

describe('ResumeAssetNavigator', () => {
  it('按基础简历/岗位版本分组并展示真实数量', () => {
    const wrapper = mountNavigator()
    const heads = wrapper.findAll('.nav-group-head')
    expect(heads).toHaveLength(2)
    expect(heads[0].text()).toContain('基础简历')
    expect(heads[0].text()).toContain('1')
    expect(heads[1].text()).toContain('岗位版本')
    expect(heads[1].text()).toContain('1')
  })

  it('资产行只展示标题/时间，岗位表达用克制标记；附真实模版缩略图', () => {
    const wrapper = mountNavigator()
    const row = wrapper.get('[data-test="asset-row-2"]')
    expect(row.text()).toContain('简历 2')
    expect(row.text()).toContain('今天')
    expect(wrapper.get('[data-test="asset-kind-2"]').text()).toBe('岗')
    // 真实模版微缩渲染缩略图
    expect(wrapper.findAll('.asset-thumb')).toHaveLength(2)
    expect(row.text()).not.toContain('岗位表达副本')
  })

  it('选中行使用 aria-current，点击发出 select', async () => {
    const wrapper = mountNavigator()
    expect(wrapper.get('[data-test="asset-row-1"]').attributes('aria-current')).toBe('true')
    await wrapper.get('[data-test="asset-row-2"]').trigger('click')
    expect(wrapper.emitted('select')).toEqual([[2]])
  })

  it('过滤三态 + 过滤菜单 + 归档独立入口', async () => {
    const wrapper = mountNavigator()
    expect(wrapper.findAll('[data-test="filter-all"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-test="filter-general"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-test="filter-expression"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-test="filter-menu"]')).toHaveLength(1)
    await wrapper.get('[data-test="filter-expression"]').trigger('click')
    expect(wrapper.emitted('update:filter')).toEqual([['expression']])
    await wrapper.get('[data-test="archived-entry"]').trigger('click')
    expect(wrapper.emitted('open-archived')).toHaveLength(1)
    expect(wrapper.get('[data-test="archived-entry"]').text()).toContain('回收站')
  })

  it('空库与失败状态诚实呈现', () => {
    const empty = mountNavigator({ items: [], loading: false, error: '' })
    expect(empty.get('[data-test="resume-library-empty"]').text()).toContain('还没有简历')

    const failed = mountNavigator({ items: [], loading: false, error: '读取失败' })
    expect(failed.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
  })
})
