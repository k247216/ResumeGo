// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PipelineView from './PipelineView.vue'

vi.mock('../../api/resume', () => ({ listResumes: vi.fn().mockResolvedValue({ success: true, data: [], message: null }), getResumeVersions: vi.fn().mockResolvedValue({ success: true, data: [], message: null }) }))
vi.mock('../../api/job', () => ({ listJobDescriptions: vi.fn().mockResolvedValue({ success: true, data: [], message: null }) }))
vi.mock('../../api/schedule', () => ({ listScheduleEvents: vi.fn().mockResolvedValue({ success: true, data: [], message: null }) }))
vi.mock('../../api/interview', () => ({ listMyInterviewPlans: vi.fn().mockResolvedValue({ success: true, data: [], message: null }) }))
vi.mock('../../stores/pipelines', () => ({
  usePipelinesStore: vi.fn(),
}))

const routeMocks = vi.hoisted(() => ({ useRoute: vi.fn(), useRouter: vi.fn() }))
vi.mock('vue-router', () => ({
  useRoute: routeMocks.useRoute,
  useRouter: routeMocks.useRouter,
}))



import { usePipelinesStore } from '../../stores/pipelines'

function p(id: number, lifecycle = 'ACTIVE') {
  return {
    id, name: '管线 ' + id, companyName: '公司', roleTitle: '岗位',
    jobDescriptionId: null, resumeVersionId: null, lifecycle, outcome: null,
    currentStageId: null, stages: [], scheduleEventIds: [], interviewPlanIds: [],
    archivedAt: null, createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function storeStub(overrides: Record<string, unknown> = {}) {
  const base = {
    pipelines: [p(1), p(2)],
    selectedPipelineId: 1,
    selectedPipeline: p(1),
    loading: false,
    errorMessage: '',
    transitionHistoryByPipelineId: {},
    historyLoadingPipelineId: null,
    historyErrorMessage: '',
    load: vi.fn().mockResolvedValue(undefined),
    retry: vi.fn().mockResolvedValue(undefined),
    select: vi.fn(),
    create: vi.fn().mockResolvedValue(undefined),
    update: vi.fn().mockResolvedValue(undefined),
    addStage: vi.fn().mockResolvedValue(undefined),
    renameStage: vi.fn().mockResolvedValue(undefined),
    reorderStages: vi.fn().mockResolvedValue(undefined),
    transitionStage: vi.fn().mockResolvedValue(undefined),
    archive: vi.fn().mockResolvedValue(undefined),
    restore: vi.fn().mockResolvedValue(undefined),
    linkScheduleEvent: vi.fn().mockResolvedValue(undefined),
    unlinkScheduleEvent: vi.fn().mockResolvedValue(undefined),
    linkInterviewPlan: vi.fn().mockResolvedValue(undefined),
    unlinkInterviewPlan: vi.fn().mockResolvedValue(undefined),
    loadTransitionHistory: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
  return base
}

function mountView(store: ReturnType<typeof storeStub>, query = {}) {
  routeMocks.useRoute.mockReturnValue({ query } as never)
  routeMocks.useRouter.mockReturnValue({ push: vi.fn(), replace: vi.fn() } as never)
  vi.mocked(usePipelinesStore).mockReturnValue(store as never)
  return mount(PipelineView, {
    global: {
      plugins: [createPinia()],
      stubs: {
        PageHeader: { template: '<header><slot name="actions" /></header>' },
        PipelineListRail: { template: '<div data-test="stub-rail"><slot /></div>' },
        PipelineIdentityPanel: { template: '<div data-test="stub-identity" />' },
        PipelineStageTrack: { template: '<div data-test="stub-track" />' },
        PipelineMaterialsPanel: { template: '<div data-test="stub-materials" />' },
        PipelineRelationsPanel: { template: '<div data-test="stub-relations" />' },
        PipelineCreateDialog: { template: '<div v-if="true" data-test="stub-create-dialog" />' },
        PipelineEditDialog: { template: '<div data-test="stub-edit-dialog" />' },
        PipelineStageManagerDialog: { template: '<div data-test="stub-stage-dialog" />' },
        PipelineTransitionDialog: { template: '<div data-test="stub-transition-dialog" />' },
        PipelineRelationDialog: { template: '<div data-test="stub-relation-dialog" />' },
        PipelineHistoryDrawer: { template: '<div data-test="stub-history-drawer" />' },
      },
      mocks: { $route: { query }, $router: { push: vi.fn(), replace: vi.fn() } },
    },
  })
}

describe('PipelineView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the rail and detail for loaded pipelines', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(store.load).toHaveBeenCalled()
    expect(wrapper.find('[data-test="stub-rail"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stub-identity"]').exists()).toBe(true)
  })

  it('shows empty state and create entry when there are no pipelines', async () => {
    const store = storeStub({ pipelines: [], selectedPipeline: null, selectedPipelineId: null })
    const wrapper = mountView(store)
    await flushPromises()
    expect(wrapper.text()).toContain('创建第一条求职管线')
    expect(wrapper.find('[data-test="pipeline-empty-create"]').exists()).toBe(true)
  })

  it('selects a valid pipeline from the query param after load', async () => {
    const store = storeStub()
    const wrapper = mountView(store, { pipelineId: '2' })
    await flushPromises()
    expect(store.select).toHaveBeenCalledWith(2)
    expect(wrapper.exists()).toBe(true)
  })

  it('opens create dialog and submits via store', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    await wrapper.get('[data-test="pipeline-create"]').trigger('click')
    expect(wrapper.find('[data-test="stub-create-dialog"]').exists()).toBe(true)
  })
})
