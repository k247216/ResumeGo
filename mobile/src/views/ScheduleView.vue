<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import PickerField from '../components/PickerField.vue'
import Sheet from '../components/Sheet.vue'
import EmptyState from '../components/EmptyState.vue'
import { companyMark } from '../constants/companyBrands'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import {
  createSchedule, deleteSchedule, getReminder, listSchedules, listTargets,
  setReminder, updateSchedule,
} from '../data/store'
import { requestNotificationPermission, scheduleReminder, cancelReminder } from '../data/notifications'
import type { ScheduleEvent, ScheduleEventType } from '../types/schedule'
import { SCHEDULE_EVENT_TYPE_LABELS, SCHEDULE_EVENT_TYPE_COLORS } from '../types/schedule'
import { TARGET_STAGE_LABELS, normalizeTargetStage } from '../types/project'

const TYPES: ScheduleEventType[] = ['interview', 'exam', 'followup', 'other']
const WEEK = ['日', '一', '二', '三', '四', '五', '六']
const REMIND_OPTIONS = [
  { value: 0, label: '不提醒' },
  { value: 10, label: '提前 10 分钟' },
  { value: 30, label: '提前 30 分钟' },
  { value: 60, label: '提前 1 小时' },
  { value: 1440, label: '提前 1 天' },
]
const typeOptions = TYPES.map((t) => ({ value: t, label: SCHEDULE_EVENT_TYPE_LABELS[t] }))
const remindOptions = REMIND_OPTIONS.map((o) => ({ value: o.value, label: o.label }))
const targetOptions = computed(() => listTargets().map((t) => ({
  value: t.id, label: t.name, hint: TARGET_STAGE_LABELS[normalizeTargetStage(t.stage)],
})))

const viewMode = ref<'agenda' | 'month'>('agenda')
const now0 = new Date()
const monthCursor = ref({ y: now0.getFullYear(), m: now0.getMonth() })
const selectedDay = ref(new Date().toDateString())

// 实时倒计时：每 30s 刷新一次“下一场”卡片
const now = ref(Date.now())
const timer = setInterval(() => { now.value = Date.now() }, 30_000)
onBeforeUnmount(() => clearInterval(timer))

const editing = ref<ScheduleEvent | null>(null)
const sheetOpen = ref(false)
const form = ref({
  title: '', eventType: 'interview' as ScheduleEventType,
  start: '', end: '', notes: '', reminder: 30, jobProjectId: null as number | null,
})

function toLocalInput(isoStr: string | null): string {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function fromLocalInput(v: string): string | null {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d.toISOString()
}

const nextUp = computed<ScheduleEvent | null>(() =>
  listSchedules().find((e) => new Date(e.startTime).getTime() >= now.value - 3_600_000) ?? null,
)
const grouped = computed(() => {
  const map = new Map<string, ScheduleEvent[]>()
  for (const ev of listSchedules()) {
    const key = new Date(ev.startTime).toDateString()
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(ev)
  }
  return [...map.entries()]
})
const eventsByDay = computed(() => new Map(grouped.value))

interface CalCell { key: string; day: number; other: boolean }
const calCells = computed<CalCell[]>(() => {
  const { y, m } = monthCursor.value
  const startWeekday = new Date(y, m, 1).getDay()
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const prevDays = new Date(y, m, 0).getDate()
  const cells: CalCell[] = []
  for (let i = startWeekday - 1; i >= 0; i--) cells.push({ key: new Date(y, m - 1, prevDays - i).toDateString(), day: prevDays - i, other: true })
  for (let d = 1; d <= daysInMonth; d++) cells.push({ key: new Date(y, m, d).toDateString(), day: d, other: false })
  const tail = (7 - (cells.length % 7)) % 7
  for (let d = 1; d <= tail; d++) cells.push({ key: new Date(y, m + 1, d).toDateString(), day: d, other: true })
  return cells
})
const monthLabel = computed(() => `${monthCursor.value.y} 年 ${monthCursor.value.m + 1} 月`)
const selectedEvents = computed(() => eventsByDay.value.get(selectedDay.value) ?? [])
function dotsOf(key: string): string[] {
  return (eventsByDay.value.get(key) ?? []).slice(0, 3).map((e) => SCHEDULE_EVENT_TYPE_COLORS[e.eventType])
}
function shiftMonth(delta: number) {
  const d = new Date(monthCursor.value.y, monthCursor.value.m + delta, 1)
  monthCursor.value = { y: d.getFullYear(), m: d.getMonth() }
}

function dayLabel(key: string): string {
  const d = new Date(key)
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const diff = Math.round((new Date(d.toDateString()).getTime() - today.getTime()) / 86400000)
  const week = '日一二三四五六'[d.getDay()]
  const base = `${d.getMonth() + 1}月${d.getDate()}日 周${week}`
  if (diff === 0) return `今天 · ${base}`
  if (diff === 1) return `明天 · ${base}`
  return base
}
function hhmm(isoStr: string): string {
  const d = new Date(isoStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function fullWhen(isoStr: string): string {
  const d = new Date(isoStr)
  return `${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]} ${hhmm(isoStr)}`
}
function untilText(isoStr: string): string {
  const t = new Date(isoStr).getTime() - now.value
  if (t <= 0) return '进行中'
  const min = Math.floor(t / 60000), h = Math.floor(min / 60), d = Math.floor(h / 24)
  if (d >= 1) return `${d} 天 ${h % 24} 小时后`
  if (h >= 1) return `${h} 小时 ${min % 60} 分后`
  return `${min} 分钟后`
}
function targetOf(id: number | null) { return id == null ? null : listTargets().find((t) => t.id === id) ?? null }
function targetName(id: number | null): string { return targetOf(id)?.name ?? '' }
function reminderLabel(id: number): string {
  const m = getReminder(id)
  if (!m) return ''
  return REMIND_OPTIONS.find((o) => o.value === m)?.label ?? `${m} 分钟前`
}

function openCreate() {
  editing.value = null
  const d = new Date(); d.setHours(d.getHours() + 1, 0, 0, 0)
  form.value = { title: '', eventType: 'interview', start: toLocalInput(d.toISOString()), end: '', notes: '', reminder: 30, jobProjectId: null }
  sheetOpen.value = true
}
function openEdit(ev: ScheduleEvent) {
  editing.value = ev
  form.value = {
    title: ev.title, eventType: ev.eventType,
    start: toLocalInput(ev.startTime), end: toLocalInput(ev.endTime),
    notes: ev.notes ?? '', reminder: getReminder(ev.id), jobProjectId: ev.jobProjectId,
  }
  sheetOpen.value = true
}

async function submit() {
  if (!form.value.title.trim()) { toast('请填写日程标题'); return }
  const start = fromLocalInput(form.value.start)
  if (!start) { toast('请选择开始时间'); return }
  const payload = {
    title: form.value.title.trim(), eventType: form.value.eventType, startTime: start,
    endTime: fromLocalInput(form.value.end), notes: form.value.notes || null,
    jobDescriptionId: null, jobProjectId: form.value.jobProjectId,
  }
  let id: number
  if (editing.value) { updateSchedule(editing.value.id, payload); id = editing.value.id }
  else { id = createSchedule(payload).id }
  setReminder(id, form.value.reminder)
  if (form.value.reminder > 0) {
    const granted = await requestNotificationPermission()
    if (!granted) toast('未获通知权限，提醒仅会话内生效')
    const ev = listSchedules().find((e) => e.id === id)
    if (ev) await scheduleReminder(ev, form.value.reminder)
  } else { cancelReminder(id) }
  sheetOpen.value = false
  toast(editing.value ? '日程已更新' : '日程已创建')
}
async function remove() {
  if (!editing.value) return
  const ev = editing.value
  const ok = await confirmAction({
    title: `删除「${ev.title}」？`,
    message: '该日程及其提醒会被移除。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  cancelReminder(ev.id)
  deleteSchedule(ev.id)
  sheetOpen.value = false
  toast('日程已删除')
}
</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">日程</h1>
        <p class="page-sub">面试 / 笔试 / 跟进 · 本地自动提醒</p>
      </div>
      <div class="view-switch" role="tablist" aria-label="视图切换">
        <button role="tab" :aria-selected="viewMode === 'agenda'" :class="{ on: viewMode === 'agenda' }" @click="viewMode = 'agenda'">议程</button>
        <button role="tab" :aria-selected="viewMode === 'month'" :class="{ on: viewMode === 'month' }" @click="viewMode = 'month'">月历</button>
      </div>
    </header>

    <template v-if="viewMode === 'agenda'">
      <!-- 下一场：倒计时英雄卡 -->
      <section v-if="nextUp" class="next-card" :style="{ '--nc': SCHEDULE_EVENT_TYPE_COLORS[nextUp.eventType] }" @click="openEdit(nextUp)">
        <div class="nc-top">
          <span class="nc-kicker"><AppIcon name="clock" :size="13" /> 下一场 · {{ SCHEDULE_EVENT_TYPE_LABELS[nextUp.eventType] }}</span>
          <span class="nc-countdown">{{ untilText(nextUp.startTime) }}</span>
        </div>
        <div class="nc-main">
          <img v-if="companyMark(targetName(nextUp.jobProjectId)).icon && nextUp.jobProjectId" class="nc-logo" :src="companyMark(targetName(nextUp.jobProjectId)).icon" alt="">
          <span v-else class="nc-logo letter" :style="{ background: SCHEDULE_EVENT_TYPE_COLORS[nextUp.eventType] }">{{ nextUp.title[0] }}</span>
          <div class="nc-copy">
            <h3>{{ nextUp.title }}</h3>
            <small>{{ fullWhen(nextUp.startTime) }}<template v-if="targetName(nextUp.jobProjectId)"> · {{ targetName(nextUp.jobProjectId) }}</template></small>
          </div>
        </div>
        <div class="nc-foot">
          <span v-if="reminderLabel(nextUp.id)" class="nc-chip"><AppIcon name="bell" :size="13" /> {{ reminderLabel(nextUp.id) }}</span>
          <span v-if="nextUp.notes" class="nc-chip"><AppIcon name="mapPin" :size="13" /> {{ nextUp.notes }}</span>
        </div>
      </section>

      <p v-if="grouped.length" class="section-kicker">全部日程</p>
      <template v-for="[day, events] in grouped" :key="day">
        <p class="day-head"><span :class="{ today: day === new Date().toDateString() }">{{ dayLabel(day) }}</span></p>
        <div class="list">
          <article v-for="(ev, i) in events" :key="ev.id" class="card event-card" :style="{ '--i': i, '--nc': SCHEDULE_EVENT_TYPE_COLORS[ev.eventType] }" @click="openEdit(ev)">
            <div class="ec-band" aria-hidden="true" />
            <div class="ec-time"><strong>{{ hhmm(ev.startTime) }}</strong><small>{{ ev.endTime ? hhmm(ev.endTime) : '—' }}</small></div>
            <div class="event-body">
              <h4>{{ ev.title }}</h4>
              <small>
                {{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}
                <template v-if="targetName(ev.jobProjectId)"> · {{ targetName(ev.jobProjectId) }}</template>
              </small>
            </div>
            <AppIcon v-if="getReminder(ev.id)" name="bell" :size="15" class="ec-bell" />
            <AppIcon name="chevronRight" :size="17" class="ec-chev" />
          </article>
        </div>
      </template>
      <EmptyState v-if="!grouped.length" icon="calendar" title="还没有日程" hint="点右下角 ＋，添加面试、笔试或跟进提醒。" />
    </template>

    <template v-else>
      <div class="cal-head">
        <strong>{{ monthLabel }}</strong>
        <div class="cal-nav">
          <button class="icon-btn" aria-label="上个月" @click="shiftMonth(-1)"><AppIcon name="chevronLeft" :size="18" /></button>
          <button class="icon-btn" aria-label="下个月" @click="shiftMonth(1)"><AppIcon name="chevronRight" :size="18" /></button>
        </div>
      </div>
      <div class="cal-week"><span v-for="w in WEEK" :key="w">{{ w }}</span></div>
      <div class="cal-grid">
        <button
          v-for="cell in calCells" :key="cell.key"
          class="cal-cell"
          :class="{ other: cell.other, today: cell.key === new Date().toDateString(), selected: cell.key === selectedDay }"
          @click="selectedDay = cell.key"
        >{{ cell.day }}<span class="cal-dots"><i v-for="(c, i) in dotsOf(cell.key)" :key="i" :style="{ background: c }" /></span></button>
      </div>
      <p class="day-head"><span :class="{ today: selectedDay === new Date().toDateString() }">{{ dayLabel(selectedDay) }}</span></p>
      <div class="list">
        <article v-for="(ev, i) in selectedEvents" :key="ev.id" class="card event-card" :style="{ '--i': i, '--nc': SCHEDULE_EVENT_TYPE_COLORS[ev.eventType] }" @click="openEdit(ev)">
          <div class="ec-band" aria-hidden="true" />
          <div class="ec-time"><strong>{{ hhmm(ev.startTime) }}</strong><small>{{ ev.endTime ? hhmm(ev.endTime) : '—' }}</small></div>
          <div class="event-body"><h4>{{ ev.title }}</h4><small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}</small></div>
          <AppIcon name="chevronRight" :size="17" class="ec-chev" />
        </article>
        <EmptyState v-if="!selectedEvents.length" icon="calendar" title="该日暂无日程" hint="换一个日期，或点右下角 ＋ 新建。" />
      </div>
    </template>

    <button class="fab" aria-label="新建日程" @click="openCreate"><AppIcon name="plus" :size="24" /></button>

    <Sheet v-if="sheetOpen" :title="editing ? '编辑日程' : '新建日程'" @close="sheetOpen = false">
      <div class="field"><label>标题</label><input v-model="form.title" placeholder="如：字节跳动 一面"></div>
      <div class="field">
        <PickerField
          :model-value="form.eventType"
          :options="typeOptions"
          label="类型"
          title="选择类型"
          @update:model-value="(v) => form.eventType = (v as ScheduleEventType)"
        />
      </div>
      <div class="field"><label>开始时间</label><input v-model="form.start" type="datetime-local"></div>
      <div class="field"><label>结束时间（可选）</label><input v-model="form.end" type="datetime-local"></div>
      <div class="field">
        <PickerField
          :model-value="form.jobProjectId"
          :options="targetOptions"
          label="关联求职目标"
          title="选择求职目标"
          placeholder="不关联"
          clearable
          clear-label="不关联"
          searchable
          icon="target"
          @update:model-value="(v) => form.jobProjectId = (v as number | null)"
        />
      </div>
      <div class="field">
        <PickerField
          :model-value="form.reminder"
          :options="remindOptions"
          label="提醒"
          title="设置提醒"
          icon="bell"
          @update:model-value="(v) => form.reminder = Number(v)"
        />
      </div>
      <div class="field"><label>备注</label><textarea v-model="form.notes" placeholder="会议链接 / 注意事项…"></textarea></div>
      <div class="sheet-actions">
        <button v-if="editing" class="btn-danger" @click="remove"><AppIcon name="trash" :size="16" /></button>
        <button class="btn-ghost" @click="sheetOpen = false">取消</button>
        <button class="btn-primary" @click="submit">保存</button>
      </div>
    </Sheet>
  </div>
</template>
