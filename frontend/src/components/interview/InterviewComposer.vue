<template>
  <section class="interview-composer" :data-mode="composer.mode.value" data-test="interview-composer">
    <template v-if="composer.mode.value">
      <div class="composer-progress" data-test="composer-progress" aria-label="练习配置进度">
        <div
          v-for="(step, index) in setupSteps"
          :key="step.label"
          class="composer-progress-step"
          :class="{ current: index === activeStepIndex, completed: index < activeStepIndex }"
          :aria-current="index === activeStepIndex ? 'step' : undefined"
        >
          <span class="composer-progress-node">{{ index < activeStepIndex ? '✓' : index + 1 }}</span>
          <strong>{{ step.label }}</strong>
          <small>{{ step.description }}</small>
        </div>
      </div>

      <div class="composer-body">
        <RoleBasedSetup
          v-if="composer.mode.value === 'ROLE_BASED'"
          :draft="composer.roleDraft.value"
          :project-options="projectOptions"
          :resume-options="resumeOptions"
          :persona-options="personaOptions"
          @update:draft="composer.roleDraft.value = $event"
        />
        <KnowledgeTrainingSetup
          v-else-if="composer.mode.value === 'KNOWLEDGE_TRAINING'"
          :draft="composer.knowledgeDraft.value"
          :document-options="documentOptions"
          :category-options="knowledgeCategoryOptions"
          :selected-category-id="selectedKnowledgeCategoryId"
          :preview="knowledgePreview"
          :preview-loading="previewLoading === 'knowledge'"
          @update:draft="composer.knowledgeDraft.value = $event"
          @preview-document="previewDocument"
          @select-category="selectKnowledgeCategory"
          @open-knowledge-document="openKnowledgeDocument"
        />
        <ExperienceSimulationSetup
          v-else
          :draft="composer.experienceDraft.value"
          :question-set-options="questionSetOptions"
          :preview="experiencePreview"
          :preview-loading="previewLoading === 'experience'"
          @update:draft="composer.experienceDraft.value = $event"
          @preview-question-set="previewQuestionSet"
          @materialize-source="materializeExperienceSource"
          @refresh-sources="loadOptions"
        />

        <p v-if="composer.missingHint.value" class="composer-hint" data-test="composer-missing-hint">{{ composer.missingHint.value }}</p>
        <p v-if="composer.error.value && !aiConfigurationRequired" class="composer-error" data-test="composer-error">{{ composer.error.value }}</p>
        <div v-if="aiConfigurationRequired" class="composer-ai-config-required" data-test="composer-ai-config-required" role="alert">
          <div>
            <strong>需要先配置 AI 服务</strong>
            <p>本次练习需要调用你配置的模型服务，配置完成后可回到这里继续，不会丢失当前设置。</p>
          </div>
          <button type="button" data-test="composer-open-settings" @click="emit('open-settings')">去设置</button>
        </div>

        <div class="composer-footer">
          <div class="composer-footer-copy">
            <strong>{{ selectionSummary }}</strong>
            <small>{{ composer.submitting.value ? '正在锁定本次练习上下文…' : '开始后模式、来源和配置会保存为本次练习快照。' }}</small>
          </div>
          <button
            type="button"
            class="composer-start"
            data-test="composer-start"
            :disabled="!composer.canStart.value"
            @click="start"
          >{{ composer.submitting.value ? '开始中…' : startLabel }}</button>
        </div>
      </div>
    </template>
    <div v-else class="composer-mode-missing" data-test="composer-mode-missing">
      <strong>尚未选择练习方式</strong>
      <p>请从面试主页进入一个具体的练习入口。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { clampQuestionCount, useInterviewComposer } from '../../composables/useInterviewComposer'
import type { InterviewMode, InterviewPlanResponse, InterviewQuestionSetResponse } from '../../types/interview'
import { listResumes } from '../../api/resume'
import { listProjects } from '../../api/project'
import { createInterviewQuestionSetFromKnowledgeDocument, getInterviewQuestionSet, listInterviewerPersonas, listInterviewQuestionSets } from '../../api/interview'
import { getKnowledgeContent, listKnowledgeCategoryTree, listKnowledgeDocuments } from '../../api/knowledge'
import { TARGET_STAGE_LABELS } from '../../types/project'
import type { ResumeContent } from '../../types/resume'
import type { KnowledgeCategoryNode, KnowledgeDocument } from '../../types/knowledge'
import RoleBasedSetup, { type SelectOption } from './RoleBasedSetup.vue'
import KnowledgeTrainingSetup from './KnowledgeTrainingSetup.vue'
import ExperienceSimulationSetup from './ExperienceSimulationSetup.vue'

const props = defineProps<{ mode: InterviewMode }>()
const emit = defineEmits<{
  started: [plan: InterviewPlanResponse]
  'open-knowledge-document': [id: number]
  'open-settings': []
}>()

const composer = useInterviewComposer()
const projectOptions = ref<SelectOption[]>([])
const resumeOptions = ref<SelectOption[]>([])
const personaOptions = ref<SelectOption[]>([])
const documentOptions = ref<SelectOption[]>([])
const knowledgeCategoryOptions = ref<KnowledgeCategoryNode[]>([])
const selectedKnowledgeCategoryId = ref<number | null>(null)
const questionSetOptions = ref<SelectOption[]>([])
type SourcePreview = { id: number; title: string; typeLabel: string; meta: string; content: string }
const knowledgePreview = ref<SourcePreview | null>(null)
const experiencePreview = ref<SourcePreview | null>(null)
const previewLoading = ref<'knowledge' | 'experience' | null>(null)

const setupSteps = computed(() => {
  if (composer.mode.value === 'ROLE_BASED') {
    return [
      { label: '目标与简历', description: '确认岗位与版本' },
      { label: '面试官顺序', description: '安排本次节奏' },
      { label: '开始', description: '锁定配置并进入房间' },
    ]
  }
  if (composer.mode.value === 'EXPERIENCE_SIMULATION') {
    return [
      { label: '题集来源', description: '选择真实面经' },
      { label: '演练重点', description: '设置追问与范围' },
      { label: '开始', description: '锁定配置并开始演练' },
    ]
  }
  return [
    { label: '资料', description: '选择知识库来源' },
    { label: '训练重点', description: '设置深度与范围' },
    { label: '开始', description: '锁定配置并开始训练' },
  ]
})

const startLabel = computed(() => composer.mode.value === 'ROLE_BASED' ? '开始模拟'
  : composer.mode.value === 'EXPERIENCE_SIMULATION' ? '开始演练' : '开始训练')
/** 顶部三步反映当前模式草稿的真实完成度，不会驱动或修改面试状态。 */
const activeStepIndex = computed(() => {
  if (composer.mode.value === 'ROLE_BASED') {
    const draft = composer.roleDraft.value
    if (draft.jobProjectId == null || draft.resumeVersionId == null) return 0
    if (draft.personaIds.length === 0) return 1
    return 2
  }
  if (composer.mode.value === 'KNOWLEDGE_TRAINING') {
    const draft = composer.knowledgeDraft.value
    if (draft.knowledgeDocumentIds.length === 0) return 0
    if (!draft.difficulty && !draft.questionStyle && draft.focusTags.length === 0) return 1
    return 2
  }
  const draft = composer.experienceDraft.value
  if (draft.questionSetId == null) return 0
  if (!draft.followUpIntensity && !draft.reviewMode && draft.focusTags.length === 0) return 1
  return 2
})
const selectionSummary = computed(() => {
  if (composer.mode.value === 'ROLE_BASED') {
    const count = composer.roleDraft.value.personaIds.length
    return count ? `已安排 ${count} 位面试官` : '还未安排面试官顺序'
  }
  if (composer.mode.value === 'EXPERIENCE_SIMULATION') {
    return composer.experienceDraft.value.questionSetId != null ? '已选择 1 组真实面经题集' : '还未选择真实面经题集'
  }
  const count = composer.knowledgeDraft.value.knowledgeDocumentIds.length
  return count ? `已选择 ${count} 份知识库资料` : '还未选择知识库资料'
})
const aiConfigurationRequired = computed(() => /NOT_CONFIGURED|尚未配置\s*AI\s*模型服务|尚未配置\s*模型服务/i.test(composer.error.value))

watch(() => props.mode, (mode) => {
  if (mode && composer.mode.value !== mode) composer.switchMode(mode)
}, { immediate: true })

let loadGeneration = 0
async function loadOptions() {
  const generation = ++loadGeneration
  const results = await Promise.allSettled([
    listProjects(),
    listResumes(),
    listInterviewerPersonas(),
    listKnowledgeDocuments(),
    listInterviewQuestionSets(),
    listKnowledgeCategoryTree(),
  ])
  if (generation !== loadGeneration) return
  if (results[0].status === 'fulfilled') {
    projectOptions.value = results[0].value.data
      .filter((item) => item.status === 'active')
      .map((item) => ({
        value: item.id,
        label: item.name,
        description: [item.targetRole, TARGET_STAGE_LABELS[item.stage]].filter(Boolean).join(' · ') || '真实求职计划',
        meta: item.location || undefined,
      }))
  }
  if (results[1].status === 'fulfilled') {
    resumeOptions.value = results[1].value.data
      .filter((item) => !item.archivedAt && item.currentVersion)
      .map((item) => ({
        value: item.currentVersion!.id,
        label: `${item.title} · V${item.currentVersion!.versionNo}`,
        description: '本地简历版本',
        preview: resumePreviewFromContent(item.currentVersion!.content),
      }))
  }
  if (results[2].status === 'fulfilled') {
    personaOptions.value = results[2].value.data
      .map((item) => ({ value: item.id, label: `${item.name} · ${item.title}`, description: '已保存的面试官角色' }))
  }
  if (results[3].status === 'fulfilled') {
    documentOptions.value = documentOptionsFromDocuments(results[3].value.data)
  }
  if (results[4].status === 'fulfilled') {
    questionSetOptions.value = results[4].value.data
      // 真题演练只展示用户手动录入或导入的真实面经；生成练习题不混入真实题集选择。
      .filter((item) => !item.archived && (item.sourceType === 'USER_MANUAL' || item.sourceType === 'IMPORTED_EXPERIENCE'))
      .map(questionSetOptionFromResponse)
  }
  if (results[5].status === 'fulfilled') {
    knowledgeCategoryOptions.value = results[5].value.data
    const realExperienceCategory = knowledgeCategoryOptions.value.find((item) =>
      item.name.trim().replace(/\s+/g, ' ').toLowerCase() === '真实面经')
    if (realExperienceCategory) {
      try {
        const response = await listKnowledgeDocuments(realExperienceCategory.id, null, true)
        if (generation !== loadGeneration) return
        const materializedDocumentIds = new Set(questionSetOptions.value
          .map((item) => item.sourceDocumentId)
          .filter((id): id is number => id != null))
        const sourceOptions = documentOptionsFromDocuments(response.data)
          .filter((item) => !materializedDocumentIds.has(item.value))
          .map((item) => ({
          ...item,
          // 负数仅作为未物化资料的 UI 标识，避免与现有题集 ID 冲突；真正开始前会换成后端题集 ID。
          value: -item.value,
          sourceType: 'KNOWLEDGE_DOCUMENT',
          sourceDocumentId: item.value,
          description: '知识库 · 真实面经资料',
          }))
        questionSetOptions.value = [...sourceOptions, ...questionSetOptions.value]
      } catch {
        // 分类存在但列表不可用时保留已经登记的题集，不注入演示数据。
      }
    }
  }

}

function questionSetOptionFromResponse(item: InterviewQuestionSetResponse): SelectOption {
  return {
    value: item.id,
    label: item.title,
    description: [item.companyName, item.targetRole].filter(Boolean).join(' · ') || '未填写岗位上下文',
    sourceType: item.sourceType,
    itemCount: item.questionCount ?? item.items?.length ?? undefined,
    updatedAt: item.updatedAt,
    companyName: item.companyName,
    targetRole: item.targetRole,
    companyIconKey: item.companyIconKey,
    sourceDocumentId: item.sourceDocumentId,
  }
}

function documentOptionsFromDocuments(documents: KnowledgeDocument[]): SelectOption[] {
  return documents
    .filter((item) => item.processingStatus === 'COMPLETED')
    .map((item) => ({
      value: item.id,
      label: item.title,
      description: '知识库 · 已处理完成',
      fileType: fileTypeLabel(item.sourceExtension, item.sourceType),
      knowledgeSourceType: item.sourceType,
      updatedAt: item.updatedAt,
      meta: item.sourceFile || '本地资料',
      fileSize: formatFileSize(item.sizeBytes),
    }))
}

async function selectKnowledgeCategory(categoryId: number | null) {
  selectedKnowledgeCategoryId.value = categoryId
  try {
    const response = await listKnowledgeDocuments(categoryId, null, false)
    documentOptions.value = documentOptionsFromDocuments(response.data)
  } catch {
    documentOptions.value = []
  }
}

function openKnowledgeDocument(documentId: number) {
  emit('open-knowledge-document', documentId)
}

function fileTypeLabel(extension: string | null | undefined, sourceType: string | null | undefined) {
  if (sourceType === 'NOTE') return 'MD'
  const normalized = extension?.replace(/^\./, '').toUpperCase()
  return normalized || 'FILE'
}

function formatFileSize(sizeBytes: number | null | undefined) {
  if (sizeBytes == null || sizeBytes < 0) return undefined
  if (sizeBytes < 1024) return `${sizeBytes} B`
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`
}

function resumePreviewFromContent(content: ResumeContent | null | undefined): SelectOption['preview'] {
  if (!content) return undefined
  const skills = [
    ...(content.skills ?? []),
    ...(content.skillCategories ?? []).flatMap((category) => category.skills ?? []),
    ...(content.projects ?? []).flatMap((project) => project.technologies ?? []),
    ...(content.workExperience ?? []).flatMap((experience) => experience.technologies ?? []),
    ...(content.githubProjects ?? []).flatMap((project) => project.technologies ?? []),
  ]
    .map((skill) => typeof skill === 'string' ? skill.trim() : '')
    .filter(Boolean)
    .filter((skill, index, values) => values.indexOf(skill) === index)
    .slice(0, 8)
  const headline = content.basicInfo?.title?.trim() || content.basicInfo?.targetRole?.trim()
  const summary = content.summary?.trim()
  if (!headline && !summary && skills.length === 0) return undefined
  return { headline, summary, skills }
}

async function previewDocument(documentId: number) {
  if (documentId <= 0) return
  const option = documentOptions.value.find((item) => item.value === documentId)
  if (!option) return
  previewLoading.value = 'knowledge'
  try {
    const response = await getKnowledgeContent(documentId)
    knowledgePreview.value = {
      id: documentId,
      title: option.label,
      typeLabel: option.fileType || 'FILE',
      meta: option.meta || '本地资料',
      content: response.data.content.slice(0, 1600),
    }
  } catch {
    knowledgePreview.value = {
      id: documentId,
      title: option.label,
      typeLabel: option.fileType || 'FILE',
      meta: '正文暂时不可用',
      content: '当前资料的正文暂时无法读取，但它仍会按来源参与本次训练。',
    }
  } finally {
    previewLoading.value = null
  }
}

async function previewQuestionSet(questionSetId: number | null) {
  if (questionSetId == null || questionSetId <= 0) {
    experiencePreview.value = null
    return
  }
  const option = questionSetOptions.value.find((item) => item.value === questionSetId)
  if (!option) return
  previewLoading.value = 'experience'
  try {
    const response = await getInterviewQuestionSet(questionSetId)
    const items = response.data.items ?? []
    experiencePreview.value = {
      id: questionSetId,
      title: option.label,
      typeLabel: '真实题集',
      meta: option.sourceType === 'IMPORTED_EXPERIENCE' ? '外部导入' : '手动笔记',
      // 预览必须保留题集的完整顺序；展示层通过滚动容器承载较长题集，不能静默截断。
      content: items.map((item, index) => `${index + 1}. ${item.questionText}`).join('\n') || '题集详情暂时没有可展示的原题。',
    }
  } catch {
    experiencePreview.value = {
      id: questionSetId,
      title: option.label,
      typeLabel: '真实题集',
      meta: '题集详情暂时不可用',
      content: '当前题集的原题暂时无法读取，请稍后重试。',
    }
  } finally {
    previewLoading.value = null
  }
}

async function materializeExperienceSource(documentId: number) {
  previewLoading.value = 'experience'
  const sourceOption = questionSetOptions.value.find((item) => item.sourceDocumentId === documentId)
  const sourceIndex = questionSetOptions.value.findIndex((item) => item.sourceDocumentId === documentId)
  try {
    const response = await createInterviewQuestionSetFromKnowledgeDocument(documentId)
    const materialized = response.data
    const materializedOption = questionSetOptionFromResponse(materialized)
    // 物化只是把当前资料替换成可启动的题集，不应改变用户正在浏览的列表顺序。
    // 这样点击第二/第三个资料时，选中项仍留在原位置，避免列表跳动造成“选取台”错觉。
    questionSetOptions.value = sourceIndex >= 0
      ? questionSetOptions.value.map((item, index) => index === sourceIndex ? materializedOption : item)
      : [...questionSetOptions.value, materializedOption]
    const current = composer.experienceDraft.value
    composer.experienceDraft.value = {
      ...current,
      questionSetId: materialized.id,
      questionCount: clampQuestionCount('EXPERIENCE_SIMULATION', current.questionCount, materializedOption.itemCount),
    }
    await previewQuestionSet(materialized.id)
  } catch (error) {
    experiencePreview.value = {
      id: documentId,
      title: sourceOption?.label || '真实面经资料',
      typeLabel: '知识库资料',
      meta: '尚未登记为题集',
      content: error instanceof Error ? error.message : '该资料暂时无法整理为真实面经题集。',
    }
    previewLoading.value = null
  }
}

async function start() {
  try {
    const selectedCount = composer.experienceDraft.value.questionSetId == null
      ? null
      : questionSetOptions.value.find((item) => item.value === composer.experienceDraft.value.questionSetId)?.itemCount ?? null
    const plan = await composer.start(selectedCount)
    emit('started', plan)
  } catch {
    // 错误已由 composer 状态记录并展示，保留草稿供重试
  }
}

function refreshSourcesOnFocus() {
  if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
  void loadOptions()
}

onMounted(() => {
  void loadOptions()
  window.addEventListener('focus', refreshSourcesOnFocus)
})
onBeforeUnmount(() => window.removeEventListener('focus', refreshSourcesOnFocus))
</script>

<style scoped>
.interview-composer{display:grid;gap:18px}
.composer-mode-missing{display:grid;gap:6px;padding:28px 0;color:var(--ink)}
.composer-mode-missing p{margin:0;color:var(--muted);font-size:12.5px}
.composer-progress{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0;position:relative;padding:0 10px 2px}
.composer-progress::before{content:'';position:absolute;top:13px;left:11%;right:11%;height:1px;background:var(--border-subtle);z-index:0}
.composer-progress-step{display:grid;justify-items:center;gap:3px;position:relative;z-index:1;color:var(--muted);text-align:center}
.composer-progress-node{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-subtle);border-radius:50%;background:var(--canvas,#fff);font-size:11px;font-weight:650}
.composer-progress-step.current,.composer-progress-step.completed{color:var(--ink)}.composer-progress-step.current .composer-progress-node,.composer-progress-step.completed .composer-progress-node{border-color:var(--ink);background:var(--ink);color:#fff}.composer-progress-step.completed .composer-progress-node{font-size:10px}
.composer-progress-step strong{font-size:12.5px;font-weight:650}.composer-progress-step small{font-size:10.5px;line-height:1.35}
.composer-body{display:grid;gap:14px;border-top:1px solid var(--border-subtle);background:transparent;padding:18px 0 0}
.composer-hint{margin:0;color:var(--muted);font-size:12.5px}
.composer-error{margin:0;color:var(--danger);font-size:12.5px}
.composer-ai-config-required{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:13px 14px;border:1px solid var(--border-default);border-left:3px solid var(--ink);background:var(--bg-surface);color:var(--ink)}
.composer-ai-config-required strong{font-size:13px;font-weight:700}.composer-ai-config-required p{margin:4px 0 0;color:var(--muted);font-size:12px;line-height:1.5}.composer-ai-config-required button{flex:0 0 auto;border:1px solid #111;border-radius:7px;background:#111;color:#fff;padding:8px 14px;font-size:12px;font-weight:650;cursor:pointer}.composer-ai-config-required button:hover{background:#333}
.composer-footer{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;border-top:1px solid var(--border-subtle);padding-top:18px}
.composer-footer-copy{display:grid;gap:4px;min-width:0}.composer-footer-copy strong{color:var(--ink);font-size:12.5px}.composer-footer-copy small{color:var(--muted);font-size:11px;line-height:1.5}
.composer-start{min-width:164px;border:1px solid #111;background:#111;color:#fff;padding:10px 18px;font-size:12.5px;font-weight:650;cursor:pointer;transition:background .18s,transform .18s}.composer-start:hover:not(:disabled){background:#292929;transform:translateY(-1px)}
.composer-start:disabled{opacity:.5;cursor:default}
@media (max-width:720px){.composer-progress{padding-inline:0}.composer-progress::before{left:8%;right:8%}.composer-footer{display:grid}.composer-start{width:100%}}
</style>
