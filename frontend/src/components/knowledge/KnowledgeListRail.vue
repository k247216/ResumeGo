<template>
  <aside class="rail" data-test="knowledge-rail">
    <div v-if="loading && !documents.length" class="rail-state">正在读取…</div>
    <div v-else-if="errorMessage && !documents.length" class="rail-state error">
      <strong>无法读取知识库</strong>
      <span>{{ errorMessage }}</span>
      <button type="button" class="text-btn" data-test="knowledge-rail-retry" @click="$emit('retry')">重试</button>
    </div>
    <div v-else-if="!documents.length" class="rail-state empty">
      <strong>还没有资料</strong>
      <span>新建一条笔记，或导入 .md/.txt 文件。</span>
      <button type="button" class="text-btn" data-test="knowledge-rail-create" @click="$emit('create-note')">新建笔记</button>
    </div>
    <ul v-else class="rail-list">
      <li v-for="doc in documents" :key="doc.id">
        <button
          type="button"
          class="rail-item"
          :class="{ selected: doc.id === selectedId }"
          :data-test="'knowledge-rail-item-' + doc.id"
          @click="$emit('select', doc.id)"
        >
          <strong>{{ doc.title }}</strong>
          <span>{{ doc.sourceType === 'FILE' ? '文件' : '笔记' }}</span>
          <em class="status" :class="statusTone(doc.processingStatus)">{{ knowledgeStatusLabel(doc.processingStatus) }}</em>
        </button>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import type { KnowledgeDocument, KnowledgeProcessingStatus } from '../../types/knowledge'
import { knowledgeStatusLabel } from './status'

defineProps<{
  documents: KnowledgeDocument[]
  selectedId: number | null
  loading: boolean
  errorMessage: string
}>()

defineEmits<{
  (e: 'select', id: number): void
  (e: 'retry'): void
  (e: 'create-note'): void
}>()

function statusTone(status: KnowledgeProcessingStatus): string {
  if (status === 'FAILED') return 'tone-danger'
  if (status === 'COMPLETED') return 'tone-ok'
  if (status === 'PENDING' || status === 'RUNNING') return 'tone-busy'
  return 'tone-idle'
}
</script>

<style scoped>
.rail{width:280px;min-height:0;display:flex;flex-direction:column;border-right:1px solid var(--border-subtle);padding:14px 10px 20px;overflow-y:auto}
.rail-state{display:grid;gap:8px;justify-items:start;padding:18px 8px;color:var(--muted);font-size:13px}
.rail-state strong{color:var(--ink);font-size:14px}
.rail-state.error strong{color:var(--danger)}
.rail-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:2px}
.rail-item{display:grid;gap:2px;text-align:left;width:100%;padding:10px 12px;border:0;border-radius:12px;background:transparent;color:var(--copy);cursor:pointer}
.rail-item:hover{background:var(--bg-hover)}
.rail-item.selected{background:var(--bg-selected)}
.rail-item strong{font-size:14px;font-weight:600;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.rail-item span{font-size:12px;color:var(--muted)}
.rail-item em{display:flex;align-items:center;gap:6px;font-style:normal;font-size:12px;color:var(--copy)}
.status{display:inline-block;margin-top:2px;padding:1px 8px;border-radius:999px;font-size:11px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
