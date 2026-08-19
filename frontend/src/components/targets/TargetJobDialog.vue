<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="target-job-title">
      <header><div><small>目标岗位</small><h2 id="target-job-title">粘贴这次求职对应的岗位信息</h2></div><button type="button" aria-label="关闭" @click="$emit('close')">×</button></header>
      <form @submit.prevent="submit">
        <div class="field-row">
          <label>岗位名称<input v-model="jobTitle" data-test="job-title" placeholder="例如：Java 后端实习"></label>
          <label>公司名称（可选）<input v-model="companyName" data-test="job-company" placeholder="例如：某某科技"></label>
        </div>
        <label>岗位描述<textarea v-model="rawText" data-test="job-raw-text" rows="10" placeholder="粘贴岗位职责、任职要求和其他公开信息"></textarea></label>
        <p class="hint">内容只作为当前求职目标的本地材料保存。</p>
        <p v-if="validationMessage || errorMessage" class="error" role="alert">{{ validationMessage || errorMessage }}</p>
        <footer><button type="button" @click="$emit('close')">取消</button><button class="primary" type="submit" :disabled="submitting">{{ submitting ? '保存中…' : '保存目标岗位' }}</button></footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CreateJobDescriptionRequest } from '../../types/job'

const props = withDefaults(defineProps<{ open: boolean; submitting?: boolean; errorMessage?: string }>(), { submitting: false, errorMessage: '' })
const emit = defineEmits<{ (event: 'close'): void; (event: 'create', payload: CreateJobDescriptionRequest): void }>()
const jobTitle = ref('')
const companyName = ref('')
const rawText = ref('')
const validationMessage = ref('')

watch(() => props.open, (open) => {
  if (!open) { jobTitle.value = ''; companyName.value = ''; rawText.value = ''; validationMessage.value = '' }
})

function submit() {
  const normalizedTitle = jobTitle.value.trim()
  const normalizedText = rawText.value.trim()
  if (!normalizedTitle) { validationMessage.value = '请输入岗位名称'; return }
  if (normalizedText.length < 20) { validationMessage.value = '岗位描述至少 20 个字符'; return }
  validationMessage.value = ''
  emit('create', { jobTitle: normalizedTitle, ...(companyName.value.trim() ? { companyName: companyName.value.trim() } : {}), rawText: normalizedText })
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:45;display:grid;place-items:center;background:rgba(19,31,43,.38);padding:24px}.dialog{width:min(650px,100%);border-radius:16px;background:#fff;box-shadow:0 22px 60px rgba(15,35,47,.22);padding:22px}.dialog header{display:flex;justify-content:space-between;gap:16px}.dialog h2{margin:5px 0 20px;font-size:21px}.dialog small{color:#168866;font-weight:700}.dialog header button{align-self:start;border:0;background:none;font-size:24px}.dialog form,.dialog label{display:grid;gap:9px}.dialog form{gap:16px}.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}.dialog label{font-weight:700;color:#344552}.dialog input,.dialog textarea{box-sizing:border-box;width:100%;border:1px solid #d6dfe4;border-radius:9px;padding:11px 12px;background:#fff;color:#22313b;font:inherit}.dialog textarea{resize:vertical;line-height:1.55}.hint{margin:0;color:#74828c;font-size:13px;line-height:1.55}.error{margin:0;color:#b53c32}.dialog footer{display:flex;justify-content:flex-end;gap:9px}.dialog footer button{border:1px solid #d6dfe4;border-radius:9px;background:#fff;padding:9px 13px}.dialog footer .primary{border-color:#168866;background:#168866;color:#fff}
</style>
