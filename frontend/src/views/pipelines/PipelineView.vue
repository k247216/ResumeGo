<template>
  <section data-test="pipeline-view" class="pipeline-view">
    <PageHeader eyebrow="求职管线" title="求职机会" subtitle="管理你的求职机会：阶段推进、岗位材料与关联安排。">
      <template #actions>
        <button type="button" class="header-btn" data-test="pipeline-refresh" :disabled="store.loading" @click="store.retry">刷新</button>
        <button type="button" class="header-btn btn-primary" data-test="pipeline-create" @click="createOpen = true">新建求职管线</button>
      </template>
    </PageHeader>

    <div class="pipeline-body">
      <!-- 左栏：列表 -->
      <PipelineListRail
        :pipelines="visiblePipelines"
        :selected-id="store.selectedPipelineId"
        :loading="store.loading"
        :error-message="store.errorMessage"
        @select="handleSelect"
        @retry="store.retry"
        @create="createOpen = true"
      />

      <!-- 右栏：详情 -->
      <main class="detail-pane">
        <div v-if="!store.selectedPipeline" class="detail-empty">
          <template v-if="store.pipelines.length === 0">
            <strong>还没有求职管线</strong>
            <span>创建第一条求职管线，开始管理你的机会。</span>
            <button type="button" class="state-primary" data-test="pipeline-empty-create" @click="createOpen = true">创建第一条求职管线</button>
          </template>
          <template v-else>
            <strong>选择一条求职管线</strong>
            <span>从左侧列表选择要查看的机会。</span>
          </template>
        </div>

        <template v-else>
          <PipelineIdentityPanel
            :pipeline="store.selectedPipeline"
            :is-busy="mutationBusy"
            @edit="editOpen = true"
            @archive="archiveCurrent"
            @restore="restoreCurrent"
          />

          <PipelineStageTrack
            :pipeline="store.selectedPipeline"
            :history="store.transitionHistoryByPipelineId[store.selectedPipeline.id] ?? []"
            :history-loading="store.historyLoadingPipelineId === store.selectedPipeline.id"
            :history-error="store.historyErrorMessage"
            @transition="openTransition"
            @manage-stages="stageManagerOpen = true"
            @load-history="loadHistory"
          />

          <div class="detail-columns">
            <PipelineMaterialsPanel
              :pipeline="store.selectedPipeline"
              :resumes="resumes"
              :jobs="jobs"
              :versions-by-resume="versionsByResume"
              :loading="auxLoading"
              @edit="editOpen = true"
            />
            <PipelineRelationsPanel
              :pipeline="store.selectedPipeline"
              :schedule-events="scheduleEvents"
              :interview-plans="interviewPlans"
              @manage="relationOpen = true"
              @go-schedule="router.push({ name: 'schedule' })"
              @go-interview="router.push({ name: 'interview' })"
            />
          </div>
        </template>
      </main>
    </div>

    <!-- 对话框与抽屉 -->
    <PipelineCreateDialog v-if="createOpen" :resumes="resumes" :jobs="jobs" :versions-by-resume="versionsByResume" :submitting="creating" :error="store.errorMessage" @close="createOpen = false" @create="handleCreate" />
    <PipelineEditDialog v-if="editOpen && store.selectedPipeline" :pipeline="store.selectedPipeline" :resumes="resumes" :jobs="jobs" :versions-by-resume="versionsByResume" :submitting="saving" :error="store.errorMessage" @close="editOpen = false" @save="handleUpdate" />
    <PipelineStageManagerDialog v-if="stageManagerOpen && store.selectedPipeline" :pipeline="store.selectedPipeline" :busy="mutationBusy" :error="store.errorMessage" @close="stageManagerOpen = false" @add="handleAddStage" @rename="handleRenameStage" @move="handleMoveStage" />
    <PipelineTransitionDialog v-if="transitionOpen && store.selectedPipeline" :pipeline="store.selectedPipeline" :target-stage-id="transitionTargetId" :busy="mutationBusy" :error="store.errorMessage" @close="transitionOpen = false" @confirm="handleTransition" />
    <PipelineRelationDialog v-if="relationOpen && store.selectedPipeline" :pipeline="store.selectedPipeline" :schedule-events="scheduleEvents" :interview-plans="interviewPlans" :busy="mutationBusy" :error="store.errorMessage" @close="relationOpen = false" @toggle-schedule="handleToggleSchedule" @toggle-interview="handleToggleInterview" />
    <PipelineHistoryDrawer v-if="historyOpen && store.selectedPipeline" :pipeline="store.selectedPipeline" :history="store.transitionHistoryByPipelineId[store.selectedPipeline.id] ?? []" :loading="store.historyLoadingPipelineId === store.selectedPipeline.id" :error="store.historyErrorMessage" @close="historyOpen = false" @load="loadHistory" />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import PipelineListRail from '../../components/pipeline/PipelineListRail.vue'
import PipelineIdentityPanel from '../../components/pipeline/PipelineIdentityPanel.vue'
import PipelineStageTrack from '../../components/pipeline/PipelineStageTrack.vue'
import PipelineMaterialsPanel from '../../components/pipeline/PipelineMaterialsPanel.vue'
import PipelineRelationsPanel from '../../components/pipeline/PipelineRelationsPanel.vue'
import PipelineCreateDialog from '../../components/pipeline/PipelineCreateDialog.vue'
import PipelineEditDialog from '../../components/pipeline/PipelineEditDialog.vue'
import PipelineStageManagerDialog from '../../components/pipeline/PipelineStageManagerDialog.vue'
import PipelineTransitionDialog from '../../components/pipeline/PipelineTransitionDialog.vue'
import PipelineRelationDialog from '../../components/pipeline/PipelineRelationDialog.vue'
import PipelineHistoryDrawer from '../../components/pipeline/PipelineHistoryDrawer.vue'
import { usePipelinesStore } from '../../stores/pipelines'
import { listResumes, getResumeVersions } from '../../api/resume'
import { listJobDescriptions } from '../../api/job'
import { listScheduleEvents } from '../../api/schedule'
import { listMyInterviewPlans } from '../../api/interview'
import type { Resume, ResumeVersion } from '../../types/resume'
import type { JobDescription } from '../../types/job'
import type { UpdatePipelineRequest } from '../../types/pipeline'

const store = usePipelinesStore()
const route = useRoute()
const router = useRouter()

const createOpen = ref(false)
const editOpen = ref(false)
const stageManagerOpen = ref(false)
const transitionOpen = ref(false)
const relationOpen = ref(false)
const historyOpen = ref(false)
const transitionTargetId = ref<number | null>(null)
const creating = ref(false)
const saving = ref(false)
const mutationBusy = ref(false)
const auxLoading = ref(false)

const resumes = ref<Resume[]>([])
const jobs = ref<JobDescription[]>([])
const versionsByResume = ref<Record<number, ResumeVersion[]>>({})
const scheduleEvents = ref<Array<{ id: number; title: string }>>([])
const interviewPlans = ref<Array<{ id: number; jobLabel: string }>>([])

const visiblePipelines = computed(() => {
  const actives = store.pipelines.filter((p) => p.lifecycle === 'ACTIVE' || p.lifecycle === 'PAUSED')
  const others = store.pipelines.filter((p) => p.lifecycle === 'ARCHIVED' || p.lifecycle === 'CLOSED')
  return [...actives, ...others]
})

async function loadAux() {
  auxLoading.value = true
  try {
    const [res, jd, sched, plans] = await Promise.allSettled([
      listResumes(), listJobDescriptions(), listScheduleEvents(), listMyInterviewPlans(),
    ])
    if (res.status === 'fulfilled') {
      resumes.value = res.value.data
      const entries = await Promise.all(res.value.data.map(async (r) => {
        try { return [r.id, (await getResumeVersions(r.id)).data] as const } catch { return [r.id, []] as const }
      }))
      versionsByResume.value = Object.fromEntries(entries)
    }
    if (jd.status === 'fulfilled') jobs.value = jd.value.data
    if (sched.status === 'fulfilled') scheduleEvents.value = sched.value.data.map((e: { id: number; title?: string; eventType?: string }) => ({ id: e.id, title: e.title || e.eventType || '日程事项' }))
    if (plans.status === 'fulfilled') interviewPlans.value = plans.value.data.map((p) => ({ id: p.planId, jobLabel: p.title || '面试计划' }))
  } finally {
    auxLoading.value = false
  }
}

function handleSelect(id: number) {
  store.select(id)
  router.replace({ query: { pipelineId: String(id) } })
}

async function handleCreate(req: { name: string; companyName: string; roleTitle: string; jobDescriptionId: number | null; resumeVersionId: number | null; stages: string[] }) {
  creating.value = true
  try {
    await store.create(req)
    createOpen.value = false
    const created = store.pipelines[store.pipelines.length - 1]
    if (created) router.replace({ query: { pipelineId: String(created.id) } })
  } finally { creating.value = false }
}

async function handleUpdate(req: UpdatePipelineRequest) {
  const id = store.selectedPipelineId
  if (!id) return
  saving.value = true
  try {
    await store.update(id, req)
    editOpen.value = false
  } finally { saving.value = false }
}

async function archiveCurrent() {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try { await store.archive(id) } finally { mutationBusy.value = false }
}

async function restoreCurrent() {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try { await store.restore(id) } finally { mutationBusy.value = false }
}

function openTransition(stageId?: number) {
  transitionTargetId.value = stageId ?? null
  transitionOpen.value = true
}

async function handleTransition(note: string | null) {
  const id = store.selectedPipelineId
  const targetId = transitionTargetId.value
  if (!id || !targetId) return
  mutationBusy.value = true
  try {
    // 只调用一次：目标就是点击的 PENDING 阶段
    await store.transitionStage(id, { targetStageId: targetId, note })
    transitionOpen.value = false
  } finally { mutationBusy.value = false }
}

async function handleAddStage(name: string) {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try { await store.addStage(id, { name }) } finally { mutationBusy.value = false }
}

async function handleRenameStage(stageId: number, name: string) {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try { await store.renameStage(id, stageId, { name }) } finally { mutationBusy.value = false }
}

async function handleMoveStage(stageId: number, direction: 'up' | 'down') {
  const id = store.selectedPipelineId
  if (!id) return
  const stages = store.selectedPipeline!.stages
  const index = stages.findIndex((s) => s.id === stageId)
  if (index < 0) return
  const swap = direction === 'up' ? index - 1 : index + 1
  if (swap < 0 || swap >= stages.length) return
  const ids = stages.map((s) => s.id)
  ;[ids[index], ids[swap]] = [ids[swap], ids[index]]
  mutationBusy.value = true
  try { await store.reorderStages(id, { stageIds: ids }) } finally { mutationBusy.value = false }
}

async function handleToggleSchedule(eventId: number, linked: boolean) {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try {
    if (linked) await store.unlinkScheduleEvent(id, eventId)
    else await store.linkScheduleEvent(id, eventId)
  } finally { mutationBusy.value = false }
}

async function handleToggleInterview(planId: number, linked: boolean) {
  const id = store.selectedPipelineId
  if (!id) return
  mutationBusy.value = true
  try {
    if (linked) await store.unlinkInterviewPlan(id, planId)
    else await store.linkInterviewPlan(id, planId)
  } finally { mutationBusy.value = false }
}

function loadHistory() {
  const id = store.selectedPipelineId
  if (!id) return
  // store 已写入 historyErrorMessage；调用层吞掉 reject 避免未处理 Promise
  void store.loadTransitionHistory(id).catch(() => undefined)
}

onMounted(async () => {
  await store.load()
  await loadAux()
  const queryId = Number(route.query.pipelineId)
  if (Number.isSafeInteger(queryId) && queryId > 0) store.select(queryId)
})

watch(() => store.selectedPipelineId, (id) => {
  if (id && route.query.pipelineId !== String(id)) {
    router.replace({ query: { pipelineId: String(id) } })
  }
})
</script>
