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

const version = (id: number, versionNo: number, overrides: Record<string, unknown> = {}) => ({
  id,
  resumeId: 3,
  parentVersionId: versionNo > 1 ? id - 1 : null,
  versionNo,
  content: {
    basicInfo: { name: '小林', targetRole: '后端开发' },
    projects: [{ title: '项目一' }],
    skills: ['Java'],
  },
  createdByType: 'user',
  createdAt: '2026-08-19',
  ...overrides,
})

const generalResume = {
  id: 3,
  title: '后端简历',
  kind: 'GENERAL',
  forkedFromVersionId: null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: version(9, 2),
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
  currentVersion: version(12, 1, { resumeId: 7, createdByType: 'fork' }),
  createdAt: '2026-08-20',
  updatedAt: '2026-08-20',
}

function mountLibrary() {
  return mount(ResumeLibraryView, { global: { stubs: ['RouterLink'] } })
}

describe('ResumeLibraryView（Version Studio）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
    api.getResumeVersions.mockResolvedValue({ success: true, data: [] })
  })

  it('首次空库：诚实空态 + 新建/导入两个入口', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-empty"]').text()).toContain('还没有简历')
    expect(wrapper.findAll('[data-test="create-blank"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-test="import-md-empty"]')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('评分')
  })

  it('资产导航分组展示：基础简历/岗位版本 + 数量；V 不重复为资产', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    const wrapper = mountLibrary()
    await flushPromises()

    const groups = wrapper.findAll('.nav-group-head')
    expect(groups).toHaveLength(2)
    expect(groups[0].text()).toContain('基础简历')
    expect(groups[1].text()).toContain('岗位版本')
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(2)
    // 岗位表达副本使用克制标记
    expect(wrapper.get('[data-test="asset-kind-7"]').text()).toBe('岗')
  })

  it('列表失败可重试', async () => {
    api.listResumes.mockRejectedValue(new Error('读取失败'))
    const wrapper = mountLibrary()
    await flushPromises()
    expect(wrapper.get('[data-test="resume-library-error"]').text()).toContain('重新加载')
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    await wrapper.get('[data-test="retry-load"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(1)
  })

  it('过滤调用真实 API 参数；归档经独立入口', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    const wrapper = mountLibrary()
    await flushPromises()
    vi.mocked(api.listResumes).mockClear()

    await wrapper.get('[data-test="filter-expression"]').trigger('click')
    await flushPromises()
    expect(api.listResumes).toHaveBeenLastCalledWith('JOB_EXPRESSION', false)

    await wrapper.get('[data-test="archived-entry"]').trigger('click')
    await flushPromises()
    expect(api.listResumes).toHaveBeenLastCalledWith(undefined, true)
  })

  it('选中资产后：身份栏/版本轨道/正文/检查器同步同一真实对象', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    api.getResumeVersions.mockResolvedValue({
      success: true,
      data: [version(9, 2), version(8, 1)],
    })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="asset-row-3"]').trigger('click')
    await flushPromises()

    // 身份栏
    expect(wrapper.get('[data-test="asset-header"]').text()).toContain('后端简历')
    expect(wrapper.get('[data-test="asset-kind-label"]').text()).toBe('通用简历')
    // 版本轨道按 versionNo 升序
    const rail = wrapper.get('[data-test="version-rail"]')
    expect(rail.text()).toContain('V1')
    expect(rail.text()).toContain('V2')
    // 正文画布是真实内容
    expect(wrapper.get('[data-test="resume-document-preview"]').text()).toContain('小林')
    // 检查器版本信息
    expect(wrapper.get('[data-test="version-meta"]').text()).toContain('手工保存')
  })

  it('点击历史版本：只读预览，不改变当前版本', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [version(9, 2), version(8, 1)] })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="rail-version-1"]').trigger('click')
    await flushPromises()

    // 历史版本只读徽标出现；当前版本仍是 V9（currentVersionId 未变）
    expect(wrapper.find('[data-test="rail-readonly-badge"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="history-readonly-note"]').text()).toContain('只读')
    // 操作层级：历史版本不提供进入编辑台
    expect(wrapper.find('[data-test="continue-editing"]').exists()).toBe(false)
  })

  it('V2+ 提供与上一版本对比；变化摘要来自确定性差异', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({
      success: true,
      data: [
        version(9, 2, { content: { basicInfo: { name: '小林' }, summary: '新简介' } }),
        version(8, 1, { content: { basicInfo: { name: '小林' }, summary: '旧简介', skills: ['Java'] } }),
      ],
    })
    const wrapper = mountLibrary()
    await flushPromises()

    // 默认选中 V2（当前版本），有父版本 → 提供对比
    await wrapper.get('[data-test="toggle-compare"]').trigger('click')
    await flushPromises()

    const summary = wrapper.get('[data-test="change-summary"]')
    expect(summary.text()).toContain('个人简介')
    expect(summary.text()).toContain('修改')
    expect(summary.text()).toContain('技能')
    expect(summary.text()).toContain('删除')
    // V1 无对比入口的诚实态在 rail 选中 V1 时出现
  })

  it('fork 弹窗显示源标题与 Vn，成功后选中新资产 V1', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [generalResume.currentVersion] })
    const forked = {
      ...expressionResume,
      id: 8,
      title: '后端简历 · 岗位表达',
      currentVersion: { id: 13, resumeId: 8, versionNo: 1, content: {}, createdByType: 'fork', createdAt: '2026-08-21' },
    }
    api.forkResumeVersion.mockImplementation(async () => {
      api.listResumes.mockResolvedValue({ success: true, data: [generalResume, forked] })
      return { success: true, data: forked }
    })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="open-fork"]').trigger('click')
    const dialog = wrapper.get('[data-test="fork-dialog"]')
    expect(dialog.get('[data-test="fork-source-label"]').text()).toContain('后端简历')
    await dialog.get('[data-test="fork-title-input"]').setValue('后端简历 · 岗位表达')
    await dialog.get('[data-test="fork-confirm"]').trigger('click')
    await flushPromises()

    expect(api.forkResumeVersion).toHaveBeenCalledWith(9, '后端简历 · 岗位表达')
    // 源资产不变，新资产进入列表
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(2)
  })

  it('归档确认后从默认列表移除', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume, expressionResume] })
    api.archiveResume.mockImplementation(async () => {
      api.listResumes.mockResolvedValue({ success: true, data: [expressionResume] })
      return { success: true, data: generalResume }
    })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="archive-resume"]').trigger('click')
    const dialog = wrapper.get('[data-test="archive-dialog"]')
    await dialog.get('[data-test="archive-confirm"]').trigger('click')
    await flushPromises()

    expect(api.archiveResume).toHaveBeenCalledWith(3)
    expect(wrapper.findAll('[data-test^="asset-row-"]')).toHaveLength(1)
  })

  it('改名入口提交后调用真实 API', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [] })
    api.renameResume.mockResolvedValue({ success: true, data: generalResume })
    const wrapper = mountLibrary()
    await flushPromises()

    await wrapper.get('[data-test="start-rename"]').trigger('click')
    await wrapper.get('[data-test="rename-input"]').setValue('改名后')
    await wrapper.get('[data-test="rename-submit"]').trigger('click')
    await flushPromises()

    expect(api.renameResume).toHaveBeenCalledWith(3, '改名后')
  })

  it('导入 Markdown 走确认流程', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [] })
    api.createResume.mockResolvedValue({ success: true, data: generalResume })
    const wrapper = mountLibrary()
    await flushPromises()

    const input = wrapper.get('[data-test="import-file"]')
    const file = new File(['# 张三\n\n## 基本信息\n- 姓名：张三\n\n## 项目经历\n- 订单系统\n'], '张三.md', { type: 'text/markdown' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    const dialog = wrapper.get('[data-test="import-dialog"]')
    expect(dialog.text()).toContain('张三')
    await wrapper.get('[data-test="import-confirm"]').trigger('click')
    await flushPromises()
    expect(api.createResume).toHaveBeenCalledWith({
      title: '张三',
      content: expect.objectContaining({ basicInfo: expect.objectContaining({ name: '张三' }) }),
      changeSummary: '从 Markdown 导入',
    })
  })

  it('列出引用本简历版本的目标并跳转', async () => {
    api.listResumes.mockResolvedValue({ success: true, data: [generalResume] })
    api.getResumeVersions.mockResolvedValue({ success: true, data: [generalResume.currentVersion] })
    targets.targets = [
      { id: 5, name: '腾讯 目标', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    jobApi.getJobDescription.mockResolvedValue({
      success: true,
      data: { id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习' },
    })
    const wrapper = mountLibrary()
    await flushPromises()

    const usedBy = wrapper.get('[data-test="inspector-used-by"]')
    expect(usedBy.text()).toContain('1 个求职目标')
    expect(usedBy.text()).toContain('腾讯 · Java 后端实习')
    await wrapper.get('[data-test="used-by-target"]').trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'targets', query: { targetId: '5' } })
  })
})
