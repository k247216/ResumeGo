<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="target-resume-title">
      <header><div><small>当前简历</small><h2 id="target-resume-title">选择这个目标采用的简历版本</h2></div><button type="button" aria-label="关闭" @click="$emit('close')">×</button></header>
      <form @submit.prevent="submit">
        <label v-if="selectableResumes.length">本地简历<select v-model="selectedVersionId" data-test="resume-version"><option value="">请选择</option><option v-for="resume in selectableResumes" :key="resume.id" :value="String(resume.currentVersion!.id)">{{ resume.title }} · v{{ resume.currentVersion!.versionNo }}</option></select></label>
        <div v-else class="empty">先创建一份本地简历，再将它用于这个求职目标。<button data-test="create-blank-resume" type="button" @click="$emit('create-resume')">创建空白简历</button></div>
        <p v-if="validationMessage || errorMessage" class="error" role="alert">{{ validationMessage || errorMessage }}</p>
        <footer><button type="button" @click="$emit('close')">取消</button><button class="primary" type="submit" :disabled="submitting || selectableResumes.length === 0">{{ submitting ? '关联中…' : '设为当前简历' }}</button></footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Resume } from '../../types/resume'

const props = withDefaults(defineProps<{ open: boolean; resumes: Resume[]; submitting?: boolean; errorMessage?: string }>(), { submitting: false, errorMessage: '' })
const emit = defineEmits<{ (event: 'close'): void; (event: 'select', versionId: number): void; (event: 'create-resume'): void }>()
const selectedVersionId = ref('')
const validationMessage = ref('')
const selectableResumes = computed(() => props.resumes.filter((resume) => resume.currentVersion?.id))
watch(() => props.open, (open) => { if (!open) { selectedVersionId.value = ''; validationMessage.value = '' } })
function submit() { const id = Number(selectedVersionId.value); if (!Number.isSafeInteger(id) || id <= 0) { validationMessage.value = '请选择一份简历版本'; return }; validationMessage.value = ''; emit('select', id) }
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:45;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}.dialog{width:min(520px,100%);border-radius:16px;background:var(--surface-solid,#fff);color:var(--ink,#141516);box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px}.dialog header{display:flex;justify-content:space-between;gap:16px}.dialog h2{margin:5px 0 20px;font-size:21px}.dialog small{color:var(--brand,#168866);font-weight:700}.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px}.dialog form,.dialog label{display:grid;gap:10px}.dialog form{gap:18px}.dialog label{font-weight:700;color:var(--ink,#344552)}.dialog select{border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:11px 12px;background:var(--surface-solid,#fff);color:var(--ink,#22313b)}.empty{display:grid;justify-items:start;gap:12px;border:1px dashed var(--line,#cbd7dc);border-radius:10px;padding:20px;color:var(--copy,#6f7e87)}.empty button{border:0;border-radius:8px;background:var(--brand-soft,#e8f6f1);color:var(--brand,#13795d);padding:8px 11px;font-weight:700}.error{margin:0;color:var(--danger,#b53c32)}.dialog footer{display:flex;justify-content:flex-end;gap:9px}.dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:var(--surface-solid,#fff);color:var(--ink,#141516);padding:9px 13px}.dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff}.dialog footer button:disabled{opacity:.55}
</style>
