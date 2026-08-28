// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ScheduleView from './ScheduleView.vue'
import ScheduleSourcesDialog from '../../components/schedule/ScheduleSourcesDialog.vue'
import type { ScheduleEvent } from '../../types/schedule'

const scheduleMock = vi.hoisted(() => ({
  visibleMonth: { value: { year: 2026, month: 7 } },
  monthEvents: { value: [] },
  events: { value: [] as ScheduleEvent[] },
  loading: { value: false },
  errorMessage: { value: '' },
  load: vi.fn(),
  goToMonth: vi.fn(),
  goToToday: vi.fn(),
  addEvent: vi.fn(),
  editEvent: vi.fn(),
  removeEvent: vi.fn(),
  eventsOn: vi.fn(),
}))
const targets = vi.hoisted(() => ({
  targets: [] as Array<Record<string, unknown>>,
  loading: false,
  errorMessage: '',
  load: vi.fn(),
}))
const externalMock = vi.hoisted(() => ({
  sources: { value: [] as Array<Record<string, unknown>> },
  externalEvents: { value: [] as Array<Record<string, unknown>> },
  importFromText: vi.fn(),
  remove: vi.fn(),
}))
const routerPush = vi.hoisted(() => vi.fn())
const elMessageBoxConfirm = vi.hoisted(() => vi.fn())
const elMessageSuccess = vi.hoisted(() => vi.fn())
const elMessageError = vi.hoisted(() => vi.fn())

vi.mock('element-plus', () => ({
  ElMessage: { success: elMessageSuccess, error: elMessageError },
  ElMessageBox: { confirm: elMessageBoxConfirm },
}))
vi.mock('../../composables/useSchedule', () => ({ useSchedule: () => scheduleMock }))
vi.mock('../../composables/useExternalCalendars', () => ({ useExternalCalendars: () => externalMock }))
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targets }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: routerPush }) }))

const stubs = {
  PageHeader: true,
  'el-icon': { template: '<i><slot /></i>' },
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function dayKey(offsetDays = 0): string {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function timeAt(dayOffset: number, hour: number, minute = 0): string {
  return `${dayKey(dayOffset)}T${pad(hour)}:${pad(minute)}:00`
}

function makeEvent(partial: Partial<ScheduleEvent>): ScheduleEvent {
  return {
    id: 0,
    title: '',
    eventType: 'interview',
    startTime: '',
    endTime: null,
    notes: null,
    jobDescriptionId: null,
    jobProjectId: null,
    createdAt: '',
    updatedAt: '',
    ...partial,
  }
}

function inputValue(wrapper: ReturnType<typeof mount>, selector: string): string {
  return (wrapper.get(selector).element as HTMLInputElement).value
}

function existsIn(wrapper: ReturnType<typeof mount>, selector: string): boolean {
  return wrapper.find(selector).exists()
}

async function openDayPanel(wrapper: ReturnType<typeof mount>, offsetDays = 0) {
  await wrapper.get(`[data-test="day-cell-${dayKey(offsetDays)}"]`).trigger('click')
  await flushPromises()
}

describe('ScheduleView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    // visibleMonth 用真实 ref，保证视图对月份切换的响应式更新
    scheduleMock.visibleMonth = ref({ year: 2026, month: 7 })
    elMessageBoxConfirm.mockReset()
    elMessageBoxConfirm.mockResolvedValue('confirm' as never)
    scheduleMock.editEvent.mockResolvedValue(undefined)
    scheduleMock.removeEvent.mockResolvedValue(undefined)
    scheduleMock.addEvent.mockResolvedValue(undefined)
    scheduleMock.eventsOn.mockReturnValue([])
    scheduleMock.events.value = []
    scheduleMock.loading.value = false
    externalMock.sources.value = []
    externalMock.externalEvents.value = []
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
  })

  it('renders the calendar card and defaults to the unified agenda with an empty state', async () => {
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.get('[data-test="month-label"]').text()).toBe('2026 年 8 月')
    expect(existsIn(wrapper, '[data-test="upcoming-list"]')).toBe(true)
    expect(wrapper.text()).toContain('未来 7 天没有安排')
    expect(scheduleMock.load).toHaveBeenCalled()
  })

  it('hides 回到今天 when already on today and shows it when browsing other days', async () => {
    const now = new Date()
    scheduleMock.visibleMonth.value = { year: now.getFullYear(), month: (now.getMonth() + 11) % 12 }
    scheduleMock.goToToday.mockImplementation(() => {
      scheduleMock.visibleMonth.value = { year: now.getFullYear(), month: now.getMonth() }
    })
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.get('[data-test="back-to-today"]').text()).toBe('回到今天')
    await wrapper.get('[data-test="back-to-today"]').trigger('click')
    await flushPromises()
    expect(scheduleMock.goToToday).toHaveBeenCalled()
    expect(wrapper.find('[data-test="back-to-today"]').exists()).toBe(false)
  })

  it('shows a loading hint while the calendar is refreshing', async () => {
    scheduleMock.loading.value = true
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.get('[data-test="calendar-loading"]').text()).toBe('载入中…')
  })

  it('renders time chips inside day cells so same-day plans are distinguishable', async () => {
    // 锚定在 mock 可见月（2026-08）内，避免与真实“今天”产生跨月耦合
    scheduleMock.eventsOn.mockImplementation((date: Date) => (
      date.getTime() === new Date(2026, 7, 26).getTime()
        ? [
            makeEvent({ id: 1, title: '上午笔试', eventType: 'exam', startTime: '2026-08-26T09:30:00' }),
            makeEvent({ id: 2, title: '下午技术面', startTime: '2026-08-26T15:00:00' }),
          ]
        : []
    ))
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    const cell = wrapper.get('[data-test="day-cell-2026-08-26"]')
    const chips = cell.findAll('.cell-chip')
    expect(chips).toHaveLength(2)
    expect(chips[0].text()).toContain('09:30')
    expect(chips[0].text()).toContain('上午笔试')
    expect(chips[1].text()).toContain('15:00')
    expect(chips[1].text()).toContain('下午技术面')
    expect(cell.text()).not.toContain('+1')
  })

  it('shows the full plan in a popover on selection without altering other cells', async () => {
    scheduleMock.eventsOn.mockImplementation((date: Date) => (
      date.getTime() === new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate()).getTime()
        ? [
            makeEvent({ id: 1, title: '阿里巴巴技术二面（含很长的岗位与轮次说明文字）', startTime: timeAt(0, 10) }),
            makeEvent({ id: 2, title: '下午 HR 沟通', startTime: timeAt(0, 16) }),
          ]
        : []
    ))
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    await openDayPanel(wrapper)
    const popover = wrapper.get(`[data-test="cell-popover-${dayKey(0)}"]`)
    expect(popover.text()).toContain('阿里巴巴技术二面（含很长的岗位与轮次说明文字）')
    expect(popover.text()).toContain('下午 HR 沟通')

    // 再点一次同一格：关闭浮层
    await wrapper.get(`[data-test="day-cell-${dayKey(0)}"]`).trigger('click')
    await flushPromises()
    expect(existsIn(wrapper, `[data-test="cell-popover-${dayKey(0)}"]`)).toBe(false)
  })

  it('groups upcoming agenda by day and can enter/leave single-day mode', async () => {
    scheduleMock.eventsOn.mockImplementation((date: Date) => {
      const key = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
      if (key === dayKey(1)) return [makeEvent({ id: 12, title: '明天上午笔试', eventType: 'exam', startTime: timeAt(1, 9) })]
      return []
    })
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    const list = wrapper.get('[data-test="upcoming-list"]')
    expect(list.text()).toContain('明天')
    expect(list.text()).toContain('明天上午笔试')

    // 点击日历某天 → 单日模式；返回按钮 → 回到聚合日程
    await openDayPanel(wrapper, 1)
    expect(wrapper.get('[data-test="selected-day-label"]').text()).toBeTruthy()
    expect(wrapper.text()).toContain('明天上午笔试')

    await wrapper.get('[data-test="panel-back"]').trigger('click')
    await flushPromises()
    expect(existsIn(wrapper, '[data-test="upcoming-list"]')).toBe(true)
  })

  it('merges external readonly events into lists without edit affordance', async () => {
    externalMock.externalEvents.value = [
      { key: 'ext-1-u1', title: '飞书例会', startTime: timeAt(0, 10), endTime: null, allDay: false, kind: 'external', sourceId: 1, sourceName: '飞书日历' },
    ]
    scheduleMock.eventsOn.mockReturnValue([
      makeEvent({ id: 7, title: '下午面试', startTime: timeAt(0, 16) }),
    ])
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    const list = wrapper.get('[data-test="upcoming-list"]')
    expect(list.text()).toContain('飞书例会')
    expect(wrapper.get('[data-test="event-card-ext-1-u1"]').find('.event-source').text()).toBe('飞书日历')

    // 外部事件卡片不可点击编辑（渲染为 div）
    const externalCard = wrapper.get('[data-test="event-card-ext-1-u1"] .day-event-main')
    expect(externalCard.element.tagName).toBe('DIV')
  })

  it('shows target context only for linked events in the day panel', async () => {
    scheduleMock.eventsOn.mockReturnValue([
      makeEvent({ id: 1, title: '技术二面', startTime: timeAt(0, 16), jobDescriptionId: 6 }),
      makeEvent({ id: 2, title: '行测', eventType: 'exam', startTime: timeAt(0, 10) }),
    ])
    targets.targets = [
      { id: 3, name: '腾讯 · Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()
    await openDayPanel(wrapper)

    const cards = wrapper.findAll('[data-test^="event-card-own-"]')
    expect(cards).toHaveLength(2)
    const contexts = wrapper.findAll('[data-test="event-target-context"]')
    expect(contexts).toHaveLength(1)
    expect(contexts[0].text()).toContain('腾讯 · Java 后端实习')

    await wrapper.get('[data-test="event-target-link"]').trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'targets', query: { targetId: '3' } })
  })

  it('keeps target context when schedule binding ids arrive as strings', async () => {
    scheduleMock.eventsOn.mockReturnValue([
      makeEvent({ id: 2, title: '技术二面', startTime: timeAt(0, 16), jobDescriptionId: '6' as unknown as number, jobProjectId: '3' as unknown as number }),
    ])
    targets.targets = [
      { id: 3, name: '腾讯 · Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()
    await openDayPanel(wrapper)

    expect(wrapper.get('[data-test="event-target-context"]').text()).toContain('腾讯 · Java 后端实习')
  })

  it('keeps endTime and job association when editing an event', async () => {
    // 回归用例：此前编辑会把 endTime/jobDescriptionId 静默清空
    scheduleMock.eventsOn.mockReturnValue([
      makeEvent({ id: 7, title: '技术二面', startTime: timeAt(0, 16), endTime: timeAt(0, 18), jobDescriptionId: 6 }),
    ])
    targets.targets = [
      { id: 3, name: '腾讯 · Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()
    await openDayPanel(wrapper)

    await wrapper.get('[data-test="event-card-own-7"]').find('button').trigger('click')
    await flushPromises()

    expect(inputValue(wrapper, '[data-test="event-end-time"]')).toBe('18:00')
    // 编辑回退：旧日程仅含 JD，自动匹配到对应计划
    expect(inputValue(wrapper, '[data-test="event-plan"]')).toBe('3')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(scheduleMock.editEvent).toHaveBeenCalledWith(7, {
      title: '技术二面',
      eventType: 'interview',
      startTime: timeAt(0, 16),
      endTime: timeAt(0, 18),
      notes: null,
      jobDescriptionId: 6,
      jobProjectId: 3,
    })
    expect(elMessageSuccess).toHaveBeenCalledWith('日程已更新')
  })

  it('asks for confirmation before deleting and aborts on cancel', async () => {
    scheduleMock.eventsOn.mockReturnValue([
      makeEvent({ id: 8, title: 'HR 沟通', eventType: 'followup', startTime: timeAt(0, 11) }),
    ])
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()
    await openDayPanel(wrapper)

    await wrapper.get('[data-test="event-card-own-8"]').find('button').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="event-delete"]').trigger('click')
    await flushPromises()

    expect(elMessageBoxConfirm).toHaveBeenCalledWith(expect.stringContaining('HR 沟通'), '删除日程', expect.objectContaining({ type: 'warning' }))
    expect(scheduleMock.removeEvent).toHaveBeenCalledWith(8)
    expect(elMessageSuccess).toHaveBeenCalledWith('日程已删除')

    elMessageBoxConfirm.mockRejectedValue(new Error('cancel') as never)
    scheduleMock.removeEvent.mockClear()
    await wrapper.get('[data-test="event-card-own-8"]').find('button').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="event-delete"]').trigger('click')
    await flushPromises()

    expect(scheduleMock.removeEvent).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="event-delete"]').exists()).toBe(true)
  })

  it('imports an external calendar source through the dialog flow', async () => {
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()

    await wrapper.get('[data-test="sources-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('导入其他日历')

    externalMock.importFromText.mockImplementation((_name: string, raw: string) => ({ count: raw.length ? 3 : 0, skippedRecurring: 0 }))
    const dialog = wrapper.findComponent(ScheduleSourcesDialog)
    dialog.vm.$emit('import', { name: '飞书日历', raw: validIcsLike() })
    await flushPromises()

    expect(externalMock.importFromText).toHaveBeenCalledWith('飞书日历', validIcsLike())
    expect(elMessageSuccess).toHaveBeenCalledWith(expect.stringContaining('已导入 3 条日程'))
    expect(externalMock.remove).not.toHaveBeenCalled()
  })

  function validIcsLike(): string {
    return ['BEGIN:VEVENT', 'SUMMARY:x', 'DTSTART:20260825T140000', 'END:VEVENT'].join('\n')
  }

  it('creates events with explicit endTime and job association passthrough', async () => {
    targets.targets = [
      { id: 5, name: '字节 · 前端实习', status: 'active', jobDescriptionId: 12, resumeVersionId: null, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    const wrapper = mount(ScheduleView, { global: { stubs } })
    await flushPromises()
    await openDayPanel(wrapper)

    await wrapper.get('[data-test="add-event-day"]').trigger('click')
    await flushPromises()

    await wrapper.get('[data-test="event-title"]').setValue('复试')
    await wrapper.get('[data-test="event-end-time"]').setValue('17:00')
    await wrapper.get('[data-test="event-plan"]').setValue('5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(scheduleMock.addEvent).toHaveBeenCalledWith({
      title: '复试',
      eventType: 'interview',
      startTime: `${dayKey(0)}T00:00:00`,
      endTime: `${dayKey(0)}T17:00:00`,
      notes: null,
      jobDescriptionId: 12,
      jobProjectId: 5,
    })
  })
})
