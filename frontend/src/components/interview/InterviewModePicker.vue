<template>
  <div class="mode-picker" data-test="interview-mode-picker" role="tablist" aria-label="训练模式">
    <button
      v-for="(option, index) in MODES"
      :key="option.value"
      type="button"
      class="mode-tab"
      :class="{ selected: modelValue === option.value }"
      :data-test="`mode-${option.value}`"
      :title="option.description"
      role="tab"
      :aria-selected="modelValue === option.value"
      @click="emit('update:modelValue', option.value)"
    >
      <span class="mode-step" aria-hidden="true">0{{ index + 1 }}</span>
      <strong class="mode-label">{{ option.label }}</strong>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { InterviewMode } from '../../types/interview'

defineProps<{ modelValue: InterviewMode | null }>()
const emit = defineEmits<{ 'update:modelValue': [mode: InterviewMode] }>()

const MODES: Array<{ value: InterviewMode; label: string; description: string }> = [
  { value: 'ROLE_BASED', label: '岗位模拟', description: '围绕明确的求职目标、简历版本与面试官队列进行模拟，逐轮深挖。' },
  { value: 'KNOWLEDGE_TRAINING', label: '知识训练', description: '选择你的知识资料出题训练，反馈带来源引用，找不到依据如实标注。' },
  { value: 'EXPERIENCE_SIMULATION', label: '真题演练', description: '使用本地面经题集原题模拟，AI 追问单独标注，不混入原题。' },
]
</script>

<style scoped>
.mode-picker{display:inline-flex;align-items:center;gap:3px;width:max-content;max-width:100%;padding:3px;border:1px solid var(--border-subtle);border-radius:10px;background:var(--bg-surface,#f7f8f6)}
.mode-tab{display:inline-flex;align-items:center;gap:7px;border:0;border-radius:7px;background:transparent;color:var(--muted);padding:8px 13px;cursor:pointer;transition:background .16s ease,color .16s ease}
.mode-tab:hover{color:var(--ink)}
.mode-tab.selected{background:var(--surface-solid,#fff);color:var(--ink);box-shadow:0 1px 4px rgba(16,24,40,.08)}
.mode-step{font-size:10px;font-weight:700;letter-spacing:.08em;color:currentColor;font-variant-numeric:tabular-nums;opacity:.55}
.mode-label{font-size:13px;font-weight:650;letter-spacing:-.01em;white-space:nowrap}
@media (max-width: 700px){.mode-picker{display:grid;grid-template-columns:1fr;width:100%}.mode-tab{justify-content:center}}
</style>
