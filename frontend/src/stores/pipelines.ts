import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addPipelineStage,
  archivePipeline,
  createPipeline,
  linkInterviewPlan,
  linkScheduleEvent,
  listPipelines,
  renamePipelineStage,
  reorderPipelineStages,
  restorePipeline,
  transitionPipelineStage,
  unlinkInterviewPlan,
  unlinkScheduleEvent,
} from '../api/pipeline'
import type {
  AddPipelineStageRequest,
  CareerPipeline,
  CreatePipelineRequest,
  RenamePipelineStageRequest,
  ReorderPipelineStagesRequest,
  TransitionPipelineStageRequest,
} from '../types/pipeline'

const selectedStorageKey = 'resumego:v2:selectedPipelineId'

function readPersistedId(): number | null {
  const raw = localStorage.getItem(selectedStorageKey)
  if (raw == null) return null
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

export const usePipelinesStore = defineStore('pipelines', () => {
  const pipelines = ref<CareerPipeline[]>([])
  const selectedPipelineId = ref<number | null>(readPersistedId())
  const loading = ref(false)
  const errorMessage = ref('')

  const selectedPipeline = computed(() => (
    pipelines.value.find((pipeline) => pipeline.id === selectedPipelineId.value) ?? null
  ))

  function persistSelection(id: number | null) {
    if (id == null) {
      localStorage.removeItem(selectedStorageKey)
    } else {
      localStorage.setItem(selectedStorageKey, String(id))
    }
  }

  function chooseSelection() {
    const persisted = pipelines.value.find((pipeline) => pipeline.id === selectedPipelineId.value)
    if (persisted) {
      selectedPipelineId.value = persisted.id
      persistSelection(persisted.id)
      return
    }
    // invalid persisted id was dropped by load; pick first ACTIVE, else first
    const fallback = pipelines.value.find((pipeline) => pipeline.lifecycle === 'ACTIVE')
      ?? pipelines.value[0]
      ?? null
    selectedPipelineId.value = fallback?.id ?? null
    persistSelection(fallback?.id ?? null)
  }

  function replacePipeline(updated: CareerPipeline) {
    const index = pipelines.value.findIndex((pipeline) => pipeline.id === updated.id)
    if (index >= 0) {
      pipelines.value[index] = updated
    } else {
      pipelines.value.push(updated)
    }
  }

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
      const response = await listPipelines()
      pipelines.value = response.data
      chooseSelection()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载求职管线失败'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    await load()
  }

  function select(id: number) {
    if (!pipelines.value.some((pipeline) => pipeline.id === id)) return
    selectedPipelineId.value = id
    persistSelection(id)
  }

  async function createImpl(req: CreatePipelineRequest) {
    const response = await createPipeline(req)
    replacePipeline(response.data)
    selectedPipelineId.value = response.data.id
    persistSelection(response.data.id)
  }

  async function addStageImpl(pipelineId: number, req: AddPipelineStageRequest) {
    const response = await addPipelineStage(pipelineId, req)
    replacePipeline(response.data)
  }

  async function renameStageImpl(pipelineId: number, stageId: number, req: RenamePipelineStageRequest) {
    const response = await renamePipelineStage(pipelineId, stageId, req)
    replacePipeline(response.data)
  }

  async function reorderStagesImpl(pipelineId: number, req: ReorderPipelineStagesRequest) {
    const response = await reorderPipelineStages(pipelineId, req)
    replacePipeline(response.data)
  }

  async function transitionStageImpl(pipelineId: number, req: TransitionPipelineStageRequest) {
    const response = await transitionPipelineStage(pipelineId, req)
    replacePipeline(response.data)
  }

  async function archiveImpl(pipelineId: number) {
    const response = await archivePipeline(pipelineId)
    replacePipeline(response.data)
    // 归档后保持当前选择，便于查看与恢复
  }

  async function restoreImpl(pipelineId: number) {
    const response = await restorePipeline(pipelineId)
    replacePipeline(response.data)
  }

  async function linkScheduleEventImpl(pipelineId: number, eventId: number) {
    const response = await linkScheduleEvent(pipelineId, eventId)
    replacePipeline(response.data)
  }

  async function unlinkScheduleEventImpl(pipelineId: number, eventId: number) {
    const response = await unlinkScheduleEvent(pipelineId, eventId)
    replacePipeline(response.data)
  }

  async function linkInterviewPlanImpl(pipelineId: number, planId: number) {
    const response = await linkInterviewPlan(pipelineId, planId)
    replacePipeline(response.data)
  }

  async function unlinkInterviewPlanImpl(pipelineId: number, planId: number) {
    const response = await unlinkInterviewPlan(pipelineId, planId)
    replacePipeline(response.data)
  }

  function guardMutation<T>(action: () => Promise<T>): Promise<T> {
    return action().catch((error: unknown) => {
      errorMessage.value = error instanceof Error ? error.message : '操作失败'
      throw error
    })
  }

  return {
    pipelines,
    selectedPipelineId,
    selectedPipeline,
    loading,
    errorMessage,
    load,
    retry,
    select,
    create: (req: CreatePipelineRequest) => guardMutation(() => createImpl(req)),
    addStage: (pipelineId: number, req: AddPipelineStageRequest) => guardMutation(() => addStageImpl(pipelineId, req)),
    renameStage: (pipelineId: number, stageId: number, req: RenamePipelineStageRequest) => guardMutation(() => renameStageImpl(pipelineId, stageId, req)),
    reorderStages: (pipelineId: number, req: ReorderPipelineStagesRequest) => guardMutation(() => reorderStagesImpl(pipelineId, req)),
    transitionStage: (pipelineId: number, req: TransitionPipelineStageRequest) => guardMutation(() => transitionStageImpl(pipelineId, req)),
    archive: (pipelineId: number) => guardMutation(() => archiveImpl(pipelineId)),
    restore: (pipelineId: number) => guardMutation(() => restoreImpl(pipelineId)),
    linkScheduleEvent: (pipelineId: number, eventId: number) => guardMutation(() => linkScheduleEventImpl(pipelineId, eventId)),
    unlinkScheduleEvent: (pipelineId: number, eventId: number) => guardMutation(() => unlinkScheduleEventImpl(pipelineId, eventId)),
    linkInterviewPlan: (pipelineId: number, planId: number) => guardMutation(() => linkInterviewPlanImpl(pipelineId, planId)),
    unlinkInterviewPlan: (pipelineId: number, planId: number) => guardMutation(() => unlinkInterviewPlanImpl(pipelineId, planId)),
  }
})
