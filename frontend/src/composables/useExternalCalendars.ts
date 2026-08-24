import { computed, ref } from 'vue'
import { parseIcs } from '../utils/ics'
import type { DisplayCalendarEvent, ExternalCalendarSource } from '../types/schedule'

const STORAGE_KEY = 'resumego:external-calendars:v1'
const MAX_SOURCES = 5
const MAX_RAW_LENGTH = 200_000

function loadSources(): ExternalCalendarSource[] {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is ExternalCalendarSource => (
      item && typeof item.id === 'number' && typeof item.name === 'string' && typeof item.raw === 'string'
    ))
  } catch {
    return []
  }
}

/**
 * 外部日历（.ics 只读叠加层）。
 * 原始 ICS 文本只保存在本地 localStorage，解析在渲染进程完成，不经过任何网络请求。
 */
export function useExternalCalendars() {
  const sources = ref<ExternalCalendarSource[]>(loadSources())
  const parseCache = new Map<number, string>()

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sources.value))
    } catch {
      // 存储不可用（隐私模式等）：保持内存态即可
    }
  }

  function parsedEvents(source: ExternalCalendarSource) {
    const cached = parseCache.get(source.id)
    if (cached === source.raw) return parseIcs(source.raw)
    parseCache.set(source.id, source.raw)
    return parseIcs(source.raw)
  }

  /** 导入一份 .ics 文本；解析不到事件时抛错，由调用方提示 */
  function importFromText(name: string, raw: string): { count: number; skippedRecurring: number } {
    const trimmed = raw.trim()
    if (!trimmed) throw new Error('文件内容为空')
    if (trimmed.length > MAX_RAW_LENGTH) throw new Error('文件过大：请控制在 200KB 以内（可在导出时缩小时间范围）')
    const parsed = parseIcs(trimmed)
    if (!parsed.events.length) {
      const reason = parsed.skippedRecurring > 0
        ? `未导入任何日程；有 ${parsed.skippedRecurring} 条重复规则（RRULE）事件暂不支持`
        : '未解析到任何日程，请确认这是 .ics / iCal 格式'
      throw new Error(reason)
    }
    const source: ExternalCalendarSource = {
      id: Date.now(),
      name: name.trim() || '外部日历',
      importedAt: new Date().toISOString(),
      raw: trimmed,
    }
    sources.value = [source, ...sources.value].slice(0, MAX_SOURCES)
    persist()
    return { count: parsed.events.length, skippedRecurring: parsed.skippedRecurring }
  }

  function remove(id: number) {
    sources.value = sources.value.filter((source) => source.id !== id)
    parseCache.delete(id)
    persist()
  }

  /** 展平为视图模型；过去 90 天以前的不再产出，控制列表规模 */
  function flatten(): DisplayCalendarEvent[] {
    const horizon = new Date()
    horizon.setDate(horizon.getDate() - 90)
    const result: DisplayCalendarEvent[] = []
    for (const source of sources.value) {
      const parsed = parsedEvents(source)
      for (const [index, event] of parsed.events.entries()) {
        if (event.endTime ? new Date(event.endTime) < horizon : new Date(event.startTime) < horizon) continue
        result.push({
          key: `ext-${source.id}-${event.uid ?? index}`,
          title: event.title,
          startTime: event.startTime,
          endTime: event.endTime,
          allDay: event.allDay,
          kind: 'external',
          sourceId: source.id,
          sourceName: source.name,
        })
      }
    }
    return result.sort((left, right) => left.startTime.localeCompare(right.startTime))
  }

  const externalEvents = computed(flatten)

  return { sources, externalEvents, importFromText, remove }
}

export type ExternalCalendarsStore = ReturnType<typeof useExternalCalendars>
