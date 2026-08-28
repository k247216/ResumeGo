import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useResumeEditor } from './useResumeEditor'

const api = vi.hoisted(() => ({
  createResume: vi.fn(),
  createResumeVersion: vi.fn(),
  getResumeVersion: vi.fn(),
  getResumeVersions: vi.fn(),
}))

vi.mock('../api/resume', () => api)

describe('useResumeEditor', () => {
  beforeEach(() => vi.clearAllMocks())

  it('starts a blank resume without a target job', async () => {
    const editor = useResumeEditor()
    await editor.load({ mode: 'blank' })
    expect(editor.blank.value).toBe(true)
    expect(editor.sections.value[0]?.title).toBe('个人信息')
  })

  it('creates a blank resume without binding a job', async () => {
    api.createResume.mockResolvedValue({ data: { id: 7, title: '王小明的简历', currentVersion: { id: 9, resumeId: 7, versionNo: 1, content: { basicInfo: { name: '王小明' } }, createdByType: 'user', createdAt: '2026-08-19' } } })
    api.getResumeVersions.mockResolvedValue({ data: [{ id: 9, resumeId: 7, versionNo: 1, content: { basicInfo: { name: '王小明' } }, createdByType: 'user', createdAt: '2026-08-19' }] })
    const editor = useResumeEditor()
    await editor.load({ mode: 'blank' })
    editor.updateField('personal-info', 'basicInfo.name', '王小明')
    await editor.save()
    expect(api.createResume).toHaveBeenCalledWith(expect.objectContaining({ targetJobDescriptionId: null }))
  })

  it('saves edits as an immutable new version', async () => {
    const version = { id: 3, resumeId: 2, versionNo: 1, content: { summary: '旧简介' }, createdByType: 'user', createdAt: '2026-08-19' }
    api.getResumeVersion.mockResolvedValue({ data: version })
    api.getResumeVersions.mockResolvedValue({ data: [version] })
    api.createResumeVersion.mockResolvedValue({ data: { ...version, id: 4, versionNo: 2, content: { summary: '新简介' } } })
    const editor = useResumeEditor()
    await editor.load({ versionId: 3 })
    editor.updateParagraph('summary', 0, '新简介')
    await editor.save()
    expect(api.createResumeVersion).toHaveBeenCalledWith(2, expect.objectContaining({ content: expect.objectContaining({ summary: '新简介' }) }))
  })

  it('passes a user-authored change summary when saving a new version', async () => {
    const version = { id: 3, resumeId: 2, versionNo: 1, content: { summary: '旧简介' }, createdByType: 'user', createdAt: '2026-08-19' }
    api.getResumeVersion.mockResolvedValue({ data: version })
    api.getResumeVersions.mockResolvedValue({ data: [version] })
    api.createResumeVersion.mockResolvedValue({ data: { ...version, id: 4, versionNo: 2, content: { summary: '新简介' }, changeSummary: '补充 Redis 项目量化结果' } })
    const editor = useResumeEditor()
    await editor.load({ versionId: 3 })
    editor.updateParagraph('summary', 0, '新简介')
    await editor.save('补充 Redis 项目量化结果')
    expect(api.createResumeVersion).toHaveBeenCalledWith(2, expect.objectContaining({ changeSummary: '补充 Redis 项目量化结果' }))
  })
})
