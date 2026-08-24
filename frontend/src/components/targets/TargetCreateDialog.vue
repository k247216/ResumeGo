<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="target-dialog-title">
      <header>
        <div>
          <small>新的求职计划</small>
          <h2 id="target-dialog-title">新建求职目标</h2>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <form @submit.prevent="submit">
        <div class="field-row">
          <label>
            公司名称
            <input v-model="companyName" data-test="target-company" autocomplete="off" placeholder="如：腾讯">
          </label>
          <label>
            岗位名称
            <input v-model="jobTitle" data-test="target-job-title" autocomplete="off" placeholder="如：Java 后端开发">
          </label>
        </div>

        <label>
          岗位描述 JD<span class="optional">粘贴原文，用于针对性准备</span>
          <textarea
            v-model="jdText"
            data-test="target-jd"
            rows="4"
            placeholder="岗位职责 / 任职要求…（可留空稍后录入）"
          ></textarea>
        </label>

        <label>
          绑定简历<span class="optional">后续针对该公司修改即为「修改版」</span>
          <select v-model="resumeVersionId" data-test="target-resume">
            <option value="">稍后选择</option>
            <option
              v-for="resume in selectableResumes"
              :key="resume.id"
              :value="String(resume.currentVersion!.id)"
            >
              {{ resume.title }} · V{{ resume.currentVersion!.versionNo }}
            </option>
          </select>
        </label>

        <p class="hint">目标名称默认取「公司 · 岗位」，可在下方覆盖。</p>
        <label>
          目标名称
          <input v-model="name" data-test="target-name" autocomplete="off" :placeholder="autoName || '如：腾讯 · Java 后端开发'">
        </label>

        <p v-if="validationMessage || errorMessage" class="error" role="alert">
          {{ validationMessage || errorMessage }}
        </p>

        <footer>
          <button type="button" @click="$emit('close')">取消</button>
          <button class="primary" type="submit" :disabled="submitting">
            {{ submitting ? '创建中…' : '创建求职目标' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Resume } from '../../types/resume'

export interface TargetDraftPayload {
  name: string
  companyName: string
  jobTitle: string
  jdText: string
  resumeVersionId?: number
}

const props = withDefaults(defineProps<{
  open: boolean
  resumes: Resume[]
  submitting?: boolean
  errorMessage?: string
}>(), { submitting: false, errorMessage: '' })

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'create', payload: TargetDraftPayload): void
}>()

const name = ref('')
const companyName = ref('')
const jobTitle = ref('')
const jdText = ref('')
const resumeVersionId = ref('')
const validationMessage = ref('')
const selectableResumes = computed(() => props.resumes.filter((resume) => resume.currentVersion?.id))

const autoName = computed(() => {
  const parts = [companyName.value.trim(), jobTitle.value.trim()].filter(Boolean)
  return parts.join(' · ')
})

watch(() => props.open, (open) => {
  if (!open) {
    name.value = ''
    companyName.value = ''
    jobTitle.value = ''
    jdText.value = ''
    resumeVersionId.value = ''
    validationMessage.value = ''
  }
})

// 用户尚未自定义名称时，跟随「公司 · 岗位」自动填充。
let lastNameSync = ''
watch([companyName, jobTitle], ([company, title]) => {
  const nextAuto = [company.trim(), title.trim()].filter(Boolean).join(' · ')
  if (name.value === lastNameSync) name.value = nextAuto
  lastNameSync = nextAuto
})

function submit() {
  const finalName = (name.value.trim() || autoName.value)
  if (!finalName) {
    validationMessage.value = '请至少填写公司或岗位'
    return
  }
  validationMessage.value = ''
  const selectedId = Number(resumeVersionId.value)
  emit('create', {
    name: finalName,
    companyName: companyName.value.trim(),
    jobTitle: jobTitle.value.trim(),
    jdText: jdText.value.trim(),
    ...(Number.isSafeInteger(selectedId) && selectedId > 0 ? { resumeVersionId: selectedId } : {}),
  })
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.dialog{width:min(560px,100%);border-radius:16px;background:#fff;color:#141516;box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px;animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.dialog{animation:none}}
.dialog header{display:flex;justify-content:space-between;gap:16px}
.dialog h2{margin:5px 0 20px;font-size:21px}
.dialog small{color:var(--brand,#168866);font-weight:700}
.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.dialog form,.dialog label{display:grid;gap:9px}
.dialog form{gap:16px}
.dialog label{font-weight:700;color:#344552;font-size:13px}
.optional{margin-left:6px;font-weight:500;color:var(--muted,#74828c)}
.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.dialog input,.dialog select,.dialog textarea{border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:10px 12px;background:#fff;color:#22313b;font:inherit;font-weight:400;resize:vertical}
.dialog input:focus,.dialog select:focus,.dialog textarea:focus{outline:none;border-color:var(--brand,#168866);box-shadow:0 0 0 3px var(--brand-soft,rgba(22,139,104,.12))}
.hint{margin:0;color:var(--muted,#74828c);font-size:13px;line-height:1.55}
.error{margin:0;color:var(--danger,#b53c32)}
.dialog footer{display:flex;justify-content:flex-end;gap:9px;margin-top:2px}
.dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:#fff;color:#141516;padding:9px 13px;font:inherit;cursor:pointer}
.dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff;font-weight:600}
.dialog footer .primary:hover{background:var(--accent-hover,#11674f)}
</style>
