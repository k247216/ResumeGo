<template>
  <section class="targets-page">
    <PageHeader eyebrow="求职目标" title="求职目标" subtitle="管理正在进行的求职准备，关联简历与目标岗位。">
      <template #actions>
        <button class="btn-primary" type="button" data-test="create-target" @click="openCreate">新建求职目标</button>
      </template>
    </PageHeader>

    <div class="targets-body">
      <aside class="target-rail">
        <div class="rail-head">
          <span>{{ store.targets.length }} 个目标</span>
          <span v-if="store.errorMessage" class="rail-error" role="alert">{{ store.errorMessage }}</span>
        </div>

        <div v-if="store.loading && !store.targets.length" class="rail-message">正在读取本地目标…</div>
        <template v-else>
          <button
            v-for="target in store.targets"
            :key="target.id"
            type="button"
            class="target-row"
            :class="{ selected: target.id === selectedTargetId, archived: target.status === 'archived' }"
            :data-test="`target-row-${target.id}`"
            @click="selectedTargetId = target.id"
          >
            <span class="status-dot" :class="target.status" aria-hidden="true" />
            <span class="row-copy">
              <strong>{{ targetTitle(target) }}</strong>
              <small>{{ targetMetaLine(target) }}</small>
            </span>
          </button>
          <div v-if="!store.targets.length" class="rail-message">还没有求职目标。点击右上角「新建求职目标」创建第一个。</div>
        </template>
      </aside>

      <main class="target-detail">
        <template v-if="selectedTarget">
          <header class="detail-identity">
            <div class="identity-copy">
              <template v-if="editingId === selectedTarget.id">
                <input
                  v-model="editingName"
                  class="identity-input"
                  :data-test="`target-name-${selectedTarget.id}`"
                  aria-label="求职目标名称"
                  @keyup.enter="saveName(selectedTarget.id)"
                >
                <span class="inline-actions">
                  <button :data-test="`save-target-name-${selectedTarget.id}`" type="button" @click="saveName(selectedTarget.id)">保存</button>
                  <button type="button" @click="cancelRename">取消</button>
                </span>
              </template>
              <template v-else>
                <h2>{{ selectedTarget.name }}</h2>
                <p v-if="identityMeta.roleLine" class="identity-role-line">{{ identityMeta.roleLine }}</p>
                <p class="identity-meta-line">{{ identityMeta.stateLine }}</p>
                <p class="identity-updated">最近更新 {{ formatDate(selectedTarget.updatedAt) }}</p>
              </template>
            </div>
          </header>

          <!-- 准备进度：简历 / 模拟面试 / 日程 -->
          <section class="detail-section progress-section">
            <h3 class="section-title">准备进度</h3>
            <div class="progress-grid">
              <div
                v-for="column in progressColumns" :key="column.key"
                class="progress-column" :data-test="`progress-${column.key}`"
              >
                <span class="progress-dot" :class="{ ready: column.ready }" aria-hidden="true">
                  <el-icon v-if="column.ready" :size="13"><Check /></el-icon>
                </span>
                <span class="progress-copy">
                  <strong>{{ column.label }}</strong>
                  <small>{{ column.meta }}</small>
                </span>
                <button type="button" class="text-action" @click="column.action()">{{ column.actionLabel }}<el-icon :size="12"><ArrowRight /></el-icon></button>
              </div>
            </div>
          </section>

          <!-- 下一场 -->
          <section class="detail-section" data-test="next-event">
            <h3 class="section-title">下一场</h3>
            <template v-if="nextEvent">
              <div class="next-event-row">
                <span class="next-event-time">{{ nextEventTimeLabel(nextEvent) }}</span>
                <strong>{{ nextEvent.title }}</strong>
              </div>
              <div class="next-event-actions">
                <p class="next-suggestion">{{ nextEventSuggestion }}</p>
                <button type="button" class="text-action" @click="startPreparation">开始准备<el-icon :size="12"><ArrowRight /></el-icon></button>
              </div>
            </template>
            <p v-else class="next-empty">暂无未来安排。为这个目标添加面试或笔试日程。</p>
          </section>

          <!-- 岗位信息 -->
          <section class="detail-section" data-test="job-info">
            <h3 class="section-title">岗位信息</h3>
            <div class="row">
              <span class="row-dot" :class="selectedJob ? 'ok' : 'off'" aria-hidden="true" />
              <div class="row-copy">
                <strong>{{ selectedJob ? jobLabel(selectedJob) : '未录入岗位' }}</strong>
                <small>{{ selectedJob ? jobStateLabel(selectedJob) : '在首页工作台为这个目标录入 JD' }}</small>
              </div>
              <button v-if="selectedJob" type="button" class="text-action" @click="openJob">查看岗位<el-icon :size="12"><ArrowRight /></el-icon></button>
              <button v-else type="button" class="text-action" @click="() => void router.push({ name: 'workbench' })">去录入<el-icon :size="12"><ArrowRight /></el-icon></button>
            </div>
          </section>

          <section class="detail-section detail-actions">
            <h3 class="section-title">操作</h3>
            <div class="action-buttons">
              <button :data-test="`rename-target-${selectedTarget.id}`" type="button" @click="beginRename(selectedTarget.id, selectedTarget.name)">重命名</button>
              <button
                v-if="selectedTarget.status === 'active'"
                :data-test="`archive-target-${selectedTarget.id}`"
                type="button"
                :disabled="busyId === selectedTarget.id"
                @click="archiveTarget(selectedTarget.id)"
              >归档</button>
              <button
                v-else
                :data-test="`restore-target-${selectedTarget.id}`"
                type="button"
                :disabled="busyId === selectedTarget.id"
                @click="restoreTarget(selectedTarget.id)"
              >恢复</button>
              <button class="danger" type="button" @click="openDelete(selectedTarget)">删除</button>
            </div>
          </section>
        </template>

        <div v-else class="detail-empty">
          <p v-if="store.errorMessage && !store.targets.length">{{ store.errorMessage }}</p>
          <p v-else>选择左侧求职目标查看详情</p>
        </div>
      </main>
    </div>

    <TargetCreateDialog
      :open="createOpen"
      :resumes="resumes"
      :submitting="creating"
      :error-message="createError"
      @close="closeCreate"
      @create="createTarget"
    />
    <TargetDeleteDialog :open="Boolean(deleteTarget)" :target="deleteTarget" :submitting="deleting" :error-message="deleteError" @close="closeDelete" @confirm="confirmDelete" />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Check } from '@element-plus/icons-vue'
import { useTargetsStore } from '../../stores/targets'
import PageHeader from '../../components/PageHeader.vue'
import TargetCreateDialog from '../../components/targets/TargetCreateDialog.vue'
import TargetDeleteDialog from '../../components/targets/TargetDeleteDialog.vue'
import type { JobProject } from '../../types/project'
import type { Resume } from '../../types/resume'
import type { JobDescription } from '../../types/job'
import type { ScheduleEvent } from '../../types/schedule'
import { SCHEDULE_EVENT_TYPE_LABELS } from '../../types/schedule'
import type { InterviewPlanResponse } from '../../types/interview'
import { getResumeVersion, listResumes } from '../../api/resume'
import { listJobDescriptions } from '../../api/job'
import { listScheduleEvents } from '../../api/schedule'
import { listMyInterviewPlans } from '../../api/interview'
import { buildTargetInterviewLocation } from '../../utils/interviewRoute'

interface LinkedResume {
  resumeId: number
  versionId: number
  title: string
  versionNo: number
}

interface ProgressColumn {
  key: string
  label: string
  meta: string
  ready: boolean
  actionLabel: string
  action: () => void
}

const store = useTargetsStore()
const route = useRoute()
const router = useRouter()

const resumes = ref<Resume[]>([])
const jobs = ref<JobDescription[]>([])
const jobsByTarget = ref(new Map<number, JobDescription | null>())
const resumeByVersion = ref(new Map<number, LinkedResume>())
const scheduleEvents = ref<ScheduleEvent[]>([])
const interviewPlans = ref<InterviewPlanResponse[]>([])

const editingId = ref<number | null>(null)
const editingName = ref('')
const busyId = ref<number | null>(null)
const operationError = ref('')
const createOpen = ref(false)
const creating = ref(false)
const createError = ref('')
const deleteTarget = ref<JobProject | null>(null)
const deleting = ref(false)
const deleteError = ref('')

const selectedTargetId = ref<number | null>(null)

onMounted(async () => {
  if (!store.targets.length) void store.load()
  await Promise.all([loadMaterials(), loadSchedule(), loadInterviewPlans()])
  const queryId = positiveQueryId(route.query.targetId)
  const fallback = queryId ?? store.activeTargetId ?? store.targets[0]?.id ?? null
  if (fallback != null && store.targets.some((target) => target.id === fallback)) {
    selectedTargetId.value = fallback
  }
})

// 路由 query 变化时跟随预选（从工作台「打开求职目标」等入口进入）。
watch(() => positiveQueryId(route.query.targetId), (queryId) => {
  if (queryId != null && store.targets.some((target) => target.id === queryId)) {
    selectedTargetId.value = queryId
  }
})

// 目标被删除后，选中态回退到当前目标或第一个。
watch(() => store.targets.map((target) => target.id).join(','), () => {
  if (selectedTargetId.value != null && store.targets.some((target) => target.id === selectedTargetId.value)) return
  selectedTargetId.value = store.activeTargetId ?? store.targets[0]?.id ?? null
})

async function loadMaterials() {
  try {
    const [resumeRes, jobRes] = await Promise.all([listResumes(), listJobDescriptions()])
    resumes.value = resumeRes.data
    jobs.value = jobRes.data
    const jobMap = new Map<number, JobDescription>()
    for (const job of jobs.value) jobMap.set(job.id, job)
    for (const target of store.targets) jobsByTarget.value.set(target.id, target.jobDescriptionId != null ? (jobMap.get(target.jobDescriptionId) ?? null) : null)
    const versionMap = new Map<number, LinkedResume>()
    for (const resume of resumeRes.data) {
      if (resume.currentVersion?.id != null) {
        versionMap.set(resume.currentVersion.id, { resumeId: resume.id, versionId: resume.currentVersion.id, title: resume.title, versionNo: resume.currentVersion.versionNo })
      }
    }
    // 目标可能关联到非当前版本：按版本 id 回查补齐，找不到就如实不渲染简历行。
    const missingVersionIds = store.targets
      .map((target) => target.resumeVersionId)
      .filter((id): id is number => id != null && !versionMap.has(id))
    await Promise.all(missingVersionIds.map(async (versionId) => {
      try {
        const versionRes = await getResumeVersion(versionId)
        const resume = resumeRes.data.find((item) => item.id === versionRes.data.resumeId)
        if (resume) {
          versionMap.set(versionId, { resumeId: resume.id, versionId, title: resume.title, versionNo: versionRes.data.versionNo })
        }
      } catch { /* 版本已删除则保持未关联 */ }
    }))
    resumeByVersion.value = versionMap
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '读取关联材料失败'
  }
}

async function loadSchedule() {
  try {
    const res = await listScheduleEvents()
    scheduleEvents.value = res.data
  } catch { /* 日程读取失败时只缺日程行，不阻塞整页 */ }
}

async function loadInterviewPlans() {
  try {
    const res = await listMyInterviewPlans()
    interviewPlans.value = res.data
  } catch { /* 模拟面试记录读取失败时只缺完成数，不阻塞整页 */ }
}

const selectedTarget = computed(() => store.targets.find((target) => target.id === selectedTargetId.value) ?? null)

function jobFor(target: JobProject): JobDescription | null {
  return jobsByTarget.value.get(target.id) ?? null
}
function targetTitle(target: JobProject): string {
  const job = jobFor(target)
  if (job) return job.companyName ? `${job.companyName} · ${job.jobTitle}` : job.jobTitle
  return target.name
}
function jobLabel(job: JobDescription): string {
  return job.companyName ? `${job.companyName} · ${job.jobTitle}` : job.jobTitle
}
function jobStateLabel(job: JobDescription): string {
  return job.parseStatus === 'succeeded' ? 'JD 已录入并解析' : 'JD 已录入，待解析'
}

function targetEvents(target: JobProject): ScheduleEvent[] {
  if (target.jobDescriptionId == null) return []
  return scheduleEvents.value
    .filter((event) => event.jobDescriptionId === target.jobDescriptionId)
    .sort((left, right) => new Date(left.startTime).getTime() - new Date(right.startTime).getTime())
}
function targetFutureEvents(target: JobProject): ScheduleEvent[] {
  const now = Date.now()
  return targetEvents(target).filter((event) => new Date(event.startTime).getTime() >= now)
}
function targetNextEvent(target: JobProject): ScheduleEvent | null {
  return targetFutureEvents(target)[0] ?? null
}
function targetCompletedPlans(target: JobProject): InterviewPlanResponse[] {
  if (target.jobDescriptionId == null || target.resumeVersionId == null) return []
  return interviewPlans.value.filter(
    (plan) => plan.completed && plan.jobDescriptionId === target.jobDescriptionId && plan.resumeVersionId === target.resumeVersionId,
  )
}
function latestFeedback(target: JobProject): InterviewPlanResponse | null {
  return targetCompletedPlans(target)
    .filter((plan) => plan.summary)
    .sort((left, right) => planTimestamp(right) - planTimestamp(left))[0] ?? null
}
function planTimestamp(plan: InterviewPlanResponse): number {
  const at = plan.summaryGeneratedAt ?? plan.updatedAt ?? plan.createdAt ?? ''
  const time = new Date(at).getTime()
  return Number.isNaN(time) ? 0 : time
}

// 左列行 meta：真实存在的部分才渲染（场次 · 简历版本 · 下一场摘要）
function targetMetaLine(target: JobProject): string {
  const parts: string[] = []
  const futureCount = targetFutureEvents(target).length
  if (futureCount > 0) parts.push(`${futureCount} 场安排`)
  const resume = selectedResumeFor(target)
  if (resume) parts.push(`简历 V${resume.versionNo}`)
  const next = targetNextEvent(target)
  if (next) parts.push(nextEventSummary(next))
  return parts.join(' · ')
}
function selectedResumeFor(target: JobProject): LinkedResume | null {
  if (!target.resumeVersionId) return null
  return resumeByVersion.value.get(target.resumeVersionId) ?? null
}
function nextEventSummary(event: ScheduleEvent): string {
  return `${relativeDayLabel(event.startTime)}${SCHEDULE_EVENT_TYPE_LABELS[event.eventType]}`
}
function nextEventTimeLabel(event: ScheduleEvent): string {
  const time = new Date(event.startTime)
  const hhmm = `${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`
  return `${relativeDayLabel(event.startTime)} ${hhmm}`
}
function relativeDayLabel(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '待定'
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startOfDay = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const days = Math.round((startOfDay - startOfToday) / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  if (days === 2) return '后天'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const selectedResume = computed<LinkedResume | null>(() => selectedTarget.value ? selectedResumeFor(selectedTarget.value) : null)
const selectedJob = computed<JobDescription | null>(() => selectedTarget.value ? jobFor(selectedTarget.value) : null)
const nextEvent = computed<ScheduleEvent | null>(() => selectedTarget.value ? targetNextEvent(selectedTarget.value) : null)

// 详情头部 identity/meta 层：只渲染真实存在的数据，缺则省略对应行。
const identityMeta = computed(() => {
  const target = selectedTarget.value
  if (!target) return { roleLine: '', stateLine: '' }
  const roleLine = selectedJob.value ? jobLabel(selectedJob.value) : ''
  const parts: string[] = [target.status === 'active' ? '进行中' : '已归档']
  if (target.id === store.activeTargetId) parts.push('当前目标')
  const count = targetFutureEvents(target).length
  if (count > 0) parts.push(`${count} 场近期安排`)
  if (selectedResume.value) parts.push(`简历 V${selectedResume.value.versionNo}`)
  return { roleLine, stateLine: parts.join(' · ') }
})

// 准备进度三列：全部来自真实数据（简历版本 / 已完成模拟面试 / 未来日程）。
const progressColumns = computed<ProgressColumn[]>(() => {
  const target = selectedTarget.value
  if (!target) return []
  const resume = selectedResume.value
  const completed = targetCompletedPlans(target).length
  const futureCount = targetFutureEvents(target).length
  return [
    {
      key: 'resume',
      label: '简历',
      meta: resume ? `V${resume.versionNo} 已就绪` : '尚未关联',
      ready: Boolean(resume),
      actionLabel: resume ? '打开简历' : '去工作台关联',
      action: resume ? () => openResume(resume) : () => void router.push({ name: 'workbench' }),
    },
    {
      key: 'interview',
      label: '模拟面试',
      meta: completed > 0 ? `${completed} 次完成` : '尚未开始',
      ready: completed > 0,
      actionLabel: completed > 0 ? '查看复盘' : '开始模拟',
      action: () => openInterview(),
    },
    {
      key: 'schedule',
      label: '日程',
      meta: futureCount > 0 ? `${futureCount} 场安排` : '暂无安排',
      ready: futureCount > 0,
      actionLabel: '查看日程',
      action: () => void router.push({ name: 'schedule' }),
    },
  ]
})

// 下一场建议：与工作台同源的反馈/准备缺口推导，全部来自真实数据。
const nextEventSuggestion = computed(() => {
  const target = selectedTarget.value
  if (!target) return ''
  if (!target.resumeVersionId) return '先为这个目标选择一份简历，再开始针对性准备'
  if (!latestFeedback(target)) return '面试前完成一次针对当前目标的模拟面试'
  return '复看最近一次模拟反馈，确认薄弱点'
})

function openResume(linked: LinkedResume) {
  void router.push({ name: 'resume-editor', query: { resumeId: String(linked.resumeId), versionId: String(linked.versionId) } })
}
function openInterview() {
  const target = selectedTarget.value
  if (!target) { void router.push({ name: 'interview' }); return }
  void router.push(buildTargetInterviewLocation({
    targetId: target.id,
    versionId: target.resumeVersionId,
    jobId: target.jobDescriptionId,
  }))
}
function startPreparation() {
  openInterview()
}
function openJob() {
  const target = selectedTarget.value
  if (!target) return
  void router.push({ name: 'workbench', query: { targetId: String(target.id) } })
}

function beginRename(id: number, name: string) { editingId.value = id; editingName.value = name; operationError.value = '' }
function cancelRename() { editingId.value = null; editingName.value = '' }
async function saveName(id: number) {
  const name = editingName.value.trim()
  if (!name) { operationError.value = '求职目标名称不能为空'; return }
  busyId.value = id; operationError.value = ''
  try { await store.rename(id, name); cancelRename() }
  catch (error) { operationError.value = error instanceof Error ? error.message : '重命名失败' }
  finally { busyId.value = null }
}
async function archiveTarget(id: number) { await runTargetAction(id, () => store.archive(id), '归档失败') }
async function restoreTarget(id: number) { await runTargetAction(id, () => store.restore(id), '恢复失败') }
async function runTargetAction(id: number, action: () => Promise<unknown>, fallback: string) {
  busyId.value = id; operationError.value = ''
  try { await action() } catch (error) { operationError.value = error instanceof Error ? error.message : fallback }
  finally { busyId.value = null }
}
function openCreate() { createError.value = ''; createOpen.value = true }
function closeCreate() { if (!creating.value) createOpen.value = false }
async function createTarget(payload: { name: string; resumeVersionId?: number | null }) {
  creating.value = true; createError.value = ''
  try {
    const created = await store.create(payload)
    createOpen.value = false
    selectedTargetId.value = created.id
  } catch (error) {
    createError.value = error instanceof Error ? error.message : '创建求职目标失败'
  } finally {
    creating.value = false
  }
}
function openDelete(target: JobProject) { deleteTarget.value = target; deleteError.value = '' }
function closeDelete() { if (!deleting.value) deleteTarget.value = null }
async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true; deleteError.value = ''
  try { await store.remove(deleteTarget.value.id); deleteTarget.value = null }
  catch (error) { deleteError.value = error instanceof Error ? error.message : '删除求职目标失败' }
  finally { deleting.value = false }
}

function positiveQueryId(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
function formatDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.targets-page{display:flex;flex-direction:column;height:100%;min-height:0}
.targets-body{flex:1;min-height:0;display:grid;grid-template-columns:300px minmax(0,1fr);border-top:1px solid var(--border-subtle)}

/* ── 左列：目标列表 ── */
.target-rail{min-height:0;overflow-y:auto;border-right:1px solid var(--border-subtle);padding:10px 12px 24px}
.rail-head{display:flex;align-items:baseline;justify-content:space-between;gap:10px;padding:6px 8px 10px;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:.04em}
.rail-error{color:var(--danger);font-weight:500;letter-spacing:0}
.target-row{display:flex;align-items:flex-start;gap:10px;width:100%;text-align:left;border:0;border-radius:10px;background:transparent;padding:10px 8px;color:var(--ink);cursor:pointer}
.target-row:hover{background:var(--bg-hover)}
.target-row.selected{background:var(--bg-selected)}
.target-row.archived .row-copy strong,.target-row.archived .row-copy small{color:var(--text-tertiary)}
.row-copy{display:grid;gap:3px;min-width:0}
.row-copy strong{font-size:14px;font-weight:600;line-height:1.35;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-copy small{font-size:12px;color:var(--muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.rail-message{padding:14px 10px;color:var(--muted);font-size:13px;line-height:1.6}

/* ── 右列：求职项目工作台 ── */
.target-detail{min-height:0;overflow-y:auto;padding:26px 34px 48px;max-width:880px}
.detail-identity{padding-bottom:22px}
.identity-copy{min-width:0}
.detail-identity h2{margin:0;font-size:24px;font-weight:650;letter-spacing:-.02em;color:var(--ink)}
.identity-role-line{margin:8px 0 0;color:var(--copy);font-size:15px;font-weight:600;line-height:1.4}
.identity-meta-line{margin:6px 0 0;color:var(--copy);font-size:13px;line-height:1.4}
.identity-updated{margin:4px 0 0;color:var(--muted);font-size:12px;line-height:1.4}
.identity-input{border:1px solid var(--border-default);border-radius:var(--radius-control);padding:8px 10px;background:var(--bg-surface);color:var(--ink);font-size:14px}
.inline-actions{display:flex;gap:8px;margin-top:10px}

.detail-section{padding:18px 0;border-top:1px solid var(--border-subtle)}
.section-title{margin:0 0 10px;font-size:12px;font-weight:600;letter-spacing:.06em;color:var(--muted)}

/* 准备进度三列 */
.progress-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
.progress-column{display:grid;gap:8px;padding:14px;border:1px solid var(--border-subtle);border-radius:var(--radius-panel);background:var(--bg-surface)}
.progress-dot{width:20px;height:20px;border-radius:50%;display:grid;place-items:center;background:var(--bg-hover);color:var(--muted)}
.progress-dot.ready{background:var(--accent-soft);color:var(--brand)}
.progress-copy{display:grid;gap:3px}
.progress-copy strong{font-size:13px;font-weight:650;color:var(--ink)}
.progress-copy small{font-size:12px;color:var(--muted)}
.text-action{display:inline-flex;align-items:center;gap:4px;justify-self:start;padding:2px 0;border:0;background:none;color:var(--brand);font-size:12px;font-weight:600;cursor:pointer}
.text-action:hover{text-decoration:underline}

/* 下一场 */
.next-event-row{display:flex;align-items:center;gap:10px;padding:10px 4px 4px}
.next-event-time{flex:0 0 auto;padding:5px 10px;border-radius:var(--radius-control);background:var(--accent-soft);color:var(--brand);font-size:12px;font-weight:650;font-variant-numeric:tabular-nums}
.next-event-row strong{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:14px;font-weight:650;color:var(--ink)}
.next-event-actions{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:6px 4px 0}
.next-suggestion{margin:0;color:var(--copy);font-size:13px;line-height:1.5}
.next-empty{padding:8px 4px;margin:0;color:var(--muted);font-size:13px}

/* 岗位信息 / 通用行 */
.row{display:flex;align-items:center;gap:12px;padding:10px 4px}
.row + .row{border-top:1px solid var(--border-subtle)}
.row-dot{flex:0 0 auto;width:8px;height:8px;border-radius:50%;background:var(--border-default)}
.row-dot.ok{background:var(--brand)}
.row-dot.off{background:var(--border-strong)}
.row .row-copy{flex:1}
.row-copy strong{font-size:14px;font-weight:600;color:var(--ink)}
.row-copy small{font-size:12px;color:var(--muted)}

.detail-actions .action-buttons{display:flex;flex-wrap:wrap;gap:9px}
.detail-actions button{border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);padding:8px 13px;font-size:13px;font-weight:500;cursor:pointer}
.detail-actions button:hover{background:var(--bg-hover)}
.detail-actions button:disabled{opacity:.5;cursor:default}
.detail-actions button.danger{color:var(--danger)}

.detail-empty{display:grid;place-items:center;min-height:100%;color:var(--muted);font-size:14px}

.btn-primary{border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:9px 15px;font-size:13px;font-weight:600;cursor:pointer}
.btn-primary:hover{background:var(--accent-hover)}
</style>
