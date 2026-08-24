import { describe, expect, it, vi } from 'vitest'
import { getProject, updateProjectLinks } from '../api/project'
import { linkResumeVersionToTarget } from './targetMaterials'

vi.mock('../api/project', () => ({ getProject: vi.fn(), updateProjectLinks: vi.fn() }))

describe('linkResumeVersionToTarget', () => {
  it('preserves the target job while linking a newly created resume version', async () => {
    vi.mocked(getProject).mockResolvedValue({ success: true, data: { id: 4, name: '目标', status: 'active', stage: 'applied', stageUpdatedAt: null, industry: null, targetRole: null, location: null, notes: null, jobDescriptionId: 9, resumeVersionId: null, archivedAt: null, createdAt: '', updatedAt: '' } })
    vi.mocked(updateProjectLinks).mockResolvedValue({ success: true, data: { id: 4, name: '目标', status: 'active', stage: 'applied', stageUpdatedAt: null, industry: null, targetRole: null, location: null, notes: null, jobDescriptionId: 9, resumeVersionId: 12, archivedAt: null, createdAt: '', updatedAt: '' } })

    await linkResumeVersionToTarget(4, 12)

    expect(updateProjectLinks).toHaveBeenCalledWith(4, { jobDescriptionId: 9, resumeVersionId: 12 })
  })
})
