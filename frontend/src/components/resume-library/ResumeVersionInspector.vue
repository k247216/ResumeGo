<template>
  <aside class="version-inspector" data-test="resume-version-inspector">
    <header class="inspector-head">
      <div>
        <p class="inspector-kicker">版本与引用</p>
        <h2 v-if="resume">{{ resume.title }}</h2>
      </div>
      <button type="button" class="inspector-close" data-test="inspector-close" aria-label="收起详情" @click="emit('close')">×
      </button>
    </header>

    <template v-if="resume">
      <section v-if="resume.forkedFromVersionId" class="inspector-section" data-test="inspector-fork-source">
        <h3 class="section-title">来源版本</h3>
        <p class="inspector-note">复制自版本 #{{ resume.forkedFromVersionId }}，创建后与源简历独立演进。</p>
      </section>

      <section class="inspector-section" data-test="inspector-used-by">
        <div class="section-head">
          <h3 class="section-title">用于</h3>
          <span class="section-count">{{ usedByTargets.length }} 个求职目标</span>
        </div>
        <p v-if="usedByLoading" class="inspector-note">正在读取关联目标…</p>
        <p v-else-if="!usedByTargets.length" class="inspector-note">尚未关联求职目标</p>
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

      <section class="inspector-section version-section" data-test="inspector-versions">
        <div class="version-head">
          <h3 class="section-title">版本历史</h3>
          <span class="version-count">{{ versions.length }} 个版本</span>
        </div>
        <p class="readonly-note">历史版本只读；继续历史内容请创建岗位表达副本。</p>
        <p v-if="versionLoading" class="inspector-note">正在读取版本…</p>
        <p v-else-if="versionError" class="inspector-error">{{ versionError }}</p>
        <template v-else>
          <div class="version-list">
            <button
              v-for="version in visibleVersions"
              :key="version.id"
              type="button"
              class="version-row"
              :class="{ active: version.id === selectedVersionId }"
              :data-test="`version-row-${version.id}`"
              @click="emit('select-version', version.id)"
            >
              <strong>V{{ version.versionNo }}</strong>
              <span class="version-meta">
                <small>{{ createdByLabel(version.createdByType) }}</small>
                <time>{{ formatTime(version.createdAt) }}</time>
              </span>
              <em>{{ version.changeSummary || '手工保存的简历版本' }}</em>
            </button>
          </div>
          <button
            v-if="versions.length > 3"
            type="button"
            class="versions-expand"
            data-test="versions-expand"
            @click="showAll = !showAll"
          >{{ showAll ? '收起版本' : '查看全部版本' }}</button>
        </template>
      </section>

      <div class="inspector-archive">
        <button type="button" class="danger-link" data-test="archive-resume" @click="emit('archive')">
          归档这份简历
        </button>
        <p>归档后默认列表隐藏，可随时恢复；历史引用仍然有效。</p>
      </div>
    </template>
    <div v-else class="inspector-empty">选择一份简历查看版本与引用</div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Resume, ResumeVersion } from '../../types/resume'

interface UsedByTarget { targetId: number; label: string }

const props = defineProps<{
  resume: Resume | null
  versions: ResumeVersion[]
  selectedVersionId: number | null
  versionLoading: boolean
  versionError: string
  usedByTargets: UsedByTarget[]
  usedByLoading: boolean
}>()

const emit = defineEmits<{
  'select-version': [id: number]
  'open-target': [id: number]
  archive: []
  close: []
}>()

const showAll = ref(false)
watch(() => props.resume?.id, () => { showAll.value = false })

const orderedVersions = computed(() =>
  [...props.versions].sort((left, right) => String(right.createdAt).localeCompare(String(left.createdAt))))
const visibleVersions = computed(() => (showAll.value ? orderedVersions.value : orderedVersions.value.slice(0, 3)))

function createdByLabel(type: string) {
  if (type === 'user') return '手工维护'
  if (type === 'fork') return '副本创建'
  if (type === 'system') return '系统创建'
  if (type === 'ai_suggestion') return '建议生成'
  if (type === 'import') return '导入'
  return '版本记录'
}
function formatTime(value: string) {
  if (!value) return '时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间未知'
    : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.version-inspector{display:grid;gap:16px;align-content:start}
.inspector-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}
.inspector-kicker{margin:0;color:var(--muted);font-size:11px;font-weight:650;letter-spacing:.06em}
.inspector-head h2{margin:6px 0 0;font-size:17px;font-weight:700;color:var(--ink)}
.inspector-close{border:0;background:none;border-radius:8px;width:26px;height:26px;color:var(--muted);font-size:16px;cursor:pointer}
.inspector-close:hover{background:var(--bg-hover);color:var(--ink)}
.inspector-section{display:grid;gap:8px;border-top:1px solid var(--border-subtle);padding-top:12px}
.section-head{display:flex;align-items:baseline;justify-content:space-between}
.section-title{margin:0;font-size:11px;font-weight:650;letter-spacing:.06em;color:var(--muted)}
.section-count{font-size:11px;color:var(--muted)}
.inspector-note{margin:0;color:var(--muted);font-size:12px;line-height:1.6}
.inspector-error{margin:0;color:var(--danger);font-size:12px}
.readonly-note{margin:0;color:var(--muted);font-size:11px}
.used-by-list{display:grid;gap:4px}
.used-by-row{display:flex;align-items:center;justify-content:space-between;gap:8px;border:0;background:none;border-radius:8px;padding:7px 9px;font-size:12.5px;color:var(--copy);cursor:pointer;text-align:left}
.used-by-row:hover{background:var(--bg-hover);color:var(--brand)}
.used-by-copy{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.version-head{display:flex;align-items:baseline;justify-content:space-between}
.version-count{font-size:11px;color:var(--muted)}
.version-list{display:grid;gap:4px}
.version-row{display:grid;grid-template-columns:34px minmax(0,1fr);gap:2px 8px;text-align:left;border:1px solid transparent;background:none;border-radius:9px;padding:8px 10px;cursor:pointer}
.version-row:hover{background:var(--bg-hover)}
.version-row.active{border-color:var(--brand-soft);background:var(--bg-selected)}
.version-row strong{grid-row:span 2;font-size:13px;color:var(--ink);font-variant-numeric:tabular-nums}
.version-meta{display:flex;gap:8px;align-items:baseline}
.version-meta small{color:var(--muted);font-size:10.5px}
.version-meta time{color:var(--muted);font-size:10.5px;font-variant-numeric:tabular-nums}
.version-row em{grid-column:2;font-style:normal;font-size:11.5px;color:var(--copy);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.versions-expand{justify-self:start;border:0;background:none;padding:2px 0;color:var(--brand);font-size:12px;font-weight:600;cursor:pointer}
.inspector-archive{border-top:1px solid var(--border-subtle);padding-top:12px}
.danger-link{border:0;background:none;padding:0;color:var(--danger);font-size:12.5px;font-weight:600;cursor:pointer}
.inspector-archive p{margin:6px 0 0;color:var(--muted);font-size:11px;line-height:1.6}
.inspector-empty{display:grid;place-items:center;min-height:120px;color:var(--muted);font-size:12.5px}
</style>
