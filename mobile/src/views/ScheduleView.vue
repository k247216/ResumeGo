<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import PickerField from '../components/PickerField.vue'
import Sheet from '../components/Sheet.vue'
import EmptyState from '../components/EmptyState.vue'
import { companyMark } from '../constants/companyBrands'
import { headEllipsis } from '../data/resumeFile'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { appendPromptLine } from '../data/prompt'
import {
  createSchedule, deleteSchedule, getReminder, listReviews, listSchedules, listTargets,
  setReminder, setScheduleReview, updateSchedule,
} from '../data/store'
import { requestNotificationPermission, scheduleReminder, cancelReminder, REMINDER_OUTCOME_MESSAGES } from '../data/notifications'
import type { ScheduleEvent, ScheduleEventType } from '../types/schedule'
import { SCHEDULE_EVENT_TYPE_LABELS, SCHEDULE_EVENT_TYPE_COLORS, isEventFinished } from '../types/schedule'
import { TARGET_STAGE_LABELS, normalizeTargetStage } from '../types/project'
import { eventsOnDate, timelineDates } from '../data/timeline'
import { addToDeviceCalendar } from '../data/calendar'
import type { CalendarHandoff } from '../data/calendar'

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

const viewMode = ref<'agenda' | 'month' | 'review'>('agenda')
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
const timeline = computed(() => timelineDates(nowDate.value, 31))

const editing = ref<ScheduleEvent | null>(null)
const sheetOpen = ref(false)
const calendarSyncing = ref(false)
const form = ref({
  title: '', eventType: 'interview' as ScheduleEventType,
  start: '', end: '', notes: '', reminder: 30, jobProjectId: null as number | null,
})
/** notes 只放赛前要看的信息；心得另开一个窗口，避免把「会议链接」和「这轮没答好」写进同一个框里。 */
const REVIEW_MAX = 500
const REVIEW_PROMPTS = ['这轮被问了什么', '哪里没答好', '下一轮要补什么']
const reviewTarget = ref<ScheduleEvent | null>(null)
const reviewDraft = ref('')

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
const nextEvent = computed(() => planEvents.value.find((event) => new Date(event.startTime).getTime() >= now.value - 30 * 60_000) ?? null)
const eventsByDay = computed(() => new Map(grouped.value))

// ── 复盘（心得）──
/** 只有预计结束时刻真的过了才算「已结束」，未来的日程不应该出现写心得的入口。 */
const finishedEvents = computed(() => planEvents.value.filter((ev) => isEventFinished(ev, now.value)))
const reviewedEvents = computed(() => listReviews())
const reviewedFinished = computed(() => finishedEvents.value.filter((ev) => !!ev.review?.trim()))
const pendingReviews = computed(() => finishedEvents.value.filter((ev) => !ev.review?.trim()).reverse())
const reviewCoverage = computed(() => (
  finishedEvents.value.length ? Math.round((reviewedFinished.value.length / finishedEvents.value.length) * 100) : 0
))
/** 头卡的第三行走「时间跨度」这个环形图和百分比都给不出的维度，顺带交代还差几篇。 */
const reviewSpanLabel = computed(() => {
  const stamps = finishedEvents.value.map((ev) => new Date(ev.startTime).getTime()).sort((a, b) => a - b)
  if (!stamps.length) return ''
  const day = (ms: number) => { const d = new Date(ms); return `${d.getMonth() + 1}月${d.getDate()}日` }
  const first = stamps[0]
  const last = stamps[stamps.length - 1]
  const span = new Date(first).toDateString() === new Date(last).toDateString() ? day(first) : `${day(first)} – ${day(last)}`
  const pending = pendingReviews.value.length
  return pending ? `${span} · 还差 ${pending} 篇` : `${span} · 全都记下了`
})

const reviewFilter = ref<'all' | ScheduleEventType>('all')
const reviewKeyword = ref('')
/** 便签墙是扁平的：每张卡片自带公司图标和计划名，再按公司分组只会把同一个名字重复两遍。 */
const reviewNotes = computed<ScheduleEvent[]>(() => {
  const kw = reviewKeyword.value.trim().toLowerCase()
  return reviewedEvents.value.filter((ev) => {
    if (reviewFilter.value !== 'all' && ev.eventType !== reviewFilter.value) return false
    return !kw || `${planName(ev)} ${ev.title} ${ev.review ?? ''}`.toLowerCase().includes(kw)
  })
})
/** 已结束才允许新写；已经写过的要能一直改，否则记录会被时间锁死。 */
function reviewable(ev: ScheduleEvent): boolean {
  return !!ev.review?.trim() || isEventFinished(ev, now.value)
}
const reviewTypeCounts = computed(() => TYPES.map((type) => ({
  key: type,
  label: SCHEDULE_EVENT_TYPE_LABELS[type],
  count: reviewedEvents.value.filter((ev) => ev.eventType === type).length,
})).filter((item) => item.count > 0))

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
/** 便签卡上的「面试计划名字」= 求职目标全名；没关联目标时退回从标题里剥出来的公司名。 */
function planName(ev: ScheduleEvent): string {
  return targetOf(ev.jobProjectId)?.name ?? companyName(ev)
}
function markOf(ev: ScheduleEvent) { return companyMark(companyName(ev)) }
function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
function timelineEvents(day: Date): ScheduleEvent[] {
  return eventsOnDate(listSchedules(), day).slice(0, 2)
}
function dayNumber(day: Date): string { return `${day.getMonth() + 1}/${day.getDate()}` }
function weekLabel(day: Date): string { return '日一二三四五六'[day.getDay()] }
function countdownLabel(event: ScheduleEvent | null): string {
  if (!event) return '暂时没有安排'
  const diff = new Date(event.startTime).getTime() - now.value
  if (diff <= 0) return '正在进行'
  const minutes = Math.ceil(diff / 60_000)
  if (minutes < 60) return `${minutes} 分钟后`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟后` : `${hours} 小时后`
}
function showView(mode: 'agenda' | 'month' | 'review') {
  viewMode.value = viewMode.value === mode ? 'agenda' : mode
}
function openReview(ev: ScheduleEvent) {
  reviewTarget.value = ev
  reviewDraft.value = ev.review ?? ''
}
/** 首页那条待办只指一个方向：一场就直接开书写窗，多场就交给复盘页的「还没写心得」列表。 */
function goPendingReview() {
  if (pendingReviews.value.length === 1) openReview(pendingReviews.value[0])
  else viewMode.value = 'review'
}
/** 标题里的轮次词。复盘窗口用它而不是「目标阶段」——后者和日程类型经常撞成「面试 · … · 面试」。 */
function roundOf(ev: ScheduleEvent): string | null {
  const m = /(一面|二面|三面|四面|终面|HR面|复试|加面|笔试|面试|跟进|回访)/u.exec(ev.title)
  return m && m[1] !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] ? m[1] : null
}
function appendPrompt(text: string) {
  reviewDraft.value = appendPromptLine(reviewDraft.value, text, REVIEW_MAX)
}
function saveReview() {
  if (!reviewTarget.value) return
  const next = reviewDraft.value.trim()
  const prev = reviewTarget.value.review ?? ''
  setScheduleReview(reviewTarget.value.id, next)
  reviewTarget.value = null
  if (next && next !== prev.trim()) toast('心得已记录')
}
async function clearReview() {
  const ev = reviewTarget.value
  const prev = ev?.review?.trim()
  if (!ev || !prev) { reviewTarget.value = null; return }
  const ok = await confirmAction({
    title: '清空这篇心得？',
    message: '这场日程会保留，只删掉写下的复盘文字。',
    confirmLabel: '清空',
    danger: true,
  })
  if (!ok) return
  setScheduleReview(ev.id, '')
  reviewTarget.value = null
  toast('心得已清空', { label: '撤销', run: () => setScheduleReview(ev.id, prev) })
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
  let reminderNote = ''
  if (form.value.reminder > 0) {
    await requestNotificationPermission()
    const ev = listSchedules().find((e) => e.id === id)
    const outcome = ev ? await scheduleReminder(ev, form.value.reminder) : 'failed' as const
    // toast 只有一个槽位，提醒失败必须并进最后一句，否则会被「日程已创建」盖掉。
    if (outcome !== 'scheduled') reminderNote = ` · ${REMINDER_OUTCOME_MESSAGES[outcome]}`
  } else { cancelReminder(id) }
  sheetOpen.value = false
  toast(`${editing.value ? '日程已更新' : '日程已创建'}${reminderNote}`)
}
async function remove() {
  if (!editing.value) return
  const ev = editing.value
  const ok = await confirmAction({
    title: `删除「${headEllipsis(ev.title, 12)}」？`,
    message: '这条日程和它的提醒会从本机移除。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  cancelReminder(ev.id)
  deleteSchedule(ev.id)
  sheetOpen.value = false
  if (reviewTarget.value?.id === ev.id) reviewTarget.value = null
  toast('日程已删除')
}
/** 四条落点必须分开说：拉起日历预填页、丢进分享面板、浏览器下载是三件不同的事，用户下一步动作也不同。 */
const CALENDAR_HANDOFF_NOTES: Record<CalendarHandoff, string> = {
  native: '已打开系统日历的新建事件页，确认无误后点保存',
  shared: '已把日历文件放进分享面板，选手机日历即可导入',
  downloaded: '已下载日历文件，可用手机日历打开',
  unavailable: '没有拿到日历，可以稍后再试一次',
}
async function syncToCalendar() {
  if (!editing.value) { toast('先保存日程，再添加到手机日历'); return }
  const event = listSchedules().find((item) => item.id === editing.value?.id)
  if (!event) return
  calendarSyncing.value = true
  try {
    toast(CALENDAR_HANDOFF_NOTES[await addToDeviceCalendar(event)])
  } catch (err) {
    toast(err instanceof Error ? `添加日历失败：${err.message}` : '添加日历失败')
  } finally { calendarSyncing.value = false }
}
</script>

<template>
  <div>
    <header class="page-head schedule-head">
      <div class="schedule-now">
        <span class="schedule-date">{{ todayLabel }}</span>
        <span class="schedule-clock">{{ nowClock }}</span>
      </div>
      <div class="head-actions">
        <button class="icon-btn" :class="{ on: viewMode === 'month' }" :aria-label="viewMode === 'month' ? '返回日程列表' : '打开月历'" @click="showView('month')">
          <AppIcon name="calendar" :size="19" />
        </button>
        <button class="icon-btn review-entry" :class="{ on: viewMode === 'review' }" :aria-label="viewMode === 'review' ? '返回日程列表' : '打开面试复盘'" @click="showView('review')">
          <AppIcon name="book" :size="19" />
        </button>
      </div>
    </header>

    <template v-if="viewMode === 'agenda'">
      <section class="schedule-focus workspace-card" aria-labelledby="focus-title">
        <div class="schedule-focus-head">
          <div>
            <span class="schedule-eyebrow">今日活动</span>
            <h1 id="focus-title">{{ nextEvent ? '下一场面试' : '今天的安排' }}</h1>
          </div>
          <span class="schedule-focus-count">{{ planEvents.length }} 场</span>
        </div>
        <template v-if="nextEvent">
          <button class="schedule-focus-event" :aria-label="`查看${companyName(nextEvent)}详情`" @click="openEdit(nextEvent)">
            <span class="schedule-focus-logo">
              <img v-if="companyMark(companyName(nextEvent)).icon" :src="companyMark(companyName(nextEvent)).icon" alt="">
              <span v-else :style="{ background: companyMark(companyName(nextEvent)).color, color: companyMark(companyName(nextEvent)).lightText ? '#fff' : '#171717' }">{{ companyMark(companyName(nextEvent)).letter }}</span>
            </span>
            <span class="schedule-focus-copy">
              <strong>{{ companyName(nextEvent) }} · {{ SCHEDULE_EVENT_TYPE_LABELS[nextEvent.eventType] }}</strong>
              <small>{{ roleName(nextEvent) }} · {{ fullWhen(nextEvent.startTime) }}</small>
            </span>
            <span class="action-arrow" aria-hidden="true"><AppIcon name="arrowUpRight" :size="17" /></span>
          </button>
          <div class="schedule-focus-meta">
            <div><small>距离开始</small><strong>{{ countdownLabel(nextEvent) }}</strong></div>
            <div><small>提醒</small><strong>{{ getReminder(nextEvent.id) > 0 ? `提前 ${getReminder(nextEvent.id)} 分钟` : '未设置' }}</strong></div>
          </div>
        </template>
        <div v-else class="schedule-focus-empty">
          <strong>还没有安排面试</strong>
          <small>添加一场真实面试，日程和提醒会在本机保存。</small>
          <button class="btn-primary" @click="openCreate"><AppIcon name="plus" :size="16" /> 添加日程</button>
        </div>
      </section>

      <section class="timeline-block schedule-month workspace-card" aria-labelledby="timeline-title">
        <div class="section-head">
          <div>
            <h2 id="timeline-title">接下来一个月</h2>
            <p>横向滑动查看整月安排</p>
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

      <section class="plan-block schedule-list workspace-card" aria-labelledby="plan-title">
        <div class="section-head">
          <h2 id="plan-title">面试计划</h2>
          <button class="inline-action add-action" @click="openCreate"><AppIcon name="plus" :size="15" /> 添加日程</button>
        </div>
        <div v-if="planEvents.length" class="plan-list">
          <div v-for="(ev, i) in planEvents" :key="ev.id" class="plan-row" :style="{ '--i': Math.min(i, 8) }">
            <button class="plan-main" @click="openEdit(ev)">
              <span class="plan-mark">
                <img v-if="markOf(ev).icon" :src="markOf(ev).icon" alt="">
                <span v-else :style="{ background: markOf(ev).color, color: markOf(ev).lightText ? '#fff' : '#171717' }">{{ markOf(ev).letter }}</span>
              </span>
              <span class="plan-copy">
                <strong>{{ companyName(ev) }}</strong>
                <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
                <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
              </span>
              <AppIcon name="chevronRight" :size="18" class="plan-chev" />
            </button>
            <button
              v-if="reviewable(ev)"
              class="plan-note"
              :class="{ filled: !!ev.review?.trim() }"
              :aria-label="ev.review ? `查看 ${companyName(ev)} 的心得` : `为 ${companyName(ev)} 写心得`"
              @click="openReview(ev)"
            >
              <AppIcon :name="ev.review?.trim() ? 'book' : 'edit'" :size="15" />
              <span>{{ ev.review?.trim() ? '看心得' : '写心得' }}</span>
            </button>
          </div>
        </div>
        <EmptyState v-else icon="calendar" title="还没有面试计划" hint="添加一场真实面试，日程和提醒会在本机保存。" />
      </section>

      <button v-if="pendingReviews.length" class="review-nudge workspace-card" @click="goPendingReview">
        <span class="review-nudge-mark"><AppIcon name="edit" :size="16" /></span>
        <span class="plan-copy">
          <strong>{{ pendingReviews.length }} 场已结束还没写心得</strong>
          <small class="plan-when">最近一场 · {{ companyName(pendingReviews[0]) }} · {{ fullWhen(pendingReviews[0].startTime) }}</small>
        </span>
        <span class="inline-action">去补写 <AppIcon name="chevronRight" :size="15" /></span>
      </button>
    </template>

    <template v-else-if="viewMode === 'review'">
      <section v-if="finishedEvents.length" class="review-hero workspace-card" :style="{ '--pct': reviewCoverage }">
        <div class="hero-ring">
          <svg viewBox="0 0 72 72" aria-hidden="true">
            <circle class="hero-ring-track" cx="36" cy="36" r="30" />
            <circle class="hero-ring-bar" :class="{ empty: !reviewCoverage }" cx="36" cy="36" r="30" />
          </svg>
          <span class="hero-ring-num">{{ reviewCoverage }}<em>%</em></span>
        </div>
        <div class="hero-copy">
          <span class="schedule-eyebrow">复盘覆盖率</span>
          <strong><b>{{ reviewedFinished.length }}</b> / {{ finishedEvents.length }} 篇已写</strong>
          <small>{{ reviewSpanLabel }}</small>
        </div>
      </section>

      <section v-if="pendingReviews.length" class="review-group workspace-card" aria-labelledby="pending-title">
        <div class="section-head">
          <div>
            <h2 id="pending-title">还没写心得</h2>
            <p>结束后当场记两句，比一周后回忆准得多</p>
          </div>
        </div>
        <button v-for="ev in pendingReviews" :key="ev.id" class="review-pending" @click="openReview(ev)">
          <span class="plan-mark">
            <img v-if="markOf(ev).icon" :src="markOf(ev).icon" alt="">
            <span v-else :style="{ background: markOf(ev).color, color: markOf(ev).lightText ? '#fff' : '#171717' }">{{ markOf(ev).letter }}</span>
          </span>
          <span class="plan-copy">
            <strong>{{ planName(ev) }}</strong>
            <small class="plan-when">{{ fullWhen(ev.startTime) }} · 还没写心得</small>
          </span>
          <span class="inline-action"><AppIcon name="edit" :size="14" /> 补写</span>
        </button>
      </section>

      <template v-if="reviewedEvents.length">
        <div class="section-head wall-head">
          <div>
            <h2>复盘心得</h2>
            <p>点开一张可以继续往下写</p>
          </div>
        </div>
        <div class="toolbar">
          <div class="pill-row">
            <button class="filter-pill" :class="{ on: reviewFilter === 'all' }" @click="reviewFilter = 'all'">全部<em>{{ reviewedEvents.length }}</em></button>
            <button
              v-for="t in reviewTypeCounts" :key="t.key"
              class="filter-pill" :class="{ on: reviewFilter === t.key }"
              @click="reviewFilter = t.key"
            >{{ t.label }}<em>{{ t.count }}</em></button>
          </div>
          <label class="search-box">
            <AppIcon name="search" :size="15" />
            <input v-model="reviewKeyword" placeholder="搜索计划名、心得内容或公司…" autocomplete="off">
          </label>
        </div>
      </template>

      <div v-if="reviewNotes.length" class="note-wall">
        <button
          v-for="ev in reviewNotes" :key="ev.id"
          class="note-card"
          :style="{ '--tint': SCHEDULE_EVENT_TYPE_COLORS[ev.eventType] }"
          @click="openReview(ev)"
        >
          <span class="note-head">
            <span class="note-mark">
              <img v-if="markOf(ev).icon" :src="markOf(ev).icon" alt="">
              <span v-else :style="{ background: markOf(ev).color, color: markOf(ev).lightText ? '#fff' : '#171717' }">{{ markOf(ev).letter }}</span>
            </span>
            <span class="note-id">
              <strong>{{ planName(ev) }}</strong>
              <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roundOf(ev)"> · {{ roundOf(ev) }}</template></small>
            </span>
          </span>
          <p class="note-body">{{ ev.review }}</p>
          <span class="note-foot">
            <time :datetime="ev.startTime">{{ fullWhen(ev.startTime) }}</time>
            <AppIcon name="chevronRight" :size="13" />
          </span>
        </button>
      </div>

      <EmptyState
        v-if="!reviewedEvents.length"
        icon="book"
        title="还没有写过心得"
        hint="日程一结束，列表那行的右侧就会出现「写心得」，点开弹窗写两句，就会汇总到这里。"
      />
      <EmptyState
        v-else-if="!reviewNotes.length"
        icon="search"
        title="没有匹配的心得"
        hint="换个关键词，或把类型筛选切回「全部」。"
      />
    </template>

    <template v-else-if="viewMode === 'month'">
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
        <div v-for="(ev, i) in selectedEvents" :key="ev.id" class="plan-row" :style="{ '--i': i }">
          <button class="plan-main" @click="openEdit(ev)">
            <span class="plan-mark">
              <img v-if="markOf(ev).icon" :src="markOf(ev).icon" alt="">
              <span v-else :style="{ background: markOf(ev).color, color: markOf(ev).lightText ? '#fff' : '#171717' }">{{ markOf(ev).letter }}</span>
            </span>
            <span class="plan-copy">
              <strong>{{ companyName(ev) }}</strong>
              <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
              <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
            </span>
            <AppIcon name="chevronRight" :size="18" class="plan-chev" />
          </button>
          <button
            v-if="reviewable(ev)"
            class="plan-note"
            :class="{ filled: !!ev.review?.trim() }"
            :aria-label="ev.review ? `查看 ${companyName(ev)} 的心得` : `为 ${companyName(ev)} 写心得`"
            @click="openReview(ev)"
          >
            <AppIcon :name="ev.review?.trim() ? 'book' : 'edit'" :size="15" />
            <span>{{ ev.review?.trim() ? '看心得' : '写心得' }}</span>
          </button>
        </div>
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
      <div class="field">
        <label>备注</label>
        <textarea v-model="form.notes" rows="3" placeholder="会议链接 / 面试官 / 注意事项…"></textarea>
      </div>
      <button v-if="editing" class="calendar-link" :disabled="calendarSyncing" @click="syncToCalendar">
        <AppIcon name="calendar" :size="16" />
        <span>{{ calendarSyncing ? '准备中…' : '在手机日历中新建' }}</span>
        <AppIcon name="external" :size="14" />
      </button>
      <div class="sheet-actions">
        <button v-if="editing" class="btn-danger" @click="remove"><AppIcon name="trash" :size="16" /> 删除</button>
        <button class="btn-ghost" @click="sheetOpen = false">取消</button>
        <button class="btn-primary" @click="submit">保存</button>
      </div>
    </Sheet>

    <Sheet v-if="reviewTarget" :title="reviewTarget.review?.trim() ? '这一场的心得' : '写下这一场的心得'" @close="reviewTarget = null">
      <div class="note-owner">
        <span class="plan-mark">
          <img v-if="markOf(reviewTarget).icon" :src="markOf(reviewTarget).icon" alt="">
          <span v-else :style="{ background: markOf(reviewTarget).color, color: markOf(reviewTarget).lightText ? '#fff' : '#171717' }">{{ markOf(reviewTarget).letter }}</span>
        </span>
        <span class="plan-copy">
          <strong>{{ planName(reviewTarget) }}</strong>
          <small class="review-meta">
            {{ SCHEDULE_EVENT_TYPE_LABELS[reviewTarget.eventType] }}<template v-if="roundOf(reviewTarget)"> · {{ roundOf(reviewTarget) }}</template> · {{ fullWhen(reviewTarget.startTime) }}
          </small>
        </span>
      </div>
      <div class="prompt-row">
        <button v-for="p in REVIEW_PROMPTS" :key="p" class="prompt-chip" @click="appendPrompt(p)">{{ p }}</button>
      </div>
      <div class="field">
        <label>这一场发生了什么</label>
        <textarea v-model="reviewDraft" rows="7" :maxlength="REVIEW_MAX" placeholder="被问了什么、哪里没答好、下一轮要补什么…"></textarea>
        <small class="field-help">{{ reviewDraft.trim().length }}/{{ REVIEW_MAX }}</small>
      </div>
      <div class="sheet-actions">
        <button v-if="reviewTarget.review" class="btn-danger" @click="clearReview"><AppIcon name="trash" :size="16" /> 清空</button>
        <button class="btn-ghost" @click="reviewTarget = null">关闭</button>
        <button class="btn-primary" @click="saveReview">保存</button>
      </div>
    </Sheet>
  </div>
</template>
