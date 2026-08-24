import { computed, ref } from 'vue'
import {
  createScheduleEvent,
  deleteScheduleEvent,
  listScheduleEvents,
  updateScheduleEvent,
} from '../api/schedule'
import type {
  CreateScheduleEventRequest,
  ScheduleEvent,
  ScheduleEventType,
  UpdateScheduleEventRequest,
} from '../types/schedule'

function toIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

/**
 * 本地月历日程：按当前月份加载事件，提供新增/编辑/删除。
 * 月份以“可见月”为中心，加载前后各 31 天，保证跨月视图完整。
 */
export function useSchedule() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const visibleMonth = ref({ year: today.getFullYear(), month: today.getMonth() })
  const events = ref<ScheduleEvent[]>([])
  const loading = ref(false)
  const errorMessage = ref('')

  /** 当前可见月的事件（按开始时间升序） */
  const monthEvents = computed(() => {
    const { year, month } = visibleMonth.value
    return events.value
      .filter((event) => {
        const start = new Date(event.startTime)
        return start.getFullYear() === year && start.getMonth() === month
      })
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
  })

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
      const { year, month } = visibleMonth.value
      const windowFrom = new Date(year, month, 1)
      windowFrom.setDate(windowFrom.getDate() - 31)
      const windowTo = new Date(year, month + 1, 1)
      windowTo.setDate(windowTo.getDate() + 31)
      // 「即将到来」面板固定展示未来 7 天：无论可见月离当前月多远，加载窗口都要覆盖今天起的 7 天
      const upcomingFrom = new Date(today.getFullYear(), today.getMonth(), today.getDate())
      const upcomingTo = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7)
      const from = windowFrom < upcomingFrom ? windowFrom : upcomingFrom
      const to = windowTo > upcomingTo ? windowTo : upcomingTo
      const res = await listScheduleEvents(toIso(from), toIso(to))
      events.value = res.data ?? []
    } catch (err) {
      errorMessage.value = err instanceof Error ? err.message : '加载日程失败'
    } finally {
      loading.value = false
    }
  }

  function goToMonth(offset: number) {
    const { year, month } = visibleMonth.value
    const next = new Date(year, month + offset, 1)
    visibleMonth.value = { year: next.getFullYear(), month: next.getMonth() }
    void load()
  }

  function goToToday() {
    visibleMonth.value = { year: today.getFullYear(), month: today.getMonth() }
    void load()
  }

  async function addEvent(request: CreateScheduleEventRequest) {
    const res = await createScheduleEvent(request)
    await load()
    return res.data
  }

  async function editEvent(id: number, request: UpdateScheduleEventRequest) {
    const res = await updateScheduleEvent(id, request)
    await load()
    return res.data
  }

  async function removeEvent(id: number) {
    await deleteScheduleEvent(id)
    await load()
  }

  function eventsOn(date: Date): ScheduleEvent[] {
    const key = toIso(new Date(date.getFullYear(), date.getMonth(), date.getDate())).slice(0, 10)
    return events.value
      .filter((event) => event.startTime.slice(0, 10) === key)
      .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
  }

  return {
    visibleMonth,
    monthEvents,
    events,
    loading,
    errorMessage,
    load,
    goToMonth,
    goToToday,
    addEvent,
    editEvent,
    removeEvent,
    eventsOn,
  }
}

export type { ScheduleEventType }
