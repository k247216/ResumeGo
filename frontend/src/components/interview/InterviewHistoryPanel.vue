<template>
  <aside class="lobby-side">
    <section v-if="currentPlan" class="plan-summary" data-test="plan-summary">
      <p class="inspector-kicker">本次练习</p>
      <div class="plan-summary-row">
        <span>目标</span>
        <strong>{{ currentPlan.jobLabel }}</strong>
      </div>
      <div class="plan-summary-row">
        <span>简历</span>
        <strong>{{ currentPlan.resumeLabel }}</strong>
      </div>
      <div class="plan-summary-row">
        <span>面试官</span>
        <strong>{{ currentPlan.personaName }}<template v-if="currentPlan.personaTitle"> · {{ currentPlan.personaTitle }}</template></strong>
      </div>
      <div class="plan-summary-row">
        <span>题目</span>
        <strong>{{ currentPlan.questionCount }} 道</strong>
      </div>
      <div class="plan-summary-row">
        <span>预计</span>
        <strong>{{ estimateSessionMinutesLabel(currentPlan.questionCount) }}</strong>
      </div>
    </section>

    <section class="history-section">
      <div class="history-head">
        <h2>最近面试</h2>
        <p>以一次面试计划为单位管理历史。</p>
      </div>

      <div v-if="records.length" class="history-filter-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.key"
          :data-test="`history-filter-${tab.key}`"
          class="filter-tab"
          :class="{ active: activeFilter === tab.key }"
          type="button"
          @click="emit('update:activeFilter', tab.key)"
        >
          {{ tab.label }} ({{ tab.count }})
        </button>
      </div>

      <div v-if="records.length === 0" class="history-empty-card">
        <el-icon><VideoPlay /></el-icon>
        <strong>还没有面试记录</strong>
        <span>先从左侧创建一次面试。</span>
      </div>

      <div v-else class="history-record-list">
        <article
          v-for="record in filteredRecords"
          :key="record.id"
          class="history-record-row"
        >
          <div class="history-record-main" @click="emit('open', record)">
            <span class="hsc-status-dot" :class="recordStatus(record)" aria-hidden="true" />
            <div class="history-record-copy">
              <span v-if="record.dateLabel" class="history-record-date">{{ record.dateLabel }}</span>
              <strong>{{ record.title }}</strong>
              <span>{{ personaChain(record) }}</span>
            </div>
            <span class="history-record-rounds">{{ record.completedCount }}/{{ record.totalCount }} 轮</span>
          </div>
          <div class="history-record-actions">
            <button
              :data-test="`history-open-${record.id}`"
              class="history-open-button"
              type="button"
              @click="emit('open', record)"
            >
              {{ record.isCompleted ? '查看复盘' : '继续面试' }}
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button
              :data-test="`history-delete-${record.id}`"
              class="history-delete-button"
              type="button"
              @click="emit('delete', record)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </article>
      </div>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { ArrowRight, Delete, VideoPlay } from '@element-plus/icons-vue'
import {
  interviewRecordStatus,
  type InterviewRecord,
} from '../../utils/interviewRecords'
import { estimateSessionMinutesLabel } from '../../utils/interviewEstimate'

export type InterviewHistoryFilter = 'all' | 'completed' | 'inProgress'

interface FilterTab {
  key: InterviewHistoryFilter
  label: string
  count: number
}

export interface CurrentPlanSummary {
  jobLabel: string
  resumeLabel: string
  questionCount: number
  personaName: string
  personaTitle?: string
}

defineProps<{
  records: InterviewRecord[]
  filteredRecords: InterviewRecord[]
  filterTabs: FilterTab[]
  activeFilter: InterviewHistoryFilter
  currentPlan: CurrentPlanSummary | null
}>()

const emit = defineEmits<{
  open: [record: InterviewRecord]
  delete: [record: InterviewRecord]
  'update:activeFilter': [filter: InterviewHistoryFilter]
}>()

function recordStatus(record: InterviewRecord) {
  return interviewRecordStatus(record)
}

// 面试官顺序：张老师 → 李架构（记录按轮次排序，来自 personaNames）
function personaChain(record: InterviewRecord): string {
  return record.subtitle.split(' / ').filter(Boolean).join(' → ')
}
</script>

<style scoped>
.lobby-side{display:flex;flex-direction:column;gap:22px;min-width:0}
.inspector-kicker{margin:0 0 12px;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:.06em}
.plan-summary{display:grid}
.plan-summary-row{display:flex;align-items:baseline;justify-content:space-between;gap:12px;padding:9px 2px}
.plan-summary-row + .plan-summary-row{border-top:1px solid var(--border-subtle)}
.plan-summary-row span{flex:0 0 auto;color:var(--muted);font-size:12px;font-weight:600}
.plan-summary-row strong{overflow:hidden;min-width:0;color:var(--ink);font-size:13px;font-weight:600;text-overflow:ellipsis;white-space:nowrap;text-align:right}
.history-section{display:grid;padding-top:20px;border-top:1px solid var(--border-subtle)}
.history-head h2{margin:0;color:var(--ink);font-size:16px;font-weight:650;letter-spacing:-.01em}
.history-head p{margin:4px 0 0;color:var(--muted);font-size:12px}
.history-filter-tabs{display:flex;gap:6px;overflow-x:auto;margin:12px 0 4px}
.filter-tab{flex-shrink:0;padding:5px 10px;border:0;border-radius:var(--radius-control);background:transparent;color:var(--muted);font-size:12px;font-weight:600;cursor:pointer}
.filter-tab.active{background:var(--bg-selected);color:var(--brand)}
.history-empty-card{display:grid;place-items:center;gap:6px;min-height:120px;color:var(--muted);text-align:center}
.history-empty-card strong{color:var(--ink);font-size:14px;font-weight:600}
.history-empty-card span{font-size:13px}
.history-record-list{display:grid;margin-top:4px}
.history-record-row{display:grid;gap:8px;padding:12px 2px}
.history-record-row + .history-record-row{border-top:1px solid var(--border-subtle)}
.history-record-main{display:flex;align-items:center;gap:10px;min-width:0;cursor:pointer}
.hsc-status-dot{flex:0 0 auto;width:8px;height:8px;border-radius:50%}
.hsc-status-dot.completed{background:var(--brand)}
.hsc-status-dot.failed{background:var(--danger)}
.hsc-status-dot.cancelled{background:var(--muted)}
.hsc-status-dot.active{background:var(--warning)}
.history-record-copy{display:grid;gap:2px;min-width:0;flex:1}
.history-record-copy strong,.history-record-copy span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.history-record-copy strong{color:var(--ink);font-size:13px;font-weight:600}
.history-record-copy span{color:var(--muted);font-size:11px}
.history-record-date{color:var(--brand)!important;font-weight:600}
.history-record-rounds{flex:0 0 auto;color:var(--copy);font-size:12px;font-variant-numeric:tabular-nums}
.history-record-actions{display:flex;gap:8px;align-items:center}
.history-open-button{display:inline-flex;align-items:center;gap:4px;padding:6px 12px;border:0;border-radius:var(--radius-control);background:var(--bg-hover);color:var(--ink);font-size:12px;font-weight:600;cursor:pointer}
.history-open-button:hover{background:var(--bg-selected);color:var(--brand)}
.history-delete-button{display:grid;place-items:center;width:28px;height:28px;border:0;border-radius:var(--radius-control);background:transparent;color:var(--muted);cursor:pointer}
.history-delete-button:hover{background:var(--danger-soft);color:var(--danger)}
</style>
