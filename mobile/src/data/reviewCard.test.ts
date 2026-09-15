import { describe, expect, it } from 'vitest'
import { wrapByMeasure } from './reviewCard'

// 假测量：每个 CJK 字符宽 30，ASCII 宽 15——和 Canvas 里真实字号的相对比例一致。
const measure = (s: string) => [...s].reduce((w, ch) => w + (/[\u2E80-\u9FFF\uFF00-\uFFEF]/.test(ch) ? 30 : 15), 0)

describe('分享长图折行', () => {
  it('超宽段落按测量宽度折行，不截断任何字符', () => {
    const text = '面试官先让我讲一个最有成就感的项目，我按 STAR 讲了订单系统的重构，但时间超了。'
    const lines = wrapByMeasure(text, measure, 600)
    const joined = lines.join('')
    expect(joined).toBe(text)
    for (const line of lines) expect(measure(line)).toBeLessThanOrEqual(600)
  })

  it('短文本只占一行', () => {
    expect(wrapByMeasure('答得不错', measure, 600)).toEqual(['答得不错'])
  })

  it('多段落各自折行，空段落保留为空行', () => {
    const lines = wrapByMeasure('第一段。\n\n第二段开始。', measure, 600)
    expect(lines).toEqual(['第一段。', '', '第二段开始。'])
  })

  it('单字超宽也不会死循环（折行至少推进一个字符）', () => {
    const lines = wrapByMeasure('aaaa', measure, 10)
    expect(lines).toEqual(['a', 'a', 'a', 'a'])
  })
})
