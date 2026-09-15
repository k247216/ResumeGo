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
  /** 心得里的标签（如「算法」「挂了」），用来在复盘墙里快速归类与筛选；纯本地、零网络。 */
  reviewTags?: string[]
  /** 便签底色（noteColors 调色板内的值）；null 表示跟随日程类型色。只管心得墙卡片。 */
  reviewColor?: string | null
  /** 编辑器的纸张风格（plain 白 / cream 米 / kraft 牛皮），与便签色无关。 */
  reviewPaper?: string | null
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

/** 日程三态：未开始 / 进行中（已开始未结束）/ 已结束。复盘入口与状态徽标跟它走。 */
export type EventStatus = 'upcoming' | 'ongoing' | 'finished'

export function eventStatus(ev: Pick<ScheduleEvent, 'startTime' | 'endTime'>, now = Date.now()): EventStatus {
  const start = new Date(ev.startTime).getTime()
  // 时间字段非法按已结束兜底：宁可多给一个复盘入口，也不要让记录被时间锁死。
  if (!Number.isFinite(start)) return 'finished'
  if (start > now) return 'upcoming'
  return eventEndsAt(ev) <= now ? 'finished' : 'ongoing'
}

/**
 * 表单时间校验：返回错误文案，null 表示通过。
 * 结束时间一旦填写就必须晚于开始时间——eventEndsAt 虽然会把 end<=start 兜成默认时长，
 * 但那是给脏数据兜底的，不该给新写入的数据开口子。
 */
export function scheduleTimeError(startTime: string | null, endTime: string | null): string | null {
  if (!startTime) return '请选择开始时间'
  const start = new Date(startTime).getTime()
  if (!Number.isFinite(start)) return '开始时间无效'
  if (endTime) {
    const end = new Date(endTime).getTime()
    if (!Number.isFinite(end)) return '结束时间无效'
    if (end <= start) return '结束时间需要晚于开始时间'
  }
  return null
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
