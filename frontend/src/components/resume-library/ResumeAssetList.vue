<template>
  <div class="asset-list" data-test="resume-asset-list">
    <div v-if="loading && !items.length" class="state-card">正在读取本地简历…</div>

    <div v-else-if="error && !items.length" data-test="resume-library-error" class="state-card error">
      <strong>无法读取本地简历</strong>
      <span>{{ error }}</span>
      <button type="button" class="state-btn" data-test="retry-load" @click="emit('retry')">重新加载</button>
    </div>

    <div v-else-if="!items.length" data-test="resume-library-empty" class="state-card empty">
      <strong>创建第一份本地简历</strong>
      <span>可以先整理通用简历，也可以直接导入 Markdown 文件。</span>
      <button type="button" class="state-primary" data-test="create-blank" @click="emit('create-blank')">从空白开始</button>
      <button type="button" class="state-btn" data-test="import-md-empty" @click="emit('import')">导入 Markdown</button>
    </div>

    <ul v-else class="asset-rows">
      <li v-for="resume in items" :key="resume.id">
        <button
          type="button"
          class="asset-row"
          :class="{ selected: resume.id === selectedId }"
          :data-test="`asset-row-${resume.id}`"
          @click="emit('select', resume.id)"
        >
          <span class="asset-copy">
            <strong>{{ resume.title }}</strong>
            <small>{{ updatedLabel(resume) }}</small>
          </span>
          <span class="asset-kind" :class="kindClass(resume)" :data-test="`asset-kind-${resume.id}`">{{ kindLabel(resume) }}</span>
          <em v-if="resume.currentVersion" class="asset-version">V{{ resume.currentVersion.versionNo }}</em>
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { Resume } from '../../types/resume'

defineProps<{
  items: Resume[]
  selectedId: number | null
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  select: [id: number]
  retry: []
  'create-blank': []
  import: []
}>()

function kindLabel(resume: Resume) {
  return resume.kind === 'JOB_EXPRESSION' ? '岗位表达' : '通用'
}
function kindClass(resume: Resume) {
  return resume.kind === 'JOB_EXPRESSION' ? 'expression' : 'general'
}
function updatedLabel(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return '时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '时间未知' : `${date.getMonth() + 1}月${date.getDate()}日更新`
}
</script>

<style scoped>
.asset-list{display:grid;gap:10px;align-content:start}
.state-card{min-height:220px;display:grid;place-content:center;justify-items:center;gap:12px;border:1px dashed var(--border-default);border-radius:var(--radius-panel);color:var(--muted);text-align:center;padding:24px}
.state-card strong{color:var(--ink);font-size:15px}
.state-card span{font-size:12.5px;line-height:1.6}
.state-card.error strong{color:var(--danger)}
.state-btn{border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--copy);padding:7px 14px;font-size:12.5px;cursor:pointer}
.state-btn:hover{background:var(--bg-hover)}
.state-primary{border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:8px 16px;font-size:13px;font-weight:600;cursor:pointer}
.asset-rows{margin:0;padding:0;list-style:none;display:grid;gap:6px}
.asset-row{display:flex;align-items:center;gap:10px;width:100%;text-align:left;border:1px solid transparent;border-radius:10px;background:transparent;padding:11px 12px;cursor:pointer}
.asset-row:hover{background:var(--bg-hover)}
.asset-row.selected{border-color:var(--brand-soft);background:var(--bg-selected)}
.asset-copy{flex:1;min-width:0;display:grid;gap:2px}
.asset-copy strong{font-size:13.5px;font-weight:600;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.asset-copy small{font-size:11px;color:var(--muted)}
.asset-kind{flex:0 0 auto;padding:2px 8px;border-radius:999px;font-size:10.5px;font-weight:650}
.asset-kind.general{background:var(--bg-subtle);color:var(--copy)}
.asset-kind.expression{background:var(--accent-soft);color:var(--brand)}
.asset-version{flex:0 0 auto;font-style:normal;font-size:11px;font-weight:700;color:var(--copy);font-variant-numeric:tabular-nums}
</style>
