<template>
  <aside class="version-inspector" data-test="resume-version-inspector">
    <header class="inspector-head">
      <div>
        <p class="inspector-kicker">版本检查器</p>
        <h2 v-if="selectedVersion">V{{ selectedVersion.versionNo }}</h2>
        <h2 v-else-if="resume">{{ resume.title }}</h2>
      </div>
      <button type="button" class="inspector-close" data-test="inspector-close" aria-label="收起检查器" @click="emit('close')">×</button>
    </header>

    <template v-if="resume && selectedVersion">
      <!-- 操作层级：按视觉目标，创建岗位副本为绿色主操作 -->
      <div class="inspector-actions">
        <button type="button" class="inspector-primary" data-test="open-fork" @click="emit('fork')">
          基于此版本创建岗位副本
        </button>
        <router-link
          v-if="viewingCurrent"
          class="inspector-secondary"
          data-test="continue-editing"
          :to="buildResumeEditorLocation({ resumeId: resume.id, versionId: selectedVersion.id })"
        >进入编辑台</router-link>
        <span v-else class="readonly-note" data-test="history-readonly-note">历史版本只读，不改变当前版本</span>
      </div>

      <section class="inspector-section" data-test="version-meta">
        <h3 class="section-title">版本信息</h3>
        <div class="meta-grid">
          <div class="meta-row"><span>创建来源</span><strong>{{ sourceLabel(selectedVersion.createdByType) }}</strong></div>
          <div class="meta-row"><span>创建时间</span><strong>{{ formatTime(selectedVersion.createdAt) }}</strong></div>
          <div v-if="selectedVersion.parentVersionId" class="meta-row"><span>父版本</span><strong>#{{ selectedVersion.parentVersionId }}</strong></div>
        </div>
        <p v-if="selectedVersion.changeSummary" class="meta-summary">{{ selectedVersion.changeSummary }}</p>
      </section>

      <section class="inspector-section" data-test="binding-status">
        <h3 class="section-title">绑定状态</h3>
        <p v-if="!usedByTargets.length" class="inspector-note" data-test="binding-empty">此版本尚未绑定到任何求职目标。</p>
        <div v-else class="used-by-list">
          <button
            v-for="row in usedByTargets"
            :key="row.targetId"
            type="button"
            class="used-by-row"
            data-test="used-by-target"
            @click="emit('open-target', row.targetId)"
          >
            <span class="used-by-copy">{{ row.label }}</span>
          </button>
        </div>
      </section>

      

      <section v-if="resume.forkedFromVersionId" class="inspector-section" data-test="inspector-fork-source">
        <h3 class="section-title">资产来源</h3>
        <p class="inspector-note">岗位表达副本：复制自源版本 #{{ resume.forkedFromVersionId }}，创建后独立演进，不随源资产同步。</p>
      </section>

      <section class="inspector-section" data-test="inspector-used-by-extra">
        <div class="section-head">
          <h3 class="section-title">历史引用</h3>
          <span class="section-count">{{ usedByTargets.length }} 个目标</span>
        </div>
        <p v-if="usedByLoading" class="inspector-note">正在读取绑定目标…</p>
        <p v-else-if="!usedByTargets.length" class="inspector-note">尚未绑定任何求职目标</p>
        <div v-else class="used-by-list">
          <button
            v-for="row in usedByTargets"
            :key="row.targetId"
            type="button"
            class="used-by-row"
            @click="emit('open-target', row.targetId)"
          >
            <span class="used-by-copy">{{ row.label }}</span>
          </button>
        </div>
      </section>

      <section class="inspector-section" data-test="activity-timeline">
        <div class="section-head">
          <h3 class="section-title">历史活动</h3>
        </div>
        <ul class="activity-list" data-test="activity-list">
          <li v-for="event in activityEvents" :key="event.key" class="activity-row">
            <span class="activity-dot" :class="{ fork: event.fork }" aria-hidden="true"></span>
            <span class="activity-copy">
              <strong>{{ event.label }}</strong>
              <small>{{ event.time }}</small>
            </span>
          </li>
        </ul>
      </section>

      <div class="inspector-overflow">
        <button type="button" class="overflow-btn" data-test="archive-resume" @click="emit('archive')">
          {{ resume.archivedAt ? '恢复这份简历' : '归档这份简历' }}
        </button>
        <p>归档不删除版本与历史引用。</p>
      </div>
    </template>
    <div v-else class="inspector-placeholder">选择资产后查看版本信息</div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Resume, ResumeVersion } from '../../types/resume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const props = defineProps<{
  resume: Resume | null
  versions: ResumeVersion[]
  selectedVersion: ResumeVersion | null
  currentVersionId: number | null
  usedByTargets: Array<{ targetId: number; label: string }>
  usedByLoading: boolean
}>()

const emit = defineEmits<{
  'open-target': [id: number]
  fork: []
  archive: []
  close: []
}>()

const viewingCurrent = computed(() =>
  props.selectedVersion != null && props.selectedVersion.id === props.currentVersionId)

/** 版本健康：确定性结构检查（正文可读、基本字段存在），不代表 AI 评分 */

/** 历史活动：从真实版本派生（创建事件），不虚构改名/编辑记录 */
const activityEvents = computed(() => {
  const events = [...props.versions].sort((left, right) =>
    String(right.createdAt).localeCompare(String(left.createdAt)))
    .map((version) => ({
      key: version.id,
      label: `创建了版本 V${version.versionNo}`,
      time: formatTime(version.createdAt),
      fork: version.createdByType === 'fork',
    }))
  return events
})

function sourceLabel(type: string) {
  if (type === 'user') return '手工保存'
  if (type === 'fork') return '岗位副本创建'
  if (type === 'import') return '导入'
  if (type === 'ai_suggestion') return 'AI 建议'
  return '版本记录'
}
function formatTime(value: string) {
  if (!value) return '时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间未知'
    : date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.version-inspector{display:grid;gap:14px;align-content:start}
.inspector-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}
.inspector-kicker{margin:0;color:var(--muted);font-size:10.5px;font-weight:650;letter-spacing:.07em}
.inspector-head h2{margin:4px 0 0;font-size:18px;font-weight:700;color:var(--ink);font-variant-numeric:tabular-nums}
.inspector-close{border:0;background:none;border-radius:8px;width:26px;height:26px;color:var(--muted);font-size:16px;cursor:pointer}
.inspector-close:hover{background:var(--bg-hover);color:var(--ink)}
.inspector-actions{display:grid;gap:8px}
.inspector-primary{display:grid;place-items:center;border:1px solid #17181a;border-radius:10px;background:#17181a;color:#fff;padding:10px 14px;font-size:13px;font-weight:600;cursor:pointer;text-decoration:none}
.health-row{display:flex;align-items:center;gap:8px;margin:0;color:var(--muted);font-size:12.5px;font-weight:600}
.health-row.ok{color:var(--brand)}
.health-dot{width:8px;height:8px;border-radius:50%;background:var(--muted)}
.health-row.ok .health-dot{background:var(--brand)}
.inspector-secondary{border:1px solid var(--line,rgba(28,31,35,.18));border-radius:10px;background:transparent;color:var(--copy);padding:9px 13px;font-size:12.5px;font-weight:550;cursor:pointer}
.inspector-secondary:hover{border-color:var(--brand);color:var(--brand)}
.readonly-note{padding:8px 0;color:var(--muted);font-size:11.5px}
.inspector-section{display:grid;gap:7px;border-top:1px solid var(--border-subtle);padding-top:12px}
.section-title{margin:0;font-size:10.5px;font-weight:650;letter-spacing:.07em;color:var(--muted)}
.section-head{display:flex;align-items:baseline;justify-content:space-between}
.section-count{font-size:10.5px;color:var(--muted)}
.meta-grid{display:grid;gap:6px}
.meta-row{display:flex;align-items:center;justify-content:space-between;font-size:12px;color:var(--copy)}
.meta-row strong{font-weight:550;font-variant-numeric:tabular-nums}
.meta-summary{margin:0;color:var(--copy);font-size:11.5px;line-height:1.6;background:var(--bg-subtle);border-radius:8px;padding:8px 10px}
.inspector-note{margin:0;color:var(--muted);font-size:11.5px;line-height:1.65}
.used-by-list{display:grid;gap:4px}
.used-by-row{display:flex;align-items:center;border:0;background:none;border-radius:8px;padding:7px 9px;font-size:12px;color:var(--copy);cursor:pointer;text-align:left}
.used-by-row:hover{background:var(--bg-hover);color:var(--brand)}
.used-by-copy{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.activity-list{margin:0;padding:0;list-style:none;display:grid;gap:10px}
.activity-row{display:flex;align-items:flex-start;gap:9px}
.activity-dot{width:8px;height:8px;border-radius:50%;background:var(--line,rgba(28,31,35,.3));flex:0 0 auto;margin-top:4px}
.activity-dot.fork{background:var(--brand)}
.activity-copy{display:grid;gap:1px;min-width:0}
.activity-copy strong{font-size:12px;font-weight:550;color:var(--copy)}
.activity-copy small{font-size:10.5px;color:var(--muted);font-variant-numeric:tabular-nums}
.inspector-overflow{border-top:1px solid var(--border-subtle);padding-top:12px}
.overflow-btn{border:0;background:none;padding:0;color:var(--muted);font-size:12px;font-weight:550;cursor:pointer}
.overflow-btn:hover{color:var(--danger)}
.inspector-overflow p{margin:6px 0 0;color:var(--muted);font-size:10.5px;line-height:1.6}
.inspector-placeholder{color:var(--muted);font-size:12px;padding:16px 0}
</style>
