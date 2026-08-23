<template>
  <section class="doc-list" data-test="knowledge-doc-list">
      <div class="list-head">
        <strong>{{ scopeLabel }}</strong>
        <span class="list-actions">
          <template v-if="selectedDocIds.size">
            <span class="bulk-count" data-test="bulk-count">已选 {{ selectedDocIds.size }} 项</span>
            <button type="button" class="list-sort danger" data-test="bulk-delete" @click="$emit('bulk-delete')">删除</button>
            <button type="button" class="list-sort" data-test="bulk-clear" @click="$emit('clear-selection')">取消</button>
          </template>
          <template v-else>
            <button type="button" class="list-sort" data-test="doc-list-sort" @click="toggleSort">
              更新时间 {{ sortDesc ? '↓' : '↑' }}
            </button>
          </template>
          <button type="button" class="list-collapse" data-test="doc-list-collapse" aria-label="收起资料列表" @click="$emit('close')"><el-icon><ArrowLeft /></el-icon></button>
        </span>
      </div>
      <div v-if="hasSearch && loading && !results.length" class="list-state">正在搜索…</div>
      <div v-else-if="hasSearch && errorMessage && !results.length" class="list-state error">
        <span>搜索失败：{{ errorMessage }}</span>
        <button type="button" class="text-btn" data-test="doc-list-search-retry" @click="$emit('retry-search')">重试</button>
      </div>
      <div v-else-if="hasSearch && !results.length" class="list-state empty" data-test="doc-list-search-empty">没有匹配结果</div>
      <div v-else-if="!hasSearch && loading && !documents.length" class="list-state">正在读取…</div>
      <div v-else-if="!hasSearch && errorMessage && !documents.length" class="list-state error">
        <span>{{ errorMessage }}</span>
        <button type="button" class="text-btn" data-test="doc-list-retry" @click="$emit('retry')">重试</button>
      </div>
      <div v-else-if="!hasSearch && !documents.length" class="list-state empty" data-test="doc-list-empty">
        还没有资料。导入 .md/.txt 或新建笔记开始。
      </div>
      <template v-else>
        <div v-if="hasSearch && errorMessage" class="stale-search" data-test="doc-list-search-stale">
          搜索失败，以下为上一次结果
          <button type="button" class="text-btn" @click="$emit('retry-search')">重试</button>
        </div>
        <ul class="list-rows">
          <li v-for="item in rows" :key="item.document.id">
          <button
            type="button"
            class="row"
            :class="{ selected: item.document.id === selectedId, checked: selectedDocIds.has(item.document.id) }"
            :data-test="'doc-row-' + item.document.id"
            @click="$emit('select', item.document.id)"
          >
            <span
              class="row-check"
              :class="{ on: selectedDocIds.has(item.document.id) }"
              :data-test="'doc-check-' + item.document.id"
              role="checkbox"
              :aria-checked="selectedDocIds.has(item.document.id)"
              @click.stop="$emit('toggle-select', item.document.id)"
            ><el-icon v-if="selectedDocIds.has(item.document.id)" :size="12"><Check /></el-icon></span>
            <span class="file-visual" :class="'type-' + iconType(item.document)">
              <el-icon class="row-icon" :data-type="iconType(item.document)">
                <component :is="iconOf(item.document)" />
              </el-icon>
              <span
                class="file-type"
                :class="'type-' + iconType(item.document)"
                :data-test="'file-type-' + item.document.id"
              >{{ shortTypeLabel(item.document) }}</span>
            </span>
            <span class="row-main">
              <strong>{{ item.document.title }}</strong>
              <span class="row-meta">{{ metaOf(item.document) }}</span>
            </span>
            <span class="row-location">{{ locationOf(item.document) }}</span>
            <span v-if="item.snippet" class="row-snippet">{{ item.snippet }}</span>
            <span class="row-side">
              <span class="row-status" :class="statusTone(item.document.processingStatus)">{{ statusLabel(item.document.processingStatus) }}</span>
              <span v-if="item.document.processingStatus === 'FAILED'" class="row-action" data-test="doc-row-retry" @click.stop="$emit('retry-doc', item.document.id)">重试</span>
            </span>
          </button>
          </li>
        </ul>
      </template>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowLeft, Check, Document, DocumentCopy, Notebook } from '@element-plus/icons-vue'
import { knowledgeStatusLabel } from './status'
import type { KnowledgeDocument, KnowledgeSearchItem } from '../../types/knowledge'

const props = withDefaults(defineProps<{
  documents: KnowledgeDocument[]
  results: KnowledgeSearchItem[]
  hasSearch: boolean
  selectedId: number | null
  loading: boolean
  errorMessage: string
  scopeLabel: string
  classificationByDocumentId: Record<number, { category: { id: number; name: string } | null; tags: { id: number; name: string }[] }>
  categoryPaths: Record<number, string>
  activeTagId?: number | null
  selectedDocIds?: Set<number>
}>(), {
  activeTagId: null,
  selectedDocIds: () => new Set<number>(),
})

const sortDesc = ref(true)

function toggleSort() {
  sortDesc.value = !sortDesc.value
}

defineEmits<{
  (e: 'close'): void
  (e: 'select', id: number): void
  (e: 'retry'): void
  (e: 'retry-search'): void
  (e: 'retry-doc', id: number): void
  (e: 'toggle-select', id: number): void
  (e: 'clear-selection'): void
  (e: 'bulk-delete'): void
}>()

const rows = computed(() => {
  const rows = props.hasSearch
    ? props.results.map((r) => ({ document: r.document, snippet: r.snippet }))
    : props.documents.map((d) => ({ document: d, snippet: null as string | null }))
  if (!props.hasSearch) {
    rows.sort((a, b) => {
      const cmp = b.document.updatedAt.localeCompare(a.document.updatedAt)
      return sortDesc.value ? cmp : -cmp
    })
  }
  return rows
})

function iconOf(doc: KnowledgeDocument) {
  if (doc.sourceType === 'NOTE') return Notebook
  if (doc.sourceExtension?.toLowerCase() === 'md') return DocumentCopy
  return Document
}

function iconType(doc: KnowledgeDocument): string {
  if (doc.sourceType === 'NOTE') return 'note'
  const ext = doc.sourceExtension?.toLowerCase()
  return !ext || ext === 'unknown' ? 'file' : ext
}

function typeLabel(doc: KnowledgeDocument): string {
  if (doc.sourceType === 'NOTE') return '笔记'
  const extension = doc.sourceExtension?.toLowerCase()
  if (!extension || extension === 'unknown') return '文件'
  if (extension === 'md') return 'Markdown'
  if (extension === 'txt') return 'TXT'
  return extension.toUpperCase()
}

function shortTypeLabel(doc: KnowledgeDocument): string {
  if (doc.sourceType === 'NOTE') return 'NOTE'
  const extension = doc.sourceExtension?.toLowerCase()
  if (!extension || extension === 'unknown') return 'FILE'
  if (extension === 'md') return 'MD'
  return extension.toUpperCase().slice(0, 4)
}

function metaOf(doc: KnowledgeDocument): string {
  return `${typeLabel(doc)} · ${new Date(doc.updatedAt).toLocaleString()}`
}

function locationOf(doc: KnowledgeDocument): string {
  const classification = props.classificationByDocumentId[doc.id]
  if (classification?.category) {
    return props.categoryPaths[classification.category.id] ?? classification.category.name
  }
  // 按标签浏览时范围标签已在列表头显示，行内不再重复标签，避免重叠
  if (props.activeTagId == null && classification?.tags.length) {
    return '#' + classification.tags[0].name
  }
  return ''
}

function statusLabel(status: KnowledgeDocument['processingStatus']): string {
  return knowledgeStatusLabel(status)
}

function statusTone(status: KnowledgeDocument['processingStatus']): string {
  if (status === 'FAILED') return 'tone-danger'
  if (status === 'COMPLETED') return 'tone-ok'
  if (status === 'PENDING' || status === 'RUNNING') return 'tone-busy'
  return 'tone-idle'
}
</script>

<style scoped>
.doc-list{width:320px;min-width:0;border-right:1px solid var(--border-subtle);display:flex;flex-direction:column;min-height:0;background:var(--surface-solid)}
.list-head{display:flex;align-items:center;justify-content:space-between;padding:12px 14px 8px}
.list-head strong{font-size:12px;font-weight:650;color:var(--ink)}
.list-actions{display:inline-flex;align-items:center;gap:8px}
.list-sort{border:0;background:transparent;color:var(--muted);font-size:11px;cursor:pointer;padding:2px 4px}
.list-sort:hover{color:var(--brand)}
.list-collapse{border:0;background:transparent;color:var(--copy);cursor:pointer;font-size:14px;padding:0 4px}
.list-rows{list-style:none;margin:0;padding:0;overflow-y:auto;display:flex;flex-direction:column}
.row{display:grid;grid-template-columns:34px minmax(0,1fr) auto;grid-template-rows:auto auto;align-items:center;column-gap:10px;row-gap:2px;width:100%;text-align:left;padding:9px 14px;border:0;border-bottom:1px solid var(--border-subtle);border-radius:0;background:transparent;cursor:pointer}
.file-visual{grid-column:1;grid-row:1/3;align-self:start;margin-top:2px}
.row-main{grid-column:2;grid-row:1}
.row-location,.row-snippet{grid-column:2;grid-row:2;font-size:11px;color:var(--muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-side{grid-column:3;grid-row:1/3;display:flex;flex-direction:column;align-items:flex-end;gap:4px}
.row{position:relative}
.row-check{position:absolute;left:6px;top:50%;transform:translateY(-50%);display:grid;width:16px;height:16px;place-items:center;border:1px solid var(--border-strong);border-radius:4px;background:var(--surface-solid);color:#fff;cursor:pointer;opacity:0;transition:opacity .12s ease;z-index:2}
.row:hover .row-check,.row.checked .row-check{opacity:1}
.row-check.on{opacity:1;background:var(--brand);border-color:var(--brand)}
.bulk-count{font-size:11.5px;color:var(--brand);font-weight:600}
.list-sort.danger{color:var(--danger)}
.list-sort.danger:hover{color:var(--danger)}
.row.checked{background:var(--brand-soft)}
.row:hover{background:var(--bg-hover)}
.row.selected{background:var(--bg-selected);box-shadow:inset 2px 0 0 var(--brand)}
.file-visual{position:relative;display:grid;width:28px;height:30px;place-items:start center;color:var(--type-color,var(--copy))}
.row-icon{font-size:20px;color:currentColor}
.file-type{position:absolute;left:50%;bottom:2px;min-width:24px;max-width:34px;transform:translateX(-50%);padding:1px 3px;border-radius:3px;background:var(--type-color,var(--copy));color:#fff;font-size:7.5px;font-weight:750;line-height:1.1;text-align:center;letter-spacing:-.02em;box-sizing:border-box}
.file-visual.type-note{--type-color:var(--brand)}
.file-visual.type-md{--type-color:#36a853}
.file-visual.type-txt{--type-color:#c48220}
.file-visual.type-pdf{--type-color:#e34b45}
.file-visual.type-doc,.file-visual.type-docx{--type-color:#3978d4}
.file-visual.type-ppt,.file-visual.type-pptx{--type-color:#e77532}
.file-visual.type-xls,.file-visual.type-xlsx{--type-color:#23875a}
.row-main{flex:1;min-width:0;display:grid;gap:2px}
.row-main strong{font-size:13px;font-weight:600;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-meta{font-size:11px;color:var(--copy)}

.row-status{flex:none;font-size:10px;padding:1px 7px;border-radius:999px;line-height:1.5}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.row-action{flex:none;font-size:11px;color:var(--brand);background:transparent;border:0;cursor:pointer;padding:0}
.list-state{display:grid;gap:8px;justify-items:start;padding:18px 14px;color:var(--muted);font-size:13px}
.list-state.error strong,.list-state.error{color:var(--danger)}
.list-state.empty{color:var(--muted)}
.stale-search{display:flex;align-items:center;justify-content:space-between;margin:0 10px 6px;padding:7px 9px;border-radius:8px;background:var(--danger-soft);color:var(--danger);font-size:11px}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
