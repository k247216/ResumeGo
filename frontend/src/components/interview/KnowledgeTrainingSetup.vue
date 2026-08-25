<template>
  <div class="knowledge-setup" data-test="knowledge-training-setup">
    <div class="setup-field">
      <span>知识资料</span>
      <el-select
        :model-value="draft.knowledgeDocumentIds"
        multiple
        filterable
        placeholder="选择知识资料"
        data-test="knowledge-doc-select"
        @update:model-value="patch({ knowledgeDocumentIds: $event })"
      >
        <el-option v-for="item in documentOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <label class="setup-field">
      <span>难度方向（可选）</span>
      <el-input :model-value="draft.difficulty" placeholder="例如：基础 / 深入 / 场景" data-test="knowledge-difficulty" @update:model-value="patch({ difficulty: $event })" />
    </label>
    <label class="setup-field">
      <span>题目数量</span>
      <el-input-number :model-value="draft.questionCount" :min="3" :max="10" data-test="knowledge-question-count" @update:model-value="patch({ questionCount: $event ?? 5 })" />
    </label>
    <p class="setup-note">反馈将引用资料片段；找不到依据时会明确标注。</p>
  </div>
</template>

<script setup lang="ts">
import type { KnowledgeTrainingDraft } from '../../composables/useInterviewComposer'

const props = defineProps<{
  draft: KnowledgeTrainingDraft
  documentOptions: Array<{ value: number; label: string }>
}>()

const emit = defineEmits<{ 'update:draft': [draft: KnowledgeTrainingDraft] }>()

function patch(patchValue: Partial<KnowledgeTrainingDraft>) {
  emit('update:draft', { ...props.draft, ...patchValue })
}
</script>

<style scoped>
.knowledge-setup{display:grid;gap:12px}
.setup-field{display:grid;gap:6px;font-size:12.5px;color:var(--copy)}
.setup-field>span{font-weight:600}
.setup-note{margin:0;color:var(--muted);font-size:11.5px}
</style>
