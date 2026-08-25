<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="emit('cancel')">
    <section class="fork-dialog" role="dialog" aria-modal="true" aria-labelledby="fork-title" data-test="fork-dialog">
      <button type="button" class="dialog-close" aria-label="关闭" @click="emit('cancel')">×</button>
      <p>创建岗位表达副本</p>
      <h2 id="fork-title" data-test="fork-source-label">{{ sourceLabel }}</h2>
      <p class="fork-note">副本复制当前选中版本的正文，此后与源简历独立演进，互不影响。</p>
      <form @submit.prevent="confirm">
        <label class="fork-field">
          <span>副本名称</span>
          <input v-model="title" data-test="fork-title-input" maxlength="120" placeholder="例如：腾讯 · Java 后端表达" />
        </label>
        <p v-if="error" class="fork-error" data-test="fork-error">{{ error }}</p>
        <footer>
          <button type="button" class="ghost" @click="emit('cancel')">取消</button>
          <button type="button" class="primary" data-test="fork-confirm" :disabled="submitting || !title.trim()" @click="confirm">
            {{ submitting ? '创建中…' : '创建副本' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Resume, ResumeVersion } from '../../types/resume'

const props = defineProps<{
  open: boolean
  resume: Resume | null
  version: ResumeVersion | null
  submitting?: boolean
  error?: string
}>()

const emit = defineEmits<{
  confirm: [title: string]
  cancel: []
}>()

const title = ref('')

const sourceLabel = computed(() => {
  if (!props.resume) return ''
  const versionLabel = props.version ? ` · V${props.version.versionNo}` : ''
  return `${props.resume.title}${versionLabel}`
})

watch(() => props.open, (open) => {
  if (open && props.resume) {
    title.value = `${props.resume.title} · 岗位表达`
  }
})

function confirm() {
  const value = title.value.trim()
  if (!value || props.submitting) return
  emit('confirm', value)
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:50;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.fork-dialog{position:relative;width:min(420px,100%);border-radius:14px;background:var(--bg-elevated,#fff);padding:20px;box-shadow:0 22px 60px rgba(0,0,0,.35)}
.dialog-close{position:absolute;top:10px;right:12px;border:0;background:none;color:var(--muted);font-size:18px;cursor:pointer}
.fork-dialog>p{margin:0;color:var(--muted);font-size:12px}
.fork-dialog h2{margin:6px 0 8px;font-size:17px;color:var(--ink)}
.fork-note{margin:0 0 14px;color:var(--muted);font-size:12px;line-height:1.6}
.fork-field{display:grid;gap:6px;font-size:12.5px;color:var(--copy)}
.fork-field input{border:1px solid var(--border-default);border-radius:9px;padding:9px 12px;font:inherit;color:var(--ink)}
.fork-error{margin:8px 0 0;color:var(--danger);font-size:12.5px}
.fork-dialog footer{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
.fork-dialog footer button{border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);padding:8px 13px;font:inherit;cursor:pointer}
.fork-dialog footer .primary{border-color:var(--brand);background:var(--brand);color:#fff;font-weight:600}
.fork-dialog footer button:disabled{opacity:.55;cursor:default}
</style>
