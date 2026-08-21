<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="target-job-title">
      <header><div><small>目标岗位</small><h2 id="target-job-title">粘贴这次求职对应的岗位信息</h2></div><button type="button" aria-label="关闭" @click="$emit('close')">×</button></header>
      <form @submit.prevent="submit">
        <div class="field-row">
          <div class="field-block">
            <label>岗位名称<input v-model="jobTitle" data-test="job-title" placeholder="例如：Java 后端实习"></label>
            <div v-if="hotJobTitles.length" class="quick-picks">
              <span class="quick-label">{{ jobTitle ? '匹配' : '热门岗位' }}</span>
              <div class="quick-chips" data-test="job-title-chips">
                <button v-for="title in hotJobTitles" :key="title" type="button" class="chip" data-test="quick-chip" @click="jobTitle = title">{{ title }}</button>
              </div>
            </div>
          </div>
          <div class="field-block">
            <label>公司名称（可选）<input v-model="companyName" data-test="job-company" placeholder="例如：某某科技"></label>
            <div v-if="hotCompanies.length" class="quick-picks">
              <span class="quick-label">{{ companyName ? '匹配' : '热门公司' }}</span>
              <div class="quick-chips" data-test="company-chips">
                <button v-for="name in hotCompanies" :key="name" type="button" class="chip" data-test="quick-chip" @click="companyName = name">{{ name }}</button>
              </div>
            </div>
          </div>
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
import { computed, ref, watch } from 'vue'
import type { CreateJobDescriptionRequest } from '../../types/job'
import { filterCompanies, filterJobTitles } from '../../utils/jobQuickPick'

const props = withDefaults(defineProps<{ open: boolean; submitting?: boolean; errorMessage?: string }>(), { submitting: false, errorMessage: '' })
const emit = defineEmits<{ (event: 'close'): void; (event: 'create', payload: CreateJobDescriptionRequest): void }>()
const jobTitle = ref('')
const companyName = ref('')
const rawText = ref('')
const validationMessage = ref('')

const hotJobTitles = computed(() => filterJobTitles(jobTitle.value))
const hotCompanies = computed(() => filterCompanies(companyName.value))

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
.dialog-backdrop{position:fixed;inset:0;z-index:45;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}.dialog{width:min(650px,100%);border-radius:16px;background:var(--surface-solid,#fff);color:var(--ink,#141516);box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px}.dialog header{display:flex;justify-content:space-between;gap:16px}.dialog h2{margin:5px 0 20px;font-size:21px}.dialog small{color:var(--brand,#168866);font-weight:700}.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px}.dialog form,.dialog label{display:grid;gap:9px}.dialog form{gap:16px}.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}.field-block{display:grid;gap:9px}.quick-picks{display:grid;gap:6px;margin-top:1px}.quick-label{color:var(--muted,#74828c);font-size:12px;font-weight:700;letter-spacing:.02em}.quick-chips{display:flex;flex-wrap:wrap;gap:6px}.chip{border:1px solid var(--line,#d6dfe4);border-radius:999px;background:var(--surface-solid,#fff);color:var(--copy,#505357);font-size:12px;font-weight:600;padding:4px 10px;cursor:pointer;transition:border-color .15s ease,color .15s ease,background .15s ease}.chip:hover{border-color:var(--brand,#168866);color:var(--brand,#168866);background:var(--brand-soft,#edf7f3)}.dialog label{font-weight:700;color:var(--ink,#344552)}.dialog input,.dialog textarea{box-sizing:border-box;width:100%;border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:11px 12px;background:var(--surface-solid,#fff);color:var(--ink,#22313b);font:inherit}.dialog textarea{resize:vertical;line-height:1.55}.hint{margin:0;color:var(--muted,#74828c);font-size:13px;line-height:1.55}.error{margin:0;color:var(--danger,#b53c32)}.dialog footer{display:flex;justify-content:flex-end;gap:9px}.dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:var(--surface-solid,#fff);color:var(--ink,#141516);padding:9px 13px}.dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff}
</style>
