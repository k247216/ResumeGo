// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeVersionInspector from './ResumeVersionInspector.vue'
import type { Resume, ResumeVersion } from '../../types/resume'

const version = (id: number, versionNo: number, createdByType = 'user'): ResumeVersion => ({
  id,
  resumeId: 3,
  parentVersionId: id > 1 ? id - 1 : null,
  versionNo,
  content: {},
  changeSummary: `版本 ${versionNo}`,
  createdByType,
  createdAt: '2026-08-25',
})

const generalResume: Resume = {
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

const expressionResume: Resume = {
  ...generalResume,
  id: 7,
  title: '腾讯岗位表达',
  kind: 'JOB_EXPRESSION',
  forkedFromVersionId: 9,
}

function mountInspector(props: Record<string, unknown>) {
  return mount(ResumeVersionInspector, {
    props: {
      resume: generalResume,
      versions: [version(9, 2), version(8, 1)],
      selectedVersionId: 9,
      versionLoading: false,
      versionError: '',
      usedByTargets: [],
      usedByLoading: false,
      ...props,
    },
  })
}

describe('ResumeVersionInspector', () => {
  it('shows version history with read-only note and selection state', () => {
    const wrapper = mountInspector({})
    const section = wrapper.get('[data-test="inspector-versions"]')
    expect(section.text()).toContain('只读')
    expect(wrapper.get('[data-test="version-row-9"]').classes()).toContain('active')
    expect(wrapper.text()).toContain('手工维护')
  })

  it('shows the fork source version for expression copies', () => {
    const wrapper = mountInspector({ resume: expressionResume })
    expect(wrapper.get('[data-test="inspector-fork-source"]').text()).toContain('#9')
  })

  it('does not show the fork source section for general resumes', () => {
    const wrapper = mountInspector({})
    expect(wrapper.find('[data-test="inspector-fork-source"]').exists()).toBe(false)
  })

  it('emits select-version, archive and close', async () => {
    const wrapper = mountInspector({})
    await wrapper.get('[data-test="version-row-8"]').trigger('click')
    await wrapper.get('[data-test="archive-resume"]').trigger('click')
    await wrapper.get('[data-test="inspector-close"]').trigger('click')

    expect(wrapper.emitted('select-version')).toEqual([[8]])
    expect(wrapper.emitted('archive')).toHaveLength(1)
    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('emits open-target from used-by rows and keeps honest empty state', async () => {
    const wrapper = mountInspector({
      usedByTargets: [
        { targetId: 5, label: '腾讯 · Java 后端实习' },
        { targetId: 7, label: '字节 目标' },
      ],
    })
    expect(wrapper.get('[data-test="inspector-used-by"]').text()).toContain('2 个求职目标')
    await wrapper.findAll('[data-test="used-by-target"]')[0].trigger('click')
    expect(wrapper.emitted('open-target')).toEqual([[5]])

    const empty = mountInspector({ usedByTargets: [] })
    expect(empty.get('[data-test="inspector-used-by"]').text()).toContain('尚未关联求职目标')
  })

  it('collapses to the three most recent versions and expands on demand', async () => {
    const versions = [version(4, 4), version(3, 3), version(2, 2), version(1, 1)]
    const wrapper = mountInspector({ versions })
    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(3)
    expect(wrapper.get('[data-test="versions-expand"]').text()).toContain('查看全部版本')
    await wrapper.get('[data-test="versions-expand"]').trigger('click')
    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(4)
    expect(wrapper.get('[data-test="versions-expand"]').text()).toContain('收起版本')
  })
})
