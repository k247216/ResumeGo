// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeLibraryView from './ResumeLibraryView.vue'

const api = vi.hoisted(() => ({ listResumes: vi.fn(), getResumeVersions: vi.fn(), createResume: vi.fn(), deleteResume: vi.fn() }))
const jobApi = vi.hoisted(() => ({ getJobDescription: vi.fn() }))
const targets = vi.hoisted(() => ({
  targets: [] as Array<Record<string, unknown>>,
  loading: false,
  errorMessage: '',
  load: vi.fn(),
}))
const routerPush = vi.hoisted(() => vi.fn())

vi.mock('../../api/resume', () => api)
vi.mock('../../api/job', () => jobApi)
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targets }))
vi.mock('element-plus', async (importOriginal) => {
  const original = await importOriginal<typeof import('element-plus')>()
  return { ...original, ElMessage: { success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  RouterLink: { name: 'RouterLink', props: ['to'], template: '<a class="router-link-stub"><slot /></a>' },
}))

const version = (overrides: Record<string, unknown> = {}) => ({
  id: 9, resumeId: 3, versionNo: 2, content: { basicInfo: { name: '小林', targetRole: '后端开发' }, projects: [{ title: '项目一' }] },
  createdByType: 'user', createdAt: '2026-08-19',
  ...overrides,
})

function mountLibrary() {
  return mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
}

describe('ResumeLibraryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
  })

  it('offers a focused empty state and blank creation', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-empty"]').text()).toContain('创建第一份本地简历')
    expect(wrapper.text()).not.toContain('评分')
    expect(wrapper.text()).not.toContain('岗位匹配')
  })

  it('shows resume identity, recent versions, and honest content counts', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '后端简历', currentVersion: version() }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version()] })
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library"]').text()).toContain('后端简历')
    expect(wrapper.text()).toContain('小林')
    expect(wrapper.text()).toContain('v2')
    expect(wrapper.get('[data-test="inspector-versions"]').text()).toContain('最近版本')
    expect(wrapper.get('[data-test="inspector-content"]').text()).toContain('项目经历')
    expect(wrapper.get('[data-test="inspector-content"]').text()).toContain('技能项')
    // 无页数字段，如实不出现页数
    expect(wrapper.text()).not.toContain('页')
  })

  it('renders a retryable load error', async () => {
    api.listResumes.mockRejectedValue(new Error('读取失败'))
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
  })

  it('imports a Markdown file through the preview confirm flow', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 2, title: '张三', currentVersion: version({ id: 5, resumeId: 2, versionNo: 1, content: {}, createdAt: '2026-08-20' }) }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version({ id: 5, resumeId: 2, versionNo: 1, content: {}, createdAt: '2026-08-20' })] })
    api.createResume.mockResolvedValue({ success: true, data: { id: 2, title: '张三', currentVersion: version({ id: 5, resumeId: 2, versionNo: 1, content: {}, createdAt: '2026-08-20' }) } })
    const wrapper = mountLibrary()
    await flushPromises()

    const input = wrapper.get('[data-test="import-file"]')
    const file = new File(['# 张三\n\n## 基本信息\n- 姓名：张三\n- 求职意向：Java 后端\n\n## 项目经历\n- 订单系统优化\n'], '张三.md', { type: 'text/markdown' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    const dialog = wrapper.get('[data-test="import-dialog"]')
    expect(dialog.text()).toContain('张三')
    expect(dialog.text()).toContain('基本信息')
    expect(dialog.text()).toContain('项目经历 1 条')

    await wrapper.get('[data-test="import-confirm"]').trigger('click')
    await flushPromises()

    expect(api.createResume).toHaveBeenCalledWith({
      title: '张三',
      content: expect.objectContaining({ basicInfo: expect.objectContaining({ name: '张三' }) }),
      changeSummary: '从 Markdown 导入',
    })
  })

  it('shows paper thumbnails and quick entry for each local resume', async () => {
    const first = { id: 3, resumeId: 3, versionNo: 2, content: { basicInfo: { name: '小林', targetRole: '后端开发' } }, createdByType: 'user', createdAt: '2026-08-19' }
    const second = { id: 8, resumeId: 7, versionNo: 1, content: { basicInfo: { name: '小周' } }, createdByType: 'user', createdAt: '2026-08-18' }
    api.listResumes.mockResolvedValue({
      success: true,
      data: [
        { id: 3, title: '后端简历', currentVersion: first },
        { id: 7, title: '通用简历', currentVersion: second },
      ],
    })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [first] })
    const wrapper = mountLibrary()
    await flushPromises()

    expect(wrapper.get('[data-test="paper-card-3"]').text()).toContain('小林')
    expect(wrapper.get('[data-test="paper-card-7"]').text()).toContain('小周')
    expect(wrapper.find('[data-test="import-md"]').exists()).toBe(true)
  })

  it('lists which targets use the selected resume with real job context and opens them', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '后端简历', currentVersion: version() }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version()] })
    targets.targets = [
      { id: 5, name: '腾讯 目标', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
      { id: 7, name: '字节 目标', status: 'active', jobDescriptionId: null, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
      { id: 9, name: '阿里 Java 实习', status: 'active', jobDescriptionId: 11, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    jobApi.getJobDescription.mockImplementation(async (id: number) => ({
      success: true,
      data: id === 11
        ? { id: 11, companyName: '', jobTitle: 'Java 实习' }
        : { id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习' },
    }))
    const wrapper = mountLibrary()
    await flushPromises()

    const usedBy = wrapper.get('[data-test="inspector-used-by"]')
    expect(usedBy.text()).toContain('3 个求职目标')
    expect(usedBy.text()).toContain('腾讯 · Java 后端实习')
    // 无 JD 的目标如实回退目标名，不编造公司/岗位
    expect(usedBy.text()).toContain('字节 目标')
    // JD 无公司名时不丢掉目标名里的公司前缀
    expect(usedBy.text()).toContain('阿里 Java 实习')

    await wrapper.findAll('[data-test="used-by-target"]')[0].trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'targets', query: { targetId: '5' } })
  })

  it('collapses the version history to the three most recent and expands on demand', async () => {
    const makeVersion = (id: number, versionNo: number, createdAt: string) => ({ id, resumeId: 3, versionNo, content: {}, createdByType: 'user', createdAt })
    const versions = [makeVersion(1, 4, '2026-08-20'), makeVersion(2, 3, '2026-08-19'), makeVersion(3, 2, '2026-08-18'), makeVersion(4, 1, '2026-08-17')]
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '后端简历', currentVersion: versions[0] }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: versions })
    const wrapper = mountLibrary()
    await flushPromises()

    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(3)
    expect(wrapper.get('[data-test="versions-expand"]').text()).toContain('查看全部版本')
    await wrapper.get('[data-test="versions-expand"]').trigger('click')
    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(4)
    expect(wrapper.get('[data-test="versions-expand"]').text()).toContain('收起版本')
  })

  it('deletes the selected resume after confirmation and refreshes the list', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '小林简历', currentVersion: version() }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version()] })
    api.deleteResume.mockResolvedValue({ success: true, data: null })
    const wrapper = mountLibrary()
    await flushPromises()
    // select the paper
    await wrapper.get('[data-test="paper-open-3"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="delete-resume"]').text()).toContain('删除这份简历')
    // confirm then delete
    const { ElMessageBox, ElMessage } = await import('element-plus')
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
    await wrapper.get('[data-test="delete-resume"]').trigger('click')
    await flushPromises()
    expect(api.deleteResume).toHaveBeenCalledWith(3)
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('does not delete when the confirmation is cancelled', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [{ id: 3, title: '小林简历', currentVersion: version() }] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version()] })
    const wrapper = mountLibrary()
    await flushPromises()
    await wrapper.get('[data-test="paper-open-3"]').trigger('click')
    await flushPromises()
    const { ElMessageBox } = await import('element-plus')
    vi.mocked(ElMessageBox.confirm).mockRejectedValue(new Error('cancel') as never)
    await wrapper.get('[data-test="delete-resume"]').trigger('click')
    await flushPromises()
    expect(api.deleteResume).not.toHaveBeenCalled()
  })
})