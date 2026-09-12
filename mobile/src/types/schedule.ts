export type ScheduleEventType = 'interview' | 'exam' | 'followup' | 'other'

export interface ScheduleEvent {
  id: number
  title: string
  eventType: ScheduleEventType
  startTime: string
  endTime: string | null
  notes: string | null
  /** 赛后复盘心得；notes 是赛前要看的，心得是结束后写的，两者不能挤在同一个字段里。 */
  review?: string | null
  jobDescriptionId: number | null
  jobProjectId: number | null
  createdAt: string
  updatedAt: string
}

/** 未填结束时间时按一小时估长度——与 .ics 生成用的是同一个口径。 */
export const DEFAULT_EVENT_DURATION_MIN = 60

/** 该日程的预计结束时刻（毫秒）。时间字段非法时退回开始时间，保证调用方拿到可比数值。 */
export function eventEndsAt(ev: Pick<ScheduleEvent, 'startTime' | 'endTime'>): number {
  const start = new Date(ev.startTime).getTime()
  if (!Number.isFinite(start)) return 0
  const end = ev.endTime ? new Date(ev.endTime).getTime() : NaN
  if (!Number.isFinite(end) || end <= start) return start + DEFAULT_EVENT_DURATION_MIN * 60_000
  return end
}
export function isEventFinished(ev: Pick<ScheduleEvent, 'startTime' | 'endTime'>, now = Date.now()): boolean {
  return eventEndsAt(ev) <= now
}

export interface CreateScheduleEventRequest {
  title: string
  eventType: ScheduleEventType
  startTime: string
  endTime?: string | null
  notes?: string | null
  jobDescriptionId?: number | null
  jobProjectId?: number | null
}

export interface UpdateScheduleEventRequest extends CreateScheduleEventRequest {}

/** 日程编辑弹窗提交的完整表单载荷（含结束时间与岗位关联） */
export interface ScheduleEventFormPayload {
  title: string
  eventType: ScheduleEventType
  startTime: string
  endTime: string | null
  notes: string | null
  jobDescriptionId: number | null
  jobProjectId: number | null
}

/** 从 .ics 等外部日历导入的只读来源（原始文本保存在本地，不上传） */
export interface ExternalCalendarSource {
  id: number
  name: string
  importedAt: string
  raw: string
}

/** 日历视图统一展示模型：自有日程 + 外部只读日程 */
export interface DisplayCalendarEvent {
  key: string
  title: string
  startTime: string
  endTime: string | null
  allDay: boolean
  kind: 'own' | 'external'
  /** kind === 'own' 时有效 */
  id?: number
  eventType?: ScheduleEventType
  notes?: string | null
  jobDescriptionId?: number | null
  jobProjectId?: number | null
  /** kind === 'external' 时有效 */
  sourceId?: number
  sourceName?: string
}

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
