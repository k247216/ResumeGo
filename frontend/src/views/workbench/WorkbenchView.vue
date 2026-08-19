<template>
  <WorkspaceLoadingState v-if="state === 'loading'" />
  <WorkspaceErrorState v-else-if="state === 'error'" :message="errorMessage" @retry="loadWorkspace" />
  <section v-else-if="state === 'first-run'" data-test="first-run-workspace" class="first-run">
    <p>欢迎使用 ResumeGo</p><h1>先建立本地资料，再决定从哪里开始</h1><span>你可以从空白简历开始，也可以先录入目标岗位；Markdown 导入将在下一阶段接入。</span>
    <div><button type="button">创建空白简历</button><button type="button">添加目标岗位</button></div>
  </section>
  <LocalLibraryState v-else-if="state === 'library'" :resumes="resumes" />
  <TargetDashboard v-else-if="targetsStore.activeTarget" :target="targetsStore.activeTarget" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listJobDescriptions } from '../../api/job'
import { listResumes } from '../../api/resume'
import WorkspaceLoadingState from '../../components/workbench/WorkspaceLoadingState.vue'
import WorkspaceErrorState from '../../components/workbench/WorkspaceErrorState.vue'
import LocalLibraryState from '../../components/workbench/LocalLibraryState.vue'
import TargetDashboard from '../../components/workbench/TargetDashboard.vue'
import { useTargetsStore } from '../../stores/targets'
import type { JobDescription } from '../../types/job'
import type { Resume } from '../../types/resume'
import { resolveWorkspaceLaunchState } from '../../utils/workspaceLaunchState'

const targetsStore = useTargetsStore()
const resumes = ref<Resume[]>([])
const jobs = ref<JobDescription[]>([])
const loading = ref(true)
const localError = ref('')
const errorMessage = computed(() => localError.value || targetsStore.errorMessage)
const state = computed(() => resolveWorkspaceLaunchState({ loading: loading.value || targetsStore.loading, hasError: Boolean(errorMessage.value), resumeCount: resumes.value.length, targetCount: targetsStore.targets.length }))

async function loadWorkspace() {
  loading.value = true
  localError.value = ''
  try {
    const [, resumeResponse, jobResponse] = await Promise.all([targetsStore.load(), listResumes(), listJobDescriptions()])
    resumes.value = resumeResponse.data
    jobs.value = jobResponse.data
    if (targetsStore.errorMessage) localError.value = targetsStore.errorMessage
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '读取本地工作区失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>.first-run{max-width:720px;padding:76px 52px}.first-run>p{color:#168866;font-weight:700}.first-run h1{font-size:34px;margin:8px 0 12px}.first-run>span{color:#718090;line-height:1.7}.first-run>div{display:flex;gap:10px;margin-top:25px}.first-run button{border:1px solid #ccd7dd;border-radius:9px;background:white;padding:10px 14px}.first-run button:first-child{border-color:#168866;background:#168866;color:white}</style>
