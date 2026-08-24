import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  archiveProject,
  createProject,
  deleteProject,
  listProjects,
  renameProject,
  restoreProject,
  updateProjectApplication,
  updateProjectLinks,
  updateProjectStage,
} from '../api/project'
import type {
  CreateJobProjectRequest,
  JobProject,
  TargetStage,
  UpdateJobProjectApplicationRequest,
  UpdateJobProjectLinksRequest,
} from '../types/project'

const activeTargetStorageKey = 'resumego:activeTargetId'

export const useTargetsStore = defineStore('targets', () => {
  const targets = ref<JobProject[]>([])
  const activeTargetId = ref<number | null>(readPersistedTargetId())
  const loading = ref(false)
  const errorMessage = ref('')

  const activeTarget = computed(() => (
    targets.value.find((target) => target.id === activeTargetId.value) ?? null
  ))

  function chooseValidTarget() {
    const selected = targets.value.find((target) => target.id === activeTargetId.value)
    if (selected?.status === 'active') return
    const fallback = targets.value.find((target) => target.status === 'active') ?? selected ?? targets.value[0] ?? null
    setActiveTargetId(fallback?.id ?? null)
  }

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
      const response = await listProjects()
      targets.value = response.data
      chooseValidTarget()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '获取求职目标失败'
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    await load()
  }

  function select(id: number) {
    if (!targets.value.some((target) => target.id === id)) return
    setActiveTargetId(id)
  }

  async function create(payload: CreateJobProjectRequest) {
    errorMessage.value = ''
    try {
      const response = await createProject(payload)
      targets.value = [response.data, ...targets.value.filter((target) => target.id !== response.data.id)]
      setActiveTargetId(response.data.id)
      return response.data
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '创建求职目标失败'
      throw error
    }
  }

  async function updateLinks(id: number, payload: UpdateJobProjectLinksRequest) {
    errorMessage.value = ''
    try {
      const response = await updateProjectLinks(id, payload)
      targets.value = targets.value.map((target) => target.id === id ? response.data : target)
      return response.data
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '更新求职目标材料失败'
      throw error
    }
  }

  async function rename(id: number, name: string) {
    return mutateTarget(id, () => renameProject(id, name), '重命名求职目标失败')
  }

  async function setStage(id: number, stage: TargetStage) {
    return mutateTarget(id, () => updateProjectStage(id, { stage }), '更新求职阶段失败')
  }

  async function saveApplication(id: number, payload: UpdateJobProjectApplicationRequest) {
    return mutateTarget(id, () => updateProjectApplication(id, payload), '保存投递信息失败')
  }

  async function archive(id: number) {
    const target = await mutateTarget(id, () => archiveProject(id), '归档求职目标失败')
    chooseValidTarget()
    return target
  }

  async function restore(id: number) {
    return mutateTarget(id, () => restoreProject(id), '恢复求职目标失败')
  }

  async function remove(id: number) {
    errorMessage.value = ''
    try {
      await deleteProject(id)
      targets.value = targets.value.filter((target) => target.id !== id)
      chooseValidTarget()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '删除求职目标失败'
      throw error
    }
  }

  async function mutateTarget(id: number, request: () => Promise<{ data: JobProject }>, fallback: string) {
    errorMessage.value = ''
    try {
      const response = await request()
      targets.value = targets.value.map((target) => target.id === id ? response.data : target)
      return response.data
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : fallback
      throw error
    }
  }

  function setActiveTargetId(id: number | null) {
    activeTargetId.value = id
    if (id === null) {
      localStorage.removeItem(activeTargetStorageKey)
    } else {
      localStorage.setItem(activeTargetStorageKey, String(id))
    }
  }

  return {
    targets,
    activeTargetId,
    activeTarget,
    loading,
    errorMessage,
    load,
    retry,
    select,
    create,
    updateLinks,
    rename,
    setStage,
    saveApplication,
    archive,
    restore,
    remove,
  }
})

function readPersistedTargetId(): number | null {
  const id = Number(localStorage.getItem(activeTargetStorageKey))
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
