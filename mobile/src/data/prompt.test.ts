import { describe, expect, it } from 'vitest'
import { appendPromptLine } from './prompt'

describe('提示词片段拼接', () => {
  it('空稿时直接落下抬头，不打断用户输入', () => {
    expect(appendPromptLine('', '这轮被问了什么', 500)).toBe('这轮被问了什么：')
    expect(appendPromptLine('   \n ', '哪里没答好', 500)).toBe('哪里没答好：')
  })

  it('已有内容时另起一行，且不会留下多余的空行', () => {
    expect(appendPromptLine('考了 LRU 手写\n\n', '下一轮要补什么', 500)).toBe('考了 LRU 手写\n下一轮要补什么：')
  })

  it('逼近上限时按码点截断，不会切出半个 emoji', () => {
    const cut = appendPromptLine('🧪'.repeat(9), '备注', 12)
    expect([...cut]).toHaveLength(12)
    expect(cut).toBe(`${'🧪'.repeat(9)}\n备注`)
  })
})
