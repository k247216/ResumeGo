<template>
  <aside class="lobby-side">
    <section class="recent-interview-card">
      <div class="recent-card-head">
        <span>Quick Access</span>
        <strong>最近面试</strong>
      </div>
      <div v-if="recentRecords.length" class="recent-record-list">
        <article
          v-for="record in recentRecords"
          :key="record.id"
          class="recent-record"
          @click="emit('open', record)"
        >
          <div>
            <strong>{{ record.title }}</strong>
            <span>{{ record.completedCount }}/{{ record.totalCount }} 轮 · {{ record.resumeLabel }}</span>
          </div>
          <button type="button">{{ record.isCompleted ? '查看' : '继续' }}</button>
        </article>
      </div>
      <p v-else class="recent-empty">创建一次面试后，这里会出现快捷入口。</p>
    </section>

    <section class="history-sessions-section side-history-section">
      <div class="history-head">
        <div>
          <p class="section-kicker">History</p>
          <h2>面试记录</h2>
          <p>以一次面试计划为单位管理历史。</p>
        </div>
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

      <div v-else class="history-record-grid">
        <article
          v-for="record in filteredRecords"
          :key="record.id"
          class="history-record-card"
        >
          <div class="history-card-top">
            <span class="hsc-status" :class="recordStatus(record)">
              {{ recordStatusText(record) }}
            </span>
          </div>
          <div class="hsc-main" @click="emit('open', record)">
            <CompanyAvatar v-if="record.job" class="hsc-company-avatar" :job="record.job" size="md" />
            <div v-else class="hsc-avatar">{{ record.latestSession.personaName?.charAt(0) || '面' }}</div>
            <div class="hsc-info">
              <span class="hsc-name">{{ record.title }}</span>
              <span class="hsc-title">{{ record.resumeLabel }} · {{ record.subtitle }}</span>
            </div>
          </div>
          <div class="hsc-progress-line">
            <span>轮次进度</span>
            <strong>{{ record.completedCount }}/{{ record.totalCount }} 轮</strong>
          </div>
          <div class="record-round-list">
            <span
              v-for="(session, index) in record.sessions"
              :key="session.sessionId"
              :class="interviewRoundStatus(session)"
            >
              {{ index + 1 }}. {{ session.personaName || '面试官' }}
            </span>
          </div>
          <el-progress
            :percentage="interviewRecordProgress(record)"
            :show-text="false"
            :stroke-width="6"
            :color="record.isCompleted ? '#10b981' : '#101a33'"
          />
          <div class="record-actions">
            <button
              :data-test="`history-open-${record.id}`"
              class="hsc-open-button"
              type="button"
              @click="emit('open', record)"
            >
              {{ record.isCompleted ? '查看复盘' : '继续面试' }}
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button
              :data-test="`history-delete-${record.id}`"
              class="hsc-delete-button"
              type="button"
              @click.stop="emit('delete', record)"
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
import CompanyAvatar from '../CompanyAvatar.vue'
import {
  interviewRecordProgress,
  interviewRecordStatus,
  interviewRoundStatus,
  type InterviewRecord,
} from '../../utils/interviewRecords'

export type InterviewHistoryFilter = 'all' | 'completed' | 'inProgress'

interface FilterTab {
  key: InterviewHistoryFilter
  label: string
  count: number
}

defineProps<{
  records: InterviewRecord[]
  filteredRecords: InterviewRecord[]
  recentRecords: InterviewRecord[]
  filterTabs: FilterTab[]
  activeFilter: InterviewHistoryFilter
}>()

const emit = defineEmits<{
  open: [record: InterviewRecord]
  delete: [record: InterviewRecord]
  'update:activeFilter': [filter: InterviewHistoryFilter]
}>()

function recordStatusText(record: InterviewRecord) {
  return {
    completed: '已完成',
    failed: '异常中断',
    cancelled: '已取消',
    active: '进行中',
  }[interviewRecordStatus(record)]
}

function recordStatus(record: InterviewRecord) {
  return interviewRecordStatus(record)
}
</script>

<style scoped>
.lobby-side{display:flex;flex-direction:column;gap:14px;min-width:0}.recent-interview-card,.history-sessions-section{padding:16px;border:1px solid #e5eaf2;border-radius:22px;background:rgba(255,255,255,.96);box-shadow:0 16px 46px rgba(15,23,42,.06)}.recent-card-head span,.recent-card-head strong{display:block}.recent-card-head span,.section-kicker{margin:0;color:#10b981;font-size:10px;font-weight:900;letter-spacing:.12em;text-transform:uppercase}.recent-card-head strong{margin-top:4px;color:#0f172a;font-size:18px}.recent-record-list{display:grid;gap:8px;margin-top:12px}.recent-record{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:10px;border:1px solid #e5eaf2;border-radius:14px;cursor:pointer}.recent-record:hover{border-color:#cfeee2;background:#f8fffb}.recent-record div{min-width:0}.recent-record strong,.recent-record span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.recent-record strong{color:#101a33;font-size:13px}.recent-record span{margin-top:3px;color:#778398;font-size:11px;font-weight:700}.recent-record button{border:0;border-radius:999px;background:#f1f5f9;color:#334155;padding:6px 10px;font-size:12px;font-weight:900}.recent-empty{margin:12px 0 0;color:#64748b;font-size:13px;line-height:1.6}.history-head{margin-bottom:12px}.history-head h2{margin:2px 0 4px;color:#0f172a;font-size:18px;font-weight:900}.history-head p:last-child{margin:0;color:#64748b;font-size:12px}.history-filter-tabs{display:flex;gap:8px;overflow-x:auto;margin-bottom:12px}.filter-tab{flex-shrink:0;padding:6px 10px;border:0;border-radius:999px;background:transparent;color:#778398;font-size:12px;font-weight:900;cursor:pointer}.filter-tab.active{background:#101a33;color:#fff}.history-empty-card{display:grid;place-items:center;gap:8px;min-height:132px;border:1px dashed #cbd5e1;border-radius:18px;background:#f8fafc;color:#64748b;text-align:center}.history-empty-card strong{color:#0f172a;font-size:14px}.history-empty-card span{font-size:13px}.history-record-grid{display:grid;gap:10px;max-height:46vh;overflow:auto;padding-right:2px}.history-record-card{display:flex;flex-direction:column;gap:8px;min-width:0;padding:10px;border:1px solid #e5eaf2;border-radius:14px;background:#fff}.history-card-top{display:flex;justify-content:flex-end}.hsc-status{padding:3px 8px;border-radius:999px;background:#eef2ff;color:#475569;font-size:10px;font-weight:900}.hsc-status.completed{background:#ecfdf5;color:#047857}.hsc-status.failed{background:#fef2f2;color:#b91c1c}.hsc-status.cancelled{background:#f8fafc;color:#64748b}.hsc-main{display:flex;align-items:center;gap:10px;min-width:0;cursor:pointer}.hsc-avatar{display:grid;place-items:center;flex:0 0 auto;width:38px;height:38px;border-radius:12px;background:#101a33;color:#fff;font-weight:900}.hsc-info{min-width:0}.hsc-name,.hsc-title{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.hsc-name{color:#0f172a;font-size:13px;font-weight:900}.hsc-title{margin-top:3px;color:#64748b;font-size:11px}.hsc-progress-line{display:flex;justify-content:space-between;color:#64748b;font-size:11px}.hsc-progress-line strong{color:#0f172a}.record-round-list{display:flex;flex-wrap:wrap;gap:5px}.record-round-list span{padding:3px 7px;border-radius:999px;background:#f1f5f9;color:#475569;font-size:10px}.record-round-list span.completed{background:#ecfdf5;color:#047857}.record-round-list span.failed{background:#fef2f2;color:#b91c1c}.record-round-list span.cancelled{color:#94a3b8}.record-actions{display:flex;gap:7px}.record-actions button{border:0;border-radius:10px;cursor:pointer}.hsc-open-button{display:flex;align-items:center;justify-content:center;gap:5px;flex:1;padding:8px;background:#101a33;color:#fff;font-size:12px;font-weight:900}.hsc-delete-button{width:34px;background:#fef2f2;color:#dc2626}@media(max-width:900px){.history-record-grid{max-height:none}}
</style>
