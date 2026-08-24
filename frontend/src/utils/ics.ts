export interface ParsedIcsEvent {
  uid: string | null
  title: string
  startTime: string
  endTime: string | null
  allDay: boolean
}

export interface IcsParseResult {
  events: ParsedIcsEvent[]
  /** 含重复规则（RRULE）暂不支持展开，按条跳过并如实告知 */
  skippedRecurring: number
  skippedCancelled: number
}

interface IcsProperty {
  name: string
  params: Record<string, string>
  value: string
}

function unfoldLines(raw: string): string[] {
  const normalized = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const lines = normalized.split('\n')
  const unfolded: string[] = []
  for (const line of lines) {
    if ((line.startsWith(' ') || line.startsWith('\t')) && unfolded.length > 0) {
      unfolded[unfolded.length - 1] += line.slice(1)
    } else {
      unfolded.push(line)
    }
  }
  return unfolded
}

function parseProperty(line: string): IcsProperty | null {
  const colon = line.indexOf(':')
  if (colon <= 0) return null
  const head = line.slice(0, colon)
  const value = line.slice(colon + 1)
  const semi = head.indexOf(';')
  const name = (semi === -1 ? head : head.slice(0, semi)).toUpperCase()
  if (!name) return null
  const params: Record<string, string> = {}
  if (semi !== -1) {
    for (const part of head.slice(semi + 1).split(';')) {
      const eq = part.indexOf('=')
      if (eq > 0) params[part.slice(0, eq).toUpperCase()] = part.slice(eq + 1).replace(/^"|"$/g, '')
    }
  }
  return { name, params, value }
}

/** 解码 TEXT 转义：\\n \\, \\; \\\\ */
function unescapeText(value: string): string {
  let out = ''
  for (let i = 0; i < value.length; i += 1) {
    const char = value[i]
    if (char === '\\' && i + 1 < value.length) {
      const next = value[i + 1]
      if (next === 'n' || next === 'N') { out += '\n'; i += 1; continue }
      if (next === ',' || next === ';' || next === '\\') { out += next; i += 1; continue }
    }
    out += char
  }
  return out.trim()
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function toLocalIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function toAllDayIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T00:00:00`
}

/**
 * 解析 DATE / DATE-TIME 值。
 * - VALUE=DATE → 全天（本地零点）
 * - 尾缀 Z → UTC，转换为本地时间
 * - TZID → 值本身是该时区的墙上时间；求出对应时刻后转为本地展示
 * - 无时区浮点值 → 按本地时间
 */
function parseIcsDateTime(value: string, params: Record<string, string>): { iso: string; allDay: boolean } {
  const match = /^(\d{4})(\d{2})(\d{2})(?:T(\d{2})(\d{2})(\d{2}))?(Z?)$/.exec(value.trim())
  if (!match) return { iso: '', allDay: false }
  const [, year, month, day, hour, minute, second] = match
  if (hour === undefined) {
    return { iso: toAllDayIso(new Date(Number(year), Number(month) - 1, Number(day))), allDay: true }
  }
  const asIfUtc = Date.UTC(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second))
  if (match[7] === 'Z') {
    return { iso: toLocalIso(new Date(asIfUtc)), allDay: false }
  }
  const tzid = params.TZID
  if (tzid && tzid.toUpperCase() !== 'UTC') {
    try {
      // 该时区在“假设为 UTC 的时刻”附近的偏移；用偏移还原真实时刻后转本地展示。
      // 极端情况（恰逢夏令时切换的歧义时间）按标准偏移处理。
      const offsetMs = timeZoneOffsetMs(tzid, new Date(asIfUtc))
      return { iso: toLocalIso(new Date(asIfUtc - offsetMs)), allDay: false }
    } catch {
      // 未知时区 id：退回浮点本地解释
    }
  }
  return {
    iso: `${year}-${month}-${day}T${hour}:${minute}:${second}`,
    allDay: false,
  }
}

function timeZoneOffsetMs(timeZone: string, date: Date): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date)
  const get = (type: string) => Number(parts.find((part) => part.type === type)?.value ?? '0')
  return Date.UTC(get('year'), get('month') - 1, get('day'), get('hour'), get('minute'), get('second')) - date.getTime()
}

/** 解析 .ics 文本为只读事件列表。仅支持单次事件；RRULE/取消的事件分别跳过计数。 */
export function parseIcs(raw: string): IcsParseResult {
  const result: IcsParseResult = { events: [], skippedRecurring: 0, skippedCancelled: 0 }
  let insideEvent = false
  let insideIgnoredBlock = false
  let current: Record<string, IcsProperty> = {}

  const flush = () => {
    if (!insideEvent || !current.DTSTART) { current = {}; return }
    if (current.RRULE) { result.skippedRecurring += 1; current = {}; return }
    if (current.STATUS && current.STATUS.value.toUpperCase() === 'CANCELLED') { result.skippedCancelled += 1; current = {}; return }

    const start = parseIcsDateTime(current.DTSTART.value, current.DTSTART.params)
    if (!start.iso) { current = {}; return }
    const end = current.DTEND ? parseIcsDateTime(current.DTEND.value, current.DTEND.params) : null

    // 全天多日事件：结束值为 DATE 时表示“最后一天的次日”，展示上收敛为起始当天全天
    result.events.push({
      uid: current.UID ? unescapeText(current.UID.value) : null,
      title: current.SUMMARY ? unescapeText(current.SUMMARY.value) || '未命名日程' : '未命名日程',
      startTime: start.iso,
      endTime: end?.iso || null,
      allDay: start.allDay,
    })
    current = {}
  }

  for (const line of unfoldLines(raw)) {
    const prop = parseProperty(line)
    if (!prop) continue
    if (prop.name === 'BEGIN') {
      const block = prop.value.toUpperCase()
      if (block === 'VEVENT') { insideEvent = true; current = {} }
      else if (insideEvent) { insideIgnoredBlock = true }
      continue
    }
    if (prop.name === 'END') {
      const block = prop.value.toUpperCase()
      if (block === 'VEVENT') { flush(); insideEvent = false }
      else if (insideEvent) { insideIgnoredBlock = false }
      continue
    }
    if (!insideEvent || insideIgnoredBlock) continue
    current[prop.name] = prop
  }
  return result
}
