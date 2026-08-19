import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeLibraryView from './ResumeLibraryView.vue'

const api = vi.hoisted(() => ({ listResumes: vi.fn(), getResumeVersions: vi.fn() }))
vi.mock('../../api/resume', () => api)

describe('ResumeLibraryView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('offers a focused empty state and blank creation', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    const wrapper = mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-empty"]').text()).toContain('创建第一份本地简历')
    expect(wrapper.text()).not.toContain('评分')
    expect(wrapper.text()).not.toContain('岗位匹配')
  })

  it('shows resume identity, current version, and version history', async () => {
    const version = { id: 9, resumeId: 3, versionNo: 2, content: { basicInfo: { name: '小林', targetRole: '后端开发' }, projects: [{ title: '项目一' }] }, createdByType: 'user', createdAt: '2026-08-19' }
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '后端简历', currentVersion: version }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version] })
    const wrapper = mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library"]').text()).toContain('后端简历')
    expect(wrapper.text()).toContain('小林')
    expect(wrapper.text()).toContain('v2')
    expect(wrapper.text()).toContain('版本历史')
  })

  it('renders a retryable load error', async () => {
    api.listResumes.mockRejectedValue(new Error('读取失败'))
    const wrapper = mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
  })
})
