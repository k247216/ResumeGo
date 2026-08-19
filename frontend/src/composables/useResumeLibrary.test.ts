import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getResumeVersions, listResumes } from '../api/resume'
import { useResumeLibrary } from './useResumeLibrary'

vi.mock('../api/resume', () => ({ listResumes: vi.fn(), getResumeVersions: vi.fn() }))

const version = { id: 9, resumeId: 3, versionNo: 2, content: { basicInfo: { name: '小林' } }, createdByType: 'user', createdAt: '2026-08-19' }
const resume = { id: 3, title: '后端简历', currentVersion: version }

describe('useResumeLibrary', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads resumes and selects the first current version', async () => {
    vi.mocked(listResumes).mockResolvedValue({ success: true, data: [resume] })
    vi.mocked(getResumeVersions).mockResolvedValue({ success: true, data: [version] })
    const library = useResumeLibrary()
    await library.load()
    expect(library.selectedResume.value?.id).toBe(3)
    expect(library.selectedVersion.value?.id).toBe(9)
  })

  it('preserves loaded resumes when a refresh fails', async () => {
    vi.mocked(listResumes).mockResolvedValueOnce({ success: true, data: [resume] }).mockRejectedValueOnce(new Error('本地服务不可用'))
    vi.mocked(getResumeVersions).mockResolvedValue({ success: true, data: [version] })
    const library = useResumeLibrary()
    await library.load()
    await library.load()
    expect(library.resumes.value).toHaveLength(1)
    expect(library.errorMessage.value).toBe('本地服务不可用')
  })
})
