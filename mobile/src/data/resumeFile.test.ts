import { describe, expect, it, vi } from 'vitest'

vi.mock('./fileStore', () => ({
  getFile: vi.fn(async (key: string) => key === 'resume-md' ? { text: async () => '# 张三\n\n- Java / Spring Boot' } : null),
  objectUrl: vi.fn(async () => null),
}))

import { previewResume } from './resumeFile'

describe('简历文件预览', () => {
  it('Markdown 版本读取为文本预览，而不是只显示文件名', async () => {
    const preview = await previewResume({
      id: 1, resumeId: 1, versionNo: 1, fileName: 'resume.md', mime: 'text/markdown',
      size: 28, fileKey: 'resume-md', note: null, createdAt: new Date().toISOString(),
    })
    expect(preview.kind).toBe('text')
    expect(preview.text).toContain('Java / Spring Boot')
  })

  it('文件本体缺失时返回诚实空态', async () => {
    const preview = await previewResume({
      id: 2, resumeId: 1, versionNo: 2, fileName: 'missing.md', mime: 'text/markdown',
      size: 0, fileKey: 'missing', note: null, createdAt: new Date().toISOString(),
    })
    expect(preview.kind).toBe('none')
  })
})
