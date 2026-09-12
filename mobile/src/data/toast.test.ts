import { afterEach, describe, expect, it, vi } from 'vitest'
import { dismissToast, runToastAction, toast, toastAction, toastMessage } from './toast'

afterEach(() => {
  dismissToast()
  vi.useRealTimers()
})

describe('可撤销的提示条', () => {
  it('带操作的提示活得比纯文案久，撤销按钮才有来得及点', () => {
    vi.useFakeTimers()
    toast('已推进到「面试」', { label: '撤销', run: () => {} })
    vi.advanceTimersByTime(3000)
    expect(toastMessage.value).toBe('已推进到「面试」')
    vi.advanceTimersByTime(2500)
    expect(toastMessage.value).toBe('')
    expect(toastAction.value).toBeNull()
  })

  it('点了撤销先收起提示再执行，不会留下一个还能再点一次的按钮', () => {
    const run = vi.fn()
    toast('已归档', { label: '撤销', run })
    runToastAction()
    expect(run).toHaveBeenCalledTimes(1)
    expect(toastMessage.value).toBe('')
    expect(toastAction.value).toBeNull()
    runToastAction()
    expect(run).toHaveBeenCalledTimes(1)
  })

  it('后续提示覆盖掉前一条的撤销入口，避免撤销到别处的改动上', () => {
    toast('已归档', { label: '撤销', run: () => {} })
    toast('已删除')
    expect(toastAction.value).toBeNull()
  })
})
