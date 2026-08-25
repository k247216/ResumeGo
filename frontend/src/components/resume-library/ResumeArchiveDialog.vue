<template>
  <div v-if="open && resume" class="dialog-backdrop" role="presentation" @click.self="emit('cancel')">
    <section class="archive-dialog" role="dialog" aria-modal="true" aria-labelledby="archive-title" data-test="archive-dialog">
      <p>归档简历</p>
      <h2 id="archive-title" data-test="archive-source-label">{{ resume.title }}</h2>
      <p class="archive-note">
        归档后这份简历从默认列表隐藏，可随时恢复。它的历史版本和被引用关系保持不变，不会影响任何求职目标。
      </p>
      <p v-if="error" class="archive-error" data-test="archive-error">{{ error }}</p>
      <footer>
        <button type="button" class="ghost" @click="emit('cancel')">取消</button>
        <button type="button" class="primary" data-test="archive-confirm" :disabled="submitting" @click="emit('confirm')">
          {{ submitting ? '归档中…' : '确认归档' }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { Resume } from '../../types/resume'

defineProps<{
  open: boolean
  resume: Resume | null
  submitting?: boolean
  error?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:50;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.archive-dialog{position:relative;width:min(380px,100%);border-radius:14px;background:var(--bg-elevated,#fff);padding:20px;box-shadow:0 22px 60px rgba(0,0,0,.35)}
.archive-dialog>p{margin:0;color:var(--muted);font-size:12px}
.archive-dialog h2{margin:6px 0 8px;font-size:17px;color:var(--ink)}
.archive-note{margin:0 0 8px;color:var(--muted);font-size:12px;line-height:1.7}
.archive-error{margin:0;color:var(--danger);font-size:12.5px}
.archive-dialog footer{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
.archive-dialog footer button{border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);padding:8px 13px;font:inherit;cursor:pointer}
.archive-dialog footer .primary{border-color:var(--warning,#ad6800);background:var(--warning,#ad6800);color:#fff;font-weight:600}
.archive-dialog footer button:disabled{opacity:.55;cursor:default}
</style>
