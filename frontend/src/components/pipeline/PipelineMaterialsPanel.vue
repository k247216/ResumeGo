<template>
  <section class="materials-panel" data-test="pipeline-materials">
    <div class="section-head"><h3 class="section-title">岗位材料</h3></div>
    <div class="material-row">
      <span class="label">岗位描述</span>
      <span v-if="jobLabel" class="value">{{ jobLabel }}</span>
      <span v-else-if="pipeline.jobDescriptionId" class="value warn">关联不可用（ID {{ pipeline.jobDescriptionId }}）</span>
      <span v-else class="value muted">未关联</span>
    </div>
    <div class="material-row">
      <span class="label">简历版本</span>
      <span v-if="resumeLabel" class="value">{{ resumeLabel }}</span>
      <span v-else-if="pipeline.resumeVersionId" class="value warn">关联不可用（ID {{ pipeline.resumeVersionId }}）</span>
      <span v-else class="value muted">未关联</span>
    </div>
    <button v-if="editable" type="button" class="soft-btn" data-test="pipeline-materials-edit" @click="$emit('edit')">编辑材料</button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'
import type { Resume, ResumeVersion } from '../../types/resume'
import type { JobDescription } from '../../types/job'

const props = defineProps<{
  pipeline: CareerPipeline
  resumes: Resume[]
  jobs: JobDescription[]
  versionsByResume: Record<number, ResumeVersion[]>
  loading: boolean
}>()
defineEmits<{ (e: 'edit'): void }>()

const editable = computed(() => props.pipeline.lifecycle === 'ACTIVE' || props.pipeline.lifecycle === 'PAUSED')
const jobLabel = computed(() => props.jobs.find((j) => j.id === props.pipeline.jobDescriptionId)?.jobTitle ?? null)
const resumeLabel = computed(() => {
  if (!props.pipeline.resumeVersionId) return null
  for (const [, versions] of Object.entries(props.versionsByResume)) {
    const v = versions.find((x) => x.id === props.pipeline.resumeVersionId)
    if (v) return '简历 V' + v.versionNo
  }
  const r = props.resumes.find((x) => x.currentVersion?.id === props.pipeline.resumeVersionId)
  return r ? r.title : null
})
</script>

<style scoped>
.materials-panel{padding:18px 2px;border:1px solid var(--border-subtle);border-radius:16px;background:var(--bg-surface);display:flex;flex-direction:column;gap:12px}
.section-head{margin-bottom:2px}
.section-title{margin:0;font-size:13px;font-weight:600;color:var(--ink)}
.material-row{display:flex;gap:10px;align-items:baseline;font-size:13px}
.label{flex:0 0 72px;color:var(--muted)}
.value{color:var(--ink)}
.value.muted{color:var(--muted)}
.value.warn{color:var(--warning)}
.soft-btn{align-self:flex-start;padding:6px 12px;border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);color:var(--copy);font-size:12px;cursor:pointer}
.soft-btn:hover{border-color:var(--brand);color:var(--brand)}
</style>