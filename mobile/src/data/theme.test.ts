import { beforeEach, describe, expect, it } from 'vitest'
import { getTheme, setTheme, THEME_OPTIONS } from './theme'

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

  it('保留浅色、薄荷绿、深色三种主题选项', () => {
    expect(THEME_OPTIONS.map((item) => item.value)).toEqual(['light', 'mint', 'dark'])
  })
})
