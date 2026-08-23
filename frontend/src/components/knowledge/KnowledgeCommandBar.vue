<template>
  <header class="command-bar" data-test="knowledge-command-bar">
    <div class="bar-identity">
      <h1 class="bar-title"><span class="bar-title-icon" aria-hidden="true"><BookIcon :size="17" /></span><span>知识库</span></h1>
      <div v-if="showNavigatorRestore || showListRestore || showInspectorRestore" class="restore-actions" aria-label="恢复面板">
        <button v-if="showNavigatorRestore" type="button" data-test="knowledge-restore-navigator" aria-label="展开资料库导航" @click="$emit('restore-navigator')"><el-icon><Menu /></el-icon></button>
        <button v-if="showListRestore" type="button" data-test="knowledge-restore-list" aria-label="展开资料列表" @click="$emit('restore-list')"><el-icon><Tickets /></el-icon></button>
        <button v-if="showInspectorRestore" type="button" data-test="knowledge-restore-inspector" aria-label="展开来源与属性" @click="$emit('restore-inspector')"><el-icon><Operation /></el-icon></button>
      </div>
    </div>
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
      <button type="button" class="bar-btn" data-test="knowledge-command-create-note" @click="$emit('create-note')"><el-icon><EditPen /></el-icon><span>新建笔记</span></button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { EditPen, Menu, Operation, Tickets } from '@element-plus/icons-vue'
import BookIcon from '../BookIcon.vue'
import KnowledgeImportControl from './KnowledgeImportControl.vue'

defineProps<{
  query: string
  importing: boolean
  importErrorMessage: string
  showNavigatorRestore: boolean
  showListRestore: boolean
  showInspectorRestore: boolean
}>()

const emit = defineEmits<{
  (e: 'update-query', q: string): void
  (e: 'import-file', file: File): void
  (e: 'create-note'): void
  (e: 'restore-navigator'): void
  (e: 'restore-list'): void
  (e: 'restore-inspector'): void
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
.command-bar{display:flex;align-items:center;gap:18px;min-height:58px;padding:8px 18px;border-bottom:1px solid var(--border-subtle);background:var(--surface-solid)}
.bar-identity{display:flex;align-items:center;gap:10px}
.bar-title{display:inline-flex;align-items:center;gap:10px;margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.bar-title-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;background:var(--brand-soft);color:var(--brand)}
.restore-actions{display:inline-flex;gap:3px}.restore-actions button{display:grid;width:28px;height:28px;place-items:center;border:0;border-radius:7px;background:transparent;color:var(--muted);cursor:pointer}.restore-actions button:hover{background:var(--bg-hover);color:var(--ink)}
.bar-search{flex:1;min-width:0;max-width:520px;padding:9px 14px;border:1px solid var(--border-default);border-radius:11px;background:var(--bg-subtle);color:var(--ink);font-size:13px}
.bar-search:focus{outline:2px solid var(--brand);border-color:transparent}
.bar-actions{display:flex;align-items:center;gap:10px;margin-left:auto}
.bar-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 13px;border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer;white-space:nowrap}
.bar-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.bar-btn.primary:hover{opacity:.92;color:var(--action-fg)}
</style>
