import { describe, expect, it, vi } from 'vitest'

vi.mock('./fileStore', () => ({
  getFile: vi.fn(async (key: string) => key === 'resume-md' ? { text: async () => '# 张三\n\n- Java / Spring Boot' } : null),
  objectUrl: vi.fn(async (key: string) => key === 'resume-image' ? 'blob:image' : null),
}))

import { headEllipsis, humanSize, isSupportedResume, middleEllipsis, previewResume } from './resumeFile'

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

  it('图片简历可以直接生成图片预览', async () => {
    const preview = await previewResume({
      id: 3, resumeId: 1, versionNo: 3, fileName: 'resume.png', mime: 'image/png',
      size: 12, fileKey: 'resume-image', note: null, createdAt: new Date().toISOString(),
    })
    expect(preview.kind).toBe('image')
    expect(preview.url).toBe('blob:image')
  })

  it('无法渲染的格式落到 unsupported，而不是当 PDF 显示成空白', async () => {
    const preview = await previewResume({
      id: 4, resumeId: 1, versionNo: 4, fileName: '简历.docx', mime: 'application/octet-stream',
      size: 4096, fileKey: 'resume-docx', note: null, createdAt: new Date().toISOString(),
    })
    expect(preview.kind).toBe('unsupported')
  })
})

describe('简历导入格式门槛', () => {
  it('扩展名或 mime 任一命中即放行，docx 一律拒绝', () => {
    expect(isSupportedResume({ name: 'a.pdf', type: '' })).toBe(true)
    expect(isSupportedResume({ name: 'a.md', type: '' })).toBe(true)
    expect(isSupportedResume({ name: 'resume', type: 'application/pdf' })).toBe(true)
    expect(isSupportedResume({ name: 'a.docx', type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })).toBe(false)
    expect(isSupportedResume({ name: 'a.zip', type: 'application/zip' })).toBe(false)
  })
})

describe('文件名展示截断', () => {
  it('不超长时原样保留，不做无谓改写', () => {
    expect(middleEllipsis('张三-后端.pdf', 28)).toBe('张三-后端.pdf')
    expect(middleEllipsis('  张三-后端.pdf  ', 28)).toBe('张三-后端.pdf')
  })

  it('超长时从中间截，前后缀都保住，总长不超过上限', () => {
    const name = `${'求'.repeat(30)}职达简历.pdf`
    const short = middleEllipsis(name, 24)
    expect([...short].length).toBeLessThanOrEqual(24)
    expect(short.startsWith('求求求')).toBe(true)
    expect(short.endsWith('.pdf')).toBe(true)
    expect(short).toContain('…')
  })

  it('没有扩展名时也能截，中文按整字切不出现半个字', () => {
    const cut = middleEllipsis('简'.repeat(40), 20)
    expect([...cut]).toHaveLength(20)
    expect(cut).toBe(`${'简'.repeat(10)}…${'简'.repeat(9)}`)
  })

  it('标题类文本只留开头，不把日期尾巴露出来', () => {
    const title = '简历_张正坤_后端开发工程师_2026届秋招_最终定稿V3_20260910'
    expect(headEllipsis('通用简历', 12)).toBe('通用简历')
    expect(headEllipsis(title, 12)).toBe('简历_张正坤_后端开发…')
    expect([...headEllipsis(title, 12)]).toHaveLength(12)
    expect(headEllipsis(title, 12)).not.toContain('20260910')
  })

  it('体积按 B / KB / MB 三档给整数读法', () => {
    expect(humanSize(900)).toBe('900 B')
    expect(humanSize(2048)).toBe('2 KB')
    expect(humanSize(3.5 * 1024 * 1024)).toBe('3.5 MB')
  })
})
