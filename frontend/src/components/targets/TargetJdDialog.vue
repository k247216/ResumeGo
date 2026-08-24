<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="jd-dialog-title">
      <header>
        <div>
          <small>补充岗位信息</small>
          <h2 id="jd-dialog-title">{{ target?.name }}</h2>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <form @submit.prevent="submit">
        <div class="field-row">
          <label>
            公司名称
            <input v-model="companyName" data-test="jd-company" autocomplete="off" placeholder="如：腾讯">
          </label>
          <label>
            岗位名称
            <input v-model="jobTitle" data-test="jd-job-title" autocomplete="off" placeholder="如：Java 后端开发">
          </label>
        </div>
        <label>
          岗位描述 JD<span class="optional">粘贴岗位职责 / 任职要求</span>
          <textarea v-model="jdText" data-test="jd-text" rows="6" placeholder="岗位职责…&#10;任职要求…"></textarea>
        </label>

        <p v-if="validationMessage || errorMessage" class="error" role="alert">{{ validationMessage || errorMessage }}</p>
        <footer>
          <button type="button" @click="$emit('close')">取消</button>
          <button class="primary" type="submit" :disabled="submitting">{{ submitting ? '保存中…' : '保存 JD' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { JobProject } from '../../types/project'

const props = withDefaults(defineProps<{
  open: boolean
  target: JobProject | null
  submitting?: boolean
  errorMessage?: string
}>(), { open: false, submitting: false, errorMessage: '' })

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save', payload: { companyName: string; jobTitle: string; jdText: string }): void
}>()

const companyName = ref('')
const jobTitle = ref('')
const jdText = ref('')
const validationMessage = ref('')

watch(() => props.open, (open) => {
  if (!open) {
    companyName.value = ''
    jobTitle.value = ''
    jdText.value = ''
    validationMessage.value = ''
  }
})

function submit() {
  if (!companyName.value.trim() || !jobTitle.value.trim() || !jdText.value.trim()) {
    validationMessage.value = '公司、岗位与 JD 原文都需要填写'
    return
  }
  validationMessage.value = ''
  emit('save', { companyName: companyName.value.trim(), jobTitle: jobTitle.value.trim(), jdText: jdText.value.trim() })
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.dialog{width:min(560px,100%);border-radius:16px;background:#fff;color:#141516;box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px;animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.dialog{animation:none}}
.dialog header{display:flex;justify-content:space-between;gap:16px;margin-bottom:14px}
.dialog h2{margin:5px 0 0;font-size:20px;font-weight:650}
.dialog small{color:var(--brand,#168866);font-weight:700}
.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.dialog form,.dialog label{display:grid;gap:9px}
.dialog form{gap:14px}
.dialog label{font-weight:650;color:#344552;font-size:13px}
.optional{margin-left:6px;font-weight:500;color:var(--muted,#74828c)}
.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.dialog input,.dialog textarea{border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:10px 12px;background:#fff;color:#22313b;font:inherit;font-weight:400;resize:vertical}
.dialog input:focus,.dialog textarea:focus{outline:none;border-color:var(--brand,#168866);box-shadow:0 0 0 3px var(--brand-soft,rgba(22,139,104,.12))}
.error{margin:0;color:var(--danger,#b53c32);font-size:13px}
.dialog footer{display:flex;justify-content:flex-end;gap:9px}
.dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:#fff;color:#141516;padding:9px 13px;font:inherit;cursor:pointer}
.dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff;font-weight:600}
</style>
