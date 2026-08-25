<template>
  <div class="mode-picker" data-test="interview-mode-picker" role="tablist" aria-label="训练模式">
    <button
      v-for="(option, index) in MODES"
      :key="option.value"
      type="button"
      class="mode-card"
      :class="{ selected: modelValue === option.value }"
      :data-test="`mode-${option.value}`"
      role="tab"
      :aria-selected="modelValue === option.value"
      @click="emit('update:modelValue', option.value)"
    >
      <span class="mode-step" aria-hidden="true">0{{ index + 1 }}</span>
      <strong class="mode-label">{{ option.label }}</strong>
      <span class="mode-desc">{{ option.description }}</span>
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
  { value: 'EXPERIENCE_SIMULATION', label: '面经模拟', description: '使用本地面经题集原题模拟，AI 追问单独标注，不混入原题。' },
]
</script>

<style scoped>
.mode-picker{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}
.mode-card{position:relative;display:grid;gap:8px;align-content:start;text-align:left;border:1px solid var(--border-subtle);border-radius:16px;background:var(--surface-solid,#fff);padding:20px 20px 18px;cursor:pointer;transition:transform .18s cubic-bezier(.2,.7,.3,1),box-shadow .18s ease-out,border-color .18s ease-out}
.mode-card:hover{transform:translateY(-2px);border-color:var(--brand-soft);box-shadow:0 12px 32px rgba(16,24,40,.08)}
.mode-card.selected{border-color:var(--brand);box-shadow:0 0 0 1px var(--brand),0 12px 32px rgba(16,24,40,.08)}
.mode-step{font-size:11px;font-weight:700;letter-spacing:.08em;color:var(--muted);font-variant-numeric:tabular-nums}
.mode-card.selected .mode-step{color:var(--brand)}
.mode-label{font-size:16px;font-weight:700;color:var(--ink);letter-spacing:-.01em}
.mode-desc{font-size:12.5px;color:var(--muted);line-height:1.7}
@media (max-width: 900px){.mode-picker{grid-template-columns:1fr}}
</style>
