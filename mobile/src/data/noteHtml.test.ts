import { describe, expect, it } from 'vitest'
import { ensureReviewHtml, reviewPlainText, sanitizeReviewHtml } from './noteHtml'
import { NOTE_COLORS, normalizeNoteColor } from '../constants/noteColors'

describe('心得 HTML 净化', () => {
  it('保留加粗、链接、图片等白名单标签', () => {
    const html = '<p><strong>重点</strong>与 <a href="https://example.com">链接</a></p><img src="data:image/jpeg;base64,abc" alt="">'
    expect(sanitizeReviewHtml(html)).toBe(html)
  })

  it('剥掉事件属性与 style/class，script 整块丢弃', () => {
    const html = '<p onclick="alert(1)" style="x:1" class="a">文字</p><script>alert(2)</script><img src="x" onerror="alert(3)">'
    const clean = sanitizeReviewHtml(html)
    expect(clean).toContain('文字')
    expect(clean).not.toContain('onclick')
    expect(clean).not.toContain('style')
    expect(clean).not.toContain('class')
    expect(clean).not.toContain('script')
    expect(clean).not.toContain('onerror')
  })

  it('a 的 href 只留 http(s)/mailto，javascript: 链接连同壳一起剥掉', () => {
    expect(sanitizeReviewHtml('<a href="javascript:alert(1)">x</a>')).toBe('x')
    expect(sanitizeReviewHtml('<a href="https://ok.com">x</a>')).toBe('<a href="https://ok.com">x</a>')
  })

  it('img 只留 data:image 与 http(s) 源，其余移除', () => {
    expect(sanitizeReviewHtml('<img src="data:image/png;base64,AA">')).toBe('<img src="data:image/png;base64,AA" alt="">')
    expect(sanitizeReviewHtml('<img src="file:///etc/passwd">')).toBe('')
  })

  it('不认识的标签剥壳留文字，<font> 这类旧包装不丢内容', () => {
    expect(sanitizeReviewHtml('<font size="3">旧内容</font>')).toBe('旧内容')
  })
})

describe('旧纯文本迁移', () => {
  it('无标签的旧心得转成段落，换行变 <br>，特殊字符转义', () => {
    const html = ensureReviewHtml('第一段\n第二段 <img> 陷阱')
    expect(html).toContain('第一段<br>第二段 &lt;img&gt; 陷阱')
    expect(html.startsWith('<p>')).toBe(true)
  })

  it('已有的 HTML 原样净化，不二次转义', () => {
    const html = ensureReviewHtml('<p><strong>已是富文本</strong></p>')
    expect(html).toBe('<p><strong>已是富文本</strong></p>')
    expect(ensureReviewHtml('有换行<br>的旧文')).toContain('<br>')
  })

  it('空内容返回空串', () => {
    expect(ensureReviewHtml(null)).toBe('')
    expect(ensureReviewHtml('   ')).toBe('')
  })
})

describe('纯文本视图', () => {
  it('剥掉所有标签只留文字，用于字数与心得墙摘要', () => {
    expect(reviewPlainText('<p><strong>算法</strong>没答好</p><p>补基础</p>')).toBe('算法没答好补基础')
    expect(reviewPlainText('')).toBe('')
  })
})

describe('便签底色调色板', () => {
  it('只接受调色板内的值', () => {
    for (const c of NOTE_COLORS) expect(normalizeNoteColor(c)).toBe(c)
    expect(normalizeNoteColor('#ff0000')).toBeNull()
    expect(normalizeNoteColor(null)).toBeNull()
    expect(normalizeNoteColor(42)).toBeNull()
    expect(normalizeNoteColor('javascript:alert(1)')).toBeNull()
  })
})
