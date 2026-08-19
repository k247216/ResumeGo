<template>
  <section data-test="target-dashboard" class="dashboard">
    <p>当前求职目标</p>
    <div class="heading">
      <div><h1>{{ target.name }}</h1><span>{{ target.status === 'active' ? '进行中' : '已归档' }}</span></div>
      <button type="button" @click="$emit('switch-target')">切换目标</button>
    </div>
    <article class="next">
      <small>下一步</small><strong>{{ nextActionLabel }}</strong>
      <button data-test="target-next-action" type="button" @click="$emit('action', nextAction)">{{ nextActionButton }} →</button>
    </article>
    <p v-if="materialError" class="material-error">{{ materialError }}</p>
    <div class="materials">
      <article>
        <small>目标岗位</small>
        <strong>{{ jobLabel }}</strong>
        <span>{{ target.jobDescriptionId ? '仅用于当前目标' : '等待录入真实岗位信息' }}</span>
      </article>
      <article>
        <small>当前简历</small>
        <strong>{{ target.resumeVersionId ? (resumeTitle || '已选择版本') : '待选择' }}</strong>
        <span>{{ target.resumeVersionId ? `版本 #${target.resumeVersionId}` : '从本地简历中选择' }}</span>
      </article>
      <article>
        <small>能力证据</small>
        <strong>{{ evidenceCount === null ? '读取中' : `${evidenceCount} 条已引用` }}</strong>
        <span>来自当前简历版本中的事实引用</span>
      </article>
      <article>
        <small>面试准备</small>
        <strong>尚未开始</strong>
        <span>将在后续阶段接入目标上下文</span>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { JobDescription } from '../../types/job'
import type { JobProject } from '../../types/project'

export type TargetDashboardAction = 'add-job' | 'select-resume' | 'open-editor'

const props = withDefaults(defineProps<{ target: JobProject; job?: JobDescription | null; resumeTitle?: string; evidenceCount?: number | null; materialError?: string }>(), { job: null, resumeTitle: '', evidenceCount: null, materialError: '' })
defineEmits<{ (event: 'action', action: TargetDashboardAction): void; (event: 'switch-target'): void }>()
const nextAction = computed<TargetDashboardAction>(() => !props.target.jobDescriptionId ? 'add-job' : !props.target.resumeVersionId ? 'select-resume' : 'open-editor')
const nextActionLabel = computed(() => nextAction.value === 'add-job' ? '录入目标岗位' : nextAction.value === 'select-resume' ? '选择当前简历' : '继续编辑当前简历')
const nextActionButton = computed(() => nextAction.value === 'open-editor' ? '打开编辑器' : '现在完成')
const jobLabel = computed(() => props.job ? [props.job.companyName, props.job.jobTitle].filter(Boolean).join(' · ') : props.target.jobDescriptionId ? '岗位信息已保存' : '待添加')
</script>

<style scoped>
.dashboard{padding:42px}.dashboard>p{color:#168866;font-weight:700}.heading{display:flex;align-items:center;justify-content:space-between}.heading h1{margin:6px 0}.heading span{color:#74808e}.heading button{border:1px solid #d8e0e5;border-radius:8px;background:white;padding:8px 11px}.next{display:grid;gap:8px;margin:26px 0;padding:22px;border-radius:15px;background:linear-gradient(135deg,#18334a,#176358);color:white}.next small{color:#b9ddd3}.next strong{font-size:19px}.next button{justify-self:start;border:0;border-radius:8px;background:#63d0a8;padding:8px 11px}.materials{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.materials article{display:grid;gap:8px;padding:18px;border:1px solid #dfe5e9;border-radius:12px;background:white}.materials small,.materials span{color:#7b8794}.materials span{font-size:12px}.material-error{border:1px solid #efc7c2;border-radius:9px;background:#fff2f0;color:#a23d35!important;padding:9px 12px}
</style>
