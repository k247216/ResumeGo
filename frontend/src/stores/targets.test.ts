import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createProject, listProjects } from '../api/project'
import type { JobProject } from '../types/project'
import { useTargetsStore } from './targets'

vi.mock('../api/project', () => ({
  listProjects: vi.fn(),
  createProject: vi.fn(),
}))

const project = (id: number, status: JobProject['status'] = 'active'): JobProject => ({
  id,
  name: `目标 ${id}`,
  status,
  jobDescriptionId: null,
  resumeVersionId: null,
  archivedAt: status === 'archived' ? '2026-08-19T08:00:00' : null,
  createdAt: '2026-08-19T08:00:00',
  updatedAt: '2026-08-19T08:00:00',
})

describe('useTargetsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('loads targets and restores a valid persisted selection', async () => {
    localStorage.setItem('resumego:activeTargetId', '2')
    vi.mocked(listProjects).mockResolvedValue({
      success: true,
      data: [project(1, 'archived'), project(2), project(3)],
    })

    const store = useTargetsStore()
    await store.load()

    expect(store.activeTarget?.id).toBe(2)
    store.select(3)
    expect(store.activeTarget?.id).toBe(3)
    expect(localStorage.getItem('resumego:activeTargetId')).toBe('3')
    store.select(999)
    expect(store.activeTarget?.id).toBe(3)
  })

  it('selects a newly created target', async () => {
    vi.mocked(createProject).mockResolvedValue({ success: true, data: project(4) })
    const store = useTargetsStore()

    await store.create({ name: '腾讯 · Java 后端实习' })

    expect(store.activeTarget?.id).toBe(4)
    expect(localStorage.getItem('resumego:activeTargetId')).toBe('4')
  })

  it('keeps existing targets visible when refresh fails', async () => {
    vi.mocked(listProjects)
      .mockResolvedValueOnce({ success: true, data: [project(2)] })
      .mockRejectedValueOnce(new Error('本地服务暂时不可用'))
    const store = useTargetsStore()
    await store.load()

    await store.retry()

    expect(store.targets).toHaveLength(1)
    expect(store.errorMessage).toBe('本地服务暂时不可用')
  })
})
