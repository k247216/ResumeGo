<template>
  <div class="role-setup" data-test="role-based-setup">
    <label class="setup-field">
      <span>求职目标</span>
      <el-select :model-value="draft.jobProjectId" placeholder="选择求职目标" data-test="role-project-select" @update:model-value="patch({ jobProjectId: $event })">
        <el-option v-for="item in projectOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </label>
    <label class="setup-field">
      <span>简历版本</span>
      <el-select :model-value="draft.resumeVersionId" placeholder="选择简历版本" data-test="role-resume-select" @update:model-value="patch({ resumeVersionId: $event })">
        <el-option v-for="item in resumeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </label>
    <div class="setup-field">
      <span>面试官</span>
      <el-select :model-value="draft.personaIds" multiple placeholder="选择面试官" data-test="role-persona-select" @update:model-value="patch({ personaIds: $event })">
        <el-option v-for="item in personaOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <label class="setup-field">
      <span>题目数量</span>
      <el-input-number :model-value="draft.questionCount" :min="3" :max="10" data-test="role-question-count" @update:model-value="patch({ questionCount: $event ?? 5 })" />
    </label>
  </div>
</template>

<script setup lang="ts">
import type { RoleBasedDraft } from '../../composables/useInterviewComposer'

export interface SelectOption { value: number; label: string }

const props = defineProps<{
  draft: RoleBasedDraft
  projectOptions: SelectOption[]
  resumeOptions: SelectOption[]
  personaOptions: SelectOption[]
}>()

const emit = defineEmits<{ 'update:draft': [draft: RoleBasedDraft] }>()

function patch(patchValue: Partial<RoleBasedDraft>) {
  emit('update:draft', { ...props.draft, ...patchValue })
}
</script>

<style scoped>
.role-setup{display:grid;gap:12px}
.setup-field{display:grid;gap:6px;font-size:12.5px;color:var(--copy)}
.setup-field>span{font-weight:600}
</style>
