<template>
  <div class="search-bar" data-test="knowledge-search-bar">
    <input
      :value="query"
      type="search"
      class="search-input"
      placeholder="搜索标题或正文…"
      data-test="knowledge-search-input"
      @input="onInput"
    />
    <select
      class="filter-select"
      :value="categoryId ?? ''"
      data-test="knowledge-search-category"
      @change="onCategory"
    >
      <option value="">全部分类</option>
      <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
    </select>
    <select
      class="filter-select"
      :value="tagId ?? ''"
      data-test="knowledge-search-tag"
      @change="onTag"
    >
      <option value="">全部标签</option>
      <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
import type { KnowledgeCategory, KnowledgeTag } from '../../types/knowledge'

defineProps<{
  query: string
  categoryId: number | null
  tagId: number | null
  categories: KnowledgeCategory[]
  tags: KnowledgeTag[]
}>()

const emit = defineEmits<{
  (e: 'update-query', q: string): void
  (e: 'update-category', id: number | null): void
  (e: 'update-tag', id: number | null): void
}>()

function onInput(event: Event) {
  emit('update-query', (event.target as HTMLInputElement).value)
}

function onCategory(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update-category', value === '' ? null : Number(value))
}

function onTag(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update-tag', value === '' ? null : Number(value))
}
</script>

<style scoped>
.search-bar{display:flex;align-items:center;gap:8px;padding:0 2px 12px}
.search-input{flex:1;min-width:0;max-width:340px;padding:8px 12px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:13px}
.search-input:focus{outline:2px solid var(--brand);border-color:transparent}
.filter-select{padding:8px 10px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--copy);font-size:13px;max-width:150px}
</style>
