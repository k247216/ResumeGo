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
            <span class="asset-accent" :style="{ background: accentColor(resume) }" aria-hidden="true"></span>
            <span class="asset-thumb" aria-hidden="true">
              <i class="thumb-name">{{ thumbName(resume) }}</i>
              <i class="thumb-line w80"></i>
              <i class="thumb-line w60"></i>
              <i class="thumb-line w72"></i>
            </span>
            <span class="asset-copy">
              <strong>{{ resume.title }}</strong>
              <small>V{{ resume.currentVersion?.versionNo ?? 1 }} · {{ preciseTime(resume) }}</small>
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

const ACCENT_PALETTE = ['#2f6fed', '#168b68', '#e07a1f', '#7c5cd6', '#0ea5b7', '#c25656', '#b7791f']

/** 按标题哈希取稳定识别色，同一简历颜色不漂移 */
function accentColor(resume: Resume) {
  let hash = 0
  for (const ch of resume.title) hash = (hash * 31 + ch.codePointAt(0)!) >>> 0
  return ACCENT_PALETTE[hash % ACCENT_PALETTE.length]
}

function thumbName(resume: Resume) {
  const basic = resume.currentVersion?.content.basicInfo
  return (basic?.name?.trim() || resume.title).slice(0, 2)
}

const groups = computed(() => {
  const general = props.items.filter((item) => item.kind !== 'JOB_EXPRESSION')
  const expression = props.items.filter((item) => item.kind === 'JOB_EXPRESSION')
  const result: Array<{ label: string; assets: Resume[] }> = []
  if (general.length) result.push({ label: '基础简历', assets: general })
  if (expression.length) result.push({ label: '岗位版本', assets: expression })
  return result
})

/** 时间精确到小时分钟：今天显示 HH:mm，更早显示 M月D日 HH:mm */
function preciseTime(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未知'
  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  const hhmm = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (sameDay) return `今天 ${hhmm}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${hhmm}`
}
</script>

<style scoped>
.asset-navigator{display:flex;flex-direction:column;min-height:0;height:100%}
.nav-filters{display:flex;gap:2px;padding:10px 14px 6px;flex:0 0 auto}
.nav-filter{border:0;background:none;padding:5px 9px;font:inherit;font-size:12px;font-weight:550;color:var(--muted);cursor:pointer;border-bottom:2px solid transparent}
.nav-filter:hover{color:var(--ink)}
.nav-filter.on{color:var(--brand);border-bottom-color:var(--brand);font-weight:650}
.nav-scroll{flex:1;min-height:0;overflow-y:auto;padding:0 10px 12px}
.nav-state{padding:22px 10px;color:var(--muted);font-size:12px;line-height:1.7;display:grid;gap:8px;justify-items:start}
.nav-state strong{color:var(--ink);font-size:13px}
.nav-state.error span{color:var(--danger)}
.nav-retry{border:1px solid var(--line,rgba(28,31,35,.18));border-radius:8px;background:transparent;color:var(--copy);padding:5px 11px;font-size:12px;cursor:pointer}
.nav-group{margin-top:12px}
.nav-group-head{display:flex;align-items:baseline;justify-content:space-between;padding:4px 8px;font-size:10.5px;font-weight:650;letter-spacing:.06em;color:var(--muted)}
.nav-group-head em{font-style:normal;font-variant-numeric:tabular-nums}
.asset-row{position:relative;display:flex;align-items:center;gap:10px;width:100%;text-align:left;border:1px solid transparent;border-radius:10px;background:none;padding:9px 10px 9px 12px;color:var(--ink);cursor:pointer;transition:background .13s ease-out,border-color .13s ease-out}
.asset-row:hover{background:var(--bg-hover)}
.asset-row.selected{border-color:var(--brand-soft);background:var(--bg-selected)}
.asset-accent{position:absolute;left:0;top:8px;bottom:8px;width:3px;border-radius:0 3px 3px 0;opacity:0}
.asset-row.selected .asset-accent,.asset-row:hover .asset-accent{opacity:1}
.asset-thumb{flex:0 0 auto;display:flex;flex-direction:column;gap:3px;width:34px;height:44px;padding:5px 6px;border:1px solid var(--border-subtle);border-radius:4px;background:#fff;box-shadow:0 1px 2px rgba(16,24,40,.06);overflow:hidden}
.thumb-name{font-size:7px;font-weight:700;color:var(--ink);line-height:1;white-space:nowrap;overflow:hidden}
.thumb-line{display:block;height:2px;border-radius:1px;background:rgba(28,31,35,.14)}
.thumb-line.w80{width:80%}
.thumb-line.w60{width:60%}
.thumb-line.w72{width:72%}
.asset-copy{flex:1;min-width:0;display:grid;gap:2px}
.asset-copy strong{font-size:12.5px;font-weight:550;color:var(--copy);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.asset-row.selected .asset-copy strong{font-weight:650;color:var(--ink)}
.asset-copy small{font-size:10.5px;color:var(--muted);font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.asset-marker{flex:0 0 auto;display:grid;place-items:center;width:18px;height:18px;border-radius:5px;background:var(--brand-soft);color:var(--brand);font-size:10px;font-weight:700}
.nav-foot{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-top:1px solid var(--border-subtle);flex:0 0 auto}
.local-note{display:inline-flex;align-items:center;gap:6px;font-size:10.5px;color:var(--muted)}
.local-note::before{content:'';width:6px;height:6px;border-radius:50%;background:var(--brand)}
.archived-link{border:0;background:none;padding:0;color:var(--muted);font-size:11px;cursor:pointer}
.archived-link:hover{color:var(--brand)}
</style>
