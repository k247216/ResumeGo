<script setup lang="ts">
import { computed, ref } from 'vue'
import TargetCard from '../components/TargetCard.vue'
import Sheet from '../components/Sheet.vue'
import StagePipeline from '../components/StagePipeline.vue'
import PickerField from '../components/PickerField.vue'
import AppIcon from '../components/AppIcon.vue'
import EmptyState from '../components/EmptyState.vue'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import {
  createTarget, currentVersionOf, deleteTarget, linkResume, listResumes, listTargets,
  interviewRoundOf, interviewRoundsOf, outcomeLabelOf, renameTarget, resumeLabel, setInterviewRound,
  setInterviewRounds, setStage, setTargetOutcome, setTargetStatus, stageEventsOf, updateApplication,
} from '../data/store'
import type { JobProject, TargetOutcome, TargetStage } from '../types/project'
import { TARGET_OUTCOME_LABELS, TARGET_STAGE_LABELS, normalizeTargetStage } from '../types/project'

const search = ref('')
const filter = ref<'all' | TargetStage | 'outcome' | 'archived'>('all')
const layoutMode = ref<1 | 2>(1)

const createOpen = ref(false)
const newName = ref('')
const newRole = ref('')
const newLocation = ref('')

const menuTarget = ref<JobProject | null>(null)
const detailTarget = ref<JobProject | null>(null)
const renameOpen = ref(false)
const renameValue = ref('')

const appForm = ref({ industry: '', role: '', location: '', notes: '' })

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

function onChangeStage(t: JobProject, stage: TargetStage) {
  const res = setStage(t.id, stage)
  toast(res.ok ? `已推进到「${TARGET_STAGE_LABELS[stage]}」` : (res.message ?? '操作失败'))
}

function onChangeInterviewRound(t: JobProject, round: number) {
  if (normalizeTargetStage(t.stage) !== 'interview') {
    const res = setStage(t.id, 'interview')
    if (!res.ok) { toast(res.message ?? '操作失败'); return }
  }
  setInterviewRound(t.id, round)
  toast(`当前进度：第 ${round} 面`)
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
  setTargetStatus(t.id, t.status === 'archived' ? 'active' : 'archived')
  menuTarget.value = null
  toast(t.status === 'archived' ? '已恢复' : '已归档')
}
async function doDelete(t: JobProject) {
  const ok = await confirmAction({
    title: `删除「${t.name}」？`,
    message: '该计划及其阶段记录会被移除，且不可恢复。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  deleteTarget(t.id)
  menuTarget.value = null
  detailTarget.value = null
  toast('已删除')
}

const resumeOptions = computed(() =>
  listResumes().flatMap((r) => {
    const v = currentVersionOf(r.id)
    return v ? [{ value: v.id, label: `${r.title} · V${v.versionNo}` }] : []
  }),
)
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
  linkResume(detailTarget.value.id, versionId)
  toast(versionId ? '已绑定简历版本' : '已解除绑定')
}
</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">求职目标</h1>
        <p class="page-sub">以公司为单位管理进度 · {{ countOf('all') }} 个计划</p>
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
        :locked="detailTarget.status === 'archived'"
        :interview-rounds="interviewRoundsOf(detailTarget)"
        :interview-round="interviewRoundOf(detailTarget)"
        @change="(s) => onChangeStage(detailTarget!, s)"
        @round="(r) => onChangeInterviewRound(detailTarget!, r)"
      />

      <p class="section-kicker">面试轮次</p>
      <PickerField
        :model-value="interviewRoundsOf(detailTarget)"
        :options="roundOptions"
        label="选择面试轮次"
        title="设置面试轮次"
        icon="target"
        @update:model-value="(v) => setInterviewRounds(detailTarget!.id, Number(v))"
      />

      <p class="section-kicker">结果标记</p>
      <PickerField
        :model-value="detailTarget.outcome ?? null"
        :options="outcomeOptions"
        label="记录这次投递的结果"
        title="选择结果标记"
        placeholder="尚未标记"
        clearable
        clear-label="尚未标记"
        icon="target"
        @update:model-value="(v) => setTargetOutcome(detailTarget!.id, v as TargetOutcome | null, interviewRoundOf(detailTarget!))"
      />
      <p v-if="detailTarget.outcome" class="chip-meta" style="margin-top:6px">当前结果：{{ outcomeLabelOf(detailTarget) }}</p>

      <p class="section-kicker">阶段时间轴</p>
      <div class="list">
        <div v-for="ev in [...stageEventsOf(detailTarget.id)].reverse()" :key="ev.id" class="setting-row" style="cursor: default">
          <span class="event-type-dot" :style="{ background: 'var(--brand)' }" />
          <span class="s-label">{{ TARGET_STAGE_LABELS[ev.stage] }}</span>
          <span class="s-value">{{ new Date(ev.occurredAt).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }}</span>
        </div>
      </div>

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
  </div>
</template>
