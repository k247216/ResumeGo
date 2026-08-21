export type ScheduleEventType = 'interview' | 'exam' | 'followup' | 'other'

export interface ScheduleEvent {
  id: number
  title: string
  eventType: ScheduleEventType
  startTime: string
  endTime: string | null
  notes: string | null
  jobDescriptionId: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateScheduleEventRequest {
  title: string
  eventType: ScheduleEventType
  startTime: string
  endTime?: string | null
  notes?: string | null
  jobDescriptionId?: number | null
}

export interface UpdateScheduleEventRequest extends CreateScheduleEventRequest {}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export const SCHEDULE_EVENT_TYPE_LABELS: Record<ScheduleEventType, string> = {
  interview: '面试',
  exam: '笔试',
  followup: '跟进',
  other: '其他',
}

/** 类型展示色（同时用于日历格中的事件点与列表标签） */
export const SCHEDULE_EVENT_TYPE_COLORS: Record<ScheduleEventType, string> = {
  interview: 'var(--brand, #168866)',
  exam: '#2563eb',
  followup: '#d97706',
  other: '#9a9c9e',
}
