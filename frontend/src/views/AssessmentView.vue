<template>
  <div class="page assessment-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">诊断—匹配—建议</p>
        <h1>简历诊断工作台</h1>
        <p class="page-desc">
          围绕一个简历版本完成评分、岗位匹配和 AI 修改建议。页面默认隐藏接口 ID、指纹等调试信息。
        </p>
      </div>
      <el-button @click="$router.push({ name: 'resumes' })">返回简历版本</el-button>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      @close="errorMessage = ''"
    />

    <el-card shadow="never" class="workflow-card">
      <div class="workflow-steps">
        <div class="workflow-step done">
          <strong>1</strong>
          <span>选择简历版本</span>
        </div>
        <div class="workflow-step" :class="{ done: assessment }">
          <strong>2</strong>
          <span>简历评分</span>
        </div>
        <div class="workflow-step" :class="{ done: jobMatch }">
          <strong>3</strong>
          <span>岗位匹配</span>
        </div>
        <div class="workflow-step" :class="{ done: suggestions.length > 0 }">
          <strong>4</strong>
          <span>AI 建议</span>
        </div>
      </div>
    </el-card>

    <div class="assessment-grid">
      <section class="assessment-main">
        <el-card shadow="never" class="resume-summary-card">
          <template #header>
            <div class="card-header">
              <span>当前简历版本</span>
              <el-tag v-if="version">v{{ version.versionNo }}</el-tag>
            </div>
          </template>

          <div v-if="versionLoading" class="loading-box">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
            <p>读取版本中...</p>
          </div>

          <el-empty v-else-if="!version" description="简历版本不存在" />

          <template v-else>
            <div class="info-grid">
              <div>
                <span>姓名</span>
                <strong>{{ content?.basicInfo?.name || '-' }}</strong>
              </div>
              <div>
                <span>目标岗位</span>
                <strong>{{ content?.basicInfo?.targetRole || '-' }}</strong>
              </div>
            </div>

            <div class="tag-list resume-skill-row">
              <el-tag
                v-for="skill in content?.skills || []"
                :key="skill"
                effect="plain"
              >
                {{ skill }}
              </el-tag>
              <span v-if="!content?.skills?.length" class="muted">暂无技能标签</span>
            </div>
          </template>
        </el-card>

        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="card-header">
              <span>简历评分</span>
              <el-button
                type="primary"
                :disabled="!version"
                :loading="assessmentLoading"
                @click="handleAssess"
              >
                生成评分
              </el-button>
            </div>
          </template>

          <el-empty v-if="!assessment" description="点击生成评分后展示诊断结果" />
          <template v-else>
            <div class="score-overview">
              <el-progress type="dashboard" :percentage="Number(assessment.totalScore)" :width="136" />
              <div>
                <h2>{{ assessment.totalScore }}</h2>
                <p>综合简历质量分</p>
              </div>
            </div>

            <div class="dimension-list">
              <div v-for="item in dimensionItems" :key="item.key" class="dimension-item">
                <span>{{ item.label }}</span>
                <el-progress :percentage="item.percentage" :show-text="false" :stroke-width="9" />
                <strong>{{ item.score }}/{{ item.max }}</strong>
              </div>
            </div>

            <el-divider />
            <h3>需要改进的地方</h3>
            <div v-if="assessment.deductions.length === 0" class="success-note">
              暂无扣分项，当前版本满足 S1 评分规则。
            </div>
            <div v-else class="issue-list">
              <div
                v-for="(deduction, index) in assessment.deductions"
                :key="`${deduction.code}-${index}`"
                class="issue-item"
              >
                <el-tag type="warning" effect="plain">扣 {{ deduction.points }} 分</el-tag>
                <div>
                  <strong>{{ deduction.reason }}</strong>
                  <p v-if="deduction.suggestion" class="muted">{{ deduction.suggestion }}</p>
                </div>
              </div>
            </div>
          </template>
        </el-card>
      </section>

      <aside class="assessment-side">
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="card-header">
              <span>岗位匹配</span>
              <el-button
                type="success"
                :disabled="!version || !selectedJobId"
                :loading="matchLoading"
                @click="handleMatch"
              >
                生成匹配
              </el-button>
            </div>
          </template>

          <el-select
            v-model="selectedJobId"
            placeholder="选择目标岗位"
            class="wide-select"
            :loading="jobLoading"
            clearable
            @change="handleSelectedJobChange"
          >
            <el-option
              v-for="job in jobs"
              :key="job.id"
              :label="job.companyName ? `${job.jobTitle}｜${job.companyName}` : job.jobTitle"
              :value="job.id"
            />
          </el-select>

          <el-alert
            v-if="selectedJob && selectedJob.parseStatus !== 'succeeded'"
            title="当前 JD 尚未完成结构化解析，请先进入岗位详情页解析。"
            type="warning"
            show-icon
            class="inline-alert"
          />

          <el-empty v-if="!jobMatch" description="选择岗位后生成匹配结果" />
          <template v-else>
            <div class="match-score-card">
              <strong>{{ jobMatch.matchScore }}</strong>
              <span>岗位匹配度</span>
              <el-tag v-if="jobMatch.details.matchLevel" type="success">
                {{ jobMatch.details.matchLevel }}
              </el-tag>
            </div>

            <div class="coverage-grid">
              <div>
                <span>必备项</span>
                <strong>{{ normalizedPercent(jobMatch.details.requiredCoverage) }}%</strong>
              </div>
              <div>
                <span>加分项</span>
                <strong>{{ normalizedPercent(jobMatch.details.preferredCoverage) }}%</strong>
              </div>
              <div>
                <span>经验</span>
                <strong>{{ normalizedPercent(jobMatch.details.experienceCoverage) }}%</strong>
              </div>
            </div>

            <el-divider />
            <h3>岗位缺口</h3>
            <div class="tag-list compact">
              <el-tag
                v-for="item in jobMatch.details.missingItems || []"
                :key="item"
                type="danger"
                effect="plain"
              >
                {{ item }}
              </el-tag>
              <span v-if="!jobMatch.details.missingItems?.length" class="muted">暂无明显缺口</span>
            </div>
          </template>
        </el-card>

        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="card-header">
              <span>AI 修改建议</span>
              <el-button
                type="primary"
                plain
                :disabled="!canGenerateSuggestions"
                :loading="suggestionLoading"
                @click="handleGenerateSuggestions"
              >
                生成建议
              </el-button>
            </div>
          </template>

          <el-empty v-if="suggestions.length === 0" description="完成岗位匹配后生成 AI 建议" />
          <div v-else class="suggestion-list">
            <div v-for="suggestion in suggestions" :key="suggestion.id" class="suggestion-item">
              <div class="suggestion-head">
                <strong>{{ suggestion.sectionKey }}</strong>
                <el-tag :type="suggestionTagType(suggestion.status)">
                  {{ suggestionStatusLabel(suggestion.status) }}
                </el-tag>
              </div>
              <el-alert
                v-if="suggestion.status === 'high_risk'"
                title="高风险：可能包含证据中不存在的事实。建议仔细核实后采纳，或选择忽略。"
                type="error"
                show-icon
                class="inline-alert"
              />
              <p class="muted">目标要求：{{ suggestion.targetRequirement }}</p>
              <p class="muted">原因：{{ suggestion.reasonText }}</p>
              <el-alert
                v-if="!suggestion.suggestedText"
                title="缺少事实证据，AI 未生成改写文本。"
                type="warning"
                show-icon
              />
              <template v-else>
                <p class="suggestion-label">建议表达</p>
                <p class="suggested-text">{{ suggestion.suggestedText }}</p>
              </template>
              <div class="suggestion-actions">
                <el-button size="small" :disabled="suggestion.status !== 'pending'" @click="handleAccept(suggestion.id)">
                  采纳
                </el-button>
                <el-button size="small" :disabled="!canReject(suggestion.status)" @click="handleReject(suggestion.id)">
                  忽略
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { listJobDescriptions } from '../api/job'
import { createJobMatch } from '../api/match'
import {
  acceptSuggestion,
  generateSuggestions,
  getSuggestions,
  rejectSuggestion,
} from '../api/optimization'
import { assessResumeVersion, getResumeVersion, getResumeVersions } from '../api/resume'
import type { JobDescription } from '../types/job'
import type { JobMatch } from '../types/match'
import type { OptimizationSuggestion } from '../types/optimization'
import type { ResumeAssessment, ResumeContent, ResumeVersion } from '../types/resume'
import { getWorkspaceSelectedJobId, setWorkspaceSelectedJobId } from '../utils/workspaceContext'

const route = useRoute()
const router = useRouter()
const versionId = computed(() => Number(route.params.versionId))

const errorMessage = ref('')
const version = ref<ResumeVersion | null>(null)
const versionLoading = ref(false)
const assessment = ref<ResumeAssessment | null>(null)
const assessmentLoading = ref(false)
const jobs = ref<JobDescription[]>([])
const jobLoading = ref(false)
const selectedJobId = ref<number | null>(null)
const jobMatch = ref<JobMatch | null>(null)
const matchLoading = ref(false)
const suggestions = ref<OptimizationSuggestion[]>([])
const suggestionLoading = ref(false)

const content = computed<ResumeContent | null>(() => version.value?.content ?? null)
const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value) ?? null)
const canGenerateSuggestions = computed(() => Boolean(jobMatch.value?.id))
const dimensionItems = computed(() => {
  const scores = assessment.value?.dimensionScores ?? {}
  const config = [
    { key: 'completeness', label: '完整度', max: 15 },
    { key: 'evidenceSupport', label: '证据支撑', max: 20 },
    { key: 'experienceQuality', label: '经历质量', max: 30 },
    { key: 'quantitativeExpression', label: '量化表达', max: 20 },
    { key: 'readability', label: '可读性', max: 15 },
  ]
  return config.map((item) => {
    const score = Number(scores[item.key] ?? 0)
    return {
      ...item,
      score,
      percentage: item.max === 0 ? 0 : Math.round((score / item.max) * 100),
    }
  })
})

onMounted(() => {
  loadVersion()
  loadJobs()
})

async function loadVersion() {
  versionLoading.value = true
  errorMessage.value = ''
  try {
    const res = await getResumeVersion(versionId.value)
    version.value = res.data
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '读取简历版本失败'
  } finally {
    versionLoading.value = false
  }
}

async function loadJobs() {
  jobLoading.value = true
  try {
    const res = await listJobDescriptions()
    jobs.value = res.data
    const queryJobId = Number(route.query.jobId)
    const storedJobId = getWorkspaceSelectedJobId()
    const queryJob = jobs.value.find((job) => job.id === queryJobId)
    const storedJob = jobs.value.find((job) => job.id === storedJobId)
    const parsed = jobs.value.find((job) => job.parseStatus === 'succeeded')
    selectedJobId.value = queryJob?.id ?? storedJob?.id ?? parsed?.id ?? jobs.value[0]?.id ?? null
    if (selectedJobId.value) {
      setWorkspaceSelectedJobId(selectedJobId.value)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载岗位 JD 失败'
  } finally {
    jobLoading.value = false
  }
}

async function handleAssess() {
  if (!version.value) return
  assessmentLoading.value = true
  errorMessage.value = ''
  try {
    const res = await assessResumeVersion(version.value.id)
    assessment.value = res.data
    ElMessage.success('简历评分已生成')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成评分失败'
  } finally {
    assessmentLoading.value = false
  }
}

async function handleMatch() {
  if (!version.value || !selectedJobId.value) return
  setWorkspaceSelectedJobId(selectedJobId.value)
  matchLoading.value = true
  errorMessage.value = ''
  suggestions.value = []
  try {
    jobMatch.value = await createJobMatch({
      resumeVersionId: version.value.id,
      jobDescriptionId: selectedJobId.value,
    })
    ElMessage.success('岗位匹配已生成')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成岗位匹配失败'
  } finally {
    matchLoading.value = false
  }
}

function handleSelectedJobChange(value: number | null) {
  if (value) {
    setWorkspaceSelectedJobId(value)
  }
}

async function handleGenerateSuggestions() {
  if (!jobMatch.value?.id) return
  suggestionLoading.value = true
  errorMessage.value = ''
  try {
    const res = await generateSuggestions(jobMatch.value.id)
    suggestions.value = res.data.suggestions
    ElMessage.success('AI 建议已生成')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成 AI 建议失败'
  } finally {
    suggestionLoading.value = false
  }
}

async function handleAccept(id: number) {
  try {
    await acceptSuggestion(id)
    await refreshSuggestions()
    await goToLatestVersion()
    ElMessage.success('已生成新简历版本，并切换到最新版本')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '采纳建议失败'
  }
}

async function handleReject(id: number) {
  try {
    await rejectSuggestion(id)
    ElMessage.success('建议已忽略')
    await refreshSuggestions()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '忽略建议失败'
  }
}

function canReject(status: string) {
  return status === 'pending' || status === 'high_risk'
}

async function refreshSuggestions() {
  if (!jobMatch.value?.id) return
  const res = await getSuggestions(jobMatch.value.id)
  suggestions.value = res.data.suggestions
}

async function goToLatestVersion() {
  if (!version.value?.resumeId) {
    await loadVersion()
    return
  }
  const res = await getResumeVersions(version.value.resumeId)
  const latest = res.data[0]
  if (!latest) {
    await loadVersion()
    return
  }
  version.value = latest
  assessment.value = null
  jobMatch.value = null
  suggestions.value = []
  await router.replace({
    name: 'resume-assessment',
    params: { versionId: latest.id },
    query: selectedJobId.value ? { jobId: selectedJobId.value } : undefined,
  })
}

function normalizedPercent(value?: number) {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.round(value)
}

function suggestionTagType(status: string) {
  if (status === 'pending') return 'success'
  if (status === 'accepted') return 'primary'
  if (status === 'rejected') return 'info'
  if (status === 'evidence_required') return 'warning'
  if (status === 'high_risk') return 'danger'
  return 'info'
}

function suggestionStatusLabel(status: string) {
  if (status === 'pending') return '待处理'
  if (status === 'accepted') return '已采纳'
  if (status === 'rejected') return '已忽略'
  if (status === 'evidence_required') return '需补证据'
  if (status === 'high_risk') return '高风险'
  return status
}
</script>
