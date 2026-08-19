import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import WorkbenchView from './WorkbenchView.vue'

const mocks = vi.hoisted(() => ({
  listResumes: vi.fn(),
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
})

vi.mock('../../api/resume', () => ({ listResumes: mocks.listResumes }))
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targetStore }))

describe('WorkbenchView', () => {
  beforeEach(() => {
    targetStore.targets = []
    targetStore.activeTarget = null
    targetStore.loading = false
    targetStore.errorMessage = ''
    vi.clearAllMocks()
    targetStore.load.mockResolvedValue(undefined)
    mocks.listResumes.mockResolvedValue({ success: true, data: [] })
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
    expect(dashboard.text()).toContain('添加目标岗位')
  })

  it('shows a retryable error instead of pretending the workspace is empty', async () => {
    mocks.listResumes.mockRejectedValue(new Error('本地服务暂时不可用'))
    const wrapper = mount(WorkbenchView, { global: { stubs: ['RouterLink'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="workspace-error"]').text()).toContain('重新加载')
    expect(wrapper.find('[data-test="first-run-workspace"]').exists()).toBe(false)
  })
})
