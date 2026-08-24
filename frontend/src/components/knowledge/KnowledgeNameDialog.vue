<template>
  <div class="dialog-mask" :data-test="'knowledge-name-dialog-' + kind" @click.self="$emit('close')">
    <form class="dialog" @submit.prevent="submit">
      <h3>{{ title }}</h3>
      <label class="field">
        <span>名称</span>
        <input
          v-model="name"
          type="text"
          maxlength="40"
          :data-test="'knowledge-name-input-' + kind"
          placeholder="1-40 个字符"
          :disabled="submitting"
        />
      </label>
      <p v-if="error" class="error" data-test="knowledge-name-error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="ghost" :data-test="'knowledge-name-cancel-' + kind" :disabled="submitting" @click="$emit('close')">取消</button>
        <button type="submit" class="primary" :data-test="'knowledge-name-submit-' + kind" :disabled="submitting || !canSubmit">
          {{ submitting ? '创建中…' : '创建' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  kind: 'category' | 'tag'
  submitting: boolean
  error: string
}>()

const emit = defineEmits<{ (e: 'close'): void; (e: 'create', name: string): void }>()

const title = computed(() => (props.kind === 'category' ? '新建分类' : '新建标签'))

const name = ref('')

const canSubmit = computed(() => {
  const trimmed = name.value.trim()
  return trimmed.length > 0 && trimmed.length <= 40
})

function submit() {
  if (!canSubmit.value || props.submitting) return
  emit('create', name.value.trim())
}
</script>

<style scoped>
.dialog-mask{position:fixed;inset:0;z-index:60;display:grid;place-items:center;background:rgba(10,10,11,.45)}
.dialog{display:grid;gap:14px;width:min(380px,calc(100vw - 48px));padding:22px;border:1px solid var(--border-default);border-radius:16px;background:var(--surface-solid);color:var(--ink);box-shadow:0 18px 48px rgba(0,0,0,.22)}
.dialog h3{margin:0;font-size:16px;font-weight:650}
.field{display:grid;gap:6px;font-size:13px;color:var(--copy)}
.field input{padding:9px 11px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:14px}
.field input:focus{outline:2px solid var(--brand);border-color:transparent}
.error{margin:0;color:var(--danger);font-size:12px}
.actions{display:flex;justify-content:flex-end;gap:10px}
.actions button{padding:8px 14px;border-radius:10px;font-size:13px;cursor:pointer}
.ghost{border:1px solid var(--border-default);background:transparent;color:var(--copy)}
.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.primary:disabled{opacity:.55;cursor:not-allowed}
</style>
