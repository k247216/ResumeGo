type Close = () => void

// 后开的浮层必须先被返回键吃掉：确认卡 > 选择器 > 详情 Sheet。
const stack: Close[] = []

/** 浮层挂载时注册它的关闭动作，返回反注册函数（组件卸载时调用）。 */
export function registerOverlay(close: Close): () => void {
  stack.push(close)
  syncScrollLock()
  let active = true
  return () => {
    if (!active) return
    active = false
    const i = stack.lastIndexOf(close)
    if (i >= 0) stack.splice(i, 1)
    syncScrollLock()
  }
}

/** 关掉最上层浮层；返回是否已消费这次返回。 */
export function closeTopOverlay(): boolean {
  const close = stack.pop()
  if (!close) return false
  close()
  syncScrollLock()
  return true
}

function syncScrollLock() {
  document.documentElement.classList.toggle('overlay-open', stack.length > 0)
}
