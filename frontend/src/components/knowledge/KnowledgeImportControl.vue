<template>
  <span class="import-control">
    <button
      type="button"
      class="tool-btn"
      data-test="knowledge-import"
      :disabled="disabled"
      @click="open"
    >
      {{ disabled ? '导入中…' : '导入文件' }}
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
.tool-btn{padding:8px 13px;border:1px solid var(--border-default);border-radius:10px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer}
.tool-btn:hover{border-color:var(--brand);color:var(--brand)}
.tool-btn:disabled{opacity:.55;cursor:not-allowed}
.hidden-input{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}
.import-error{color:var(--danger);font-size:12px;max-width:260px}
</style>
