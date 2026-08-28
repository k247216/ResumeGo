import { computed, ref } from 'vue'
import type {
  CreateInterviewPlanRequest,
  ExperienceSimulationPlanRequest,
  InterviewReviewMode,
  InterviewMode,
  InterviewPlanResponse,
  KnowledgeTrainingPlanRequest,
  RoleBasedPlanRequest,
} from '../types/interview'
import { createInterviewPlan } from '../api/interview'

export const INTERVIEW_QUESTION_LIMITS = {
  ROLE_BASED: { min: 5, max: 15 },
  KNOWLEDGE_TRAINING: { min: 1, max: 20 },
  /** 面经允许练习题集全部原题，但单次练习最多 30 题。小题集按实际数量放宽下限。 */
  EXPERIENCE_SIMULATION: { min: 1, max: 30 },
} as const

export function getQuestionCountBounds(mode: InterviewMode, availableCount?: number | null) {
  const base = INTERVIEW_QUESTION_LIMITS[mode]
  if (mode !== 'EXPERIENCE_SIMULATION' || availableCount == null) return base
  const max = Math.max(1, Math.min(base.max, Math.floor(availableCount)))
  return { min: max >= 10 ? 10 : 1, max }
}

export function clampQuestionCount(mode: InterviewMode, value: number, availableCount?: number | null) {
  const bounds = getQuestionCountBounds(mode, availableCount)
  const normalized = Number.isFinite(value) ? Math.round(value) : bounds.min
  return Math.min(bounds.max, Math.max(bounds.min, normalized))
}

/** 各模式独立草稿：切换模式不复制不兼容 ID */
export interface RoleBasedDraft {
  jobProjectId: number | null
  resumeVersionId: number | null
  personaIds: number[]
  questionCount: number
  focusTags: string[]
  supplement: string
}
export interface KnowledgeTrainingDraft {
  knowledgeDocumentIds: number[]
  difficulty: string
  questionStyle?: string
  questionCount: number
  focusTags: string[]
  supplement: string
}
export interface ExperienceSimulationDraft {
  questionSetId: number | null
  personaIds: number[]
  /** 题集原始题目索引的用户排序；空数组表示沿用资料原顺序。 */
  questionOrder?: number[]
  followUpIntensity: string
  reviewMode?: InterviewReviewMode
  questionCount: number
  focusTags: string[]
  supplement: string
}

/**
 * 三模式面试准备状态：模式选择、模式专属草稿与开始动作。
 * 不自动选择列表第一条 Pipeline、简历、资料或题集。
 */
export function useInterviewComposer() {
  const mode = ref<InterviewMode | null>(null)
  const submitting = ref(false)
  const error = ref('')
  const resultPlan = ref<InterviewPlanResponse | null>(null)

  const roleDraft = ref<RoleBasedDraft>({
    jobProjectId: null,
    resumeVersionId: null,
    personaIds: [],
    questionCount: 5,
    focusTags: [],
    supplement: '',
  })
  const knowledgeDraft = ref<KnowledgeTrainingDraft>({
    knowledgeDocumentIds: [],
    difficulty: '',
    questionStyle: '',
    questionCount: 5,
    focusTags: [],
    supplement: '',
  })
  const experienceDraft = ref<ExperienceSimulationDraft>({
    questionSetId: null,
    personaIds: [],
    questionOrder: [],
    followUpIntensity: '',
    reviewMode: undefined,
    questionCount: 10,
    focusTags: [],
    supplement: '',
  })

  function switchMode(next: InterviewMode) {
    mode.value = next
    error.value = ''
  }

  const roleReady = computed(() =>
    roleDraft.value.jobProjectId != null
    && roleDraft.value.resumeVersionId != null
    && roleDraft.value.personaIds.length > 0)
  const knowledgeReady = computed(() => knowledgeDraft.value.knowledgeDocumentIds.length > 0)
  const experienceReady = computed(() => experienceDraft.value.questionSetId != null)

  const canStart = computed(() => {
    if (mode.value == null || submitting.value) return false
    return mode.value === 'ROLE_BASED' ? roleReady.value
      : mode.value === 'KNOWLEDGE_TRAINING' ? knowledgeReady.value
        : experienceReady.value
  })

  /** 缺失项的用户可读解释（不自动补齐） */
  const missingHint = computed<string | null>(() => {
    if (mode.value == null) return '请先选择训练模式'
    if (submitting.value) return null
    if (mode.value === 'ROLE_BASED') {
      const draft = roleDraft.value
      if (draft.jobProjectId == null) return '请选择求职目标'
      if (draft.resumeVersionId == null) return '请选择简历版本'
      if (draft.personaIds.length === 0) return '请至少选择一位面试官'
      return null
    }
    if (mode.value === 'KNOWLEDGE_TRAINING') {
      return knowledgeDraft.value.knowledgeDocumentIds.length === 0 ? '请选择知识资料' : null
    }
    return experienceDraft.value.questionSetId == null ? '请选择面经题集' : null
  })

  function buildRequest(experienceAvailableCount?: number | null): CreateInterviewPlanRequest {
    if (mode.value === 'ROLE_BASED') {
      const draft = roleDraft.value
      if (draft.jobProjectId == null || draft.resumeVersionId == null) {
        throw new Error('岗位模拟必须选择求职目标与简历版本')
      }
      const req: RoleBasedPlanRequest = {
        mode: 'ROLE_BASED',
        jobProjectId: draft.jobProjectId,
        resumeVersionId: draft.resumeVersionId,
        questionCount: clampQuestionCount('ROLE_BASED', draft.questionCount),
        personaIds: draft.personaIds,
        focusTags: draft.focusTags.length ? draft.focusTags : undefined,
        supplement: draft.supplement.trim() || undefined,
      }
      return req
    }
    if (mode.value === 'KNOWLEDGE_TRAINING') {
      const draft = knowledgeDraft.value
      if (draft.knowledgeDocumentIds.length === 0) {
        throw new Error('知识训练必须选择知识资料')
      }
      const req: KnowledgeTrainingPlanRequest = {
        mode: 'KNOWLEDGE_TRAINING',
        knowledgeDocumentIds: draft.knowledgeDocumentIds,
        difficulty: draft.difficulty.trim() || undefined,
        questionStyle: draft.questionStyle?.trim() || undefined,
        questionCount: clampQuestionCount('KNOWLEDGE_TRAINING', draft.questionCount),
        focusTags: draft.focusTags.length ? draft.focusTags : undefined,
        supplement: draft.supplement.trim() || undefined,
      }
      return req
    }
    const draft = experienceDraft.value
    if (draft.questionSetId == null) {
      throw new Error('面经模拟必须选择题集')
    }
    const req: ExperienceSimulationPlanRequest = {
      mode: 'EXPERIENCE_SIMULATION',
      questionSetId: draft.questionSetId,
      personaIds: draft.personaIds.length ? draft.personaIds : undefined,
      ...(draft.questionOrder?.length ? { questionOrder: [...draft.questionOrder] } : {}),
      followUpIntensity: draft.followUpIntensity.trim() || undefined,
      reviewMode: draft.reviewMode,
      questionCount: clampQuestionCount('EXPERIENCE_SIMULATION', draft.questionCount, experienceAvailableCount),
      focusTags: draft.focusTags.length ? draft.focusTags : undefined,
      supplement: draft.supplement.trim() || undefined,
    }
    return req
  }

  /** 开始训练：成功返回真实 plan 并保存为结果；失败保留草稿与错误。 */
  async function start(experienceAvailableCount?: number | null): Promise<InterviewPlanResponse> {
    if (mode.value == null) {
      throw new Error('请先选择训练模式')
    }
    error.value = ''
    submitting.value = true
    try {
      const response = await createInterviewPlan(buildRequest(experienceAvailableCount))
      resultPlan.value = response.data
      return response.data
    } catch (e) {
      error.value = e instanceof Error ? e.message : '开始训练失败'
      throw e
    } finally {
      submitting.value = false
    }
  }

  return {
    mode,
    roleDraft,
    knowledgeDraft,
    experienceDraft,
    submitting,
    error,
    resultPlan,
    roleReady,
    knowledgeReady,
    experienceReady,
    canStart,
    missingHint,
    switchMode,
    start,
  }
}
