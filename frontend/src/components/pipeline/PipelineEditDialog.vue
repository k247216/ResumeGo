<template>
  <div class="overlay" role="dialog" aria-modal="true" aria-label="编辑求职管线">
    <div class="card">
      <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      <h2>编辑管线信息</h2>
      <p class="desc">全量提交以下五项。岗位描述与简历版本可选择「未关联」明确解除。</p>
      <div class="field"><label>管线名称 *</label><input v-model="form.name" data-test="pipeline-edit-name" maxlength="120" /></div>
      <div class="field"><label>公司 *</label><input v-model="form.companyName" data-test="pipeline-edit-company" maxlength="120" /></div>
      <div class="field"><label>岗位 *</label><input v-model="form.roleTitle" data-test="pipeline-edit-role" maxlength="160" /></div>
      <div class="field">
        <label>岗位描述</label>
        <select v-model="jobId" data-test="pipeline-edit-jd">
          <option :value="null">未关联</option>
          <option v-for="j in jobs" :key="j.id" :value="j.id">{{ j.jobTitle }}</option>
        </select>
      </div>
      <div class="field">
        <label>简历版本</label>
        <select v-model="resumeVersionId" data-test="pipeline-edit-resume">
          <option :value="null">未关联</option>
          <template v-for="r in resumes" :key="r.id">
            <option v-for="v in versionsByResume[r.id] ?? []" :key="v.id" :value="v.id">{{ r.title }} · V{{ v.versionNo }}</option>
          </template>
        </select>
      </div>
      <p v-if="error" class="err" data-test="pipeline-edit-error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn ghost" @click="$emit('close')">取消</button>
        <button type="button" class="btn primary" :disabled="submitting || !form.name.trim() || !form.companyName.trim() || !form.roleTitle.trim()" data-test="pipeline-edit-submit" @click="submit">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'
import type { Resume, ResumeVersion } from '../../types/resume'
import type { JobDescription } from '../../types/job'

const props = defineProps<{
  pipeline: CareerPipeline
  resumes: Resume[]
  jobs: JobDescription[]
  versionsByResume: Record<number, ResumeVersion[]>
  submitting: boolean
  error: string
}>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'save', req: { name: string; companyName: string; roleTitle: string; jobDescriptionId: number | null; resumeVersionId: number | null }): void }>()

const form = reactive({ name: props.pipeline.name, companyName: props.pipeline.companyName, roleTitle: props.pipeline.roleTitle })
const jobId = ref<number | null>(props.pipeline.jobDescriptionId)
const resumeVersionId = ref<number | null>(props.pipeline.resumeVersionId)

function submit() {
  emit('save', {
    name: form.name.trim(), companyName: form.companyName.trim(), roleTitle: form.roleTitle.trim(),
    jobDescriptionId: jobId.value, resumeVersionId: resumeVersionId.value,
  })
}
</script>

<style scoped>
.overlay{position:fixed;z-index:2000;inset:0;display:grid;place-items:center;padding:24px;background:rgba(7,8,8,.5)}
.card{width:min(520px,100%);padding:26px;border:1px solid var(--line,#dce4e2);border-radius:18px;background:var(--surface-solid,#fff);color:var(--ink,#26343d);max-height:86vh;overflow-y:auto}
.card h2{margin:0 0 6px;font-size:20px;font-weight:650}
.card .desc{margin:0 0 18px;color:var(--muted,#687586);font-size:13px;line-height:1.6}
.field{display:grid;gap:6px;margin-bottom:14px}
.field label{font-size:13px;font-weight:600;color:var(--copy,#42515c)}
.field input,.field select,.field textarea{box-sizing:border-box;width:100%;padding:9px 11px;border:1px solid var(--line,#d8e0e1);border-radius:10px;background:var(--bg-surface,#fff);color:var(--ink,#26343d);font:inherit;font-size:14px;resize:vertical}
.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}
.btn{border:0;border-radius:10px;padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer}
.btn.ghost{background:transparent;color:var(--copy,#42515c);border:1px solid var(--line,#d8e0e1)}
.btn.primary{background:var(--brand,#168866);color:#fff}
.btn.primary:disabled{opacity:.5;cursor:not-allowed}
.err{margin:10px 0 0;color:var(--danger,#b53c32);font-size:12px}
.close-x{position:absolute;top:14px;right:16px;border:0;background:none;color:var(--muted,#687586);font-size:20px;cursor:pointer}
.row{display:flex;align-items:center;gap:10px;padding:9px 2px;border-bottom:1px solid var(--line,#eef1f0);font-size:13px}
.row .grow{flex:1}
</style>