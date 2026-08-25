// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  createInterviewPlan: vi.fn(),
  listMyInterviewPlans: vi.fn(),
  getInterviewPlan: vi.fn(),
  deleteInterviewPlan: vi.fn(),
  generateInterviewPlanSummary: vi.fn(),
  listMyInterviews: vi.fn(),
  startInterview: vi.fn(),
  getInterviewStatus: vi.fn(),
  submitInterviewAnswer: vi.fn(),
  listInterviewerPersonas: vi.fn(),
  createInterviewerPersona: vi.fn(),
  deleteInterviewerPersona: vi.fn(),
  getInterviewGrowthReport: vi.fn(),
  generateMultiSessionSummary: vi.fn(),
  getSessionHistory: vi.fn(),
  listInterviewQuestionSets: vi.fn(),
}))
const jobApi = vi.hoisted(() => ({ listJobDescriptions: vi.fn(), resolveCompanyProfile: vi.fn() }))
const targets = vi.hoisted(() => ({
  targets: [] as Array<Record<string, unknown>>,
  loading: false,
  errorMessage: '',
  load: vi.fn(),
}))

vi.mock('../api/interview', () => api)
vi.mock('../api/job', () => jobApi)
vi.mock('../api/resume', () => ({ listResumes: vi.fn().mockResolvedValue({ success: true, data: [] }) }))
vi.mock('../stores/targets', () => ({ useTargetsStore: () => targets }))
vi.mock('element-plus', async (importOriginal) => {
  const original = await importOriginal<typeof import('element-plus')>()
  return { ...original, ElMessage: { success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }
})
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
  RouterLink: { name: 'RouterLink', props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../composables/useInterviewSessions', async (importOriginal) => {
  const original = await importOriginal<typeof import('../composables/useInterviewSessions')>()
  const { ref, computed } = await import('vue')
  return {
    ...original,
    useInterviewSessions: () => {
      const sessions = ref([])
      const activeSessionId = ref<number | null>(null)
      const activeSession = computed(() => null)
      const activeState = ref({})
      const sessionStates = ref({})
      return {
        sessions,
        activeSessionId,
        sessionStates,
        activeSession,
        activeState,
        getOrCreateSessionState: vi.fn().mockReturnValue({}),
        updateSession: vi.fn(),
        upsertSession: vi.fn(),
        removeSessionState: vi.fn(),
        loadSessions: vi.fn().mockResolvedValue([]),
      }
    },
  }
})
vi.mock('../utils/interviewContext', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../utils/interviewContext')>()),
  resolveTargetInterviewContext: vi.fn().mockResolvedValue(null),
}))
vi.mock('../utils/interviewRecords', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../utils/interviewRecords')>()),
  loadInterviewRecords: vi.fn().mockResolvedValue([]),
}))
vi.mock('../utils/interviewReview', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../utils/interviewReview')>()),
  loadInterviewReviewState: vi.fn().mockResolvedValue(null),
}))
vi.mock('../components/CompanyProfileSignal.vue', () => ({
  default: { name: 'CompanyProfileSignal', template: '<div />' },
}))
vi.mock('../components/interview/InterviewHistoryPanel.vue', () => ({
  default: { name: 'InterviewHistoryPanel', template: '<div data-test="history-stub" />' },
}))
vi.mock('../components/interview/InterviewPlanReviewDialog.vue', () => ({
  default: { name: 'InterviewPlanReviewDialog', template: '<div />' },
}))
vi.mock('../components/interview/InterviewRoomSidebar.vue', () => ({
  default: { name: 'InterviewRoomSidebar', template: '<div />' },
}))
vi.mock('../components/interview/GrowthTrendDialog.vue', () => ({
  default: { name: 'GrowthTrendDialog', template: '<div />' },
}))
vi.mock('../components/interview/InterviewChatThread.vue', () => ({
  default: { name: 'InterviewChatThread', template: '<div />' },
}))
vi.mock('../components/interview/InterviewResultWorkspace.vue', () => ({
  default: {
    name: 'InterviewResultWorkspace',
    props: ['plan'],
    template: '<div data-test="result-stub">{{ plan?.planId }}</div>',
  },
}))

import InterviewView from './InterviewView.vue'

function mountView() {
  return mount(InterviewView, { global: { stubs: ['RouterLink'] } })
}

describe('InterviewView 三模式入口', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
    api.listMyInterviewPlans.mockResolvedValue({ success: true, data: [] })
    api.listMyInterviews.mockResolvedValue({ success: true, data: [] })
    api.listInterviewerPersonas.mockResolvedValue({ success: true, data: [] })
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [] })
    jobApi.listJobDescriptions.mockResolvedValue({ success: true, data: [] })
  })

  it('大厅常驻三模式英雄区，三个模式卡等权呈现', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-test="three-mode-entry"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="interview-command-bar"]').text()).toContain('模拟面试')
    expect(wrapper.findAll('[data-test^="mode-"]')).toHaveLength(3)
    const text = wrapper.text()
    expect(text).not.toContain('规划中')
    expect(text).not.toContain('敬请期待')
  })

  it('选择模式卡片后进入对应配置面板', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').trigger('click')
    expect(wrapper.findAll('[data-test="knowledge-training-setup"]')).toHaveLength(1)

    await wrapper.get('[data-test="mode-EXPERIENCE_SIMULATION"]').trigger('click')
    expect(wrapper.findAll('[data-test="experience-simulation-setup"]')).toHaveLength(1)
  })
})
