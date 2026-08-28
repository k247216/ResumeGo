import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InterviewRoomContextPanel from './InterviewRoomContextPanel.vue'

const session = {
  sessionId: 1,
  status: 'WAITING_ANSWER',
  currentQuestionIndex: 2,
  totalQuestions: 10,
  currentQuestion: null,
  completed: false,
  perQuestionScores: [],
  personaName: '高级 Java 工程师',
  personaTitle: '后端面试官',
} as any

describe('InterviewRoomContextPanel', () => {
  it('renders role-based snapshot without borrowing knowledge fields', () => {
    const wrapper = mount(InterviewRoomContextPanel, {
      props: {
        mode: 'ROLE_BASED',
        plan: {
          jobLabel: '腾讯 · Java 后端',
          resumeLabel: '后端开发简历 · V3',
          questionCount: 10,
          focusTags: ['项目还原'],
          startContextSnapshot: { jobProjectName: '腾讯 Java 后端', resumeTitle: '后端开发简历', resumeVersionNo: 3 },
        },
        session,
        activePersona: { name: '高级 Java 工程师', title: '后端面试官' },
      },
      global: { stubs: { ElIcon: { template: '<span><slot /></span>' } } },
    })

    expect(wrapper.text()).toContain('腾讯 Java 后端')
    expect(wrapper.text()).toContain('后端开发简历 · V3')
    expect(wrapper.text()).not.toContain('Knowledge Base')
  })

  it('renders knowledge documents and strategy from the locked snapshot', () => {
    const wrapper = mount(InterviewRoomContextPanel, {
      props: {
        mode: 'KNOWLEDGE_TRAINING',
        plan: {
          jobLabel: '知识训练',
          questionCount: 8,
          focusTags: [],
          startContextSnapshot: {
            knowledgeDocumentTitles: ['Redis 笔记', 'JVM 调优'],
            difficulty: '深入',
          },
        },
        session,
        activePersona: null,
      },
      global: { stubs: { ElIcon: { template: '<span><slot /></span>' } } },
    })

    expect(wrapper.text()).toContain('Redis 笔记、JVM 调优')
    expect(wrapper.text()).toContain('深入')
    expect(wrapper.text()).toContain('Knowledge Base')
  })
})
