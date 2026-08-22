import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePipelinesStore } from './pipelines'
import type { CareerPipeline } from '../types/pipeline'

vi.mock('../api/pipeline', () => {
  const api = {
    listPipelines: vi.fn(),
    createPipeline: vi.fn(),
    addPipelineStage: vi.fn(),
    renamePipelineStage: vi.fn(),
    reorderPipelineStages: vi.fn(),
    transitionPipelineStage: vi.fn(),
    archivePipeline: vi.fn(),
    restorePipeline: vi.fn(),
    linkScheduleEvent: vi.fn(),
    unlinkScheduleEvent: vi.fn(),
    linkInterviewPlan: vi.fn(),
    updatePipeline: vi.fn(),
    listPipelineTransitions: vi.fn(),
    unlinkInterviewPlan: vi.fn(),
  }
  return api
})

import * as pipelineApi from '../api/pipeline'

const api = vi.mocked(pipelineApi)

function p(id: number, lifecycle: CareerPipeline['lifecycle'] = 'ACTIVE'): CareerPipeline {
  return {
    id, name: '管线 ' + id, companyName: '公司', roleTitle: '岗位',
    jobDescriptionId: null, resumeVersionId: null, lifecycle, outcome: null,
    currentStageId: null, stages: [], scheduleEventIds: [], interviewPlanIds: [],
    archivedAt: null, createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function ok<T>(data: T): { success: true; data: T; message: null } {
  return { success: true, data, message: null }
}

describe('usePipelinesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.removeItem('resumego:v2:selectedPipelineId')
  })

  it('STORE-01 loads in API order and selects first ACTIVE', async () => {
    api.listPipelines.mockResolvedValue(ok([p(2, 'PAUSED'), p(3), p(1)]))
    const store = usePipelinesStore()
    await store.load()
    expect(store.pipelines.map((x) => x.id)).toEqual([2, 3, 1])
    expect(store.selectedPipelineId).toBe(3)
  })

  it('STORE-02 restores a valid persisted selection and clears an invalid one', async () => {
    localStorage.setItem('resumego:v2:selectedPipelineId', '3')
    api.listPipelines.mockResolvedValue(ok([p(3), p(1)]))
    const store = usePipelinesStore()
    await store.load()
    expect(store.selectedPipelineId).toBe(3)

    localStorage.setItem('resumego:v2:selectedPipelineId', '99')
    api.listPipelines.mockResolvedValue(ok([p(3), p(1)]))
    await store.load()
    expect(store.selectedPipelineId).not.toBe(99)
    expect(localStorage.getItem('resumego:v2:selectedPipelineId')).not.toBe('99')
  })

  it('STORE-03 empty list leaves selection null', async () => {
    api.listPipelines.mockResolvedValue(ok([]))
    const store = usePipelinesStore()
    await store.load()
    expect(store.pipelines).toEqual([])
    expect(store.selectedPipelineId).toBeNull()
  })

  it('STORE-04 load failure keeps old data and retry clears the error', async () => {
    api.listPipelines.mockResolvedValueOnce(ok([p(3)]))
    const store = usePipelinesStore()
    await store.load()
    expect(store.pipelines).toHaveLength(1)

    api.listPipelines.mockRejectedValueOnce(new Error('网络错误'))
    await expect(store.load()).rejects.toThrow('网络错误')
    expect(store.pipelines).toHaveLength(1)
    expect(store.errorMessage).toContain('网络错误')

    api.listPipelines.mockResolvedValueOnce(ok([p(3), p(1)]))
    await store.retry()
    expect(store.errorMessage).toBe('')
    expect(store.pipelines).toHaveLength(2)
  })

  it('STORE-05 create and mutations replace the item with the API-returned pipeline', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1)]))
    const store = usePipelinesStore()
    await store.load()

    const created = p(5)
    api.createPipeline.mockResolvedValue(ok(created))
    await store.create({ name: '新管线', companyName: '公司', roleTitle: '岗位' })
    expect(store.pipelines.find((x) => x.id === 5)).toEqual(created)

    const renamed = p(1)
    api.renamePipelineStage.mockResolvedValue(ok(renamed))
    await store.renameStage(1, 11, { name: '新阶段' })
    expect(store.pipelines.find((x) => x.id === 1)).toEqual(renamed)
  })

  it('STORE-06 mutation failure keeps data and selection and rethrows', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1)]))
    const store = usePipelinesStore()
    await store.load()
    const before = store.pipelines
    api.archivePipeline.mockRejectedValue(new Error('归档失败'))
    await expect(store.archive(1)).rejects.toThrow('归档失败')
    expect(store.pipelines).toEqual(before)
    expect(store.selectedPipelineId).toBe(1)
    expect(store.errorMessage).toContain('归档失败')
  })

  it('STORE-06b clears a previous error when a retried mutation succeeds', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1)]))
    const store = usePipelinesStore()
    await store.load()

    api.archivePipeline.mockRejectedValueOnce(new Error('归档失败'))
    await expect(store.archive(1)).rejects.toThrow('归档失败')
    expect(store.errorMessage).toContain('归档失败')

    api.archivePipeline.mockResolvedValueOnce(ok(p(1, 'ARCHIVED')))
    await store.archive(1)
    expect(store.errorMessage).toBe('')
  })

  it('STORE-07 archive keeps the current selection so it can be restored', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1)]))
    const store = usePipelinesStore()
    await store.load()
    const archived = p(1, 'ARCHIVED')
    api.archivePipeline.mockResolvedValue(ok(archived))
    await store.archive(1)
    expect(store.selectedPipelineId).toBe(1)
    api.restorePipeline.mockResolvedValue(ok(p(1)))
    await store.restore(1)
    expect(store.pipelines.find((x) => x.id === 1)?.lifecycle).toBe('ACTIVE')
  })

  it('STORE-08 select only touches the pipeline store, not resume/schedule/interview', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1), p(2)]))
    const store = usePipelinesStore()
    await store.load()
    store.select(2)
    expect(store.selectedPipelineId).toBe(2)
    expect(localStorage.getItem('resumego:v2:selectedPipelineId')).toBe('2')
    // select with unknown id must not change selection
    store.select(99)
    expect(store.selectedPipelineId).toBe(2)
  })

  it('FE-02B update replaces the pipeline and keeps selection; failure does not pollute', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1)]))
    const store = usePipelinesStore()
    await store.load()
    store.select(1)

    const updated = { ...p(1), name: '腾讯 Java 后端', roleTitle: 'Java 后端实习' }
    api.updatePipeline.mockResolvedValue(ok(updated))
    await store.update(1, { name: '腾讯 Java 后端', companyName: '公司', roleTitle: 'Java 后端实习', jobDescriptionId: 20, resumeVersionId: null })
    expect(store.pipelines.find((x) => x.id === 1)?.name).toBe('腾讯 Java 后端')
    expect(store.selectedPipelineId).toBe(1)

    api.updatePipeline.mockRejectedValueOnce(new Error('更新失败'))
    const before = store.pipelines
    await expect(store.update(1, { name: 'x', companyName: 'y', roleTitle: 'z', jobDescriptionId: null, resumeVersionId: null })).rejects.toThrow('更新失败')
    expect(store.pipelines).toEqual(before)
    expect(store.errorMessage).toContain('更新失败')
  })

  it('FE-02B keeps history per pipeline and preserves old history on failure', async () => {
    api.listPipelines.mockResolvedValue(ok([p(1), p(2)]))
    const store = usePipelinesStore()
    await store.load()

    const h1 = [{ id: 1, pipelineId: 1, fromStageId: null, toStageId: 11, actor: 'USER', note: null, occurredAt: '2026-08-22T14:30:00' }]
    const h2 = [{ id: 5, pipelineId: 2, fromStageId: 11, toStageId: 12, actor: 'USER', note: '技术面', occurredAt: '2026-08-22T15:00:00' }]
    api.listPipelineTransitions.mockResolvedValueOnce(ok(h1)).mockResolvedValueOnce(ok(h2))
    await store.loadTransitionHistory(1)
    await store.loadTransitionHistory(2)
    expect(store.transitionHistoryByPipelineId[1]).toEqual(h1)
    expect(store.transitionHistoryByPipelineId[2]).toEqual(h2)

    api.listPipelineTransitions.mockRejectedValueOnce(new Error('历史读取失败'))
    await expect(store.loadTransitionHistory(1)).rejects.toThrow('历史读取失败')
    expect(store.transitionHistoryByPipelineId[1]).toEqual(h1)
    expect(store.historyErrorMessage).toContain('历史读取失败')

    api.listPipelineTransitions.mockResolvedValueOnce(ok([...h1, { id: 2, pipelineId: 1, fromStageId: 11, toStageId: 12, actor: 'USER', note: '进入技术面', occurredAt: '2026-08-22T16:00:00' }]))
    await store.loadTransitionHistory(1)
    expect(store.historyErrorMessage).toBe('')
    expect(store.historyLoadingPipelineId).toBeNull()
  })
})
