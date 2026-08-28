import { describe, expect, it } from 'vitest'
import { diffResumeContent } from './resumeVersionDiff'
import type { ResumeContent } from '../types/resume'

describe('resumeVersionDiff', () => {
  it('detects added, modified and removed chapters deterministically', () => {
    const prev: ResumeContent = {
      summary: '旧简介',
      projects: [{ title: '项目一' }],
      skills: ['Java'],
    }
    const next: ResumeContent = {
      summary: '新简介',
      projects: [{ title: '项目一', highlights: ['量化结果'] }],
      education: [{ school: '某大学' }],
    }

    const changes = diffResumeContent(prev, next)

    expect(changes).toEqual(expect.arrayContaining([
      expect.objectContaining({ chapterKey: 'summary', chapterLabel: '个人简介', changeType: 'modified' }),
      expect.objectContaining({ chapterKey: 'projects', chapterLabel: '项目经历', changeType: 'modified' }),
      expect.objectContaining({ chapterKey: 'education', chapterLabel: '教育经历', changeType: 'added' }),
      expect.objectContaining({ chapterKey: 'skills', chapterLabel: '技能', changeType: 'removed' }),
    ]))
    expect(changes.find((change) => change.chapterKey === 'summary')?.details).toEqual([
      { value: '新简介', changeType: 'modified' },
    ])
  })

  it('returns empty array when contents are equivalent', () => {
    const prev: ResumeContent = { summary: '一致', skills: ['Java'] }
    const next: ResumeContent = { summary: '一致', skills: ['Java'] }
    expect(diffResumeContent(prev, next)).toEqual([])
  })

  it('treats empty string and missing chapter the same', () => {
    const prev: ResumeContent = { summary: '  ' }
    const next: ResumeContent = {}
    expect(diffResumeContent(prev, next)).toEqual([])
  })

  it('is order-stable: same inputs produce same output order', () => {
    const prev: ResumeContent = { summary: 'a', projects: [] }
    const next: ResumeContent = { summary: 'b', education: [{}] }
    const first = diffResumeContent(prev, next)
    const second = diffResumeContent(prev, next)
    expect(first).toEqual(second)
  })
})
