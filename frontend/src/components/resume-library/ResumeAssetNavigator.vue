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
      <button type="button" class="nav-filter-icon favorite-filter" :class="{ on: filter === 'favorites' }" data-test="filter-favorites" aria-label="查看收藏简历" :aria-pressed="filter === 'favorites'" @click="emit('update:filter', 'favorites')">
        <el-icon :size="13"><Star /></el-icon>
      </button>
      <button type="button" class="nav-filter-icon" data-test="filter-menu" aria-label="打开筛选" :aria-expanded="filterMenuOpen" @click="filterMenuOpen = !filterMenuOpen">
        <el-icon :size="12"><Filter /></el-icon>
      </button>
      <div v-if="filterMenuOpen" class="filter-menu" role="menu" data-test="filter-menu-panel">
        <button v-for="option in FILTER_MENU" :key="option.value" type="button" role="menuitem" :class="{ active: filter === option.value }" @click="chooseFilter(option.value)">{{ option.label }}</button>
        <button type="button" role="menuitem" :class="{ active: filter === 'archived' }" @click="chooseFilter('archived')">回收站</button>
      </div>
    </div>

    <div class="nav-scroll">
      <div v-if="loading && !items.length" class="nav-state">正在读取…</div>
      <div v-else-if="error && !items.length" class="nav-state error" data-test="resume-library-error">
        <span>{{ error }}</span>
        <button type="button" class="nav-retry" data-test="retry-load" @click="emit('retry')">重新加载</button>
      </div>
      <div v-else-if="!items.length" class="nav-state" :data-test="filter === 'archived' ? 'archived-empty' : 'resume-library-empty'">
        <strong>{{ filter === 'archived' ? '回收站为空' : filter === 'favorites' ? '还没有收藏的简历' : '还没有简历' }}</strong>
        <span>{{ filter === 'archived' ? '归档后的简历会出现在这里，可从右侧版本工具恢复。' : filter === 'favorites' ? '在简历标题旁点亮星标，常用版本会集中出现在这里。' : '新建空白简历或导入 Markdown 开始。' }}</span>
      </div>

      <template v-else>
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <div class="nav-group-head">
            <span class="group-label">{{ group.label }}</span>
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
            <span class="asset-thumb" aria-hidden="true">
              <span class="thumb-scale">
                <EditorPreviewPanel
                  :sections="buildSections(resume.currentVersion?.content ?? {})"
                  selected-section-id=""
                  version-label=""
                  :template-style="templateStyle(resume)"
                />
              </span>
            </span>            <span class="asset-copy">
              <strong>{{ displayTitle(resume) }}</strong>
              <small>{{ preciseTime(resume) }}</small>
            </span>
            <span v-if="isFavorite(resume)" class="asset-favorite" title="已收藏" aria-label="已收藏"><el-icon :size="12"><StarFilled /></el-icon></span>
            <span
              class="asset-status-dot"
              :class="{ recent: isRecentlyUpdated(resume) }"
              :title="isRecentlyUpdated(resume) ? '最近更新' : '较早更新'"
              aria-hidden="true"
            ></span>
          </button>
        </div>
        <p v-if="!groups.length" class="nav-state">当前过滤没有简历资产。</p>
      </template>
    </div>

    <button type="button" class="recycle-row" :class="{ selected: filter === 'archived' }" data-test="archived-entry" @click="emit('open-archived')">
      <el-icon :size="14"><Delete /></el-icon>
      <span>回收站</span>
      <em v-if="archivedCount">{{ archivedCount }}</em>
      <el-icon class="recycle-arrow" :size="11"><ArrowRight /></el-icon>
    </button>
    <button v-if="filter === 'archived' && items.length" type="button" class="clear-trash" data-test="clear-trash" @click="emit('clear-trash')">
      <el-icon :size="13"><Delete /></el-icon><span>清空回收站</span>
    </button>

    <div class="nav-foot">
      <span class="local-note">仅保存在本机</span>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Delete, Filter, ArrowRight, Star, StarFilled } from '@element-plus/icons-vue'
import EditorPreviewPanel from '../editor/EditorPreviewPanel.vue'
import { buildSections } from '../../composables/useResumeEditor'
import type { Resume } from '../../types/resume'
import type { ResumeLibraryKindFilter } from '../../composables/useResumeLibrary'
import { getResumeDisplayTitle, readResumeTemplate } from '../../utils/resumeTemplate'
import { isResumeFavorite, RESUME_FAVORITE_CHANGED } from '../../utils/resumeFavorite'

const props = defineProps<{
  items: Resume[]
  selectedId: number | null
  loading: boolean
  error: string
  filter: ResumeLibraryKindFilter
  archivedCount: number
}>()

const filterMenuOpen = ref(false)
const favoriteRevision = ref(0)

const emit = defineEmits<{
  select: [id: number]
  retry: []
  'update:filter': [filter: ResumeLibraryKindFilter]
  'open-archived': []
  'clear-trash': []
}>()

const FILTERS: Array<{ value: ResumeLibraryKindFilter; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'general', label: '基础' },
  { value: 'expression', label: '岗位版本' },
]
const FILTER_MENU: Array<{ value: ResumeLibraryKindFilter; label: string }> = [
  ...FILTERS,
  { value: 'favorites', label: '收藏' },
]

function onFavoriteChanged() {
  favoriteRevision.value += 1
}

onMounted(() => window.addEventListener(RESUME_FAVORITE_CHANGED, onFavoriteChanged))
onBeforeUnmount(() => window.removeEventListener(RESUME_FAVORITE_CHANGED, onFavoriteChanged))

function chooseFilter(value: ResumeLibraryKindFilter) {
  filterMenuOpen.value = false
  emit('update:filter', value)
}

function displayTitle(resume: Resume) {
  return getResumeDisplayTitle(resume)
}

function templateStyle(resume: Resume) {
  return readResumeTemplate(resume.id)
}

function isFavorite(resume: Resume) {
  void favoriteRevision.value
  return isResumeFavorite(resume.id)
}

const groups = computed(() => {
  if (props.filter === 'archived') return props.items.length ? [{ label: '回收站', assets: props.items }] : []
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

function isRecentlyUpdated(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return false
  const timestamp = new Date(value).getTime()
  return Number.isFinite(timestamp) && Date.now() - timestamp <= 3 * 24 * 60 * 60 * 1000
}
</script>

<style scoped>
.asset-navigator{display:flex;flex-direction:column;min-height:0;height:100%}
.nav-filters{position:relative;display:flex;align-items:center;gap:0;padding:9px 10px 5px;flex:0 0 auto;overflow:visible}
.nav-filter{border:0;background:none;padding:5px 7px;font:inherit;font-size:11px;font-weight:550;color:var(--muted);cursor:pointer;border-bottom:2px solid transparent;white-space:nowrap}
.nav-filter:hover{color:var(--ink)}
.nav-filter.on{color:var(--brand);border-bottom-color:var(--brand);font-weight:650}
.nav-filter-icon{margin-left:auto;display:grid;place-items:center;width:24px;height:24px;border:0;border-radius:6px;background:none;color:var(--muted);cursor:pointer}
.nav-filter-icon:hover,.nav-filter-icon.on{background:var(--bg-selected);color:var(--brand)}
.favorite-filter{position:relative;margin-left:3px}
.filter-menu{position:absolute;z-index:12;top:39px;right:10px;display:grid;min-width:112px;padding:4px;border:1px solid var(--border-subtle);border-radius:9px;background:var(--surface-solid,#fff);box-shadow:0 12px 28px rgba(16,24,40,.14)}.filter-menu button{border:0;border-radius:6px;background:transparent;padding:7px 9px;color:var(--copy);font:inherit;font-size:11px;text-align:left;cursor:pointer}.filter-menu button:hover,.filter-menu button.active{background:var(--bg-selected);color:var(--ink);font-weight:650}
.nav-scroll{flex:1;min-height:0;overflow-y:auto;padding:0 8px 10px}
.nav-state{padding:22px 10px;color:var(--muted);font-size:12px;line-height:1.7;display:grid;gap:8px;justify-items:start}
.nav-state strong{color:var(--ink);font-size:13px}
.nav-state.error span{color:var(--danger)}
.nav-retry{border:1px solid var(--line,rgba(28,31,35,.18));border-radius:8px;background:transparent;color:var(--copy);padding:5px 11px;font-size:12px;cursor:pointer}
.nav-group{margin-top:12px}
.nav-group-head{display:flex;align-items:baseline;justify-content:space-between;padding:4px 8px;font-size:10.5px;font-weight:650;letter-spacing:.06em;color:var(--muted)}
.group-label{display:inline-flex;align-items:center;gap:6px}
.nav-group-head em{font-style:normal;font-variant-numeric:tabular-nums}
.asset-row{position:relative;display:flex;align-items:center;gap:9px;width:100%;text-align:left;border:1px solid transparent;border-radius:10px;background:none;padding:8px 8px 8px 11px;color:var(--ink);cursor:pointer;transition:background .13s ease-out,border-color .13s ease-out}
.asset-row:hover{background:var(--bg-hover)}
.asset-row.selected{border-color:var(--brand-soft);background:var(--bg-selected)}
/* 缩略图：真实模版微缩渲染 */
.asset-thumb{position:relative;flex:0 0 auto;width:36px;height:51px;border:1px solid var(--border-subtle);border-radius:4px;background:#fff;box-shadow:0 1px 2px rgba(16,24,40,.08);overflow:hidden}
/* 缩略图按 A4 比例锁定视口，避免内容因编辑器默认 70% 缩放而偏到一侧。 */
.thumb-scale{position:absolute;top:0;left:0;width:794px;height:1123px;transform:scale(.04534);transform-origin:top left}
.thumb-scale :deep(.editor-preview-panel){width:794px;height:1123px;background:#fff}
.thumb-scale :deep(.preview-header){display:none}
.thumb-scale :deep(.preview-scroll){overflow:visible;background:#fff;padding:0;display:block}
.thumb-scale :deep(.paper-viewport){width:794px!important;min-height:1123px!important;height:1123px!important}
.thumb-scale :deep(.a4-paper){box-shadow:none;position:static;transform:none;left:auto;width:794px!important;min-height:1123px!important}
.asset-copy{flex:1;min-width:0;display:grid;gap:2px}
.asset-copy strong{font-size:12.5px;font-weight:550;color:var(--copy);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.asset-row.selected .asset-copy strong{font-weight:650;color:var(--ink)}
.asset-copy small{font-size:10.5px;color:var(--muted);font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.asset-favorite{display:grid;place-items:center;flex:0 0 auto;color:var(--brand);line-height:1}
.asset-status-dot{flex:0 0 auto;width:6px;height:6px;border-radius:50%;background:#a7aaa7;opacity:.72}
.asset-status-dot.recent{background:var(--brand);opacity:1}
.recycle-row{display:flex;align-items:center;gap:9px;width:100%;border:0;border-top:1px solid var(--border-subtle);background:none;padding:11px 14px;color:var(--copy);font-size:12px;font-weight:550;cursor:pointer}
.recycle-row:hover,.recycle-row.selected{background:var(--bg-selected);color:var(--ink)}
.recycle-row span{flex:1;text-align:left}
.recycle-row em{font-style:normal;font-size:10.5px;color:var(--muted)}
.recycle-arrow{color:var(--muted)}
.clear-trash{display:flex;align-items:center;gap:6px;width:100%;border:0;background:none;padding:7px 14px;color:var(--muted);font-size:10.5px;cursor:pointer;text-align:left}.clear-trash:hover{color:var(--danger);background:var(--bg-hover)}
.nav-foot{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-top:1px solid var(--border-subtle);flex:0 0 auto}
.local-note{display:inline-flex;align-items:center;gap:6px;font-size:10.5px;color:var(--muted)}
.local-note::before{content:'';width:6px;height:6px;border-radius:50%;background:var(--brand)}
.archived-link{display:inline-flex;align-items:center;gap:5px;border:0;background:none;padding:0;color:var(--muted);font-size:11px;cursor:pointer}
.archived-link:hover{color:var(--brand)}
</style>
