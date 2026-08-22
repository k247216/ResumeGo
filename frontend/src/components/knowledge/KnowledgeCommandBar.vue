<template>
  <header class="command-bar" data-test="knowledge-command-bar">
    <h1 class="bar-title">知识库</h1>
    <input
      ref="searchInput"
      :value="query"
      type="search"
      class="bar-search"
      placeholder="搜索资料标题或内容（Ctrl/Cmd+K）"
      data-test="knowledge-command-search"
      @input="onQuery"
      @keydown.esc.prevent="onQuery('')"
    />
    <div class="bar-actions">
      <KnowledgeImportControl
        :disabled="importing"
        :error="importErrorMessage"
        :primary="true"
        label="导入资料"
        @file-selected="$emit('import-file', $event)"
      />
      <button type="button" class="bar-btn" data-test="knowledge-command-create-note" @click="$emit('create-note')">新建笔记</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import KnowledgeImportControl from './KnowledgeImportControl.vue'

defineProps<{
  query: string
  importing: boolean
  importErrorMessage: string
}>()

const emit = defineEmits<{
  (e: 'update-query', q: string): void
  (e: 'import-file', file: File): void
  (e: 'create-note'): void
}>()

const searchInput = ref<HTMLInputElement | null>(null)

function onQuery(value: string | Event) {
  emit('update-query', typeof value === 'string' ? value : (value.target as HTMLInputElement).value)
}

function onGlobalKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchInput.value?.focus()
  }
}

onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))
</script>

<style scoped>
.command-bar{display:flex;align-items:center;gap:16px;padding:18px 24px 12px}
.bar-title{margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.bar-search{flex:1;min-width:0;max-width:520px;padding:9px 14px;border:1px solid var(--border-default);border-radius:11px;background:var(--bg-subtle);color:var(--ink);font-size:13px}
.bar-search:focus{outline:2px solid var(--brand);border-color:transparent}
.bar-actions{display:flex;align-items:center;gap:10px;margin-left:auto}
.bar-btn{padding:9px 16px;border:1px solid var(--border-default);border-radius:11px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer;white-space:nowrap}
.bar-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.bar-btn.primary:hover{opacity:.92;color:var(--action-fg)}
</style>
