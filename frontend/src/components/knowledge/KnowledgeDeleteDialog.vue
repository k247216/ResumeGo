<template>
  <div class="dialog-mask" data-test="knowledge-delete-dialog" @click.self="$emit('close')">
    <form class="dialog" @submit.prevent="confirm">
      <template v-if="bulk">
        <h3>彻底删除选中的 {{ bulkCount }} 个资料？</h3>
        <p class="impact" data-test="delete-impact">
          将删除选中的 {{ bulkCount }} 个资料及其受管副本、正文、分类与标签关联。此操作不可恢复。
        </p>
      </template>
      <template v-else>
        <h3>彻底删除「{{ impact?.title ?? '该资料' }}」？</h3>
        <p class="impact" data-test="delete-impact">
          将删除：
          <span v-if="impact?.hasContent">已提取正文；</span>
          <span v-if="impact?.hasSource">受管原文件；</span>
          <span v-if="impact?.hasCategory">分类关联；</span>
          <span v-if="impact?.hasTags">标签关联；</span>
          <span v-if="!impact?.hasContent && !impact?.hasSource && !impact?.hasCategory && !impact?.hasTags">无关联派生数据；</span>
          此操作不可恢复。
        </p>
      </template>
      <label class="field">
        <span>输入「删除」确认</span>
        <input v-model="confirmText" type="text" data-test="delete-confirm-input" :disabled="deleting" placeholder="删除" />
      </label>
      <p v-if="loading" class="hint" data-test="delete-impact-loading">正在读取影响摘要…</p>
      <p v-if="error" class="error" data-test="delete-dialog-error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="ghost" data-test="delete-cancel" :disabled="deleting" @click="$emit('close')">取消</button>
        <button type="submit" class="primary danger" data-test="delete-confirm" :disabled="deleting || confirmText !== '删除' || (!impact && !bulk)">
          {{ deleting ? '删除中…' : '确认删除' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { KnowledgeDeletionImpact } from '../../types/knowledge'

const props = defineProps<{
  impact: KnowledgeDeletionImpact | null
  loading: boolean
  deleting: boolean
  error: string
  bulk?: boolean
  bulkCount?: number
}>()

const emit = defineEmits<{ (e: 'close'): void; (e: 'confirm', token: string): void }>()

const confirmText = ref('')

watch(() => props.impact?.confirmationToken, () => {
  confirmText.value = ''
})

function confirm() {
  if ((!props.impact && !props.bulk) || confirmText.value !== '删除' || props.deleting) return
  emit('confirm', props.impact?.confirmationToken ?? '')
}
</script>

<style scoped>
.dialog-mask{position:fixed;inset:0;z-index:60;display:grid;place-items:center;background:rgba(10,10,11,.45)}
.dialog{display:grid;gap:14px;width:min(420px,calc(100vw - 48px));padding:22px;border:1px solid var(--border-default);border-radius:16px;background:var(--surface-solid);color:var(--ink);box-shadow:0 18px 48px rgba(0,0,0,.22)}
.dialog h3{margin:0;font-size:16px;font-weight:650}
.impact{margin:0;color:var(--copy);font-size:13px;line-height:1.7}
.field{display:grid;gap:6px;font-size:13px;color:var(--copy)}
.field input{padding:9px 11px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-subtle);color:var(--ink);font-size:14px}
.hint{color:var(--muted);font-size:12px;margin:0}
.error{margin:0;color:var(--danger);font-size:12px}
.actions{display:flex;justify-content:flex-end;gap:10px}
.actions button{padding:8px 14px;border-radius:10px;font-size:13px;cursor:pointer}
.ghost{border:1px solid var(--border-default);background:transparent;color:var(--copy)}
.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.primary.danger{background:var(--danger);color:#fff}
.primary:disabled{opacity:.55;cursor:not-allowed}
</style>
