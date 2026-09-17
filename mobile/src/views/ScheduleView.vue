<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import CompanyMark from '../components/CompanyMark.vue'
import PickerField from '../components/PickerField.vue'
import Sheet from '../components/Sheet.vue'
import EmptyState from '../components/EmptyState.vue'
import { headEllipsis } from '../data/resumeFile'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { ensureReviewHtml, sanitizeReviewHtml, reviewPlainText, compressImageToDataUrl, escapeHtml } from '../data/noteHtml'
import { renderReviewCard } from '../data/reviewCard'
import { shareErrorMessage, shareFileEx, shareTargetName } from '../data/share'
import { NOTE_COLORS, NOTE_PAPERS, normalizeNotePaper } from '../constants/noteColors'
import { parseInviteText } from '../data/parseInvite'
import {
  createSchedule, deleteSchedule, getReminder, listInterviewLogs, listReviews, listReviewTags, listSchedules, listStageEvents, listTargets,
  recordScheduleResult, setReminder, setReviewTags, setScheduleReview, updateSchedule,
} from '../data/store'
import { stagePaceLine, stagePaceSummary } from '../data/stagePace'
import { requestNotificationPermission, scheduleReminder, cancelReminder, pendingNotificationIds, reviewNudgeEnabled, scheduleReviewNudge, cancelReviewNudge, REMINDER_OUTCOME_MESSAGES } from '../data/notifications'
import type { ScheduleEvent, ScheduleEventType } from '../types/schedule'
import { SCHEDULE_EVENT_TYPE_LABELS, SCHEDULE_EVENT_TYPE_COLORS, eventStatus, isEventFinished, scheduleTimeError } from '../types/schedule'
import { isTerminalStage, TARGET_STAGE_LABELS, normalizeTargetStage } from '../types/project'
import { eventsOnDate, timelineDates } from '../data/timeline'
import { addToDeviceCalendar } from '../data/calendar'
import type { CalendarHandoff } from '../data/calendar'

const TYPES: ScheduleEventType[] = ['interview', 'exam', 'followup', 'other']
const WEEK = ['日', '一', '二', '三', '四', '五', '六']
const REMIND_OPTIONS = [
  { value: 0, label: '不提醒' },
  { value: 10, label: '提前 10 分钟' },
  { value: 30, label: '提前 30 分钟' },
  { value: 60, label: '提前 1 小时' },
  { value: 1440, label: '提前 1 天' },
]
const typeOptions = TYPES.map((t) => ({ value: t, label: SCHEDULE_EVENT_TYPE_LABELS[t] }))
const remindOptions = REMIND_OPTIONS.map((o) => ({ value: o.value, label: o.label }))
const targetOptions = computed(() => listTargets().map((t) => ({
  value: t.id, label: t.name, hint: TARGET_STAGE_LABELS[normalizeTargetStage(t.stage)],
})))

const viewMode = ref<'agenda' | 'month' | 'review'>('agenda')
const router = useRouter()

// ── 全局搜索：目标 / 日程 / 心得一框搜完，找东西不用回忆它在哪个 tab ──
const searchOpen = ref(false)
const searchKw = ref('')
const searchResults = computed(() => {
  const kw = searchKw.value.trim().toLowerCase()
  if (!kw) return { targets: [], schedules: [], reviews: [] }
  const targets = listTargets()
    .filter((t) => `${t.name} ${t.targetRole ?? ''} ${t.location ?? ''} ${t.notes ?? ''}`.toLowerCase().includes(kw))
    .slice(0, 5)
  const schedules = listSchedules()
    .filter((e) => `${e.title} ${e.notes ?? ''}`.toLowerCase().includes(kw))
    .slice(0, 6)
  const reviews = listReviews()
    .filter((e) => `${planName(e)} ${noteSnippet(e)}`.toLowerCase().includes(kw))
    .slice(0, 6)
  return { targets, schedules, reviews }
})
const searchEmpty = computed(() => searchKw.value.trim() &&
  !searchResults.value.targets.length && !searchResults.value.schedules.length && !searchResults.value.reviews.length)
function openSearch() { searchKw.value = ''; searchOpen.value = true }
function searchGoTargets() { searchOpen.value = false; router.push('/targets') }

// ── 提醒排入状态：把「ROM 有没有偷偷吃掉通知」摆到用户眼前，而不是藏在设置页 ──
const armedIds = ref<Set<number> | null>(null)
void refreshArmed()
async function refreshArmed() {
  armedIds.value = await pendingNotificationIds()
}
/** '' = 不显示（没设提醒 / 无法核实）；'ok' 已排入；'miss' 未排入——ROM 吃通知的证据；
 *  'fired' 提醒时刻已过——通知触发后会被系统移出待发队列，不能拿「不在队列」当「没排入」。 */
const ARM_LABELS = { ok: '提醒已排入系统', miss: '提醒未排入', fired: '提醒已触发' } as const
function armStateOf(ev: ScheduleEvent): '' | 'ok' | 'miss' | 'fired' {
  const rem = getReminder(ev.id)
  if (rem <= 0) return ''
  // 提醒响过之后待发队列里必然没有这条：此刻改报「已触发」，不再制造「未排入」的假警报。
  const fireAt = new Date(ev.startTime).getTime() - rem * 60_000
  if (Number.isFinite(fireAt) && now.value >= fireAt) return 'fired'
  if (!armedIds.value) return ''
  if (eventStatus(ev, now.value) !== 'upcoming') return ''
  return armedIds.value.has(ev.id) ? 'ok' : 'miss'
}
/** 状态文案（'' = 该行不显示提醒状态）。 */
function armLabelOf(ev: ScheduleEvent): string {
  const s = armStateOf(ev)
  return s ? ARM_LABELS[s] : ''
}

// ── 粘贴式快速录入：把通知原文整段粘进来，核心动作的成本砍掉一半 ──
const pasteOpen = ref(false)
const pasteText = ref('')
function openPaste() { pasteText.value = ''; pasteOpen.value = true }
function applyPaste() {
  const text = pasteText.value.trim()
  if (!text) { toast('先粘贴通知原文'); return }
  const r = parseInviteText(text, new Date(), listTargets().map((t) => t.name))
  const target = r.company ? listTargets().find((t) => t.name === r.company) : null
  const titleBits = [r.company ?? '', r.roundLabel ?? SCHEDULE_EVENT_TYPE_LABELS[r.eventType]].filter(Boolean)
  form.value = {
    title: titleBits.join(' ') || '新日程',
    eventType: r.eventType,
    // 时间认不出就先给明天同一时刻：用户必经表单核对，不会把错误时间存进去。
    start: r.startTime ? toLocalInput(r.startTime) : toLocalInput(new Date(Date.now() + 24 * 3600_000).toISOString()),
    end: '', notes: text, reminder: 30,
    jobProjectId: target?.id ?? null,
  }
  pasteOpen.value = false
  editing.value = null
  sheetOpen.value = true
  if (!r.dateKnown) toast('日期没认出来，先按「明天」预填，请核对')
  else if (!r.timeKnown) toast('具体时间没认出来，暂按上午 10 点，请核对')
  else if (!r.company) toast('已识别时间和类型，公司名请确认')
}

// ── 战前速览：面试前 24 小时，把这家公司的历史复盘推到眼前——复盘的价值在下一场之前 ──
const PRE_BATTLE_WINDOW = 24 * 3600_000
const preBattle = computed(() => {
  const ev = nextEvent.value
  if (!ev) return null
  if (ev.eventType !== 'exam' && ev.eventType !== 'interview') return null
  const diff = new Date(ev.startTime).getTime() - now.value
  if (diff > PRE_BATTLE_WINDOW || diff < -60 * 60_000) return null
  if (ev.jobProjectId == null) return null
  const past = listSchedules()
    .filter((x) => x.jobProjectId === ev.jobProjectId && x.id !== ev.id
      && isEventFinished(x, now.value) && !!x.review?.trim())
    .sort((a, b) => b.startTime.localeCompare(a.startTime))
  const last = past[0] ?? null
  const tagCount = new Map<string, number>()
  for (const p of past) for (const t of p.reviewTags ?? []) tagCount.set(t, (tagCount.get(t) ?? 0) + 1)
  const tags = [...tagCount.entries()].sort((a, b) => b[1] - a[1]).slice(0, 4).map(([t]) => t)
  return { ev, last, tags }
})
/** 战前速览的面经弹药：这家公司被公开讨论过的真题，最多取 4 条。 */
const pbIvQuestions = computed(() => {
  const pb = preBattle.value
  if (!pb) return []
  const seen = new Set<string>()
  for (const log of listInterviewLogs()) {
    if (log.targetId !== pb.ev.jobProjectId) continue
    for (const q of log.questions ?? []) seen.add(q)
  }
  return [...seen].slice(0, 4)
})
const now0 = new Date()
const monthCursor = ref({ y: now0.getFullYear(), m: now0.getMonth() })
const selectedDay = ref(new Date().toDateString())

// 实时倒计时：每 30s 刷新一次“下一场”卡片
const now = ref(Date.now())
const timer = setInterval(() => { now.value = Date.now() }, 30_000)
onBeforeUnmount(() => clearInterval(timer))

const nowDate = computed(() => new Date(now.value))
const nowClock = computed(() => hhmm(nowDate.value.toISOString()))
const todayLabel = computed(() => {
  const d = nowDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 · 周${'日一二三四五六'[d.getDay()]}`
})
const timeline = computed(() => timelineDates(nowDate.value, 31))

const editing = ref<ScheduleEvent | null>(null)
const sheetOpen = ref(false)
const calendarSyncing = ref(false)
const form = ref({
  title: '', eventType: 'interview' as ScheduleEventType,
  start: '', end: '', notes: '', reminder: 30, jobProjectId: null as number | null,
})
/** notes 只放赛前要看的信息；心得另开一个窗口，避免把「会议链接」和「这轮没答好」写进同一个框里。 */
/** 心得正文上限按纯文本计（HTML 标签不占额度）；富文本一篇 5000 字足够写完一场复盘。 */
const NOTE_MAX_TEXT = 5000
const REVIEW_PROMPTS = ['这轮被问了什么', '哪里没答好', '下一轮要补什么']
/** 心得标签：常用的先摆在前面当推荐，其余收进「更多」，不占地方；用户也能自己打字加任意标签。 */
const REVIEW_TAG_SUGGESTIONS = [
  '自我介绍', '项目深挖', '算法', '系统设计', '手撕代码', '八股', '行为题', '英语',
  'HR面', '主管面', '群面', '拿offer', '挂了', '答得不错', '待补', '薪资',
  '反问环节', '反问质量高', '进度催一下', '感谢信', '二面预告', '面试官好', '氛围好', '白板题',
  'case interview', '数理逻辑', '编程环境坑', '迟到体验差', '流程清晰', '已感谢拒绝',
]
const TAG_PREVIEW_COUNT = 8
const TAG_SELECTED_PREVIEW = 6
const tagsExpanded = ref(false)
const selectedExpanded = ref(false)
const visibleTagSuggestions = computed(() =>
  tagsExpanded.value ? REVIEW_TAG_SUGGESTIONS : REVIEW_TAG_SUGGESTIONS.slice(0, TAG_PREVIEW_COUNT),
)
const hiddenTagCount = REVIEW_TAG_SUGGESTIONS.length - TAG_PREVIEW_COUNT
const MAX_TAGS = 12

// ── 全屏心得笔记本：阅读与编辑是同一张纸，点开就能直接往下写 ──
const noteTarget = ref<ScheduleEvent | null>(null)
const draftTags = ref<string[]>([])
const draftColor = ref<string | null>(null)
const draftPaper = ref('plain')
const tagInput = ref('')
const noteEditorEl = ref<HTMLDivElement | null>(null)
const imageInputEl = ref<HTMLInputElement | null>(null)
const noteTextLen = ref(0)
const noteOverLimit = ref(false)
/** 点工具栏前记住光标（点输入框/选图会抢焦点），回来接着选区插入。 */
const savedRange = ref<Range | null>(null)
const savedText = ref('')
/** 链接输入条：null = 收起；空串 = 展开待输入。 */
const linkDraft = ref<string | null>(null)
/** 抽屉（标签 + 提示语 + 清空/分享图）：无痕化交互。0~1 的展开比例由细线抓握驱动——
 *  拖动时比例实时跟手（关过渡），松手按阈值吸附全开/全关；轻点细线等于切换。 */
const drawerRatio = ref(0)
const drawerMax = ref(0)
const drawerDragging = ref(false)
const drawerInnerEl = ref<HTMLElement | null>(null)
const drawerHeight = computed(() => `${(drawerRatio.value * drawerMax.value).toFixed(1)}px`)
function visibleViewportHeight(): number {
  // 键盘弹出时 visualViewport 会缩小——抽屉的上限跟着它走，而不是跟「布局视口」走
  return typeof visualViewport !== 'undefined' ? visualViewport!.height : window.innerHeight
}
function measureDrawer() {
  const el = drawerInnerEl.value
  if (!el) return
  // 内容特别多时给个上限，超出部分在抽屉内滚动，别让抽屉吃掉整张纸
  drawerMax.value = Math.min(el.scrollHeight, Math.round(visibleViewportHeight() * 0.45))
}
/** 键盘弹出/收起：视口一变就重测，抽屉永远只长在可见区域里。 */
function onViewportResize() { if (drawerRatio.value > 0) measureDrawer() }
if (typeof visualViewport !== 'undefined') {
  visualViewport!.addEventListener('resize', onViewportResize)
  onBeforeUnmount(() => visualViewport!.removeEventListener('resize', onViewportResize))
}
function setDrawer(open: boolean) {
  measureDrawer()
  drawerRatio.value = open ? 1 : 0
}
let gripStartY = 0
let gripStartRatio = 0
let gripMoved = false
function onGripDown(e: PointerEvent) {
  measureDrawer()
  gripStartY = e.clientY
  gripStartRatio = drawerRatio.value
  gripMoved = false
  drawerDragging.value = true
  try { (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId) } catch { /* 合成事件/已释放时照常工作 */ }
}
function onGripMove(e: PointerEvent) {
  if (!drawerDragging.value) return
  const dy = gripStartY - e.clientY
  if (Math.abs(dy) > 4) gripMoved = true
  drawerRatio.value = Math.min(1, Math.max(0, gripStartRatio + dy / Math.max(drawerMax.value, 1)))
}
function onGripUp(e: PointerEvent) {
  if (!drawerDragging.value) return
  drawerDragging.value = false
  try { (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId) } catch { /* 已释放 */ }
  if (!gripMoved) { setDrawer(drawerRatio.value <= 0.5); return }
  drawerRatio.value = drawerRatio.value > 0.3 ? 1 : 0
}

/** 纸张风格 → 纸面底色：白/米/牛皮是三种「本子」，与外面的便签色互不相干。 */
const PAPER_BG: Record<string, string> = {
  plain: 'var(--paper-white)',
  cream: 'var(--paper-cream)',
  kraft: 'var(--paper-kraft)',
}
const paperColor = computed(() => PAPER_BG[draftPaper.value] ?? PAPER_BG.plain)

/** 已选标签折叠：超过 6 个才出现「展开」，少的时候保持一排摊开、不给收纳钮。 */
const visibleDraftTags = computed(() =>
  selectedExpanded.value ? draftTags.value : draftTags.value.slice(0, TAG_SELECTED_PREVIEW),
)
const hiddenSelectedCount = computed(() => Math.max(0, draftTags.value.length - TAG_SELECTED_PREVIEW))
/** 标签「展开/收起」会改变内容高度：不重测就会发生「点了展开却被抽屉裁住」的假卡死。 */
watch([tagsExpanded, selectedExpanded, () => draftTags.value.length, () => draftPaper.value], () => nextTick(measureDrawer))

/** 心得墙卡片底色：没选便签色时跟随日程类型色（编辑器纸面由 reviewPaper 管，与此无关）。 */
function noteColorOf(ev: ScheduleEvent): string {
  return ev.reviewColor ?? SCHEDULE_EVENT_TYPE_COLORS[ev.eventType]
}
function noteSnippet(ev: ScheduleEvent): string {
  return reviewPlainText(ev.review ?? '').trim()
}
function noteLen(ev: ScheduleEvent): number {
  return reviewPlainText(ev.review ?? '').trim().length
}
const NOTE_TAG_PREVIEW = 3
function visibleNoteTags(ev: ScheduleEvent): string[] {
  return (ev.reviewTags ?? []).slice(0, NOTE_TAG_PREVIEW)
}
function hiddenNoteTags(ev: ScheduleEvent): number {
  return Math.max(0, (ev.reviewTags ?? []).length - NOTE_TAG_PREVIEW)
}

function toLocalInput(isoStr: string | null): string {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function fromLocalInput(v: string): string | null {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d.toISOString()
}

const grouped = computed(() => {
  const map = new Map<string, ScheduleEvent[]>()
  for (const ev of listSchedules()) {
    const key = new Date(ev.startTime).toDateString()
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(ev)
  }
  return [...map.entries()]
})
const planEvents = computed(() => listSchedules())
const nextEvent = computed(() => planEvents.value.find((event) => new Date(event.startTime).getTime() >= now.value - 30 * 60_000) ?? null)
const eventsByDay = computed(() => new Map(grouped.value))

// ── 复盘（心得）──
/** 只有预计结束时刻真的过了才算「已结束」，未来的日程不应该出现写心得的入口。 */
const finishedEvents = computed(() => planEvents.value.filter((ev) => isEventFinished(ev, now.value)))
const reviewedEvents = computed(() => listReviews())
const reviewedFinished = computed(() => finishedEvents.value.filter((ev) => !!ev.review?.trim()))
const pendingReviews = computed(() => finishedEvents.value.filter((ev) => !ev.review?.trim()).reverse())
const reviewCoverage = computed(() => (
  finishedEvents.value.length ? Math.round((reviewedFinished.value.length / finishedEvents.value.length) * 100) : 0
))
/** 头卡的第三行走「时间跨度」这个环形图和百分比都给不出的维度，顺带交代还差几篇。 */
const reviewSpanLabel = computed(() => {
  const stamps = finishedEvents.value.map((ev) => new Date(ev.startTime).getTime()).sort((a, b) => a - b)
  if (!stamps.length) return ''
  const day = (ms: number) => { const d = new Date(ms); return `${d.getMonth() + 1}月${d.getDate()}日` }
  const first = stamps[0]
  const last = stamps[stamps.length - 1]
  const span = new Date(first).toDateString() === new Date(last).toDateString() ? day(first) : `${day(first)} – ${day(last)}`
  const pending = pendingReviews.value.length
  return pending ? `${span} · 还差 ${pending} 篇` : `${span} · 全都记下了`
})

const reviewFilter = ref<'all' | ScheduleEventType>('all')
const reviewTagFilter = ref<string | null>(null)
const reviewKeyword = ref('')
/** 复盘墙里用过的所有标签，按热度排——直接喂给标签筛选条。 */
const allTags = computed(() => listReviewTags())
/** 便签墙是扁平的：每张卡片自带公司图标和计划名，再按公司分组只会把同一个名字重复两遍。 */
const reviewNotes = computed<ScheduleEvent[]>(() => {
  const kw = reviewKeyword.value.trim().toLowerCase()
  return reviewedEvents.value.filter((ev) => {
    if (reviewFilter.value !== 'all' && ev.eventType !== reviewFilter.value) return false
    if (reviewTagFilter.value && !(ev.reviewTags ?? []).includes(reviewTagFilter.value)) return false
    return !kw || `${planName(ev)} ${ev.title} ${noteSnippet(ev)}`.toLowerCase().includes(kw)
  })
})
/**
 * 复盘入口跟着「开始」走而不是「结束」：面试一开始就该能随手记要点，
 * 结束后自然变成写心得——用户反馈「时间到了没有任何提示」的根源就是以前卡在结束时刻。
 */
function reviewable(ev: ScheduleEvent): boolean {
  return !!ev.review?.trim() || eventStatus(ev, now.value) !== 'upcoming'
}
/** 行内状态徽标：进行中的日程要有活的提示，而不是默默等着结束。 */
function statusOf(ev: ScheduleEvent): 'ongoing' | null {
  if (ev.review?.trim()) return null
  return eventStatus(ev, now.value) === 'ongoing' ? 'ongoing' : null
}
/** 心得按钮的文案：已写→看心得；进行中→记心得；结束→写心得。 */
function reviewLabel(ev: ScheduleEvent): string {
  if (ev.review?.trim()) return '看心得'
  return eventStatus(ev, now.value) === 'ongoing' ? '记心得' : '写心得'
}
/** 点心得按钮：无论写过没写，都进同一张全屏笔记本——阅读与编辑不再分家。 */
function onReviewClick(ev: ScheduleEvent) {
  openNote(ev)
}
const reviewTypeCounts = computed(() => TYPES.map((type) => ({
  key: type,
  label: SCHEDULE_EVENT_TYPE_LABELS[type],
  count: reviewedEvents.value.filter((ev) => ev.eventType === type).length,
})).filter((item) => item.count > 0))

// ── 标签聚合：复盘从「记录」升维成「能看趋势」——挂点都记在哪些标签上，一眼见底 ──
const reviewTagStats = computed(() => {
  const counts = new Map<string, number>()
  for (const ev of reviewedEvents.value) {
    for (const t of ev.reviewTags ?? []) counts.set(t, (counts.get(t) ?? 0) + 1)
  }
  const top = [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, 8)
  const max = top[0]?.[1] ?? 1
  return top.map(([tag, count]) => ({ tag, count, pct: Math.round((count / max) * 100) }))
})

// ── 求职季报告：给用户一个「总结时刻」——投了多少、走到哪、卡在哪，数据全是现成的 ──
const reportOpen = ref(false)
const seasonReport = computed(() => {
  const targets = listTargets()
  const stageOf = (t: ReturnType<typeof listTargets>[number]) => normalizeTargetStage(t.stage)
  const offered = targets.filter((t) => stageOf(t) === 'offer').length
  const failed = targets.filter((t) => ['screened_out', 'rejected', 'closed'].includes(stageOf(t))).length
  const active = targets.filter((t) => t.status === 'active' && !isTerminalStage(stageOf(t))).length
  const interviews = finishedEvents.value.filter((e) => e.eventType === 'interview' || e.eventType === 'exam').length
  const topTag = reviewTagStats.value[0]
  const insight = topTag
    ? `心得里最常出现的是「${topTag.tag}」，下轮复习从它开始。`
    : '多写几篇心得并打上标签，就能看到自己的挂点分布。'
  // 节奏维度：数量之外还有时间——「卡在哪」没有天数就没有体感。
  const pace = stagePaceLine(stagePaceSummary(listStageEvents(), listTargets(), now.value))
  return { total: targets.length, active, offered, failed, interviews, coverage: reviewCoverage.value, insight, pace }
})

// ── 面经的「消费点」在战前速览（见 pbIvQuestions）；面经库本体在「资料」页（ResumesView） ──

/**
 * 便签按月份分组。reviewNotes 已是时间倒序，所以顺序扫一遍即可连续成组。
 * 一屏几十张便签时纯平铺会失去节奏，月份标题是唯一能让人「找回位置」的视觉锚点。
 */
const reviewGroups = computed(() => {
  const groups: Array<{ key: string; label: string; notes: ScheduleEvent[] }> = []
  for (const ev of reviewNotes.value) {
    const d = new Date(ev.startTime)
    if (Number.isNaN(d.getTime())) continue
    const key = `${d.getFullYear()}-${d.getMonth()}`
    const label = `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`
    const last = groups[groups.length - 1]
    if (last?.key === key) last.notes.push(ev)
    else groups.push({ key, label, notes: [ev] })
  }
  return groups
})
/**
 * 便签卡不再随机倾斜、不再贴胶带：心得是正经的记录工具，
 * 花哨的「随手贴」质感与内容的严肃性不匹配（用户原话：太随意，和实际情况差太多）。
 * 彩色感改由便签底色承担——每张卡一个可选的底色，整面墙依然是一面彩墙。
 */

// ── 全屏笔记本：像看日记一样读心得，光标点进去就能接着写 ──
/** 心得日期抬头：「9月14日 · 周一」这种日记式落款。 */
function nbDate(ev: ScheduleEvent): string {
  const d = new Date(ev.startTime)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}月${d.getDate()}日 · 周${'日一二三四五六'[d.getDay()]}`
}

function openNote(ev: ScheduleEvent) {
  noteTarget.value = ev
  draftTags.value = [...(ev.reviewTags ?? [])]
  draftColor.value = ev.reviewColor ?? null
  draftPaper.value = normalizeNotePaper(ev.reviewPaper)
  tagInput.value = ''
  tagsExpanded.value = false
  selectedExpanded.value = false
  savedRange.value = null
  savedText.value = ''
  linkDraft.value = null
  drawerRatio.value = 0
  noteOverLimit.value = false
  // 编辑器节点要等下一拍才挂上（Transition 分支渲染），先填内容再统计字数
  nextTick(() => {
    const el = noteEditorEl.value
    if (!el) return
    el.innerHTML = ensureReviewHtml(ev.review ?? '')
    countNoteText()
  })
}
function countNoteText() {
  const text = reviewPlainText(noteEditorEl.value?.innerHTML ?? '').trim()
  noteTextLen.value = text.length
  noteOverLimit.value = text.length > NOTE_MAX_TEXT
}
function onNoteInput() { countNoteText() }

function rememberRange() {
  const sel = window.getSelection()
  if (sel && sel.rangeCount && noteEditorEl.value?.contains(sel.anchorNode)) {
    const range = sel.getRangeAt(0)
    savedRange.value = range.cloneRange()
    savedText.value = range.toString()
  }
}
function restoreRange() {
  const range = savedRange.value
  const el = noteEditorEl.value
  if (!range || !el) return
  el.focus()
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(range)
}
function exec(cmd: string, value?: string) {
  document.execCommand(cmd, false, value)
  countNoteText()
}
function boldSelection() { exec('bold') }
function placeCaretEnd(el: HTMLElement) {
  el.focus()
  const sel = window.getSelection()
  const range = document.createRange()
  range.selectNodeContents(el)
  range.collapse(false)
  sel?.removeAllRanges()
  sel?.addRange(range)
}
/** 提示词片段：有光标插到光标处，没光标就补到文末，加粗成小抬头让用户接着往下写。 */
function appendPrompt(label: string) {
  const el = noteEditorEl.value
  if (!el) return
  const html = `<p><strong>${label}：</strong></p>`
  const sel = window.getSelection()
  if (sel && sel.rangeCount && el.contains(sel.anchorNode)) {
    exec('insertHTML', html)
  } else {
    el.innerHTML += html
    countNoteText()
    placeCaretEnd(el)
  }
}
function insertImage() {
  rememberRange()
  imageInputEl.value?.click()
}
async function onPickImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) { toast('只能插入图片文件'); return }
  if (file.size > 8 * 1024 * 1024) { toast('图片太大（超过 8MB），换一张小的'); return }
  try {
    const dataUrl = await compressImageToDataUrl(file)
    restoreRange()
    exec('insertImage', dataUrl)
  } catch {
    toast('这张图片读不出来，换一张试试')
  }
}
function insertLink() {
  rememberRange()
  linkDraft.value = ''
}
/** 确认插链接：不用 window.prompt（部分 WebView 会吞掉它，而且选区保不住），
 *  在输入条里填 URL，回到记住的选区用 insertHTML 原位生成 <a>——
 *  没选中文字时直接把链接本身当文字插进去。 */
function confirmLink() {
  const raw = (linkDraft.value ?? '').trim()
  linkDraft.value = null
  if (!raw) return
  const url = /^(https?:\/\/|mailto:)/i.test(raw) ? raw : `https://${raw}`
  const el = noteEditorEl.value
  if (!el) return
  const sel = window.getSelection()
  const hasLiveSelection = !!(savedRange.value && sel && sel.rangeCount && el.contains(sel.anchorNode))
  if (!hasLiveSelection && !savedRange.value) placeCaretEnd(el)
  else restoreRange()
  const text = savedText.value.trim() || url
  exec('insertHTML', `<a href="${escapeHtml(url)}">${escapeHtml(text)}</a>`)
}
function pickColor(color: string | null) { draftColor.value = color }
function pickPaper(key: string) { draftPaper.value = key }

/** 关笔记本 = 保存：没有「忘记点保存」这回事。 */
function closeNote() {
  if (!noteTarget.value) return
  const ev = noteTarget.value
  const html = sanitizeReviewHtml(noteEditorEl.value?.innerHTML ?? '')
  const text = reviewPlainText(html).trim()
  if (noteOverLimit.value) { toast(`心得超过 ${NOTE_MAX_TEXT} 字，先删一点再关`); return }
  const prev = ev.review ?? ''
  setScheduleReview(ev.id, text ? html : '', draftColor.value, draftPaper.value)
  setReviewTags(ev.id, draftTags.value)
  noteTarget.value = null
  if (text && reviewPlainText(prev).trim() !== text) toast('心得已保存')
  // 心得写完了，「该写复盘了」的提醒就没有存在的理由，立刻撤掉。
  if (text) cancelReviewNudge(ev.id)
  // 写完心得正是用户对「这场打得到底怎样」记忆最新鲜的时刻，顺手把目标状态也对齐。
  if (text) void maybeAskOutcome(ev)
}

// ── 复盘联动推进：过了推进阶段/轮次，挂了标记结果——别让看板停在「过去填的样子」 ──
const outcomeAsk = ref<ScheduleEvent | null>(null)
async function maybeAskOutcome(ev: ScheduleEvent) {
  if (ev.outcomePrompted) return
  if (eventStatus(ev, now.value) !== 'finished') return
  // 只问笔试和面试：跟进电话、其他事项谈不上「过/挂」，硬问反而烦人。
  if (ev.eventType !== 'exam' && ev.eventType !== 'interview') return
  const t = ev.jobProjectId == null ? null : listTargets().find((x) => x.id === ev.jobProjectId)
  if (!t || isTerminalStage(normalizeTargetStage(t.stage))) return
  // 等 toast 落定再弹，避免两个浮层打架
  await new Promise((r) => setTimeout(r, 650))
  if (!noteTarget.value && !outcomeAsk.value) outcomeAsk.value = ev
}
function askOutcome(passed: boolean) {
  const ev = outcomeAsk.value
  if (!ev) return
  // 无论选哪边都记「问过」：跳过的用户不想被反复追问，状态可去目标页手动调。
  updateSchedule(ev.id, { outcomePrompted: true })
  outcomeAsk.value = null
  const res = recordScheduleResult(ev.id, passed)
  if (!res.ok) { toast(res.message ?? '状态未更新'); return }
  if (!passed) { toast('已记录「未通过」，目标状态已锁定'); return }
  if (res.round) { toast(`本轮通过，进入第 ${res.round} 面`); return }
  if (res.stage) { toast(`已推进到「${TARGET_STAGE_LABELS[res.stage]}」`) }
}
function skipOutcome() {
  const ev = outcomeAsk.value
  if (ev) updateSchedule(ev.id, { outcomePrompted: true })
  outcomeAsk.value = null
}
/** 把当前纸面上的心得生成分享长图：发给导师/朋友求指点，纯本地渲染零网络。 */
const cardBusy = ref(false)
async function shareReviewCard() {
  if (!noteTarget.value || cardBusy.value) return
  const ev = noteTarget.value
  cardBusy.value = true
  try {
    // 分享的是纸面上此刻的内容：改了还没关笔记本也能把新版分享出去。
    const text = reviewPlainText(sanitizeReviewHtml(noteEditorEl.value?.innerHTML ?? '')).trim()
    if (!text) { toast('先写点内容再分享'); return }
    const blob = await renderReviewCard({
      company: companyName(ev),
      typeLabel: SCHEDULE_EVENT_TYPE_LABELS[ev.eventType],
      dateLabel: nbDate(ev),
      text,
      tags: ev.reviewTags ?? [],
    })
    if (!blob) { toast('生成图片失败'); return }
    const outcome = await shareFileEx({
      fileName: `zhida-review-${ev.id}.png`,
      blob,
      subject: '面试复盘',
      dialogTitle: '分享复盘',
    })
    if (outcome.status === 'cancelled') { toast('已取消分享'); return }
    if (outcome.status === 'downloaded') { toast('长图已下载到本机'); return }
    const label = shareTargetName(outcome.target)
    toast(label ? `长图已交给${label}` : '长图已交给所选应用')
  } catch (err) {
    toast(shareErrorMessage(err))
  } finally { cardBusy.value = false }
}

async function clearNote() {  const ev = noteTarget.value
  if (!ev) return
  const prevHtml = ev.review ?? ''
  const prevTags = ev.reviewTags ?? []
  const prevColor = ev.reviewColor ?? null
  const prevPaper = normalizeNotePaper(ev.reviewPaper)
  if (!reviewPlainText(prevHtml).trim()) {
    // 还没写过：清空就是关掉，不必弹确认
    noteTarget.value = null
    return
  }
  const ok = await confirmAction({
    title: '清空这篇心得？',
    message: '这场日程会保留，只删掉写下的复盘文字和标签。',
    confirmLabel: '清空',
    danger: true,
  })
  if (!ok) return
  setScheduleReview(ev.id, '', null)
  cancelReviewNudge(ev.id)
  setReviewTags(ev.id, [])
  const el = noteEditorEl.value
  if (el) el.innerHTML = ''
  countNoteText()
  draftTags.value = []
  noteTarget.value = null
  toast('心得已清空', { label: '撤销', run: () => { setScheduleReview(ev.id, prevHtml, prevColor, prevPaper); setReviewTags(ev.id, prevTags) } })
}

// ── 复盘统计环：各类型心得的占比，点击即筛选，默认收起不占地方 ──
const statsOpen = ref(false)
const RING_CIRCUMFERENCE = 2 * Math.PI * 20
const reviewTypeRings = computed(() => {
  const total = reviewedEvents.value.length || 1
  return TYPES.map((type) => {
    const count = reviewedEvents.value.filter((ev) => ev.eventType === type).length
    return { key: type, label: SCHEDULE_EVENT_TYPE_LABELS[type], count, pct: Math.round((count / total) * 100) }
  })
})
function toggleTypeFilter(type: ScheduleEventType) {
  reviewFilter.value = reviewFilter.value === type ? 'all' : type
}

interface CalCell { key: string; day: number; other: boolean }
const calCells = computed<CalCell[]>(() => {
  const { y, m } = monthCursor.value
  const startWeekday = new Date(y, m, 1).getDay()
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const prevDays = new Date(y, m, 0).getDate()
  const cells: CalCell[] = []
  for (let i = startWeekday - 1; i >= 0; i--) cells.push({ key: new Date(y, m - 1, prevDays - i).toDateString(), day: prevDays - i, other: true })
  for (let d = 1; d <= daysInMonth; d++) cells.push({ key: new Date(y, m, d).toDateString(), day: d, other: false })
  const tail = (7 - (cells.length % 7)) % 7
  for (let d = 1; d <= tail; d++) cells.push({ key: new Date(y, m + 1, d).toDateString(), day: d, other: true })
  return cells
})
const monthLabel = computed(() => `${monthCursor.value.y} 年 ${monthCursor.value.m + 1} 月`)
const selectedEvents = computed(() => eventsByDay.value.get(selectedDay.value) ?? [])
function dotsOf(key: string): string[] {
  return (eventsByDay.value.get(key) ?? []).slice(0, 3).map((e) => SCHEDULE_EVENT_TYPE_COLORS[e.eventType])
}
function shiftMonth(delta: number) {
  const d = new Date(monthCursor.value.y, monthCursor.value.m + delta, 1)
  monthCursor.value = { y: d.getFullYear(), m: d.getMonth() }
}

function dayLabel(key: string): string {
  const d = new Date(key)
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const diff = Math.round((new Date(d.toDateString()).getTime() - today.getTime()) / 86400000)
  const week = '日一二三四五六'[d.getDay()]
  const base = `${d.getMonth() + 1}月${d.getDate()}日 周${week}`
  if (diff === 0) return `今天 · ${base}`
  if (diff === 1) return `明天 · ${base}`
  return base
}
function hhmm(isoStr: string): string {
  const d = new Date(isoStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
function fullWhen(isoStr: string): string {
  const d = new Date(isoStr)
  return `${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]} ${hhmm(isoStr)}`
}
function targetOf(id: number | null) { return id == null ? null : listTargets().find((t) => t.id === id) ?? null }
function companyName(ev: ScheduleEvent): string {
  const target = targetOf(ev.jobProjectId)
  if (target) return target.name.split(' · ')[0]
  return ev.title.replace(/\s*(一面|二面|三面|终面|HR面|笔试|面试|跟进).*$/u, '').trim() || ev.title
}
function roleName(ev: ScheduleEvent): string {
  const target = targetOf(ev.jobProjectId)
  return target?.targetRole || SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]
}
/** 便签卡上的「面试计划名字」= 求职目标全名；没关联目标时退回从标题里剥出来的公司名。 */
function planName(ev: ScheduleEvent): string {
  return targetOf(ev.jobProjectId)?.name ?? companyName(ev)
}
function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
/** 时间轴一天最多摊 2 条，装不下的收进「+N」点开月历看——日程一多，摊开会把整条轨撑得参差不齐。 */
const TIMELINE_PREVIEW = 2
function timelineEvents(day: Date): ScheduleEvent[] {
  return eventsOnDate(listSchedules(), day).slice(0, TIMELINE_PREVIEW)
}
function extraEventsOn(day: Date): number {
  return Math.max(0, eventsOnDate(listSchedules(), day).length - TIMELINE_PREVIEW)
}
/** 「+N」的落点：月历并选中那一天，把溢出的日程完整交给月历视图。 */
function openDayInMonth(day: Date) {
  selectedDay.value = day.toDateString()
  viewMode.value = 'month'
}
function dayNumber(day: Date): string { return `${day.getMonth() + 1}/${day.getDate()}` }
function weekLabel(day: Date): string { return '日一二三四五六'[day.getDay()] }
function countdownLabel(event: ScheduleEvent | null): string {
  if (!event) return '暂时没有安排'
  const diff = new Date(event.startTime).getTime() - now.value
  if (diff <= 0) return '正在进行'
  const minutes = Math.ceil(diff / 60_000)
  if (minutes < 60) return `${minutes} 分钟后`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟后` : `${hours} 小时后`
}
function showView(mode: 'agenda' | 'month' | 'review') {
  viewMode.value = viewMode.value === mode ? 'agenda' : mode
}
/** 首页那条待办只指一个方向：一场就直接开笔记本，多场就交给复盘页的「还没写心得」列表。 */
function goPendingReview() {
  if (pendingReviews.value.length === 1) openNote(pendingReviews.value[0])
  else viewMode.value = 'review'
}
/** 标题里的轮次词。复盘窗口用它而不是「目标阶段」——后者和日程类型经常撞成「面试 · … · 面试」。 */
function roundOf(ev: ScheduleEvent): string | null {
  const m = /(一面|二面|三面|四面|终面|HR面|复试|加面|笔试|面试|跟进|回访)/u.exec(ev.title)
  return m && m[1] !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] ? m[1] : null
}
function addTagFromInput() {
  const t = tagInput.value.trim()
  if (!t) return
  if (draftTags.value.length >= MAX_TAGS) { toast('标签最多 12 个'); return }
  if (!draftTags.value.includes(t)) draftTags.value = [...draftTags.value, t]
  tagInput.value = ''
}
function toggleTag(tag: string) {
  draftTags.value = draftTags.value.includes(tag)
    ? draftTags.value.filter((t) => t !== tag)
    : [...draftTags.value, tag]
}
function removeTag(tag: string) {
  draftTags.value = draftTags.value.filter((t) => t !== tag)
}
function openCreate() {
  editing.value = null
  const d = new Date(); d.setHours(d.getHours() + 1, 0, 0, 0)
  form.value = { title: '', eventType: 'interview', start: toLocalInput(d.toISOString()), end: '', notes: '', reminder: 30, jobProjectId: null }
  sheetOpen.value = true
}
function openEdit(ev: ScheduleEvent) {
  editing.value = ev
  form.value = {
    title: ev.title, eventType: ev.eventType,
    start: toLocalInput(ev.startTime), end: toLocalInput(ev.endTime),
    notes: ev.notes ?? '', reminder: getReminder(ev.id), jobProjectId: ev.jobProjectId,
  }
  sheetOpen.value = true
}

async function submit() {
  if (!form.value.title.trim()) { toast('请填写日程标题'); return }
  const start = fromLocalInput(form.value.start)
  const end = fromLocalInput(form.value.end)
  // 结束早于开始以前会被静默存进去，再靠 eventEndsAt 兜底；现在在入口处拦下并说清原因。
  const timeError = scheduleTimeError(start, end)
  if (timeError || !start) { toast(timeError ?? '请选择开始时间'); return }
  const payload = {
    title: form.value.title.trim(), eventType: form.value.eventType, startTime: start,
    endTime: end, notes: form.value.notes || null,
    jobDescriptionId: null, jobProjectId: form.value.jobProjectId,
  }
  let id: number
  if (editing.value) { updateSchedule(editing.value.id, payload); id = editing.value.id }
  else { id = createSchedule(payload).id }
  setReminder(id, form.value.reminder)
  let reminderNote = ''
  if (form.value.reminder > 0) {
    await requestNotificationPermission()
    const ev = listSchedules().find((e) => e.id === id)
    const outcome = ev ? await scheduleReminder(ev, form.value.reminder) : 'failed' as const
    // toast 只有一个槽位，提醒失败必须并进最后一句，否则会被「日程已创建」盖掉。
    if (outcome !== 'scheduled') reminderNote = ` · ${REMINDER_OUTCOME_MESSAGES[outcome]}`
  } else { cancelReminder(id) }
  // 提醒链最后一环：面后 2 小时推一把「该写复盘了」；写完心得、删日程时都会撤掉。
  if ((payload.eventType === 'exam' || payload.eventType === 'interview') && reviewNudgeEnabled()) {
    const ev = listSchedules().find((e) => e.id === id)
    if (ev) await scheduleReviewNudge(ev)
  }
  sheetOpen.value = false
  toast(`${editing.value ? '日程已更新' : '日程已创建'}${reminderNote}`)
  void refreshArmed()
}
async function remove() {
  if (!editing.value) return
  const ev = editing.value
  const ok = await confirmAction({
    title: `删除「${headEllipsis(ev.title, 12)}」？`,
    message: '这条日程和它的提醒会从本机移除。',
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  cancelReminder(ev.id)
  cancelReviewNudge(ev.id)
  deleteSchedule(ev.id)
  sheetOpen.value = false
  if (noteTarget.value?.id === ev.id) noteTarget.value = null
  toast('日程已删除')
}
/** 四条落点必须分开说：拉起日历预填页、丢进分享面板、浏览器下载是三件不同的事，用户下一步动作也不同。 */
const CALENDAR_HANDOFF_NOTES: Record<CalendarHandoff, string> = {
  native: '已打开系统日历的新建事件页，确认无误后点保存',
  shared: '已把日历文件放进分享面板，选手机日历即可导入',
  downloaded: '已下载日历文件，可用手机日历打开',
  unavailable: '没有拿到日历，可以稍后再试一次',
}
async function syncToCalendar() {
  if (!editing.value) { toast('先保存日程，再添加到手机日历'); return }
  const event = listSchedules().find((item) => item.id === editing.value?.id)
  if (!event) return
  calendarSyncing.value = true
  try {
    toast(CALENDAR_HANDOFF_NOTES[await addToDeviceCalendar(event)])
  } catch (err) {
    toast(err instanceof Error ? `添加日历失败：${err.message}` : '添加日历失败')
  } finally { calendarSyncing.value = false }
}
</script>

<template>
  <div>
    <header class="page-head schedule-head">
      <div class="schedule-now">
        <span class="schedule-date">{{ todayLabel }}</span>
        <span class="schedule-clock">{{ nowClock }}</span>
      </div>
      <div class="head-actions">
        <button class="icon-btn" aria-label="全局搜索" @click="openSearch">
          <AppIcon name="search" :size="19" />
        </button>
        <button class="icon-btn" :class="{ on: viewMode === 'month' }" :aria-label="viewMode === 'month' ? '返回日程列表' : '打开月历'" @click="showView('month')">
          <AppIcon name="calendar" :size="19" />
        </button>
        <button class="icon-btn review-entry" :class="{ on: viewMode === 'review' }" :aria-label="viewMode === 'review' ? '返回日程列表' : '打开面试复盘'" @click="showView('review')">
          <AppIcon name="book" :size="19" />
        </button>
      </div>
    </header>

    <template v-if="viewMode === 'agenda'">
      <section class="schedule-focus workspace-card" aria-labelledby="focus-title">
        <div class="schedule-focus-head">
          <div>
            <span class="schedule-eyebrow">今日活动</span>
            <h1 id="focus-title">{{ nextEvent ? '下一场面试' : '今天的安排' }}</h1>
          </div>
          <span class="schedule-focus-count">{{ planEvents.length }} 场</span>
        </div>
        <template v-if="nextEvent">
          <button class="schedule-focus-event" :aria-label="`查看${companyName(nextEvent)}详情`" @click="openEdit(nextEvent)">
        <CompanyMark :name="companyName(nextEvent)" :size="48" />
            <span class="schedule-focus-copy">
              <strong>{{ companyName(nextEvent) }} · {{ SCHEDULE_EVENT_TYPE_LABELS[nextEvent.eventType] }}</strong>
              <small>{{ roleName(nextEvent) }} · {{ fullWhen(nextEvent.startTime) }}</small>
            </span>
            <span class="action-arrow" aria-hidden="true"><AppIcon name="arrowUpRight" :size="17" /></span>
          </button>
          <div class="schedule-focus-meta">
            <div><small>距离开始</small><strong>{{ countdownLabel(nextEvent) }}</strong></div>
            <div><small>提醒</small><strong>{{ getReminder(nextEvent.id) > 0 ? `提前 ${getReminder(nextEvent.id)} 分钟` : '未设置' }}<em v-if="armStateOf(nextEvent) === 'ok'" class="arm-tag ok">已排入</em><em v-else-if="armStateOf(nextEvent) === 'miss'" class="arm-tag miss">未排入</em><em v-else-if="armStateOf(nextEvent) === 'fired'" class="arm-tag fired">已提醒</em></strong></div>
          </div>
        </template>
        <div v-else class="schedule-focus-empty">
          <!-- nextEvent 只认「还没开始、或开始不到半小时」的日程。已经记了几十场但都过去了时，
               说「还没有安排面试」会和右上角的场次数直接打架。 -->
          <strong>{{ planEvents.length ? '没有即将开始的安排' : '还没有安排面试' }}</strong>
          <small v-if="planEvents.length">已记录 {{ planEvents.length }} 场，下一场还没添加。</small>
          <small v-else>添加一场真实面试，日程和提醒会在本机保存。</small>
          <button class="btn-primary" @click="openCreate"><AppIcon name="plus" :size="16" /> 添加日程</button>
        </div>
      </section>

      <!-- 战前速览：24 小时内开考/开面时，把这家公司的历史复盘推回眼前——上场被问了什么，这轮别再栽一次 -->
      <section v-if="preBattle" class="pre-battle workspace-card" aria-label="战前速览">
        <div class="pb-head">
          <span class="schedule-eyebrow">战前速览 · 24 小时内</span>
          <AppIcon name="book" :size="15" />
        </div>
        <strong class="pb-title">{{ companyName(preBattle.ev) }} · {{ SCHEDULE_EVENT_TYPE_LABELS[preBattle.ev.eventType] }}</strong>
        <template v-if="preBattle.last">
          <p class="pb-quote">「{{ headEllipsis(reviewPlainText(preBattle.last.review ?? ''), 72) }}」</p>
          <div v-if="preBattle.tags.length" class="pb-tags">
            <span v-for="t in preBattle.tags" :key="t" class="tag-chip">{{ t }}</span>
          </div>
          <template v-if="pbIvQuestions.length">
            <p class="pb-iv-label">这家公司的面经真题</p>
            <ul class="pb-iv-list">
              <li v-for="q in pbIvQuestions" :key="q">{{ q }}</li>
            </ul>
          </template>
          <button class="pb-open" @click="openNote(preBattle.last)">回看那篇心得 <AppIcon name="chevronRight" :size="14" /></button>
        </template>
        <p v-else class="pb-quote muted">这家你还没写过心得。面完回来补一篇，下一场就有了。</p>
      </section>

      <section class="timeline-block schedule-month workspace-card" aria-labelledby="timeline-title">
        <div class="section-head">
          <div>
            <h2 id="timeline-title">接下来一个月</h2>
            <p>横向滑动查看整月安排</p>
          </div>
          <button class="inline-action" @click="viewMode = 'month'">查看全部 <AppIcon name="chevronRight" :size="15" /></button>
        </div>
        <div class="timeline-scroll" tabindex="0" aria-label="接下来七天的日程时间轴">
          <div class="timeline-rail">
            <div v-for="day in timeline" :key="day.toDateString()" class="timeline-day" :class="{ today: isSameDay(day, nowDate) }">
              <div class="timeline-date"><strong>{{ isSameDay(day, nowDate) ? '今天' : dayNumber(day) }}</strong><small>周{{ weekLabel(day) }}</small></div>
              <div class="timeline-axis"><span class="timeline-node" /></div>
              <div class="timeline-events">
                <button v-for="ev in timelineEvents(day)" :key="ev.id" class="timeline-event" :aria-label="`${companyName(ev)} ${fullWhen(ev.startTime)}`" @click="openEdit(ev)">
                    <CompanyMark :name="companyName(ev)" :size="34" />
                  <span class="timeline-event-copy">
                    <strong>{{ companyName(ev) }}<template v-if="statusOf(ev)"><i class="live-badge"><span class="live-dot" />进行中</i></template></strong>
                    <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }} · {{ hhmm(ev.startTime) }}</small>
                  </span>
                </button>
                <button
                  v-if="extraEventsOn(day)"
                  class="timeline-more"
                  :aria-label="`该日还有 ${extraEventsOn(day)} 条日程，打开月历查看`"
                  @click="openDayInMonth(day)"
                >+{{ extraEventsOn(day) }}</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="plan-block schedule-list workspace-card" aria-labelledby="plan-title">
        <div class="section-head">
          <h2 id="plan-title">面试计划</h2>
          <span class="head-actions-gap">
            <button class="inline-action add-action" @click="openPaste"><AppIcon name="edit" :size="15" /> 粘贴录入</button>
            <button class="inline-action add-action" @click="openCreate"><AppIcon name="plus" :size="15" /> 添加日程</button>
          </span>
        </div>
        <div v-if="planEvents.length" class="plan-list">
          <div v-for="(ev, i) in planEvents" :key="ev.id" class="plan-row" :style="{ '--i': Math.min(i, 8) }">
            <button class="plan-main" @click="openEdit(ev)">
              <CompanyMark :name="companyName(ev)" :size="42" />
              <span class="plan-copy">
                <strong>{{ companyName(ev) }}<template v-if="statusOf(ev)"><i class="live-badge"><span class="live-dot" />进行中</i></template></strong>
                <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
                <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
                <small v-if="armStateOf(ev)" class="r-arm" :class="armStateOf(ev)">
                  <i class="r-arm-dot" aria-hidden="true" />{{ armLabelOf(ev) }}
                </small>
              </span>
              <AppIcon name="chevronRight" :size="18" class="plan-chev" />
            </button>
            <button
              v-if="reviewable(ev)"
              class="plan-note"
              :class="{ filled: !!ev.review?.trim() }"
              :aria-label="ev.review ? `查看 ${companyName(ev)} 的心得` : `为 ${companyName(ev)} 记心得`"
              @click="onReviewClick(ev)"
            >
              <AppIcon :name="ev.review?.trim() ? 'book' : 'edit'" :size="15" />
              <span>{{ reviewLabel(ev) }}</span>
            </button>
          </div>
        </div>
        <EmptyState v-else icon="calendar" title="还没有面试计划" hint="添加一场真实面试，日程和提醒会在本机保存。" />
      </section>

      <button v-if="pendingReviews.length" class="review-nudge workspace-card" @click="goPendingReview">
        <span class="review-nudge-mark"><AppIcon name="edit" :size="16" /></span>
        <span class="plan-copy">
          <strong>{{ pendingReviews.length }} 场已结束还没写心得</strong>
          <small class="plan-when">最近一场 · {{ companyName(pendingReviews[0]) }} · {{ fullWhen(pendingReviews[0].startTime) }}</small>
        </span>
        <span class="inline-action">去补写 <AppIcon name="chevronRight" :size="15" /></span>
      </button>
    </template>

    <template v-else-if="viewMode === 'review'">
      <section v-if="finishedEvents.length" class="review-hero workspace-card" :style="{ '--pct': reviewCoverage }">
        <div class="hero-ring">
          <svg viewBox="0 0 72 72" aria-hidden="true">
            <circle class="hero-ring-track" cx="36" cy="36" r="30" />
            <circle class="hero-ring-bar" :class="{ empty: !reviewCoverage }" cx="36" cy="36" r="30" />
          </svg>
          <span class="hero-ring-num">{{ reviewCoverage }}<em>%</em></span>
        </div>
        <div class="hero-copy">
          <span class="schedule-eyebrow">复盘覆盖率</span>
          <strong><b>{{ reviewedFinished.length }}</b> / {{ finishedEvents.length }} 篇已写</strong>
          <small>{{ reviewSpanLabel }}</small>
        </div>
      </section>

      <!-- 统计与报告合并为一张卡：两个可折叠区，不再互相抢视觉焦点 -->
      <section v-if="finishedEvents.length || listTargets().length" class="review-stats workspace-card" aria-label="统计与报告">
        <button v-if="finishedEvents.length" class="rs-toggle" :aria-expanded="statsOpen" @click="statsOpen = !statsOpen">
          <span class="schedule-eyebrow">心得统计</span>
          <span class="rs-hint">{{ reviewedEvents.length }} 篇 · {{ reviewFilter === 'all' ? '全部类型' : SCHEDULE_EVENT_TYPE_LABELS[reviewFilter] }}</span>
          <AppIcon name="chevronDown" :size="16" class="rs-chev" :class="{ flip: statsOpen }" />
        </button>
        <div v-if="statsOpen" class="rs-rings">
          <button
            v-for="r in reviewTypeRings" :key="r.key"
            class="rs-ring" :class="{ on: reviewFilter === r.key, zero: !r.count }"
            :style="{ '--tint': SCHEDULE_EVENT_TYPE_COLORS[r.key] }"
            :aria-label="`${r.label} ${r.count} 篇`"
            @click="toggleTypeFilter(r.key)"
          >
            <svg viewBox="0 0 48 48" aria-hidden="true">
              <circle class="rs-track" cx="24" cy="24" r="20" />
              <circle class="rs-bar" cx="24" cy="24" r="20" :stroke-dasharray="`${(r.pct / 100) * RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`" />
            </svg>
            <span class="rs-num"><strong>{{ r.count }}</strong><small>{{ r.pct }}%</small></span>
            <small class="rs-label">{{ r.label }}</small>
          </button>
        </div>
        <!-- 高频标签：复盘从记录升维成趋势——挂点都记在哪，一眼见底；点一根即按标签筛墙 -->
        <div v-if="statsOpen && reviewTagStats.length" class="rs-tagbars">
          <button
            v-for="s in reviewTagStats" :key="s.tag"
            class="rs-tagbar" :class="{ on: reviewTagFilter === s.tag }"
            :aria-label="`${s.tag} ${s.count} 篇`"
            @click="reviewTagFilter = reviewTagFilter === s.tag ? null : s.tag"
          >
            <span class="rt-name">{{ s.tag }}</span>
            <span class="rt-track"><i :style="{ width: `${s.pct}%` }" /></span>
            <span class="rt-num">{{ s.count }}</span>
          </button>
        </div>
        <div v-if="finishedEvents.length" class="rs-divider" aria-hidden="true" />
        <button class="rs-toggle" :aria-expanded="reportOpen" @click="reportOpen = !reportOpen">
          <span class="schedule-eyebrow">求职季报告</span>
          <span class="rs-hint">{{ seasonReport.active }} 个进行中 · {{ seasonReport.offered }} 个 Offer</span>
          <AppIcon name="chevronDown" :size="16" class="rs-chev" :class="{ flip: reportOpen }" />
        </button>
        <div v-if="reportOpen" class="report-grid">
          <div class="metric-card pastel-lilac"><strong>{{ seasonReport.total }}</strong><small>投递总数</small></div>
          <div class="metric-card pastel-mint"><strong>{{ seasonReport.active }}</strong><small>进行中</small></div>
          <div class="metric-card pastel-yellow"><strong>{{ seasonReport.interviews }}</strong><small>笔试/面试</small></div>
          <div class="metric-card pastel-pink"><strong>{{ seasonReport.coverage }}%</strong><small>复盘覆盖</small></div>
          <div class="metric-card pastel-mint"><strong>{{ seasonReport.offered }}</strong><small>已拿 Offer</small></div>
          <div class="metric-card pastel-yellow"><strong>{{ seasonReport.failed }}</strong><small>未通过/放弃</small></div>
          <p class="report-insight">{{ seasonReport.insight }}</p>
          <p v-if="seasonReport.pace" class="report-insight">{{ seasonReport.pace }}</p>
        </div>
      </section>

      <section v-if="pendingReviews.length" class="review-group workspace-card" aria-labelledby="pending-title">
        <div class="section-head">
          <div>
            <h2 id="pending-title">还没写心得</h2>
            <p>结束后当场记两句，比一周后回忆准得多</p>
          </div>
        </div>
        <button v-for="ev in pendingReviews" :key="ev.id" class="review-pending" @click="openNote(ev)">
          <CompanyMark :name="companyName(ev)" :size="42" />
          <span class="plan-copy">
            <strong>{{ planName(ev) }}</strong>
            <small class="plan-when">{{ fullWhen(ev.startTime) }} · 还没写心得</small>
          </span>
          <span class="inline-action"><AppIcon name="edit" :size="14" /> 补写</span>
        </button>
      </section>

      <template v-if="reviewedEvents.length">
        <div class="section-head wall-head">
          <div>
            <h2>复盘心得</h2>
            <p>点开一张就是笔记本，读到哪写到哪</p>
          </div>
        </div>
        <div class="toolbar">
          <div class="pill-row">
            <button class="filter-pill" :class="{ on: reviewFilter === 'all' }" @click="reviewFilter = 'all'">全部<em>{{ reviewedEvents.length }}</em></button>
            <button
              v-for="t in reviewTypeCounts" :key="t.key"
              class="filter-pill" :class="{ on: reviewFilter === t.key }"
              @click="reviewFilter = t.key"
            >{{ t.label }}<em>{{ t.count }}</em></button>
          </div>
          <div v-if="allTags.length" class="pill-row tag-filter">
            <button class="filter-pill" :class="{ on: reviewTagFilter === null }" @click="reviewTagFilter = null">不限标签</button>
            <button
              v-for="t in allTags" :key="t"
              class="filter-pill" :class="{ on: reviewTagFilter === t }"
              @click="reviewTagFilter = reviewTagFilter === t ? null : t"
            >{{ t }}</button>
          </div>
          <label class="search-box">
            <AppIcon name="search" :size="15" />
            <input v-model="reviewKeyword" placeholder="搜索计划名、心得内容或公司…" autocomplete="off">
          </label>
        </div>
      </template>

      <div v-if="reviewNotes.length" class="note-wall-wrap">
        <section
          v-for="group in reviewGroups"
          :key="group.key"
          class="note-month-group"
          :aria-label="`${group.label}，${group.notes.length} 篇心得`"
        >
          <p class="note-month"><strong>{{ group.label }}</strong><em>{{ group.notes.length }} 篇</em></p>
          <div class="note-wall">
            <button
              v-for="ev in group.notes" :key="ev.id"
              class="note-card"
              :style="{ '--note-c': noteColorOf(ev) }"
              @click="openNote(ev)"
            >
              <span class="note-tape" aria-hidden="true"></span>
              <span class="note-head">
                <CompanyMark :name="companyName(ev)" :size="28" />
                <span class="note-id">
                  <strong>{{ planName(ev) }}</strong>
                  <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roundOf(ev)"> · {{ roundOf(ev) }}</template></small>
                </span>
              </span>
              <p class="note-body">{{ noteSnippet(ev) }}</p>
              <span v-if="ev.reviewTags?.length" class="note-tags">
                <span v-for="t in visibleNoteTags(ev)" :key="t" class="note-tag">{{ t }}</span>
                <span v-if="hiddenNoteTags(ev)" class="note-tag note-tag-more">+{{ hiddenNoteTags(ev) }}</span>
              </span>
              <span class="note-foot">
                <time :datetime="ev.startTime">{{ fullWhen(ev.startTime) }}</time>
                <span class="note-len">{{ noteLen(ev) }} 字</span>
                <AppIcon name="chevronRight" :size="13" />
              </span>
            </button>
          </div>
        </section>
      </div>

      <EmptyState
        v-if="!reviewedEvents.length"
        icon="book"
        title="还没有写过心得"
        hint="日程一结束，列表那行的右侧就会出现「写心得」，点开像日记一样的笔记本写两句，就会汇总到这里。"
      />
      <EmptyState
        v-else-if="!reviewNotes.length"
        icon="search"
        title="没有匹配的心得"
        hint="换个关键词，或把类型筛选切回「全部」。"
      />
    </template>

    <template v-else-if="viewMode === 'month'">
      <div class="cal-head">
        <strong>{{ monthLabel }}</strong>
        <div class="cal-nav">
          <button class="icon-btn" aria-label="上个月" @click="shiftMonth(-1)"><AppIcon name="chevronLeft" :size="18" /></button>
          <button class="icon-btn" aria-label="下个月" @click="shiftMonth(1)"><AppIcon name="chevronRight" :size="18" /></button>
        </div>
      </div>
      <div class="cal-week"><span v-for="w in WEEK" :key="w">{{ w }}</span></div>
      <div class="cal-grid">
        <button
          v-for="cell in calCells" :key="cell.key"
          class="cal-cell"
          :class="{ other: cell.other, today: cell.key === new Date().toDateString(), selected: cell.key === selectedDay }"
          @click="selectedDay = cell.key"
        >{{ cell.day }}<span class="cal-dots"><i v-for="(c, i) in dotsOf(cell.key)" :key="i" :style="{ background: c }" /></span></button>
      </div>
      <p class="day-head"><span :class="{ today: selectedDay === new Date().toDateString() }">{{ dayLabel(selectedDay) }}</span></p>
      <div v-if="selectedEvents.length" class="plan-list calendar-plan-list">
        <div v-for="(ev, i) in selectedEvents" :key="ev.id" class="plan-row" :style="{ '--i': i }">
          <button class="plan-main" @click="openEdit(ev)">
            <CompanyMark :name="companyName(ev)" :size="42" />
            <span class="plan-copy">
              <strong>{{ companyName(ev) }}</strong>
              <small>{{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}<template v-if="roleName(ev) !== SCHEDULE_EVENT_TYPE_LABELS[ev.eventType]"> · {{ roleName(ev) }}</template></small>
              <small class="plan-when">{{ fullWhen(ev.startTime) }}<template v-if="ev.notes"> · {{ ev.notes }}</template></small>
            </span>
            <AppIcon name="chevronRight" :size="18" class="plan-chev" />
          </button>
          <button
            v-if="reviewable(ev)"
            class="plan-note"
            :class="{ filled: !!ev.review?.trim() }"
            :aria-label="ev.review ? `查看 ${companyName(ev)} 的心得` : `为 ${companyName(ev)} 写心得`"
            @click="openNote(ev)"
          >
            <AppIcon :name="ev.review?.trim() ? 'book' : 'edit'" :size="15" />
            <span>{{ ev.review?.trim() ? '看心得' : '写心得' }}</span>
          </button>
        </div>
      </div>
      <div v-else class="list">
        <EmptyState v-if="!selectedEvents.length" icon="calendar" title="该日暂无日程" hint="换一个日期，或返回日程页添加安排。" />
      </div>
    </template>

    <Sheet v-if="sheetOpen" :title="editing ? '编辑日程' : '新建日程'" @close="sheetOpen = false">
      <div class="field"><label>标题</label><input v-model="form.title" placeholder="如：字节跳动 一面"></div>
      <div class="field">
        <PickerField
          :model-value="form.eventType"
          :options="typeOptions"
          label="类型"
          title="选择类型"
          @update:model-value="(v) => form.eventType = (v as ScheduleEventType)"
        />
      </div>
      <div class="field"><label>开始时间</label><input v-model="form.start" type="datetime-local"></div>
      <div class="field"><label>结束时间（可选）</label><input v-model="form.end" type="datetime-local"></div>
      <div class="field">
        <PickerField
          :model-value="form.jobProjectId"
          :options="targetOptions"
          label="关联求职目标"
          title="选择求职目标"
          placeholder="不关联"
          clearable
          clear-label="不关联"
          searchable
          icon="target"
          @update:model-value="(v) => form.jobProjectId = (v as number | null)"
        />
      </div>
      <div class="field">
        <PickerField
          :model-value="form.reminder"
          :options="remindOptions"
          label="提醒"
          title="设置提醒"
          icon="bell"
          @update:model-value="(v) => form.reminder = Number(v)"
        />
      </div>
      <div class="field">
        <label>备注</label>
        <textarea v-model="form.notes" rows="3" placeholder="会议链接 / 面试官 / 注意事项…"></textarea>
      </div>
      <button v-if="editing" class="calendar-link" :disabled="calendarSyncing" @click="syncToCalendar">
        <AppIcon name="calendar" :size="16" />
        <span>{{ calendarSyncing ? '准备中…' : '在手机日历中新建' }}</span>
        <AppIcon name="external" :size="14" />
      </button>
      <div class="sheet-actions">
        <button v-if="editing" class="btn-danger" @click="remove"><AppIcon name="trash" :size="16" /> 删除</button>
        <button class="btn-ghost" @click="sheetOpen = false">取消</button>
        <button class="btn-primary" @click="submit">保存</button>
      </div>
    </Sheet>

    <!-- 全屏心得笔记本：阅读与编辑是同一张纸，点开就能直接往下写 -->
    <Transition name="reader">
      <div v-if="noteTarget" class="note-editor" role="dialog" aria-label="心得笔记本">
        <article class="ne-card">
          <header class="ne-head">
            <CompanyMark :name="companyName(noteTarget)" :size="44" />
            <div class="nr-head-copy">
              <p class="nb-date">{{ nbDate(noteTarget) }}<i v-if="statusOf(noteTarget)" class="live-badge"><span class="live-dot" />进行中</i></p>
              <strong class="nr-title">{{ planName(noteTarget) }}<template v-if="roundOf(noteTarget)"> · {{ roundOf(noteTarget) }}</template></strong>
              <small class="nr-sub">
                <span class="event-type-dot" :style="{ background: SCHEDULE_EVENT_TYPE_COLORS[noteTarget.eventType] }" />
                {{ SCHEDULE_EVENT_TYPE_LABELS[noteTarget.eventType] }} · {{ fullWhen(noteTarget.startTime) }}
              </small>
            </div>
            <button class="icon-btn" aria-label="保存并关闭" @click="closeNote"><AppIcon name="close" :size="18" /></button>
          </header>

          <!-- 便签纸：纸张风格（白/米/牛皮）单独选，默认白纸；胶带压角，像贴在桌面上的手写便签 -->
          <div class="ne-desk">
            <div class="ne-sheet" :style="{ '--paper-c': paperColor }">
              <span class="ne-tape" aria-hidden="true"></span>
              <div
                ref="noteEditorEl"
                class="ne-input"
                contenteditable="true"
                role="textbox"
                aria-multiline="true"
                aria-label="心得正文"
                data-placeholder="像写日记一样把这一场讲清楚：被问了什么、哪里没答好、下一轮补什么…"
                @input="onNoteInput"
              ></div>
            </div>
            <small class="nb-count" :class="{ over: noteOverLimit }">{{ noteTextLen }}/{{ NOTE_MAX_TEXT }}</small>
          </div>

          <!-- 抽屉：标签 + 提示语 + 清空/分享图，默认收起。由下方那根细线驱动：
               轻点开合，按住上下拖则跟手展开（拖动时关掉过渡，松手按阈值吸附）。 -->
          <div class="ne-drawer" :class="{ dragging: drawerDragging }" :style="{ height: drawerHeight }">
            <div ref="drawerInnerEl" class="ne-drawer-inner">
              <button class="ne-drawer-collapse" aria-label="收起标签与操作" @click="setDrawer(false)">
                <AppIcon name="chevronDown" :size="16" />
              </button>
              <div class="review-tags">
                <div class="review-tags-head">
                  <span class="review-tags-title">标签</span>
                  <small class="review-tags-hint">打几个标签，复盘墙里就能按它筛</small>
                </div>
                <div class="tag-list">
                  <button
                    v-for="t in visibleDraftTags" :key="t" type="button"
                    class="tag-chip on" @click="removeTag(t)"
                  >{{ t }}<AppIcon name="close" :size="12" /></button>
                  <button
                    v-if="hiddenSelectedCount > 0" type="button"
                    class="tag-chip tag-more" @click="selectedExpanded = !selectedExpanded"
                  >{{ selectedExpanded ? '收起' : `展开 ${hiddenSelectedCount} 个` }}
                    <AppIcon :name="selectedExpanded ? 'chevronDown' : 'chevronRight'" :size="12" />
                  </button>
                  <input
                    v-model="tagInput" class="tag-input" :maxlength="12"
                    placeholder="加标签…" @keydown.enter.prevent="addTagFromInput" @blur="addTagFromInput"
                  >
                </div>
                <div class="tag-suggest">
                  <button
                    v-for="t in visibleTagSuggestions" :key="t" type="button"
                    class="tag-chip" :class="{ on: draftTags.includes(t) }"
                    :disabled="!draftTags.includes(t) && draftTags.length >= MAX_TAGS"
                    @click="toggleTag(t)"
                  >{{ t }}</button>
                  <button v-if="hiddenTagCount > 0" type="button" class="tag-chip tag-more" @click="tagsExpanded = !tagsExpanded">
                    {{ tagsExpanded ? '收起' : `更多 ${hiddenTagCount} 个` }}
                    <AppIcon :name="tagsExpanded ? 'chevronDown' : 'chevronRight'" :size="12" />
                  </button>
                </div>
              </div>
              <div class="prompt-row">
                <button v-for="p in REVIEW_PROMPTS" :key="p" class="prompt-chip" @click="appendPrompt(p)">{{ p }}</button>
              </div>
              <div class="ne-drawer-actions">
                <button v-if="reviewPlainText(noteTarget.review ?? '').trim()" class="btn-danger" @click="clearNote"><AppIcon name="trash" :size="16" /> 清空</button>
                <button class="btn-ghost" :disabled="cardBusy" @click="shareReviewCard"><AppIcon name="share" :size="16" /> {{ cardBusy ? '生成中…' : '分享图' }}</button>
              </div>
            </div>
          </div>
          <!-- 无痕抓握线：一根细线，不像按钮——向上拖功能全部跟手带出来，向下拖或点右上角轻钮收起 -->
          <div
            class="ne-grip" role="button" tabindex="0"
            :aria-expanded="drawerRatio > 0.5" aria-label="标签与操作"
            @pointerdown="onGripDown" @pointermove="onGripMove"
            @pointerup="onGripUp" @pointercancel="onGripUp"
            @keydown.enter.prevent="setDrawer(drawerRatio <= 0.5)"
          ><i class="ne-grip-line" aria-hidden="true" /></div>
        </article>

        <!-- 插链接：不用弹窗，就在工具栏上方输入，确认后按记住的选区原位插入 -->
        <div v-if="linkDraft !== null" class="ne-linkbar">
          <input
            v-model="linkDraft" class="ne-link-input" type="url" inputmode="url"
            placeholder="输入链接，如 https://…"
            @keydown.enter.prevent="confirmLink"
          >
          <button class="btn-ghost btn-sm ne-link-cancel" @click="linkDraft = null">取消</button>
          <button class="btn-primary btn-sm ne-link-ok" @click="confirmLink">插入</button>
        </div>

        <!-- 工具栏常驻底部：加粗 / 图片 / 链接 / 纸张 / 便签色，不随长文滚走。
             「纸张」是里面写的本子（白/米/牛皮），「便签色」是外头那张墙上的便签——两个概念分开选。 -->
        <div class="ne-toolbar">
          <button class="ne-tool" aria-label="加粗" @pointerdown.prevent="boldSelection"><b>B</b></button>
          <button class="ne-tool" aria-label="插入图片" @pointerdown.prevent="insertImage"><AppIcon name="image" :size="17" /></button>
          <button class="ne-tool" aria-label="插入链接" @pointerdown.prevent="insertLink"><AppIcon name="link" :size="17" /></button>
          <span class="ne-sep" aria-hidden="true" />
          <div class="ne-group" role="group" aria-label="纸张风格">
            <span class="ne-group-name">纸张</span>
            <button
              v-for="p in NOTE_PAPERS" :key="p.key"
              class="ne-paper" :class="{ on: draftPaper === p.key }"
              :aria-pressed="draftPaper === p.key"
              @pointerdown.prevent="pickPaper(p.key)"
            >{{ p.label }}</button>
          </div>
          <span class="ne-sep" aria-hidden="true" />
          <div class="ne-group" role="group" aria-label="便签颜色">
            <span class="ne-group-name">便签色</span>
            <button
              v-for="c in NOTE_COLORS" :key="c"
              class="ne-swatch" :class="{ on: draftColor === c }"
              :style="{ background: c }" :aria-label="`便签色 ${c}`"
              @pointerdown.prevent="pickColor(c)"
            />
            <button class="ne-swatch-auto" :class="{ on: !draftColor }" aria-label="便签色跟随日程类型配色" @pointerdown.prevent="pickColor(null)">类型色</button>
          </div>
          <span class="ne-sep" aria-hidden="true" />
          <button class="ne-done" @click="closeNote">完成</button>
        </div>
        <input ref="imageInputEl" type="file" accept="image/*" class="ne-file" @change="onPickImage">
      </div>
    </Transition>

    <!-- 复盘联动推进：写完心得顺手问一句结果，目标状态跟着长准，不用用户再去目标页手动改 -->
    <Sheet v-if="outcomeAsk" title="这场的结果是？" @close="skipOutcome">
      <p class="oc-lead">
        {{ companyName(outcomeAsk) }} · {{ SCHEDULE_EVENT_TYPE_LABELS[outcomeAsk.eventType] }}已结束。
        对齐一下目标状态，看板和漏斗才会反映真实进度。
      </p>
      <div class="oc-actions">
        <button class="oc-btn oc-pass" @click="askOutcome(true)">
          <strong>过了</strong>
          <small>推进到下一环</small>
        </button>
        <button class="oc-btn oc-fail" @click="askOutcome(false)">
          <strong>挂了</strong>
          <small>标记结果并锁定</small>
        </button>
      </div>
      <button class="btn-ghost oc-skip" @click="skipOutcome">先不定，之后在目标页里调</button>
    </Sheet>

    <!-- 粘贴录入：面试通知原文一粘，公司/时间/类型先填好九成，剩下一成在表单里确认 -->
    <Sheet v-if="pasteOpen" title="粘贴通知，自动填日程" @close="pasteOpen = false">
      <p class="oc-lead">把面试 / 笔试通知的原文整段粘进来——公司、日期、时间、轮次能认的都替你填好，提交前在表单里核对一遍。</p>
      <textarea
        v-model="pasteText" class="paste-input" rows="6"
        placeholder="例如：【腾讯】邀请您参加后端开发工程师岗位的一面。时间：9月18日（周五）下午2:30…"
      ></textarea>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="pasteOpen = false">取消</button>
        <button class="btn-primary" @click="applyPaste">解析并预填</button>
      </div>
    </Sheet>

    <!-- 全局搜索：目标 / 日程 / 心得一框搜完，结果直接跳到能动手的地方 -->
    <Sheet v-if="searchOpen" title="全局搜索" @close="searchOpen = false">
      <input v-model="searchKw" class="paste-input" placeholder="搜目标、日程、心得…" />
      <div class="search-results">
        <template v-if="searchResults.targets.length">
          <p class="search-group">求职目标</p>
          <button v-for="t in searchResults.targets" :key="`t${t.id}`" class="search-row" @click="searchGoTargets">
            <strong>{{ t.name }}</strong>
            <small>{{ TARGET_STAGE_LABELS[normalizeTargetStage(t.stage)] }}<template v-if="t.targetRole"> · {{ t.targetRole }}</template></small>
          </button>
        </template>
        <template v-if="searchResults.schedules.length">
          <p class="search-group">日程</p>
          <button v-for="ev in searchResults.schedules" :key="`s${ev.id}`" class="search-row" @click="searchOpen = false; openEdit(ev)">
            <strong>{{ planName(ev) }} · {{ ev.title }}</strong>
            <small>{{ fullWhen(ev.startTime) }}</small>
          </button>
        </template>
        <template v-if="searchResults.reviews.length">
          <p class="search-group">心得</p>
          <button v-for="ev in searchResults.reviews" :key="`r${ev.id}`" class="search-row" @click="searchOpen = false; openNote(ev)">
            <strong>{{ planName(ev) }} · {{ SCHEDULE_EVENT_TYPE_LABELS[ev.eventType] }}</strong>
            <small>{{ headEllipsis(noteSnippet(ev), 40) }}</small>
          </button>
        </template>
        <p v-if="searchEmpty" class="pb-quote muted">没有匹配的目标、日程或心得。</p>
        <p v-else-if="!searchKw.trim()" class="pb-quote muted">输入关键词，找回任何一条记录。</p>
      </div>
    </Sheet>
  </div>
</template>
