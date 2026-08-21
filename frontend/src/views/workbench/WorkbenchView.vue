<template>
  <WorkspaceLoadingState v-if="state === 'loading'" />
  <WorkspaceErrorState v-else-if="state === 'error'" :message="errorMessage" @retry="loadWorkspace" />
  <section v-else-if="state === 'first-run'" data-test="first-run-workspace" class="first-run">
    <p>欢迎使用 ResumeGo</p><h1>先建立本地资料，再决定从哪里开始</h1><span>你可以从空白简历开始，也可以先录入目标岗位；Markdown 导入将在下一阶段接入。</span>
    <div><button type="button" @click="openBlankResume">创建空白简历</button><button type="button" @click="openTargetDialog">添加目标岗位</button></div>
  </section>
  <TargetDashboard
    v-else
    :agenda-events="agendaEvents"
    :selected-event-id="selectedEventId"
    :detail="detail"
    :recent-activity="recentActivity"
    :material-error="targetMaterialError"
    @select-event="handleSelectEvent"
    @action="handleTargetAction"
  />
  <div v-if="linkDialogOpen" class="link-backdrop" role="presentation" @click.self="closeLinkDialog">
    <section class="link-dialog" role="dialog" aria-modal="true" aria-labelledby="link-title">
      <button type="button" aria-label="关闭关联" @click="closeLinkDialog">×</button>
      <p>关联求职目标</p>
      <h2 id="link-title">选择这个安排对应的求职目标</h2>
      <div v-if="linkableTargets.length" class="link-list">
        <button
          v-for="target in linkableTargets"
          :key="target.id"
          type="button"
          data-test="link-target-option"
          :disabled="linkSubmitting"
          @click="linkEventToTarget(target.id)"
        >{{ target.name }}</button>
      </div>
      <div v-else class="link-empty">
        <p>还没有可关联的求职目标，先录入一个岗位。</p>
        <button type="button" data-test="link-add-job" @click="openLinkJob">录入岗位</button>
      </div>
      <p v-if="linkError" class="link-error">{{ linkError }}</p>
    </section>
  </div>
  <TargetCreateDialog
    :open="targetDialogOpen"
    :resumes="resumes"
    :submitting="creatingTarget"
    :error-message="targetCreateError"
    @close="closeTargetDialog"
    @create="createTarget"
  />
  <TargetJobDialog
    :open="jobDialogOpen"
    :submitting="updatingTargetMaterials"
    :error-message="targetMaterialError"
    @close="closeMaterialDialogs"
    @create="saveTargetJob"
  />
  <TargetResumeDialog
    :open="resumeDialogOpen"
    :resumes="resumes"
    :submitting="updatingTargetMaterials"
    :error-message="targetMaterialError"
    @close="closeMaterialDialogs"
    @select="selectTargetResume"
    @create-resume="createResumeForTarget"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createJobDescription, getJobDescription } from '../../api/job'
import { listMyInterviewPlans } from '../../api/interview'
import { listScheduleEvents, updateScheduleEvent } from '../../api/schedule'
import { getResumeVersion, listResumes } from '../../api/resume'
import TargetCreateDialog from '../../components/targets/TargetCreateDialog.vue'
import TargetJobDialog from '../../components/targets/TargetJobDialog.vue'
import TargetResumeDialog from '../../components/targets/TargetResumeDialog.vue'
import WorkspaceLoadingState from '../../components/workbench/WorkspaceLoadingState.vue'
import WorkspaceErrorState from '../../components/workbench/WorkspaceErrorState.vue'
import TargetDashboard, {
  type AgendaEventView,
  type DetailView,
  type ReadinessRow,
  type RecentActivityItem,
  type TargetDashboardAction,
} from '../../components/workbench/TargetDashboard.vue'
import { useTargetsStore } from '../../stores/targets'
import type { CreateJobProjectRequest, JobProject } from '../../types/project'
import type { CreateJobDescriptionRequest, JobDescription } from '../../types/job'
import type { Resume, ResumeVersion } from '../../types/resume'
import type { InterviewPlanResponse } from '../../types/interview'
import type { ScheduleEvent } from '../../types/schedule'
import { buildResumeEditorLocation } from '../../utils/editorRoute'
import { buildTargetInterviewLocation } from '../../utils/interviewRoute'

const targetsStore = useTargetsStore()
const route = useRoute()
const router = useRouter()
const resumes = ref<Resume[]>([])
const loading = ref(true)
const localError = ref('')
const targetDialogOpen = ref(false)
const creatingTarget = ref(false)
const targetCreateError = ref('')
const jobDialogOpen = ref(false)
const resumeDialogOpen = ref(false)
const updatingTargetMaterials = ref(false)
const targetMaterialError = ref('')
const interviewPlans = ref<InterviewPlanResponse[]>([])
const scheduleEvents = ref<ScheduleEvent[]>([])
const jobsByTarget = ref<Record<number, JobDescription | null>>({})
const inspectorVersions = ref(new Map<number, ResumeVersion | null>())
const linkDialogOpen = ref(false)
const linkSubmitting = ref(false)
const linkError = ref('')
const pendingLinkEventId = ref<string | number | undefined>(undefined)

// 首页不依赖 global active target：选中态 = 日程中选中的安排（或刚录入/关联后的目标）。
const selectedEventId = ref<string | null>(null)
const focusedTargetId = ref<number | null>(null)

const errorMessage = computed(() => localError.value)
// 首页即工作台：只要有简历或目标就进入 Dashboard（无目标时由 Inspector 呈现空态），
// 只有真正空白的首次启动保留 onboarding；local-library 引导不再占用首页。
const state = computed<'loading' | 'error' | 'first-run' | 'target'>(() => {
  if (loading.value || targetsStore.loading) return 'loading'
  if (errorMessage.value) return 'error'
  if (targetsStore.targets.length || resumes.value.length) return 'target'
  return 'first-run'
})

// ── Agenda：跨所有目标的真实日程，公司/角色从事件关联的 JD 解析 ──
const agendaEvents = computed<AgendaEventView[]>(() => {
  const todayStart = new Date()
  todayStart.setHours(0, 0, 0, 0)
  return scheduleEvents.value
    .filter((event) => event.eventType === 'interview' || event.eventType === 'exam' || event.eventType === 'followup')
    .filter((event) => new Date(event.startTime).getTime() >= todayStart.getTime())
    .sort((left, right) => left.startTime.localeCompare(right.startTime))
    .map((event) => {
      const target = resolveEventTarget(event)
      const job = target ? jobsByTarget.value[target.id] ?? null : null
      return {
        id: String(event.id),
        title: event.title,
        eventType: event.eventType,
        timeLabel: formatEventTime(event.startTime),
        relativeLabel: relativeEventLabel(event.startTime),
        dayLabel: dayLabelFor(event.startTime),
        companyLabel: job?.companyName ?? '',
        roleLabel: job?.jobTitle ?? '',
        countdownLabel: remainingLabel(event.startTime),
      }
    })
})

// 默认选中最近的安排；用户聚焦某个目标（focusedTargetId）时不抢占，等待其点击事件。
watch(agendaEvents, (events) => {
  if (!events.length) { selectedEventId.value = null; return }
  if (focusedTargetId.value) return
  if (!events.some((event) => event.id === selectedEventId.value)) {
    selectedEventId.value = events[0].id
  }
}, { immediate: true })

const selectedEvent = computed(() => agendaEvents.value.find((event) => event.id === selectedEventId.value) ?? agendaEvents.value[0] ?? null)

function resolveEventTarget(event: ScheduleEvent): JobProject | null {
  if (event.jobDescriptionId === null) return null
  return targetsStore.targets.find((target) => target.jobDescriptionId === event.jobDescriptionId) ?? null
}

// ── Detail：跟随选中的安排 → 其目标；无安排时回退到最近活跃目标；完全没有目标时为空态 ──
const fallbackTarget = computed<JobProject | null>(() => {
  if (selectedEvent.value || focusedTargetId.value) return null
  const active = targetsStore.targets.filter((target) => target.status === 'active')
  return [...active].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))[0]
    ?? [...targetsStore.targets].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))[0]
    ?? null
})
const detailTarget = computed<JobProject | null>(() => {
  const event = selectedEvent.value
  if (event) {
    const source = scheduleEvents.value.find((item) => String(item.id) === event.id)
    if (source?.jobDescriptionId != null) {
      return targetsStore.targets.find((item) => item.jobDescriptionId === source.jobDescriptionId) ?? null
    }
    return null
  }
  if (focusedTargetId.value) return targetsStore.targets.find((item) => item.id === focusedTargetId.value) ?? null
  return fallbackTarget.value
})

// 选中目标变化时按需读取其简历版本（jobs 已随 targets 全量加载）。
watch(() => detailTarget.value?.id ?? null, (targetId) => {
  void ensureInspectorVersion(targetId)
}, { immediate: true })

async function ensureInspectorVersion(targetId: number | null) {
  if (!targetId) return
  const target = targetsStore.targets.find((item) => item.id === targetId)
  if (!target?.resumeVersionId || inspectorVersions.value.has(targetId)) return
  try {
    const response = await getResumeVersion(target.resumeVersionId)
    inspectorVersions.value.set(targetId, response.data)
  } catch {
    inspectorVersions.value.set(targetId, null)
  }
}

const detailJob = computed(() => {
  const target = detailTarget.value
  return target ? jobsByTarget.value[target.id] ?? null : null
})
const detailFeedback = computed<InterviewPlanResponse | null>(() => {
  const target = detailTarget.value
  if (!target?.jobDescriptionId || !target.resumeVersionId) return null
  return interviewPlans.value
    .filter((plan) => plan.completed && plan.jobDescriptionId === target.jobDescriptionId && plan.resumeVersionId === target.resumeVersionId && plan.summary)
    .sort((left, right) => planTimestamp(right) - planTimestamp(left))[0] ?? null
})
const detailNextAction = computed<{ text: string; button: string; hint?: string; action: TargetDashboardAction } | null>(() => {
  const target = detailTarget.value
  if (!target) return null
  if (!target.resumeVersionId) return { text: '先为这个目标选择一份简历', button: '选择简历', action: 'select-resume' }
  if (!detailFeedback.value) return { text: '面试前完成一次针对当前目标的模拟面试', button: '开始模拟面试', hint: '预计 20 分钟', action: 'open-interview' }
  return { text: '复看最近一次模拟反馈，确认薄弱点', button: '查看完整报告', action: 'open-feedback' }
})

const detail = computed<DetailView>(() => {
  const event = selectedEvent.value
  if (event) {
    const target = detailTarget.value
    if (target) {
      const job = detailJob.value
      return {
        kind: 'event',
        note: '',
        companyLabel: job?.companyName ?? '',
        roleLabel: job?.jobTitle ?? '',
        targetName: target.name,
        targetLinked: true,
        targetId: target.id,
        readiness: buildReadiness(),
        nextAction: detailNextAction.value,
      }
    }
    return {
      kind: 'event',
      note: '',
      companyLabel: '',
      roleLabel: '',
      targetName: '',
      targetLinked: false,
      targetId: null,
      readiness: [],
      nextAction: null,
    }
  }
  const target = detailTarget.value
  if (!target) {
    return { kind: 'empty', note: '还没有求职目标', companyLabel: '', roleLabel: '', targetName: '', targetLinked: false, targetId: null, readiness: [], nextAction: null }
  }
  return {
    kind: 'target',
    note: focusedTargetId.value ? '刚录入的求职目标' : '当前没有选中的安排',
    companyLabel: detailJob.value?.companyName ?? '',
    roleLabel: detailJob.value?.jobTitle ?? '',
    targetName: target.name,
    targetLinked: false,
    targetId: target.id,
    readiness: buildReadiness(),
    nextAction: detailNextAction.value,
  }
})

function buildReadiness(): ReadinessRow[] {
  const target = detailTarget.value
  const resumeState = resumeStateFor(target)
  const feedback = detailFeedback.value
  const targetEvents = target ? agendaEvents.value.filter((event) => targetMatchesEvent(target, event)) : []
  const first = targetEvents[0]
  return [
    {
      key: 'resume', label: '简历',
      meta: resumeState.meta, subMeta: resumeState.subMeta, ready: resumeState.ready,
      actionLabel: resumeState.ready ? '编辑' : '选择',
      action: resumeState.ready ? 'open-editor' : 'select-resume',
    },
    {
      key: 'mock', label: '模拟面试',
      meta: feedback ? `已完成 · ${formatPlanDate(feedback.updatedAt || feedback.createdAt)}` : '尚未完成',
      subMeta: '', ready: Boolean(feedback),
      actionLabel: feedback ? '复盘' : '开始',
      action: feedback ? 'open-feedback' : 'open-interview',
    },
    {
      key: 'schedule', label: '日程',
      meta: first ? `${first.relativeLabel} ${first.timeLabel}` : '暂无安排',
      subMeta: targetEvents.length > 1 ? `共 ${targetEvents.length} 场` : '', ready: targetEvents.length > 0,
      actionLabel: targetEvents.length > 0 ? '查看' : '添加',
      action: 'open-schedule',
    },
  ]
}

function targetMatchesEvent(target: JobProject | null, event: AgendaEventView): boolean {
  if (!target?.jobDescriptionId) return false
  const source = scheduleEvents.value.find((item) => String(item.id) === event.id)
  return source?.jobDescriptionId === target.jobDescriptionId
}

function resumeStateFor(target: JobProject | null): { ready: boolean; meta: string; subMeta: string } {
  if (!target?.resumeVersionId) return { ready: false, meta: '未选择简历', subMeta: '' }
  if (!inspectorVersions.value.has(target.id)) return { ready: false, meta: '读取中…', subMeta: '' }
  const version = inspectorVersions.value.get(target.id)
  if (!version) return { ready: false, meta: '读取失败', subMeta: '' }
  const resume = resumes.value.find((item) => item.id === version.resumeId)
  const updated = formatShortDate(version.createdAt)
  return { ready: true, meta: `${resume?.title ?? '简历'} · V${version.versionNo}`, subMeta: updated ? `${updated} 更新` : '' }
}

function formatShortDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// ── 最近活动：所有已完成的模拟面试（含总结），按更新时间取最新一条 ──
const recentActivity = computed<RecentActivityItem[]>(() => {
  return interviewPlans.value
    .filter((plan) => plan.completed && plan.summary?.overallSummary)
    .sort((left, right) => planTimestamp(right) - planTimestamp(left))
    .slice(0, 1)
    .map((plan) => {
      const target = targetsStore.targets.find((item) => item.jobDescriptionId === plan.jobDescriptionId)
      const job = target ? jobsByTarget.value[target.id] ?? null : null
      return {
        id: plan.planId,
        dateLabel: formatPlanDate(plan.updatedAt || plan.createdAt),
        companyLabel: job?.companyName ?? '',
        title: plan.title || '模拟面试',
        summary: plan.summary!.overallSummary,
        targetId: target?.id ?? null,
      }
    })
})

async function loadWorkspace() {
  loading.value = true
  localError.value = ''
  try {
    const [, resumeResponse, planResponse] = await Promise.all([targetsStore.load(), listResumes(), listMyInterviewPlans()])
    resumes.value = resumeResponse.data
    interviewPlans.value = planResponse.data
    const requestedTargetId = Number(Array.isArray(route.query.targetId) ? route.query.targetId[0] : route.query.targetId)
    if (Number.isSafeInteger(requestedTargetId) && requestedTargetId > 0) targetsStore.select(requestedTargetId)
    if (targetsStore.errorMessage) localError.value = targetsStore.errorMessage
    await loadTargetJobs()
    await loadUpcomingSchedule()
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '读取本地工作区失败'
  } finally {
    loading.value = false
  }
}

async function loadTargetJobs() {
  targetMaterialError.value = ''
  const jobs: Record<number, JobDescription | null> = {}
  const errors: string[] = []
  await Promise.all(targetsStore.targets.map(async (target) => {
    if (!target.jobDescriptionId) return
    try {
      const response = await getJobDescription(target.jobDescriptionId)
      jobs[target.id] = response.data
    } catch (error) {
      jobs[target.id] = null
      errors.push(error instanceof Error ? error.message : '读取目标岗位失败')
    }
  }))
  jobsByTarget.value = jobs
  if (errors.length) targetMaterialError.value = errors.join('；')
}

async function loadUpcomingSchedule() {
  try {
    const from = new Date()
    const to = new Date()
    to.setDate(to.getDate() + 30)
    const response = await listScheduleEvents(toIsoLocal(from), toIsoLocal(to))
    scheduleEvents.value = response.data
  } catch {
    scheduleEvents.value = []
  }
}

function handleSelectEvent(id: string) {
  focusedTargetId.value = null
  selectedEventId.value = id
}

function handleTargetAction(action: TargetDashboardAction, targetId?: number) {
  targetMaterialError.value = ''
  if (action === 'add-job') { jobDialogOpen.value = true; return }
  if (action === 'open-schedule') { void router.push({ name: 'schedule' }); return }
  if (action === 'open-target') { void router.push({ name: 'targets', query: targetId ? { targetId: String(targetId) } : {} }); return }
  if (action === 'select-resume') { resumeDialogOpen.value = true; return }
  if (action === 'open-interview' || action === 'open-feedback') { openTargetInterview(targetId); return }
  if (action === 'open-editor') { openTargetEditor(targetId); return }
  if (action === 'link-target') { openLinkDialog(targetId); return }
}

// ── 未关联事件的「关联目标 →」：选择已有目标，PATCH 事件的 jobDescriptionId 后刷新 ──
const linkableTargets = computed(() => targetsStore.targets.filter((target) => target.jobDescriptionId != null))

function openLinkDialog(eventId?: number) {
  linkError.value = ''
  linkDialogOpen.value = true
  pendingLinkEventId.value = eventId ?? selectedEvent.value?.id
}
function closeLinkDialog() {
  if (linkSubmitting.value) return
  linkDialogOpen.value = false
  linkError.value = ''
}
function openLinkJob() {
  linkDialogOpen.value = false
  jobDialogOpen.value = true
}

async function linkEventToTarget(targetId: number) {
  const event = scheduleEvents.value.find((item) => String(item.id) === String(pendingLinkEventId.value))
  const target = targetsStore.targets.find((item) => item.id === targetId)
  if (!event || !target?.jobDescriptionId || linkSubmitting.value) return
  linkSubmitting.value = true
  linkError.value = ''
  try {
    await updateScheduleEvent(event.id, {
      title: event.title,
      eventType: event.eventType,
      startTime: event.startTime,
      endTime: event.endTime,
      notes: event.notes,
      jobDescriptionId: target.jobDescriptionId,
    })
    await loadUpcomingSchedule()
    linkDialogOpen.value = false
  } catch (error) {
    linkError.value = error instanceof Error ? error.message : '关联求职目标失败'
  } finally {
    linkSubmitting.value = false
  }
}

function openTargetInterview(targetId?: number) {
  const target = targetsStore.targets.find((item) => item.id === targetId) ?? null
  if (!target) {
    void router.push({ name: 'interview' })
    return
  }
  // 始终携带目标上下文：缺 JD 或简历时也带 targetId，大厅据此显示目标并进入「补充绑定」态，
  // 不再丢掉上下文裸进大厅造成与主界面不一致。
  const location = buildTargetInterviewLocation({
    targetId: target.id,
    versionId: target.resumeVersionId,
    jobId: target.jobDescriptionId,
  }) as { name: string; query: Record<string, string> }
  void router.push(location)
}

function openTargetEditor(targetId?: number) {
  const target = targetsStore.targets.find((item) => item.id === targetId) ?? null
  if (!target?.resumeVersionId) return
  void router.push(buildResumeEditorLocation({ versionId: target.resumeVersionId, targetId: target.id }))
}

function openTargetDialog() {
  targetCreateError.value = ''
  targetDialogOpen.value = true
}

function closeTargetDialog() {
  if (creatingTarget.value) return
  targetDialogOpen.value = false
  targetCreateError.value = ''
}

async function createTarget(payload: CreateJobProjectRequest) {
  creatingTarget.value = true
  targetCreateError.value = ''
  try {
    await targetsStore.create(payload)
    targetDialogOpen.value = false
  } catch (error) {
    targetCreateError.value = error instanceof Error ? error.message : '创建求职目标失败'
  } finally {
    creatingTarget.value = false
  }
}

function openBlankResume() {
  void router.push(buildResumeEditorLocation({ mode: 'blank' }))
}

function closeMaterialDialogs() {
  if (updatingTargetMaterials.value) return
  jobDialogOpen.value = false
  resumeDialogOpen.value = false
  targetMaterialError.value = ''
}

// 全局「录入岗位」：保存 JD 并创建与之关联的新求职目标（首页没有 global active target 可依附）。
async function saveTargetJob(payload: CreateJobDescriptionRequest) {
  updatingTargetMaterials.value = true
  targetMaterialError.value = ''
  try {
    const response = await createJobDescription(payload)
    const company = response.data.companyName?.trim() || ''
    const title = response.data.jobTitle?.trim() || ''
    const target = await targetsStore.create({ name: [company, title].filter(Boolean).join(' · ') || '新求职目标', jobDescriptionId: response.data.id })
    jobsByTarget.value = { ...jobsByTarget.value, [target.id]: response.data }
    focusedTargetId.value = target.id
    selectedEventId.value = null
    jobDialogOpen.value = false
  } catch (error) {
    targetMaterialError.value = error instanceof Error ? error.message : '保存目标岗位失败'
  } finally {
    updatingTargetMaterials.value = false
  }
}

async function selectTargetResume(versionId: number) {
  const target = detailTarget.value
  if (!target) return
  updatingTargetMaterials.value = true
  targetMaterialError.value = ''
  try {
    await targetsStore.updateLinks(target.id, { jobDescriptionId: target.jobDescriptionId, resumeVersionId: versionId })
    inspectorVersions.value = new Map(Array.from(inspectorVersions.value).filter(([key]) => key !== target.id))
    await ensureInspectorVersion(target.id)
    focusedTargetId.value = target.id
    resumeDialogOpen.value = false
  } catch (error) {
    targetMaterialError.value = error instanceof Error ? error.message : '关联当前简历失败'
  } finally {
    updatingTargetMaterials.value = false
  }
}

function createResumeForTarget() {
  const targetId = detailTarget.value?.id
  resumeDialogOpen.value = false
  void router.push(buildResumeEditorLocation({ mode: 'blank', targetId }))
}

function toIsoLocal(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatEventTime(value: string): string {
  return value.slice(11, 16)
}

function relativeEventLabel(value: string): string {
  const date = new Date(value)
  const today = new Date()
  const dayStart = (target: Date) => new Date(target.getFullYear(), target.getMonth(), target.getDate()).getTime()
  const diff = Math.round((dayStart(date) - dayStart(today)) / 86400000)
  if (diff <= 0) return '今天'
  if (diff === 1) return '明天'
  return `${diff} 天后`
}

// compact rail 行内联时间：今天只显示时刻，其余日期按「M月D日」避免跨月歧义。
function dayLabelFor(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const today = new Date()
  const sameDay = date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate()
  if (sameDay) return ''
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 静态倒计时：只作 secondary metadata，不做 live ticker；小时粒度符合 Detail 头部文案。
function remainingLabel(startTime: string): string {
  const diff = new Date(startTime).getTime() - Date.now()
  if (!Number.isFinite(diff) || diff <= 0) return ''
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  return hours > 0 ? `还有 ${hours} 小时` : minutes > 0 ? `还有 ${minutes} 分钟` : ''
}

function planTimestamp(plan: InterviewPlanResponse) {
  const value = Date.parse(plan.updatedAt || plan.createdAt || '')
  return Number.isNaN(value) ? plan.planId : value
}

function formatPlanDate(value?: string) {
  if (!value) return '最近'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '最近'
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}

onMounted(loadWorkspace)
</script>

<style scoped>.first-run{max-width:720px;padding:76px 52px;color:var(--ink,#141516)}.first-run>p{color:var(--brand,#168866);font-weight:700}.first-run h1{color:var(--ink,#141516);font-size:34px;margin:8px 0 12px}.first-run>span{color:var(--muted,#718090);line-height:1.7}.first-run>div{display:flex;gap:10px;margin-top:25px}.first-run button{border:1px solid var(--line,#ccd7dd);border-radius:9px;background:var(--surface-solid,#fff);color:var(--ink,#141516);padding:10px 14px}.first-run button:first-child{border-color:var(--brand,#168866);background:var(--brand,#168866);color:white}.link-backdrop{position:fixed;z-index:50;inset:0;display:grid;place-items:center;background:rgba(7,8,8,.25);backdrop-filter:blur(8px)}.link-dialog{position:relative;display:grid;gap:10px;width:min(430px,calc(100vw - 40px));padding:30px;border:1px solid var(--line,rgba(20,22,24,.12));border-radius:18px;background:var(--surface-solid,#fff);color:var(--ink);box-shadow:0 24px 80px rgba(0,0,0,.18)}.link-dialog>button{position:absolute;top:12px;right:14px;border:0;background:transparent;color:var(--muted);font-size:22px}.link-dialog p{margin:0;color:var(--brand,#188d49);font-size:12px;font-weight:700}.link-dialog h2{margin:0;color:var(--ink);font-size:22px}.link-list{display:grid;gap:6px;margin-top:8px}.link-list button{border:1px solid var(--line,#ccd7dd);border-radius:11px;background:var(--surface-solid,#fff);color:var(--ink);padding:12px 14px;text-align:left}.link-list button:hover:not(:disabled){border-color:var(--brand,#168866);background:var(--brand-soft,rgba(22,139,104,.09))}.link-list button:disabled{opacity:.55;cursor:default}.link-empty{display:grid;gap:10px;margin-top:8px}.link-empty p{margin:0;color:var(--copy,#60656a);font-size:14px;font-weight:400;line-height:1.6}.link-empty button{justify-self:start;border:1px solid var(--line,#ccd7dd);border-radius:11px;background:var(--surface-solid,#fff);color:var(--ink);padding:10px 14px}.link-error{color:var(--danger,#b53c32)!important;font-size:13px!important;font-weight:500!important}</style>
