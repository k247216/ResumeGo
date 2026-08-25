// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  createInterviewPlan: vi.fn(),
  listInterviewerPersonas: vi.fn(),
  listInterviewQuestionSets: vi.fn(),
}))
vi.mock('../../api/interview', () => api)
vi.mock('../../api/resume', () => ({ listResumes: vi.fn().mockResolvedValue({ success: true, data: [] }) }))
vi.mock('../../api/project', () => ({ listProjects: vi.fn().mockResolvedValue({ success: true, data: [] }) }))
vi.mock('../../api/knowledge', () => ({
  listKnowledgeDocuments: vi.fn().mockResolvedValue({ success: true, data: [
    { id: 30, title: 'JVM 笔记', processingStatus: 'COMPLETED' },
    { id: 31, title: '提取中文档', processingStatus: 'PROCESSING' },
  ] }),
}))

import InterviewComposer from './InterviewComposer.vue'
import type { InterviewPlanResponse } from '../../types/interview'

const plan = (id: number): InterviewPlanResponse => ({
  planId: id,
  mode: 'KNOWLEDGE_TRAINING',
  contextContractVersion: '1',
  startContextSnapshot: { knowledgeDocumentTitles: ['JVM 笔记'] },
  resumeVersionId: 0,
  jobDescriptionId: 0,
  title: '多轮模拟面试',
  questionCount: 5,
  focusTags: [],
  supplement: null,
  summary: null,
  summaryGeneratedAt: null,
  rounds: [],
  completed: false,
})

function mountComposer() {
  return mount(InterviewComposer)
}

describe('InterviewComposer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listInterviewerPersonas.mockResolvedValue({ success: true, data: [] })
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [] })
  })

  it('knowledge mode: only usable documents are selectable; start requires selection', async () => {
    const wrapper = mountComposer()
    await flushPromises()

    await wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').trigger('click')
    await flushPromises()

    // 未选资料时不可开始并给出提示
    expect(wrapper.get('[data-test="composer-start"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="composer-missing-hint"]').text()).toContain('知识资料')
  })

  it('knowledge mode: successful start emits the real returned plan', async () => {
    api.createInterviewPlan.mockResolvedValue({ success: true, data: plan(100) })
    const wrapper = mountComposer()
    await flushPromises()

    await wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').trigger('click')
    await flushPromises()

    // 通过选项组件选中资料（el-select 交互复杂，直接操作组件内部状态：触发 draft 更新事件）
    const setup = wrapper.findComponent({ name: 'KnowledgeTrainingSetup' })
    expect(setup.exists()).toBe(true)
    setup.vm.$emit('update:draft', {
      knowledgeDocumentIds: [30],
      difficulty: '深入',
      questionCount: 5,
      focusTags: [],
      supplement: '',
    })
    await flushPromises()

    await wrapper.get('[data-test="composer-start"]').trigger('click')
    await flushPromises()

    expect(api.createInterviewPlan).toHaveBeenCalledWith({
      mode: 'KNOWLEDGE_TRAINING',
      knowledgeDocumentIds: [30],
      difficulty: '深入',
      questionCount: 5,
      focusTags: undefined,
      supplement: undefined,
    })
    expect(wrapper.emitted('started')).toEqual([[plan(100)]])
  })

  it('experience mode: missing question set blocks start with a hint', async () => {
    const wrapper = mountComposer()
    await flushPromises()

    await wrapper.get('[data-test="mode-EXPERIENCE_SIMULATION"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="composer-start"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="composer-missing-hint"]').text()).toContain('题集')
  })

  it('start failure keeps the composer with the error visible for retry', async () => {
    api.createInterviewPlan.mockRejectedValue(new Error('知识训练题目生成失败'))
    const wrapper = mountComposer()
    await flushPromises()

    await wrapper.get('[data-test="mode-KNOWLEDGE_TRAINING"]').trigger('click')
    await flushPromises()
    const setup = wrapper.findComponent({ name: 'KnowledgeTrainingSetup' })
    setup.vm.$emit('update:draft', {
      knowledgeDocumentIds: [30],
      difficulty: '',
      questionCount: 5,
      focusTags: [],
      supplement: '',
    })
    await flushPromises()

    await wrapper.get('[data-test="composer-start"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="composer-error"]').text()).toContain('知识训练题目生成失败')
    // 草稿保留，可重试
    expect(wrapper.find('[data-test="composer-start"]').attributes('disabled')).toBeUndefined()
  })
})
