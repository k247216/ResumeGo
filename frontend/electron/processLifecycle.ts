import type { ChildProcess } from 'node:child_process'

export async function terminateChildProcess(child: ChildProcess, graceMs = 3_000): Promise<void> {
  if (child.exitCode !== null || child.signalCode !== null) return
  await new Promise<void>((resolve) => {
    let settled = false
    let forceTimer: NodeJS.Timeout | undefined
    const finish = () => {
      if (settled) return
      settled = true
      if (forceTimer) clearTimeout(forceTimer)
      resolve()
    }
    child.once('exit', finish)
    child.kill('SIGTERM')
    forceTimer = setTimeout(() => {
      if (child.exitCode === null && child.signalCode === null) child.kill('SIGKILL')
      finish()
    }, graceMs)
  })
}

export interface QuitEvent {
  preventDefault: () => void
}

export function createBeforeQuitHandler(stop: () => Promise<void>, quit: () => void) {
  let cleanupStarted = false
  let cleanupComplete = false
  return (event: QuitEvent): void => {
    if (cleanupComplete) return
    event.preventDefault()
    if (cleanupStarted) return
    cleanupStarted = true
    void stop().finally(() => {
      cleanupComplete = true
      quit()
    })
  }
}
