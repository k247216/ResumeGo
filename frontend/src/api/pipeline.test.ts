// @vitest-environment happy-dom

import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  addPipelineStage,
  archivePipeline,
  createPipeline,
  getPipeline,
  linkInterviewPlan,
  linkScheduleEvent,
  listPipelines,
  renamePipelineStage,
  reorderPipelineStages,
  restorePipeline,
  transitionPipelineStage,
  unlinkInterviewPlan,
  unlinkScheduleEvent,
} from './pipeline'
import type { CareerPipeline } from '../types/pipeline'

vi.mock('./http', () => {
  const apiFetch = vi.fn()
  return { apiFetch }
})

import { apiFetch } from './http'

const mockedFetch = vi.mocked(apiFetch)

function okResponse(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, data, message: null }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function errResponse(message: string): Response {
  return new Response(JSON.stringify({ success: false, data: null, message }), {
    status: 400,
    headers: { 'Content-Type': 'application/json' },
  })
}

const pipeline: CareerPipeline = {
  id: 7,
  name: '腾讯 Java',
  companyName: '腾讯',
  roleTitle: 'Java 后端',
  jobDescriptionId: null,
  resumeVersionId: null,
  lifecycle: 'ACTIVE',
  outcome: null,
  currentStageId: 11,
  stages: [{ id: 11, name: '准备中', position: 0, state: 'CURRENT' }],
  scheduleEventIds: [],
  interviewPlanIds: [],
  archivedAt: null,
  createdAt: '2026-08-22T14:30:00',
  updatedAt: '2026-08-22T14:30:00',
}

describe('pipeline api client', () => {
  beforeEach(() => {
    mockedFetch.mockReset()
  })

  it('FE-01 lists pipelines with a typed GET', async () => {
    mockedFetch.mockResolvedValue(okResponse([pipeline]))
    const result = await listPipelines()
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/pipelines')
    expect(result.data[0].name).toBe('腾讯 Java')
  })

  it('FE-01 gets a pipeline by id', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    const result = await getPipeline(7)
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/pipelines/7')
    expect(result.data.id).toBe(7)
  })

  it('FE-02 creates a pipeline with accurate JSON body', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await createPipeline({ name: '腾讯 Java', companyName: '腾讯', roleTitle: 'Java 后端', stages: ['准备中', '技术面'] })
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({
      name: '腾讯 Java', companyName: '腾讯', roleTitle: 'Java 后端',
      jobDescriptionId: null, resumeVersionId: null, stages: ['准备中', '技术面'],
    })
  })

  it('FE-02 adds a stage with POST to the stages path', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await addPipelineStage(7, { name: 'HR 面' })
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(init?.method).toBe('POST')
    expect(url).toBe('/api/v2/pipelines/7/stages')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ name: 'HR 面' })
  })

  it('FE-02 renames a stage with PATCH', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await renamePipelineStage(7, 12, { name: '技术一面' })
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/stages/12')
    expect(init?.method).toBe('PATCH')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ name: '技术一面' })
  })

  it('FE-02 reorders stages with PUT to the order path', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await reorderPipelineStages(7, { stageIds: [12, 11] })
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/stages/order')
    expect(init?.method).toBe('PUT')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ stageIds: [12, 11] })
  })

  it('FE-02 transitions a stage with POST including optional note', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await transitionPipelineStage(7, { targetStageId: 12, note: '进入技术面' })
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/transitions')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ targetStageId: 12, note: '进入技术面' })
  })

  it('FE-03 archives with POST and no fabricated body', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await archivePipeline(7)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/archive')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBeUndefined()
  })

  it('FE-03 restores with POST and no fabricated body', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await restorePipeline(7)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/restore')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBeUndefined()
  })

  it('FE-04 links a schedule event with PUT', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await linkScheduleEvent(7, 100)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/schedule-events/100')
    expect(init?.method).toBe('PUT')
  })

  it('FE-04 unlinks a schedule event with DELETE on the same path', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await unlinkScheduleEvent(7, 100)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/schedule-events/100')
    expect(init?.method).toBe('DELETE')
  })

  it('FE-04 links an interview plan with PUT', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await linkInterviewPlan(7, 300)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/interview-plans/300')
    expect(init?.method).toBe('PUT')
  })

  it('FE-04 unlinks an interview plan with DELETE on the same path', async () => {
    mockedFetch.mockResolvedValue(okResponse(pipeline))
    await unlinkInterviewPlan(7, 300)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/pipelines/7/interview-plans/300')
    expect(init?.method).toBe('DELETE')
  })

  it('FE-05 prefers the server message on failure', async () => {
    mockedFetch.mockResolvedValue(errResponse('求职管线不存在'))
    await expect(listPipelines()).rejects.toThrow('求职管线不存在')
  })

  it('FE-06 falls back to a Chinese message when the body cannot be parsed', async () => {
    mockedFetch.mockResolvedValue(new Response('not json', { status: 500 }))
    await expect(getPipeline(7)).rejects.toThrow('获取求职管线失败')
  })
})
