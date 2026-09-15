import type { ScheduleEventType } from '../types/schedule'

/**
 * 面试/笔试通知的粘贴解析——纯本地规则，零网络。
 * 目标不是读懂天下所有文案，而是把最常见的那几句话
 * （【公司】… 时间：9月18日（周五）下午2:30 …）变成一张 90% 正确的草稿，
 * 剩下的让用户在日程表单里一键确认。解析不出就如实返回 null，绝不编。
 */
export interface ParsedInvite {
  /** 公司名：优先匹配已知目标，其次取【】「」里的词。 */
  company: string | null
  eventType: ScheduleEventType
  /** ISO 时间串；日期或时间认不出时为 null。 */
  startTime: string | null
  dateKnown: boolean
  timeKnown: boolean
  /** 「一面」「二面」「HR面」这类轮次词，拼标题用。 */
  roundLabel: string | null
}

const CN_NUM: Record<string, number> = { 零: 0, 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9, 十: 10 }

function cnToNumber(token: string): number {
  if (/^\d+$/.test(token)) return Number(token)
  if (CN_NUM[token] != null) return CN_NUM[token]
  // 十一~十九
  const m = token.match(/^十([一二三四五六七八九])$/)
  if (m) return 10 + (CN_NUM[m[1]] ?? 0)
  return NaN
}

const TYPE_RULES: Array<{ type: ScheduleEventType; re: RegExp }> = [
  { type: 'exam', re: /笔试|测评|考试|oj\s*test|online\s*(test|assessment)|aptitude/i },
  { type: 'interview', re: /面试|interview/i },
  { type: 'followup', re: /沟通|电话|跟进|回访|offer\s*(沟通|电话)/i },
]

function detectType(text: string): ScheduleEventType {
  for (const rule of TYPE_RULES) {
    if (rule.re.test(text)) return rule.type
  }
  return 'interview'
}

function detectRound(text: string): string | null {
  const hr = text.match(/HR\s*面/i)
  if (hr) return 'HR面'
  const m = text.match(/([一二三四五六123456])\s*面/)
  if (m) {
    const n = cnToNumber(m[1])
    if (Number.isFinite(n)) return `第${n}面`
  }
  if (/终面/.test(text)) return '终面'
  if (/群面/.test(text)) return '群面'
  return null
}

function detectCompany(text: string, knownCompanies: string[]): string | null {
  // 已有目标名（含其第一个分词，如「腾讯 · 后端开发」的「腾讯」）出现在文案里就是最强信号。
  for (const name of knownCompanies) {
    const head = name.split(/[·\s|/]/)[0]?.trim()
    for (const candidate of [name, head]) {
      if (candidate && candidate.length >= 2 && text.includes(candidate)) return candidate
    }
  }
  const bracket = text.match(/[【\[「]([^】\]」]{2,20})[】\]」]/)
  if (!bracket) return null
  // 括号里常是「字节的确认函」这类短语：去掉「的…」尾巴得到主体，再和已知目标对上。
  const base = bracket[1].split('的')[0].trim()
  for (const name of knownCompanies) {
    const head = name.split(/[·\s|/]/)[0]?.trim()
    if (head && head.length >= 2 && (head.startsWith(base) || base.startsWith(head))) return name
  }
  return base || bracket[1].trim()
}

/**
 * 日期解析：返回当天零点（本地时区）的 Date；认不出返回 null。
 * rollable = 只写了「月日」没写年份——组出来的时刻若已过去，允许滚到明年。
 * 相对日（明天/下周X）和显式年份都不可滚：前者语义就是那一天，后者用户写死了。
 */
function parseDate(text: string, now: Date): { date: Date; rollable: boolean } | null {
  const y = now.getFullYear()
  // 明确年份：2026年9月18日 / 2026-09-18 / 2026.9.18
  let m = text.match(/(\d{4})\s*[年./-]\s*(\d{1,2})\s*[月./-]\s*(\d{1,2})\s*[日号]?/)
  if (m) return { date: new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])), rollable: false }
  // 月日：9月18日 / 9-18 / 9/18 / 09.18（小心别吃掉时间，排除紧跟 时分 的情况）
  m = text.match(/(?<!\d)(\d{1,2})\s*[月./-]\s*(\d{1,2})\s*[日号](?!\d)/)
  if (m) {
    const d = new Date(y, Number(m[1]) - 1, Number(m[2]))
    return { date: d, rollable: true }
  }
  // 相对日：今天/明天/后天/大后天
  const rel: Array<[RegExp, number]> = [[/大后天/, 3], [/后天/, 2], [/明天/, 1], [/今天|当日/, 0]]
  for (const [re, add] of rel) {
    if (re.test(text)) return { date: new Date(y, now.getMonth(), now.getDate() + add), rollable: false }
  }
  // 下周X 优先于 周X（都出现时下周赢）
  m = text.match(/下周([一二三四五六日天])/)
  if (m) return { date: nextWeekday(now, m[1], 'nextWeek'), rollable: false }
  m = text.match(/(?<![下本])周([一二三四五六日天])/)
  if (m) return { date: nextWeekday(now, m[1], 'plain'), rollable: false }
  return null
}

function nextWeekday(now: Date, cn: string, mode: 'plain' | 'nextWeek'): Date {
  const map: Record<string, number> = { 日: 0, 天: 0, 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6 }
  const target = map[cn] ?? 1
  const base = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const day = base.getDay()
  let add: number
  if (mode === 'nextWeek') {
    // 下周X：无论今天周几，都落到下一周的周 X（周二说「下周一」= 还有 6 天）
    add = 7 - day + target
  } else {
    // 周X 单独出现：指最近的下一个周 X，不含今天
    add = (target - day + 7) % 7
    if (add === 0) add = 7
  }
  base.setDate(base.getDate() + add)
  return base
}

/** 时间解析：返回当天内的 时:分；认不出返回 null。支持中文数字小时（三点/十点半）。 */
function parseTime(text: string): { h: number; min: number } | null {
  const m = text.match(/(上午|早上|中午|下午|傍晚|晚上)?\s*(\d{1,2}|[零一二两三四五六七八九十]{1,3})\s*[点:：]\s*(?:(半)|(\d{1,2})\s*分?|整)?/)
  if (!m) return null
  let h = Number(m[2])
  if (!Number.isFinite(h) || Number.isNaN(h)) {
    h = cnToNumber(m[2])
  }
  const meridiem = m[1]
  const min = m[3] ? 30 : m[4] ? Number(m[4]) : 0
  if (!Number.isFinite(h) || min > 59 || h > 24) return null
  if (meridiem) {
    if ((meridiem === '下午' || meridiem === '傍晚' || meridiem === '晚上') && h < 12) h += 12
    if (meridiem === '中午' && h < 12) h = 12
  } else if (h >= 1 && h <= 7) {
    // 没写上下午的小时数按面试场景惯例当作下午（2:30 → 14:30）；0 点保持 0。
    h += 12
  }
  if (h > 23) return null
  return { h, min }
}

export function parseInviteText(text: string, now: Date, knownCompanies: string[] = []): ParsedInvite {
  const clean = text.replace(/\s+/g, ' ').trim()
  const company = detectCompany(clean, knownCompanies)
  const eventType = detectType(clean)
  const roundLabel = detectRound(clean)

  const parsedDate = parseDate(clean, now)
  // 「两点半」「两点」是邀请文案里的高频写法，先归一成数字再解析。
  const time = parseTime(clean.replace(/两点/g, '2点'))
  const dateKnown = !!parsedDate
  const timeKnown = !!time
  let startTime: string | null = null
  if (parsedDate) {
    const { h, min } = time ?? { h: 10, min: 0 }
    const d = new Date(parsedDate.date.getFullYear(), parsedDate.date.getMonth(), parsedDate.date.getDate(), h, min)
    // 只写了月日、组出来的时刻又已过超过 1 小时 → 按明年理解，别让用户存进一条过去的日程
    if (parsedDate.rollable && d.getTime() < now.getTime() - 3600_000) {
      d.setFullYear(d.getFullYear() + 1)
    }
    startTime = d.toISOString()
  }
  return { company, eventType, startTime, dateKnown, timeKnown, roundLabel }
}
