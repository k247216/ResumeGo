<template>
  <section class="schedule-view">
    <PageHeader eyebrow="日程" title="求职日程" subtitle="把面试、笔试和跟进放进日历，所有记录只保存在本地。">
      <template #actions>
        <button type="button" class="btn-primary" data-test="add-event" @click="openCreate"><el-icon :size="14"><Plus /></el-icon>添加日程</button>
      </template>
    </PageHeader>

    <p v-if="schedule.errorMessage.value" class="schedule-error" role="alert">{{ schedule.errorMessage.value }}</p>

    <div class="schedule-body">
      <div class="calendar-pane">
        <div class="calendar-toolbar">
          <div class="month-nav">
            <button type="button" data-test="prev-month" aria-label="上个月" @click="schedule.goToMonth(-1)"><el-icon><ArrowLeft /></el-icon></button>
            <strong data-test="month-label">{{ monthLabel }}</strong>
            <button type="button" data-test="next-month" aria-label="下个月" @click="schedule.goToMonth(1)"><el-icon><ArrowRight /></el-icon></button>
            <button type="button" class="today-button" data-test="today-button" @click="schedule.goToToday">今天</button>
          </div>
          <div class="month-summary">
            <span v-for="entry in monthTypeSummary" :key="entry.label"><i :style="{ background: entry.color }"></i>{{ entry.label }}<strong>{{ entry.count }}</strong></span>
            <em>本月共 {{ monthEvents.length }} 场安排</em>
          </div>
        </div>

        <div class="week-head">
          <span v-for="day in weekDays" :key="day" :class="{ weekend: day === '日' }">{{ day }}</span>
        </div>

        <div class="calendar-grid">
          <button v-for="cell in cells" :key="cell.key" type="button"
            :class="['day-cell', { outside: !cell.inMonth, today: cell.isToday, selected: cell.isSelected, hasEvents: cell.events.length > 0 }]"
            :data-test="`day-cell-${cell.key}`"
            @click="selectDate(cell.date)">
            <span class="day-number">{{ cell.day }}</span>
            <span v-if="cell.events.length" class="day-dots">
              <i v-for="event in cell.events.slice(0, 3)" :key="event.id" :style="{ background: eventTypeColor(event.eventType) }"></i>
              <em v-if="cell.events.length > 3" class="dots-more">+{{ cell.events.length - 3 }}</em>
            </span>
          </button>
        </div>

        <div class="type-legend">
          <span v-for="type in legend" :key="type.value"><i :style="{ background: type.color }"></i>{{ type.label }}</span>
        </div>
      </div>

      <aside class="day-panel">
        <div class="day-panel-head">
          <div><small>已选日期</small><strong data-test="selected-day-label">{{ selectedDayLabel }}</strong></div>
          <button type="button" data-test="add-event-day" @click="openCreate"><el-icon :size="13"><Plus /></el-icon>添加</button>
        </div>
        <div v-if="selectedEvents.length" class="day-events">
          <article v-for="event in selectedEvents" :key="event.id" class="day-event" :data-test="`event-card-${event.id}`">
            <button type="button" class="day-event-main" @click="openEdit(event)">
              <span class="event-dot" :style="{ background: eventTypeColor(event.eventType) }" aria-hidden="true" />
              <span class="event-copy">
                <strong>{{ event.title }}</strong>
                <small>{{ eventTypeLabel(event.eventType) }} · {{ eventTimeLabel(event) }}</small>
                <p v-if="event.notes">{{ event.notes }}</p>
              </span>
              <el-icon class="event-arrow"><ArrowRight /></el-icon>
            </button>
            <div v-if="linkedTarget(event)" class="day-event-context" data-test="event-target-context">
              <span class="day-event-context-copy">{{ linkedTarget(event)!.name }}</span>
              <button type="button" class="day-event-context-action" data-test="event-target-link" @click="openTarget(linkedTarget(event)!.id)">目标详情<el-icon :size="12"><ArrowRight /></el-icon></button>
            </div>
          </article>
        </div>
        <div v-else class="day-empty">
          <span class="empty-mark"><el-icon :size="22"><Calendar /></el-icon></span>
          <strong>这一天还没有安排</strong>
          <p>点击「添加」录入面试、笔试或跟进事项。</p>
        </div>
      </aside>
    </div>

    <ScheduleEventDialog
      :open="dialogOpen"
      :editing="editingEvent"
      :submitting="submitting"
      :error-message="dialogError"
      @close="closeDialog"
      @save="handleSave"
      @delete="handleDelete"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Calendar, Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import ScheduleEventDialog from '../../components/schedule/ScheduleEventDialog.vue'
import { useSchedule } from '../../composables/useSchedule'
import { useTargetsStore } from '../../stores/targets'
import { SCHEDULE_EVENT_TYPE_COLORS, SCHEDULE_EVENT_TYPE_LABELS } from '../../types/schedule'
import type { ScheduleEvent, ScheduleEventType } from '../../types/schedule'

const schedule = useSchedule()
const targetsStore = useTargetsStore()
const router = useRouter()

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const legend = (Object.keys(SCHEDULE_EVENT_TYPE_LABELS) as ScheduleEventType[]).map((value) => ({
  value,
  label: SCHEDULE_EVENT_TYPE_LABELS[value],
  color: SCHEDULE_EVENT_TYPE_COLORS[value],
}))

const now = new Date()
const selectedDate = ref<Date>(new Date(now.getFullYear(), now.getMonth(), now.getDate()))

const monthLabel = computed(() => {
  const { year, month } = schedule.visibleMonth.value
  return `${year} 年 ${month + 1} 月`
})

const monthEvents = computed(() => schedule.monthEvents.value)

const monthTypeSummary = computed(() => {
  const counts = new Map<ScheduleEventType, number>()
  for (const event of monthEvents.value) {
    counts.set(event.eventType, (counts.get(event.eventType) ?? 0) + 1)
  }
  return (Object.keys(SCHEDULE_EVENT_TYPE_LABELS) as ScheduleEventType[])
    .filter((type) => (counts.get(type) ?? 0) > 0)
    .map((type) => ({ label: SCHEDULE_EVENT_TYPE_LABELS[type], color: SCHEDULE_EVENT_TYPE_COLORS[type], count: counts.get(type) ?? 0 }))
})

interface DayCell {
  key: string
  date: Date
  day: number
  inMonth: boolean
  isToday: boolean
  isSelected: boolean
  events: ScheduleEvent[]
}

function localDateKey(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const cells = computed<DayCell[]>(() => {
  const { year, month } = schedule.visibleMonth.value
  const first = new Date(year, month, 1)
  const offset = (first.getDay() + 6) % 7 // 周一为一周起点
  const result: DayCell[] = []
  for (let i = 0; i < 42; i += 1) {
    const date = new Date(year, month, 1 - offset + i)
    const today = new Date()
    const isToday = date.getFullYear() === today.getFullYear()
      && date.getMonth() === today.getMonth()
      && date.getDate() === today.getDate()
    const isSelected = date.getFullYear() === selectedDate.value.getFullYear()
      && date.getMonth() === selectedDate.value.getMonth()
      && date.getDate() === selectedDate.value.getDate()
    result.push({
      key: localDateKey(date),
      date,
      day: date.getDate(),
      inMonth: date.getMonth() === month,
      isToday,
      isSelected,
      events: schedule.eventsOn(date),
    })
  }
  return result
})

const selectedEvents = computed(() => schedule.eventsOn(selectedDate.value))

const selectedDayLabel = computed(() => {
  const date = selectedDate.value
  const week = ['日', '一', '二', '三', '四', '五', '六'][date.getDay()]
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日 · 周${week}`
})

function selectDate(date: Date) {
  selectedDate.value = date
  if (date.getMonth() !== schedule.visibleMonth.value.month
    || date.getFullYear() !== schedule.visibleMonth.value.year) {
    schedule.visibleMonth.value = { year: date.getFullYear(), month: date.getMonth() }
    void schedule.load()
  }
}

function eventTypeLabel(type: ScheduleEventType): string {
  return SCHEDULE_EVENT_TYPE_LABELS[type]
}

function eventTypeColor(type: ScheduleEventType): string {
  return SCHEDULE_EVENT_TYPE_COLORS[type]
}

// 已关联目标的事件（jobDescriptionId 有值）在事件行下方给出目标上下文；目标已删除则如实不显示。
function linkedTarget(event: ScheduleEvent) {
  if (event.jobDescriptionId == null) return null
  return targetsStore.targets.find((target) => target.jobDescriptionId === event.jobDescriptionId) ?? null
}

function openTarget(targetId: number) {
  void router.push({ name: 'targets', query: { targetId: String(targetId) } })
}

function eventTimeLabel(event: ScheduleEvent): string {
  const start = new Date(event.startTime)
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(start.getHours())}:${pad(start.getMinutes())}`
  if (!event.endTime) return time
  const end = new Date(event.endTime)
  return `${time} - ${pad(end.getHours())}:${pad(end.getMinutes())}`
}

const dialogOpen = ref(false)
const editingEvent = ref<ScheduleEvent | null>(null)
const submitting = ref(false)
const dialogError = ref('')

function openCreate() {
  editingEvent.value = null
  dialogError.value = ''
  dialogOpen.value = true
}

function openEdit(event: ScheduleEvent) {
  editingEvent.value = event
  dialogError.value = ''
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  editingEvent.value = null
  dialogError.value = ''
}

async function handleSave(payload: { title: string; eventType: ScheduleEventType; startTime: string; notes: string | null }) {
  submitting.value = true
  dialogError.value = ''
  try {
    if (editingEvent.value) {
      await schedule.editEvent(editingEvent.value.id, { ...payload, endTime: null, jobDescriptionId: null })
    } else {
      await schedule.addEvent({ ...payload, endTime: null, jobDescriptionId: null })
    }
    closeDialog()
  } catch (err) {
    dialogError.value = err instanceof Error ? err.message : '保存日程失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!editingEvent.value) return
  submitting.value = true
  dialogError.value = ''
  try {
    await schedule.removeEvent(editingEvent.value.id)
    closeDialog()
  } catch (err) {
    dialogError.value = err instanceof Error ? err.message : '删除日程失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void schedule.load()
  if (!targetsStore.targets.length && !targetsStore.loading) void targetsStore.load()
})
</script>

<style scoped>
.schedule-view{display:flex;flex-direction:column;height:100%;min-height:0}
.schedule-body{flex:1;min-height:0;display:grid;grid-template-columns:minmax(0,1fr) 320px;border-top:1px solid var(--border-subtle)}

.schedule-error{margin:0 0 14px;padding:10px 14px;border-radius:var(--radius-control);background:var(--danger-soft);color:var(--danger);font-size:13px}

/* ── 左列：日历（平表面，内部滚动） ── */
.calendar-pane{min-height:0;overflow-y:auto;border-right:1px solid var(--border-subtle);padding:20px 22px 30px}
.calendar-toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:14px}
.month-nav{display:flex;align-items:center;gap:7px}
.month-nav button{display:grid;width:32px;height:32px;place-items:center;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);cursor:pointer}
.month-nav button:hover{border-color:var(--brand);color:var(--brand)}
.month-nav strong{min-width:126px;text-align:center;font-size:16px;letter-spacing:-.01em;color:var(--ink)}
.today-button{height:32px;padding:0 13px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--copy);font-size:12px;font-weight:600;cursor:pointer}
.today-button:hover{border-color:var(--brand);color:var(--brand)}
.month-summary{display:flex;flex-wrap:wrap;align-items:center;gap:10px 16px;padding:0 2px 12px;border-bottom:1px solid var(--border-subtle);color:var(--muted);font-size:12px}
.month-summary span{display:inline-flex;align-items:center;gap:6px}
.month-summary span i{width:7px;height:7px;border-radius:50%}
.month-summary span strong{margin-left:2px;color:var(--ink);font-size:13px;font-weight:600}
.month-summary em{margin-left:auto;color:var(--muted);font-style:normal}
.week-head,.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:6px}
.week-head span{color:var(--muted);font-size:12px;font-weight:600;text-align:center;padding:10px 0 6px}
.week-head span.weekend{color:var(--danger)}
.calendar-grid{row-gap:4px;margin-top:2px}
.day-cell{position:relative;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;gap:4px;min-height:58px;padding:6px 0 4px;border:1px solid transparent;border-radius:var(--radius-control);background:transparent;color:var(--ink);cursor:pointer;transition:background .15s ease,border-color .15s ease}
.day-cell:hover{background:var(--bg-hover)}
.day-cell.outside{color:var(--text-tertiary);opacity:.5}
.day-cell.outside .day-dots i{opacity:.5}
.day-cell.today .day-number,.day-cell.selected .day-number{display:grid;width:26px;height:26px;place-items:center;border-radius:50%;background:var(--brand);color:#fff;font-weight:700}
.day-cell.selected{background:var(--bg-selected)}
.day-cell.selected.today .day-number{color:#fff}
.day-cell.hasEvents .day-number{font-weight:700}
.day-number{font-size:13px;line-height:1}
.day-dots{display:flex;align-items:center;gap:3px;min-height:8px}
.day-dots i{width:6px;height:6px;border-radius:999px}
.dots-more{color:var(--muted);font-size:10px;font-style:normal;font-weight:700}
.type-legend{display:flex;gap:18px;margin-top:16px;padding-top:14px;border-top:1px solid var(--border-subtle)}
.type-legend span{display:flex;align-items:center;gap:6px;color:var(--muted);font-size:12px}
.type-legend i{width:8px;height:8px;border-radius:50%}

/* ── 右列：当日事件（平列表行 + 类型圆点） ── */
.day-panel{min-height:0;overflow-y:auto;padding:20px 20px 30px}
.day-panel-head{display:flex;align-items:flex-end;justify-content:space-between;gap:10px;padding-bottom:14px;border-bottom:1px solid var(--border-subtle)}
.day-panel-head>div{display:grid;gap:4px}
.day-panel-head small{color:var(--muted);font-size:11px;font-weight:600;letter-spacing:.08em}
.day-panel-head strong{font-size:16px;color:var(--ink)}
.day-panel-head button{display:inline-flex;align-items:center;gap:4px;border:0;background:none;color:var(--brand);font-size:13px;font-weight:600;cursor:pointer;padding:6px 10px;border-radius:var(--radius-control)}
.day-panel-head button:hover{background:var(--bg-hover)}
.day-events{display:grid}
.day-event + .day-event{border-top:1px solid var(--border-subtle)}
.day-event-main{display:flex;align-items:flex-start;gap:10px;width:100%;padding:13px 4px;border:0;background:transparent;text-align:left;cursor:pointer;color:var(--ink)}
.day-event-main:hover .event-copy strong{color:var(--brand)}
.event-dot{flex:0 0 auto;width:8px;height:8px;margin-top:5px;border-radius:50%}
.event-copy{display:grid;gap:4px;min-width:0}
.event-copy strong{font-size:14px;font-weight:600}
.event-copy small{display:flex;align-items:center;gap:5px;color:var(--muted);font-size:12px}
.event-copy p{margin:2px 0 0;color:var(--copy);font-size:12px;line-height:1.55;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.event-arrow{margin-left:auto;margin-top:14px;color:var(--text-tertiary);flex:0 0 auto}
.day-event-context{display:flex;align-items:center;gap:10px;padding:0 4px 10px 22px}
.day-event-context-copy{overflow:hidden;flex:1;min-width:0;color:var(--muted);font-size:12px;text-overflow:ellipsis;white-space:nowrap}
.day-event-context-action{display:inline-flex;align-items:center;gap:4px;flex:0 0 auto;padding:0;border:0;background:transparent;color:var(--copy);font-size:12px;font-weight:500;cursor:pointer}
.day-event-context-action:hover{color:var(--brand)}
.day-empty{display:grid;justify-items:center;gap:8px;padding:52px 10px;color:var(--muted);text-align:center}
.empty-mark{display:grid;width:52px;height:52px;place-items:center;border-radius:var(--radius-panel);background:var(--bg-selected);color:var(--brand);margin-bottom:4px}
.day-empty strong{color:var(--ink);font-size:14px}
.day-empty p{margin:0;font-size:13px;line-height:1.6}

.btn-primary{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:9px 15px;font-size:13px;font-weight:600;cursor:pointer}
.btn-primary:hover{background:var(--accent-hover)}

/* 共享断点阶梯：窄窗口下日历与事件面板上下堆叠 */
@media (max-width: 959px) {
  .schedule-body{grid-template-columns:minmax(0,1fr);grid-template-rows:minmax(0,1fr) auto}
  .calendar-pane{border-right:0;border-bottom:1px solid var(--border-subtle)}
  .day-panel{max-height:44vh;padding-top:18px}
  .calendar-toolbar{flex-wrap:wrap}
  .day-cell{min-height:46px}
  .month-summary em{margin-left:0}
}
</style>
