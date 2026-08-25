// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResumeAssetList from './ResumeAssetList.vue'
import type { Resume } from '../../types/resume'

const resume = (id: number, overrides: Partial<Resume> = {}): Resume => ({
  id,
  title: `简历 ${id}`,
  kind: 'GENERAL',
  forkedFromVersionId: null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: { id: id * 10, resumeId: id, parentVersionId: null, versionNo: 1, content: {}, createdByType: 'user', createdAt: '2026-08-25' },
  createdAt: '2026-08-25',
  updatedAt: '2026-08-25',
  ...overrides,
})

describe('ResumeAssetList', () => {
  it('renders asset rows with title, kind badge, current version and updated time', () => {
    const wrapper = mount(ResumeAssetList, {
      props: {
        items: [
          resume(1, { title: '通用简历', updatedAt: '2026-08-25T10:00:00' }),
          resume(2, { title: '腾讯岗位表达', kind: 'JOB_EXPRESSION', currentVersion: { id: 20, resumeId: 2, parentVersionId: null, versionNo: 3, content: {}, createdByType: 'fork', createdAt: '2026-08-25' } }),
        ],
        selectedId: 1,
        loading: false,
        error: '',
      },
    })

    const row1 = wrapper.get('[data-test="asset-row-1"]')
    expect(row1.text()).toContain('通用简历')
    expect(wrapper.get('[data-test="asset-kind-1"]').text()).toBe('通用')
    expect(row1.text()).toContain('V1')

    const row2 = wrapper.get('[data-test="asset-row-2"]')
    expect(wrapper.get('[data-test="asset-kind-2"]').text()).toBe('岗位表达')
    expect(row2.text()).toContain('V3')
    expect(row2.text()).toContain('更新')

    expect(row1.classes()).toContain('selected')
  })

  it('shows the honest empty state with blank creation and import entries', () => {
    const wrapper = mount(ResumeAssetList, {
      props: { items: [], selectedId: null, loading: false, error: '' },
    })
    const empty = wrapper.get('[data-test="resume-library-empty"]')
    expect(empty.text()).toContain('创建第一份本地简历')
    expect(empty.find('[data-test="create-blank"]').exists()).toBe(true)
    expect(empty.find('[data-test="import-md-empty"]').exists()).toBe(true)
  })

  it('shows a retryable load error', () => {
    const wrapper = mount(ResumeAssetList, {
      props: { items: [], selectedId: null, loading: false, error: '后端不可用' },
    })
    expect(wrapper.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
  })

  it('emits select on row click and retry on error retry', async () => {
    const wrapper = mount(ResumeAssetList, {
      props: { items: [resume(1)], selectedId: null, loading: false, error: '' },
    })
    await wrapper.get('[data-test="asset-row-1"]').trigger('click')
    expect(wrapper.emitted('select')).toEqual([[1]])
  })

  it('emits create-blank and import from the empty state', async () => {
    const wrapper = mount(ResumeAssetList, {
      props: { items: [], selectedId: null, loading: false, error: '' },
    })
    await wrapper.get('[data-test="create-blank"]').trigger('click')
    await wrapper.get('[data-test="import-md-empty"]').trigger('click')
    expect(wrapper.emitted('create-blank')).toHaveLength(1)
    expect(wrapper.emitted('import')).toHaveLength(1)
  })
})
