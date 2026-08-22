<template>
  <div class="dialog-mask" data-test="knowledge-unsaved-dialog" @click.self="$emit('keep-editing')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="knowledge-unsaved-title">
      <div class="dialog-mark" aria-hidden="true">•••</div>
      <div class="dialog-copy">
        <h3 id="knowledge-unsaved-title">保留这次修改吗？</h3>
        <p>当前资料还有未保存内容。你可以继续编辑、放弃修改，或保存后切换。</p>
        <p v-if="error" class="error" data-test="knowledge-unsaved-error">{{ error }}</p>
      </div>
      <div class="actions">
        <button type="button" class="ghost" data-test="knowledge-unsaved-keep" :disabled="saving" @click="$emit('keep-editing')">继续编辑</button>
        <button type="button" class="ghost danger" data-test="knowledge-unsaved-discard" :disabled="saving" @click="$emit('discard')">放弃修改</button>
        <button type="button" class="primary" data-test="knowledge-unsaved-save" :disabled="saving" @click="$emit('save')">
          {{ saving ? '保存中…' : '保存并切换' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{ saving: boolean; error: string }>()
defineEmits<{
  (e: 'keep-editing'): void
  (e: 'discard'): void
  (e: 'save'): void
}>()
</script>

<style scoped>
.dialog-mask{position:fixed;inset:0;z-index:80;display:grid;place-items:center;background:rgba(12,13,14,.34);backdrop-filter:blur(4px)}
.dialog{display:grid;grid-template-columns:auto 1fr;gap:14px;width:min(430px,calc(100vw - 40px));padding:22px;border:1px solid var(--border-default);border-radius:15px;background:var(--surface-solid);color:var(--ink);box-shadow:0 22px 60px rgba(0,0,0,.2)}
.dialog-mark{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:var(--bg-subtle);font-size:13px;letter-spacing:2px;color:var(--copy)}
.dialog-copy{display:grid;gap:7px}
.dialog h3,.dialog p{margin:0}.dialog h3{font-size:16px}.dialog p{color:var(--muted);font-size:13px;line-height:1.6}.dialog p.error{color:var(--danger)}
.actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px;margin-top:4px}.actions button{padding:8px 12px;border-radius:8px;font-size:12px;cursor:pointer}.actions button:disabled{opacity:.5;cursor:not-allowed}
.ghost{border:1px solid var(--border-default);background:transparent;color:var(--copy)}.ghost.danger{color:var(--danger)}.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
</style>
