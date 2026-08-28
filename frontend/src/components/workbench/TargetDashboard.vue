<template>
  <section data-test="target-dashboard" class="workbench-dashboard">
    <p v-if="materialError" class="material-error">{{ materialError }}</p>

    <header class="workspace-header">
      <div>
        <p class="workspace-date">{{ todayLabel }}</p>
        <p class="workspace-motto">专注今天的准备，明天的你会感谢现在的投入。</p>
      </div>
      <button type="button" class="schedule-link" data-test="header-schedule" @click="emitAction('open-schedule')">
        <el-icon :size="14"><Calendar /></el-icon>
        <span>查看日程</span>
      </button>
    </header>

    <!-- 主工作区：左侧行动与复盘，右侧只承载真实的近期面试。 -->
    <div class="master-grid" :style="masterGridStyle">
      <main data-test="detail-pane" class="detail-pane">
        <section data-test="next-interview" class="next-interview">
          <div class="section-heading accent-heading">
            <span class="section-mark" aria-hidden="true"></span>
            <p class="eyebrow">下一场面试</p>
          </div>

          <template v-if="selectedEvent">
            <div class="next-identity">
              <span class="company-mark next-company-mark" :class="[companyTone(selectedEvent.companyLabel || selectedEvent.title), { 'has-logo': Boolean(selectedEvent.logoUrl) }]" :style="!selectedEvent.logoUrl && selectedEvent.logoColor ? { backgroundColor: selectedEvent.logoColor } : undefined">
                <span v-if="!selectedEvent.logoUrl">{{ companyInitial(selectedEvent.companyLabel || selectedEvent.title) }}</span>
                <img v-if="selectedEvent.logoUrl" :src="selectedEvent.logoUrl" alt="" @error="hideBrokenCompanyLogo" />
              </span>
              <div class="next-identity-copy">
                <h1 data-test="detail-title">{{ selectedEvent.companyLabel ? `${selectedEvent.companyLabel} · ${selectedEvent.title}` : selectedEvent.title }}</h1>
                <p v-if="selectedEvent.roleLabel" class="next-role">{{ selectedEvent.roleLabel }}</p>
              </div>
            </div>
            <div class="next-meta">
              <span class="next-date"><span class="next-day">{{ selectedEvent.dayLabel || selectedEvent.relativeLabel }}</span><span data-test="detail-time">{{ selectedEvent.timeLabel }}</span></span>
              <span v-if="selectedEvent.countdownLabel" class="next-countdown">{{ selectedEvent.countdownLabel.replace(/^还有\s*/, '') }}</span>
            </div>
            <div data-test="next-actions" class="next-actions next-actions--spaced">
              <button type="button" class="primary-button" data-test="next-button" @click="emitAction(detail.targetId ? 'open-target' : 'link-target', detail.targetId ?? Number(selectedEvent.id))">
                开始准备
              </button>
              <button type="button" class="text-action" data-test="view-schedule" @click="emitAction('open-interview-home', detail.targetId ?? undefined)">
                <el-icon :size="20"><Tickets /></el-icon>面试详情
              </button>
            </div>

            <section v-if="detail.targetLinked || detail.kind === 'target'" data-test="detail-linked-target" class="context-rows context-rows--spaced">
              <span class="sr-only">{{ linkedTargetCopy() }}</span>
              <div class="context-row" data-test="readiness-resume">
                <el-icon :size="16"><Document /></el-icon>
                <span class="context-label">关联简历</span>
                <span class="context-value">{{ readinessValue('resume') }}</span>
                <button type="button" class="context-action object-action" @click="emitAction(resumeAction, detail.targetId ?? undefined)">{{ resumeActionLabel }}</button>
              </div>
              <div class="context-row" data-test="readiness-stage">
                <el-icon :size="16"><Notebook /></el-icon>
                <span class="context-label">当前阶段</span>
                <span class="context-value">{{ stageValue }}</span>
                <button type="button" class="context-action" @click="emitAction('open-target', detail.targetId ?? undefined)">查看</button>
              </div>
            </section>
            <section v-else data-test="detail-unlinked" class="unlinked-context context-rows--spaced">
              <p>未关联求职目标</p>
              <div><button type="button" data-test="link-target" class="text-action" @click="emitAction('link-target', Number(selectedEvent.id))">关联目标<el-icon :size="13"><ArrowRight /></el-icon></button><button type="button" class="text-action" @click="emitAction('open-schedule')">查看日程<el-icon :size="13"><ArrowRight /></el-icon></button></div>
            </section>
          </template>
          <template v-else>
            <div class="empty-interview-state">
              <h1 data-test="detail-empty">{{ detail.kind === 'empty' ? '今天没有安排' : '还没有安排面试' }}</h1>
              <p class="empty-copy" data-test="detail-empty-copy">为当前求职计划添加一场面试后，这里会显示准备入口。</p>
              <button v-if="detail.kind === 'empty'" type="button" class="primary-button" data-test="detail-empty-action" @click="emitAction('add-job', detail.targetId ?? undefined)">录入岗位<el-icon :size="14"><ArrowRight /></el-icon></button>
              <button v-else type="button" class="primary-button" data-test="detail-empty-action" @click="emitAction('open-schedule')">查看日程<el-icon :size="14"><ArrowRight /></el-icon></button>
            </div>
          </template>
        </section>

      </main>

      <div data-test="pane-separator" class="pane-divider" aria-hidden="true"></div>

      <aside data-test="agenda-pane" class="agenda-pane" aria-label="接下来七天的面试">
        <header class="agenda-header">
          <p class="eyebrow">接下来 7 天</p>
        </header>

        <div v-if="!upcomingEvents.length" class="agenda-empty-state">
          <p class="agenda-empty">近期没有安排</p>
          <button type="button" class="quiet-action agenda-all-link" data-test="view-all-schedule" @click="emitAction('open-schedule')">查看全部日程<el-icon :size="13"><ArrowRight /></el-icon></button>
        </div>
        <div v-else class="agenda-timeline">
          <div v-for="event in upcomingEvents" :key="event.id" class="agenda-group" data-test="agenda-group">
            <button
              :key="event.id"
              type="button"
              data-test="agenda-row"
              class="agenda-row"
              :class="{ selected: event.id === selectedEventId }"
              :aria-pressed="event.id === selectedEventId"
              @click="handlePick(event.id)"
            >
              <span class="agenda-company" aria-hidden="true"><span class="company-mark" :class="[companyTone(event.companyLabel || event.title), { 'has-logo': Boolean(event.logoUrl) }]" :style="!event.logoUrl && event.logoColor ? { backgroundColor: event.logoColor } : undefined"><span v-if="!event.logoUrl">{{ companyInitial(event.companyLabel || event.title) }}</span><img v-if="event.logoUrl" :src="event.logoUrl" alt="" @error="hideBrokenCompanyLogo" /></span></span>
              <span class="row-copy">
                <span class="row-title"><span class="row-title-text">{{ rowTitle(event) }}</span></span>
                <span v-if="event.targetName" data-test="agenda-target" class="row-target">{{ event.targetName }}</span>
                <span class="row-meta"><span class="row-day">{{ event.dayLabel || event.relativeLabel }}</span>{{ event.timeLabel }}</span>
              </span>
              <el-icon class="row-arrow" :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
        <button v-if="upcomingEvents.length" type="button" class="quiet-action agenda-all-link" data-test="view-all-schedule" @click="emitAction('open-schedule')">查看全部日程<el-icon :size="13"><ArrowRight /></el-icon></button>
      </aside>
    </div>

    <section class="workspace-lower" aria-label="最近练习与工具">
      <section data-test="recent-activity" class="recent-activity">
        <div class="section-heading">
          <p class="eyebrow">最近练习</p>
          <button v-if="recentActivity.length" type="button" class="quiet-action" @click="emitAction('open-feedback', recentActivity[0].targetId ?? undefined, recentActivity[0].sessionId ?? undefined)">查看复盘<el-icon :size="13"><ArrowRight /></el-icon></button>
        </div>
        <template v-if="recentActivity.length">
          <div v-for="item in recentActivity" :key="item.id" class="review-row" data-test="activity-row">
            <span class="review-icon" :class="`mode-${item.mode ?? 'ROLE_BASED'}`" aria-hidden="true"><el-icon :size="22"><component :is="recentModeIcon(item.mode)" /></el-icon></span>
            <div class="review-copy">
              <p class="review-title">{{ recentModeLabel(item.mode) }} · {{ item.title }}</p>
              <p class="review-date">{{ item.dateLabel }}<span v-if="item.companyLabel"> · {{ item.companyLabel }}</span></p>
            </div>
            <p class="review-summary">{{ item.summary }}</p>
            <button type="button" class="quiet-action activity-report" data-test="activity-report" @click="emitAction('open-feedback', item.targetId ?? undefined, item.sessionId ?? undefined)">查看复盘<el-icon :size="13"><ArrowRight /></el-icon></button>
          </div>
        </template>
        <div v-else class="empty-activity">
          <el-icon :size="18"><ChatDotRound /></el-icon>
          <span>完成第一次练习后，这里会保留你的核心反馈。</span>
        </div>
      </section>

      <section data-test="tool-index" class="tool-index">
        <p class="eyebrow">职达工具</p>
        <div data-test="tool-list" class="tool-list tool-list--bounded">
          <button v-for="tool in tools" :key="tool.label" type="button" class="tool-item tool-item--centered" @click="emitAction(tool.action, detail.targetId ?? undefined)">
            <el-icon :size="34"><component :is="tool.icon" /></el-icon>
            <span><strong>{{ tool.label }}</strong><small>{{ tool.description }}</small></span>
          </button>
        </div>
      </section>

      <section v-if="detail.nextAction && selectedEvent" data-test="detail-next" class="workspace-footer">
        <span class="footer-leading" aria-hidden="true"><el-icon :size="22"><Sunny /></el-icon></span>
        <span class="hint-label">建议</span>
        <span class="hint-copy">{{ detail.nextAction.text }}</span>
        <button type="button" class="quiet-action" @click="emitAction(detail.nextAction.action, detail.targetId ?? undefined, detail.feedbackSessionId ?? undefined)"><el-icon :size="13"><ArrowRight /></el-icon></button>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Aim, ArrowRight, Calendar, ChatDotRound, ChatLineSquare, Document, Notebook, Reading, Sunny, Tickets } from '@element-plus/icons-vue'

export interface AgendaEventView {
  id: string
  title: string
  eventType: string
  timeLabel: string
  relativeLabel: string
  dayLabel: string
  companyLabel: string
  roleLabel: string
  /** 用户创建的求职目标名称；与岗位详情分离，确保 JD 读取失败时仍可见。 */
  targetName?: string
  countdownLabel: string
  logoUrl?: string
  logoColor?: string
}
export interface ReadinessRow {
  key: string
  label: string
  meta: string
  subMeta?: string
  ready: boolean
  actionLabel: string
  action: TargetDashboardAction
}
export interface NextAction { text: string; button: string; hint?: string; action: TargetDashboardAction }
export interface DetailView {
  kind: 'event' | 'target' | 'empty'
  note: string
  companyLabel: string
  roleLabel: string
  targetName: string
  targetLinked: boolean
  targetId: number | null
  readiness: ReadinessRow[]
  nextAction: NextAction | null
  stageLabel?: string
  feedbackSessionId?: number | null
}
export interface RecentActivityItem { id: number; dateLabel: string; companyLabel: string; title: string; summary: string; targetId: number | null; sessionId?: number | null; mode?: 'ROLE_BASED' | 'KNOWLEDGE_TRAINING' | 'EXPERIENCE_SIMULATION' }
export type TargetDashboardAction = 'add-job' | 'select-resume' | 'open-editor' | 'open-interview' | 'open-interview-home' | 'open-schedule' | 'open-feedback' | 'open-target' | 'link-target' | 'open-knowledge' | 'open-resumes'

const props = withDefaults(defineProps<{
  agendaEvents: AgendaEventView[]
  selectedEventId: string | null
  detail: DetailView
  recentActivity: RecentActivityItem[]
  materialError?: string
}>(), {
  agendaEvents: () => [],
  selectedEventId: null,
  detail: () => ({ kind: 'empty' as const, note: '还没有求职目标', companyLabel: '', roleLabel: '', targetName: '', targetLinked: false, targetId: null, readiness: [], nextAction: null }),
  recentActivity: () => [],
  materialError: '',
})
const emit = defineEmits<{ (event: 'select-event', id: string): void; (event: 'action', action: TargetDashboardAction, targetId?: number, sessionId?: number): void }>()

// The split follows the reference composition and is intentionally fixed.
// A visual divider is enough; making it draggable adds noise to a designed workspace.
const masterGridStyle = { gridTemplateColumns: 'minmax(0, 1fr) 1px 520px' }
const today = new Date()
const todayLabel = `${today.getMonth() + 1}月${today.getDate()}日 · ${new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(today)}`

const selectedEvent = computed(() => props.agendaEvents.find((event) => event.id === props.selectedEventId) ?? props.agendaEvents[0] ?? null)
const upcomingEvents = computed(() => props.agendaEvents.slice(0, 3))
const resumeRow = computed(() => props.detail.readiness.find((row) => row.key === 'resume'))
const resumeAction = computed<TargetDashboardAction>(() => resumeRow.value?.action ?? (props.detail.targetId ? 'open-editor' : 'open-resumes'))
const resumeActionLabel = computed(() => resumeRow.value?.actionLabel ?? '查看')
const stageValue = computed(() => props.detail.stageLabel || '准备中')
const tools = [
  { label: '简历', description: '查看简历库', icon: Document, action: 'open-resumes' as TargetDashboardAction },
  { label: '知识库', description: '选择资料', icon: Reading, action: 'open-knowledge' as TargetDashboardAction },
  { label: '面试练习', description: '查看练习记录', icon: ChatLineSquare, action: 'open-interview-home' as TargetDashboardAction },
  { label: '日程', description: '查看安排', icon: Calendar, action: 'open-schedule' as TargetDashboardAction },
  { label: '职业目标', description: '切换目标', icon: Aim, action: 'open-target' as TargetDashboardAction },
]

function emitAction(action: TargetDashboardAction, targetId?: number, sessionId?: number) {
  if (sessionId == null) emit('action', action, targetId)
  else emit('action', action, targetId, sessionId)
}
function readinessValue(key: string) {
  const row = props.detail.readiness.find((item) => item.key === key)
  return row ? `${row.meta}${row.subMeta ? ` · ${row.subMeta}` : ''}` : '未绑定'
}
function linkedTargetCopy() {
  if (props.detail.companyLabel) return props.detail.roleLabel ? `${props.detail.companyLabel} · ${props.detail.roleLabel}` : props.detail.companyLabel
  return props.detail.targetName || props.detail.roleLabel || '求职目标'
}
function handlePick(id: string) { emit('select-event', id) }
function rowTitle(event: AgendaEventView) { return event.companyLabel ? `${event.companyLabel} · ${event.title}` : event.title }
function companyInitial(company: string) {
  const value = company.trim()
  if (value.includes('腾讯')) return 'T'
  if (value.includes('字节')) return 'B'
  if (value.includes('美团')) return '美'
  if (value.includes('阿里')) return 'A'
  return value.slice(0, 1) || '·'
}
function companyTone(company: string) {
  if (company.includes('字节')) return 'tone-byte'
  if (company.includes('美团')) return 'tone-meituan'
  if (company.includes('腾讯')) return 'tone-tencent'
  return 'tone-neutral'
}
function hideBrokenCompanyLogo(event: Event) {
  if (event.target instanceof HTMLImageElement) event.target.style.display = 'none'
}
function recentModeLabel(mode?: RecentActivityItem['mode']) {
  return { ROLE_BASED: '自由面试', KNOWLEDGE_TRAINING: '知识训练', EXPERIENCE_SIMULATION: '真题演练' }[mode ?? 'ROLE_BASED']
}
function recentModeIcon(mode?: RecentActivityItem['mode']) {
  return { ROLE_BASED: ChatLineSquare, KNOWLEDGE_TRAINING: Reading, EXPERIENCE_SIMULATION: Document }[mode ?? 'ROLE_BASED']
}
</script>

<style scoped>
.workbench-dashboard{--canvas:#fff;--surface:#fff;--surface-soft:#fafbf9;--ink:#171717;--copy:#5f625f;--muted:#929690;--line:rgba(23,23,23,.12);--line-soft:rgba(23,23,23,.07);--brand:#159447;--brand-soft:rgba(21,148,71,.08);--action:#111;display:flex;flex-direction:column;box-sizing:border-box;width:100%;height:100%;min-height:0;overflow:auto;background:var(--canvas);color:var(--ink);font-family:-apple-system,BlinkMacSystemFont,"SF Pro Text","PingFang SC","Microsoft YaHei",sans-serif;color-scheme:light}
.is-dark .workbench-dashboard{--canvas:#111212;--surface:#181a19;--surface-soft:#151716;--ink:#f2f2ef;--copy:#a7aaa5;--muted:#767b75;--line:rgba(255,255,255,.13);--line-soft:rgba(255,255,255,.08);--brand:#54bd7c;--brand-soft:rgba(84,189,124,.11);--action:#f1f1ee;color-scheme:dark}
.workbench-dashboard button{font:inherit}.material-error{margin:8px 36px 0;padding:8px 12px;border:1px solid rgba(183,58,47,.2);border-radius:8px;background:rgba(183,58,47,.06);color:#a33e34;font-size:12px}
.workspace-header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px;padding:54px 52px 22px;border-bottom:1px solid var(--line-soft)}.workspace-date{margin:0;color:var(--copy);font-size:12px;letter-spacing:.03em}.workspace-motto{margin:11px 0 0;font-size:22px;font-weight:520;letter-spacing:-.035em;line-height:1.35}.schedule-link{display:inline-flex;align-items:center;gap:7px;margin-top:2px;padding:8px 12px;border:1px solid var(--line);border-radius:8px;background:transparent;color:var(--ink);font-size:12px;cursor:pointer}.schedule-link:hover{border-color:var(--ink)}
.master-grid{display:grid;flex:1 0 auto;min-height:0}.detail-pane{min-width:0;box-sizing:border-box;padding:28px 44px 24px 52px}.next-interview{max-width:790px}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:18px}.accent-heading{justify-content:flex-start;align-items:flex-start}.section-mark{width:4px;min-height:38px;margin-top:3px;background:var(--brand);border-radius:3px}.eyebrow{margin:0;color:var(--ink);font-size:15px;font-weight:650;letter-spacing:-.015em}.accent-heading .eyebrow{color:var(--copy);font-size:13px;font-weight:600}.accent-heading h1{margin:7px 0 0;font-size:25px;font-weight:600;letter-spacing:-.035em;line-height:1.2}.next-meta{display:flex;align-items:baseline;gap:13px;margin:20px 0 0 22px}.next-date{display:flex;align-items:baseline;gap:10px;font-size:23px;font-weight:500;letter-spacing:-.025em;font-variant-numeric:tabular-nums}.next-day{font-size:.56em;color:var(--copy);font-weight:550;letter-spacing:0}.next-countdown{color:var(--brand);font-size:13px;font-weight:650}.next-role{margin:6px 0 0 22px;color:var(--copy);font-size:13px}.next-actions{display:flex;align-items:center;gap:17px;margin:18px 0 0 22px}.primary-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:38px;padding:0 20px;border:0;border-radius:8px;background:var(--action);color:#fff;font-size:13px;font-weight:650;cursor:pointer;transition:transform .15s ease,opacity .15s ease}.is-dark .primary-button{color:#171717}.primary-button:hover{opacity:.88;transform:translateY(-1px)}.text-action,.quiet-action{display:inline-flex;align-items:center;gap:6px;padding:0;border:0;background:transparent;color:var(--copy);font-size:12px;cursor:pointer}.text-action:hover,.quiet-action:hover{color:var(--ink)}
.context-rows{display:grid;gap:0;margin:21px 0 0 22px;border-top:1px solid var(--line-soft);max-width:650px}.context-row{display:grid;grid-template-columns:22px 74px minmax(0,1fr) auto;align-items:center;gap:10px;min-height:44px;border-bottom:1px solid var(--line-soft);color:var(--copy);font-size:12px}.context-row>.el-icon{color:var(--muted)}.context-label{color:var(--muted);font-size:11px}.context-value{overflow:hidden;color:var(--ink);font-weight:550;text-overflow:ellipsis;white-space:nowrap}.context-action{display:inline-flex;align-items:center;gap:3px;padding:0;border:0;background:transparent;color:var(--copy);font-size:11px;cursor:pointer}.context-action:hover{color:var(--ink)}.unlinked-context{display:grid;gap:9px;margin:21px 0 0 22px;padding:13px 0;border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft);color:var(--copy);font-size:12px}.unlinked-context p{margin:0;color:var(--muted)}.unlinked-context>div{display:flex;gap:18px}.fallback-identity{display:grid;gap:4px;margin:18px 0 0 22px;color:var(--ink);font-size:13px;font-weight:600}.fallback-identity small{color:var(--copy);font-size:11px;font-weight:400}.action-hint{display:flex;align-items:baseline;gap:13px;max-width:790px;margin:20px 0 0 22px;padding:12px 0;border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)}.hint-label{color:var(--brand);font-size:11px;font-weight:650}.hint-copy{color:var(--copy);font-size:12px;line-height:1.5}
.recent-activity{max-width:790px;margin:23px 0 0 22px}.recent-activity .section-heading{margin-bottom:10px}.review-row{display:grid;grid-template-columns:70px minmax(0,1fr) auto;align-items:start;gap:14px;padding:12px 0;border-top:1px solid var(--line-soft)}.review-date{color:var(--muted);font-size:11px;font-variant-numeric:tabular-nums}.review-copy{min-width:0}.review-title{margin:0;font-size:13px;font-weight:600}.review-summary{display:-webkit-box;overflow:hidden;margin:4px 0 0;color:var(--copy);font-size:12px;line-height:1.5;-webkit-box-orient:vertical;-webkit-line-clamp:2}.empty-activity{display:flex;align-items:center;gap:9px;padding:14px 0;border-top:1px solid var(--line-soft);color:var(--muted);font-size:12px}
.resume-strip{display:flex;align-items:center;justify-content:space-between;gap:22px;max-width:790px;margin:25px 0 0 22px;padding:14px 0;border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)}.resume-title{display:flex;align-items:center;gap:10px;min-width:0}.resume-title>.el-icon{color:var(--copy)}.resume-title div{display:grid;gap:3px;min-width:0}.resume-title strong{overflow:hidden;font-size:13px;text-overflow:ellipsis;white-space:nowrap}.resume-title span{color:var(--muted);font-size:11px}.resume-actions{display:flex;align-items:center;gap:7px;flex:0 0 auto}.resume-primary,.resume-action{min-height:31px;padding:0 12px;border-radius:7px;font-size:11px;cursor:pointer}.resume-primary{border:1px solid var(--action);background:var(--action);color:#fff}.is-dark .resume-primary{color:#171717}.resume-action{border:1px solid var(--line);background:transparent;color:var(--ink)}.resume-action:hover{border-color:var(--ink)}
.tool-index{max-width:790px;margin:22px 0 0 22px;padding-bottom:10px}.tool-list{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));margin-top:11px;border-top:1px solid var(--line-soft);border-bottom:1px solid var(--line-soft)}.tool-item{display:flex;align-items:center;gap:9px;min-width:0;padding:12px 12px 12px 0;border:0;border-right:1px solid var(--line-soft);background:transparent;color:var(--ink);text-align:left;cursor:pointer}.tool-item:last-child{border-right:0;padding-left:12px}.tool-item:not(:first-child){padding-left:12px}.tool-item>.el-icon{color:var(--copy)}.tool-item span{display:grid;gap:3px;min-width:0}.tool-item strong{font-size:13px;font-weight:600}.tool-item small{overflow:hidden;color:var(--muted);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.tool-item:hover>.el-icon,.tool-item:hover strong{color:var(--brand)}
.pane-divider{position:relative;width:1px;background:var(--line-soft);cursor:col-resize;touch-action:none;outline:none}.pane-divider::before{content:'';position:absolute;top:0;bottom:0;left:-2px;width:5px}.pane-divider:hover,.pane-divider:focus-visible,.pane-divider.dragging{background:var(--brand)}.pane-divider:focus-visible{box-shadow:0 0 0 2px var(--brand-soft)}
.agenda-pane{min-width:0;box-sizing:border-box;padding:28px 28px 24px 30px;overflow:hidden}.agenda-header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.agenda-header>div{display:grid;gap:5px}.agenda-count{color:var(--muted);font-size:11px}.agenda-empty{margin:22px 0 0;color:var(--muted);font-size:13px}.agenda-timeline{display:grid;margin-top:16px}.agenda-group{display:contents}.tl-milestone{position:relative;display:grid;grid-template-columns:26px minmax(0,1fr);align-items:center;gap:9px;min-height:26px;padding:7px 0 2px}.tl-milestone.first{padding-top:0}.tl-milestone::before,.agenda-row::before{content:'';position:absolute;top:0;bottom:0;left:12px;width:1px;background:var(--line-soft);pointer-events:none}.tl-milestone.first::before{top:50%}.agenda-row.last::before{bottom:50%}.tl-milestone-dot{position:relative;z-index:1;width:7px;height:7px;margin-left:9px;border-radius:50%;background:var(--muted)}.tl-milestone-label{color:var(--muted);font-size:11px;font-weight:600}.agenda-row{position:relative;display:grid;grid-template-columns:26px minmax(0,1fr) auto;align-items:center;gap:9px;width:100%;min-height:64px;padding:7px 0;border:0;border-bottom:1px solid var(--line-soft);background:transparent;color:var(--ink);text-align:left;cursor:pointer}.agenda-row:hover{background:var(--surface-soft)}.agenda-row.selected{background:var(--brand-soft)}.rail-track{display:flex;justify-content:center;align-self:stretch}.rail-node{position:relative;z-index:1;display:grid;width:22px;height:22px;place-items:center;align-self:center;border:1px solid var(--line);border-radius:50%;background:var(--surface);color:var(--muted)}.rail-node.selected{border-color:var(--brand);background:var(--brand);color:#fff}.row-copy{display:grid;gap:4px;min-width:0}.row-title{display:flex;align-items:flex-start;gap:7px;overflow:visible;font-size:13px;font-weight:600;line-height:1.35;white-space:normal}.row-title-text{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.company-mark{display:inline-grid;flex:0 0 auto;width:22px;height:22px;place-items:center;border-radius:50%;font-size:10px;font-weight:750}.tone-tencent{background:#111;color:#fff}.tone-byte{background:#eef1f2;color:#111}.tone-meituan{background:#f8d82b;color:#111}.tone-neutral{background:var(--surface-soft);border:1px solid var(--line);color:var(--copy)}.row-meta{display:flex;gap:8px;color:var(--copy);font-size:11px;font-variant-numeric:tabular-nums}.row-day{color:var(--muted)}.row-status{display:inline-flex;align-items:center;gap:3px;align-self:center;font-size:10px;white-space:nowrap}.status-green{color:var(--brand)}.status-amber{color:#b78216}.status-gray{color:var(--muted)}
.agenda-timeline.compact{margin-top:12px}.agenda-timeline.compact .agenda-row{grid-template-columns:12px minmax(0,1fr) auto;gap:7px;min-height:52px}.agenda-timeline.compact .agenda-row::before{display:none}.compact-dot{width:7px;height:7px;border-radius:50%;background:var(--muted)}.compact-dot.selected{background:var(--brand)}
:global(body.rail-resizing){cursor:col-resize;user-select:none}.empty-copy{margin:22px 0 0 22px;color:var(--copy);font-size:14px;line-height:1.6}
@media(max-width:1120px){.workspace-header{padding-left:34px;padding-right:30px}.detail-pane{padding-left:34px;padding-right:28px}.agenda-pane{padding-right:18px;padding-left:20px}.tool-list{grid-template-columns:repeat(3,minmax(0,1fr))}.tool-item:nth-child(3){border-right:0}.tool-item:nth-child(n+4){border-top:1px solid var(--line-soft)}}
@media(max-height:899px){.workspace-header{padding-top:34px;padding-bottom:20px}.workspace-motto{margin-top:9px;font-size:23px}.detail-pane{padding-top:25px;padding-bottom:20px}.agenda-pane{padding-top:25px}.next-meta{margin-top:18px}.next-actions{margin-top:18px}.context-rows{margin-top:18px}.action-hint{margin-top:16px}.recent-activity{margin-top:22px}.resume-strip{margin-top:20px}.tool-index{margin-top:18px}}
@media(min-width:761px) and (max-height:1100px){.workspace-header{padding-top:40px;padding-bottom:16px}.workspace-motto{margin-top:9px;font-size:22px}.detail-pane{padding-top:22px;padding-bottom:14px}.agenda-pane{padding-top:22px;padding-bottom:16px}.next-meta{margin-top:16px}.next-actions{margin-top:14px}.context-rows,.unlinked-context{margin-top:14px}.action-hint{margin-top:14px;padding-top:10px;padding-bottom:10px}.recent-activity{margin-top:16px}.review-row{padding-top:10px;padding-bottom:10px}.resume-strip{margin-top:14px;padding-top:11px;padding-bottom:11px}.tool-index{margin-top:13px}.tool-list{margin-top:9px}.tool-item{padding-top:9px;padding-bottom:9px}}
@media(min-width:761px) and (max-height:820px){.workspace-header{padding-top:27px;padding-bottom:12px}.workspace-date{font-size:11px}.workspace-motto{margin-top:6px;font-size:20px}.schedule-link{padding-top:6px;padding-bottom:6px}.detail-pane{padding-top:15px;padding-bottom:9px}.agenda-pane{padding-top:15px;padding-bottom:11px}.section-mark{min-height:32px}.accent-heading h1{margin-top:5px;font-size:22px}.next-meta{margin-top:10px}.next-date{font-size:20px}.next-role{margin-top:4px;font-size:12px}.next-actions{margin-top:10px}.primary-button{min-height:34px;padding-left:16px;padding-right:16px;font-size:12px}.context-rows,.unlinked-context{margin-top:10px}.context-row{min-height:37px}.unlinked-context{padding-top:9px;padding-bottom:9px}.fallback-identity{margin-top:11px}.action-hint{margin-top:9px;padding-top:7px;padding-bottom:7px}.recent-activity{margin-top:10px}.recent-activity .section-heading{margin-bottom:6px}.review-row{padding-top:7px;padding-bottom:7px}.review-summary{margin-top:2px;line-height:1.35}.empty-activity{padding-top:9px;padding-bottom:9px}.resume-strip{margin-top:9px;padding-top:8px;padding-bottom:8px}.resume-primary,.resume-action{min-height:28px}.tool-index{margin-top:8px;padding-bottom:4px}.tool-list{margin-top:6px}.tool-item{padding-top:6px;padding-bottom:6px}.tool-item small{font-size:10px}}
@media(max-width:760px){.workspace-header{padding:28px 20px 18px}.workspace-motto{font-size:20px}.schedule-link span{display:none}.master-grid{grid-template-columns:minmax(0,1fr)!important}.pane-divider{display:none}.agenda-pane{border-top:1px solid var(--line-soft);padding:22px 20px}.detail-pane{padding:26px 20px}.next-meta{margin-left:14px}.next-role,.next-actions,.context-rows,.recent-activity,.resume-strip,.tool-index,.action-hint{margin-left:14px}.next-date{font-size:22px}.next-interview{max-width:none}.resume-strip{display:grid}.resume-actions{justify-content:flex-start}.tool-list{grid-template-columns:repeat(2,minmax(0,1fr))}.tool-item:nth-child(3){border-right:1px solid var(--line-soft)}.tool-item:nth-child(n+3){border-top:1px solid var(--line-soft)}}

/* Source-aligned desktop composition: quiet header, split interview block, full-width activity/tools. */
.workbench-dashboard{overflow:hidden}
.workspace-header{padding:60px 52px 42px;border-bottom:0}
.workspace-date{font-size:12px}
.workspace-motto{margin-top:11px;font-size:22px}
.schedule-link{padding:8px 12px;font-size:12px}
.master-grid{flex:0 0 auto;min-height:0}
.detail-pane{padding:38px 52px 34px}
.agenda-pane{padding:38px 34px 34px}
.accent-heading{align-items:center}
.section-mark{min-height:29px}
.accent-heading .eyebrow{color:var(--ink);font-size:21px}
.next-identity{display:flex;align-items:center;gap:18px;margin:27px 0 0 17px}
.fallback-identity-row{display:flex;align-items:center;gap:18px;margin:27px 0 0 17px}
.fallback-identity-row .fallback-identity{margin:0;gap:3px}
.next-company-mark{width:56px;height:56px;font-size:27px}
.next-identity-copy{display:grid;gap:5px;min-width:0}
.next-identity-copy h1{margin:0;overflow:hidden;font-size:23px;font-weight:600;letter-spacing:-.03em;text-overflow:ellipsis;white-space:nowrap}
.next-identity-copy .next-role{margin:0;font-size:13px}
.next-meta{margin:24px 0 0 17px}
.next-date{font-size:25px}
.next-countdown{font-size:14px}
.next-actions{margin:24px 0 0 17px;gap:27px}
.primary-button{min-height:42px;padding:0 36px;font-size:13px}
.context-rows{margin:28px 0 0 17px;max-width:620px}
.context-row{min-height:43px;font-size:12px}
.action-hint{display:none}
.workspace-lower{border-top:1px solid var(--line-soft);padding:0 52px}
.workspace-lower .recent-activity{max-width:none;margin:0;padding:24px 0 0}
.workspace-lower .recent-activity .section-heading{margin-bottom:10px}
.review-row{grid-template-columns:46px 92px minmax(0,1fr) auto;gap:14px;padding:17px 0;border-top:0;border-bottom:1px solid var(--line-soft)}
.review-icon{display:grid;width:42px;height:42px;place-items:center;border:1px solid var(--line);border-radius:50%;color:var(--ink)}
.review-date{align-self:center;font-size:12px}
.review-title{font-size:14px}
.review-summary{margin-top:5px;font-size:12px;line-height:1.5;-webkit-line-clamp:2}
.workspace-lower .tool-index{max-width:none;margin:25px 0 0;padding-bottom:0}
.workspace-lower .tool-list{margin-top:14px}
.workspace-lower .tool-item{padding-top:16px;padding-bottom:16px}
.workspace-lower .tool-item strong{font-size:13px}
.workspace-lower .tool-item small{font-size:11px}
.workspace-footer{display:flex;align-items:center;gap:9px;margin-top:22px;padding:18px 0;border-top:1px solid var(--line-soft);color:var(--copy)}
.footer-leading{display:grid;place-items:center;color:var(--ink)}
.workspace-footer .hint-label{color:var(--copy);font-size:12px}
.workspace-footer .hint-copy{font-size:12px}
.workspace-footer .quiet-action{margin-left:auto}
.agenda-timeline{margin-top:15px}
.agenda-group+.agenda-group{border-top:1px solid var(--line-soft)}
.agenda-row{grid-template-columns:52px minmax(0,1fr) auto;min-height:78px;padding:10px 0;gap:12px}
.agenda-row.selected{background:transparent}
.agenda-company{display:flex;align-items:center;justify-content:center}
.agenda-company .company-mark{width:44px;height:44px;font-size:20px}
.row-copy{gap:5px}
.row-title{font-size:14px;line-height:1.35}
.row-meta{font-size:12px}
.row-status{font-size:11px}

@media(min-width:761px) and (max-height:820px){
  .workspace-header{padding:25px 34px 16px}
  .workspace-date{font-size:11px}
  .workspace-motto{margin-top:6px;font-size:20px}
  .schedule-link{padding-top:6px;padding-bottom:6px}
  .detail-pane{padding:16px 34px 14px}
  .agenda-pane{padding:16px 26px 14px}
  .section-mark{min-height:25px}
  .accent-heading .eyebrow{font-size:18px}
  .next-identity{gap:13px;margin-top:13px;margin-left:12px}
  .fallback-identity-row{gap:13px;margin-top:13px;margin-left:12px}
  .next-company-mark{width:44px;height:44px;font-size:21px}
  .next-identity-copy h1{font-size:20px}
  .next-identity-copy .next-role{font-size:11px}
  .next-meta{margin:12px 0 0 12px}
  .next-date{font-size:20px}
  .next-actions{margin:12px 0 0 12px;gap:18px}
  .primary-button{min-height:34px;padding-left:22px;padding-right:22px;font-size:12px}
  .context-rows{margin:12px 0 0 12px}
  .context-row{min-height:35px}
  .workspace-lower{padding:0 34px}
  .workspace-lower .recent-activity{padding-top:13px}
  .workspace-lower .recent-activity .section-heading{margin-bottom:5px}
  .review-row{padding-top:8px;padding-bottom:8px}
  .review-icon{width:34px;height:34px}
  .review-title{font-size:12px}
  .review-summary{margin-top:2px;font-size:11px;line-height:1.35}
  .workspace-lower .tool-index{margin-top:12px}
  .workspace-lower .tool-list{margin-top:6px}
  .workspace-lower .tool-item{padding-top:8px;padding-bottom:8px}
  .workspace-footer{margin-top:10px;padding-top:10px;padding-bottom:10px}
  .agenda-timeline{margin-top:8px}
  .agenda-row{min-height:57px;padding-top:6px;padding-bottom:6px}
  .agenda-company .company-mark{width:34px;height:34px;font-size:16px}
  .row-title{font-size:12px}
  .row-meta{font-size:10px}
  .row-status{font-size:10px}
}

/* Final source-fit: preserve the reference proportions instead of scaling each block independently. */
.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
.workspace-header{padding:52px 28px 21px}
.workspace-date{font-size:15px;line-height:1.2}
.workspace-motto{margin-top:41px;font-size:25px;font-weight:520;line-height:1.3}
.schedule-link{margin-top:-2px;padding:9px 15px;font-size:13px}
.detail-pane{padding:37px 28px 30px}
.next-interview{max-width:none}
.accent-heading .eyebrow{font-size:20px}
.section-mark{width:4px;min-height:29px;margin-right:13px}
.next-identity,.fallback-identity-row{margin-top:27px;margin-left:17px}
.next-company-mark{width:58px;height:58px;font-size:28px}
.next-identity-copy h1{font-size:21px}
.next-identity-copy .next-role{font-size:13px}
.next-meta{margin-top:28px}
.next-date{gap:16px;font-size:26px}
.next-day{font-size:1em;color:var(--ink);font-weight:500}
.next-countdown{margin-left:18px;font-size:14px}
.next-actions{margin-top:22px;gap:36px}
.next-actions .primary-button{width:338px;min-height:54px;padding:0 24px;font-size:16px}
.next-actions .text-action{gap:10px;color:var(--ink);font-size:15px}
.context-rows{max-width:590px;margin-top:28px}
.context-row{grid-template-columns:22px 92px minmax(0,1fr) auto;min-height:42px;font-size:13px}
.context-label{color:var(--ink);font-size:12px}
.context-value{font-size:12px;font-weight:450}
.context-action{font-size:12px}
.master-grid{min-height:453px}
.agenda-pane{padding:38px 34px 25px}
.agenda-header .eyebrow{font-size:20px}
.agenda-timeline{margin-top:19px}
.agenda-row{grid-template-columns:58px minmax(0,1fr) auto;min-height:96px;padding:12px 0}
.agenda-company .company-mark{width:48px;height:48px;font-size:21px}
.row-title{font-size:15px}
.row-meta{font-size:13px}
.row-status{gap:8px;font-size:12px}
.agenda-all-link{margin-top:17px;color:var(--ink);font-size:12px}
.agenda-empty{min-height:244px;margin-top:26px}
.workspace-lower{padding:0 28px}
.workspace-lower .recent-activity{padding-top:24px}
.workspace-lower .recent-activity .section-heading{margin-bottom:14px}
.workspace-lower .recent-activity .eyebrow,.workspace-lower .tool-index>.eyebrow{font-size:19px}
.workspace-lower .recent-activity>.section-heading>.quiet-action{display:none}
.review-row{grid-template-columns:50px 250px minmax(0,1fr) auto;align-items:center;gap:16px;min-height:94px;padding:7px 0}
.review-icon{width:46px;height:46px}
.review-copy{display:grid;gap:4px}
.review-title{font-size:14px}
.review-date{font-size:12px}
.review-summary{margin:0;font-size:13px;line-height:1.55;-webkit-line-clamp:2}
.activity-report{font-size:13px}
.workspace-lower .tool-index{margin-top:19px}
.workspace-lower .tool-list{min-height:82px;margin-top:11px}
.workspace-lower .tool-item{gap:14px;padding:15px 18px}
.workspace-lower .tool-item:first-child{padding-left:12px}
.workspace-lower .tool-item strong{font-size:15px}
.workspace-lower .tool-item small{font-size:12px}
.workspace-footer{margin-top:0;padding:19px 0}
.workspace-footer .hint-label,.workspace-footer .hint-copy{font-size:12px}

.workspace-date{transform:translateY(-7px);font-size:13px}
.schedule-link{min-width:108px;justify-content:center;padding:8px 10px;border-radius:6px}
.next-actions .primary-button{width:274px;min-height:46px;font-size:15px}
.company-mark{position:relative;overflow:hidden}
.company-mark>span{position:relative;z-index:0}
.company-mark>img{position:absolute;z-index:1;inset:0;width:100%;height:100%;object-fit:contain;background:#fff}
.agenda-row{grid-template-columns:58px minmax(0,1fr) 18px}
.row-arrow{justify-self:end;color:var(--copy)}
.review-icon.mode-ROLE_BASED{color:#111}
.review-icon.mode-KNOWLEDGE_TRAINING{color:#7c3aed}
.review-icon.mode-EXPERIENCE_SIMULATION{color:#2563eb}
.workspace-footer .quiet-action{margin-left:0}
.weekly-practice{margin-left:auto;color:var(--copy);font-size:12px;white-space:nowrap}

/* Empty states stay intentionally quiet: no target or resume bindings are implied
   until a real interview arrangement exists. */
.empty-interview-state{margin:28px 0 0 17px;max-width:520px}
.empty-interview-state h1{margin:0;color:var(--ink);font-size:22px;font-weight:550;letter-spacing:-.02em}
.empty-interview-state .empty-copy{margin:10px 0 0;color:var(--copy);font-size:13px;line-height:1.55}
.empty-interview-state .primary-button{margin-top:18px;min-height:38px;width:auto;padding:0 18px;font-size:13px}
.agenda-empty-state{display:flex;align-items:center;gap:18px;margin-top:28px}
.agenda-empty-state .agenda-empty{min-height:0;margin:0;color:var(--copy);font-size:13px}
.agenda-empty-state .agenda-all-link{margin:0;color:var(--ink);font-size:12px}
.workspace-date,.next-role,.context-label,.context-action,.row-meta,.review-date{font-family:-apple-system,BlinkMacSystemFont,"SF Pro Text","PingFang SC","Microsoft YaHei",sans-serif}
.schedule-link{min-width:104px;height:36px;padding:0 10px;border-radius:6px;font-size:12px}
.next-actions .primary-button{width:260px;min-height:44px;font-size:14px}
.next-actions{justify-content:flex-start}
.workspace-footer{margin-top:12px;padding-top:12px;padding-bottom:10px}
.pane-divider{cursor:default;touch-action:auto}
.pane-divider:hover,.pane-divider:focus-visible,.pane-divider.dragging{background:var(--line-soft);box-shadow:none}

/* Secondary actions should recede behind the primary preparation action. */
.schedule-link,
.next-actions .text-action,
.agenda-all-link,
.recent-activity .quiet-action,
.activity-report,
.context-action,
.unlinked-context .text-action,
.workspace-footer .quiet-action{
  color:var(--muted);
  font-size:12px !important;
  font-weight:500;
}
.schedule-link{border-color:var(--line);}
.schedule-link .el-icon,
.next-actions .text-action .el-icon,
.agenda-all-link .el-icon,
.recent-activity .quiet-action .el-icon,
.activity-report .el-icon,
.context-action .el-icon,
.unlinked-context .text-action .el-icon,
.workspace-footer .quiet-action .el-icon{color:var(--muted);}
.schedule-link:hover,
.next-actions .text-action:hover,
.agenda-all-link:hover,
.recent-activity .quiet-action:hover,
.activity-report:hover,
.context-action:hover,
.unlinked-context .text-action:hover,
.workspace-footer .quiet-action:hover{color:var(--copy);}

@media(min-width:761px) and (max-height:900px){
  .workspace-header{padding-top:32px;padding-bottom:18px}
  .workspace-motto{margin-top:13px;font-size:21px}
  .master-grid{min-height:342px}
  .detail-pane,.agenda-pane{padding-top:24px;padding-bottom:18px}
  .next-identity,.fallback-identity-row{margin-top:16px}
  .next-meta{margin-top:16px}
  .next-actions{margin-top:14px}
  .next-actions .primary-button{min-height:44px}
  .context-rows{margin-top:16px}
  .agenda-row{min-height:72px}
  .agenda-empty{min-height:190px}
  .workspace-lower .recent-activity{padding-top:15px}
  .review-row{min-height:62px}
  .workspace-lower .tool-index{margin-top:12px}
  .workspace-lower .tool-list{min-height:74px;margin-top:6px}
  .workspace-lower .tool-item{padding-top:9px;padding-bottom:9px}
  .workspace-footer{padding:12px 0}
}
@media(min-width:761px) and (max-height:760px){
  .master-grid{min-height:295px}
  .next-identity,.fallback-identity-row{margin-top:9px}
  .next-company-mark{width:44px;height:44px;font-size:21px}
  .next-meta{margin-top:8px}
  .next-date{font-size:21px}
  .next-actions{margin-top:8px}
  .next-actions .primary-button{min-height:36px}
  .context-rows{margin-top:8px}
  .context-row{min-height:34px}
  .agenda-row{min-height:58px}
  .agenda-empty{min-height:145px}
  .review-row{min-height:52px}
  .workspace-lower .tool-list{min-height:62px}
  .workspace-lower .tool-item{padding-top:5px;padding-bottom:5px}
  .workspace-footer{padding:8px 0}
}

@media(min-width:761px) and (max-height:900px){
  .empty-interview-state{margin-top:22px}
  .empty-interview-state .primary-button{margin-top:14px}
  .agenda-empty-state{margin-top:20px}
}

/* Final hierarchy pass: the date and motto are one quiet header group, while
   the two interview area labels remain functional section markers. */
.workspace-header{padding:32px 28px 18px}
.workspace-date{margin:0;transform:none;font-size:12px;line-height:1.25}
.workspace-motto{margin-top:6px;font-size:18px;font-weight:500;line-height:1.4}
.accent-heading .eyebrow,.agenda-header .eyebrow{color:var(--copy);font-size:13px;font-weight:600}
.accent-heading .section-mark{min-height:24px;margin-top:0}

/* Unified desktop composition. Keep one grid and one spacing rhythm for the
   whole workspace instead of letting individual blocks scale independently. */
.workbench-dashboard{
  --workspace-gutter:clamp(32px,4.4vw,64px);
  display:flex;
  flex-direction:column;
  min-height:100%;
  overflow:hidden;
  background:var(--canvas);
  color:var(--ink);
}
.workspace-header{
  flex:0 0 auto;
  box-sizing:border-box;
  width:100%;
  padding:clamp(34px,5.2vh,58px) var(--workspace-gutter) 24px;
  border:0;
}
.workspace-date{margin:0;transform:none;font-size:12px;line-height:1.35;color:var(--copy)}
.workspace-motto{margin:7px 0 0;font-size:19px;font-weight:500;line-height:1.35;letter-spacing:-.025em;color:var(--ink)}
.schedule-link{align-self:flex-start;margin-top:0;height:34px;min-width:104px;padding:0 11px;border:1px solid var(--line);border-radius:7px;color:var(--muted);font-size:12px}
.schedule-link .el-icon{color:var(--muted)}
.master-grid{
  flex:0 0 auto;
  grid-template-columns:minmax(0,1fr) 1px minmax(420px,.82fr)!important;
  min-height:330px!important;
  border-top:1px solid var(--line-soft);
  border-bottom:1px solid var(--line-soft);
}
.detail-pane{box-sizing:border-box;min-width:0;padding:30px var(--workspace-gutter) 28px}
.agenda-pane{box-sizing:border-box;min-width:0;padding:30px var(--workspace-gutter) 25px 30px}
.pane-divider{width:1px;background:var(--line-soft);cursor:default;touch-action:auto}
.pane-divider::before{display:none}
.pane-divider:hover,.pane-divider:focus-visible,.pane-divider.dragging{background:var(--line-soft);box-shadow:none}
.next-interview{max-width:700px}
.accent-heading{align-items:center;gap:12px}
.accent-heading .section-mark{width:3px;min-height:26px;margin:0;background:var(--brand)}
.accent-heading .eyebrow,.agenda-header .eyebrow{color:var(--copy);font-size:13px;font-weight:600;letter-spacing:0}
.next-identity{margin:23px 0 0 15px;gap:16px}
.next-company-mark{width:52px;height:52px;font-size:24px}
.next-identity-copy{gap:4px}
.next-identity-copy h1{font-size:23px;line-height:1.25}
.next-identity-copy .next-role{font-size:13px}
.next-meta{margin:18px 0 0 15px;gap:12px}
.next-date{font-size:24px;gap:10px}
.next-day{font-size:.62em;color:var(--copy)}
.next-countdown{margin-left:4px;font-size:12px;font-weight:600}
.next-actions{margin:16px 0 0 15px;gap:24px}
.next-actions .primary-button{width:260px;min-height:40px;padding:0 22px;font-size:13px}
.next-actions .text-action{gap:6px;color:var(--muted);font-size:12px}
.next-actions .text-action .el-icon{color:var(--muted)}
.context-rows{max-width:620px;margin:18px 0 0 15px}
.context-row{grid-template-columns:22px 78px minmax(0,1fr) auto;min-height:38px;font-size:12px}
.context-label{font-size:11px;color:var(--muted)}
.context-value{font-size:12px;font-weight:500}
.context-action{font-size:12px;color:var(--muted)}
.agenda-header{align-items:center}
.agenda-timeline{margin-top:12px}
.agenda-row{grid-template-columns:48px minmax(0,1fr) 16px;min-height:70px;padding:9px 0;gap:12px}
.agenda-company .company-mark{width:40px;height:40px;font-size:18px}
.row-copy{gap:4px}
.row-title{font-size:13px;line-height:1.35}
.row-meta{font-size:11px;color:var(--copy)}
.row-arrow{color:var(--muted)}
.agenda-all-link{margin-top:12px;color:var(--muted);font-size:12px}
.agenda-all-link .el-icon{color:var(--muted)}
.agenda-empty{min-height:170px;margin-top:22px;font-size:13px;color:var(--muted)}
.workspace-lower{flex:0 0 auto;box-sizing:border-box;width:100%;padding:0 var(--workspace-gutter);border:0}
.workspace-lower .recent-activity{max-width:none;margin:0;padding:20px 0 0}
.workspace-lower .recent-activity .section-heading{margin-bottom:8px}
.workspace-lower .recent-activity .eyebrow,.workspace-lower .tool-index>.eyebrow{font-size:15px;font-weight:650;color:var(--ink)}
.review-row{grid-template-columns:48px 180px minmax(0,1fr) auto;gap:14px;min-height:70px;padding:10px 0;border-top:1px solid var(--line-soft);border-bottom:0}
.review-icon{width:40px;height:40px}
.review-title{font-size:13px}
.review-date{font-size:11px}
.review-summary{margin-top:3px;font-size:12px;line-height:1.45;-webkit-line-clamp:1}
.activity-report{font-size:12px;color:var(--muted)}
.workspace-lower .tool-index{max-width:none;margin:16px 0 0;padding:0}
.workspace-lower .tool-list{margin-top:8px;min-height:70px}
.workspace-lower .tool-item{padding:12px 16px 12px 0;gap:10px}
.workspace-lower .tool-item:not(:first-child){padding-left:16px}
.workspace-lower .tool-item>.el-icon{font-size:28px!important;color:var(--copy)}
.workspace-lower .tool-item strong{font-size:13px}
.workspace-lower .tool-item small{font-size:11px}
.workspace-footer{margin:12px 0 0;padding:11px 0;border-top:1px solid var(--line-soft)}
.workspace-footer .hint-label,.workspace-footer .hint-copy,.workspace-footer .quiet-action{font-size:12px}
.weekly-practice{font-size:12px;color:var(--muted)}

/* Final line discipline: use whitespace for grouping and reserve rules for
   the few places where a list genuinely needs a boundary. */
.master-grid{border-top:0;border-bottom:0}
.pane-divider{background:transparent}
.pane-divider:hover,.pane-divider:focus-visible,.pane-divider.dragging{background:transparent;box-shadow:none}
.agenda-group+.agenda-group{border-top:0}
.agenda-row{border-bottom:0}
.agenda-row::before{display:none}
.agenda-group+.agenda-group .row-copy{border-top:1px solid var(--line-soft);padding-top:9px}
.agenda-group+.agenda-group .agenda-row{padding-top:0}
.workspace-lower .section-heading + .review-row{border-top:0}
.workspace-lower .tool-list{border-top:0;border-bottom:0}
.workspace-footer{margin-top:28px;padding-top:15px}

@media (min-width:761px) and (max-height:820px){
  .workspace-header{padding-top:26px;padding-bottom:17px}
  .workspace-motto{margin-top:5px;font-size:18px}
  .master-grid{min-height:286px!important}
  .detail-pane,.agenda-pane{padding-top:22px;padding-bottom:18px}
  .next-identity{margin-top:15px}
  .next-company-mark{width:46px;height:46px;font-size:21px}
  .next-identity-copy h1{font-size:20px}
  .next-meta{margin-top:11px}
  .next-date{font-size:21px}
  .next-actions{margin-top:10px}
  .context-rows{margin-top:10px}
  .agenda-row{min-height:56px}
  .workspace-lower .recent-activity{padding-top:13px}
  .review-row{min-height:56px;padding-top:7px;padding-bottom:7px}
  .workspace-lower .tool-index{margin-top:10px}
  .workspace-lower .tool-list{min-height:58px;margin-top:5px}
  .workspace-lower .tool-item{padding-top:7px;padding-bottom:7px}
}
.company-mark{overflow:visible}
.company-mark>img{inset:2px;width:calc(100% - 4px);height:calc(100% - 4px);background:transparent;border-radius:inherit}
.workspace-lower .tool-item{display:grid;grid-template-columns:32px minmax(0,1fr);align-items:center;min-height:70px;box-sizing:border-box}
.workspace-lower .tool-item>.el-icon{display:grid;width:32px;height:32px;place-items:center;align-self:center;justify-self:center;flex:none}
.workspace-lower .tool-item>span{align-self:center;min-width:0;line-height:1.25}
.workspace-lower .tool-item strong,.workspace-lower .tool-item small{display:block;line-height:1.3}
.workspace-lower .tool-list--bounded{border-left:1px solid var(--line-soft);border-right:1px solid var(--line-soft)}
.workspace-lower .tool-list--bounded .tool-item{border-right:1px solid var(--line-soft)}
.workspace-lower .tool-list--bounded .tool-item:last-child{border-right:0}
.workspace-lower .tool-item--centered{grid-template-columns:32px max-content;justify-content:center}
.workspace-lower .tool-item--centered>span{max-width:100%;overflow:hidden}
.company-mark.has-logo{background:transparent;border:0;border-radius:0;color:inherit}
.company-mark.has-logo>img{inset:0;width:100%;height:100%;border-radius:0}
.next-actions--spaced + .context-rows--spaced,.next-actions--spaced + .unlinked-context.context-rows--spaced{margin-top:22px}
.agenda-row .row-target{display:block;max-width:100%;overflow:hidden;color:var(--muted);font-size:10.5px;font-weight:500;line-height:1.25;text-overflow:ellipsis;white-space:nowrap}
</style>
