<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import TargetCard from '../components/TargetCard.vue'
import Sheet from '../components/Sheet.vue'
import StagePipeline from '../components/StagePipeline.vue'
import PickerField from '../components/PickerField.vue'
import AppIcon from '../components/AppIcon.vue'
import EmptyState from '../components/EmptyState.vue'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import {
  createTarget, createMilestone, currentVersionOf, deleteMilestone, deleteTarget, getReminder, linkResume, listMilestones, listResumes, listSchedules, listStageEvents, listTargets,
  interviewRoundOf, interviewRoundsOf, outcomeLabelOf, renameTarget, reopenTarget, resumeLabel, restoreTarget, saveMilestoneImage,
  setInterviewRound, setInterviewRounds, setStage, setTargetOutcome, setTargetStatus, snapshotTarget, updateMilestone,
  stageEventsOf, schedulesOfTarget, updateApplication,
} from '../data/store'
import type { TargetSnapshot } from '../data/store'
import { deleteFile, objectUrl } from '../data/fileStore'
import { computeFunnel } from '../data/funnel'
import { cancelReminder } from '../data/notifications'
import { headEllipsis } from '../data/resumeFile'
import { SCHEDULE_EVENT_TYPE_COLORS } from '../types/schedule'
import type { JobProject, Milestone, MilestoneKind, TargetOutcome, TargetStage } from '../types/project'
import { MILESTONE_KINDS, TARGET_OUTCOME_LABELS, TARGET_STAGE_COLORS, TARGET_STAGE_LABELS, isTerminalStage, normalizeTargetStage, stageFlowRank } from '../types/project'

const search = ref('')
const filter = ref<'all' | TargetStage | 'outcome' | 'archived'>('all')
const layoutMode = ref<1 | 2>(1)
const statsOpen = ref(false)

/** 漏斗按全部目标（含归档）统计——被拒和放弃也是转化链路上的真实终点。 */
const funnel = computed(() => computeFunnel(listTargets(), listStageEvents(), listSchedules()))
const funnelMax = computed(() => Math.max(1, ...funnel.value.rows.map((r) => r.reached)))
function barWidth(reached: number): string {
  return `${reached ? Math.max(4, (reached / funnelMax.value) * 100) : 0}%`
}

const createOpen = ref(false)
const newName = ref('')
const newRole = ref('')
const newLocation = ref('')

const menuTarget = ref<JobProject | null>(null)
const detailTarget = ref<JobProject | null>(null)
const renameOpen = ref(false)
const renameValue = ref('')

const appForm = ref({ industry: '', role: '', location: '', notes: '' })
const detailLocked = computed(() => {
  const target = detailTarget.value
  return !!target && (target.status === 'archived' || isTerminalStage(normalizeTargetStage(target.stage)))
})
const terminalLock = computed(() => {
  const target = detailTarget.value
  return !!target && isTerminalStage(normalizeTargetStage(target.stage))
})
const detailSchedules = computed(() => (detailTarget.value ? schedulesOfTarget(detailTarget.value.id) : []))

const FILTERS: Array<{ key: typeof filter.value; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'applied', label: TARGET_STAGE_LABELS.applied },
  { key: 'exam', label: TARGET_STAGE_LABELS.exam },
  { key: 'interview', label: TARGET_STAGE_LABELS.interview },
  { key: 'hr', label: TARGET_STAGE_LABELS.hr },
  { key: 'offer', label: 'Offer' },
  { key: 'outcome', label: '已有结果' },
  { key: 'archived', label: '已归档' },
]

const OUTCOMES: TargetStage[] = ['pool', 'screened_out', 'rejected', 'closed']

function countOf(key: typeof filter.value): number {
  const all = listTargets()
  if (key === 'all') return all.length
  if (key === 'archived') return all.filter((t) => t.status === 'archived').length
  if (key === 'outcome') return all.filter((t) => t.status === 'active' && OUTCOMES.includes(normalizeTargetStage(t.stage))).length
  return all.filter((t) => t.status === 'active' && normalizeTargetStage(t.stage) === key).length
}

const visible = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return listTargets().filter((t) => {
    if (filter.value === 'archived') { if (t.status !== 'archived') return false }
    else if (filter.value === 'outcome') { if (t.status !== 'active' || !OUTCOMES.includes(normalizeTargetStage(t.stage))) return false }
    else if (filter.value !== 'all') { if (t.status !== 'active' || normalizeTargetStage(t.stage) !== filter.value) return false }
    if (keyword && !t.name.toLowerCase().includes(keyword)) return false
    return true
  })
})

function stageTimesOf(t: JobProject) {
  const map: Partial<Record<TargetStage, string>> = {}
  for (const ev of stageEventsOf(t.id)) {
    const d = new Date(ev.occurredAt)
    map[ev.stage] = `${d.getMonth() + 1}月${d.getDate()}日`
  }
  return map
}

function formatEventTime(value: string): string {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function reminderNoteOf(scheduleId: number): string {
  const minutes = getReminder(scheduleId)
  return minutes > 0 ? ` · 提前 ${minutes} 分钟` : ' · 无提醒'
}

function openCreate() { newName.value = ''; newRole.value = ''; newLocation.value = ''; createOpen.value = true }
function submitCreate() {
  const name = newName.value.trim()
  if (!name) { toast('请填写目标名称'); return }
  createTarget(name, {})
  if (newRole.value || newLocation.value) {
    const created = listTargets()[0]
    updateApplication(created.id, { role: newRole.value || null, location: newLocation.value || null })
  }
  createOpen.value = false
  toast('已创建求职目标')
}

/**
 * 改完就给一次「撤销」。这里不搭 undo 栈：一次改动对应一份快照，用户只关心刚点错的那一下，
 * 而快照能把阶段、轮次、结果标记、归档状态一起还原，比逐个写逆操作更不容易漏。
 */
function doneWithUndo(snap: TargetSnapshot | null, message: string) {
  if (!snap) { toast(message); return }
  toast(message, {
    label: '撤销',
    run: () => { toast(restoreTarget(snap) ? '已撤销' : '该计划已被删除，无法撤销') },
  })
}
function movedBackward(from: TargetStage, to: TargetStage): boolean {
  const cur = stageFlowRank(from)
  const next = stageFlowRank(to)
  return cur > 0 && next > 0 && next < cur
}
function outcomeText(t: JobProject, value: TargetOutcome): string {
  if (value === 'interview_failed') return `面试第 ${interviewRoundOf(t)} 面未通过`
  return TARGET_OUTCOME_LABELS[value]
}

async function onChangeStage(t: JobProject, stage: TargetStage) {
  const cur = normalizeTargetStage(t.stage)
  const back = movedBackward(cur, stage)
  if (isTerminalStage(stage) && cur !== stage) {
    const ok = await confirmAction({
      title: `标记为「${TARGET_STAGE_LABELS[stage]}」？`,
      message: '这是终态，确认后阶段、面试轮次和结果标记都会被锁定，需要手动解锁才能继续编辑。',
      confirmLabel: '确认锁定',
    })
    if (!ok) return
  } else if (back) {
    const ok = await confirmAction({
      title: `退回「${TARGET_STAGE_LABELS[stage]}」？`,
      message: '进度会退回这一步，之后的阶段要重新点一次。退回后仍然可以撤销。',
      confirmLabel: '退回',
    })
    if (!ok) return
  }
  const snap = snapshotTarget(t.id)
  const res = setStage(t.id, stage, { allowBackward: back })
  if (!res.ok) { toast(res.message ?? '操作失败'); return }
  doneWithUndo(snap, `${back ? '已退回' : '已推进到'}「${TARGET_STAGE_LABELS[stage]}」`)
}

function onPickRounds(t: JobProject, rounds: number) {
  const snap = snapshotTarget(t.id)
  const res = setInterviewRounds(t.id, rounds)
  if (!res.ok) { toast(res.message ?? '操作失败'); return }
  doneWithUndo(snap, `面试总轮次设为 ${rounds} 轮`)
}

async function onPickOutcome(t: JobProject, value: TargetOutcome | null) {
  if (value) {
    const ok = await confirmAction({
      title: `标记为「${outcomeText(t, value)}」？`,
      message: '标记结果会把这条计划推进到终态并锁定，之后要手动解锁才能继续改流程。',
      confirmLabel: '标记',
    })
    if (!ok) return
  }
  const snap = snapshotTarget(t.id)
  const res = setTargetOutcome(t.id, value, interviewRoundOf(t))
  if (!res.ok) { toast(res.message ?? '操作失败'); return }
  doneWithUndo(snap, value ? `已标记「${outcomeLabelOf(t)}」` : '已清除结果标记')
}

async function doReopen(t: JobProject) {
  const ok = await confirmAction({
    title: '解除锁定，继续编辑？',
    message: '阶段会回到锁定前的位置，结果标记会被清除，之后可以继续推进流程。',
    confirmLabel: '解除锁定',
  })
  if (!ok) return
  const snap = snapshotTarget(t.id)
  const res = reopenTarget(t.id)
  if (!res.ok) { toast(res.message ?? '操作失败'); return }
  doneWithUndo(snap, `已解锁，回到「${TARGET_STAGE_LABELS[res.stage ?? 'applied']}」`)
}

function onChangeInterviewRound(t: JobProject, round: number) {
  const snap = snapshotTarget(t.id)
  if (normalizeTargetStage(t.stage) !== 'interview') {
    const res = setStage(t.id, 'interview')
    if (!res.ok) { toast(res.message ?? '操作失败'); return }
  }
  setInterviewRound(t.id, round)
  doneWithUndo(snap, `当前进度：第 ${round} 面`)
}

function openDetail(t: JobProject) {
  detailTarget.value = t
  appForm.value = { industry: t.industry ?? '', role: t.targetRole ?? '', location: t.location ?? '', notes: t.notes ?? '' }
}
function saveApplication() {
  if (!detailTarget.value) return
  updateApplication(detailTarget.value.id, { ...appForm.value })
  detailTarget.value = null
  toast('投递信息已保存')
}
function openMenu(t: JobProject) { menuTarget.value = t }
function doRename() {
  if (!menuTarget.value) return
  renameValue.value = menuTarget.value.name
  renameOpen.value = true
}
function submitRename() {
  if (!menuTarget.value) return
  const v = renameValue.value.trim()
  if (!v) { toast('名称不能为空'); return }
  renameTarget(menuTarget.value.id, v)
  renameOpen.value = false; menuTarget.value = null
  toast('已重命名')
}
function toggleArchive(t: JobProject) {
  const snap = snapshotTarget(t.id)
  const wasArchived = t.status === 'archived'
  setTargetStatus(t.id, wasArchived ? 'active' : 'archived')
  menuTarget.value = null
  doneWithUndo(snap, wasArchived ? '已恢复到进行中' : '已归档，可在筛选「已归档」里找到')
}
async function doDelete(t: JobProject) {
  const related = schedulesOfTarget(t.id)
  const ok = await confirmAction({
    title: `删除「${t.name}」？`,
    message: related.length
      ? `该计划、阶段记录以及关联的 ${related.length} 条日程和提醒会被移除，且不可恢复。`
      : '该计划及其阶段记录会被移除，且不可恢复。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  const { removedScheduleIds } = await deleteTarget(t.id)
  for (const scheduleId of removedScheduleIds) cancelReminder(scheduleId)
  menuTarget.value = null
  detailTarget.value = null
  toast(related.length ? `已删除（含 ${removedScheduleIds.length} 条日程）` : '已删除')
}

const resumeOptions = computed(() =>
  listResumes().flatMap((r) => {
    const v = currentVersionOf(r.id)
    return v ? [{ value: v.id, label: `${headEllipsis(r.title, 14)} · V${v.versionNo}` }] : []
  }),
)

// ── 里程碑（历程 / 荣誉墙）──
const detailMilestones = computed(() => (detailTarget.value ? listMilestones(detailTarget.value.id) : []))
/** 截图本体在 IndexedDB：进详情后按需换 objectURL，键不在列表里就 revoke，避免泄漏。 */
const msUrls = ref<Record<string, string>>({})
async function loadMsUrls() {
  const keys = detailMilestones.value.flatMap((m) => m.images)
  for (const k of keys) {
    if (msUrls.value[k]) continue
    const u = await objectUrl(k)
    if (u) msUrls.value[k] = u
  }
  for (const k of Object.keys(msUrls.value)) {
    if (!keys.includes(k)) { URL.revokeObjectURL(msUrls.value[k]); delete msUrls.value[k] }
  }
}
watch(detailMilestones, loadMsUrls)

const msSheetOpen = ref(false)
const msEditing = ref<Milestone | null>(null)
const msForm = ref({ kind: 'invite' as MilestoneKind, title: '', note: '', occurredAt: '' })
const msNewImages = ref<string[]>([])
const msRemoveImages = ref<string[]>([])
const msUploading = ref(false)
const kindOptions = (Object.keys(MILESTONE_KINDS) as MilestoneKind[]).map((k) => ({ value: k, label: MILESTONE_KINDS[k].label }))

function msDate(value: string): string {
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '—' : `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}
function openMsCreate() {
  msEditing.value = null
  msForm.value = { kind: 'invite', title: '', note: '', occurredAt: new Date().toISOString().slice(0, 10) }
  msNewImages.value = []
  msRemoveImages.value = []
  msSheetOpen.value = true
}
function openMsEdit(ms: Milestone) {
  msEditing.value = ms
  msForm.value = { kind: ms.kind, title: ms.title, note: ms.note, occurredAt: ms.occurredAt.slice(0, 10) }
  msNewImages.value = []
  msRemoveImages.value = []
  msSheetOpen.value = true
}
function toggleRemoveImage(key: string) {
  const i = msRemoveImages.value.indexOf(key)
  if (i >= 0) msRemoveImages.value.splice(i, 1)
  else msRemoveImages.value.push(key)
}
async function onMsFiles(ev: Event) {
  const input = ev.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  msUploading.value = true
  for (const f of files) {
    const key = await saveMilestoneImage(f)
    if (key) {
      msNewImages.value.push(key)
      // 预览直接用本地 File 的 objectURL，不必从 IndexedDB 读回。
      msUrls.value[key] = URL.createObjectURL(f)
    } else {
      toast('有一张截图处理失败，已跳过')
    }
  }
  msUploading.value = false
}
/** 未保存就关掉编辑页时，把已落盘的新截图清掉，不留 IndexedDB 孤儿。 */
function closeMsSheet() {
  for (const k of msNewImages.value) {
    URL.revokeObjectURL(msUrls.value[k] ?? '')
    delete msUrls.value[k]
    void deleteFile(k)
  }
  msNewImages.value = []
  msSheetOpen.value = false
}
function saveMs() {
  if (!detailTarget.value) return
  const title = msForm.value.title.trim()
  if (!title) { toast('请填写标题，如「约面邮件」'); return }
  const day = new Date(`${msForm.value.occurredAt}T12:00:00`)
  const occurredAt = Number.isNaN(day.getTime()) ? new Date().toISOString() : day.toISOString()
  if (msEditing.value) {
    const images = [...msEditing.value.images.filter((k) => !msRemoveImages.value.includes(k)), ...msNewImages.value]
    updateMilestone(msEditing.value.id, { kind: msForm.value.kind, title, note: msForm.value.note, images, occurredAt })
    for (const k of msRemoveImages.value) void deleteFile(k)
    toast('里程碑已更新')
  } else {
    createMilestone({ targetId: detailTarget.value.id, kind: msForm.value.kind, title, note: msForm.value.note, images: [...msNewImages.value], occurredAt })
    toast('已记下一程')
  }
  msNewImages.value = []
  msRemoveImages.value = []
  msSheetOpen.value = false
}
async function removeMilestone(ms: Milestone) {
  const ok = await confirmAction({
    title: `删除「${ms.title}」？`,
    message: '这条里程碑和它的截图会被一起删除，不可恢复。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  await deleteMilestone(ms.id)
  toast('已删除')
}

const viewer = ref<{ ms: Milestone; index: number } | null>(null)
function openViewer(ms: Milestone, index: number) { viewer.value = { ms, index } }
function viewerStep(d: number) {
  if (!viewer.value) return
  const n = viewer.value.ms.images.length
  if (!n) return
  viewer.value = { ms: viewer.value.ms, index: (viewer.value.index + d + n) % n }
}
const roundOptions = [1, 2, 3, 4, 5].map((value) => ({ value, label: `${value} 轮` }))
const outcomeOptions: Array<{ value: TargetOutcome; label: string }> = [
  { value: 'pool', label: TARGET_OUTCOME_LABELS.pool },
  { value: 'exam_failed', label: TARGET_OUTCOME_LABELS.exam_failed },
  { value: 'interview_failed', label: '面试第 N 轮未通过（使用当前面试轮次）' },
  { value: 'hr_failed', label: TARGET_OUTCOME_LABELS.hr_failed },
  { value: 'resume_failed', label: TARGET_OUTCOME_LABELS.resume_failed },
  { value: 'rejected', label: TARGET_OUTCOME_LABELS.rejected },
  { value: 'closed', label: TARGET_OUTCOME_LABELS.closed },
]
function onLinkResume(versionId: number | null) {
  if (!detailTarget.value) return
  const snap = snapshotTarget(detailTarget.value.id)
  linkResume(detailTarget.value.id, versionId)
  doneWithUndo(snap, versionId ? `已绑定 ${resumeLabel(versionId) ?? '该简历版本'}` : '已解除简历绑定')
}
</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">求职目标</h1>
        <p class="page-sub">以公司为单位管理进度 · {{ countOf('all') }} 个计划</p>
      </div>
      <div class="head-actions">
        <button class="icon-btn" aria-label="求职漏斗统计" title="求职漏斗统计" @click="statsOpen = true">
          <AppIcon name="chart" :size="18" />
        </button>
      </div>
      <div class="view-switch" aria-label="目标卡片布局">
        <button :class="{ on: layoutMode === 1 }" aria-label="一列布局" title="一列布局" @click="layoutMode = 1"><AppIcon name="list" :size="16" /></button>
        <button :class="{ on: layoutMode === 2 }" aria-label="两列布局" title="两列布局" @click="layoutMode = 2"><AppIcon name="grid" :size="16" /></button>
      </div>
    </header>

    <div class="toolbar">
      <div class="pill-row">
        <button
          v-for="f in FILTERS"
          :key="f.key"
          class="filter-pill"
          :class="{ on: filter === f.key }"
          @click="filter = f.key"
        >{{ f.label }}<em>{{ countOf(f.key) }}</em></button>
      </div>
      <label class="search-box">
        <AppIcon name="search" :size="15" />
        <input v-model="search" placeholder="搜索公司或岗位…" autocomplete="off">
      </label>
    </div>

    <div class="targets-grid" :class="{ 'is-double': layoutMode === 2 }">
      <TargetCard
        v-for="(t, i) in visible"
        :key="t.id"
        :target="t"
        :index="i"
        :compact="layoutMode === 2"
        :resume-label="resumeLabel(t.resumeVersionId)"
        :stage-times="stageTimesOf(t)"
        @open="openDetail(t)"
        @stage="(s) => onChangeStage(t, s)"
        @round="(r) => onChangeInterviewRound(t, r)"
        @menu="openMenu(t)"
        @link-resume="openDetail(t)"
      />
      <EmptyState
        v-if="!visible.length"
        icon="target"
        :title="search ? '没有匹配的目标' : '还没有求职目标'"
        :hint="search ? '换个关键词，或清除筛选看看全部计划。' : '点右下角 ＋，按公司新建第一个求职计划。'"
      />
    </div>

    <button class="fab" aria-label="新建求职目标" @click="openCreate"><AppIcon name="plus" :size="24" /></button>

    <!-- 新建 -->
    <Sheet v-if="createOpen" title="新建求职目标" @close="createOpen = false">
      <div class="field"><label>目标名称（公司 · 岗位）</label><input v-model="newName" placeholder="如：字节跳动 · 前端开发"></div>
      <div class="field"><label>期望岗位（可选）</label><input v-model="newRole" placeholder="前端 / 后端 / 算法…"></div>
      <div class="field"><label>地点（可选）</label><input v-model="newLocation" placeholder="北京 / 上海…"></div>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="createOpen = false">取消</button>
        <button class="btn-primary" @click="submitCreate">创建</button>
      </div>
    </Sheet>

    <!-- ⋯ 菜单 -->
    <Sheet v-if="menuTarget" :title="menuTarget.name" @close="menuTarget = null">
      <div class="list">
        <button class="setting-row" @click="doRename"><span class="s-label">重命名</span></button>
        <button class="setting-row" @click="toggleArchive(menuTarget!)">
          <span class="s-label">{{ menuTarget.status === 'archived' ? '恢复计划' : '归档计划' }}</span>
        </button>
        <button class="setting-row" style="color: var(--danger)" @click="doDelete(menuTarget!)">
          <span class="s-label" style="color: var(--danger)">删除</span>
        </button>
      </div>
    </Sheet>

    <!-- 重命名 -->
    <Sheet v-if="renameOpen" title="重命名" @close="renameOpen = false">
      <div class="field"><label>新名称</label><input v-model="renameValue"></div>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="renameOpen = false">取消</button>
        <button class="btn-primary" @click="submitRename">保存</button>
      </div>
    </Sheet>

    <!-- 详情 -->
    <Sheet v-if="detailTarget" :title="detailTarget.name" @close="detailTarget = null">
      <StagePipeline
        :stage="normalizeTargetStage(detailTarget.stage)"
        :times="stageTimesOf(detailTarget)"
        :locked="detailLocked"
        :allow-backward="true"
        :interview-rounds="interviewRoundsOf(detailTarget)"
        :interview-round="interviewRoundOf(detailTarget)"
        @change="(s) => onChangeStage(detailTarget!, s)"
        @round="(r) => onChangeInterviewRound(detailTarget!, r)"
      />
      <p v-if="!detailLocked" class="chip-meta">点已完成的阶段可以退回上一步；每次改动都会给一条「撤销」。</p>
      <div v-if="detailLocked" class="lock-note">
        <p class="chip-meta">
          {{ detailTarget.status === 'archived' ? '该计划已归档，恢复后才能继续编辑。' : '该计划已进入终态，阶段、面试轮次和结果标记已锁定。' }}
        </p>
        <button v-if="terminalLock" class="btn-ghost btn-sm" @click="doReopen(detailTarget!)">解除锁定</button>
      </div>

      <p class="section-kicker">面试轮次</p>
      <PickerField
        :model-value="interviewRoundsOf(detailTarget)"
        :options="roundOptions"
        :disabled="detailLocked"
        label="选择面试轮次"
        title="设置面试轮次"
        icon="target"
        @update:model-value="(v) => onPickRounds(detailTarget!, Number(v))"
      />

      <p class="section-kicker">结果标记</p>
      <PickerField
        :model-value="detailTarget.outcome ?? null"
        :options="outcomeOptions"
        :disabled="detailLocked"
        label="记录这次投递的结果"
        title="选择结果标记"
        placeholder="尚未标记"
        clearable
        clear-label="尚未标记"
        icon="target"
        @update:model-value="(v) => onPickOutcome(detailTarget!, v as TargetOutcome | null)"
      />
      <p v-if="detailTarget.outcome" class="chip-meta" style="margin-top:6px">当前结果：{{ outcomeLabelOf(detailTarget) }}</p>

      <p class="section-kicker">关联日程</p>
      <div v-if="detailSchedules.length" class="list">
        <div v-for="ev in detailSchedules" :key="ev.id" class="setting-row" style="cursor: default">
          <span class="event-type-dot" :style="{ background: SCHEDULE_EVENT_TYPE_COLORS[ev.eventType] }" />
          <span class="s-label">{{ ev.title }}</span>
          <span class="s-value">{{ formatEventTime(ev.startTime) }}{{ reminderNoteOf(ev.id) }}</span>
        </div>
      </div>
      <p v-else class="chip-meta">还没有与该计划关联的日程。去「日程」新建时选择这个计划，面试和笔试就会出现在这里。</p>

      <p class="section-kicker">阶段时间轴</p>
      <div class="list">
        <div v-for="ev in [...stageEventsOf(detailTarget.id)].reverse()" :key="ev.id" class="setting-row" style="cursor: default">
          <span class="event-type-dot" :style="{ background: 'var(--brand)' }" />
          <span class="s-label">{{ TARGET_STAGE_LABELS[ev.stage] }}</span>
          <span class="s-value">{{ new Date(ev.occurredAt).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }}</span>
        </div>
      </div>

      <p class="section-kicker">历程 · 荣誉墙</p>
      <div v-if="detailMilestones.length" class="ms-list">
        <div v-for="ms in detailMilestones" :key="ms.id" class="ms-item">
          <span class="ms-dot" :style="{ background: MILESTONE_KINDS[ms.kind].color }" />
          <div class="ms-body">
            <div class="ms-head">
              <span class="ms-kind" :style="{ color: MILESTONE_KINDS[ms.kind].color, borderColor: MILESTONE_KINDS[ms.kind].color }">{{ MILESTONE_KINDS[ms.kind].label }}</span>
              <span class="ms-title">{{ ms.title }}</span>
              <span class="ms-date">{{ msDate(ms.occurredAt) }}</span>
            </div>
            <p v-if="ms.note" class="ms-note">{{ ms.note }}</p>
            <div v-if="ms.images.length" class="ms-thumbs">
              <template v-for="(k, i) in ms.images" :key="k">
                <img v-if="msUrls[k]" :src="msUrls[k]" alt="里程碑截图" @click="openViewer(ms, i)">
                <span v-else class="ms-thumb-loading" />
              </template>
            </div>
            <div class="ms-ops">
              <button class="btn-ghost btn-sm" @click="openMsEdit(ms)">编辑</button>
              <button class="btn-ghost btn-sm" style="color: var(--danger)" @click="removeMilestone(ms)">删除</button>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="chip-meta">把约面邮件、笔试通过、offer 截图存进这条投递的时间线，回头翻就是一面荣誉墙。</p>
      <button class="btn-ghost btn-sm ms-add" @click="openMsCreate">＋ 添加里程碑</button>

      <p class="section-kicker">绑定简历版本</p>
      <PickerField
        :model-value="detailTarget.resumeVersionId"
        :options="resumeOptions"
        title="选择要绑定的简历版本"
        placeholder="未绑定简历"
        clearable
        clear-label="不绑定"
        icon="file"
        @update:model-value="(v) => onLinkResume(v ?? null)"
      />
      <p v-if="!resumeOptions.length" class="chip-meta" style="margin-top:6px">还没有导入简历，去「简历」页上传一份。</p>

      <p class="section-kicker">投递信息</p>
      <div class="field"><label>行业</label><input v-model="appForm.industry"></div>
      <div class="field"><label>期望岗位</label><input v-model="appForm.role"></div>
      <div class="field"><label>地点</label><input v-model="appForm.location"></div>
      <div class="field"><label>备注</label><textarea v-model="appForm.notes"></textarea></div>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="detailTarget = null">关闭</button>
        <button class="btn-primary" @click="saveApplication">保存投递信息</button>
      </div>
    </Sheet>

    <!-- 里程碑编辑 -->
    <Sheet v-if="msSheetOpen" :title="msEditing ? '编辑里程碑' : '添加里程碑'" @close="closeMsSheet">
      <p class="section-kicker">类型</p>
      <PickerField
        :model-value="msForm.kind"
        :options="kindOptions"
        label="选择里程碑类型"
        title="选择类型"
        icon="target"
        @update:model-value="(v) => (msForm.kind = v as MilestoneKind)"
      />
      <div class="field"><label>标题</label><input v-model="msForm.title" placeholder="如：约面邮件 / 二面通过 / Offer call"></div>
      <div class="field"><label>日期</label><input v-model="msForm.occurredAt" type="date"></div>
      <div class="field"><label>备注（可选）</label><textarea v-model="msForm.note" placeholder="值得记住的细节：面试官说了什么、薪资多少…"></textarea></div>
      <p class="section-kicker">截图</p>
      <label class="ms-upload">
        <input type="file" accept="image/*" multiple hidden @change="onMsFiles">
        <AppIcon name="plus" :size="16" />
        <span>{{ msUploading ? '处理中…' : '从相册选择截图（可多选）' }}</span>
      </label>
      <div v-if="msEditing && msEditing.images.length" class="ms-thumbs" style="margin-top:8px">
        <template v-for="k in msEditing.images" :key="k">
          <div class="ms-thumb-wrap" :class="{ removing: msRemoveImages.includes(k) }">
            <img v-if="msUrls[k]" :src="msUrls[k]" alt="已有截图" @click="toggleRemoveImage(k)">
            <span v-if="msRemoveImages.includes(k)" class="ms-thumb-x">将删除</span>
          </div>
        </template>
      </div>
      <div v-if="msNewImages.length" class="ms-thumbs" style="margin-top:8px">
        <div v-for="k in msNewImages" :key="k" class="ms-thumb-wrap new">
          <img :src="msUrls[k]" alt="新截图" @click="msNewImages = msNewImages.filter((x) => x !== k)">
          <span class="ms-thumb-x">点击移除</span>
        </div>
      </div>
      <p class="chip-meta" style="margin-top:6px">截图压缩后存本机，不会上传，也不会包含在 JSON 备份里（与简历文件同理）。</p>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="msSheetOpen = false">取消</button>
        <button class="btn-primary" :disabled="msUploading" @click="saveMs">{{ msEditing ? '保存' : '添加' }}</button>
      </div>
    </Sheet>

    <!-- 全屏看图 -->
    <div v-if="viewer" class="img-viewer" @click.self="viewer = null">
      <img v-if="msUrls[viewer.ms.images[viewer.index]]" :src="msUrls[viewer.ms.images[viewer.index]]" alt="截图大图">
      <div class="img-viewer-bar">
        <button class="iv-nav" aria-label="上一张" @click="viewerStep(-1)">‹</button>
        <span>{{ viewer.ms.title }} · {{ viewer.index + 1 }}/{{ viewer.ms.images.length }}</span>
        <button class="iv-nav" aria-label="下一张" @click="viewerStep(1)">›</button>
      </div>
      <button class="img-viewer-close" aria-label="关闭" @click="viewer = null">✕</button>
    </div>

    <!-- 求职漏斗统计 -->
    <Sheet v-if="statsOpen" title="求职漏斗" @close="statsOpen = false">
      <p class="theme-intro">统计「到达过」每个阶段的目标数——后来被拒的也留在它到过的格子里，这样才算转化率。</p>
      <div class="funnel">
        <div v-for="(row, i) in funnel.rows" :key="row.stage" class="funnel-row" :style="{ '--d': `${i * 70}ms` }">
          <span class="funnel-label">{{ row.label }}</span>
          <div class="funnel-bar-wrap">
            <div class="funnel-bar" :style="{ '--w': barWidth(row.reached), '--c': TARGET_STAGE_COLORS[row.stage] }" />
          </div>
          <span class="funnel-num">
            <strong>{{ row.reached }}</strong>
            <small v-if="row.rate != null">转化 {{ row.rate }}%</small>
            <small v-else>起点</small>
          </span>
        </div>
      </div>
      <p class="section-kicker">结果分布</p>
      <div class="chip-row">
        <span
          v-for="o in funnel.outcomes" :key="o.stage" class="chip"
          :style="o.count ? { borderColor: TARGET_STAGE_COLORS[o.stage], color: TARGET_STAGE_COLORS[o.stage] } : undefined"
          :class="{ 'chip-zero': !o.count }"
        >{{ o.label }} <strong>{{ o.count }}</strong></span>
      </div>
      <p class="chip-meta" style="margin-top:10px">
        共 {{ funnel.total }} 个目标 · 未归档 {{ funnel.active }} 个 · 已出结果 {{ funnel.settled }} 个 · 已归档 {{ funnel.archived }} 个
      </p>
      <div class="sheet-actions">
        <button class="btn-primary" @click="statsOpen = false">关闭</button>
      </div>
    </Sheet>
  </div>
</template>
