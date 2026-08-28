import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Resume } from '../types/resume'
import { useResumeLibrary } from './useResumeLibrary'
import { archiveResume, forkResumeVersion, listResumes, renameResume, restoreResume } from '../api/resume'
import { resumeFavoriteStorageKey } from '../utils/resumeFavorite'

vi.mock('../api/resume', () => ({
  listResumes: vi.fn(),
  forkResumeVersion: vi.fn(),
  archiveResume: vi.fn(),
  restoreResume: vi.fn(),
  renameResume: vi.fn(),
  deleteResume: vi.fn(),
  getResumeVersions: vi.fn().mockResolvedValue({ success: true, data: [] }),
}))

const resume = (id: number, overrides: Partial<Resume> = {}): Resume => ({
  id,
  title: `简历 ${id}`,
  kind: 'GENERAL',
  forkedFromVersionId: null,
  archivedAt: null,
  targetJobDescriptionId: null,
  currentVersion: null,
  createdAt: '2026-08-25T10:00:00',
  updatedAt: '2026-08-25T10:00:00',
  ...overrides,
})

const generalV2 = resume(1, {
  title: '通用简历',
  currentVersion: { id: 10, resumeId: 1, parentVersionId: 9, versionNo: 2, content: {}, createdByType: 'user', createdAt: '2026-08-25T10:00:00' },
})
const expression = resume(2, { title: '腾讯岗位表达', kind: 'JOB_EXPRESSION', forkedFromVersionId: 10 })

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

/** 模拟服务端：列表数据可变，load() 每次读取当前列表 */
function mockServerList(initial: Resume[]) {
  let current = initial
  const api = {
    getList: () => current,
    setList: (next: Resume[]) => { current = next },
  }
  vi.mocked(listResumes).mockImplementation(async () => ({ success: true, data: api.getList() }))
  return api
}

describe('useResumeLibrary', () => {
  it('加载资产并默认选中第一个；再次加载保持已选中的资产', async () => {
    mockServerList([generalV2, expression])
    const lib = useResumeLibrary()
    await lib.load()
    expect(lib.items.value.map((item) => item.id)).toEqual([1, 2])
    expect(lib.selectedResumeId.value).toBe(1)

    lib.select(2)
    await lib.load()
    expect(lib.selectedResumeId.value).toBe(2)
  })

  it('按 kind 过滤调用真实 API 参数；archived 视图请求归档资产', async () => {
    mockServerList([generalV2, expression])
    const lib = useResumeLibrary()
    lib.filter.value.kind = 'expression'
    await lib.load()
    expect(vi.mocked(listResumes)).toHaveBeenLastCalledWith('JOB_EXPRESSION', false)

    lib.filter.value.kind = 'archived'
    await lib.load()
    expect(vi.mocked(listResumes)).toHaveBeenLastCalledWith(undefined, true)

    lib.filter.value.kind = 'all'
    await lib.load()
    expect(vi.mocked(listResumes)).toHaveBeenLastCalledWith(undefined, false)
  })

  it('关键词过滤标题，不产生额外请求', async () => {
    mockServerList([generalV2, expression])
    const lib = useResumeLibrary()
    await lib.load()
    lib.filter.value.keyword = '腾讯'
    expect(lib.visibleItems.value.map((item) => item.id)).toEqual([2])
    expect(vi.mocked(listResumes)).toHaveBeenCalledTimes(1)
  })

  it('收藏筛选只展示本地标记的资产，并保持服务端列表契约', async () => {
    mockServerList([generalV2, expression])
    localStorage.setItem(resumeFavoriteStorageKey(generalV2.id), 'true')
    const lib = useResumeLibrary()
    lib.filter.value.kind = 'favorites'
    await lib.load()

    expect(vi.mocked(listResumes)).toHaveBeenLastCalledWith(undefined, false)
    expect(lib.visibleItems.value.map((item) => item.id)).toEqual([generalV2.id])
    expect(lib.selectedResumeId.value).toBe(generalV2.id)
  })

  it('fork 后刷新并选中新资产', async () => {
    const server = mockServerList([generalV2, expression])
    const forked = resume(3, { title: '新副本', kind: 'JOB_EXPRESSION', forkedFromVersionId: 10 })
    vi.mocked(forkResumeVersion).mockImplementation(async () => {
      server.setList([generalV2, expression, forked])
      return { success: true, data: forked }
    })
    const lib = useResumeLibrary()
    await lib.load()

    const result = await lib.fork(10, '新副本')

    expect(vi.mocked(forkResumeVersion)).toHaveBeenCalledWith(10, '新副本')
    expect(result.id).toBe(3)
    expect(lib.selectedResumeId.value).toBe(3)
    expect(lib.items.value.some((item) => item.id === 3)).toBe(true)
  })

  it('归档后从当前列表移除，选中回退；恢复后重新出现', async () => {
    const server = mockServerList([generalV2, expression])
    vi.mocked(archiveResume).mockImplementation(async () => {
      server.setList([generalV2])
      return { success: true, data: generalV2 }
    })
    vi.mocked(restoreResume).mockImplementation(async () => {
      server.setList([generalV2, expression])
      return { success: true, data: expression }
    })
    const lib = useResumeLibrary()
    lib.select(2)
    await lib.load()

    await lib.archive(2)
    expect(vi.mocked(archiveResume)).toHaveBeenCalledWith(2)
    expect(lib.items.value.map((item) => item.id)).toEqual([1])
    expect(lib.selectedResumeId.value).toBe(1)

    await lib.restore(2)
    expect(vi.mocked(restoreResume)).toHaveBeenCalledWith(2)
    expect(lib.items.value.map((item) => item.id)).toEqual([1, 2])
  })

  it('加载失败保留原数据并暴露错误，重试成功后清除', async () => {
    const server = mockServerList([generalV2, expression])
    const lib = useResumeLibrary()
    await lib.load()
    expect(lib.error.value).toBe('')

    vi.mocked(listResumes).mockRejectedValue(new Error('后端不可用'))
    await lib.load()
    expect(lib.error.value).toBe('后端不可用')
    expect(lib.items.value.map((item) => item.id)).toEqual([1, 2])

    vi.mocked(listResumes).mockImplementation(async () => ({ success: true, data: [generalV2] }))
    server.setList([generalV2])
    await lib.load()
    expect(lib.error.value).toBe('')
    expect(lib.items.value.map((item) => item.id)).toEqual([1])
  })

  it('改名后刷新列表', async () => {
    const server = mockServerList([generalV2, expression])
    const renamed = resume(1, { title: '改名后' })
    vi.mocked(renameResume).mockImplementation(async () => {
      server.setList([renamed, expression])
      return { success: true, data: renamed }
    })
    const lib = useResumeLibrary()
    await lib.load()

    await lib.rename(1, '改名后')

    expect(vi.mocked(renameResume)).toHaveBeenCalledWith(1, '改名后')
    expect(lib.items.value[0]?.title).toBe('改名后')
  })
})
