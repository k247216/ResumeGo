<template>
  <aside class="rail results" data-test="knowledge-search-results">
    <div v-if="loading && !results.length" class="rail-state">正在搜索…</div>
    <div v-else-if="errorMessage && !results.length" class="rail-state error">
      <strong>搜索失败</strong>
      <span>{{ errorMessage }}</span>
      <button type="button" class="text-btn" data-test="knowledge-search-retry" @click="$emit('retry')">重试</button>
    </div>
    <div v-else-if="!results.length" class="rail-state empty" data-test="knowledge-search-empty">
      <strong>没有匹配结果</strong>
      <span>换个关键词，或调整分类/标签筛选。</span>
    </div>
    <ul v-else class="rail-list">
      <li v-for="item in results" :key="item.document.id">
        <button
          type="button"
          class="rail-item"
          :class="{ selected: item.document.id === selectedId }"
          :data-test="'knowledge-search-item-' + item.document.id"
          @click="$emit('select', item.document.id)"
        >
          <strong>{{ item.document.title }}</strong>
          <span class="match">
            <em class="field" :class="item.matchedField === 'CONTENT' ? 'field-content' : 'field-title'">
              {{ item.matchedField === 'CONTENT' ? '正文命中' : '标题命中' }}
            </em>
            <em v-if="item.lineNumber != null" class="line">第 {{ item.lineNumber }} 行</em>
          </span>
          <span class="snippet">{{ item.snippet }}</span>
        </button>
      </li>
    </ul>
    <div v-if="loading && results.length" class="rail-refreshing">更新结果中…</div>
  </aside>
</template>

<script setup lang="ts">
import type { KnowledgeSearchItem } from '../../types/knowledge'

defineProps<{
  results: KnowledgeSearchItem[]
  selectedId: number | null
  loading: boolean
  errorMessage: string
}>()

defineEmits<{ (e: 'select', id: number): void; (e: 'retry'): void }>()
</script>

<style scoped>
.rail{width:280px;min-height:0;display:flex;flex-direction:column;border-right:1px solid var(--border-subtle);padding:14px 10px 20px;overflow-y:auto}
.rail-state{display:grid;gap:8px;justify-items:start;padding:18px 8px;color:var(--muted);font-size:13px}
.rail-state strong{color:var(--ink);font-size:14px}
.rail-state.error strong{color:var(--danger)}
.rail-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:2px}
.rail-item{display:grid;gap:4px;text-align:left;width:100%;padding:10px 12px;border:0;border-radius:12px;background:transparent;color:var(--copy);cursor:pointer}
.rail-item:hover{background:var(--bg-hover)}
.rail-item.selected{background:var(--bg-selected)}
.rail-item strong{font-size:14px;font-weight:600;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.match{display:flex;align-items:center;gap:6px;font-style:normal}
.field{display:inline-block;padding:1px 8px;border-radius:999px;font-size:11px}
.field-title{color:var(--brand);background:var(--brand-soft)}
.field-content{color:var(--copy);background:var(--bg-subtle)}
.line{font-style:normal;font-size:11px;color:var(--muted)}
.snippet{font-size:12px;color:var(--muted);display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;word-break:break-word}
.rail-refreshing{padding:8px;text-align:center;color:var(--muted);font-size:12px}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
