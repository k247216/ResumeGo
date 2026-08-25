<template>
  <div class="mode-picker" data-test="interview-mode-picker">
    <button
      v-for="option in MODES"
      :key="option.value"
      type="button"
      class="mode-card"
      :class="{ selected: modelValue === option.value }"
      :data-test="`mode-${option.value}`"
      @click="emit('update:modelValue', option.value)"
    >
      <strong>{{ option.label }}</strong>
      <span>{{ option.description }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { InterviewMode } from '../../types/interview'

defineProps<{ modelValue: InterviewMode | null }>()
const emit = defineEmits<{ 'update:modelValue': [mode: InterviewMode] }>()

const MODES: Array<{ value: InterviewMode; label: string; description: string }> = [
  { value: 'ROLE_BASED', label: '岗位模拟', description: '围绕明确的求职目标、简历版本与面试官队列进行模拟。' },
  { value: 'KNOWLEDGE_TRAINING', label: '知识训练', description: '选择你的知识资料出题训练，反馈带来源引用。' },
  { value: 'EXPERIENCE_SIMULATION', label: '面经模拟', description: '使用本地面经题集原题模拟，AI 追问单独标注。' },
]
</script>

<style scoped>
.mode-picker{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
.mode-card{display:grid;gap:6px;text-align:left;border:1px solid var(--border-default);border-radius:12px;background:var(--bg-surface);padding:14px 16px;cursor:pointer}
.mode-card:hover{border-color:var(--brand-soft);background:var(--bg-hover)}
.mode-card.selected{border-color:var(--brand);box-shadow:0 0 0 1px var(--brand)}
.mode-card strong{font-size:14px;color:var(--ink)}
.mode-card span{font-size:12px;color:var(--muted);line-height:1.6}
@media (max-width: 900px){.mode-picker{grid-template-columns:1fr}}
</style>
