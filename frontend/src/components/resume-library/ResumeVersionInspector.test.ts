// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeVersionInspector from './ResumeVersionInspector.vue'
import type { Resume, ResumeVersion } from '../../types/resume'

const version = (id: number, versionNo: number, createdByType = 'user'): ResumeVersion => ({
  id,
  resumeId: 3,
  parentVersionId: versionNo > 1 ? id - 1 : null,
  versionNo,
  content: {},
  changeSummary: `第 ${versionNo} 版`,
  createdByType,
  createdAt: '2026-08-25T10:00:00',
})

const resume: Resume = {
  id: 3,
  title: '后端简历',
  kind: 'GENERAL',
  forkedFromVersionId: null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: version(9, 2),
  createdAt: '2026-08-25',
  updatedAt: '2026-08-25',
}

function mountInspector(props: Record<string, unknown> = {}) {
  return mount(ResumeVersionInspector, {
    props: {
      versions: [version(9, 2), version(8, 1)],
      resume,
      selectedVersion: version(9, 2),
      currentVersionId: 9,
      usedByTargets: [],
      usedByLoading: false,
      ...props,
    },
    global: { stubs: ['RouterLink'] },
  })
}

describe('ResumeVersionInspector（版本检查器）', () => {
  it('展示版本元数据：来源、时间、变更说明独立区块', () => {
    const wrapper = mountInspector({})
    expect(wrapper.get('.inspector-head').text()).toContain('V2')
    const meta = wrapper.get('[data-test="version-meta"]')
    expect(meta.text()).toContain('手工保存')
    expect(meta.text()).toContain('2026')
    expect(wrapper.get('[data-test="version-note"]').text()).toContain('第 2 版')
  })

  it('当前版本：主操作为进入编辑台，无只读提示', () => {
    const wrapper = mountInspector({})
    expect(wrapper.findAll('[data-test="continue-editing"]').length).toBe(1)
    expect(wrapper.find('[data-test="history-readonly-note"]').exists()).toBe(false)
  })

  it('历史版本：只读提示 + 主操作变为创建岗位副本', () => {
    const wrapper = mountInspector({
      versions: [version(9, 2), version(8, 1)],
      selectedVersion: version(8, 1),
      currentVersionId: 9,
    })
    expect(wrapper.get('[data-test="history-readonly-note"]').text()).toContain('只读')
    expect(wrapper.get('[data-test="open-fork"]').text()).toContain('基于此版本创建岗位副本')
    expect(wrapper.find('[data-test="continue-editing"]').exists()).toBe(false)
  })

  it('岗位表达资产展示跨资产来源版本', () => {
    const wrapper = mountInspector({
      resume: { ...resume, kind: 'JOB_EXPRESSION', forkedFromVersionId: 77 },
    })
    expect(wrapper.get('[data-test="version-meta"]').text()).toContain('#77')
  })

  it('引用状态：真实绑定列表 + 诚实空态 + 跳转事件', async () => {
    const withRefs = mountInspector({
      usedByTargets: [{ targetId: 5, label: '腾讯 · Java 后端实习' }],
    })
    expect(withRefs.get('[data-test="binding-status"]').text()).toContain('腾讯 · Java 后端实习')
    await withRefs.get('[data-test="used-by-target"]').trigger('click')
    expect(withRefs.emitted('open-target')).toEqual([[5]])

    const empty = mountInspector({})
    expect(empty.get('[data-test="binding-status"]').text()).toContain('尚未绑定')
  })

  it('归档入口发出 archive 事件；关闭发出 close', async () => {
    const wrapper = mountInspector({})
    await wrapper.get('[data-test="archive-resume"]').trigger('click')
    await wrapper.get('[data-test="inspector-close"]').trigger('click')
    expect(wrapper.emitted('archive')).toHaveLength(1)
    expect(wrapper.emitted('close')).toHaveLength(1)
  })
})
