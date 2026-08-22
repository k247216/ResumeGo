<template>
  <span class="import-control">
    <button
      type="button"
      class="tool-btn"
      :class="{ primary }"
      data-test="knowledge-import"
      :disabled="disabled"
      @click="open"
    >
      {{ disabled ? '导入中…' : label }}
    </button>
    <input
      ref="input"
      type="file"
      class="hidden-input"
      accept=".md,.txt,text/plain,text/markdown"
      data-test="knowledge-file-input"
      @change="onChange"
    />
    <span v-if="error" class="import-error" data-test="knowledge-import-error">{{ error }}</span>
  </span>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  disabled: boolean
  error: string
  primary?: boolean
  label?: string
}>()

const emit = defineEmits<{ (e: 'file-selected', file: File): void }>()

const input = ref<HTMLInputElement | null>(null)

function open() {
  input.value?.click()
}

function onChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    emit('file-selected', file)
  }
  // 允许重复选择同一文件
  target.value = ''
}
</script>

<style scoped>
.import-control{display:inline-flex;align-items:center;gap:8px}
.tool-btn{padding:9px 16px;border:1px solid var(--border-default);border-radius:11px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer;white-space:nowrap}
.tool-btn:hover{border-color:var(--brand);color:var(--brand)}
.tool-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.tool-btn.primary:hover{opacity:.92;color:var(--action-fg)}
.tool-btn:disabled{opacity:.55;cursor:not-allowed}
.hidden-input{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}
.import-error{color:var(--danger);font-size:12px;max-width:260px}
</style>
