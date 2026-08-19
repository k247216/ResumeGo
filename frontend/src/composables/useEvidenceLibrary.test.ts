import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createEvidence, listEvidences } from '../api/evidence'
import { useEvidenceLibrary } from './useEvidenceLibrary'

vi.mock('../api/evidence', () => ({ listEvidences: vi.fn(), createEvidence: vi.fn() }))
const evidence = { id: 1, userId: 1, evidenceType: 'project' as const, title: '项目', actionText: '完成核心开发', skillTags: ['Java'], createdAt: '', updatedAt: '' }

describe('useEvidenceLibrary', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads and preserves evidence when refresh fails', async () => {
    vi.mocked(listEvidences).mockResolvedValueOnce({ success: true, data: [evidence] }).mockRejectedValueOnce(new Error('读取失败'))
    const library = useEvidenceLibrary()
    await library.load(); await library.load()
    expect(library.evidences.value).toEqual([evidence])
    expect(library.errorMessage.value).toBe('读取失败')
  })

  it('appends newly created user evidence', async () => {
    vi.mocked(createEvidence).mockResolvedValue({ success: true, data: evidence })
    const library = useEvidenceLibrary()
    await library.create({ evidenceType: 'project', title: '项目', actionText: '完成核心开发', skillTags: ['Java'] })
    expect(library.evidences.value).toEqual([evidence])
  })
})
