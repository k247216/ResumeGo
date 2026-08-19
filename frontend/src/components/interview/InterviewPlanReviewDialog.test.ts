import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { InterviewPlanReviewSummary } from '../../utils/interviewReview'
import InterviewPlanReviewDialog from './InterviewPlanReviewDialog.vue'

const summary: InterviewPlanReviewSummary = {
  plan: { jobLabel: '示例公司 · 后端工程师', resumeLabel: '主简历 v3' },
  completedRounds: 2,
  totalRounds: 2,
  overall: {
    average: 7.5,
    displayAverage: '7.5',
    dimensions: [
      { key: 'clarity', label: '表达清晰度', value: 7.5, color: '#3b82f6' },
      { key: 'relevance', label: '岗位相关性', value: 8, color: '#10b981' },
      { key: 'depth', label: '技术深度', value: 6.5, color: '#f59e0b' },
      { key: 'accuracy', label: '回答准确性', value: 8, color: '#8b5cf6' },
    ],
    strongest: { key: 'accuracy', label: '回答准确性', value: 8, color: '#8b5cf6' },
    weakest: { key: 'depth', label: '技术深度', value: 6.5, color: '#f59e0b' },
  },
  rounds: [{
    sessionId: 31,
    personaName: '技术负责人',
    personaTitle: '研发总监',
    order: 1,
    completed: true,
    questionCount: 5,
    summary: null,
  }],
  cachedSummary: {
    overallSummary: '表达完整，技术细节仍需补充。',
    overallScore: 7.5,
    crossStrengths: ['结构清晰'],
    crossWeaknesses: ['技术深度不足'],
    suggestions: ['补充方案取舍'],
    sessions: [],
  },
}

describe('InterviewPlanReviewDialog', () => {
  it('renders the complete plan review without owning interview state', () => {
    const wrapper = mount(InterviewPlanReviewDialog, {
      props: { modelValue: true, summary },
      global: {
        stubs: {
          ElDialog: { template: '<section><slot /><slot name="footer" /></section>' },
          ElButton: { template: '<button><slot /></button>' },
          ElIcon: { template: '<span><slot /></span>' },
          Trophy: true,
        },
      },
    })

    expect(wrapper.text()).toContain('示例公司 · 后端工程师')
    expect(wrapper.text()).toContain('7.5')
    expect(wrapper.text()).toContain('表达完整，技术细节仍需补充。')
    expect(wrapper.text()).toContain('补充方案取舍')
  })

  it('emits close through the model contract', async () => {
    const wrapper = mount(InterviewPlanReviewDialog, {
      props: { modelValue: true, summary: null },
      global: {
        stubs: {
          ElDialog: { template: '<section><slot /><slot name="footer" /></section>' },
          ElButton: { template: '<button><slot /></button>' },
          ElIcon: true,
          Trophy: true,
        },
      },
    })

    await wrapper.get('[data-test="close-plan-review"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })
})
