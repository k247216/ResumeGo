<template>
  <section class="schedule-view">
    <PageHeader eyebrow="日程" title="求职日程" subtitle="把面试、笔试和跟进放进日历，所有记录只保存在本地。">
      <template #actions>
        <button type="button" class="btn-primary" data-test="add-event" @click="openCreate()"><el-icon :size="14"><Plus /></el-icon>添加日程</button>
      </template>
    </PageHeader>

    <p v-if="schedule.errorMessage.value" class="schedule-error" role="alert">{{ schedule.errorMessage.value }}</p>

    <div class="schedule-frame">
      <!-- 左：紧凑月历（白底卡片） -->
      <div class="calendar-card" :class="{ 'is-loading': schedule.loading.value }">
        <div class="calendar-toolbar">
          <div class="month-nav">
            <button type="button" data-test="prev-month" aria-label="上个月" @click="shiftMonth(-1)"><el-icon><ArrowLeft /></el-icon></button>
            <strong data-test="month-label">{{ monthLabel }}</strong>
            <button type="button" data-test="next-month" aria-label="下个月" @click="shiftMonth(1)"><el-icon><ArrowRight /></el-icon></button>
          </div>
          <div class="toolbar-side">
            <span v-if="schedule.loading.value" class="calendar-loading" data-test="calendar-loading">载入中…</span>
            <button v-if="awayFromToday" type="button" class="btn-primary btn-small" data-test="back-to-today" @click="backToToday">回到今天</button>
          </div>
        </div>

        <div class="week-head">
          <span v-for="day in weekDays" :key="day" :class="{ weekend: day === '日' }">{{ day }}</span>
        </div>

        <div class="calendar-grid">
          <button v-for="cell in cells" :key="cell.key" type="button"
            :class="['day-cell', { outside: !cell.inMonth, today: cell.isToday, selected: cell.isSelected, hasEvents: cell.events.length > 0 }]"
            :data-test="`day-cell-${cell.key}`"
            @click.stop="onCellClick(cell)">
            <span class="day-number-row"><b>{{ cell.day }}</b><i v-if="cell.isToday && !cell.isSelected" class="today-dot" aria-hidden="true"></i></span>
            <span v-for="item in cell.events.slice(0, 2)" :key="item.key" class="cell-chip" :class="{ external: item.kind === 'external' }">
              <i :style="{ background: chipColor(item) }"></i>
              <em>{{ item.allDay ? '全天' : clockTime(item.startTime) }}</em>
              <b>{{ item.title }}</b>
            </span>
            <em v-if="cell.events.length > 2" class="chip-more">+{{ cell.events.length - 2 }}</em>

            <!-- 选中日的完整安排浮层：绝对定位展示全部条目，不改变日历布局 -->
            <span
              v-if="cell.isSelected && popoverOpen && cell.events.length"
              class="cell-popover"
              :class="[popoverPositionClass(cell)]"
              :data-test="`cell-popover-${cell.key}`"
              @click.stop
            >
              <span v-for="item in cell.events" :key="item.key" class="popover-row">
                <em>{{ item.allDay ? '全天' : clockTime(item.startTime) }}</em>
                <b>{{ item.title }}</b>
                <i v-if="item.kind === 'external'" class="popover-ext">{{ item.sourceName }}</i>
              </span>
            </span>
          </button>
        </div>

        <footer class="calendar-foot">
          <div class="type-legend">
            <span v-for="type in legend" :key="type.value"><i :style="{ background: type.color }"></i>{{ type.label }}</span>
            <span v-if="external.externalEvents.value.length"><i class="legend-external"></i>外部日历</span>
          </div>
          <div class="foot-side">
            <em v-if="monthEvents.length" class="month-total">本月 {{ monthEvents.length }} 场</em>
            <button type="button" class="sources-link" data-test="sources-button" @click="sourcesOpen = true">
              <el-icon :size="13"><Calendar /></el-icon>{{ external.sources.value.length ? '外部日历' : '导入日历' }}
            </button>
          </div>
        </footer>
      </div>

      <!-- 右：一体化日程面板 -->
      <aside class="panel-card">
        <div class="panel-head">
          <div class="panel-head-copy">
            <button v-if="panelMode === 'day'" type="button" class="back-link" data-test="panel-back" @click="panelMode = 'agenda'">
              <el-icon :size="11"><ArrowLeft /></el-icon>最近日程
            </button>
            <strong data-test="selected-day-label">{{ panelTitle }}</strong>
            <small>{{ panelSubtitle }}</small>
          </div>
          <button type="button" data-test="add-event-day" @click="openCreate(panelMode === 'day' ? selectedDate : undefined)"><el-icon :size="13"><Plus /></el-icon>添加</button>
        </div>

        <!-- 聚合模式：未来 7 天连续时间线 -->
        <div v-if="panelMode === 'agenda'" class="agenda" data-test="upcoming-list">
          <template v-if="upcomingGroups.length">
            <section v-for="group in upcomingGroups" :key="group.key" class="agenda-group" :data-test="`upcoming-date-${group.key}`">
              <header class="agenda-date"><b>{{ group.label }}</b><em>{{ group.events.length }} 场</em></header>
              <ScheduleEventCard v-for="item in group.events" :key="item.key" :event="item" @edit="openDisplayEdit" />
            </section>
          </template>
          <div v-else-if="nextBeyondWeek" class="day-empty compact-empty">
            <p>未来 7 天没有安排。</p>
            <p class="next-hint">下一条：{{ nextBeyondWeek.label }} · {{ nextBeyondWeek.title }}</p>
          </div>
          <div v-else class="day-empty">
            <span class="empty-mark"><el-icon :size="22"><Calendar /></el-icon></span>
            <strong>未来 7 天没有安排</strong>
            <p>点击「添加」录入面试、笔试或跟进事项。</p>
          </div>
        </div>

        <!-- 单日模式：从日历点入某天 -->
        <div v-else class="agenda">
          <div v-if="selectedDisplayEvents.length" class="agenda-group" :data-test="`day-list-${dayKeyOf(selectedDate)}`">
            <ScheduleEventCard v-for="item in selectedDisplayEvents" :key="item.key" :event="item" @edit="openDisplayEdit" />
          </div>
          <div v-else class="day-empty">
            <span class="empty-mark"><el-icon :size="22"><Calendar /></el-icon></span>
            <strong>这一天还没有安排</strong>
            <p>点击「添加」录入面试、笔试或跟进事项。</p>
          </div>
        </div>
      </aside>
    </div>

    <ScheduleEventDialog
      :open="dialogOpen"
      :editing="editingEvent"
      :submitting="submitting"
      :error-message="dialogError"
      :plans="planOptions"
      :default-date="dialogDefaultDate"
      @close="closeDialog"
      @save="handleSave"
      @delete="handleDelete"
    />

    <ScheduleSourcesDialog
      :open="sourcesOpen"
      :sources="external.sources.value"
      :importing="importingSource"
      @close="sourcesOpen = false"
      @import="handleImportSource"
      @remove="handleRemoveSource"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight, Calendar, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import ScheduleEventDialog from '../../components/schedule/ScheduleEventDialog.vue'
import ScheduleEventCard from '../../components/schedule/ScheduleEventCard.vue'
import ScheduleSourcesDialog from '../../components/schedule/ScheduleSourcesDialog.vue'
import { useSchedule } from '../../composables/useSchedule'
import { useExternalCalendars } from '../../composables/useExternalCalendars'
import { useTargetsStore } from '../../stores/targets'
import { SCHEDULE_EVENT_TYPE_COLORS, SCHEDULE_EVENT_TYPE_LABELS } from '../../types/schedule'
import type { DisplayCalendarEvent, ScheduleEvent, ScheduleEventType } from '../../types/schedule'

const schedule = useSchedule()
const external = useExternalCalendars()
const targetsStore = useTargetsStore()

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const legend = (Object.keys(SCHEDULE_EVENT_TYPE_LABELS) as ScheduleEventType[]).map((value) => ({
  value,
  label: SCHEDULE_EVENT_TYPE_LABELS[value],
  color: SCHEDULE_EVENT_TYPE_COLORS[value],
}))

const now = new Date()
const selectedDate = ref<Date>(new Date(now.getFullYear(), now.getMonth(), now.getDate()))

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function localDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function dayKeyOf(date: Date): string {
  return localDateKey(startOfDay(date))
}

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function clockTime(iso: string): string {
  const date = new Date(iso)
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const monthLabel = computed(() => {
  const { year, month } = schedule.visibleMonth.value
  return `${year} 年 ${month + 1} 月`
})

const monthEvents = computed(() => schedule.monthEvents.value)

// ── 统一展示模型：自有日程 + 外部只读日程按天合并 ──
function ownDisplay(event: ScheduleEvent): DisplayCalendarEvent {
  return {
    key: `own-${event.id}`,
    title: event.title,
    startTime: event.startTime,
    endTime: event.endTime,
    allDay: false,
    kind: 'own',
    id: event.id,
    eventType: event.eventType,
    notes: event.notes,
    jobDescriptionId: event.jobDescriptionId,
    jobProjectId: event.jobProjectId,
  }
}

function sortDisplay(items: DisplayCalendarEvent[]): DisplayCalendarEvent[] {
  // 全天置顶，其余按开始时间排序；同一天多场安排通过时间段区分先后
  return [...items].sort((left, right) => {
    if (left.allDay !== right.allDay) return left.allDay ? -1 : 1
    return left.startTime.localeCompare(right.startTime)
  })
}

const externalsByDay = computed(() => {
  const map = new Map<string, DisplayCalendarEvent[]>()
  for (const event of external.externalEvents.value) {
    const key = event.startTime.slice(0, 10)
    const list = map.get(key)
    if (list) list.push(event)
    else map.set(key, [event])
  }
  return map
})

function displayEventsOn(date: Date): DisplayCalendarEvent[] {
  return sortDisplay([
    ...schedule.eventsOn(date).map(ownDisplay),
    ...(externalsByDay.value.get(localDateKey(date)) ?? []),
  ])
}

function chipColor(item: DisplayCalendarEvent): string {
  if (item.kind === 'external') return 'var(--external-accent)'
  return SCHEDULE_EVENT_TYPE_COLORS[(item.eventType ?? 'other') as ScheduleEventType]
}

interface DayCell {
  key: string
  date: Date
  day: number
  row: number
  col: number
  inMonth: boolean
  isToday: boolean
  isSelected: boolean
  events: DisplayCalendarEvent[]
}

// 固定格高 + 溢出隐藏：任何交互都不会改变日历布局；
// 完整安排通过选中浮层查看。
const cells = computed<DayCell[]>(() => {
  const { year, month } = schedule.visibleMonth.value
  const first = new Date(year, month, 1)
  const offset = (first.getDay() + 6) % 7 // 周一为一周起点
  const todayStart = startOfDay(new Date())
  const selectedStart = startOfDay(selectedDate.value)
  const result: DayCell[] = []
  for (let i = 0; i < 42; i += 1) {
    const date = new Date(year, month, 1 - offset + i)
    const dayStart = startOfDay(date)
    result.push({
      key: localDateKey(dayStart),
      date: dayStart,
      day: date.getDate(),
      row: Math.floor(i / 7),
      col: i % 7,
      inMonth: date.getMonth() === month,
      isToday: dayStart.getTime() === todayStart.getTime(),
      isSelected: dayStart.getTime() === selectedStart.getTime(),
      events: displayEventsOn(dayStart),
    })
  }
  return result
})

// 「回到今天」只在离开当前日期/月份时出现；今天格平时仅保留小圆点标记。
const awayFromToday = computed(() => {
  const today = startOfDay(new Date())
  const { year, month } = schedule.visibleMonth.value
  return year !== today.getFullYear() || month !== today.getMonth()
    || startOfDay(selectedDate.value).getTime() !== today.getTime()
})

function shiftMonth(offset: number) {
  popoverOpen.value = false
  schedule.goToMonth(offset)
}

function backToToday() {
  const today = new Date()
  selectedDate.value = startOfDay(today)
  popoverOpen.value = false
  panelMode.value = 'agenda'
  if (schedule.visibleMonth.value.year !== today.getFullYear() || schedule.visibleMonth.value.month !== today.getMonth()) {
    schedule.goToToday()
  }
}

const selectedDisplayEvents = computed(() => displayEventsOn(startOfDay(selectedDate.value)))

function dayGroupLabel(date: Date, relative = true): string {
  if (relative) {
    const diff = Math.round((date.getTime() - startOfDay(new Date()).getTime()) / 86400000)
    if (diff === 0) return '今天'
    if (diff === 1) return '明天'
  }
  const week = ['日', '一', '二', '三', '四', '五', '六'][date.getDay()]
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日 · 周${week}`
}

// ── 右侧面板：聚合（未来 7 天）/ 单日两种模式，同一套视觉 ──
const UPCOMING_DAYS = 7

interface UpcomingGroup { key: string; date: Date; label: string; events: DisplayCalendarEvent[] }

const upcomingGroups = computed<UpcomingGroup[]>(() => {
  const groups: UpcomingGroup[] = []
  for (let offset = 0; offset < UPCOMING_DAYS; offset += 1) {
    const day = startOfDay(new Date())
    day.setDate(day.getDate() + offset)
    const events = displayEventsOn(day)
    if (!events.length) continue
    groups.push({ key: localDateKey(day), date: day, label: dayGroupLabel(day), events })
  }
  return groups
})

const upcomingTotal = computed(() => upcomingGroups.value.reduce((sum, group) => sum + group.events.length, 0))

/** 7 天内没有安排时，给出加载范围内最早的下一条，避免“空”得没有方向 */
const nextBeyondWeek = computed(() => {
  if (upcomingGroups.value.length) return null
  const horizon = startOfDay(new Date())
  horizon.setDate(horizon.getDate() + UPCOMING_DAYS)
  const candidates = [
    ...external.externalEvents.value.map((item) => ({ label: dayGroupLabel(startOfDay(new Date(item.startTime))), title: item.title })),
    ...schedule.events.value
      .filter((event) => new Date(event.startTime) >= horizon)
      .map((event) => ({ label: dayGroupLabel(startOfDay(new Date(event.startTime))), title: event.title })),
  ].sort((left, right) => left.label.localeCompare(right.label))
  return candidates[0] ?? null
})

const panelMode = ref<'agenda' | 'day'>('agenda')

const panelTitle = computed(() => (panelMode.value === 'day'
  ? dayGroupLabel(startOfDay(selectedDate.value), false)
  : '最近日程'))

const panelSubtitle = computed(() => (panelMode.value === 'day'
  ? `${selectedDisplayEvents.value.length} 场安排`
  : `未来 7 天 · ${upcomingTotal.value} 场安排`))

// 弹窗的关联求职计划候选：全部进行中的计划（无论是否已录入 JD）。
// 编辑中的事件若关联了已不在候选里的计划，保留原关联避免静默清空。
const planOptions = computed(() => {
  const options = targetsStore.targets
    .filter((target) => target.status === 'active')
    .map((target) => ({
      id: target.id,
      label: target.name || '求职目标',
      jobDescriptionId: target.jobDescriptionId ?? null,
    }))
  const currentProjectId = editingEvent.value?.jobProjectId
  if (currentProjectId != null && !options.some((option) => option.id === currentProjectId)) {
    options.push({ id: currentProjectId, label: '当前关联的计划', jobDescriptionId: editingEvent.value?.jobDescriptionId ?? null })
  }
  return options
})

const sourcesOpen = ref(false)

const popoverOpen = ref(false)

function onCellClick(cell: DayCell) {
  if (cell.isSelected && panelMode.value === 'day') {
    // 再次点击已选中的日子：切换完整安排浮层
    popoverOpen.value = !popoverOpen.value
    return
  }
  selectDate(cell.date)
  popoverOpen.value = true
}

/** 浮层贴边策略：底部两行向上展开，右侧三列向左对齐 */
function popoverPositionClass(cell: DayCell): string {
  return [cell.row >= 4 ? 'up' : 'down', cell.col >= 5 ? 'align-right' : 'align-left'].join(' ')
}

function selectDate(date: Date) {
  selectedDate.value = date
  panelMode.value = 'day'
  if (date.getMonth() !== schedule.visibleMonth.value.month
    || date.getFullYear() !== schedule.visibleMonth.value.year) {
    schedule.visibleMonth.value = { year: date.getFullYear(), month: date.getMonth() }
    void schedule.load()
  }
}

const dialogOpen = ref(false)
const editingEvent = ref<ScheduleEvent | null>(null)
const submitting = ref(false)
const dialogError = ref('')
const createDefaultDate = ref<Date | null>(null)

function openCreate(defaultDate?: Date) {
  editingEvent.value = null
  dialogError.value = ''
  // 单日面板「添加」落在所选日期；其余入口传空，由弹窗兜底当前时刻
  createDefaultDate.value = defaultDate ?? null
  dialogOpen.value = true
}
const dialogDefaultDate = computed(() => createDefaultDate.value)

function openEdit(event: ScheduleEvent) {
  editingEvent.value = event
  dialogError.value = ''
  dialogOpen.value = true
}

/** 卡片编辑回调：只处理自有日程（外部日程只读） */
function openDisplayEdit(item: DisplayCalendarEvent) {
  if (item.kind !== 'own' || item.id == null) return
  const event = schedule.events.value.find((candidate) => candidate.id === item.id)
    ?? {
      id: item.id,
      title: item.title,
      eventType: (item.eventType ?? 'other') as ScheduleEventType,
      startTime: item.startTime,
      endTime: item.endTime,
      notes: item.notes ?? null,
      jobDescriptionId: item.jobDescriptionId ?? null,
      jobProjectId: item.jobProjectId ?? null,
      createdAt: '',
      updatedAt: '',
    }
  openEdit(event)
}

function closeDialog() {
  dialogOpen.value = false
  editingEvent.value = null
  dialogError.value = ''
}

async function handleSave(payload: { title: string; eventType: ScheduleEventType; startTime: string; endTime: string | null; notes: string | null; jobDescriptionId: number | null }) {
  submitting.value = true
  dialogError.value = ''
  try {
    if (editingEvent.value) {
      await schedule.editEvent(editingEvent.value.id, payload)
      ElMessage.success('日程已更新')
    } else {
      await schedule.addEvent(payload)
      ElMessage.success('日程已添加')
    }
    closeDialog()
  } catch (err) {
    dialogError.value = err instanceof Error ? err.message : '保存日程失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  const event = editingEvent.value
  if (!event || submitting.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${event.title}」吗？此操作不可恢复。`,
      '删除日程',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  submitting.value = true
  dialogError.value = ''
  try {
    await schedule.removeEvent(event.id)
    ElMessage.success('日程已删除')
    closeDialog()
  } catch (err) {
    dialogError.value = err instanceof Error ? err.message : '删除日程失败'
  } finally {
    submitting.value = false
  }
}

// ── 外部日历导入 ──
const importingSource = ref(false)

function handleImportSource(payload: { name: string; raw: string }) {
  if (importingSource.value) return
  importingSource.value = true
  try {
    const result = external.importFromText(payload.name, payload.raw)
    ElMessage.success(`已导入 ${result.count} 条日程${result.skippedRecurring ? `（${result.skippedRecurring} 条重复规则事件未包含）` : ''}`)
    sourcesOpen.value = false
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '导入失败')
  } finally {
    importingSource.value = false
  }
}

function handleRemoveSource(id: number) {
  external.remove(id)
  ElMessage.success('来源已移除')
}

onMounted(() => {
  void schedule.load()
  if (!targetsStore.targets.length && !targetsStore.loading) void targetsStore.load()
})
</script>

<style scoped>
/* 页头压缩：本页信息密度高，标题区让位给内容。
   标题固定在左侧（与导航栏留出间距），不随居中的内容区移动。 */
.schedule-view :deep(.page-header){padding:24px 8px 12px 12px}
.schedule-view :deep(.page-eyebrow){margin-bottom:3px;font-size:11px}
.schedule-view :deep(.page-title){font-size:20px}
.schedule-view :deep(.page-subtitle){margin-top:3px;font-size:12.5px}

.schedule-view{display:flex;flex-direction:column;height:100%;min-height:0}

.schedule-error{max-width:1180px;margin:0 auto 12px;width:100%;padding:10px 14px;border-radius:var(--radius-control);background:var(--danger-soft);color:var(--danger);font-size:13px}

/* 整体框架：限宽居中，避免日历在宽屏上被拉伸得过大 */
.schedule-frame{flex:1;min-height:0;display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:14px;width:100%;max-width:1180px;margin:0 auto;padding-bottom:16px}

/* ── 左：月历卡片 ── */
.calendar-card{min-height:0;display:flex;flex-direction:column;overflow-y:auto;border:1px solid var(--border-subtle);border-radius:16px;background:var(--surface-solid);padding:14px 16px 10px;box-shadow:0 1px 2px rgba(0,0,0,.03)}
.calendar-toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:10px}
.month-nav{display:flex;align-items:center;gap:4px}
.month-nav button{display:grid;width:28px;height:28px;place-items:center;border:0;border-radius:8px;background:transparent;color:var(--copy);cursor:pointer}
.month-nav button:hover{background:var(--bg-hover);color:var(--ink)}
.month-nav strong{min-width:112px;text-align:center;font-size:15px;font-weight:700;letter-spacing:-.01em;color:var(--ink)}
.toolbar-side{display:flex;align-items:center;gap:10px;min-height:28px}
.calendar-loading{color:var(--muted);font-size:12px}
.btn-primary{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--action-bg);border-radius:9px;background:var(--action-bg);color:var(--action-fg);padding:8px 14px;font-size:13px;font-weight:600;cursor:pointer}
.btn-primary:hover{background:var(--control-hover)}
.btn-small{height:27px;padding:0 11px;font-size:12px}

.week-head{display:grid;grid-template-columns:repeat(7,1fr)}
.week-head span{padding:5px 0;color:var(--muted);font-size:11px;font-weight:600;text-align:center;letter-spacing:.06em}
.week-head span.weekend{color:var(--danger);opacity:.75}

.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr);row-gap:3px}
/* 固定高度 + 溢出隐藏：点击/悬浮永不改变日历布局 */
.day-cell{position:relative;display:flex;height:68px;flex-direction:column;align-items:stretch;gap:2px;padding:5px 6px 4px;border:1px solid transparent;border-radius:9px;background:transparent;text-align:left;color:var(--ink);cursor:pointer;overflow:hidden;transition:background .15s ease,border-color .15s ease}
.day-cell:hover{background:var(--bg-hover)}
.day-cell.outside{opacity:.42}
.day-number-row{position:relative;display:flex;align-items:center;justify-content:center;height:20px;margin-bottom:1px}
.day-number-row b{color:var(--copy);font-size:12.5px;font-weight:500;line-height:1;font-variant-numeric:tabular-nums}
/* 仅日期数字获得强调；日程文字不受选中态影响 */
.day-cell.hasEvents .day-number-row b{color:var(--ink);font-weight:600}
.day-cell.selected{background:var(--surface-hover)}
.day-cell.selected .day-number-row b{display:grid;width:20px;height:20px;place-items:center;border-radius:50%;background:var(--action-bg);color:var(--action-fg);font-weight:700}
.today-dot{width:4px;height:4px;margin-left:3px;border-radius:50%;background:var(--brand)}

.cell-chip{display:flex;align-items:center;gap:4px;min-width:0;height:17px;padding:0 4px 0 5px;border-radius:5px;background:var(--surface-subtle);font-size:10.5px;line-height:1}
.cell-chip i{flex:0 0 auto;width:3px;height:11px;border-radius:2px}
.cell-chip em{flex:0 0 auto;color:var(--muted);font-style:normal;font-variant-numeric:tabular-nums}
.cell-chip b{overflow:hidden;flex:0 1 auto;min-width:0;color:var(--copy);font-weight:500;text-overflow:ellipsis;white-space:nowrap}
.cell-chip.external{background:transparent;box-shadow:inset 0 0 0 1px var(--border-subtle)}
.chip-more{position:absolute;top:8px;right:7px;color:var(--muted);font-size:10px;font-style:normal;font-weight:600;line-height:1}

/* 选中日完整安排浮层：绝对定位，不参与格子布局 */
.cell-popover{position:absolute;z-index:30;display:grid;gap:3px;min-width:180px;max-width:240px;max-height:200px;overflow-y:auto;padding:9px 11px;border:1px solid var(--border-subtle);border-radius:11px;background:var(--surface-solid);box-shadow:0 14px 36px rgba(0,0,0,.16);text-align:left}
.cell-popover.down{top:calc(100% + 5px)}
.cell-popover.up{bottom:calc(100% + 5px)}
.cell-popover.align-left{left:0}
.cell-popover.align-right{right:0;left:auto}
.popover-row{display:flex;align-items:flex-start;gap:7px;font-size:11.5px;line-height:1.45}
.popover-row em{flex:0 0 auto;color:var(--muted);font-style:normal;font-variant-numeric:tabular-nums}
.popover-row b{color:var(--ink);font-weight:500;white-space:normal}
.popover-ext{flex:0 0 auto;color:var(--external-accent);font-size:10px;font-style:normal;font-weight:600;white-space:nowrap}

.calendar-foot{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:auto;padding-top:8px}
.foot-side{display:flex;align-items:center;gap:8px}
.month-total{color:var(--muted);font-size:11.5px;font-style:normal}
.type-legend{display:flex;flex-wrap:wrap;gap:12px}
.type-legend span{display:flex;align-items:center;gap:5px;color:var(--muted);font-size:11.5px}
.type-legend i{width:8px;height:3px;border-radius:2px}
.type-legend i.legend-external{background:repeating-linear-gradient(90deg,var(--external-accent) 0 3px,transparent 3px 6px)}
.sources-link{display:inline-flex;align-items:center;gap:5px;border:0;background:none;color:var(--muted);font-size:12px;font-weight:600;cursor:pointer;padding:4px 6px;border-radius:7px}
.sources-link:hover{color:var(--ink);background:var(--bg-hover)}

/* ── 右：一体化日程面板 ── */
.panel-card{min-height:0;display:flex;flex-direction:column;border:1px solid var(--border-subtle);border-radius:16px;background:var(--surface-solid);padding:12px 12px 8px;box-shadow:0 1px 2px rgba(0,0,0,.03)}
.panel-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px;padding:2px 2px 10px;border-bottom:1px solid var(--border-subtle)}
.panel-head-copy{display:grid;gap:2px;min-width:0}
.back-link{display:inline-flex;align-items:center;gap:4px;justify-self:start;border:0;background:none;color:var(--muted);font-size:11px;font-weight:600;cursor:pointer;padding:2px 4px 2px 0}
.back-link:hover{color:var(--ink)}
.panel-head strong{overflow:hidden;color:var(--ink);font-size:15px;text-overflow:ellipsis;white-space:nowrap}
.panel-head small{color:var(--muted);font-size:11px}
.panel-head button:not(.back-link){display:inline-flex;align-items:center;gap:4px;flex:0 0 auto;border:0;background:none;color:var(--ink);font-size:12px;font-weight:600;cursor:pointer;padding:5px 9px;border-radius:7px}
.panel-head button:not(.back-link):hover{background:var(--bg-hover)}

.agenda{flex:1;min-height:0;overflow-y:auto;padding:2px 2px 4px}
.agenda-group+.agenda-group{margin-top:14px}
.agenda-date{position:sticky;top:-2px;z-index:2;display:flex;align-items:center;gap:8px;margin:0 -2px;padding:7px 2px 5px;background:var(--surface-solid);border-bottom:1px solid var(--border-subtle)}
.agenda-date b{color:var(--ink);font-size:12px;font-weight:700}
.agenda-date em{margin-left:auto;color:var(--muted);font-size:11px;font-style:normal}

.day-empty{display:grid;justify-items:center;gap:7px;padding:40px 8px;color:var(--muted);text-align:center}
.compact-empty{justify-items:start;padding:22px 2px;text-align:left}
.compact-empty p{margin:0;color:var(--muted);font-size:13px}
.next-hint{color:var(--ink)!important;font-size:12.5px!important;font-weight:600}
.empty-mark{display:grid;width:46px;height:46px;place-items:center;border-radius:14px;background:var(--surface-subtle);color:var(--ink);margin-bottom:2px}
.day-empty strong{color:var(--ink);font-size:13.5px}
.day-empty p{margin:0;font-size:12.5px;line-height:1.6}

/* 共享断点阶梯：窄窗口下日历与事件面板上下堆叠 */
@media (max-width: 959px) {
  .schedule-frame{grid-template-columns:minmax(0,1fr);grid-template-rows:minmax(0,1fr) auto;padding-bottom:12px}
  .panel-card{max-height:44vh}
  .day-cell{height:52px}
  .cell-chip em{display:none}
}
</style>
