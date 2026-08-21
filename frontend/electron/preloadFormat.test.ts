// @vitest-environment node

import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const electronDist = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'electron-dist')

describe('preload module format', () => {
  it('compiles to CommonJS so it works under sandbox:true', () => {
    const preload = readFileSync(path.join(electronDist, 'preload.js'), 'utf-8')
    expect(preload).not.toMatch(/^import /m)
    expect(preload).toMatch(/require\(['"]electron['"]\)/)
    expect(preload).toContain('contextBridge.exposeInMainWorld')
  })

  it('keeps the main process as ESM', () => {
    const main = readFileSync(path.join(electronDist, 'main.js'), 'utf-8')
    expect(main).toMatch(/^import /m)
  })

  it('exposes the desktop bridge under sandbox constraints', () => {
    const preload = readFileSync(path.join(electronDist, 'preload.js'), 'utf-8')
    expect(preload).toContain("exposeInMainWorld('resumeGoDesktop'")
    expect(preload).toContain("'resumego:runtime-config'")
  })
})
