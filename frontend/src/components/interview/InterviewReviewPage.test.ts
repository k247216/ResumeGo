// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InterviewReviewPage from './InterviewReviewPage.vue'
import type {
  InterviewPlanResponse,
  InterviewStatusResponse,
  PerQuestionScore,
  SessionHistoryItem,
} from '../../types/interview'

const session: InterviewStatusResponse = {
  sessionId: 7,
  status: 'COMPLETED',
  currentQuestionIndex: 2,
  totalQuestions: 2,
  completed: true,
  summaryJson: JSON.stringify({
    weaknesses: ['项目回答缺少技术取舍'],
    suggestions: ['补充一次缓存失效处理的真实证据'],
  }),
  personaName: '高级 Java 工程师',
  personaTitle: '后端面试官',
}

const plan: InterviewPlanResponse = {
  planId: 9,
  mode: 'ROLE_BASED',
  contextContractVersion: 'v1',
  startContextSnapshot: {
    jobProjectName: '腾讯 Java 后端',
    resumeTitle: '后端开发简历',
    resumeVersionNo: 3,
    reviewMode: 'END_OF_SESSION',
  },
  resumeVersionId: 3,
  jobDescriptionId: 4,
  title: '腾讯 Java 后端',
  questionCount: 2,
  focusTags: ['项目深度'],
  supplement: null,
  summary: null,
  rounds: [],
  completed: true,
  createdAt: '2026-08-22T06:00:00.000Z',
  updatedAt: '2026-08-22T07:30:00.000Z',
}

const history: SessionHistoryItem[] = [{
  questionIndex: 0,
  questionText: '请介绍一次缓存一致性问题。',
  questionType: 'technical',
  answerText: '我使用缓存和失效策略解决了问题。',
  evaluation: {
    weaknesses: ['没有说明方案取舍'],
    suggestions: ['补充失效处理过程'],
    strengths: [],
    referenceAnswer: null,
    score: { clarity: 8, relevance: 7, depth: 6, accuracy: 8 },
  },
  source: 'IMPORTED_EXPERIENCE',
  provenanceLabel: '导入面经原题',
}]

const scores: PerQuestionScore[] = [{
  questionIndex: 0,
  questionText: '请介绍一次缓存一致性问题。',
  clarity: 8,
  relevance: 7,
  depth: 6,
  accuracy: 8,
}]

function mountReview(overrides: Partial<{
  session: InterviewStatusResponse
  plan: InterviewPlanResponse | null
  history: SessionHistoryItem[]
  scores: PerQuestionScore[]
}> = {}) {
  return mount(InterviewReviewPage, {
    props: {
      session,
      plan,
      history,
      scores,
      ...overrides,
    },
  })
}

describe('InterviewReviewPage', () => {
  it('shows mode, immutable context, core issue and real answer evidence', () => {
    const wrapper = mountReview()

    expect(wrapper.get('[data-test="review-core-issue"]').text()).toContain('项目回答缺少技术取舍')
    expect(wrapper.text()).toContain('岗位模拟 · 腾讯 Java 后端')
    expect(wrapper.text()).toContain('腾讯 Java 后端 · 后端开发简历 · V3')
    expect(wrapper.text()).toContain('导入面经原题')
    expect(wrapper.text()).toContain('回顾：结束后复盘')
    expect(wrapper.text()).toContain('请介绍一次缓存一致性问题。')
    expect(wrapper.text()).toContain('没有说明方案取舍')
    expect(wrapper.findAll('[data-test="review-score-card"] .review-score-item')).toHaveLength(5)
    expect(wrapper.get('[data-test="review-score-card"] .review-score-item:nth-child(4) strong').text()).toBe('—')
  })

  it('keeps honest empty state when no real scores exist', () => {
    const wrapper = mountReview({ scores: [] })

    expect(wrapper.get('[data-test="review-empty-score"]').text()).toContain('暂无真实评分')
    expect(wrapper.findAll('[data-test="review-score-card"] .review-score-item')).toHaveLength(0)
  })

  it('renders the five homepage dimensions when the evaluation provides them', () => {
    const wrapper = mountReview({
      scores: [{
        ...scores[0],
        structure: 8,
        evidence: 7,
      }],
    })

    expect(wrapper.findAll('[data-test="review-score-card"] .review-score-item')).toHaveLength(5)
    expect(wrapper.text()).toContain('回答结构')
    expect(wrapper.text()).toContain('证据具体性')
  })

  it('emits navigation actions from both primary entry points', async () => {
    const wrapper = mountReview()

    await wrapper.get('.review-back-link').trigger('click')
    await wrapper.get('.review-primary-action').trigger('click')
    await wrapper.get('.review-aside-action').trigger('click')

    expect(wrapper.emitted('back-home')).toHaveLength(1)
    expect(wrapper.emitted('re-practice')).toHaveLength(2)
  })
})
