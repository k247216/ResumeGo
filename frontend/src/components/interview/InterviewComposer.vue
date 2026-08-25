<template>
  <section class="interview-composer" data-test="interview-composer">
    <InterviewModePicker :model-value="composer.mode.value" @update:model-value="composer.switchMode($event)" />

    <div v-if="composer.mode.value" class="composer-body">
      <RoleBasedSetup
        v-if="composer.mode.value === 'ROLE_BASED'"
        :draft="composer.roleDraft.value"
        :project-options="projectOptions"
        :resume-options="resumeOptions"
        :persona-options="personaOptions"
        @update:draft="composer.roleDraft.value = $event"
      />
      <KnowledgeTrainingSetup
        v-else-if="composer.mode.value === 'KNOWLEDGE_TRAINING'"
        :draft="composer.knowledgeDraft.value"
        :document-options="documentOptions"
        @update:draft="composer.knowledgeDraft.value = $event"
      />
      <ExperienceSimulationSetup
        v-else
        :draft="composer.experienceDraft.value"
        :question-set-options="questionSetOptions"
        :persona-options="personaOptions"
        @update:draft="composer.experienceDraft.value = $event"
      />

      <p v-if="composer.missingHint.value" class="composer-hint" data-test="composer-missing-hint">{{ composer.missingHint.value }}</p>
      <p v-if="composer.error.value" class="composer-error" data-test="composer-error">{{ composer.error.value }}</p>

      <button
        type="button"
        class="composer-start"
        data-test="composer-start"
        :disabled="!composer.canStart.value"
        @click="start"
      >{{ composer.submitting.value ? '开始中…' : '开始训练' }}</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useInterviewComposer } from '../../composables/useInterviewComposer'
import type { InterviewPlanResponse } from '../../types/interview'
import { listResumes } from '../../api/resume'
import { listProjects } from '../../api/project'
import { listInterviewerPersonas } from '../../api/interview'
import { listInterviewQuestionSets } from '../../api/interview'
import { listKnowledgeDocuments } from '../../api/knowledge'
import InterviewModePicker from './InterviewModePicker.vue'
import RoleBasedSetup, { type SelectOption } from './RoleBasedSetup.vue'
import KnowledgeTrainingSetup from './KnowledgeTrainingSetup.vue'
import ExperienceSimulationSetup from './ExperienceSimulationSetup.vue'

const emit = defineEmits<{ started: [plan: InterviewPlanResponse] }>()

const composer = useInterviewComposer()
const projectOptions = ref<SelectOption[]>([])
const resumeOptions = ref<SelectOption[]>([])
const personaOptions = ref<SelectOption[]>([])
const documentOptions = ref<SelectOption[]>([])
const questionSetOptions = ref<SelectOption[]>([])

async function loadOptions() {
  const results = await Promise.allSettled([
    listProjects(),
    listResumes(),
    listInterviewerPersonas(),
    listKnowledgeDocuments(),
    listInterviewQuestionSets(),
  ])
  if (results[0].status === 'fulfilled') {
    projectOptions.value = results[0].value.data
      .filter((item) => item.status === 'active')
      .map((item) => ({ value: item.id, label: item.name }))
  }
  if (results[1].status === 'fulfilled') {
    resumeOptions.value = results[1].value.data
      .filter((item) => !item.archivedAt && item.currentVersion)
      .map((item) => ({
        value: item.currentVersion!.id,
        label: `${item.title} · V${item.currentVersion!.versionNo}`,
      }))
  }
  if (results[2].status === 'fulfilled') {
    personaOptions.value = results[2].value.data
      .map((item) => ({ value: item.id, label: `${item.name} · ${item.title}` }))
  }
  if (results[3].status === 'fulfilled') {
    documentOptions.value = results[3].value.data
      .filter((item) => item.processingStatus === 'COMPLETED')
      .map((item) => ({ value: item.id, label: item.title }))
  }
  if (results[4].status === 'fulfilled') {
    questionSetOptions.value = results[4].value.data
      .filter((item) => !item.archived)
      .map((item) => ({ value: item.id, label: item.title }))
  }
}

async function start() {
  try {
    const plan = await composer.start()
    emit('started', plan)
  } catch {
    // 错误已由 composer 状态记录并展示，保留草稿供重试
  }
}

onMounted(() => { void loadOptions() })
</script>

<style scoped>
.interview-composer{display:grid;gap:16px}
.composer-body{display:grid;gap:14px;border:1px solid var(--border-subtle);border-radius:12px;background:var(--bg-surface);padding:16px}
.composer-hint{margin:0;color:var(--muted);font-size:12.5px}
.composer-error{margin:0;color:var(--danger);font-size:12.5px}
.composer-start{justify-self:start;border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:9px 18px;font-size:13px;font-weight:600;cursor:pointer}
.composer-start:disabled{opacity:.5;cursor:default}
</style>
