// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ScheduleView from './ScheduleView.vue'

const scheduleMock = vi.hoisted(() => ({
  visibleMonth: { value: { year: 2026, month: 7 } },
  monthEvents: { value: [] },
  events: { value: [] },
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
const routerPush = vi.hoisted(() => vi.fn())

vi.mock('../../composables/useSchedule', () => ({ useSchedule: () => scheduleMock }))
vi.mock('../../stores/targets', () => ({ useTargetsStore: () => targets }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: routerPush }) }))
vi.mock('../../components/schedule/ScheduleEventDialog.vue', () => ({ default: { name: 'ScheduleEventDialog', template: '<div />' } }))

function todayTime(hour: number, minute: number): string {
  const today = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}T${pad(hour)}:${pad(minute)}:00`
}

describe('ScheduleView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    targets.targets = []
    targets.load.mockResolvedValue(undefined)
    scheduleMock.eventsOn.mockReturnValue([])
  })

  it('renders the calendar with the current month label and an empty day panel', async () => {
    const wrapper = mount(ScheduleView, { global: { stubs: ['PageHeader'] } })
    await flushPromises()
    expect(wrapper.get('[data-test="month-label"]').text()).toBe('2026 年 8 月')
    expect(wrapper.find('[data-test="selected-day-label"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('这一天还没有安排')
    expect(scheduleMock.load).toHaveBeenCalled()
  })

  it('shows a target detail link for linked events and none for unlinked ones', async () => {
    scheduleMock.eventsOn.mockReturnValue([
      { id: 1, title: '技术二面', eventType: 'interview', startTime: todayTime(16, 0), endTime: null, notes: null, jobDescriptionId: 6, createdAt: '', updatedAt: '' },
      { id: 2, title: '行测', eventType: 'exam', startTime: todayTime(10, 0), endTime: null, notes: null, jobDescriptionId: null, createdAt: '', updatedAt: '' },
    ])
    targets.targets = [
      { id: 3, name: '腾讯 · Java 后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 9, archivedAt: null, createdAt: '', updatedAt: '' },
    ]
    const wrapper = mount(ScheduleView, { global: { stubs: ['PageHeader'] } })
    await flushPromises()

    const cards = wrapper.findAll('[data-test^="event-card-"]')
    expect(cards).toHaveLength(2)
    const contexts = wrapper.findAll('[data-test="event-target-context"]')
    expect(contexts).toHaveLength(1)
    expect(contexts[0].text()).toContain('腾讯 · Java 后端实习')
    expect(contexts[0].text()).toContain('目标详情')

    await wrapper.get('[data-test="event-target-link"]').trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'targets', query: { targetId: '3' } })
  })
})
