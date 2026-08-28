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
    expect(wrapper.get('[data-test="version-meta"]').text()).toContain('历史版本')
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

  it('提供移入回收站入口，但检查器不提供关闭按钮并保持常驻', async () => {
    const wrapper = mountInspector({})
    await wrapper.get('[data-test="delete-resume"]').trigger('click')
    expect(wrapper.emitted('archive')).toHaveLength(1)
    expect(wrapper.find('[data-test="inspector-close"]').exists()).toBe(false)
  })

  it('已绑定时展示状态卡片而不是原生下拉框', async () => {
    const wrapper = mountInspector({
      availableTargets: [{ targetId: 12, label: '腾讯 Java 后端', resumeVersionId: 9 }],
    })
    expect(wrapper.find('[data-test="bind-target-select"]').exists()).toBe(false)
    expect(wrapper.get('[data-test="binding-summary"]').text()).toContain('腾讯 Java 后端')
    expect(wrapper.find('[data-test="change-binding"]').exists()).toBe(true)
  })

  it('未绑定时通过按钮打开目标选择面板', async () => {
    const wrapper = mountInspector({
      availableTargets: [{ targetId: 12, label: '腾讯 Java 后端', resumeVersionId: null }],
    })
    await wrapper.get('[data-test="open-binding-dialog"]').trigger('click')
    expect(wrapper.find('[data-test="resume-target-binding-dialog"]').exists()).toBe(true)
  })

  it('编辑说明进入编辑状态并发出更新说明事件，而不是创建副本', async () => {
    const wrapper = mountInspector({})
    await wrapper.get('.inline-action').trigger('click')
    expect((wrapper.get('[data-test="change-summary-input"]').element as HTMLTextAreaElement).value).toBe('第 2 版')
    await wrapper.get('[data-test="change-summary-input"]').setValue('补充 Redis 项目量化结果')
    await wrapper.get('[data-test="save-change-summary"]').trigger('click')
    expect(wrapper.emitted('update-summary')).toEqual([[9, '补充 Redis 项目量化结果']])
    expect(wrapper.emitted('fork')).toBeUndefined()
  })
})
