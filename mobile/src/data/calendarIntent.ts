import { Capacitor, registerPlugin } from '@capacitor/core'

/**
 * 拉起系统日历「新建事件」预填页的入参。
 *
 * 时间一律传 UTC 毫秒：原生侧只搬数字，不解析 ISO 字符串，避免日历 App 与 JS 各换算一次导致时区二次偏移。
 */
export interface CreateEventOptions {
  title: string
  description?: string | null
  location?: string | null
  beginTime: number
  endTime: number
  allDay?: boolean
}

interface CalendarIntentPlugin {
  openCreateEvent(options: CreateEventOptions): Promise<void>
}

const PLUGIN_NAME = 'CalendarIntent'

/** 原生插件（MainActivity 手动注册）在不在，缺失时调用方要能安静降级到 .ics。 */
export function isCalendarIntentAvailable(): boolean {
  try {
    return Capacitor.isNativePlatform() && Capacitor.isPluginAvailable(PLUGIN_NAME)
  } catch {
    return false
  }
}

let handle: CalendarIntentPlugin | null = null

/**
 * 延迟到首次调用才 registerPlugin：模块求值时原生 bridge 可能还没注入 PluginHeaders，
 * 那样拿到的会是一个「未实现」的代理，ACTION_INSERT 在这台机器上永远走不通。
 */
function getPlugin(): CalendarIntentPlugin {
  if (!handle) handle = registerPlugin<CalendarIntentPlugin>(PLUGIN_NAME)
  return handle
}

/** 打开系统日历的新建事件页；失败一律以 reject 抛出，由调用方决定降级。 */
export async function openCreateEvent(options: CreateEventOptions): Promise<void> {
  await getPlugin().openCreateEvent(options)
}
