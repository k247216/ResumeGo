// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeLibraryView from './ResumeLibraryView.vue'

const api = vi.hoisted(() => ({
  listResumes: vi.fn(),
  getResumeVersions: vi.fn(),
  createResume: vi.fn(),
  deleteResume: vi.fn(),
  forkResumeVersion: vi.fn(),
  archiveResume: vi.fn(),
  restoreResume: vi.fn(),
  renameResume: vi.fn(),
}))
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

const generalResume = {
  id: 3,
  title: '后端简历',
  kind: 'GENERAL',
  forkedFromVersionId: null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: {
    id: 9, resumeId: 3, versionNo: 2,
    content: { basicInfo: { name: '小林', targetRole: '后端开发' }, projects: [{ title: '项目一' }] },
    createdByType: 'user', createdAt: '2026-08-19',
  },
  createdAt: '2026-08-19',
  updatedAt: '2026-08-19',
}
const expressionResume = {
  id: 7,
  title: '腾讯岗位表达',
  kind: 'JOB_EXPRESSION',
  forkedFromVersionId: 9,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: {
    id: 12, resumeId: 7, versionNo: 1, content: { basicInfo: { name: '小林' } },
    createdByType: 'fork', createdAt: '2026-08-20',
  },
  createdAt: '2026-08-20',
  updatedAt: '2026-08-20',
}

function mountLibrary() {
  return mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
}

describe('ResumeLibraryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
    api.getResumeVersions.mockResolvedValue({ success: true, data: [] })
  })

  it('offers a focused empty state and blank creation', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-empty"]').text()).toContain('创建第一份本地简历')
    expect(wrapper.text()).not.toContain('评分')
    expect(wrapper.text()).not.toContain('岗位匹配')
  })

  it('lists assets with kind badges and current version; versions are not separate resumes', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    const wrapper = mountLibrary()
    await flushPromises()

    const generalRow = wrapper.get('[data-test="asset-row-3"]')
    expect(generalRow.text()).toContain('后端简历')
    expect(generalRow.text()).toContain('V2')
    expect(wrapper.get('[data-test="asset-kind-3"]').text()).toBe('通用')

    const expressionRow = wrapper.get('[data-test="asset-row-7"]')
    expect(wrapper.get('[data-test="asset-kind-7"]').text()).toBe('岗位表达')
    expect(expressionRow.text()).toContain('V1')

    // 两份资产（不是把 V1/V2 当成两份简历）
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(2)
  })

  it('renders a retryable load error', async () => {
    api.listResumes.mockRejectedValue(new Error('读取失败'))
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
  })

  it('filters assets by kind through the real API parameter', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    const wrapper = mountLibrary()
    await flushPromises()
    vi.mocked(api.listResumes).mockClear()

    await wrapper.get('[data-test="filter-expression"]').trigger('click')
    await flushPromises()
    expect(api.listResumes).toHaveBeenLastCalledWith('JOB_EXPRESSION', false)

    await wrapper.get('[data-test="filter-archived"]').trigger('click')
    await flushPromises()
    expect(api.listResumes).toHaveBeenLastCalledWith(undefined, true)
  })

  it('shows the fork dialog with source title and version, then forks without changing the source', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    const forked = {
      ...expressionResume,
      id: 8,
      title: '后端简历 · 岗位表达',
      forkedFromVersionId: 9,
      currentVersion: { id: 13, resumeId: 8, versionNo: 1, content: {}, createdByType: 'fork', createdAt: '2026-08-21' },
    }
    api.forkResumeVersion.mockImplementation(async () => {
      api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume, forked] })
      return { success: true, data: forked }
    })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [] })
    const wrapper = mountLibrary()
    await flushPromises()

    // 选中源简历并打开 fork 弹窗
    await wrapper.get('[data-test="asset-row-3"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="open-fork"]').trigger('click')

    const dialog = wrapper.get('[data-test="fork-dialog"]')
    expect(dialog.get('[data-test="fork-source-label"]').text()).toContain('后端简历')
    expect(dialog.get('[data-test="fork-source-label"]').text()).toContain('V2')

    await dialog.get('[data-test="fork-title-input"]').setValue('后端简历 · 岗位表达')
    await dialog.get('[data-test="fork-confirm"]').trigger('click')
    await flushPromises()

    expect(api.forkResumeVersion).toHaveBeenCalledWith(9, '后端简历 · 岗位表达')
    // 源资产仍在列表且未被修改（fork 创建了新资产 8）
    const rows = wrapper.findAll('[data-test^="asset-row-"]')
    expect(rows).toHaveLength(3)
    expect(wrapper.get('[data-test="asset-row-3"]').text()).toContain('V2')
    expect(wrapper.get('[data-test="asset-kind-8"]').text()).toBe('岗位表达')
  })

  it('archives the selected resume after confirmation and removes it from the default list', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    api.archiveResume.mockImplementation(async () => {
      api.listResumes.mockResolvedValue({ success: true, data: [expressionResume] })
      return { success: true, data: generalResume }
    })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="asset-row-3"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="archive-resume"]').trigger('click')

    const dialog = wrapper.get('[data-test="archive-dialog"]')
    expect(dialog.get('[data-test="archive-source-label"]').text()).toContain('后端简历')
    await dialog.get('[data-test="archive-confirm"]').trigger('click')
    await flushPromises()

    expect(api.archiveResume).toHaveBeenCalledWith(3)
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(1)
  })

  it('collapses the version history to the three most recent and marks history read-only', async () => {
    const makeVersion = (id: number, versionNo: number, createdAt: string) => ({
      id, resumeId: 3, versionNo, content: {}, createdByType: 'user', createdAt,
    })
    const versions = [makeVersion(1, 4, '2026-08-20'), makeVersion(2, 3, '2026-08-19'), makeVersion(3, 2, '2026-08-18'), makeVersion(4, 1, '2026-08-17')]
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: versions })
    const wrapper = mountLibrary()
    await flushPromises()

    const inspector = wrapper.get('[data-test="resume-version-inspector"]')
    expect(inspector.get('[data-test="inspector-versions"]').text()).toContain('只读')
    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(3)
    await wrapper.get('[data-test="versions-expand"]').trigger('click')
    expect(wrapper.findAll('[data-test^="version-row-"]')).toHaveLength(4)
  })

  it('shows the fork source version in the inspector for expression copies', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [expressionResume] })
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="inspector-fork-source"]').text()).toContain('#9')
  })

  it('can close and reopen the inspector', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    const wrapper = mountLibrary()
    await flushPromises()

    expect(wrapper.find('[data-test="resume-version-inspector"]').exists()).toBe(true)
    await wrapper.get('[data-test="inspector-close"]').trigger('click')
    expect(wrapper.find('[data-test="resume-version-inspector"]').exists()).toBe(false)
    await wrapper.get('[data-test="inspector-reopen"]').trigger('click')
    expect(wrapper.find('[data-test="resume-version-inspector"]').exists()).toBe(true)
  })

  it('imports a Markdown file through the preview confirm flow', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    api.createResume.mockResolvedValue({ success: true, data: generalResume })
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

  it('lists which targets use the selected resume with real job context and opens them', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [generalResume.currentVersion] })
    targets.targets = [
      { id: 5, name: '腾讯 目标', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
      { id: 7, name: '字节 目标', status: 'active', jobDescriptionId: null, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    jobApi.getJobDescription.mockImplementation(async () => ({
      success: true,
      data: { id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习' },
    }))
    const wrapper = mountLibrary()
    await flushPromises()

    const usedBy = wrapper.get('[data-test="inspector-used-by"]')
    expect(usedBy.text()).toContain('2 个求职目标')
    expect(usedBy.text()).toContain('腾讯 · Java 后端实习')
    expect(usedBy.text()).toContain('字节 目标')

    await wrapper.findAll('[data-test="used-by-target"]')[0].trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'targets', query: { targetId: '5' } })
  })
})
