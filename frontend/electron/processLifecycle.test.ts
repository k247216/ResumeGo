// @vitest-environment node

import { spawn } from 'node:child_process'
import { describe, expect, it } from 'vitest'
import { createBeforeQuitHandler, terminateChildProcess } from './processLifecycle.js'

describe('terminateChildProcess', () => {
  it('terminates an actual spawned backend-like child', async () => {
    const child = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' })
    await new Promise<void>((resolve, reject) => {
      child.once('spawn', resolve)
      child.once('error', reject)
    })

    await terminateChildProcess(child, 1_000)

    expect(child.exitCode !== null || child.signalCode !== null).toBe(true)
  })

  it('blocks the first quit until cleanup completes and then permits the final quit', async () => {
    let finishCleanup!: () => void
    const cleanup = new Promise<void>((resolve) => { finishCleanup = resolve })
    let stopCalls = 0
    let quitCalls = 0
    let prevented = 0
    const handler = createBeforeQuitHandler(() => {
      stopCalls += 1
      return cleanup
    }, () => { quitCalls += 1 })

    handler({ preventDefault: () => { prevented += 1 } })
    handler({ preventDefault: () => { prevented += 1 } })
    expect(stopCalls).toBe(1)
    expect(prevented).toBe(2)
    expect(quitCalls).toBe(0)

    finishCleanup()
    await cleanup
    await Promise.resolve()
    expect(quitCalls).toBe(1)

    handler({ preventDefault: () => { prevented += 1 } })
    expect(prevented).toBe(2)
  })
})
