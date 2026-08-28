import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { InterviewStatusResponse, InterviewerPersona } from '../../types/interview'
import InterviewRoomSidebar from './InterviewRoomSidebar.vue'

function round(sessionId: number, status: string, personaName: string): InterviewStatusResponse {
  return {
    sessionId,
    status,
    currentQuestionIndex: status === 'COMPLETED' ? 5 : 2,
    totalQuestions: 5,
    currentQuestion: null,
    completed: status === 'COMPLETED',
    perQuestionScores: [],
    personaName,
    personaTitle: '模拟面试官',
  }
}

const persona: InterviewerPersona = {
  id: 7,
  name: '技术负责人',
  title: '研发总监',
  style: '关注技术细节',
  avatar: 'architect',
  type: 'preset',
}

describe('InterviewRoomSidebar', () => {
  it('renders question and round progress and emits navigation intents', async () => {
    const active = round(31, 'WAITING_ANSWER', '技术负责人')
    const completed = round(30, 'COMPLETED', 'HR')
    const wrapper = mount(InterviewRoomSidebar, {
      props: {
        activeSession: active,
        activePersona: persona,
        activePersonaStyle: persona.style,
        planSessions: [completed, active],
        activeSessionId: 31,
        completedQuestionSteps: [1],
        viewingHistoryIndex: null,
        reviewMode: false,
        actionLoading: false,
      },
      global: {
        stubs: {
          ElIcon: { template: '<span><slot /></span>' },
          ArrowLeft: true,
        },
      },
    })

    expect(wrapper.text()).toContain('第 2 题')
    expect(wrapper.text()).toContain('技术负责人')
    expect(wrapper.text()).toContain('已完成')

    await wrapper.get('[data-test="interview-room-back"]').trigger('click')
    await wrapper.get('[data-test="switch-round-30"]').trigger('click')

    expect(wrapper.emitted('back')).toHaveLength(1)
    expect(wrapper.emitted('switch-session')).toEqual([[30]])
  })

  it('shows ordered interviewer rounds only for role-based rooms', () => {
    const active = round(31, 'WAITING_ANSWER', '技术负责人')
    const wrapper = mount(InterviewRoomSidebar, {
      props: {
        mode: 'ROLE_BASED',
        activeSession: active,
        activePersona: persona,
        activePersonaStyle: persona.style,
        activeModelLabel: 'deepseek-chat',
        activeStrategyLabel: '项目追问',
        planSessions: [round(30, 'COMPLETED', 'HR'), active],
        activeSessionId: 31,
        completedQuestionSteps: [1],
        viewingHistoryIndex: null,
        reviewMode: false,
        actionLoading: false,
      },
      global: { stubs: { ElIcon: { template: '<span><slot /></span>' } } },
    })

    expect(wrapper.find('.sidebar-persona-card').exists()).toBe(false)
    expect(wrapper.find('[data-test="interview-room-personas"]').exists()).toBe(true)
    expect(wrapper.find('.persona-avatar').exists()).toBe(true)
    expect(wrapper.find('.round-switch-button').exists()).toBe(false)
    expect(wrapper.text()).toContain('技术负责人')
    expect(wrapper.text()).toContain('HR')
    expect(wrapper.text()).not.toContain('模型')
  })

  it('keeps knowledge rooms focused on the full progress axis without a model card', () => {
    const wrapper = mount(InterviewRoomSidebar, {
      props: {
        mode: 'KNOWLEDGE_TRAINING',
        activeSession: round(31, 'WAITING_ANSWER', '知识训练'),
        activePersona: null,
        activePersonaStyle: '',
        activeModelLabel: 'deepseek-chat',
        activeStrategyLabel: '深入',
        planSessions: [round(31, 'WAITING_ANSWER', '知识训练')],
        activeSessionId: 31,
        completedQuestionSteps: [],
        viewingHistoryIndex: null,
        reviewMode: false,
        actionLoading: false,
      },
      global: { stubs: { ElIcon: { template: '<span><slot /></span>' } } },
    })

    expect(wrapper.find('.sidebar-persona-card').exists()).toBe(false)
    expect(wrapper.get('.sidebar-progress').text()).toContain('第 2 题')
    expect(wrapper.find('.sidebar-runtime-meta').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('deepseek-chat')
  })
})
