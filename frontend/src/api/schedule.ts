import type {
  ApiResponse,
  CreateScheduleEventRequest,
  ScheduleEvent,
  UpdateScheduleEventRequest,
} from '../types/schedule'
import { apiFetch } from './http'

const BASE = '/api/v1/schedule-events'

function parseError(res: Response, fallback: string): Promise<Error> {
  return res.json().then((err) => {
    throw new Error(err.message || err.error?.message || fallback)
  }).catch((e) => {
    if (e instanceof Error) throw e
    throw new Error(fallback)
  })
}

/** 按时间范围查询日程（from/to 可选，ISO 本地时间） */
export async function listScheduleEvents(from?: string, to?: string): Promise<ApiResponse<ScheduleEvent[]>> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const query = params.toString() ? `?${params.toString()}` : ''
  const res = await apiFetch(`${BASE}${query}`)
  if (!res.ok) await parseError(res, '获取日程失败')
  return res.json()
}

export async function createScheduleEvent(req: CreateScheduleEventRequest): Promise<ApiResponse<ScheduleEvent>> {
  const res = await apiFetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  if (!res.ok) await parseError(res, '创建日程失败')
  return res.json()
}

export async function updateScheduleEvent(id: number, req: UpdateScheduleEventRequest): Promise<ApiResponse<ScheduleEvent>> {
  const res = await apiFetch(`${BASE}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  if (!res.ok) await parseError(res, '更新日程失败')
  return res.json()
}

export async function deleteScheduleEvent(id: number): Promise<ApiResponse<null>> {
  const res = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) await parseError(res, '删除日程失败')
  return res.json()
}
