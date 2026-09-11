import { describe, expect, it } from 'vitest'
import { closeTopOverlay, registerOverlay } from './overlays'

function locked() { return document.documentElement.classList.contains('overlay-open') }

describe('浮层栈', () => {
  it('后开的浮层先被返回键吃掉', () => {
    const closed: string[] = []
    const offSheet = registerOverlay(() => closed.push('sheet'))
    const offPicker = registerOverlay(() => closed.push('picker'))

    expect(closeTopOverlay()).toBe(true)
    expect(closeTopOverlay()).toBe(true)
    expect(closed).toEqual(['picker', 'sheet'])
    expect(closeTopOverlay()).toBe(false)

    offSheet()
    offPicker()
  })

  it('反注册幂等，重复调用不会吃掉别人的槽位', () => {
    const closed: string[] = []
    const off = registerOverlay(() => closed.push('first'))
    const offSecond = registerOverlay(() => closed.push('second'))

    off()
    off()
    expect(closeTopOverlay()).toBe(true)
    expect(closed).toEqual(['second'])
    offSecond()
  })

  it('浮层存续期间锁住背景滚动，全部关完立刻解锁', () => {
    const offA = registerOverlay(() => {})
    const offB = registerOverlay(() => {})
    expect(locked()).toBe(true)

    offA()
    expect(locked()).toBe(true)
    offB()
    expect(locked()).toBe(false)

    const offC = registerOverlay(() => {})
    expect(locked()).toBe(true)
    expect(closeTopOverlay()).toBe(true)
    expect(locked()).toBe(false)
    offC()
  })
})
