import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { InterviewPlanResponse } from '../types/interview'
import { useInterviewComposer } from './useInterviewComposer'
import { createInterviewPlan } from '../api/interview'

vi.mock('../api/interview', () => ({
  createInterviewPlan: vi.fn(),
}))

const plan = (id: number): InterviewPlanResponse => ({
  planId: id,
  mode: 'ROLE_BASED',
  contextContractVersion: '1',
  startContextSnapshot: {},
  resumeVersionId: 10,
  jobDescriptionId: 20,
  title: '多轮模拟面试',
  questionCount: 5,
  focusTags: [],
  supplement: null,
  summary: null,
  summaryGeneratedAt: null,
  rounds: [],
  completed: false,
})

beforeEach(() => {
  vi.clearAllMocks()
})

describe('useInterviewComposer', () => {
  it('初始状态未选模式，不可开始', () => {
    const composer = useInterviewComposer()
    expect(composer.mode.value).toBeNull()
    expect(composer.canStart.value).toBe(false)
    expect(composer.missingHint.value).toContain('训练模式')
  })

  it('岗位模式：目标/版本/人设齐备才可开始，缺失项给出可读提示', () => {
    const composer = useInterviewComposer()
    composer.switchMode('ROLE_BASED')

    expect(composer.canStart.value).toBe(false)
    expect(composer.missingHint.value).toContain('求职目标')

    composer.roleDraft.value.jobProjectId = 5
    expect(composer.missingHint.value).toContain('简历版本')

    composer.roleDraft.value.resumeVersionId = 10
    expect(composer.missingHint.value).toContain('面试官')

    composer.roleDraft.value.personaIds = [1]
    expect(composer.canStart.value).toBe(true)
    expect(composer.missingHint.value).toBeNull()
  })

  it('知识模式只要求资料；面经模式只要求题集，互不复制不兼容字段', () => {
    const composer = useInterviewComposer()
    composer.switchMode('KNOWLEDGE_TRAINING')
    composer.knowledgeDraft.value.knowledgeDocumentIds = [30]
    expect(composer.canStart.value).toBe(true)

    composer.switchMode('EXPERIENCE_SIMULATION')
    expect(composer.canStart.value).toBe(false)
    expect(composer.missingHint.value).toContain('题集')

    composer.experienceDraft.value.questionSetId = 40
    expect(composer.canStart.value).toBe(true)

    // 切回知识模式：草稿保留
    composer.switchMode('KNOWLEDGE_TRAINING')
    expect(composer.knowledgeDraft.value.knowledgeDocumentIds).toEqual([30])
  })

  it('岗位模式草稿在模式切换后保留', () => {
    const composer = useInterviewComposer()
    composer.switchMode('ROLE_BASED')
    composer.roleDraft.value.jobProjectId = 5
    composer.roleDraft.value.resumeVersionId = 10
    composer.roleDraft.value.personaIds = [1]

    composer.switchMode('EXPERIENCE_SIMULATION')
    composer.switchMode('ROLE_BASED')

    expect(composer.roleDraft.value.jobProjectId).toBe(5)
    expect(composer.roleDraft.value.personaIds).toEqual([1])
  })

  it('开始成功使用真实返回 plan 并保存为结果', async () => {
    vi.mocked(createInterviewPlan).mockResolvedValue({ success: true, data: plan(100) })
    const composer = useInterviewComposer()
    composer.switchMode('ROLE_BASED')
    composer.roleDraft.value.jobProjectId = 5
    composer.roleDraft.value.resumeVersionId = 10
    composer.roleDraft.value.personaIds = [1]

    const result = await composer.start()

    expect(createInterviewPlan).toHaveBeenCalledWith({
      mode: 'ROLE_BASED',
      jobProjectId: 5,
      resumeVersionId: 10,
      questionCount: 5,
      personaIds: [1],
      focusTags: undefined,
      supplement: undefined,
    })
    expect(result.planId).toBe(100)
    expect(composer.resultPlan.value?.planId).toBe(100)
    expect(composer.error.value).toBe('')
  })

  it('开始失败保留草稿并暴露错误，可重试', async () => {
    vi.mocked(createInterviewPlan).mockRejectedValue(new Error('JD 缺失'))
    const composer = useInterviewComposer()
    composer.switchMode('ROLE_BASED')
    composer.roleDraft.value.jobProjectId = 5
    composer.roleDraft.value.resumeVersionId = 10
    composer.roleDraft.value.personaIds = [1]

    await expect(composer.start()).rejects.toThrow('JD 缺失')
    expect(composer.error.value).toBe('JD 缺失')
    expect(composer.roleDraft.value.jobProjectId).toBe(5)

    vi.mocked(createInterviewPlan).mockResolvedValue({ success: true, data: plan(101) })
    const result = await composer.start()
    expect(result.planId).toBe(101)
    expect(composer.error.value).toBe('')
  })

  it('未选模式直接开始被拒绝', async () => {
    const composer = useInterviewComposer()
    await expect(composer.start()).rejects.toThrow('训练模式')
  })
})
