<template>
  <div class="experience-setup" data-test="experience-simulation-setup">
    <label class="setup-field">
      <span>面经题集</span>
      <el-select :model-value="draft.questionSetId" placeholder="选择本地题集" data-test="experience-set-select" @update:model-value="patch({ questionSetId: $event })">
        <el-option v-for="item in questionSetOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </label>
    <div class="setup-field">
      <span>面试官（可选）</span>
      <el-select :model-value="draft.personaIds" multiple placeholder="选择面试官" data-test="experience-persona-select" @update:model-value="patch({ personaIds: $event })">
        <el-option v-for="item in personaOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <label class="setup-field">
      <span>追问强度（可选）</span>
      <el-select :model-value="draft.followUpIntensity" placeholder="适中" data-test="experience-intensity" @update:model-value="patch({ followUpIntensity: $event })">
        <el-option label="克制" value="克制" />
        <el-option label="适中" value="适中" />
        <el-option label="高压" value="高压" />
      </el-select>
    </label>
    <label class="setup-field">
      <span>题目数量</span>
      <el-input-number :model-value="draft.questionCount" :min="3" :max="10" data-test="experience-question-count" @update:model-value="patch({ questionCount: $event ?? 5 })" />
    </label>
    <p class="setup-note">原题保持真实来源；AI 追问会单独标注，不混入原题。</p>
  </div>
</template>

<script setup lang="ts">
import type { ExperienceSimulationDraft } from '../../composables/useInterviewComposer'

const props = defineProps<{
  draft: ExperienceSimulationDraft
  questionSetOptions: Array<{ value: number; label: string }>
  personaOptions: Array<{ value: number; label: string }>
}>()

const emit = defineEmits<{ 'update:draft': [draft: ExperienceSimulationDraft] }>()

function patch(patchValue: Partial<ExperienceSimulationDraft>) {
  emit('update:draft', { ...props.draft, ...patchValue })
}
</script>

<style scoped>
.experience-setup{display:grid;gap:12px}
.setup-field{display:grid;gap:6px;font-size:12.5px;color:var(--copy)}
.setup-field>span{font-weight:600}
.setup-note{margin:0;color:var(--muted);font-size:11.5px}
</style>
