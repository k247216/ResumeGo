<template>
  <section v-if="resume" class="asset-workspace" data-test="asset-workspace">
    <header class="workspace-identity">
      <p class="workspace-kicker">{{ kindLabel(resume) }}</p>
      <h2>{{ resume.title }}</h2>
      <p class="workspace-line">{{ identityLine }}</p>
      <p v-if="resume.archivedAt" class="workspace-archived" data-test="archived-note">该简历已归档，恢复后重新出现在默认列表。</p>
    </header>

    <div class="workspace-actions">
      <router-link
        class="btn-primary"
        data-test="continue-editing"
        :to="buildResumeEditorLocation({ resumeId: resume.id, versionId: resume.currentVersion?.id })"
      >继续编辑</router-link>
      <router-link
        v-if="resume.currentVersion"
        class="btn-secondary"
        data-test="view-current-version"
        :to="buildResumeEditorLocation({ resumeId: resume.id, versionId: resume.currentVersion.id })"
      >查看当前版本</router-link>
      <button
        type="button"
        class="btn-secondary"
        data-test="open-fork"
        :disabled="!!resume.archivedAt"
        @click="emit('fork')"
      >创建岗位表达副本</button>
    </div>

    <section class="workspace-summary" data-test="workspace-summary">
      <h3 class="section-title">当前版本摘要</h3>
      <div class="stat-row"><span>当前版本</span><strong>{{ resume.currentVersion ? `V${resume.currentVersion.versionNo}` : '尚未创建' }}</strong></div>
      <div class="stat-row"><span>项目经历</span><strong>{{ projectCount }} 条</strong></div>
      <div class="stat-row"><span>技能项</span><strong>{{ skillCount }} 项</strong></div>
      <div class="stat-row"><span>最近更新</span><strong>{{ updatedLabel(resume) }}</strong></div>
    </section>
  </section>
  <div v-else class="workspace-empty" data-test="workspace-empty">选择一份简历开始</div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Resume, ResumeVersion } from '../../types/resume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const props = defineProps<{
  resume: Resume | null
  version: ResumeVersion | null
}>()

const emit = defineEmits<{ fork: [] }>()

const identityLine = computed(() => {
  const basic = props.version?.content.basicInfo
  return [basic?.name || '姓名待补充', basic?.targetRole || '目标方向待补充'].join(' · ')
})
const projectCount = computed(() => props.version?.content.projects?.length ?? 0)
const skillCount = computed(() => {
  const content = props.version?.content
  return content?.skillCategories?.reduce((count, category) => count + (category.skills?.length ?? 0), 0)
    || content?.skills?.length || 0
})

function kindLabel(resume: Resume) {
  return resume.kind === 'JOB_EXPRESSION' ? '岗位表达副本' : '通用简历'
}
function updatedLabel(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return '未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '未知' : `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>

<style scoped>
.asset-workspace{display:grid;gap:18px;align-content:start}
.workspace-kicker{margin:0;color:var(--muted);font-size:11px;font-weight:650;letter-spacing:.06em}
.workspace-identity h2{margin:6px 0 8px;font-size:20px;font-weight:700;color:var(--ink)}
.workspace-line{margin:0;color:var(--copy);font-size:13px}
.workspace-archived{margin:8px 0 0;color:var(--warning,#ad6800);font-size:12px}
.workspace-actions{display:flex;flex-wrap:wrap;gap:8px}
.btn-primary{border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:8px 15px;font-size:13px;font-weight:600;text-decoration:none}
.btn-secondary{border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--copy);padding:8px 13px;font-size:12.5px;cursor:pointer;text-decoration:none}
.btn-secondary:hover{background:var(--bg-hover)}
.btn-secondary:disabled{opacity:.5;cursor:default}
.workspace-summary{display:grid;gap:8px;border-top:1px solid var(--border-subtle);padding-top:14px}
.section-title{margin:0 0 2px;font-size:11px;font-weight:650;letter-spacing:.06em;color:var(--muted)}
.stat-row{display:flex;align-items:center;justify-content:space-between;font-size:12.5px;color:var(--copy)}
.stat-row strong{font-variant-numeric:tabular-nums}
.workspace-empty{display:grid;place-items:center;min-height:160px;color:var(--muted);font-size:13px}
</style>
