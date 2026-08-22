<template>
  <section class="classification" data-test="knowledge-classification-panel">
    <div v-if="loading" class="cl-state" data-test="classification-loading">正在读取关联…</div>
    <div v-else-if="error && !classification" class="cl-state error" data-test="classification-error">
      <strong>关联读取失败</strong>
      <span>{{ error }}</span>
      <button type="button" class="text-btn" data-test="classification-retry" @click="$emit('reload')">重试</button>
    </div>

    <template v-else-if="classification">
      <div class="cl-row">
        <label>分类</label>
        <select
          class="cl-select"
          :value="classification.category?.id ?? ''"
          data-test="classification-category"
          :disabled="saving"
          @change="onCategory"
        >
          <option value="">无分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>

      <div class="cl-row">
        <label>标签</label>
        <div class="cl-tags">
          <span v-if="!classification.tags.length" class="cl-empty" data-test="classification-no-tags">暂无标签</span>
          <span v-for="t in classification.tags" :key="t.id" class="cl-tag" :data-test="'classification-tag-' + t.id">
            {{ t.name }}
            <button type="button" class="cl-remove" :data-test="'classification-remove-tag-' + t.id" :disabled="saving" @click="$emit('toggle-tag', t.id, false)">×</button>
          </span>
          <select
            class="cl-add-tag"
            :value="''"
            data-test="classification-add-tag"
            :disabled="saving"
            @change="onAddTag"
          >
            <option value="">+ 添加标签</option>
            <option v-for="t in availableTags" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
        </div>
      </div>

      <div class="cl-actions">
        <button type="button" class="text-btn" data-test="classification-new-category" @click="$emit('create-category')">新建分类</button>
        <button type="button" class="text-btn" data-test="classification-new-tag" @click="$emit('create-tag')">新建标签</button>
      </div>
      <p v-if="saving" class="cl-saving" data-test="classification-saving">保存中…</p>
      <p v-if="error" class="cl-error" data-test="classification-save-error">{{ error }}</p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KnowledgeCategory, KnowledgeDocumentClassification, KnowledgeTag } from '../../types/knowledge'

const props = defineProps<{
  classification: KnowledgeDocumentClassification | null
  loading: boolean
  saving: boolean
  error: string
  categories: KnowledgeCategory[]
  tags: KnowledgeTag[]
}>()

const emit = defineEmits<{
  (e: 'reload'): void
  (e: 'set-category', id: number | null): void
  (e: 'toggle-tag', tagId: number, add: boolean): void
  (e: 'create-category'): void
  (e: 'create-tag'): void
}>()

const availableTags = computed(() => {
  const used = new Set(props.classification?.tags.map((t) => t.id) ?? [])
  return props.tags.filter((t) => !used.has(t.id))
})

function onCategory(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('set-category', value === '' ? null : Number(value))
}

function onAddTag(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (value !== '') {
    emit('toggle-tag', Number(value), true)
  }
  // 重置以便重复选择
  ;(event.target as HTMLSelectElement).value = ''
}
</script>

<style scoped>
.classification{flex:none;padding:14px 26px 20px;border-top:1px solid var(--border-subtle)}
.cl-state{display:grid;gap:8px;justify-items:start;color:var(--muted);font-size:13px}
.cl-state strong{color:var(--ink);font-size:14px}
.cl-state.error strong{color:var(--danger)}
.cl-row{display:flex;align-items:center;gap:10px;margin-top:10px}
.cl-row label{flex:none;width:48px;font-size:13px;color:var(--copy)}
.cl-select{padding:7px 10px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:13px;min-width:150px}
.cl-tags{display:flex;flex-wrap:wrap;align-items:center;gap:6px}
.cl-tag{display:inline-flex;align-items:center;gap:4px;padding:3px 9px;border-radius:999px;background:var(--bg-subtle);color:var(--copy);font-size:12px}
.cl-remove{border:0;background:transparent;color:var(--muted);font-size:13px;cursor:pointer;padding:0 2px}
.cl-remove:hover{color:var(--danger)}
.cl-empty{font-size:12px;color:var(--muted)}
.cl-add-tag{padding:4px 8px;border:1px dashed var(--border-default);border-radius:999px;background:transparent;color:var(--copy);font-size:12px}
.cl-actions{display:flex;gap:14px;margin-top:12px}
.cl-saving{margin:8px 0 0;font-size:12px;color:var(--muted)}
.cl-error{margin:8px 0 0;font-size:12px;color:var(--danger)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
