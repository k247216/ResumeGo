// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TargetListView from './TargetListView.vue'

const mocks = vi.hoisted(() => ({
  listResumes: vi.fn(),
  getResumeVersion: vi.fn(),
  listJobDescriptions: vi.fn(),
  createJobDescription: vi.fn(),
  listScheduleEvents: vi.fn(),
  listInterviewPlans: vi.fn(),
  getSessionHistory: vi.fn(),
  routerPush: vi.fn(),
}))

interface TestTarget {
  id: number
  name: string
  status: 'active' | 'archived'
  stage?: string
  stageUpdatedAt?: string | null
  createdAt?: string
  jobDescriptionId: number | null
  resumeVersionId: number | null
}

const store = reactive({
  targets: [
    { id: 1, name: '腾讯 Java 后端开发', status: 'active', stage: 'interview', stageUpdatedAt: '2026-08-22T10:00:00Z', createdAt: '2026-08-01T00:00:00Z', jobDescriptionId: 6, resumeVersionId: 21 },
    { id: 2, name: '美团 测试开发', status: 'archived', stage: 'closed', stageUpdatedAt: null, createdAt: '2026-07-01T00:00:00Z', jobDescriptionId: null, resumeVersionId: null },
  ] as TestTarget[],
  activeTargetId: 1,
  loading: false,
  errorMessage: '',
  load: vi.fn(), retry: vi.fn(), select: vi.fn(), create: vi.fn(), updateLinks: vi.fn(), rename: vi.fn(), archive: vi.fn(), restore: vi.fn(), remove: vi.fn(), setStage: vi.fn(), saveApplication: vi.fn(),
})

vi.mock('../../stores/targets', () => ({ useTargetsStore: () => store }))
vi.mock('vue-router', async () => ({
  ...(await vi.importActual<typeof import('vue-router')>('vue-router')),
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: mocks.routerPush }),
}))
vi.mock('../../api/resume', () => ({
  listResumes: mocks.listResumes,
  getResumeVersion: mocks.getResumeVersion,
}))
vi.mock('../../api/job', () => ({
  listJobDescriptions: mocks.listJobDescriptions,
  createJobDescription: mocks.createJobDescription,
}))
vi.mock('../../api/schedule', () => ({ listScheduleEvents: mocks.listScheduleEvents }))
vi.mock('../../api/interview', () => ({
  listMyInterviewPlans: mocks.listInterviewPlans,
  getSessionHistory: mocks.getSessionHistory,
}))

const planFixture = {
  planId: 5, resumeVersionId: 21, jobDescriptionId: 6, title: '技术二面模拟', questionCount: 2,
  focusTags: ['技术基础'], supplement: null, completed: true,
  summary: { overallScore: 8.2 }, updatedAt: '2026-08-21T00:00:00',
  rounds: [{ sessionId: 90, personaId: 1, personaName: '张老师', personaTitle: '资深面试官', roundOrder: 1, status: 'COMPLETED', currentQuestionIndex: 1, totalQuestions: 2, completed: true }],
}

describe('TargetListView（求职计划卡片）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    store.targets = [
      { id: 1, name: '腾讯 Java 后端开发', status: 'active', stage: 'interview', stageUpdatedAt: '2026-08-22T10:00:00Z', createdAt: '2026-08-01T00:00:00Z', jobDescriptionId: 6, resumeVersionId: 21 },
      { id: 2, name: '美团 测试开发', status: 'archived', stage: 'closed', stageUpdatedAt: null, createdAt: '2026-07-01T00:00:00Z', jobDescriptionId: null, resumeVersionId: null },
    ]
    store.activeTargetId = 1
    mocks.listResumes.mockResolvedValue({ data: [{ id: 8, title: '后端简历', currentVersion: { id: 21, versionNo: 4 } }] })
    mocks.listJobDescriptions.mockResolvedValue({
      data: [{ id: 6, companyName: '腾讯', jobTitle: 'Java 后端开发', rawText: '岗位描述', parseStatus: 'succeeded', createdAt: '', updatedAt: '' }],
    })
    mocks.createJobDescription.mockResolvedValue({ data: { id: 99, companyName: '', jobTitle: '', rawText: '', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    mocks.getResumeVersion.mockResolvedValue({ data: { resumeId: 8, versionNo: 3 } })
    mocks.listScheduleEvents.mockResolvedValue({ data: [] })
    mocks.listInterviewPlans.mockResolvedValue({ data: [planFixture] })
    mocks.getSessionHistory.mockResolvedValue({
      data: { sessionId: 90, items: [
        { questionIndex: 0, questionText: '介绍一个你负责的项目', questionType: 'behavioral', answerText: '……', evaluation: null },
        { questionIndex: 1, questionText: 'MySQL 索引失效场景？', questionType: 'technical', answerText: '', evaluation: null },
      ] },
    })
    mocks.routerPush.mockResolvedValue(undefined)
  })

  it('renders a plan card per target with company mark, stage pipeline and resume badge', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    const card = wrapper.get('[data-test="plan-card-1"]')
    expect(card.text()).toContain('腾讯 · Java 后端开发')
    expect(card.text()).toContain('JD 已录入并解析')
    expect(card.text()).toContain('投递中')
    expect(card.find('[data-test="stage-pill-1"]').classes()).not.toContain('closed')

    // 简历修改版徽章
    const chip = card.get('[data-test="resume-chip"]')
    expect(chip.text()).toContain('腾讯修改版 · V4')

    // 近期面试记录 + 分数
    expect(card.get(`[data-test="interviews-1"]`).text()).toContain('技术二面模拟')
    expect(card.text()).toContain('8.2')
    expect(card.text()).toContain('创建于 8月1日')
  })

  it('advances the pipeline by clicking a stage node', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.get('[data-test="stage-1-hr"]').trigger('click')
    expect(store.setStage).toHaveBeenCalledWith(1, 'hr')
  })

  it('shows an unbound resume hint for targets without resume', async () => {
    store.targets = [
      { id: 9, name: '新计划', status: 'active', stage: 'applied', stageUpdatedAt: null, createdAt: '2026-08-20T00:00:00Z', jobDescriptionId: null, resumeVersionId: null },
    ]
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    const chip = wrapper.get('[data-test="plan-card-9"] [data-test="resume-chip"]')
    expect(chip.text()).toContain('绑定简历')
    await chip.trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith(expect.objectContaining({ name: 'resumes' }))
  })

  it('opens interview records dialog with clickable questions', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.get('.plan-row').trigger('click')
    await flushPromises()

    expect(mocks.getSessionHistory).toHaveBeenCalledWith(90)
    const dialog = wrapper.get('[role="dialog"]')
    expect(dialog.text()).toContain('技术二面模拟')
    expect(dialog.text()).toContain('MySQL 索引失效场景？')
  })

  it('creates a JD then the project when submitting the create dialog with company info', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()
    await wrapper.get('[data-test="create-target"]').trigger('click')

    await wrapper.get('[data-test="target-company"]').setValue('字节跳动')
    await wrapper.get('[data-test="target-job-title"]').setValue('前端工程师')
    await wrapper.get('[data-test="target-jd"]').setValue('负责抖音 Web 开发')
    await wrapper.get('[data-test="target-resume"]').setValue('21')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createJobDescription).toHaveBeenCalledWith(expect.objectContaining({
      companyName: '字节跳动',
      jobTitle: '前端工程师',
      rawText: '负责抖音 Web 开发',
    }))
    expect(store.create).toHaveBeenCalledWith({
      name: '字节跳动 · 前端工程师',
      jobDescriptionId: 99,
      resumeVersionId: 21,
    })
  })

  it('archives from the card menu and dims archived cards', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    expect(wrapper.get('[data-test="plan-card-2"]').classes()).toContain('archived')
    await wrapper.get('[data-test="plan-card-1"] .menu-trigger').trigger('click')
    const menuButtons = wrapper.findAll('.menu-popover button')
    await menuButtons.find((button) => button.text() === '归档计划')!.trigger('click')
    expect(store.archive).toHaveBeenCalledWith(1)
  })

  it('renames through the mini dialog', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.get('[data-test="plan-card-1"] .menu-trigger').trigger('click')
    const menuButtons = wrapper.findAll('.menu-popover button')
    await menuButtons.find((button) => button.text() === '重命名')!.trigger('click')
    await wrapper.get('[data-test="rename-input"]').setValue('腾讯后端专项')
    await wrapper.find('.mini-dialog form').trigger('submit')
    await flushPromises()
    expect(store.rename).toHaveBeenCalledWith(1, '腾讯后端专项')
  })

  it('filters cards by stage and role category through the toolbar', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    // 阶段筛选：只看 Offer
    await wrapper.get('[data-test="filter-stage-offer"]').trigger('click')
    expect(wrapper.find('[data-test="plan-card-1"]').exists()).toBe(false)

    // 回到全部后按岗位分类筛选
    await wrapper.get('[data-test="filter-stage-all"]').trigger('click')
    const rolePills = wrapper.findAll('.filter-pill').filter((pill) => pill.text().startsWith('后端'))
    if (rolePills.length) {
      await rolePills[0].trigger('click')
      expect(wrapper.find('[data-test="plan-card-2"]').exists()).toBe(false)
      expect(wrapper.find('[data-test="plan-card-1"]').exists()).toBe(true)
    }
  })

  it('edits application info from the dedicated applications zone table', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    // 卡片上不应再有投递按钮
    expect(wrapper.find('[data-test="apply-1"]').exists()).toBe(false)

    await wrapper.get('[data-test="view-applications"]').trigger('click')
    const row = wrapper.get('[data-test="apply-row-1"]')
    expect(row.text()).toContain('腾讯 · Java 后端开发')
    expect(row.text()).toContain('面试')

    await wrapper.get('[data-test="edit-apply-1"]').trigger('click')
    const dialog = wrapper.get('.dialog-backdrop [role="dialog"]')
    await dialog.get('[data-test="apply-industry"]').setValue('互联网')
    await dialog.get('[data-test="apply-role"]').setValue('Java 后端')
    await dialog.get('[data-test="apply-location"]').setValue('深圳')
    await dialog.get('[data-test="apply-notes"]').setValue('官网投递')
    await dialog.get('form').trigger('submit')
    await flushPromises()

    expect(store.saveApplication).toHaveBeenCalledWith(1, { industry: '互联网', role: 'Java 后端', location: '深圳', notes: '官网投递' })
  })

  it('allows adding a JD after creation and links it to the target', async () => {
    mocks.createJobDescription.mockResolvedValue({ data: { id: 77, companyName: '', jobTitle: '', rawText: '', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    store.targets = [
      { id: 9, name: '美团 测试开发', status: 'active', stage: 'applied', stageUpdatedAt: null, createdAt: '2026-08-20T00:00:00Z', jobDescriptionId: null, resumeVersionId: null },
    ]
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    // 无 JD 的目标显示「录入岗位 JD」入口
    await wrapper.get('[data-test="jd-add-9"]').trigger('click')
    const dialog = wrapper.get('.dialog-backdrop [role="dialog"]')
    await dialog.get('[data-test="jd-company"]').setValue('美团')
    await dialog.get('[data-test="jd-job-title"]').setValue('测试开发')
    await dialog.get('[data-test="jd-text"]').setValue('负责业务测试与工具开发')
    await dialog.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.createJobDescription).toHaveBeenCalledWith(expect.objectContaining({
      companyName: '美团',
      jobTitle: '测试开发',
      rawText: '负责业务测试与工具开发',
    }))
    expect(store.updateLinks).toHaveBeenCalledWith(9, { jobDescriptionId: 77, resumeVersionId: null })
  })

  it('filters cards by keyword search in the toolbar', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: { teleport: true } } })
    await flushPromises()

    await wrapper.get('[data-test="plan-search"]').setValue('美团')
    await flushPromises()
    expect(wrapper.find('[data-test="plan-card-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="plan-card-2"]').exists()).toBe(true)
  })
})
