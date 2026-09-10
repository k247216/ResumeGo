import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getTheme, setTheme, THEME_OPTIONS } from './theme'

describe('移动端主题', () => {
  beforeEach(() => {
    try { localStorage.clear() } catch { /* 无 localStorage 环境：theme 走内存回退 */ }
    document.documentElement.dataset.theme = ''
  })
  afterEach(() => vi.unstubAllGlobals())

  it('未选择主题时默认纸张浅色（白色）', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => {},
      removeItem: () => {},
      clear: () => {},
    } as unknown as Storage)
    expect(getTheme()).toBe('light')
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
