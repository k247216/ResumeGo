import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import WorkbenchView from './WorkbenchView.vue'

const mocks = vi.hoisted(() => ({
  listResumes: vi.fn(),
  getResumeVersion: vi.fn(),
  createJob: vi.fn(),
  getJob: vi.fn(),
  listInterviewPlans: vi.fn(),
  listScheduleEvents: vi.fn(),
  updateScheduleEvent: vi.fn(),
  routerPush: vi.fn(),
  routeQuery: {} as Record<string, string>,
}))

const targetStore = reactive({
  targets: [] as Array<Record<string, unknown>>,
  activeTarget: null as Record<string, unknown> | null,
  activeTargetId: null as number | null,
  loading: false,
  errorMessage: '',
  load: vi.fn(),
  retry: vi.fn(),
  select: vi.fn(),
  create: vi.fn(),
  updateLinks: vi.fn(),
})

vi.mock('../../api/resume', () => ({ listResumes: mocks.listResumes, getResumeVersion: mocks.getResumeVersion }))
vi.mock('../../api/job', () => ({ createJobDescription: mocks.createJob, getJobDescription: mocks.getJob }))
vi.mock('../../api/interview', () => ({ listMyInterviewPlans: mocks.listInterviewPlans }))
vi.mock('../../api/schedule', () => ({ listScheduleEvents: mocks.listScheduleEvents, updateScheduleEvent: mocks.updateScheduleEvent }))
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targetStore }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: mocks.routeQuery }),
  useRouter: () => ({ push: mocks.routerPush }),
}))

type TargetShape = Record<string, unknown>
const target = (overrides: TargetShape = {}): TargetShape => ({
  id: 3, name: '腾讯 Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 21,
  archivedAt: null, createdAt: '2026-08-01T00:00:00', updatedAt: '2026-08-18T00:00:00',
  ...overrides,
})
const job = (overrides: TargetShape = {}): TargetShape => ({
  id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习', rawText: '岗位描述内容足够长并用于组件测试。', parseStatus: 'pending', createdAt: '', updatedAt: '',
  ...overrides,
})
const version = (overrides: TargetShape = {}): TargetShape => ({
  id: 21, resumeId: 8, versionNo: 4, createdByType: 'user', createdAt: '2026-07-15T00:00:00', content: { projects: [] },
  ...overrides,
})

function tomorrowTime(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T14:30:00`
}

function primeJobs(jobsById: Record<number, TargetShape>) {
  mocks.getJob.mockImplementation((id: number) => Promise.resolve({ success: true, data: jobsById[id] }))
}

function primeVersions(versionsById: Record<number, TargetShape>) {
  mocks.getResumeVersion.mockImplementation((id: number) => Promise.resolve({ success: true, data: versionsById[id] }))
}

function mountWorkspace() {
  return mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
}

describe('WorkbenchView', () => {
  beforeEach(() => {
    targetStore.targets = []
    targetStore.activeTarget = null
    targetStore.loading = false
    targetStore.errorMessage = ''
    vi.resetAllMocks()
    mocks.routeQuery = {}
    targetStore.load.mockResolvedValue(undefined)
    mocks.listResumes.mockResolvedValue({ success: true, data: [] })
    mocks.listInterviewPlans.mockResolvedValue({ success: true, data: [] })
    mocks.listScheduleEvents.mockResolvedValue({ success: true, data: [] })
    targetStore.updateLinks.mockImplementation(async (id: number, payload: TargetShape) => {
      const item = targetStore.targets.find((entry) => entry.id === id)
      if (item) { item.resumeVersionId = payload.resumeVersionId; item.jobDescriptionId = payload.jobDescriptionId }
    })
  })

  it('shows first-run guidance when no local materials or targets exist', async () => {
    const wrapper = mountWorkspace()
    await flushPromises()
    expect(wrapper.get('[data-test="first-run-workspace"]').text()).toContain('建立本地资料')
  })

  it('shows the dashboard with the no-target detail when resumes exist without targets', async () => {
    mocks.listResumes.mockResolvedValue({ success: true, data: [{ id: 8, title: '通用中文简历', currentVersion: { id: 9, versionNo: 2 } }] })
    const wrapper = mountWorkspace()
    await flushPromises()
    expect(wrapper.find('[data-test="target-dashboard"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('还没有求职目标')
    expect(wrapper.get('[data-test="agenda-pane"]').text()).toContain('近期没有安排')
  })

  it('aggregates schedule events across all targets as grouped rail rows with the nearest selected', async () => {
    targetStore.targets = [target(), target({ id: 4, name: '字节跳动 后端实习', jobDescriptionId: 7, resumeVersionId: 30, updatedAt: '2026-08-15T00:00:00' })]
    primeJobs({ 6: job(), 7: job({ id: 7, companyName: '字节跳动', jobTitle: '后端实习' }) })
    const today = new Date()
    const pad = (value: number) => String(value).padStart(2, '0')
    const todayTime = (hour: number, minute: number) => `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T${pad(hour)}:${pad(minute)}:00`
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [
        { id: 11, title: '技术面', eventType: 'interview', startTime: todayTime(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
        { id: 12, title: '笔试', eventType: 'exam', startTime: todayTime(14, 30), endTime: null, notes: null, jobDescriptionId: 7, createdAt: '', updatedAt: '' },
        { id: 13, title: '行测', eventType: 'exam', startTime: tomorrowTime(), endTime: null, notes: null, jobDescriptionId: null, createdAt: '', updatedAt: '' },
        { id: 14, title: '已过期的旧安排', eventType: 'interview', startTime: '2020-01-01T10:00:00', endTime: null, notes: null, jobDescriptionId: null, createdAt: '', updatedAt: '' },
      ],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    const pane = wrapper.get('[data-test="agenda-pane"]')
    expect(pane.text()).toContain('3 场安排')
    expect(pane.text()).not.toContain('已过期的旧安排')
    const rows = wrapper.findAll('[data-test="agenda-row"]')
    expect(rows).toHaveLength(3)
    expect(rows[0].find('.row-title').text()).toBe('字节跳动 · 笔试')
    expect(rows[1].find('.row-title').text()).toBe('腾讯 · 技术面')
    expect(rows[2].find('.row-title').text()).toBe('行测')

    expect(wrapper.get('[data-test="detail-title"]').text()).toBe('字节跳动 · 笔试')
    // 关联目标行 = 目标公司 · 岗位，岗位不再重复出现在头部（去重契约）
    const linked = wrapper.get('[data-test="detail-linked-target"]')
    expect(linked.text()).toContain('字节跳动 · 后端实习')
    expect(linked.text()).toContain('目标详情')
    expect(wrapper.get('[data-test="detail-pane"]').text()).toContain('后端实习')
  })

  it('switches the detail identity, preparation state, and next action when another event is selected', async () => {
    targetStore.targets = [target(), target({ id: 4, name: '字节跳动 后端实习', jobDescriptionId: 7, resumeVersionId: 30, updatedAt: '2026-08-15T00:00:00' })]
    primeJobs({ 6: job(), 7: job({ id: 7, companyName: '字节跳动', jobTitle: '后端实习' }) })
    primeVersions({ 21: version(), 30: version({ id: 30, resumeId: 9, versionNo: 2 }) })
    mocks.listResumes.mockResolvedValue({ success: true, data: [{ id: 8, title: '腾讯版简历' }, { id: 9, title: '通用简历' }] })
    const today = new Date()
    const pad = (value: number) => String(value).padStart(2, '0')
    const todayTime = (hour: number, minute: number) => `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T${pad(hour)}:${pad(minute)}:00`
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [
        { id: 11, title: '技术面', eventType: 'interview', startTime: todayTime(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
        { id: 12, title: '笔试', eventType: 'exam', startTime: todayTime(14, 30), endTime: null, notes: null, jobDescriptionId: 7, createdAt: '', updatedAt: '' },
      ],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    await wrapper.findAll('[data-test="agenda-row"]')[1].trigger('click')
    await flushPromises()

    const linked = wrapper.get('[data-test="detail-linked-target"]')
    expect(linked.text()).toContain('腾讯 · Java 后端实习')
    expect(linked.text()).toContain('目标详情')
    expect(wrapper.get('[data-test="detail-pane"]').text()).toContain('Java 后端实习')
    expect(wrapper.get('[data-test="readiness-resume"]').text()).toContain('腾讯版简历 · V4')
    expect(wrapper.get('[data-test="readiness-resume"]').text()).toContain('7/15 更新')
    const next = wrapper.get('[data-test="detail-next"]')
    expect(next.text()).toContain('模拟面试')
    expect(next.text()).toContain('预计 20 分钟')
    expect(mocks.getResumeVersion).toHaveBeenCalledWith(21)
  })

  it('shows an unlinked event in the detail pane without fabricating preparation data', async () => {
    targetStore.targets = [target()]
    primeJobs({ 6: job() })
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [{ id: 13, title: '行测', eventType: 'exam', startTime: tomorrowTime(), endTime: null, notes: null, jobDescriptionId: null, createdAt: '', updatedAt: '' }],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    expect(wrapper.get('[data-test="detail-title"]').text()).toContain('行测')
    expect(wrapper.get('[data-test="detail-unlinked"]').text()).toContain('未关联求职目标')
    expect(wrapper.find('[data-test="readiness-resume"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="detail-next"]').exists()).toBe(false)
    expect(wrapper.get('[data-test="agenda-pane"]').text()).toContain('行测')
  })

  it('does not relink an explicitly associated event to another target through the legacy JD fallback', async () => {
    targetStore.targets = [target()]
    primeJobs({ 6: job() })
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [{
        id: 13,
        title: '已归档计划的技术面',
        eventType: 'interview',
        startTime: tomorrowTime(),
        endTime: null,
        notes: null,
        jobDescriptionId: 6,
        jobProjectId: 999,
        createdAt: '',
        updatedAt: '',
      }],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    expect(wrapper.get('[data-test="detail-unlinked"]').text()).toContain('未关联求职目标')
    expect(wrapper.find('[data-test="detail-linked-target"]').exists()).toBe(false)
  })

  it('links an unlinked event to a chosen target from the detail pane', async () => {
    targetStore.targets = [target()]
    primeJobs({ 6: job() })
    mocks.updateScheduleEvent.mockResolvedValue({ success: true, data: {} })
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [{ id: 13, title: '行测', eventType: 'exam', startTime: tomorrowTime(), endTime: null, notes: null, jobDescriptionId: null, createdAt: '', updatedAt: '' }],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    await wrapper.get('[data-test="link-target"]').trigger('click')
    const options = wrapper.findAll('[data-test="link-target-option"]')
    expect(options).toHaveLength(1)
    expect(options[0].text()).toContain('腾讯')
    await options[0].trigger('click')
    await flushPromises()

    expect(mocks.updateScheduleEvent).toHaveBeenCalledWith(13, expect.objectContaining({ jobDescriptionId: 6, title: '行测', eventType: 'exam' }))
    expect(mocks.listScheduleEvents).toHaveBeenCalled()
  })

  it('routes interview preparation with the selected target context', async () => {
    targetStore.targets = [target(), target({ id: 4, name: '字节跳动 后端实习', jobDescriptionId: 7, resumeVersionId: 30, updatedAt: '2026-08-15T00:00:00' })]
    primeJobs({ 6: job(), 7: job({ id: 7, companyName: '字节跳动', jobTitle: '后端实习' }) })
    primeVersions({ 30: version({ id: 30, resumeId: 9, versionNo: 2 }) })
    const today = new Date()
    const pad = (value: number) => String(value).padStart(2, '0')
    const todayTime = (hour: number, minute: number) => `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T${pad(hour)}:${pad(minute)}:00`
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [
        { id: 11, title: '技术面', eventType: 'interview', startTime: todayTime(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
        { id: 12, title: '笔试', eventType: 'exam', startTime: todayTime(14, 30), endTime: null, notes: null, jobDescriptionId: 7, createdAt: '', updatedAt: '' },
      ],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    await wrapper.findAll('[data-test="agenda-row"]')[1].trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="next-button"]').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith({
      name: 'interview',
      query: { from: 'target', targetId: '3', versionId: '21', jobId: '6' },
    })
  })

  it('falls back to the most recent active target when there are no arrangements', async () => {
    targetStore.targets = [target({ updatedAt: '2026-08-01T00:00:00' }), target({ id: 4, name: '字节跳动 后端实习', jobDescriptionId: 7, resumeVersionId: null, updatedAt: '2026-08-15T00:00:00' })]
    primeJobs({ 6: job(), 7: job({ id: 7, companyName: '字节跳动', jobTitle: '后端实习' }) })
    const wrapper = mountWorkspace()
    await flushPromises()

    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('当前没有选中的安排')
    const identity = wrapper.get('[data-test="detail-identity"]')
    expect(identity.text()).toContain('字节跳动')
    expect(wrapper.get('[data-test="readiness-resume"]').text()).toContain('未选择简历')
    expect(wrapper.get('[data-test="detail-next"]').text()).toContain('选择简历')
  })

  it('creates a linked target from the empty-state job entry', async () => {
    mocks.listResumes.mockResolvedValue({ success: true, data: [{ id: 8, title: '通用中文简历', currentVersion: { id: 9, versionNo: 2 } }] })
    targetStore.create.mockImplementation(async (payload: TargetShape) => {
      const created = { id: 5, name: payload.name, status: 'active', jobDescriptionId: payload.jobDescriptionId, resumeVersionId: payload.resumeVersionId ?? null, archivedAt: null, createdAt: '', updatedAt: '' }
      targetStore.targets.push(created)
      return created
    })
    mocks.createJob.mockResolvedValue({ success: true, data: { id: 77, companyName: '美团', jobTitle: '前端实习', rawText: '岗位描述内容足够长并用于组件测试。', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    const wrapper = mountWorkspace()
    await flushPromises()

    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('还没有求职目标')
    await wrapper.get('[data-test="detail-empty-action"]').trigger('click')
    await wrapper.get('[data-test="job-title"]').setValue('前端实习')
    await wrapper.get('[data-test="job-company"]').setValue('美团')
    await wrapper.get('[data-test="job-raw-text"]').setValue('这是一段长度超过二十个字符且需要保存为岗位描述的真实内容。')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.createJob).toHaveBeenCalledWith({ jobTitle: '前端实习', companyName: '美团', rawText: expect.stringContaining('岗位描述') })
    expect(targetStore.create).toHaveBeenCalledWith({ name: '美团 · 前端实习', jobDescriptionId: 77 })
    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('刚录入的求职目标')
    expect(wrapper.get('[data-test="detail-identity"]').text()).toContain('美团')
    expect(wrapper.get('[data-test="detail-next"]').text()).toContain('选择简历')
  })

  it('links the selected resume version to the detail target', async () => {
    targetStore.targets = [target({ resumeVersionId: null })]
    primeJobs({ 6: job() })
    mocks.listResumes.mockResolvedValue({ success: true, data: [{ id: 8, title: '后端开发实习简历', currentVersion: { id: 21, versionNo: 4 } }] })
    primeVersions({ 21: version() })
    mocks.listScheduleEvents.mockResolvedValue({
      success: true,
      data: [{ id: 11, title: '技术面', eventType: 'interview', startTime: tomorrowTime(), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' }],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    expect(wrapper.get('[data-test="detail-next"]').text()).toContain('选择简历')
    await wrapper.get('[data-test="next-button"]').trigger('click')
    await wrapper.get('[data-test="resume-version"]').setValue('21')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(targetStore.updateLinks).toHaveBeenCalledWith(3, { jobDescriptionId: 6, resumeVersionId: 21 })
    expect(wrapper.get('[data-test="readiness-resume"]').text()).toContain('后端开发实习简历 · V4')
  })

  it('shows only the latest recent activity entry with real summary content', async () => {
    targetStore.targets = [target(), target({ id: 4, name: '字节跳动 后端实习', jobDescriptionId: 7, resumeVersionId: 30, updatedAt: '2026-08-15T00:00:00' })]
    primeJobs({ 6: job(), 7: job({ id: 7, companyName: '字节跳动', jobTitle: '后端实习' }) })
    mocks.listInterviewPlans.mockResolvedValue({
      success: true,
      data: [
        { planId: 9, resumeVersionId: 21, jobDescriptionId: 6, title: '技术面', questionCount: 5, focusTags: [], rounds: [], completed: true, updatedAt: '2026-08-18T14:00:00Z', summary: { overallSummary: '技术思路完整，但关键判断缺少量化依据。', overallScore: 0, crossStrengths: [], crossWeaknesses: [], suggestions: ['补充并发项目的量化结果。'], sessions: [] } },
        { planId: 8, resumeVersionId: 30, jobDescriptionId: 7, title: '笔试', questionCount: 4, focusTags: [], rounds: [], completed: true, updatedAt: '2026-08-19T09:00:00Z', summary: { overallSummary: '行测基础扎实，算法题需要提速。', overallScore: 0, crossStrengths: [], crossWeaknesses: [], suggestions: ['限时刷题。'], sessions: [] } },
      ],
    })
    const wrapper = mountWorkspace()
    await flushPromises()

    const activity = wrapper.get('[data-test="recent-activity"]')
    expect(activity.text()).toContain('字节跳动 · 笔试')
    expect(activity.text()).toContain('行测基础扎实')
    expect(activity.text()).not.toContain('腾讯 · 技术面')
    await wrapper.get('[data-test="activity-row"] .activity-report').trigger('click')
    expect(mocks.routerPush).toHaveBeenCalledWith({
      name: 'interview',
      query: { from: 'target', targetId: '4', versionId: '30', jobId: '7' },
    })
  })

  it('shows a retryable error instead of pretending the workspace is empty', async () => {
    mocks.listResumes.mockRejectedValue(new Error('本地服务暂时不可用'))
    const wrapper = mountWorkspace()
    await flushPromises()
    expect(wrapper.get('[data-test="workspace-error"]').text()).toContain('重新加载')
    expect(wrapper.find('[data-test="first-run-workspace"]').exists()).toBe(false)
  })
})
