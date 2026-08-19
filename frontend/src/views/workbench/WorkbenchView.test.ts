import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import WorkbenchView from './WorkbenchView.vue'

const mocks = vi.hoisted(() => ({
  listResumes: vi.fn(),
  getResumeVersion: vi.fn(),
  createJob: vi.fn(),
  getJob: vi.fn(),
  routerPush: vi.fn(),
  routeQuery: {} as Record<string, string>,
}))

const targetStore = reactive({
  targets: [] as Array<Record<string, unknown>>,
  activeTarget: null as Record<string, unknown> | null,
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
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targetStore }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: mocks.routeQuery }),
  useRouter: () => ({ push: mocks.routerPush }),
}))

describe('WorkbenchView', () => {
  beforeEach(() => {
    targetStore.targets = []
    targetStore.activeTarget = null
    targetStore.loading = false
    targetStore.errorMessage = ''
    vi.clearAllMocks()
    mocks.routeQuery = {}
    targetStore.load.mockResolvedValue(undefined)
    mocks.listResumes.mockResolvedValue({ success: true, data: [] })
    targetStore.updateLinks.mockResolvedValue(undefined)
  })

  it('shows first-run guidance when no local materials or targets exist', async () => {
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="first-run-workspace"]').text()).toContain('建立本地资料')
  })

  it('shows the local library before a target is created', async () => {
    mocks.listResumes.mockResolvedValue({
      success: true,
      data: [{ id: 8, title: '通用中文简历', currentVersion: { id: 9, versionNo: 2 } }],
    })
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="local-library"]').text()).toContain('针对岗位开始优化')
  })

  it('shows the active target dashboard and deterministic next action', async () => {
    const target = { id: 3, name: '腾讯 · Java 后端实习', status: 'active', jobDescriptionId: null, resumeVersionId: null }
    targetStore.targets = [target]
    targetStore.activeTarget = target
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    const dashboard = wrapper.get('[data-test="target-dashboard"]')
    expect(dashboard.text()).toContain('腾讯 · Java 后端实习')
    expect(dashboard.text()).toContain('录入目标岗位')
    await dashboard.get('[data-test="target-next-action"]').trigger('click')
    expect(wrapper.get('[data-test="job-title"]')).toBeTruthy()
  })

  it('opens resume selection after target job information exists', async () => {
    const target = { id: 3, name: '目标', status: 'active', jobDescriptionId: 6, resumeVersionId: null }
    targetStore.targets = [target]
    targetStore.activeTarget = target
    mocks.getJob.mockResolvedValue({ success: true, data: { id: 6, jobTitle: '后端实习', rawText: '岗位描述内容足够长并用于组件测试。', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    await wrapper.get('[data-test="target-next-action"]').trigger('click')
    expect(wrapper.text()).toContain('选择这个目标采用的简历版本')
  })

  it('shows evidence count from the exact resume version selected by the target', async () => {
    const target = { id: 3, name: '完整目标', status: 'active', jobDescriptionId: 6, resumeVersionId: 21 }
    targetStore.targets = [target]
    targetStore.activeTarget = target
    mocks.getJob.mockResolvedValue({ success: true, data: { id: 6, jobTitle: '后端实习', rawText: '岗位描述内容足够长并用于组件测试。', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    mocks.getResumeVersion.mockResolvedValue({ success: true, data: { id: 21, resumeId: 8, versionNo: 4, createdByType: 'user', createdAt: '', content: { projects: [{ title: '一', evidenceId: 2 }, { title: '二' }, { title: '三', evidenceId: 5 }] } } })
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="target-dashboard"]').text()).toContain('2 条已引用')
  })

  it('opens interview preparation with the active target context', async () => {
    const target = { id: 3, name: '完整目标', status: 'active', jobDescriptionId: 6, resumeVersionId: 21 }
    targetStore.targets = [target]
    targetStore.activeTarget = target
    mocks.getJob.mockResolvedValue({ success: true, data: { id: 6, jobTitle: '后端实习', rawText: '岗位描述内容足够长并用于组件测试。', parseStatus: 'pending', createdAt: '', updatedAt: '' } })
    mocks.getResumeVersion.mockResolvedValue({ success: true, data: { id: 21, resumeId: 8, versionNo: 4, createdByType: 'user', createdAt: '', content: { projects: [] } } })
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()

    await wrapper.get('[data-test="prepare-interview"]').trigger('click')

    expect(mocks.routerPush).toHaveBeenCalledWith({
      name: 'interview',
      query: { from: 'target', targetId: '3', versionId: '21', jobId: '6' },
    })
  })

  it('keeps target context and form content visible when saving a job fails', async () => {
    const target = { id: 3, name: '稳定保留的目标', status: 'active', jobDescriptionId: null, resumeVersionId: null }
    targetStore.targets = [target]
    targetStore.activeTarget = target
    mocks.createJob.mockRejectedValue(new Error('岗位保存失败'))
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    await wrapper.get('[data-test="target-next-action"]').trigger('click')
    await wrapper.get('[data-test="job-title"]').setValue('后端实习')
    await wrapper.get('[data-test="job-raw-text"]').setValue('这是一段长度超过二十个字符且需要在失败后保留的岗位描述内容。')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-test="target-dashboard"]').text()).toContain('稳定保留的目标')
    expect(wrapper.text()).toContain('岗位保存失败')
    expect((wrapper.get('[data-test="job-raw-text"]').element as HTMLTextAreaElement).value).toContain('失败后保留')
  })

  it('shows a retryable error instead of pretending the workspace is empty', async () => {
    mocks.listResumes.mockRejectedValue(new Error('本地服务暂时不可用'))
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="workspace-error"]').text()).toContain('重新加载')
    expect(wrapper.find('[data-test="first-run-workspace"]').exists()).toBe(false)
  })
})
