<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="target-dialog-title">
      <header>
        <div>
          <small>新的求职目标</small>
          <h2 id="target-dialog-title">为一次真实求职建立上下文</h2>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <form @submit.prevent="submit">
        <label>
          目标名称
          <input
            v-model="name"
            data-test="target-name"
            autocomplete="off"
            placeholder="例如：腾讯 · Java 后端实习"
          >
        </label>
        <label>
          当前简历（可选）
          <select v-model="resumeVersionId" data-test="target-resume">
            <option value="">稍后选择</option>
            <option
              v-for="resume in selectableResumes"
              :key="resume.id"
              :value="String(resume.currentVersion!.id)"
            >
              {{ resume.title }} · v{{ resume.currentVersion!.versionNo }}
            </option>
          </select>
        </label>
        <p class="hint">目标岗位内容会在进入该求职目标后单独录入。</p>
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
import type { CreateJobProjectRequest } from '../../types/project'
import type { Resume } from '../../types/resume'

const props = withDefaults(defineProps<{
  open: boolean
  resumes: Resume[]
  submitting?: boolean
  errorMessage?: string
}>(), { submitting: false, errorMessage: '' })

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'create', payload: CreateJobProjectRequest): void
}>()

const name = ref('')
const resumeVersionId = ref('')
const validationMessage = ref('')
const selectableResumes = computed(() => props.resumes.filter((resume) => resume.currentVersion?.id))

watch(() => props.open, (open) => {
  if (!open) {
    name.value = ''
    resumeVersionId.value = ''
    validationMessage.value = ''
  }
})

function submit() {
  const normalizedName = name.value.trim()
  if (!normalizedName) {
    validationMessage.value = '请输入求职目标名称'
    return
  }
  validationMessage.value = ''
  const selectedId = Number(resumeVersionId.value)
  emit('create', {
    name: normalizedName,
    ...(Number.isSafeInteger(selectedId) && selectedId > 0 ? { resumeVersionId: selectedId } : {}),
  })
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(19,31,43,.38);padding:24px}.dialog{width:min(520px,100%);border-radius:16px;background:#fff;box-shadow:0 22px 60px rgba(15,35,47,.22);padding:22px}.dialog header{display:flex;justify-content:space-between;gap:16px}.dialog h2{margin:5px 0 20px;font-size:21px}.dialog small{color:#168866;font-weight:700}.dialog header button{align-self:start;border:0;background:none;font-size:24px}.dialog form,.dialog label{display:grid;gap:9px}.dialog form{gap:16px}.dialog label{font-weight:700;color:#344552}.dialog input,.dialog select{border:1px solid #d6dfe4;border-radius:9px;padding:11px 12px;background:#fff;color:#22313b}.hint{margin:0;color:#74828c;font-size:13px;line-height:1.55}.error{margin:0;color:#b53c32}.dialog footer{display:flex;justify-content:flex-end;gap:9px}.dialog footer button{border:1px solid #d6dfe4;border-radius:9px;background:#fff;padding:9px 13px}.dialog footer .primary{border-color:#168866;background:#168866;color:#fff}
</style>
