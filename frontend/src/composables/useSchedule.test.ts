import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createScheduleEvent,
  deleteScheduleEvent,
  listScheduleEvents,
  updateScheduleEvent,
} from '../api/schedule'
import { useSchedule } from './useSchedule'

vi.mock('../api/schedule', () => ({
  listScheduleEvents: vi.fn(),
  createScheduleEvent: vi.fn(),
  updateScheduleEvent: vi.fn(),
  deleteScheduleEvent: vi.fn(),
}))

const baseEvent = {
  id: 1,
  title: '腾讯技术面',
  eventType: 'interview' as const,
  startTime: '2026-08-25T14:00:00',
  endTime: null,
  notes: null,
  jobDescriptionId: null,
  createdAt: '',
  updatedAt: '',
}

describe('useSchedule', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads events around the visible month and groups them by day', async () => {
    vi.mocked(listScheduleEvents).mockResolvedValue({
      success: true,
      data: [
        baseEvent,
        { ...baseEvent, id: 2, title: '在线笔试', eventType: 'exam', startTime: '2026-08-28T19:00:00' },
        { ...baseEvent, id: 3, title: '下月面试', startTime: '2026-09-02T10:00:00' },
      ],
      message: null,
    })
    const schedule = useSchedule()
    await schedule.load()

    expect(schedule.events.value).toHaveLength(3)
    expect(schedule.monthEvents.value.map((event) => event.id)).toEqual([1, 2])
    expect(schedule.eventsOn(new Date(2026, 7, 25)).map((event) => event.id)).toEqual([1])
    expect(schedule.eventsOn(new Date(2026, 7, 26))).toEqual([])
  })

  it('keeps loaded events when a refresh fails', async () => {
    vi.mocked(listScheduleEvents)
      .mockResolvedValueOnce({ success: true, data: [baseEvent], message: null })
      .mockRejectedValueOnce(new Error('加载失败'))
    const schedule = useSchedule()
    await schedule.load()
    await schedule.load()
    expect(schedule.events.value).toEqual([baseEvent])
    expect(schedule.errorMessage.value).toBe('加载失败')
  })

  it('adds, edits and removes events through the api', async () => {
    vi.mocked(listScheduleEvents).mockResolvedValue({ success: true, data: [], message: null })
    vi.mocked(createScheduleEvent).mockResolvedValue({ success: true, data: baseEvent, message: null })
    vi.mocked(updateScheduleEvent).mockResolvedValue({ success: true, data: baseEvent, message: null })
    vi.mocked(deleteScheduleEvent).mockResolvedValue({ success: true, data: null, message: null })

    const schedule = useSchedule()
    await schedule.addEvent({ title: '腾讯技术面', eventType: 'interview', startTime: '2026-08-25T14:00:00', notes: null })
    await schedule.editEvent(1, { title: '腾讯技术面', eventType: 'interview', startTime: '2026-08-25T14:00:00', notes: null })
    await schedule.removeEvent(1)

    expect(createScheduleEvent).toHaveBeenCalledTimes(1)
    expect(updateScheduleEvent).toHaveBeenCalledWith(1, expect.objectContaining({ title: '腾讯技术面' }))
    expect(deleteScheduleEvent).toHaveBeenCalledWith(1)
  })

  it('navigates months and reloads', async () => {
    vi.mocked(listScheduleEvents).mockResolvedValue({ success: true, data: [], message: null })
    const schedule = useSchedule()
    await schedule.load()
    schedule.goToMonth(-1)
    expect(listScheduleEvents).toHaveBeenCalledTimes(2)
    schedule.goToToday()
    const now = new Date()
    expect(schedule.visibleMonth.value).toEqual({ year: now.getFullYear(), month: now.getMonth() })
  })

  it('always keeps the next 7 days inside the loaded window', async () => {
    // 「即将到来」面板固定展示未来 7 天：即使可见月远离当前月，查询起点也不能晚于今天
    vi.mocked(listScheduleEvents).mockResolvedValue({ success: true, data: [], message: null })
    const schedule = useSchedule()
    const now = new Date()
    schedule.visibleMonth.value = { year: now.getFullYear() + 1, month: now.getMonth() }
    await schedule.load()

    const [from] = vi.mocked(listScheduleEvents).mock.lastCall!
    const pad = (value: number) => String(value).padStart(2, '0')
    expect(from).toBe(`${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T00:00:00`)
  })
})
