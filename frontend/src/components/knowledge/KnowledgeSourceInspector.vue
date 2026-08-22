<template>
  <aside class="inspector" data-test="knowledge-inspector">
    <div class="inspector-head">
      <strong>来源与属性</strong>
      <button type="button" class="inspector-close" data-test="inspector-close" aria-label="关闭来源检查器" @click="$emit('close')">
        <el-icon :size="14"><Close /></el-icon>
      </button>
    </div>

    <div v-if="!document" class="inspector-empty">选择一份资料查看属性。</div>
    <template v-else>
      <section class="block">
        <h3>分类</h3>
        <p v-if="classification?.category" class="category-path" data-test="inspector-category-path">
          {{ categoryPaths[classification.category.id] ?? classification.category.name }}
        </p>
        <KnowledgeSelect
          :model-value="String(classification?.category?.id ?? '')"
          :options="categoryOptions"
          placeholder="无分类"
          test-id="inspector-category"
          :disabled="saving"
          @change="onCategory"
        />
        <p v-if="classificationError" class="block-error" data-test="inspector-category-error">{{ classificationError }}</p>
      </section>

      <section class="block">
        <h3>标签</h3>
        <div class="tags">
          <span v-if="!classification?.tags.length" class="tag-empty">暂无标签</span>
          <span v-for="t in classification?.tags ?? []" :key="t.id" class="tag" :data-test="'inspector-tag-' + t.id">
            {{ t.name }}<button type="button" class="tag-remove" :data-test="'inspector-tag-remove-' + t.id" :disabled="saving" @click="$emit('toggle-tag', t.id, false)">×</button>
          </span>
          <KnowledgeSelect
            :model-value="''"
            :options="availableTagOptions"
            placeholder="+ 添加标签"
            test-id="inspector-add-tag"
            :disabled="saving"
            @change="onAddTag"
          />
        </div>
      </section>

      <section v-if="document.sourceType === 'FILE'" class="block">
        <h3>本地来源</h3>
        <dl class="source-meta">
          <dt>类型</dt><dd>{{ document.processingStatus === 'COMPLETED' ? '已提取文本' : statusLabel(document.processingStatus) }}</dd>
          <dt>更新时间</dt><dd>{{ new Date(document.updatedAt).toLocaleString() }}</dd>
        </dl>
        <div class="actions">
          <button type="button" class="action-btn" data-test="inspector-open-source" :disabled="!sourceEnabled" @click="$emit('open-source')">打开原文</button>
          <button type="button" class="action-btn" data-test="inspector-reveal-source" :disabled="!sourceEnabled" @click="$emit('reveal-source')">在文件夹中显示</button>
        </div>
        <p v-if="sourceResultMessage" class="block-error" data-test="inspector-source-message">{{ sourceResultMessage }}</p>
      </section>

      <section v-if="document.sourceType === 'FILE' && document.processingStatus === 'FAILED'" class="block">
        <h3>处理</h3>
        <button type="button" class="action-btn primary" data-test="inspector-retry" :disabled="retrying" @click="$emit('retry')">
          {{ retrying ? '重试中…' : '重试解析' }}
        </button>
        <p v-if="retryError" class="block-error" data-test="inspector-retry-error">{{ retryError }}</p>
      </section>

      <section class="block danger">
        <h3>危险操作</h3>
        <button type="button" class="action-btn danger-btn" data-test="inspector-delete" @click="$emit('delete')">彻底删除资料</button>
        <p v-if="deleteError" class="block-error" data-test="inspector-delete-error">{{ deleteError }}</p>
      </section>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import KnowledgeSelect from './KnowledgeSelect.vue'
import { knowledgeStatusLabel } from './status'
import type { KnowledgeCategoryNode, KnowledgeDocument, KnowledgeDocumentClassification, KnowledgeTag } from '../../types/knowledge'

const props = defineProps<{
  document: KnowledgeDocument | null
  classification: KnowledgeDocumentClassification | null
  classificationError: string
  categories: KnowledgeCategoryNode[]
  tags: KnowledgeTag[]
  saving: boolean
  retrying: boolean
  retryError: string
  deleteError: string
  sourceResultMessage: string
  categoryPaths: Record<number, string>
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'set-category', id: number | null): void
  (e: 'toggle-tag', tagId: number, add: boolean): void
  (e: 'open-source'): void
  (e: 'reveal-source'): void
  (e: 'retry'): void
  (e: 'delete'): void
}>()

const categoryOptions = computed(() => {
  const list: { value: string; label: string; indent?: number }[] = [{ value: '', label: '无分类' }]
  for (const c of props.categories) {
    list.push({ value: String(c.id), label: c.name, indent: c.depth })
  }
  return list
})

const availableTagOptions = computed(() => {
  const used = new Set(props.classification?.tags.map((t) => t.id) ?? [])
  return props.tags.filter((t) => !used.has(t.id)).map((t) => ({ value: String(t.id), label: t.name }))
})

const sourceEnabled = computed(() => (
  props.document?.sourceType === 'FILE'
  && props.document.processingStatus === 'COMPLETED'
))

function onCategory(value: string) {
  emit('set-category', value === '' ? null : Number(value))
}

function onAddTag(value: string) {
  if (value !== '') emit('toggle-tag', Number(value), true)
}

function statusLabel(status: KnowledgeDocument['processingStatus']): string {
  return knowledgeStatusLabel(status)
}
</script>

<style scoped>
.inspector{box-sizing:border-box;width:284px;min-width:0;border-left:1px solid var(--border-subtle);overflow-y:auto;padding:14px 15px;background:var(--bg-subtle)}
.inspector-head{display:flex;align-items:center;justify-content:space-between;padding-bottom:10px;border-bottom:1px solid var(--border-subtle)}
.inspector-head strong{font-size:13px;font-weight:600;color:var(--ink)}
.inspector-close{border:0;background:transparent;color:var(--copy);cursor:pointer;line-height:1;display:inline-flex;align-items:center}
.category-path{margin:0 0 6px;color:var(--brand);font-size:12px}
.inspector-empty{padding:18px 4px;color:var(--muted);font-size:13px}
.block{padding:14px 0;border-bottom:1px solid var(--border-subtle)}
.block h3{margin:0 0 8px;font-size:11px;font-weight:600;color:var(--muted);letter-spacing:.04em}
.block.danger h3{color:var(--danger)}
.select-wrap{position:relative}
.tags{display:flex;flex-wrap:wrap;gap:6px;align-items:center}
.tags .kselect{width:132px}
.tag{display:inline-flex;align-items:center;gap:4px;padding:3px 9px;border-radius:999px;background:var(--bg-subtle);color:var(--copy);font-size:12px}
.tag-remove{border:0;background:transparent;color:var(--muted);cursor:pointer;font-size:12px;padding:0 2px}
.tag-remove:hover{color:var(--danger)}
.tag-empty{font-size:12px;color:var(--muted)}

.source-meta{display:grid;grid-template-columns:auto 1fr;gap:4px 10px;margin:0 0 10px;font-size:12px}
.source-meta dt{color:var(--muted)}
.source-meta dd{margin:0;color:var(--copy)}
.actions{display:flex;gap:8px}
.action-btn{padding:7px 12px;border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);font-size:12px;cursor:pointer}
.action-btn:hover:not(:disabled){border-color:var(--brand);color:var(--brand)}
.action-btn:disabled{opacity:.5;cursor:not-allowed}
.action-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg)}
.danger-btn{border-color:var(--danger);color:var(--danger)}
.danger-btn:hover:not(:disabled){background:var(--danger-soft)}
.block-error{margin:8px 0 0;color:var(--danger);font-size:12px}
</style>
