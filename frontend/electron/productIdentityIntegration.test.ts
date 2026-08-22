import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

describe('V2 preview desktop startup', () => {
  it('sets an isolated userData path before application startup', () => {
    const main = readFileSync(path.resolve('electron-dist/main.js'), 'utf8')
    const setPath = main.indexOf("setPath('userData'")
    const start = main.lastIndexOf('startApplication()')
    expect(setPath).toBeGreaterThan(-1)
    expect(start).toBeGreaterThan(setPath)
    expect(main).toContain('resolveV2PreviewUserDataPath')
  })
})
