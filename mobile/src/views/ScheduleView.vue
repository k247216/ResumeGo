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
import { eventsOnDate, timelineDates } from '../data/timeline'
import { addToDeviceCalendar } from '../data/calendar'

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

const nowDate = computed(() => new Date(now.value))
const nowClock = computed(() => hhmm(nowDate.value.toISOString()))
const todayLabel = computed(() => {
  const d = nowDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 · 周${'日一二三四五六'[d.getDay()]}`
})
const timeline = computed(() => timelineDates(nowDate.value, 7))

const editing = ref<ScheduleEvent | null>(null)
const sheetOpen = ref(false)
const calendarSyncing = ref(false)
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
const planEvents = computed(() => listSchedules())
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
function targetOf(id: number | null) { return id == null ? null : listTargets().find((t) => t.id === id) ?? null }
function companyName(ev: ScheduleEvent): string {
  const target = targetOf(ev.jobProjectId)
  if (target) return target.name.split(' · ')[0]
  return ev.title.replace(/\s*(一面|二面|三面|终面|HR面|笔试|面试|跟进).*$/u, '').trim() || ev.title
}
function roleName(ev: ScheduleEvent): string {
  const target = targetOf(ev.jobProjectId)
  return target?.targetRole || SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]
}
function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
function timelineEvents(day: Date): ScheduleEvent[] {
  return eventsOnDate(listSchedules(), day).slice(0, 2)
}
function dayNumber(day: Date): string { return `${day.getMonth() + 1}/${day.getDate()}` }
function weekLabel(day: Date): string { return '日一二三四五六'[day.getDay()] }
function toggleCalendar() { viewMode.value = viewMode.value === 'agenda' ? 'month' : 'agenda' }
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
async function syncToCalendar() {
  if (!editing.value) { toast('先保存日程，再添加到手机日历'); return }
  const event = listSchedules().find((item) => item.id === editing.value?.id)
  if (!event) return
  calendarSyncing.value = true
  try {
    const mode = await addToDeviceCalendar(event)
    toast(mode === 'shared' ? '已打开系统分享，可选择手机日历' : '已下载日历文件，可用手机日历打开')
  } catch { toast('暂时无法连接手机日历，请稍后重试') }
  finally { calendarSyncing.value = false }
}
</script>

<template>
  <div>
    <header class="page-head schedule-head">
      <div class="schedule-now">
        <span class="schedule-date">{{ todayLabel }}</span>
        <span class="schedule-clock">{{ nowClock }}</span>
      </div>
      <button class="icon-btn schedule-calendar" :aria-label="viewMode === 'agenda' ? '打开月历' : '返回日程'" @click="toggleCalendar">
        <AppIcon name="calendar" :size="19" />
      </button>
    </header>

    <template v-if="viewMode === 'agenda'">
      <section class="timeline-block" aria-labelledby="timeline-title">
        <div class="section-head">
          <div>
            <h2 id="timeline-title">接下来 7 天</h2>
            <p>横向滑动查看安排</p>
          </div>
          <button class="inline-action" @click="viewMode = 'month'">查看全部 <AppIcon name="chevronRight" :size="15" /></button>
        </div>
        <div class="timeline-scroll" tabindex="0" aria-label="接下来七天的日程时间轴">
          <div class="timeline-rail">
            <div v-for="day in timeline" :key="day.toDateString()" class="timeline-day" :class="{ today: isSameDay(day, nowDate) }">
              <div class="timeline-date"><strong>{{ isSameDay(day, nowDate) ? '今天' : dayNumber(day) }}</strong><small>周{{ weekLabel(day) }}</small></div>
              <div class="timeline-axis"><span class="timeline-node" /></div>
              <div class="timeline-events">
                <button v-for="ev in timelineEvents(day)" :key="ev.id" class="timeline-event" :aria-label="`${companyName(ev)} ${fullWhen(ev.startTime)}`" @click="openEdit(ev)">
                  <span class="timeline-mark">
                    <img v-if="companyMark(companyName(ev)).icon" :src="companyMark(companyName(ev)).icon" alt="">
                    <span v-else :style="{ background: companyMark(companyName(ev)).color, color: companyMark(companyName(ev)).lightText ? '#fff' : '#171717' }">{{ companyMark(companyName(ev)).letter }}</span>
                  </span>
                  <span class="timeline-event-copy">
                    <strong>{{ companyName(ev) }}</strong>
                    <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }} · {{ hhmm(ev.startTime) }}</small>
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="plan-block" aria-labelledby="plan-title">
        <div class="section-head">
          <h2 id="plan-title">面试计划</h2>
          <button class="inline-action add-action" @click="openCreate"><AppIcon name="plus" :size="15" /> 添加日程</button>
        </div>
        <div v-if="planEvents.length" class="plan-list">
          <button v-for="(ev, i) in planEvents" :key="ev.id" class="plan-row" :class="{ selected: nextUp?.id === ev.id }" :style="{ '--i': Math.min(i, 8) }" @click="openEdit(ev)">
            <span class="plan-mark">
              <img v-if="companyMark(companyName(ev)).icon" :src="companyMark(companyName(ev)).icon" alt="">
              <span v-else :style="{ background: companyMark(companyName(ev)).color, color: companyMark(companyName(ev)).lightText ? '#fff' : '#171717' }">{{ companyMark(companyName(ev)).letter }}</span>
            </span>
            <span class="plan-copy">
              <strong>{{ companyName(ev) }}</strong>
              <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
              <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
            </span>
            <AppIcon name="chevronRight" :size="18" class="plan-chev" />
          </button>
        </div>
        <EmptyState v-else icon="calendar" title="还没有面试计划" hint="添加一场真实面试，日程和提醒会在本机保存。" />
      </section>
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
      <div v-if="selectedEvents.length" class="plan-list calendar-plan-list">
        <button v-for="(ev, i) in selectedEvents" :key="ev.id" class="plan-row" :style="{ '--i': i }" @click="openEdit(ev)">
          <span class="plan-mark">
            <img v-if="companyMark(companyName(ev)).icon" :src="companyMark(companyName(ev)).icon" alt="">
            <span v-else :style="{ background: companyMark(companyName(ev)).color, color: companyMark(companyName(ev)).lightText ? '#fff' : '#171717' }">{{ companyMark(companyName(ev)).letter }}</span>
          </span>
          <span class="plan-copy">
            <strong>{{ companyName(ev) }}</strong>
            <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
            <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
          </span>
          <AppIcon name="chevronRight" :size="18" class="plan-chev" />
        </button>
      </div>
      <div v-else class="list">
        <EmptyState v-if="!selectedEvents.length" icon="calendar" title="该日暂无日程" hint="换一个日期，或返回日程页添加安排。" />
      </div>
    </template>

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
      <button v-if="editing" class="calendar-link" :disabled="calendarSyncing" @click="syncToCalendar">
        <AppIcon name="calendar" :size="16" />
        <span>{{ calendarSyncing ? '准备中…' : '添加到手机日历' }}</span>
        <AppIcon name="external" :size="14" />
      </button>
      <div class="sheet-actions">
        <button v-if="editing" class="btn-danger" @click="remove"><AppIcon name="trash" :size="16" /></button>
        <button class="btn-ghost" @click="sheetOpen = false">取消</button>
        <button class="btn-primary" @click="submit">保存</button>
      </div>
    </Sheet>
  </div>
</template>
