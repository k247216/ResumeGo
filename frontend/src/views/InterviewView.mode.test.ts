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
const aiApi = vi.hoisted(() => ({ listAiProviders: vi.fn() }))
const targets = vi.hoisted(() => ({
  targets: [] as Array<Record<string, unknown>>,
  loading: false,
  errorMessage: '',
  load: vi.fn(),
}))
const sessionFixture = vi.hoisted(() => ({ value: [] as Array<Record<string, unknown>> }))

vi.mock('../api/interview', () => api)
vi.mock('../api/job', () => jobApi)
vi.mock('../api/aiProviders', () => aiApi)
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
      const sessions = ref(sessionFixture.value)
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
import interviewViewSource from './InterviewView.vue?raw'

function mountView() {
  return mount(InterviewView, { global: { stubs: ['RouterLink'] } })
}

describe('InterviewView 三模式入口', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    sessionFixture.value = []
    targets.load.mockResolvedValue(undefined)
    api.listMyInterviewPlans.mockResolvedValue({ success: true, data: [] })
    api.listMyInterviews.mockResolvedValue({ success: true, data: [] })
    api.listInterviewerPersonas.mockResolvedValue({ success: true, data: [] })
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [] })
    jobApi.listJobDescriptions.mockResolvedValue({ success: true, data: [] })
    aiApi.listAiProviders.mockResolvedValue([])
  })

  it('首次进入是面试主页，点击练习入口直达对应配置页', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-test="interview-home"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="interview-command-bar"]').text()).toContain('面试主页')
    expect(wrapper.find('[data-test="practice-ROLE_BASED"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test^="mode-"]')).toHaveLength(0)
    const text = wrapper.text()
    expect(text).not.toContain('规划中')
    expect(text).not.toContain('敬请期待')

    await wrapper.get('[data-test="practice-ROLE_BASED"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="interview-setup"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="interview-setup"]').attributes('data-setup-mode')).toBe('ROLE_BASED')
    expect(wrapper.find('[data-test="role-based-setup"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test^="mode-"]')).toHaveLength(0)
    expect(wrapper.get('[data-test="interview-command-bar"]').text()).not.toContain('面试引擎')
  })

  it('每个主页入口只挂载自己的配置面板', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="practice-KNOWLEDGE_TRAINING"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-test="knowledge-training-setup"]')).toHaveLength(1)
    expect(wrapper.find('[data-test="role-based-setup"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="experience-simulation-setup"]').exists()).toBe(false)

    const next = mountView()
    await flushPromises()
    await next.get('[data-test="practice-EXPERIENCE_SIMULATION"]').trigger('click')
    await flushPromises()
    expect(next.findAll('[data-test="experience-simulation-setup"]')).toHaveLength(1)
    expect(next.find('[data-test="knowledge-training-setup"]').exists()).toBe(false)
    expect(next.find('[data-test="role-based-setup"]').exists()).toBe(false)
    expect(next.find('[data-test="experience-persona-select"]').exists()).toBe(false)
  })

  it('主页默认展示最近五条记录，展开后可滚动查看全部记录', async () => {
    sessionFixture.value = Array.from({ length: 6 }, (_, index) => ({
      sessionId: index + 1,
      status: 'COMPLETED',
      completed: true,
      personaName: `面试官 ${index + 1}`,
    }))
    api.listMyInterviews.mockResolvedValue({ success: true, data: sessionFixture.value })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.findAll('.engine-timeline-row')).toHaveLength(5)
    const viewAll = wrapper.get('.engine-view-all')
    expect(viewAll.text()).toContain('查看全部记录')
    await viewAll.trigger('click')
    expect(wrapper.findAll('.engine-timeline-row')).toHaveLength(6)
    expect(wrapper.get('.engine-timeline-list').attributes('data-expanded')).toBe('true')
  })

  it('加载面试工作区时读取当前 API 模型配置', async () => {
    aiApi.listAiProviders.mockResolvedValue([{
      id: 1,
      displayName: 'OpenAI',
      protocolType: 'openai-compatible',
      baseUrl: 'https://api.openai.com/v1',
      defaultModel: 'gpt-4.1-mini',
      defaultProfile: true,
      apiKeyConfigured: true,
      lastTestedAt: null,
      lastTestStatus: null,
      lastTestMessage: null,
    }])

    mountView()
    await flushPromises()

    expect(aiApi.listAiProviders).toHaveBeenCalledTimes(1)
  })

  it('面试房间按目标图保留窄进度栏，并让上下文分隔线贯穿顶栏', () => {
    expect(interviewViewSource).toContain('grid-template-columns: 172px minmax(0, 1fr) 248px')
    expect(interviewViewSource).toContain('.interview-chat-layout::after')
  })

  it('面试房间使用当前配置模型，并在回到窗口时刷新模型状态', () => {
    expect(interviewViewSource).toContain("if (!activeReviewMode.value && configuredNow) return configuredNow")
    expect(interviewViewSource).toContain("window.addEventListener('focus', refreshAiModelProfile)")
    expect(interviewViewSource).toContain("document.addEventListener('visibilitychange', handleAiModelVisibilityChange)")
  })

})
