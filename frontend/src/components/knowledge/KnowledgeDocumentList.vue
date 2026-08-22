<template>
  <section class="doc-list" :class="{ collapsed: collapsed }" data-test="knowledge-doc-list">
    <template v-if="!collapsed">
      <div class="list-head">
        <strong>{{ scopeLabel }}</strong>
        <span class="list-actions">
          <button type="button" class="list-sort" data-test="doc-list-sort" @click="toggleSort">
            更新时间 {{ sortDesc ? '↓' : '↑' }}
          </button>
          <button type="button" class="list-collapse" data-test="doc-list-collapse" aria-label="收起资料列表" @click="$emit('toggle-collapse')">«</button>
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
            :class="{ selected: item.document.id === selectedId }"
            :data-test="'doc-row-' + item.document.id"
            @click="$emit('select', item.document.id)"
          >
            <el-icon class="row-icon" :data-type="item.document.sourceType.toLowerCase()">
              <component :is="iconOf(item.document)" />
            </el-icon>
            <span class="row-main">
              <strong>{{ item.document.title }}</strong>
              <span class="row-meta">{{ metaOf(item.document) }}</span>
            </span>
            <span class="row-location">{{ locationOf(item.document) }}</span>
            <span v-if="item.snippet" class="row-snippet">{{ item.snippet }}</span>
            <span class="row-status" :class="statusTone(item.document.processingStatus)">{{ statusLabel(item.document.processingStatus) }}</span>
            <span v-if="item.document.processingStatus === 'FAILED'" class="row-action" data-test="doc-row-retry" @click.stop="$emit('retry-doc', item.document.id)">重试</span>
          </button>
          </li>
        </ul>
      </template>
    </template>
    <button v-else type="button" class="list-restore" data-test="doc-list-restore" aria-label="展开资料列表" @click="$emit('toggle-collapse')">»</button>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, Notebook } from '@element-plus/icons-vue'
import { knowledgeStatusLabel } from './status'
import type { KnowledgeDocument, KnowledgeSearchItem } from '../../types/knowledge'

const props = defineProps<{
  documents: KnowledgeDocument[]
  results: KnowledgeSearchItem[]
  hasSearch: boolean
  selectedId: number | null
  loading: boolean
  errorMessage: string
  collapsed: boolean
  scopeLabel: string
  classificationByDocumentId: Record<number, { category: { id: number; name: string } | null; tags: { id: number; name: string }[] }>
  categoryPaths: Record<number, string>
}>()

const sortDesc = ref(true)

function toggleSort() {
  sortDesc.value = !sortDesc.value
}

defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'select', id: number): void
  (e: 'retry'): void
  (e: 'retry-search'): void
  (e: 'retry-doc', id: number): void
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
  return Document
}

function typeLabel(doc: KnowledgeDocument): string {
  if (doc.sourceType === 'NOTE') return '笔记'
  const extension = doc.sourceFile?.split('.').pop()?.toLowerCase()
  if (extension === 'md') return 'Markdown'
  if (extension === 'txt') return 'TXT'
  return '文件'
}

function metaOf(doc: KnowledgeDocument): string {
  return `${typeLabel(doc)} · ${new Date(doc.updatedAt).toLocaleString()}`
}

function locationOf(doc: KnowledgeDocument): string {
  const classification = props.classificationByDocumentId[doc.id]
  if (classification?.category) {
    return props.categoryPaths[classification.category.id] ?? classification.category.name
  }
  if (classification?.tags.length) {
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
.doc-list{width:330px;min-width:0;border-right:1px solid var(--border-subtle);display:flex;flex-direction:column;min-height:0}
.doc-list.collapsed{width:44px}
.list-head{display:flex;align-items:center;justify-content:space-between;padding:12px 14px 8px}
.list-head strong{font-size:12px;font-weight:600;color:var(--muted)}
.list-actions{display:inline-flex;align-items:center;gap:8px}
.list-sort{border:0;background:transparent;color:var(--muted);font-size:11px;cursor:pointer;padding:2px 4px}
.list-sort:hover{color:var(--brand)}
.list-collapse{border:0;background:transparent;color:var(--copy);cursor:pointer;font-size:14px;padding:0 4px}
.list-rows{list-style:none;margin:0;padding:0 8px 16px;overflow-y:auto;display:flex;flex-direction:column;gap:1px}
.row{display:flex;align-items:flex-start;gap:8px;width:100%;text-align:left;padding:8px 10px;border:0;border-radius:10px;background:transparent;cursor:pointer}
.row:hover{background:var(--bg-hover)}
.row.selected{background:var(--bg-selected)}
.row-icon{flex:none;font-size:15px;color:var(--muted)}
.row-main{flex:1;min-width:0;display:grid;gap:2px}
.row-main strong{font-size:13px;font-weight:600;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-meta{font-size:11px;color:var(--muted)}
.row-location{font-size:11px;color:var(--brand);max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-snippet{font-size:11px;color:var(--muted);max-width:130px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row-status{flex:none;font-size:10px;padding:1px 7px;border-radius:999px;margin-top:2px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.row-action{flex:none;font-size:11px;color:var(--brand);background:transparent;border:0;cursor:pointer;margin-top:1px}
.list-state{display:grid;gap:8px;justify-items:start;padding:18px 14px;color:var(--muted);font-size:13px}
.list-state.error strong,.list-state.error{color:var(--danger)}
.list-state.empty{color:var(--muted)}
.stale-search{display:flex;align-items:center;justify-content:space-between;margin:0 10px 6px;padding:7px 9px;border-radius:8px;background:var(--danger-soft);color:var(--danger);font-size:11px}
.list-restore{border:0;background:transparent;color:var(--copy);font-size:14px;cursor:pointer;width:100%;padding:10px 0}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
