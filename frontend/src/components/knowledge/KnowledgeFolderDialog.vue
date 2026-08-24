<template>
  <div class="dialog-mask" :data-test="'knowledge-folder-dialog-' + kind" @click.self="$emit('close')">
    <form class="dialog" @submit.prevent="submit">
      <h3>{{ kind === 'create' ? '新建文件夹' : '编辑文件夹' }}</h3>
      <label class="field">
        <span>名称</span>
        <input v-model="name" type="text" maxlength="40" data-test="folder-name-input" placeholder="1-40 个字符" :disabled="submitting" />
      </label>
      <label class="field">
        <span>父文件夹</span>
        <select v-model.number="parentId" data-test="folder-parent-select" :disabled="submitting">
          <option :value="null">（根节点）</option>
          <option v-for="opt in parentOptions" :key="opt.id" :value="opt.id">{{ indent(opt.depth) }}{{ opt.name }}</option>
        </select>
      </label>
      <p v-if="error" class="error" data-test="folder-dialog-error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="ghost" data-test="folder-dialog-cancel" :disabled="submitting" @click="$emit('close')">取消</button>
        <button type="submit" class="primary" data-test="folder-dialog-submit" :disabled="submitting || !canSubmit">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { KnowledgeCategoryNode } from '../../types/knowledge'

const props = defineProps<{
  kind: 'create' | 'edit'
  initialName: string
  initialParentId: number | null
  excludedIds: number[]
  parentOptions: KnowledgeCategoryNode[]
  submitting: boolean
  error: string
}>()

const emit = defineEmits<{ (e: 'close'): void; (e: 'submit', name: string, parentId: number | null): void }>()

const name = ref(props.initialName)
const parentId = ref<number | null>(props.initialParentId)

watch(() => props.initialName, (v) => { name.value = v })
watch(() => props.initialParentId, (v) => { parentId.value = v })

const canSubmit = computed(() => name.value.trim().length > 0 && name.value.trim().length <= 40)

function indent(depth: number): string {
  return '　'.repeat(depth)
}

function submit() {
  if (!canSubmit.value || props.submitting) return
  emit('submit', name.value.trim(), parentId.value)
}
</script>

<style scoped>
.dialog-mask{position:fixed;inset:0;z-index:60;display:grid;place-items:center;background:rgba(10,10,11,.45)}
.dialog{display:grid;gap:14px;width:min(380px,calc(100vw - 48px));padding:22px;border:1px solid var(--border-default);border-radius:16px;background:var(--surface-solid);color:var(--ink);box-shadow:0 18px 48px rgba(0,0,0,.22)}
.dialog h3{margin:0;font-size:16px;font-weight:650}
.field{display:grid;gap:6px;font-size:13px;color:var(--copy)}
.field input,.field select{padding:9px 11px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:14px}
.error{margin:0;color:var(--danger);font-size:12px}
.actions{display:flex;justify-content:flex-end;gap:10px}
.actions button{padding:8px 14px;border-radius:10px;font-size:13px;cursor:pointer}
.ghost{border:1px solid var(--border-default);background:transparent;color:var(--copy)}
.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.primary:disabled{opacity:.55;cursor:not-allowed}
</style>
