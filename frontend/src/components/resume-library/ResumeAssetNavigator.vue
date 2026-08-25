<template>
  <nav class="asset-navigator" data-test="resume-asset-navigator">
    <div class="nav-filters" role="tablist" aria-label="简历过滤">
      <button
        v-for="option in FILTERS"
        :key="option.value"
        type="button"
        class="nav-filter"
        :class="{ on: filter === option.value }"
        :data-test="`filter-${option.value}`"
        :aria-selected="filter === option.value"
        @click="emit('update:filter', option.value)"
      >{{ option.label }}</button>
    </div>

    <div class="nav-scroll">
      <div v-if="loading && !items.length" class="nav-state">正在读取…</div>
      <div v-else-if="error && !items.length" class="nav-state error" data-test="resume-library-error">
        <span>{{ error }}</span>
        <button type="button" class="nav-retry" data-test="retry-load" @click="emit('retry')">重新加载</button>
      </div>
      <div v-else-if="!items.length" class="nav-state" data-test="resume-library-empty">
        <strong>还没有简历</strong>
        <span>新建空白简历或导入 Markdown 开始。</span>
      </div>

      <template v-else>
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <div class="nav-group-head">
            <span>{{ group.label }}</span>
            <em>{{ group.assets.length }}</em>
          </div>
          <button
            v-for="resume in group.assets"
            :key="resume.id"
            type="button"
            class="asset-row"
            :class="{ selected: resume.id === selectedId }"
            :data-test="`asset-row-${resume.id}`"
            :aria-current="resume.id === selectedId ? 'true' : undefined"
            @click="emit('select', resume.id)"
          >
            <span class="asset-icon" aria-hidden="true"><el-icon :size="13"><Document /></el-icon></span>
            <span class="asset-copy">
              <strong>{{ resume.title }}</strong>
              <small>V{{ resume.currentVersion?.versionNo ?? 1 }} · {{ updatedLabel(resume) }}</small>
            </span>
            <span
              v-if="resume.kind === 'JOB_EXPRESSION'"
              class="asset-marker"
              :data-test="`asset-kind-${resume.id}`"
              title="岗位表达副本"
            >岗</span>
          </button>
        </div>
        <p v-if="!groups.length" class="nav-state">当前过滤没有简历资产。</p>
      </template>
    </div>

    <div class="nav-foot">
      <span class="local-note">仅保存在本机</span>
      <button type="button" class="archived-link" data-test="archived-entry" @click="emit('open-archived')">
        归档{{ archivedCount ? ` (${archivedCount})` : '' }}
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'
import type { Resume } from '../../types/resume'
import type { ResumeLibraryKindFilter } from '../../composables/useResumeLibrary'

const props = defineProps<{
  items: Resume[]
  selectedId: number | null
  loading: boolean
  error: string
  filter: ResumeLibraryKindFilter
  archivedCount: number
}>()

const emit = defineEmits<{
  select: [id: number]
  retry: []
  'update:filter': [filter: ResumeLibraryKindFilter]
  'open-archived': []
}>()

const FILTERS: Array<{ value: ResumeLibraryKindFilter; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'general', label: '通用' },
  { value: 'expression', label: '岗位版本' },
]

const groups = computed(() => {
  const general = props.items.filter((item) => item.kind !== 'JOB_EXPRESSION')
  const expression = props.items.filter((item) => item.kind === 'JOB_EXPRESSION')
  const result: Array<{ label: string; assets: Resume[] }> = []
  if (general.length) result.push({ label: '基础简历', assets: general })
  if (expression.length) result.push({ label: '岗位版本', assets: expression })
  return result
})

function updatedLabel(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return '未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '未知' : `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>

<style scoped>
.asset-navigator{display:flex;flex-direction:column;min-height:0;height:100%}
.nav-filters{display:flex;gap:2px;padding:10px 12px 6px;flex:0 0 auto}
.nav-filter{border:0;background:none;padding:5px 9px;font:inherit;font-size:12px;font-weight:550;color:var(--muted);cursor:pointer;border-bottom:2px solid transparent}
.nav-filter:hover{color:var(--ink)}
.nav-filter.on{color:var(--brand);border-bottom-color:var(--brand);font-weight:650}
.nav-scroll{flex:1;min-height:0;overflow-y:auto;padding:0 8px 12px}
.nav-state{padding:22px 10px;color:var(--muted);font-size:12px;line-height:1.7;display:grid;gap:8px;justify-items:start}
.nav-state strong{color:var(--ink);font-size:13px}
.nav-state.error span{color:var(--danger)}
.nav-retry{border:1px solid var(--line,rgba(28,31,35,.18));border-radius:8px;background:transparent;color:var(--copy);padding:5px 11px;font-size:12px;cursor:pointer}
.nav-group{margin-top:10px}
.nav-group-head{display:flex;align-items:baseline;justify-content:space-between;padding:4px 8px;font-size:10.5px;font-weight:650;letter-spacing:.06em;color:var(--muted)}
.nav-group-head em{font-style:normal;font-variant-numeric:tabular-nums}
.asset-row{display:flex;align-items:center;gap:9px;width:100%;text-align:left;border:0;border-left:2px solid transparent;border-radius:0 8px 8px 0;background:none;padding:8px 9px;color:var(--ink);cursor:pointer}
.asset-row:hover{background:var(--bg-hover)}
.asset-row.selected{border-left-color:var(--brand);background:var(--bg-selected)}
.asset-row.selected .asset-copy strong{font-weight:650;color:var(--ink)}
.asset-icon{flex:0 0 auto;display:grid;width:26px;height:26px;place-items:center;border-radius:6px;background:var(--bg-subtle);color:var(--muted)}
.asset-copy{flex:1;min-width:0;display:grid;gap:1px}
.asset-copy strong{font-size:12.5px;font-weight:550;color:var(--copy);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.asset-copy small{font-size:10.5px;color:var(--muted);font-variant-numeric:tabular-nums}
.asset-marker{flex:0 0 auto;display:grid;place-items:center;width:18px;height:18px;border-radius:5px;background:var(--brand-soft);color:var(--brand);font-size:10px;font-weight:700}
.nav-foot{display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-top:1px solid var(--border-subtle);flex:0 0 auto}
.local-note{font-size:10.5px;color:var(--muted)}
.archived-link{border:0;background:none;padding:0;color:var(--muted);font-size:11px;cursor:pointer}
.archived-link:hover{color:var(--brand)}
</style>
