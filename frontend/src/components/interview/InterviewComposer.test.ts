// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  createInterviewPlan: vi.fn(),
  createInterviewQuestionSetFromKnowledgeDocument: vi.fn(),
  getInterviewQuestionSet: vi.fn(),
  listInterviewerPersonas: vi.fn(),
  listInterviewQuestionSets: vi.fn(),
}))
vi.mock('../../api/interview', () => api)
const resumeApi = vi.hoisted(() => ({ listResumes: vi.fn() }))
vi.mock('../../api/resume', () => resumeApi)
vi.mock('../../api/project', () => ({ listProjects: vi.fn().mockResolvedValue({ success: true, data: [] }) }))
const knowledgeApi = vi.hoisted(() => ({
  listKnowledgeDocuments: vi.fn(),
  listKnowledgeCategoryTree: vi.fn(),
}))
vi.mock('../../api/knowledge', () => knowledgeApi)

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

function mountComposer(props: { mode: 'ROLE_BASED' | 'KNOWLEDGE_TRAINING' | 'EXPERIENCE_SIMULATION' } = { mode: 'ROLE_BASED' }) {
  return mount(InterviewComposer, { props })
}

describe('InterviewComposer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resumeApi.listResumes.mockResolvedValue({ success: true, data: [] })
    api.listInterviewerPersonas.mockResolvedValue({ success: true, data: [] })
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [] })
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: 'JVM 笔记', processingStatus: 'COMPLETED' },
      { id: 31, title: '提取中文档', processingStatus: 'PROCESSING' },
    ] })
    knowledgeApi.listKnowledgeCategoryTree.mockResolvedValue({ success: true, data: [] })
  })

  it('can open directly into one mode without rendering the other setup fields', async () => {
    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()

    expect(wrapper.attributes('data-mode')).toBe('EXPERIENCE_SIMULATION')
    expect(wrapper.find('[data-test="experience-simulation-setup"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="role-based-setup"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="knowledge-training-setup"]').exists()).toBe(false)
  })

  it('knowledge mode: only usable documents are selectable; start requires selection', async () => {
    const wrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
    await flushPromises()

    // 未选资料时不可开始并给出提示
    expect(wrapper.get('[data-test="composer-start"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="composer-missing-hint"]').text()).toContain('知识资料')
  })

  it('knowledge mode only exposes completed Knowledge Base documents', async () => {
    const wrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
    await flushPromises()
    const setup = wrapper.findComponent({ name: 'KnowledgeTrainingSetup' })
    expect(setup.props('documentOptions')).toEqual([expect.objectContaining({ value: 30, label: 'JVM 笔记', description: '知识库 · 已处理完成' })])
  })

  it('knowledge mode: successful start emits the real returned plan', async () => {
    api.createInterviewPlan.mockResolvedValue({ success: true, data: plan(100) })
    const wrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
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
    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()

    expect(wrapper.get('[data-test="composer-start"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-test="composer-missing-hint"]').text()).toContain('题集')
  })

  it('does not inject an undeletable demo question set when the Knowledge Base is empty', async () => {
    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()

    const setup = wrapper.findComponent({ name: 'ExperienceSimulationSetup' })
    expect(setup.props('questionSetOptions')).toEqual([])
    expect(wrapper.findAll('.question-set-row')).toHaveLength(0)
    expect(wrapper.get('.question-empty').text()).toContain('还没有真实面经题集')
    expect(wrapper.find('.demo-note').exists()).toBe(false)
  })

  it('experience mode only exposes manual or imported real interview sets', async () => {
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [
      { id: 41, title: '腾讯面经', sourceType: 'IMPORTED_EXPERIENCE', archived: false },
      { id: 42, title: '系统练习题', sourceType: 'GENERATED_PRACTICE', archived: false },
      { id: 43, title: '已归档面经', sourceType: 'USER_MANUAL', archived: true },
    ] })
    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    const setup = wrapper.findComponent({ name: 'ExperienceSimulationSetup' })
    expect(setup.props('questionSetOptions')).toEqual([expect.objectContaining({ value: 41, label: '腾讯面经', description: '未填写岗位上下文' })])
    expect(wrapper.find('[data-test="experience-persona-select"]').exists()).toBe(false)
  })

  it('exposes completed documents in the real-experience folder and materializes the selected source', async () => {
    knowledgeApi.listKnowledgeCategoryTree.mockResolvedValue({ success: true, data: [
      { id: 9, name: '真实面经', normalizedName: '真实面经', parentId: null, depth: 0, directDocumentCount: 1, descendantDocumentCount: 1, createdAt: '2026-08-27T09:00:00', updatedAt: '2026-08-27T09:00:00' },
    ] })
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: '腾讯技术面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', sourceFile: null, sourceExtension: null, sizeBytes: null, createdAt: '2026-08-27T09:00:00', updatedAt: '2026-08-27T10:00:00' },
    ] })
    api.createInterviewQuestionSetFromKnowledgeDocument.mockResolvedValue({ success: true, data: {
      id: 44, title: '腾讯技术面经', sourceType: 'USER_MANUAL', companyName: '腾讯', targetRole: 'Java 后端', questionCount: 2, archived: false,
      items: [{ positionIndex: 0, questionText: '讲讲 Redis' }, { positionIndex: 1, questionText: '如何排查慢查询？' }],
    } })
    api.getInterviewQuestionSet.mockResolvedValue({ success: true, data: {
      id: 44, title: '腾讯技术面经', sourceType: 'USER_MANUAL', questionCount: 2, archived: false,
      items: [{ positionIndex: 0, questionText: '讲讲 Redis' }, { positionIndex: 1, questionText: '如何排查慢查询？' }],
    } })

    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    const setup = wrapper.findComponent({ name: 'ExperienceSimulationSetup' })
    expect(setup.props('questionSetOptions')).toEqual([
      expect.objectContaining({ value: -30, sourceDocumentId: 30, sourceType: 'KNOWLEDGE_DOCUMENT', label: '腾讯技术面经' }),
    ])

    await setup.get('.question-set-row').trigger('click')
    await flushPromises()

    expect(api.createInterviewQuestionSetFromKnowledgeDocument).toHaveBeenCalledWith(30)
    expect(setup.props('draft')).toEqual(expect.objectContaining({ questionSetId: 44 }))
    expect(setup.props('questionSetOptions')).toEqual([
      expect.objectContaining({ value: 44, label: '腾讯技术面经', sourceType: 'USER_MANUAL' }),
    ])
  })

  it('keeps a materialized source in its original list position after selection', async () => {
    knowledgeApi.listKnowledgeCategoryTree.mockResolvedValue({ success: true, data: [
      { id: 9, name: '真实面经', normalizedName: '真实面经', parentId: null, depth: 0, directDocumentCount: 2, descendantDocumentCount: 2, createdAt: '2026-08-27T09:00:00', updatedAt: '2026-08-27T09:00:00' },
    ]})
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: '腾讯技术面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:00:00' },
      { id: 31, title: '字节技术面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:01:00' },
    ]})
    api.createInterviewQuestionSetFromKnowledgeDocument.mockResolvedValue({ success: true, data: {
      id: 44, title: '字节技术面经', sourceType: 'USER_MANUAL', companyName: '字节跳动', targetRole: 'Java 后端', questionCount: 2, archived: false,
      items: [{ positionIndex: 0, questionText: '讲讲 Redis' }, { positionIndex: 1, questionText: '如何排查慢查询？' }],
    }})

    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    const setup = wrapper.findComponent({ name: 'ExperienceSimulationSetup' })
    await setup.findAll('.question-set-row')[1].trigger('click')
    await flushPromises()

    expect(setup.props('questionSetOptions').map((item: { label: string }) => item.label)).toEqual(['腾讯技术面经', '字节技术面经'])
  })

  it('exposes every completed document in the real-experience folder', async () => {
    knowledgeApi.listKnowledgeCategoryTree.mockResolvedValue({ success: true, data: [
      { id: 9, name: '真实面经', normalizedName: '真实面经', parentId: null, depth: 0, directDocumentCount: 3, descendantDocumentCount: 3, createdAt: '2026-08-27T09:00:00', updatedAt: '2026-08-27T09:00:00' },
    ]})
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: '腾讯技术面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:00:00' },
      { id: 31, title: '字节技术面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:01:00' },
      { id: 32, title: '美团技术面经', sourceType: 'FILE', sourceExtension: 'txt', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:02:00' },
    ]})

    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()

    const setup = wrapper.findComponent({ name: 'ExperienceSimulationSetup' })
    expect(setup.props('questionSetOptions')).toEqual([
      expect.objectContaining({ value: -30, sourceDocumentId: 30, label: '腾讯技术面经' }),
      expect.objectContaining({ value: -31, sourceDocumentId: 31, label: '字节技术面经' }),
      expect.objectContaining({ value: -32, sourceDocumentId: 32, label: '美团技术面经' }),
    ])
  })

  it('refreshes the real-experience folder after another document is created', async () => {
    knowledgeApi.listKnowledgeCategoryTree.mockResolvedValue({ success: true, data: [
      { id: 9, name: '真实面经', normalizedName: '真实面经', parentId: null, depth: 0, directDocumentCount: 1, descendantDocumentCount: 1, createdAt: '2026-08-27T09:00:00', updatedAt: '2026-08-27T09:00:00' },
    ]})
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: '最初面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:00:00' },
    ]})

    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    knowledgeApi.listKnowledgeDocuments.mockResolvedValue({ success: true, data: [
      { id: 30, title: '最初面经', sourceType: 'NOTE', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:00:00' },
      { id: 31, title: '刚创建的面经', sourceType: 'FILE', sourceExtension: 'txt', processingStatus: 'COMPLETED', updatedAt: '2026-08-27T10:01:00' },
    ]})

    await wrapper.get('[data-test="experience-refresh-sources"]').trigger('click')
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ExperienceSimulationSetup' }).props('questionSetOptions'))
      .toEqual(expect.arrayContaining([
        expect.objectContaining({ value: -30, label: '最初面经' }),
        expect.objectContaining({ value: -31, label: '刚创建的面经' }),
      ]))
  })

  it('keeps the complete ordered question preview instead of truncating to the first eight', async () => {
    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [
      { id: 41, title: '腾讯面经', sourceType: 'IMPORTED_EXPERIENCE', archived: false, questionCount: 12 },
    ]})
    api.getInterviewQuestionSet.mockResolvedValue({ success: true, data: {
      id: 41, title: '腾讯面经', sourceType: 'IMPORTED_EXPERIENCE', questionCount: 12, archived: false,
      items: Array.from({ length: 12 }, (_, index) => ({ positionIndex: index, questionText: `第 ${index + 1} 题` })),
    }})

    const wrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    await wrapper.findComponent({ name: 'ExperienceSimulationSetup' }).get('.question-set-row').trigger('click')
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ExperienceSimulationSetup' }).props('preview').content).toContain('12. 第 12 题')
  })

  it('advances knowledge and experience progress from source selection to settings', async () => {
    const knowledgeWrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
    await flushPromises()
    const knowledgeSteps = () => knowledgeWrapper.findAll('[data-test="composer-progress"] .composer-progress-step')
    expect(knowledgeSteps()[0].classes()).toContain('current')
    knowledgeWrapper.findComponent({ name: 'KnowledgeTrainingSetup' }).vm.$emit('update:draft', {
      knowledgeDocumentIds: [30], difficulty: '', questionStyle: '', questionCount: 5, focusTags: [], supplement: '',
    })
    await flushPromises()
    expect(knowledgeSteps()[1].classes()).toContain('current')
    knowledgeWrapper.findComponent({ name: 'KnowledgeTrainingSetup' }).vm.$emit('update:draft', {
      knowledgeDocumentIds: [30], difficulty: '基础', questionStyle: '结构化', questionCount: 5, focusTags: [], supplement: '',
    })
    await flushPromises()
    expect(knowledgeSteps()[2].classes()).toContain('current')

    api.listInterviewQuestionSets.mockResolvedValue({ success: true, data: [
      { id: 41, title: '腾讯面经', sourceType: 'IMPORTED_EXPERIENCE', archived: false, questionCount: 12 },
    ] })
    const experienceWrapper = mountComposer({ mode: 'EXPERIENCE_SIMULATION' })
    await flushPromises()
    const experienceSteps = () => experienceWrapper.findAll('[data-test="composer-progress"] .composer-progress-step')
    experienceWrapper.findComponent({ name: 'ExperienceSimulationSetup' }).vm.$emit('update:draft', {
      questionSetId: 41, personaIds: [], followUpIntensity: '', questionCount: 10, focusTags: [], supplement: '',
    })
    await flushPromises()
    expect(experienceSteps()[1].classes()).toContain('current')
    experienceWrapper.findComponent({ name: 'ExperienceSimulationSetup' }).vm.$emit('update:draft', {
      questionSetId: 41, personaIds: [], followUpIntensity: '适中', reviewMode: 'END_OF_SESSION', questionCount: 10, focusTags: [], supplement: '',
    })
    await flushPromises()
    expect(experienceSteps()[2].classes()).toContain('current')
  })

  it('start failure keeps the composer with the error visible for retry', async () => {
    api.createInterviewPlan.mockRejectedValue(new Error('知识训练题目生成失败'))
    const wrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
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

  it('shows a direct settings action when the AI provider is not configured', async () => {
    api.createInterviewPlan.mockRejectedValue(new Error('NOT_CONFIGURED: 尚未配置 AI 模型服务'))
    const wrapper = mountComposer({ mode: 'KNOWLEDGE_TRAINING' })
    await flushPromises()
    wrapper.findComponent({ name: 'KnowledgeTrainingSetup' }).vm.$emit('update:draft', {
      knowledgeDocumentIds: [30], difficulty: '', questionCount: 5, focusTags: [], supplement: '',
    })
    await flushPromises()
    await wrapper.get('[data-test="composer-start"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="composer-ai-config-required"]').text()).toContain('需要先配置 AI 服务')
    await wrapper.get('[data-test="composer-open-settings"]').trigger('click')
    expect(wrapper.emitted('open-settings')).toHaveLength(1)
  })

  it('role mode advances the progress hint from context to interviewers and reads resume content', async () => {
    await Promise.resolve()
    resumeApi.listResumes.mockResolvedValue({ success: true, data: [{
      id: 9,
      title: '后端岗位表达',
      archivedAt: null,
      currentVersion: {
        id: 29,
        resumeId: 9,
        versionNo: 2,
        content: {
          basicInfo: { title: 'Java 后端工程师' },
          summary: '负责交易系统稳定性建设',
          skills: ['Java', 'Redis'],
        },
        createdByType: 'user',
        createdAt: '2026-08-27T10:00:00',
      },
    }] })
    vi.mocked((await import('../../api/project')).listProjects).mockResolvedValue({ success: true, data: [{
      id: 7,
      name: '腾讯 Java 后端',
      targetRole: '后端工程师',
      stage: 'interview',
      status: 'active',
      location: '深圳',
      jobDescriptionId: null,
      resumeVersionId: null,
      archivedAt: null,
      stageUpdatedAt: null,
      industry: null,
      notes: null,
      createdAt: '2026-08-27T09:00:00',
      updatedAt: '2026-08-27T09:00:00',
    }] })
    api.listInterviewerPersonas.mockResolvedValue({ success: true, data: [{ id: 5, name: '技术面试官', title: '高级工程师' }] })

    const wrapper = mountComposer({ mode: 'ROLE_BASED' })
    await flushPromises()

    const progress = () => wrapper.findAll('[data-test="composer-progress"] .composer-progress-step')
    expect(progress()[0].classes()).toContain('current')
    expect(wrapper.findComponent({ name: 'RoleBasedSetup' }).props('resumeOptions')).toEqual([
      expect.objectContaining({
        value: 29,
        preview: expect.objectContaining({
          headline: 'Java 后端工程师',
          summary: '负责交易系统稳定性建设',
          skills: ['Java', 'Redis'],
        }),
      }),
    ])

    const setup = wrapper.findComponent({ name: 'RoleBasedSetup' })
    setup.vm.$emit('update:draft', { jobProjectId: 7, resumeVersionId: 29, personaIds: [], questionCount: 5, focusTags: [], supplement: '' })
    await flushPromises()
    expect(progress()[0].classes()).toContain('completed')
    expect(progress()[1].classes()).toContain('current')

    setup.vm.$emit('update:draft', { jobProjectId: 7, resumeVersionId: 29, personaIds: [5], questionCount: 5, focusTags: [], supplement: '' })
    await flushPromises()
    expect(progress()[2].classes()).toContain('current')
    expect(wrapper.get('[data-test="role-resume-preview"]').text()).toContain('Java')
    expect(wrapper.get('[data-test="role-resume-preview"]').text()).toContain('Redis')
    expect(wrapper.get('[data-test="role-resume-preview"]').text()).not.toContain('Spring Boot')
  })
})
