<template>
  <div class="job-detail-page">
    <header class="detail-topbar">
      <button type="button" class="ghost-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回岗位库
      </button>
      <div class="topbar-actions">
        <button
          v-if="job"
          type="button"
          class="target-action"
          :class="{ selected: isCurrentTargetJob }"
          @click="selectTargetJob"
        >
          {{ isCurrentTargetJob ? (fromEditor ? '当前目标，返回工作台' : '当前目标') : fromEditor ? '设为目标并返回' : '设为目标岗位' }}
        </button>
        <button
          v-if="job && (job.parseStatus === 'pending' || job.parseStatus === 'failed')"
          type="button"
          class="parse-action"
          :disabled="parsing"
          @click="handleParse"
        >
          {{ parsing ? '解析中...' : job.parseStatus === 'failed' ? '重新解析' : '解析 JD' }}
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-box">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <el-empty v-else-if="!job" description="岗位不存在" />

    <template v-else>
      <div class="hero-card">
        <div class="hero-left">
          <CompanyAvatar :job="job" size="lg" />
          <div class="hero-info">
            <h1 class="job-title">{{ job.jobTitle }}</h1>
            <p class="company-name">{{ job.companyName || '未填写公司' }}</p>
            <div class="hero-meta" aria-label="岗位基础信息">
              <span v-if="displayLocation">
                <el-icon><Location /></el-icon>
                {{ displayLocation }}
              </span>
              <span v-if="displaySalary">
                <el-icon><Money /></el-icon>
                {{ displaySalary }}
              </span>
              <span v-if="displayJobType">
                <el-icon><Briefcase /></el-icon>
                {{ displayJobType }}
              </span>
              <span v-if="job.sourceMeta?.platform">
                <el-icon><Monitor /></el-icon>
                {{ job.sourceMeta.platform }}
              </span>
              <span v-if="job.sourceMeta?.industry">
                {{ job.sourceMeta.industry }}
              </span>
              <span v-if="job.sourceMeta?.companySize">
                {{ job.sourceMeta.companySize }}
              </span>
            </div>
          </div>
        </div>
        <div class="hero-right">
          <span class="status-pill" :class="job.parseStatus">{{ statusLabel }}</span>
          <span class="update-time">更新于 {{ formatDateTime(job.updatedAt) }}</span>
        </div>
      </div>

      <div v-if="fromEditor" class="workspace-return-bar">
        <span>该 JD 会作为当前简历的目标岗位，用于评分、建议和面试上下文。</span>
        <button type="button" @click="returnToEditor()">返回工作台</button>
      </div>

      <CompanyProfileSignal
        v-if="companyProfile"
        class="detail-company-signal"
        :profile="companyProfile"
        variant="full"
        label="Company Signal"
      />

      <div v-if="job.parseStatus !== 'succeeded'" class="soft-alert" :class="job.parseStatus">
        <strong>{{ job.parseStatus === 'failed' ? '解析失败' : '尚未解析' }}</strong>
        <span>{{ job.parseStatus === 'failed' ? '可以重新解析，或先查看原始 JD。' : '解析后会提取技能、职责、经验和学历要求。' }}</span>
      </div>

      <section class="detail-layout">
        <article class="card-block structure-card">
          <div class="block-head">
            <div>
              <span>Structured JD</span>
              <h3>结构化岗位要求</h3>
            </div>
            <em>{{ statusLabel }}</em>
          </div>

          <div v-if="job.parsed?.requiredSkills?.length" class="skill-field">
            <h4>技能关键词</h4>
            <div class="skill-tag-list">
              <span
                v-for="(item, i) in job.parsed?.requiredSkills ?? []"
                :key="'required-' + i"
                class="required"
              >
                {{ item }}
              </span>
            </div>
          </div>

          <ParsedFields v-if="job.parseStatus === 'succeeded' && job.parsed" :parsed="job.parsed" />
          <el-empty v-else description="暂无结构化解析结果" :image-size="96" />
        </article>

        <aside class="side-stack">
          <article class="card-block compact-card">
            <div class="block-head compact">
              <div>
                <span>Meta</span>
                <h3>岗位来源</h3>
              </div>
            </div>
            <dl class="info-list">
              <div>
                <dt>公司</dt>
                <dd>{{ job.companyName || '未填写' }}</dd>
              </div>
              <div>
                <dt>城市</dt>
                <dd>{{ displayLocation || '未填写' }}</dd>
              </div>
              <div>
                <dt>薪资</dt>
                <dd>{{ displaySalary || '未填写' }}</dd>
              </div>
              <div>
                <dt>平台</dt>
                <dd>{{ job.sourceMeta?.platform || '未填写' }}</dd>
              </div>
            </dl>
          </article>

          <article class="card-block compact-card">
            <div class="block-head compact">
              <div>
                <span>Original JD</span>
                <h3>岗位原文</h3>
              </div>
            </div>
            <div class="raw-text-box">{{ job.rawText || '暂无原始 JD' }}</div>
          </article>
        </aside>
      </section>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        closable
        style="margin-top: 16px"
        @close="errorMessage = ''"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Loading,
  ArrowLeft,
  Location,
  Money,
  Briefcase,
  Monitor,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { parseJobDescription, getJobDescription, resolveCompanyProfile } from '../api/job'
import type { CompanyProfile, JobDescription } from '../types/job'
import CompanyAvatar from '../components/CompanyAvatar.vue'
import CompanyProfileSignal from '../components/CompanyProfileSignal.vue'
import ParsedFields from '../components/ParsedFields.vue'
import {
  cancelPendingWorkspaceAction,
  getWorkspaceSelectedJobId,
  markPendingWorkspaceSelectedJobId,
  markReturnToEditor,
  setWorkspaceSelectedJobId,
} from '../utils/workspaceContext'

const route = useRoute()
const router = useRouter()

const job = ref<JobDescription | null>(null)
const companyProfile = ref<CompanyProfile | null>(null)
const loading = ref(true)
const parsing = ref(false)
const errorMessage = ref('')
const fromEditor = computed(() => route.query.from === 'editor')
const selectedWorkspaceJobId = ref<number | null>(getWorkspaceSelectedJobId())

function readRouteString(key: string): string | undefined {
  const value = route.query[key]
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue ? rawValue : undefined
}

function editorContextQuery() {
  if (!fromEditor.value) return undefined
  const query: Record<string, string> = { from: 'editor' }
  const resumeId = readRouteString('resumeId')
  const versionId = readRouteString('versionId')
  const mode = readRouteString('mode')
  if (resumeId) query.resumeId = resumeId
  if (versionId) query.versionId = versionId
  if (mode) query.mode = mode
  return query
}

function editorReturnQuery() {
  const query: Record<string, string> = { editor: '1' }
  const resumeId = readRouteString('resumeId')
  const versionId = readRouteString('versionId')
  const mode = readRouteString('mode')
  if (resumeId) query.resumeId = resumeId
  if (versionId) query.versionId = versionId
  if (mode) query.mode = mode
  return query
}

// ---- 计算属性 ----

const displayLocation = computed(() => {
  return job.value?.sourceMeta?.base || ''
})

const displaySalary = computed(() => {
  const meta = job.value?.sourceMeta
  if (!meta) return ''
  if (meta.salary) return meta.salary
  if (meta.salaryMin != null && meta.salaryMax != null) {
    return `${meta.salaryMin}k - ${meta.salaryMax}k`
  }
  if (meta.salaryAvg != null) {
    return `${meta.salaryAvg}k`
  }
  return ''
})

const displayJobType = computed(() => {
  const type = job.value?.jobType
  if (type === 'internship') return '实习'
  if (type === 'campus') return '校招'
  if (type === 'social') return '社招'
  return job.value?.sourceMeta?.workType || type || ''
})

const statusLabel = computed(() => {
  switch (job.value?.parseStatus) {
    case 'succeeded': return '已解析'
    case 'failed': return '解析失败'
    default: return '待解析'
  }
})

const isCurrentTargetJob = computed(() => Boolean(job.value && job.value.id === selectedWorkspaceJobId.value))

// ---- 生命周期 ----

onMounted(() => {
  loadJob()
})

// ---- 方法 ----

async function loadJob() {
  loading.value = true
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  try {
    const res = await getJobDescription(id)
    job.value = res.data
    await loadCompanyProfile(res.data.companyName)
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载岗位详情失败'
  } finally {
    loading.value = false
  }
}

async function loadCompanyProfile(companyName?: string | null) {
  companyProfile.value = null
  if (!companyName) return
  try {
    const res = await resolveCompanyProfile(companyName)
    companyProfile.value = res.data?.companyName ? res.data : null
  } catch {
    companyProfile.value = null
  }
}

async function handleParse() {
  if (!job.value) return

  parsing.value = true
  errorMessage.value = ''
  try {
    const res = await parseJobDescription(job.value.id)
    job.value = res.data
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '解析失败'
  } finally {
    parsing.value = false
  }
}

function goBack() {
  router.push({
    name: 'jobs',
    query: editorContextQuery(),
  })
}

function returnToEditor(cancelPendingAction = true) {
  if (cancelPendingAction) {
    cancelPendingWorkspaceAction()
  }
  markReturnToEditor()
  router.push({ name: 'home', query: editorReturnQuery() })
}

function selectTargetJob() {
  if (!job.value) return
  selectedWorkspaceJobId.value = job.value.id
  setWorkspaceSelectedJobId(job.value.id)
  if (fromEditor.value) {
    markPendingWorkspaceSelectedJobId(job.value.id)
  }
  ElMessage.success(`已将「${job.value.jobTitle}」设为目标岗位`)
  if (fromEditor.value) {
    returnToEditor(false)
  }
}

function formatDateTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.job-detail-page {
  min-height: 100vh;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% 0, rgba(16, 168, 120, 0.08), transparent 28%),
    linear-gradient(180deg, #f7f9fc 0%, #eef2f7 100%);
  padding: 18px 28px 44px;
}

.detail-topbar {
  max-width: 1220px;
  margin: 0 auto 14px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.ghost-link,
.target-action,
.parse-action,
.workspace-return-bar button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 8px 13px;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.ghost-link:hover,
.target-action:hover,
.parse-action:hover,
.workspace-return-bar button:hover {
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.topbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.target-action {
  border-color: #101a33;
  background: #101a33;
  color: #ffffff;
}

.target-action.selected {
  border-color: #10a878;
  background: #10a878;
}

.parse-action {
  border-color: #cfe9df;
  color: #07875f;
}

.parse-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
}

.loading-box {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.hero-card,
.workspace-return-bar,
.detail-company-signal,
.soft-alert,
.detail-layout {
  max-width: 1220px;
  margin-right: auto;
  margin-left: auto;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 24px;
  border: 1px solid #e5eaf2;
  border-radius: 24px;
  background:
    radial-gradient(circle at 0 0, rgba(16, 168, 120, 0.12), transparent 36%),
    #ffffff;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.07);
  padding: 26px;
}

.hero-left {
  min-width: 0;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

.hero-info {
  min-width: 0;
}

.job-title,
.company-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-title {
  margin: 0;
  color: #101a33;
  font-size: 27px;
  font-weight: 950;
  letter-spacing: -0.03em;
  line-height: 1.18;
}

.company-name {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 750;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 230px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.9);
  color: #475569;
  font-size: 12px;
  font-weight: 850;
  padding: 7px 10px;
}

.hero-right {
  display: grid;
  justify-items: end;
  gap: 9px;
}

.status-pill {
  border-radius: 999px;
  font-size: 12px;
  font-weight: 950;
  padding: 7px 11px;
}

.status-pill.succeeded {
  background: #e9fff5;
  color: #047857;
}

.status-pill.pending {
  background: #eff6ff;
  color: #2563eb;
}

.status-pill.failed {
  background: #fff1f1;
  color: #dc2626;
}

.update-time {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

.workspace-return-bar,
.soft-alert {
  box-sizing: border-box;
  margin-top: 14px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.04);
  padding: 12px 14px;
}

.detail-company-signal {
  box-sizing: border-box;
  margin-top: 14px;
}

.workspace-return-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.workspace-return-bar span,
.soft-alert span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.workspace-return-bar button {
  flex: 0 0 auto;
  border-color: #d8efe6;
  color: #07875f;
}

.soft-alert {
  display: grid;
  gap: 4px;
}

.soft-alert strong {
  color: #101a33;
  font-size: 14px;
}

.soft-alert.failed strong {
  color: #dc2626;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.44fr);
  gap: 16px;
  margin-top: 16px;
}

.side-stack {
  display: grid;
  align-content: start;
  gap: 16px;
}

.card-block {
  min-width: 0;
  border: 1px solid #e5eaf2;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.055);
  padding: 18px;
}

.structure-card {
  padding: 20px;
}

.block-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.block-head.compact {
  margin-bottom: 12px;
}

.block-head span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.block-head h3 {
  margin: 4px 0 0;
  color: #101a33;
  font-size: 17px;
  font-weight: 950;
}

.block-head em {
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  padding: 6px 9px;
}

.skill-field {
  border-radius: 16px;
  background: #f8fafc;
  padding: 12px;
  margin-bottom: 14px;
}

.skill-field h4 {
  margin: 0 0 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 950;
}

.skill-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag-list span {
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #ffffff;
  color: #36567f;
  font-size: 12px;
  font-weight: 850;
  padding: 6px 9px;
}

.skill-tag-list span.required {
  border-color: #bcebdc;
  background: #ecfdf5;
  color: #047857;
}

.info-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.info-list div {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  border-radius: 13px;
  background: #f8fafc;
  padding: 10px;
}

.info-list dt,
.info-list dd {
  margin: 0;
  min-width: 0;
}

.info-list dt {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.info-list dd {
  overflow: hidden;
  color: #101a33;
  font-size: 13px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.raw-text-box {
  max-height: 360px;
  overflow: auto;
  border-radius: 16px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 1.8;
  padding: 13px;
  white-space: pre-wrap;
}

.raw-text-box::-webkit-scrollbar {
  width: 6px;
}

.raw-text-box::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #cbd5e1;
}

@media (max-width: 980px) {
  .job-detail-page {
    padding: 14px 14px 32px;
  }

  .hero-card,
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .hero-right {
    justify-items: start;
  }

  .detail-topbar,
  .workspace-return-bar {
    align-items: stretch;
    flex-direction: column;
    height: auto;
  }

  .topbar-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
