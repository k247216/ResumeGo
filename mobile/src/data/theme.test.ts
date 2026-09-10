import { beforeEach, describe, expect, it } from 'vitest'
import { getTheme, setTheme } from './theme'

describe('移动端主题', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.dataset.theme = ''
  })

  it('支持薄荷绿主题并持久化选择', () => {
    setTheme('mint')

    expect(getTheme()).toBe('mint')
    expect(document.documentElement.dataset.theme).toBe('mint')
  })
})
