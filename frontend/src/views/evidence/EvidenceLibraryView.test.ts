import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import EvidenceLibraryView from './EvidenceLibraryView.vue'

const api = vi.hoisted(() => ({ list: vi.fn(), create: vi.fn() }))
vi.mock('../../api/evidence', () => ({ listEvidences: api.list, createEvidence: api.create }))

describe('EvidenceLibraryView', () => {
  beforeEach(() => { vi.clearAllMocks(); api.list.mockResolvedValue({ success: true, data: [] }) })

  it('renders a local evidence empty state without project-era copy', async () => {
    const wrapper = mount(EvidenceLibraryView)
    await flushPromises()
    expect(wrapper.get('[data-test="evidence-empty"]').text()).toContain('还没有能力证据')
    expect(wrapper.text()).not.toContain('Sprint')
  })

  it('validates required facts before creation', async () => {
    const wrapper = mount(EvidenceLibraryView)
    await flushPromises()
    await wrapper.get('[data-test="open-evidence-form"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('请补充标题、实际行动和至少一个技能标签')
    expect(api.create).not.toHaveBeenCalled()
  })

  it('preserves entered facts when saving fails', async () => {
    api.create.mockRejectedValue(new Error('保存失败'))
    const wrapper = mount(EvidenceLibraryView)
    await flushPromises()
    await wrapper.get('[data-test="open-evidence-form"]').trigger('click')
    await wrapper.get('[data-test="evidence-title"]').setValue('真实项目')
    await wrapper.get('[data-test="evidence-action"]').setValue('独立完成接口开发和测试')
    await wrapper.get('[data-test="evidence-skills"]').setValue('Java, MySQL')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('保存失败')
    expect((wrapper.get('[data-test="evidence-title"]').element as HTMLInputElement).value).toBe('真实项目')
  })
})
