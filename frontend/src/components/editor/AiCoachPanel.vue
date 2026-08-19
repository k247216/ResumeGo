<template>
  <aside class="ai-coach-panel">
    <section class="ai-card target-card">
      <template v-if="selectedJob">
        <div class="target-job-main">
          <CompanyAvatar :job="selectedJob" size="md" />
          <div>
            <h2>{{ selectedJob.jobTitle }}</h2>
            <p>{{ targetCompactMeta }}</p>
          </div>
          <div class="target-inline-actions">
            <button type="button" @click="$emit('view-job')">详情</button>
            <button type="button" @click="$emit('open-jobs')">更换</button>
          </div>
        </div>
        <CompanyProfileSignal
          v-if="hasCompanyProfile"
          :profile="companyProfile"
          variant="compact"
          label="Target Company"
        />
      </template>
      <template v-else>
        <div class="target-empty-row">
          <span>未选择目标岗位</span>
          <button type="button" @click="$emit('open-jobs')">选择</button>
        </div>
      </template>
    </section>

    <section class="ai-card">
      <div class="ai-card__head">
        <span>Match Snapshot</span>
        <strong>{{ matchScoreText || '待匹配' }}</strong>
      </div>
      <div class="assistant-input">
        <span>{{ assistantPromptText }}</span>
        <button type="button" :disabled="!canRunPrimaryAction || suggestionLoading" @click="handlePrimaryAction">
          {{ primaryActionText }}
        </button>
      </div>

      <div v-if="matchScoreText" class="match-mini-card">
        <div>
          <span>当前匹配度</span>
          <small>{{ selectedJob?.companyName || selectedJob?.jobTitle }}</small>
        </div>
        <strong>{{ matchScoreText }}</strong>
      </div>

    </section>

    <section class="ai-card suggestion-card">
      <div class="ai-card__head">
        <span>Ability Coach</span>
        <strong>{{ suggestions.length }} 条</strong>
      </div>

      <el-empty
        v-if="suggestions.length === 0"
        description="选择岗位后生成薄弱点与训练计划"
        :image-size="72"
      />

      <template v-else>
        <div class="advice-section-list">
          <section
            v-for="group in groupedAdviceSections"
            :key="group.sectionId"
            class="advice-section"
          >
            <header>
              <div>
                <span>{{ group.eyebrow }}</span>
                <strong>{{ group.title }}</strong>
              </div>
              <button type="button" @click="focusSuggestionSection(group.sectionId)">
                前往模块
              </button>
            </header>

            <article
              v-for="suggestion in group.suggestions"
              :key="suggestion.id"
              class="advice-card"
              :class="{ followup: isFollowUpSuggestion(suggestion) }"
            >
              <div class="advice-card__top">
                <span>{{ statusLabel(suggestion.status) }}</span>
                <em>{{ suggestion.targetRequirement }}</em>
              </div>
              <div v-if="hasCompanyProfile" class="advice-profile-badge">
                基于 {{ companyProfile?.companyName || selectedJob?.companyName }} 偏好：{{ companyProfileFocusText }}
              </div>

              <p v-if="suggestion.originalText" class="advice-observation">
                {{ suggestion.originalText }}
              </p>

              <div class="weakness-block">
                <small>薄弱点</small>
                <p>{{ weaknessText(suggestion) }}</p>
              </div>

              <div class="advice-block">
                <small>{{ suggestion.suggestedText ? '行动建议' : '追问补证据' }}</small>
                <p>{{ suggestion.suggestedText || followUpQuestion(suggestion) }}</p>
              </div>

              <ol v-if="suggestion.suggestedText" class="training-plan">
                <li
                  v-for="step in trainingPlanForSuggestion(suggestion)"
                  :key="step"
                >
                  {{ step }}
                </li>
              </ol>

              <div
                v-if="isFollowUpSuggestion(suggestion)"
                class="supplement-box"
              >
                <label>
                  <span>补充事实</span>
                  <textarea
                    :value="supplementDrafts[suggestion.id] || ''"
                    rows="3"
                    placeholder="例如：项目背景、你的职责、技术动作、遇到的难点、指标结果、是否真实使用过相关技术..."
                    @input="updateSupplementDraft(suggestion.id, ($event.target as HTMLTextAreaElement).value)"
                  ></textarea>
                </label>
                <button
                  type="button"
                  :disabled="isSupplementLoading(suggestion.id)"
                  @click="submitSupplement(suggestion)"
                >
                  {{ isSupplementLoading(suggestion.id) ? '生成中...' : '生成最终建议' }}
                </button>
                <div v-if="supplements[suggestion.id]" class="supplement-result">
                  <span>已补充</span>
                  <p>{{ supplements[suggestion.id] }}</p>
                  <small>补充事实不会自动写入简历，只用于生成更聚焦的人工修改建议。</small>
                </div>
                <div v-if="finalAdviceById[suggestion.id]" class="supplement-final">
                  <span>最终建议与训练计划</span>
                  <p>{{ finalAdviceById[suggestion.id].finalAdvice }}</p>
                  <ul v-if="finalAdviceById[suggestion.id].nextSteps?.length">
                    <li
                      v-for="step in finalAdviceById[suggestion.id].nextSteps"
                      :key="step"
                    >
                      {{ step }}
                    </li>
                  </ul>
                </div>
                <p v-if="supplementErrorById[suggestion.id]" class="supplement-error">
                  {{ supplementErrorById[suggestion.id] }}
                </p>
              </div>
            </article>
          </section>
        </div>
      </template>
    </section>

  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { generateSuggestionFollowUp } from '../../api/optimization'
import CompanyAvatar from '../CompanyAvatar.vue'
import CompanyProfileSignal from '../CompanyProfileSignal.vue'
import type { EditorSection } from '../../types/editor'
import type { CompanyProfile, JobDescription } from '../../types/job'
import type { JobMatch } from '../../types/match'
import type { OptimizationSuggestion, SuggestionFollowUpResponse } from '../../types/optimization'
import type { ResumeVersion } from '../../types/resume'

const props = defineProps<{
  version: ResumeVersion | null
  selectedJob: JobDescription | null
  sections: EditorSection[]
  selectedSection: EditorSection | null
  suggestions: OptimizationSuggestion[]
  jobMatch: JobMatch | null
  suggestionLoading: boolean
  companyProfile: CompanyProfile | null
}>()

const emit = defineEmits<{
  (event: 'open-jobs'): void
  (event: 'view-job'): void
  (event: 'select-section', sectionId: string): void
  (event: 'generate-suggestions'): void
}>()

const supplementDrafts = ref<Record<number, string>>({})
const supplements = ref<Record<number, string>>({})
const supplementLoadingIds = ref<Set<number>>(new Set())
const finalAdviceById = ref<Record<number, SuggestionFollowUpResponse>>({})
const supplementErrorById = ref<Record<number, string>>({})

const isJobParsed = computed(() => props.selectedJob?.parseStatus === 'succeeded')
const profileTags = computed(() => [
  ...(props.companyProfile?.preferenceTags ?? []),
  ...(props.companyProfile?.interviewFocus ?? []),
].filter(Boolean))
const hasCompanyProfile = computed(() => Boolean(props.companyProfile?.companyName && profileTags.value.length))
const companyProfileFocusText = computed(() => profileTags.value.slice(0, 2).join(' / '))
const matchScoreText = computed(() => {
  if (!props.jobMatch) return ''
  return `${Math.round(Number(props.jobMatch.matchScore || 0))}%`
})

const targetCompactMeta = computed(() => {
  if (!props.selectedJob) return ''
  const meta = props.selectedJob.sourceMeta
  return [
    props.selectedJob.companyName || '未知公司',
    meta?.base,
    meta?.industry,
    meta?.salary,
  ].filter(Boolean).slice(0, 4).join(' · ')
})

const assistantPromptText = computed(() => {
  if (!props.version) return '保存简历后可生成岗位建议'
  if (!props.selectedJob) return '选择目标岗位后开始匹配'
  if (!isJobParsed.value) return '解析 JD 后可生成建议'
  return '基于当前简历与目标岗位生成建议'
})

const primaryActionText = computed(() => {
  if (props.suggestionLoading) return '生成中...'
  if (!props.version) return '先保存版本'
  if (!props.selectedJob) return '选择目标岗位'
  if (!isJobParsed.value) return '去解析 JD'
  return '生成技术建议'
})

const canRunPrimaryAction = computed(() => {
  if (props.suggestionLoading) return false
  if (!props.version) return false
  return true
})

const groupedAdviceSections = computed(() => {
  const groups = props.sections
    .map((section) => {
      const sectionSuggestions = props.suggestions.filter((suggestion) => matchesSection(suggestion.sectionKey, section.id))
      return {
        sectionId: section.id,
        title: section.title,
        eyebrow: section.subtitle || section.type,
        suggestions: sectionSuggestions,
      }
    })
    .filter((group) => group.suggestions.length > 0)

  const groupedIds = new Set(groups.flatMap((group) => group.suggestions.map((suggestion) => suggestion.id)))
  const others = props.suggestions.filter((suggestion) => !groupedIds.has(suggestion.id))
  if (others.length) {
    groups.push({
      sectionId: props.selectedSection?.id ?? 'projects',
      title: '其他建议',
      eyebrow: 'Unmapped Advice',
      suggestions: others,
    })
  }
  return groups
})

function matchesSection(sectionKey: string, sectionId: string) {
  const normalized = sectionKey.toLowerCase()
  if (sectionId === 'personal-info') return normalized.includes('basic') || normalized.includes('personal')
  if (sectionId === 'education') return normalized.includes('education')
  if (sectionId === 'work-experience') return normalized.includes('work') || normalized.includes('experience') || normalized.includes('intern')
  if (sectionId === 'projects') return normalized.includes('project')
  if (sectionId === 'skills') return normalized.includes('skill')
  return false
}

function statusLabel(status: string) {
  if (status === 'pending') return '建议'
  if (status === 'accepted') return '已记录'
  if (status === 'applied_to_draft') return '已参考'
  if (status === 'rejected') return '已跳过'
  if (status === 'evidence_required') return '需补证据'
  if (status === 'high_risk') return '需核实'
  return status
}

function isFollowUpSuggestion(suggestion: OptimizationSuggestion) {
  return suggestion.status === 'evidence_required' || suggestion.status === 'high_risk' || !suggestion.suggestedText
}

function followUpQuestion(suggestion: OptimizationSuggestion) {
  if (suggestion.reasonText) return suggestion.reasonText
  return '请补充这个项目的技术背景、你负责的具体动作、遇到的难点、使用的技术栈、可验证结果或指标。'
}

function weaknessText(suggestion: OptimizationSuggestion) {
  const reason = suggestion.reasonText || ''
  const match = reason.match(/薄弱点[:：]\s*([^；;。]+)/)
  if (match?.[1]) return match[1].trim()
  if (suggestion.status === 'evidence_required') return '缺少可验证事实，暂时无法形成可信表达建议'
  if (suggestion.status === 'high_risk') return '建议中可能包含未被证据支持的信息，需要先核实'
  return reason || '当前模块与目标岗位的能力表达还不够具体'
}

function trainingPlanForSuggestion(suggestion: OptimizationSuggestion) {
  const section = suggestion.sectionKey.toLowerCase()
  if (section.includes('skill')) {
    return [
      'Day 1：列出该技能在项目中的真实使用场景',
      'Day 2：补充一次问题定位或技术取舍过程',
      'Day 3：用 60 秒讲清“我如何使用这项技术解决问题”',
    ]
  }
  if (section.includes('work') || section.includes('experience') || section.includes('intern')) {
    return [
      'Day 1：按“职责—动作—结果”重写一条经历',
      'Day 2：补充团队规模、业务影响或效率指标',
      'Day 3：练习回答“这段经历中你独立负责了什么”',
    ]
  }
  return [
    'Day 1：用 STAR 法拆解一个项目经历',
    'Day 2：补充技术难点、方案选择和可验证结果',
    'Day 3：模拟回答“这个项目最大的技术挑战是什么”',
  ]
}

function focusSuggestionSection(sectionId: string) {
  emit('select-section', sectionId)
}

function updateSupplementDraft(suggestionId: number, value: string) {
  supplementDrafts.value = {
    ...supplementDrafts.value,
    [suggestionId]: value,
  }
}

function isSupplementLoading(suggestionId: number) {
  return supplementLoadingIds.value.has(suggestionId)
}

async function submitSupplement(suggestion: OptimizationSuggestion) {
  const value = (supplementDrafts.value[suggestion.id] || '').trim()
  if (!value) {
    supplementErrorById.value = {
      ...supplementErrorById.value,
      [suggestion.id]: '请先补充真实项目事实，再生成最终建议。',
    }
    return
  }

  supplements.value = {
    ...supplements.value,
    [suggestion.id]: value,
  }
  supplementErrorById.value = {
    ...supplementErrorById.value,
    [suggestion.id]: '',
  }
  const nextLoadingIds = new Set(supplementLoadingIds.value)
  nextLoadingIds.add(suggestion.id)
  supplementLoadingIds.value = nextLoadingIds

  try {
    const response = await generateSuggestionFollowUp(suggestion.id, {
      userSupplement: value,
    })
    finalAdviceById.value = {
      ...finalAdviceById.value,
      [suggestion.id]: response.data,
    }
  } catch (error) {
    supplementErrorById.value = {
      ...supplementErrorById.value,
      [suggestion.id]: error instanceof Error ? error.message : '生成最终建议失败，请稍后重试。',
    }
  } finally {
    const restLoadingIds = new Set(supplementLoadingIds.value)
    restLoadingIds.delete(suggestion.id)
    supplementLoadingIds.value = restLoadingIds
  }
}

function handlePrimaryAction() {
  if (!props.version) return
  if (!props.selectedJob) {
    emitOpenJobs()
    return
  }
  if (!isJobParsed.value) {
    emitViewJob()
    return
  }
  emitGenerateSuggestions()
}

function emitOpenJobs() {
  emit('open-jobs')
}

function emitViewJob() {
  emit('view-job')
}

function emitGenerateSuggestions() {
  emit('generate-suggestions')
}

</script>

<style scoped>
.ai-coach-panel {
  width: 100%;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.9);
  padding: 18px;
}

.ai-card {
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 42px rgba(15, 23, 42, 0.04);
  padding: 16px;
  margin-bottom: 14px;
}

.ai-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ai-card__head span {
  color: #7c8aa2;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.ai-card__head strong {
  border-radius: 999px;
  background: #e9fff5;
  color: #07875f;
  font-size: 12px;
  padding: 4px 8px;
}

.ai-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.ai-card__head button {
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #087a57;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 5px 8px;
}

.ai-card__head button:hover {
  background: #ecfdf5;
}

.target-job-main {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.target-job-main > div {
  min-width: 0;
}

.target-card h2,
.target-card p {
  margin: 0;
}

.target-card h2 {
  overflow: hidden;
  color: #101a33;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-card p {
  overflow: hidden;
  margin-top: 2px;
  color: #7c8aa2;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-inline-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.target-inline-actions button,
.target-empty-row button {
  border: 0;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 6px 8px;
}

.target-inline-actions button:hover,
.target-empty-row button:hover {
  background: #e2e8f0;
  color: #334155;
}

.target-empty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.target-empty-row span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.assistant-input {
  border: 1px solid #dfe6f1;
  border-radius: 16px;
  background: #f8fafc;
  padding: 12px;
  display: grid;
  gap: 12px;
}

.selected-section-card {
  border: 1px solid #d8efe6;
  border-radius: 16px;
  background: linear-gradient(135deg, #f5fffb, #ffffff);
  padding: 12px;
  margin-bottom: 10px;
}

.selected-section-card span {
  display: block;
  color: #0f9f73;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.selected-section-card strong {
  display: block;
  margin-top: 5px;
  color: #101a33;
}

.selected-section-card p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.assistant-input span {
  color: #94a3b8;
  font-size: 13px;
}

.assistant-input button,
.diagnosis-stack button {
  border: 0;
  border-radius: 999px;
  background: #101a33;
  color: #fff;
  cursor: pointer;
  font-weight: 800;
  padding: 9px 12px;
}

.assistant-input button:disabled,
.diagnosis-stack button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.assistant-note {
  color: #7c8aa2;
  font-size: 12px;
  line-height: 1.7;
  margin: 10px 0 0;
}

.match-mini-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border: 1px solid #d8efe6;
  border-radius: 14px;
  background:
    radial-gradient(circle at 0 0, rgba(16, 185, 129, 0.12), transparent 44%),
    #f7fffb;
  padding: 10px 12px;
  margin-top: 10px;
}

.match-mini-card div {
  min-width: 0;
}

.match-mini-card span,
.match-mini-card small {
  display: block;
  overflow: hidden;
  color: #7c8aa2;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-mini-card strong {
  color: #08956a;
  font-size: 20px;
  font-weight: 950;
}

.advice-section-list {
  display: grid;
  gap: 18px;
}

.advice-section {
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  padding: 14px;
}

.advice-section > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.advice-section > header div {
  min-width: 0;
}

.advice-section > header span,
.advice-card__top span,
.advice-block small,
.weakness-block small,
.supplement-box label span,
.supplement-result span,
.supplement-final span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.advice-section > header strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #101a33;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.advice-section > header button,
.supplement-box > button {
  flex: 0 0 auto;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 7px 10px;
}

.advice-card {
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #f8fafc;
  padding: 14px;
}

.advice-card + .advice-card {
  margin-top: 14px;
}

.advice-card.followup {
  background: #fbfbfc;
}

.advice-card__top {
  display: grid;
  gap: 6px;
}

.advice-card__top em {
  color: #475569;
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
  line-height: 1.5;
}

.advice-profile-badge {
  display: inline-flex;
  width: fit-content;
  margin-top: 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 850;
  line-height: 1.4;
  padding: 5px 9px;
}

.advice-observation {
  border-radius: 12px;
  background: #ffffff;
  color: #64748b;
  font-size: 12px;
  line-height: 1.65;
  margin: 12px 0 0;
  padding: 10px 11px;
}

.advice-block {
  border-radius: 13px;
  background: #ffffff;
  margin-top: 12px;
  padding: 11px;
}

.weakness-block {
  border: 1px solid #f2d8b8;
  border-radius: 13px;
  background: linear-gradient(180deg, #fffaf2, #ffffff);
  margin-top: 12px;
  padding: 11px;
}

.weakness-block small {
  color: #d97706;
}

.weakness-block p {
  color: #78350f;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.65;
  margin: 5px 0 0;
}

.advice-block p,
.advice-reason,
.supplement-result p,
.supplement-result small,
.supplement-final p,
.supplement-final li,
.supplement-error {
  color: #26344d;
  font-size: 12px;
  line-height: 1.7;
  margin: 5px 0 0;
}

.advice-reason {
  color: #64748b;
}

.training-plan {
  display: grid;
  gap: 6px;
  margin: 10px 0 0;
  padding: 10px 10px 10px 28px;
  border-radius: 13px;
  background: #eefcf6;
}

.training-plan li {
  color: #0f766e;
  font-size: 12px;
  line-height: 1.55;
}

.supplement-box {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  border-top: 1px dashed #dbe3ef;
  padding-top: 10px;
}

.supplement-box textarea {
  width: 100%;
  box-sizing: border-box;
  margin-top: 6px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #ffffff;
  color: #1f2937;
  font: inherit;
  font-size: 12px;
  line-height: 1.6;
  outline: none;
  padding: 9px 10px;
  resize: vertical;
}

.supplement-box textarea:focus {
  border-color: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.16);
}

.supplement-box > button {
  justify-self: end;
  background: #101a33;
  color: #ffffff;
}

.supplement-box > button:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.supplement-result {
  border-radius: 12px;
  background: #ffffff;
  padding: 10px;
}

.supplement-result small {
  display: block;
  color: #64748b;
}

.supplement-final {
  border: 1px solid #e5eaf2;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  padding: 11px;
}

.supplement-final p {
  color: #101a33;
  font-weight: 750;
}

.supplement-final ul {
  display: grid;
  gap: 5px;
  margin: 8px 0 0;
  padding-left: 18px;
}

.supplement-final li {
  color: #475569;
}

.supplement-error {
  color: #dc2626;
}

.diagnosis-stack {
  display: grid;
  gap: 10px;
}

.diagnosis-stack article {
  border-radius: 16px;
  padding: 13px;
  border-left: 3px solid #10b981;
  background: #f8fffc;
}

.diagnosis-stack article.blocked {
  border-left-color: #cbd5e1;
  background: #f8fafc;
}

.diagnosis-stack span {
  color: #10a878;
  font-size: 12px;
  font-weight: 900;
}

.diagnosis-stack article.blocked span {
  color: #94a3b8;
}

.diagnosis-stack h3,
.diagnosis-stack p {
  margin: 0;
}

.diagnosis-stack h3 {
  margin-top: 5px;
  color: #101a33;
  font-size: 14px;
}

.diagnosis-stack p {
  margin-top: 6px;
  color: #63718a;
  font-size: 12px;
  line-height: 1.6;
}

.diagnosis-stack button {
  margin-top: 10px;
  background: #10a878;
  font-size: 12px;
  padding: 7px 10px;
}

.safety-card {
  background: linear-gradient(135deg, #101a33, #153d33);
  color: #fff;
}

.safety-card strong {
  display: block;
  margin-bottom: 8px;
}

.safety-card p {
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
  line-height: 1.7;
  margin: 0;
}
</style>
