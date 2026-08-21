// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TargetListView from './TargetListView.vue'

const mocks = vi.hoisted(() => ({
  listResumes: vi.fn(),
  getResumeVersion: vi.fn(),
  listJobDescriptions: vi.fn(),
  listScheduleEvents: vi.fn(),
  listInterviewPlans: vi.fn(),
  routerPush: vi.fn(),
}))

interface TestTarget {
  id: number
  name: string
  status: 'active' | 'archived'
  jobDescriptionId: number | null
  resumeVersionId: number | null
  updatedAt: string
}

const store = reactive({
  targets: [
    { id: 1, name: '原目标', status: 'active', jobDescriptionId: null, resumeVersionId: null, updatedAt: '2026-08-01T00:00:00Z' },
    { id: 2, name: '旧目标', status: 'archived', jobDescriptionId: null, resumeVersionId: null, updatedAt: '2026-07-01T00:00:00Z' },
  ] as TestTarget[],
  activeTargetId: 1,
  loading: false,
  errorMessage: '',
  load: vi.fn(), retry: vi.fn(), select: vi.fn(), rename: vi.fn(), archive: vi.fn(), restore: vi.fn(),
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
vi.mock('../../api/job', () => ({ listJobDescriptions: mocks.listJobDescriptions }))
vi.mock('../../api/schedule', () => ({ listScheduleEvents: mocks.listScheduleEvents }))
vi.mock('../../api/interview', () => ({ listMyInterviewPlans: mocks.listInterviewPlans }))

function tomorrowAt(hour: number, minute: number): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(hour)}:${pad(minute)}:00`
}

describe('TargetListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.listResumes.mockResolvedValue({ data: [] })
    mocks.listJobDescriptions.mockResolvedValue({ data: [] })
    mocks.listScheduleEvents.mockResolvedValue({ data: [] })
    mocks.listInterviewPlans.mockResolvedValue({ data: [] })
    mocks.routerPush.mockResolvedValue(undefined)
  })

  it('preselects the active target and renames it inline in the detail pane', async () => {
    const wrapper = mount(TargetListView)
    await flushPromises()
    await wrapper.get('[data-test="rename-target-1"]').trigger('click')
    await wrapper.get('[data-test="target-name-1"]').setValue('新的目标')
    await wrapper.get('[data-test="save-target-name-1"]').trigger('click')
    await flushPromises()
    expect(store.rename).toHaveBeenCalledWith(1, '新的目标')
  })

  it('shows the active-target identity meta under the title', async () => {
    const wrapper = mount(TargetListView)
    await flushPromises()

    // 岗位为空 → 不渲染岗位行；状态行来自真实数据（status + activeTargetId）
    expect(wrapper.find('.identity-role-line').exists()).toBe(false)
    expect(wrapper.get('.identity-meta-line').text()).toBe('进行中 · 当前目标')
    expect(wrapper.get('.identity-updated').text()).toContain('最近更新')
  })

  it('offers archive for the active target and restore after selecting the archived one', async () => {
    const wrapper = mount(TargetListView)
    await flushPromises()
    await wrapper.get('[data-test="archive-target-1"]').trigger('click')
    await wrapper.get('[data-test="target-row-2"]').trigger('click')
    await wrapper.get('[data-test="restore-target-2"]').trigger('click')
    expect(store.archive).toHaveBeenCalledWith(1)
    expect(store.restore).toHaveBeenCalledWith(2)
  })

  it('renders the job-project workspace from real data', async () => {
    store.targets = [
      { id: 1, name: '腾讯 Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 21, updatedAt: '2026-08-18T00:00:00Z' },
      { id: 2, name: '旧目标', status: 'archived', jobDescriptionId: null, resumeVersionId: null, updatedAt: '2026-07-01T00:00:00Z' },
    ]
    store.activeTargetId = 1
    mocks.listResumes.mockResolvedValue({ data: [{ id: 8, title: '算法工程师简历', currentVersion: { id: 21, versionNo: 4 } }] })
    mocks.listJobDescriptions.mockResolvedValue({
      data: [{ id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习', rawText: '岗位描述', parseStatus: 'succeeded', createdAt: '', updatedAt: '' }],
    })
    mocks.listScheduleEvents.mockResolvedValue({
      data: [
        { id: 11, title: '技术二面', eventType: 'interview', startTime: tomorrowAt(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
        { id: 12, title: '历史笔试', eventType: 'exam', startTime: '2026-01-01T10:00:00', endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
      ],
    })
    mocks.listInterviewPlans.mockResolvedValue({
      data: [{
        planId: 5, resumeVersionId: 21, jobDescriptionId: 6, title: '技术二面模拟', questionCount: 5, focusTags: ['技术基础'],
        supplement: null, rounds: [], completed: true, summary: { overallScore: 8 }, updatedAt: '2026-08-19T00:00:00',
      }],
    })

    const wrapper = mount(TargetListView)
    await flushPromises()

    // 左列行：公司 · 岗位 + 场次 · 简历版本 · 下一场摘要
    const row = wrapper.get('[data-test="target-row-1"]')
    expect(row.text()).toContain('腾讯 · Java 后端实习')
    expect(row.text()).toContain('1 场安排')
    expect(row.text()).toContain('简历 V4')
    expect(row.text()).toContain('明天面试')

    // identity
    expect(wrapper.get('.identity-role-line').text()).toBe('腾讯 · Java 后端实习')

    // 准备进度三列
    expect(wrapper.get('[data-test="progress-resume"]').text()).toContain('V4 已就绪')
    expect(wrapper.get('[data-test="progress-resume"]').text()).toContain('打开简历')
    expect(wrapper.get('[data-test="progress-interview"]').text()).toContain('1 次完成')
    expect(wrapper.get('[data-test="progress-interview"]').text()).toContain('查看复盘')
    expect(wrapper.get('[data-test="progress-schedule"]').text()).toContain('1 场安排')

    // 下一场：时间 + 标题 + 反馈驱动的建议
    const nextEvent = wrapper.get('[data-test="next-event"]')
    expect(nextEvent.text()).toContain('明天 16:00')
    expect(nextEvent.text()).toContain('技术二面')
    expect(nextEvent.text()).toContain('复看最近一次模拟反馈')
    expect(nextEvent.text()).toContain('开始准备')

    // 岗位信息
    const jobInfo = wrapper.get('[data-test="job-info"]')
    expect(jobInfo.text()).toContain('腾讯 · Java 后端实习')
    expect(jobInfo.text()).toContain('JD 已录入并解析')
    expect(jobInfo.text()).toContain('查看岗位')
  })

  it('suggests a first mock interview when no feedback exists yet', async () => {
    store.targets = [
      { id: 1, name: '腾讯 Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 21, updatedAt: '2026-08-18T00:00:00Z' },
    ]
    store.activeTargetId = 1
    mocks.listResumes.mockResolvedValue({ data: [{ id: 8, title: '算法工程师简历', currentVersion: { id: 21, versionNo: 4 } }] })
    mocks.listJobDescriptions.mockResolvedValue({
      data: [{ id: 6, companyName: '腾讯', jobTitle: 'Java 后端实习', rawText: '岗位描述', parseStatus: 'pending', createdAt: '', updatedAt: '' }],
    })
    mocks.listScheduleEvents.mockResolvedValue({
      data: [
        { id: 11, title: '技术二面', eventType: 'interview', startTime: tomorrowAt(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
      ],
    })

    const wrapper = mount(TargetListView)
    await flushPromises()

    expect(wrapper.get('[data-test="next-event"]').text()).toContain('完成一次针对当前目标的模拟面试')
    expect(wrapper.get('[data-test="job-info"]').text()).toContain('JD 已录入，待解析')
  })
})
