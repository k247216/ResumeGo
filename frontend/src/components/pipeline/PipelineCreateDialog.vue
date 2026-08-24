<template>
  <div class="overlay" role="dialog" aria-modal="true" aria-label="新建求职管线">
    <div class="card">
      <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      <h2>新建求职管线</h2>
      <p class="desc">记录一个新的求职机会。初始阶段留空则使用默认阶段，之后可进入阶段管理调整。</p>
      <div class="field"><label>管线名称 *</label><input v-model="form.name" data-test="pipeline-create-name" placeholder="如：腾讯 Java 后端" maxlength="120" /></div>
      <div class="field"><label>公司 *</label><input v-model="form.companyName" data-test="pipeline-create-company" placeholder="如：腾讯" maxlength="120" /></div>
      <div class="field"><label>岗位 *</label><input v-model="form.roleTitle" data-test="pipeline-create-role" placeholder="如：Java 后端实习" maxlength="160" /></div>
      <div class="field">
        <label>岗位描述（可选）</label>
        <select v-model="jobId" data-test="pipeline-create-jd">
          <option :value="null">未关联</option>
          <option v-for="j in jobs" :key="j.id" :value="j.id">{{ j.jobTitle }}</option>
        </select>
      </div>
      <div class="field">
        <label>简历版本（可选）</label>
        <select v-model="resumeVersionId" data-test="pipeline-create-resume">
          <option :value="null">未关联</option>
          <template v-for="r in resumes" :key="r.id">
            <option v-for="v in versionsByResume[r.id] ?? []" :key="v.id" :value="v.id">{{ r.title }} · V{{ v.versionNo }}</option>
          </template>
        </select>
      </div>
      <p v-if="error" class="err" data-test="pipeline-create-error">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn ghost" @click="$emit('close')">取消</button>
        <button type="button" class="btn primary" :disabled="submitting || !form.name.trim() || !form.companyName.trim() || !form.roleTitle.trim()" data-test="pipeline-create-submit" @click="submit">创建</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { Resume, ResumeVersion } from '../../types/resume'
import type { JobDescription } from '../../types/job'

const props = defineProps<{
  resumes: Resume[]
  jobs: JobDescription[]
  versionsByResume: Record<number, ResumeVersion[]>
  submitting: boolean
  error: string
}>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'create', req: { name: string; companyName: string; roleTitle: string; jobDescriptionId: number | null; resumeVersionId: number | null; stages: string[] }): void }>()

const form = reactive({ name: '', companyName: '', roleTitle: '' })
const jobId = ref<number | null>(null)
const resumeVersionId = ref<number | null>(null)
watch(() => props.error, () => { if (props.error) { /* keep open */ } })

function submit() {
  emit('create', {
    name: form.name.trim(), companyName: form.companyName.trim(), roleTitle: form.roleTitle.trim(),
    jobDescriptionId: jobId.value, resumeVersionId: resumeVersionId.value, stages: [],
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