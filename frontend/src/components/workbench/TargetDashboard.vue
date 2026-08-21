<template>
  <section data-test="target-dashboard" class="workbench-dashboard">
    <p v-if="materialError" class="material-error">{{ materialError }}</p>

    <!-- 一级空间：Agenda Pane | 4px 命中区 Divider（可拖拽） | Detail Pane（Dock 在 shell 层） -->
    <div class="master-grid" :style="masterGridStyle">
      <aside data-test="agenda-pane" class="agenda-pane">
        <header class="rail-header">
          <h2 class="rail-title">接下来</h2>
          <span v-if="agendaEvents.length" class="rail-count">{{ agendaEvents.length }} 场安排</span>
        </header>

        <p v-if="!agendaEvents.length" class="rail-empty">近期没有安排</p>
        <!-- 事件时间轴：日标签作为里程碑节点，事件为类型图标节点，轴线贯穿全部组；compact 下改为行内联时间 -->
        <div class="agenda-timeline" :class="{ compact }">
          <div v-for="(group, groupIndex) in groupedEvents" :key="groupIndex" class="agenda-group" data-test="agenda-group">
            <div v-if="!compact" class="tl-milestone" :class="{ first: groupIndex === 0 }">
              <span class="rail-track" aria-hidden="true"><span class="tl-milestone-dot"></span></span>
              <span class="tl-milestone-label">{{ group.label }}</span>
            </div>
            <button
              v-for="(event, index) in group.events"
              :key="event.id"
              type="button"
              data-test="agenda-row"
              class="agenda-row"
              :class="{
                selected: event.id === selectedEventId,
                compact,
                last: groupIndex === groupedEvents.length - 1 && index === group.events.length - 1,
              }"
              :aria-pressed="event.id === selectedEventId"
              @click="handlePick(event.id)"
            >
              <span class="rail-track" aria-hidden="true">
                <span v-if="compact" class="compact-dot" :class="{ selected: event.id === selectedEventId }"></span>
                <span v-else class="rail-node" :class="{ selected: event.id === selectedEventId }"><el-icon :size="13"><component :is="eventTypeIcon[event.eventType]" /></el-icon></span>
              </span>
              <span class="row-copy">
                <span class="row-title"><span v-if="compact" class="row-inline-time">{{ inlineTime(event) }}</span>{{ rowTitle(event) }}</span>
                <span v-if="!compact" class="row-meta">{{ rowMeta(event) }}</span>
              </span>
            </button>
          </div>
        </div>
      </aside>

      <div
        data-test="pane-separator"
        class="pane-divider"
        :class="{ dragging }"
        role="separator"
        aria-orientation="vertical"
        aria-label="调整「接下来」面板宽度"
        aria-valuemin="270"
        aria-valuemax="420"
        :aria-valuenow="railWidth"
        tabindex="0"
        @pointerdown="startDrag"
        @dblclick="railWidth = DEFAULT_RAIL_WIDTH"
        @keydown.left.prevent="nudgeRail(-RAIL_NUDGE)"
        @keydown.right.prevent="nudgeRail(RAIL_NUDGE)"
      ></div>

      <main data-test="detail-pane" class="detail-pane">
        <template v-if="selectedEvent">
          <header class="detail-header">
            <p class="detail-relative">{{ selectedEvent.relativeLabel }}</p>
            <p class="detail-time" data-test="detail-time">{{ selectedEvent.timeLabel }}</p>
            <p class="detail-title" data-test="detail-title">{{ detailTitle }}</p>
            <p v-if="!showLinkedBody && selectedEvent.roleLabel" class="detail-role">{{ selectedEvent.roleLabel }}</p>
            <p v-if="selectedEvent.countdownLabel" class="detail-countdown">{{ selectedEvent.countdownLabel }}</p>
          </header>

          <!-- 已关联目标：关联目标行 → 准备时间轴 → 下一步（纵向顺延，不拆列） -->
          <div v-if="showLinkedBody" class="detail-body">
            <section class="linked-target" data-test="detail-linked-target">
              <span class="linked-target-label">关联目标</span>
              <span class="linked-target-row">
                <span class="linked-target-copy">{{ linkedTargetCopy() }}</span>
                <button type="button" class="linked-target-action" data-test="linked-target-open" @click="$emit('action', 'open-target', detail.targetId ?? undefined)">目标详情<el-icon :size="13"><ArrowRight /></el-icon></button>
              </span>
            </section>

            <section class="detail-section">
              <p class="section-label">准备</p>
              <div class="object-list">
                <div
                  v-for="(row, index) in detail.readiness"
                  :key="row.key"
                  class="object-row"
                  :class="{ last: index === detail.readiness.length - 1 }"
                  :data-test="`readiness-${row.key}`"
                >
                  <span class="tl-track" aria-hidden="true">
                    <span class="tl-dot" :class="{ ready: row.ready }"><el-icon :size="14"><component :is="rowIcon[row.key] ?? Document" /></el-icon></span>
                  </span>
                  <span class="object-copy">
                    <span class="object-name">{{ row.label }}</span>
                    <span class="object-meta">{{ row.meta }}<template v-if="row.subMeta"> · {{ row.subMeta }}</template></span>
                  </span>
                  <button type="button" class="object-action" @click="$emit('action', row.action, detail.targetId ?? undefined)">{{ row.actionLabel }}<el-icon :size="12"><ArrowRight /></el-icon></button>
                </div>
              </div>
            </section>

            <section v-if="detail.nextAction" class="detail-section" data-test="detail-next">
              <p class="section-label">下一步</p>
              <p class="next-text">{{ detail.nextAction.text }}</p>
              <div class="next-actions">
                <button type="button" class="primary-button" data-test="next-button" @click="$emit('action', detail.nextAction!.action, detail.targetId ?? undefined)">
                  {{ detail.nextAction.button }}
                </button>
                <span v-if="detail.nextAction.hint" class="next-hint">{{ detail.nextAction.hint }}</span>
              </div>
            </section>
          </div>

          <!-- 未关联目标：只展示事件事实与关联入口，不虚构准备状态 -->
          <div v-else class="detail-body" data-test="detail-unlinked">
            <p class="unlinked-line">未关联求职目标</p>
            <div class="unlinked-actions">
              <button type="button" data-test="link-target" class="unlinked-action" @click="$emit('action', 'link-target', Number(selectedEvent.id))">关联目标<el-icon :size="13"><ArrowRight /></el-icon></button>
              <button type="button" data-test="view-schedule" class="unlinked-action" @click="$emit('action', 'open-schedule')">查看日程<el-icon :size="13"><ArrowRight /></el-icon></button>
            </div>
          </div>
        </template>

        <!-- 没有选中安排：目标回退或完全空态 -->
        <template v-else>
          <header class="detail-header">
            <p class="detail-relative" data-test="detail-empty">{{ detail.note }}</p>
          </header>
          <div v-if="detail.kind === 'target'" class="detail-body">
            <section class="identity-block" data-test="detail-identity">
              <p class="identity-company">{{ detail.companyLabel || detail.targetName }}</p>
              <p class="identity-role">{{ detail.roleLabel }}</p>
            </section>
            <section class="detail-section">
              <p class="section-label">准备</p>
              <div class="object-list">
                <div
                  v-for="(row, index) in detail.readiness"
                  :key="row.key"
                  class="object-row"
                  :class="{ last: index === detail.readiness.length - 1 }"
                  :data-test="`readiness-${row.key}`"
                >
                  <span class="tl-track" aria-hidden="true">
                    <span class="tl-dot" :class="{ ready: row.ready }"><el-icon :size="14"><component :is="rowIcon[row.key] ?? Document" /></el-icon></span>
                  </span>
                  <span class="object-copy">
                    <span class="object-name">{{ row.label }}</span>
                    <span class="object-meta">{{ row.meta }}<template v-if="row.subMeta"> · {{ row.subMeta }}</template></span>
                  </span>
                  <button type="button" class="object-action" @click="$emit('action', row.action, detail.targetId ?? undefined)">{{ row.actionLabel }}<el-icon :size="12"><ArrowRight /></el-icon></button>
                </div>
              </div>
            </section>
            <section v-if="detail.nextAction" class="detail-section" data-test="detail-next">
              <p class="section-label">下一步</p>
              <p class="next-text">{{ detail.nextAction.text }}</p>
              <div class="next-actions">
                <button type="button" class="primary-button" data-test="next-button" @click="$emit('action', detail.nextAction!.action, detail.targetId ?? undefined)">
                  {{ detail.nextAction.button }}
                </button>
                <span v-if="detail.nextAction.hint" class="next-hint">{{ detail.nextAction.hint }}</span>
              </div>
            </section>
            <button type="button" class="target-detail" data-test="target-detail" @click="$emit('action', 'open-target', detail.targetId ?? undefined)">目标详情<el-icon :size="13"><ArrowRight /></el-icon></button>
          </div>
          <div v-else class="detail-body">
            <button type="button" class="primary-button" data-test="detail-empty-action" @click="$emit('action', 'add-job')">录入岗位<el-icon :size="14"><ArrowRight /></el-icon></button>
          </div>
        </template>

        <!-- 最近活动：Detail Workspace 底部 -->
        <section data-test="recent-activity" class="activity-zone">
          <h2 class="zone-title">最近活动</h2>
          <template v-if="recentActivity.length">
            <div v-for="item in recentActivity" :key="item.id" class="activity-row" data-test="activity-row">
              <span class="activity-date">{{ item.dateLabel }}</span>
              <div class="activity-copy">
                <p class="activity-title">{{ item.companyLabel ? `${item.companyLabel} · ` : '' }}{{ item.title }}</p>
                <p class="activity-summary">{{ item.summary }}</p>
              </div>
              <button type="button" class="activity-report" @click="$emit('action', 'open-feedback', item.targetId ?? undefined)">查看完整报告<el-icon :size="12"><ArrowRight /></el-icon></button>
            </div>
          </template>
          <div v-else class="activity-empty">
            <p>还没有模拟面试记录</p>
            <p>完成第一次模拟后会在这里保留反馈。</p>
          </div>
        </section>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowRight, Calendar, ChatDotRound, Clock, Document, EditPen } from '@element-plus/icons-vue'

export interface AgendaEventView {
  id: string
  title: string
  eventType: string
  timeLabel: string
  relativeLabel: string
  dayLabel: string
  companyLabel: string
  roleLabel: string
  countdownLabel: string
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
}
export interface RecentActivityItem { id: number; dateLabel: string; companyLabel: string; title: string; summary: string; targetId: number | null }
export type TargetDashboardAction = 'add-job' | 'select-resume' | 'open-editor' | 'open-interview' | 'open-schedule' | 'open-feedback' | 'open-target' | 'link-target'

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
const emit = defineEmits<{ (event: 'select-event', id: string): void; (event: 'action', action: TargetDashboardAction, targetId?: number): void }>()

// ── Split Pane：rail 宽度由用户拖拽/键盘微调，<290px 进入 compact 行内联时间；无新依赖 ──
const DEFAULT_RAIL_WIDTH = 330
const MIN_RAIL_WIDTH = 270
const MAX_RAIL_WIDTH = 420
const COMPACT_RAIL_WIDTH = 290
const RAIL_NUDGE = 8
const railWidth = ref(DEFAULT_RAIL_WIDTH)
const dragging = ref(false)
const compact = computed(() => railWidth.value < COMPACT_RAIL_WIDTH)
const masterGridStyle = computed(() => ({ gridTemplateColumns: `${railWidth.value}px 4px minmax(0, 1fr)` }))

function clampRail(value: number): number {
  return Math.min(MAX_RAIL_WIDTH, Math.max(MIN_RAIL_WIDTH, value))
}
function nudgeRail(delta: number) {
  railWidth.value = clampRail(railWidth.value + delta)
}
function startDrag(event: PointerEvent) {
  const divider = event.currentTarget as HTMLElement | null
  if (!divider || event.button !== 0) return
  event.preventDefault()
  divider.setPointerCapture(event.pointerId)
  dragging.value = true
  document.body.classList.add('rail-resizing')
  const startX = event.clientX
  const startWidth = railWidth.value
  const onMove = (move: PointerEvent) => {
    railWidth.value = clampRail(startWidth + (move.clientX - startX))
  }
  const onEnd = () => {
    divider.removeEventListener('pointermove', onMove)
    divider.removeEventListener('pointerup', onEnd)
    divider.removeEventListener('pointercancel', onEnd)
    if (divider.hasPointerCapture(event.pointerId)) divider.releasePointerCapture(event.pointerId)
    dragging.value = false
    document.body.classList.remove('rail-resizing')
  }
  divider.addEventListener('pointermove', onMove)
  divider.addEventListener('pointerup', onEnd)
  divider.addEventListener('pointercancel', onEnd)
}
// compact 行内联时间：今天只有时刻，其余日期按「M月D日」，避免跨月歧义。
function inlineTime(event: AgendaEventView): string {
  return event.dayLabel || event.timeLabel
}
// 关联目标行：优先 公司 · 岗位；公司为空时回退目标名，若目标名已含岗位则不再重复追加。
function linkedTargetCopy(): string {
  if (props.detail.companyLabel) {
    return props.detail.roleLabel ? `${props.detail.companyLabel} · ${props.detail.roleLabel}` : props.detail.companyLabel
  }
  if (props.detail.roleLabel && props.detail.targetName && !props.detail.targetName.includes(props.detail.roleLabel)) {
    return `${props.detail.targetName} · ${props.detail.roleLabel}`
  }
  return props.detail.targetName || '求职目标'
}

// 选中行 = 最近 upcoming；事件已按时间升序传入，按 relativeLabel 连续分组。
const selectedEvent = computed(() => props.agendaEvents.find((event) => event.id === props.selectedEventId) ?? props.agendaEvents[0] ?? null)
const groupedEvents = computed(() => {
  const groups: { label: string; events: AgendaEventView[] }[] = []
  for (const event of props.agendaEvents) {
    const last = groups[groups.length - 1]
    if (last && last.label === event.relativeLabel) last.events.push(event)
    else groups.push({ label: event.relativeLabel, events: [event] })
  }
  return groups
})
const detailTitle = computed(() => {
  const event = selectedEvent.value
  if (!event) return ''
  return event.companyLabel ? `${event.companyLabel} · ${event.title}` : event.title
})
const showLinkedBody = computed(() => props.detail.targetLinked || props.detail.kind === 'target')

function rowTitle(event: AgendaEventView): string {
  return event.companyLabel ? `${event.companyLabel} · ${event.title}` : event.title
}
// 行 meta 只保留时间：角色信息已在 Detail 头部呈现，避免「岗位词」在页面内重复。
function rowMeta(event: AgendaEventView): string {
  return event.timeLabel
}
function handlePick(id: string) {
  emit('select-event', id)
}

const rowIcon: Record<string, unknown> = { resume: Document, mock: ChatDotRound, schedule: Calendar }
// 事件类型 → 时间轴节点图标：面试=对话、笔试=笔、跟进=时钟
const eventTypeIcon: Record<string, unknown> = { interview: ChatDotRound, exam: EditPen, followup: Clock }
</script>

<style scoped>
.workbench-dashboard{
  /* 首页色板：scope 到首页，不影响其他页面；dark 只换语义 token */
  --canvas:#F5F5F2;--surface:#FFFFFF;--surface-subtle:#F9F9F7;--surface-hover:#F1F1EE;
  --ink:#171717;--copy:#6E6E6A;--muted:#989893;
  --line-subtle:rgba(20,20,20,.065);--line:rgba(20,20,20,.10);
  --brand:#168B68;--brand-soft:rgba(22,139,104,.09);
  --action-bg:#111212;--action-fg:#FFFFFF;
  display:flex;flex-direction:column;box-sizing:border-box;width:100%;height:100%;
  padding:0;overflow:hidden;background:var(--canvas);color:var(--ink);
  font-family:-apple-system,BlinkMacSystemFont,"SF Pro Text","PingFang SC","Microsoft YaHei",sans-serif;
  color-scheme:light;
}
.is-dark .workbench-dashboard{
  --canvas:#111212;--surface:#191A1A;--surface-subtle:#161717;--surface-hover:#202121;
  --ink:#F2F2EF;--copy:#A3A39E;--muted:#73736F;
  --line-subtle:rgba(255,255,255,.07);--line:rgba(255,255,255,.11);
  --brand:#47B58E;--brand-soft:rgba(71,181,142,.11);
  --action-bg:#F1F1EE;--action-fg:#171717;
  color-scheme:dark;
}
.workbench-dashboard button{font-family:inherit}

.material-error{flex:0 0 auto;margin:12px 20px 0;padding:10px 14px;border-radius:11px;background:var(--danger-soft,#fff0ee);color:var(--danger,#a44337);font-size:13px}

/* ── Master–Detail：Agenda Pane（可拖拽） | 4px 命中区 | flex Detail Pane；宽度由内联 gridTemplateColumns 控制 ── */
.master-grid{display:grid;grid-template-columns:330px 4px minmax(0,1fr);flex:1 1 auto;min-height:0}

/* ── Agenda Pane：整块 subtle pane surface，不是多个 Card ── */
.agenda-pane{display:flex;flex-direction:column;min-height:0;box-sizing:border-box;padding:22px 14px 16px;background:var(--surface-subtle);overflow:hidden}
.rail-header{display:flex;align-items:baseline;gap:8px;padding:0 6px}
.rail-title{margin:0;font-size:15px;font-weight:600;letter-spacing:-.01em}
.rail-count{color:var(--copy);font-size:13px;font-weight:400}
.rail-empty{margin:16px 6px 0;color:var(--copy);font-size:13px}

/* 事件时间轴：日标签里程碑 + 类型图标节点；轴线为每个元素盒内的贯穿线，首尾止于节点中心 */
.agenda-timeline{display:grid}
.agenda-group{display:contents}
.tl-milestone{position:relative;display:grid;grid-template-columns:28px minmax(0,1fr);gap:12px;align-items:center;min-height:30px;box-sizing:border-box;padding:18px 14px 8px}
.tl-milestone.first{padding-top:6px}
.tl-milestone::before,.agenda-row::before{content:'';position:absolute;top:0;bottom:0;left:27px;width:2px;background:var(--line);border-radius:1px;pointer-events:none}
.tl-milestone.first::before{top:50%}
.agenda-row.last::before{bottom:50%}
.tl-milestone-dot{position:relative;z-index:1;width:10px;height:10px;border-radius:50%;background:var(--line);align-self:center}
.tl-milestone-label{color:var(--copy);font-size:13px;font-weight:600;letter-spacing:.02em}
.agenda-row{position:relative;display:grid;grid-template-columns:28px minmax(0,1fr);gap:12px;align-items:center;width:100%;min-height:86px;box-sizing:border-box;padding:14px;border:0;border-radius:12px;background:transparent;color:var(--ink);text-align:left;cursor:pointer;transition:background .15s ease}
.agenda-row:hover{background:var(--surface-hover)}
.agenda-row.selected{background:var(--brand-soft)}
.rail-track{display:flex;justify-content:center;align-self:stretch}
.rail-node{position:relative;z-index:1;display:grid;width:28px;height:28px;place-items:center;border:1px solid var(--line);border-radius:50%;background:var(--surface);color:var(--muted);align-self:center;transition:background .15s ease,border-color .15s ease,color .15s ease}
.rail-node.selected{border-color:transparent;background:var(--brand);color:var(--action-fg)}
.row-copy{display:grid;gap:4px;min-width:0}
.row-title{overflow:hidden;font-size:15px;font-weight:600;line-height:1.25;text-overflow:ellipsis;white-space:nowrap}
.row-meta{overflow:hidden;color:var(--copy);font-size:13px;line-height:1.3;text-overflow:ellipsis;white-space:nowrap;font-variant-numeric:tabular-nums}

/* ── Compact：rail < 290px 时隐藏日分组标签，行内联时间（● 09:50 公司 · 标题） ── */
.agenda-timeline.compact{padding-top:8px}
.agenda-timeline.compact .agenda-row{grid-template-columns:14px minmax(0,1fr);gap:8px;min-height:46px;padding:0 12px}
.agenda-timeline.compact .agenda-row::before{display:none}
.agenda-timeline.compact .agenda-row .row-title{font-size:14px}
.compact-dot{width:8px;height:8px;border-radius:50%;background:var(--line);align-self:center;transition:background .15s ease}
.compact-dot.selected{background:var(--brand)}
.row-inline-time{margin-right:8px;color:var(--copy);font-weight:500;font-variant-numeric:tabular-nums}

/* ── Split Pane Divider：1px hairline + 4px 命中区；hover/focus 加宽提示可拖 ── */
.pane-divider{position:relative;box-sizing:border-box;width:4px;background:transparent;cursor:col-resize;touch-action:none;outline:none}
.pane-divider::before{content:'';position:absolute;top:0;bottom:0;left:1.5px;width:1px;background:var(--line-subtle);transition:background .15s ease,width .15s ease,left .15s ease}
.pane-divider:hover::before,.pane-divider:focus-visible::before,.pane-divider.dragging::before{left:1px;width:2px;background:var(--brand)}
.pane-divider:focus-visible{box-shadow:inset 0 0 0 1px var(--brand)}

/* ── Detail Pane ── */
.detail-pane{display:flex;flex-direction:column;min-height:0;box-sizing:border-box;padding:26px 28px 20px 24px;overflow:auto}
.detail-header{display:grid;justify-items:start;min-width:0;max-width:720px}
.detail-relative{margin:0;color:var(--brand);font-size:13px;font-weight:600;line-height:1.2}
.detail-time{margin:4px 0 0;font-size:48px;font-weight:300;line-height:1;letter-spacing:-.03em;font-variant-numeric:tabular-nums}
.detail-title{margin:8px 0 0;font-size:21px;font-weight:600;line-height:1.25;letter-spacing:-.01em}
.detail-role{margin:6px 0 0;color:var(--copy);font-size:14px;line-height:1.35}
.detail-countdown{margin:12px 0 0;color:var(--muted);font-size:13px;line-height:1.3;font-variant-numeric:tabular-nums}
.detail-body{margin-top:28px;min-width:0}

/* ── 关联目标行 + 准备时间轴 → 下一步（纵向顺延） ── */
.detail-body{margin-top:24px;max-width:720px;min-width:0}
.identity-block{display:grid;gap:4px;min-width:0}
.identity-company{overflow:hidden;margin:0;color:var(--copy);font-size:13px;line-height:1.3;text-overflow:ellipsis;white-space:nowrap}
.identity-role{overflow:hidden;margin:0;font-size:18px;font-weight:600;line-height:1.25;letter-spacing:-.01em;text-overflow:ellipsis;white-space:nowrap}
.linked-target{display:grid;gap:6px;min-width:0}
.linked-target-label{color:var(--muted);font-size:12px;font-weight:600;letter-spacing:.05em}
.linked-target-row{display:flex;align-items:center;gap:10px;min-width:0}
.linked-target-copy{overflow:hidden;min-width:0;color:var(--copy);font-size:14px;font-weight:600;line-height:1.4;text-overflow:ellipsis;white-space:nowrap}
.linked-target-action{display:inline-flex;align-items:center;gap:4px;flex:0 0 auto;margin-left:auto;padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500;cursor:pointer;transition:color .15s ease}
.linked-target-action:hover{color:var(--ink)}
.detail-section{margin-top:24px}
.section-label{margin:0 0 8px;color:var(--copy);font-size:13px;font-weight:600}

/* 准备 = 32px 中性图标容器；完成态以右下角绿色小 check 表达，无贯穿线 */
.object-list{display:grid}
.object-row{display:grid;grid-template-columns:32px minmax(0,1fr) auto;gap:12px;align-items:start;min-height:64px;box-sizing:border-box}
.tl-track{position:relative;display:flex;justify-content:center;align-self:stretch}
.tl-dot{position:relative;z-index:1;display:grid;width:32px;height:32px;margin-top:16px;place-items:center;border:1px solid var(--line-subtle);border-radius:10px;background:var(--surface);color:var(--muted)}
.tl-dot.ready::after{content:'✓';position:absolute;right:-3px;bottom:-3px;display:grid;width:13px;height:13px;place-items:center;border-radius:50%;background:var(--brand);color:#fff;font-size:9px;font-weight:700;line-height:1}
.object-copy{display:grid;gap:3px;min-width:0;padding-top:19px}
.object-name{font-size:13px;font-weight:600;line-height:1.3;white-space:nowrap}
.object-meta{overflow:hidden;color:var(--muted);font-size:12px;line-height:1.3;text-overflow:ellipsis;white-space:nowrap}
.object-action{display:inline-flex;align-items:center;gap:3px;margin-top:19px;padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500;cursor:pointer;transition:color .15s ease}
.object-action:hover{color:var(--ink)}
.next-text{margin:0;font-size:15px;font-weight:550;line-height:1.5}
.next-actions{display:flex;align-items:center;gap:12px;margin-top:14px;justify-content:flex-start}
.next-hint{color:var(--muted);font-size:13px;line-height:1.3;white-space:nowrap}
.primary-button{display:inline-flex;align-items:center;gap:8px;height:40px;padding:0 16px;border:0;border-radius:11px;background:var(--action-bg);color:var(--action-fg);font-size:14px;font-weight:600;cursor:pointer;transition:filter .15s ease}
.primary-button:hover{filter:brightness(1.07)}
.target-detail{display:flex;align-items:center;gap:5px;margin-top:28px;padding:0;border:0;background:transparent;color:var(--copy);font-size:14px;font-weight:500;cursor:pointer;transition:color .15s ease}
.target-detail:hover{color:var(--ink)}

/* ── 未关联：只展示真实信息 + 关联/查看日程 ── */
.unlinked-line{margin:0;color:var(--copy);font-size:13px}
.unlinked-actions{display:flex;align-items:center;gap:24px;margin-top:12px}
.unlinked-action{display:flex;align-items:center;gap:5px;padding:0;border:0;background:transparent;color:var(--copy);font-size:14px;font-weight:500;cursor:pointer;transition:color .15s ease}
.unlinked-action:hover{color:var(--ink)}

/* ── 最近活动：Detail Workspace 底部，不独立漂浮 ── */
.activity-zone{margin-top:auto;padding-top:40px;max-width:720px}
.zone-title{margin:0 0 14px;font-size:15px;font-weight:600;letter-spacing:-.01em}
.activity-row{display:grid;grid-template-columns:auto minmax(0,1fr) auto;gap:14px;align-items:start}
.activity-date{color:var(--muted);font-size:12px;white-space:nowrap;padding-top:3px}
.activity-copy{min-width:0}
.activity-title{margin:0;font-size:14px;font-weight:600;line-height:1.4}
.activity-summary{margin:5px 0 0;display:-webkit-box;overflow:hidden;color:var(--copy);font-size:14px;line-height:1.6;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.activity-report{display:flex;align-items:center;gap:4px;padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500;cursor:pointer;transition:color .15s ease}
.activity-report:hover{color:var(--ink)}
.activity-empty{display:grid;gap:4px}
.activity-empty p{margin:0;color:var(--copy);font-size:13px;line-height:1.5}

/* ── 拖拽期间禁止文本选中/光标抖动（组件内 global body 类，随拖拽结束移除） ── */
:global(body.rail-resizing){cursor:col-resize;user-select:none}

/* ── 高度紧凑：900px 以下保持单屏不滚动 ── */
@media (max-height:899px){
  .agenda-pane{padding-top:18px}
  .agenda-row{min-height:72px;padding:12px 14px}
  .tl-milestone{padding-top:12px}
  .detail-pane{padding-top:22px}
  .detail-time{font-size:40px}
  .detail-body{margin-top:18px}
  .detail-section{margin-top:18px}
  .target-detail{margin-top:22px}
  .activity-zone{padding-top:32px}
  .object-row{min-height:56px}
}
</style>
