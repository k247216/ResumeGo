<template>
  <WorkspaceLoadingState v-if="state === 'loading'" />
  <WorkspaceErrorState v-else-if="state === 'error'" :message="errorMessage" @retry="loadWorkspace" />
  <section v-else-if="state === 'first-run'" data-test="first-run-workspace" class="first-run">
    <p>欢迎使用 ResumeGo</p><h1>先建立本地资料，再决定从哪里开始</h1><span>你可以从空白简历开始，也可以先录入目标岗位；Markdown 导入将在下一阶段接入。</span>
    <div><button type="button" @click="openBlankResume">创建空白简历</button><button type="button" @click="openTargetDialog">添加目标岗位</button></div>
  </section>
  <LocalLibraryState
    v-else-if="state === 'library'"
    :resumes="resumes"
    @create-target="openTargetDialog"
    @open-resume="openResume"
  />
  <TargetDashboard
    v-else-if="targetsStore.activeTarget"
    :target="targetsStore.activeTarget"
    :job="activeJob"
    :resume-title="activeResumeTitle"
    :material-error="targetMaterialError"
    @action="handleTargetAction"
    @switch-target="router.push({ name: 'targets' })"
  />
  <TargetCreateDialog
    :open="targetDialogOpen"
    :resumes="resumes"
    :submitting="creatingTarget"
    :error-message="targetCreateError"
    @close="closeTargetDialog"
    @create="createTarget"
  />
  <TargetJobDialog
    :open="jobDialogOpen"
    :submitting="updatingTargetMaterials"
    :error-message="targetMaterialError"
    @close="closeMaterialDialogs"
    @create="saveTargetJob"
  />
  <TargetResumeDialog
    :open="resumeDialogOpen"
    :resumes="resumes"
    :submitting="updatingTargetMaterials"
    :error-message="targetMaterialError"
    @close="closeMaterialDialogs"
    @select="selectTargetResume"
    @create-resume="createResumeForTarget"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createJobDescription, getJobDescription } from '../../api/job'
import { listResumes } from '../../api/resume'
import TargetCreateDialog from '../../components/targets/TargetCreateDialog.vue'
import TargetJobDialog from '../../components/targets/TargetJobDialog.vue'
import TargetResumeDialog from '../../components/targets/TargetResumeDialog.vue'
import WorkspaceLoadingState from '../../components/workbench/WorkspaceLoadingState.vue'
import WorkspaceErrorState from '../../components/workbench/WorkspaceErrorState.vue'
import LocalLibraryState from '../../components/workbench/LocalLibraryState.vue'
import TargetDashboard, { type TargetDashboardAction } from '../../components/workbench/TargetDashboard.vue'
import { useTargetsStore } from '../../stores/targets'
import type { CreateJobProjectRequest } from '../../types/project'
import type { CreateJobDescriptionRequest, JobDescription } from '../../types/job'
import type { Resume } from '../../types/resume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'
import { resolveWorkspaceLaunchState } from '../../utils/workspaceLaunchState'

const targetsStore = useTargetsStore()
const router = useRouter()
const resumes = ref<Resume[]>([])
const loading = ref(true)
const localError = ref('')
const targetDialogOpen = ref(false)
const creatingTarget = ref(false)
const targetCreateError = ref('')
const jobDialogOpen = ref(false)
const resumeDialogOpen = ref(false)
const updatingTargetMaterials = ref(false)
const targetMaterialError = ref('')
const activeJob = ref<JobDescription | null>(null)
const errorMessage = computed(() => localError.value)
const state = computed(() => resolveWorkspaceLaunchState({ loading: loading.value || targetsStore.loading, hasError: Boolean(errorMessage.value), resumeCount: resumes.value.length, targetCount: targetsStore.targets.length }))
const activeResumeTitle = computed(() => resumes.value.find((resume) => resume.currentVersion?.id === targetsStore.activeTarget?.resumeVersionId)?.title ?? '')

async function loadWorkspace() {
  loading.value = true
  localError.value = ''
  try {
    const [, resumeResponse] = await Promise.all([targetsStore.load(), listResumes()])
    resumes.value = resumeResponse.data
    if (targetsStore.errorMessage) localError.value = targetsStore.errorMessage
    await loadActiveJob()
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '读取本地工作区失败'
  } finally {
    loading.value = false
  }
}

async function loadActiveJob() {
  activeJob.value = null
  targetMaterialError.value = ''
  const jobId = targetsStore.activeTarget?.jobDescriptionId
  if (!jobId) return
  try { activeJob.value = (await getJobDescription(jobId)).data }
  catch (error) { targetMaterialError.value = error instanceof Error ? error.message : '读取目标岗位失败' }
}

function openTargetDialog() {
  targetCreateError.value = ''
  targetDialogOpen.value = true
}

function closeTargetDialog() {
  if (creatingTarget.value) return
  targetDialogOpen.value = false
  targetCreateError.value = ''
}

async function createTarget(payload: CreateJobProjectRequest) {
  creatingTarget.value = true
  targetCreateError.value = ''
  try {
    await targetsStore.create(payload)
    targetDialogOpen.value = false
  } catch (error) {
    targetCreateError.value = error instanceof Error ? error.message : '创建求职目标失败'
  } finally {
    creatingTarget.value = false
  }
}

function openBlankResume() {
  void router.push(buildResumeEditorLocation({ mode: 'blank' }))
}

function openResume(resumeId: number) {
  const resume = resumes.value.find((item) => item.id === resumeId)
  void router.push(buildResumeEditorLocation({ resumeId, versionId: resume?.currentVersion?.id }))
}

function handleTargetAction(action: TargetDashboardAction) {
  targetMaterialError.value = ''
  if (action === 'add-job') { jobDialogOpen.value = true; return }
  if (action === 'select-resume') { resumeDialogOpen.value = true; return }
  openTargetEditor()
}

function openTargetEditor() {
  const target = targetsStore.activeTarget
  if (!target?.resumeVersionId) return
  void router.push(buildResumeEditorLocation({ versionId: target.resumeVersionId, targetId: target.id }))
}

function closeMaterialDialogs() {
  if (updatingTargetMaterials.value) return
  jobDialogOpen.value = false
  resumeDialogOpen.value = false
  targetMaterialError.value = ''
}

async function saveTargetJob(payload: CreateJobDescriptionRequest) {
  const target = targetsStore.activeTarget
  if (!target) return
  updatingTargetMaterials.value = true
  targetMaterialError.value = ''
  try {
    const response = await createJobDescription(payload)
    await targetsStore.updateLinks(target.id, { jobDescriptionId: response.data.id, resumeVersionId: target.resumeVersionId })
    activeJob.value = response.data
    jobDialogOpen.value = false
  } catch (error) {
    targetMaterialError.value = error instanceof Error ? error.message : '保存目标岗位失败'
  } finally { updatingTargetMaterials.value = false }
}

async function selectTargetResume(versionId: number) {
  const target = targetsStore.activeTarget
  if (!target) return
  updatingTargetMaterials.value = true
  targetMaterialError.value = ''
  try {
    await targetsStore.updateLinks(target.id, { jobDescriptionId: target.jobDescriptionId, resumeVersionId: versionId })
    resumeDialogOpen.value = false
  } catch (error) {
    targetMaterialError.value = error instanceof Error ? error.message : '关联当前简历失败'
  } finally { updatingTargetMaterials.value = false }
}

function createResumeForTarget() {
  const targetId = targetsStore.activeTarget?.id
  resumeDialogOpen.value = false
  void router.push(buildResumeEditorLocation({ mode: 'blank', targetId }))
}

onMounted(loadWorkspace)
</script>

<style scoped>.first-run{max-width:720px;padding:76px 52px}.first-run>p{color:#168866;font-weight:700}.first-run h1{font-size:34px;margin:8px 0 12px}.first-run>span{color:#718090;line-height:1.7}.first-run>div{display:flex;gap:10px;margin-top:25px}.first-run button{border:1px solid #ccd7dd;border-radius:9px;background:white;padding:10px 14px}.first-run button:first-child{border-color:#168866;background:#168866;color:white}</style>
