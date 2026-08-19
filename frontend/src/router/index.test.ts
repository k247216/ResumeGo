import { describe, expect, it } from 'vitest'
import router from './index'

describe('product routes', () => {
  it('keeps retired job and scoring pages unreachable', () => {
    const names = router.getRoutes().map((route) => route.name)
    expect(names).not.toContain('jobs')
    expect(names).not.toContain('job-create')
    expect(names).not.toContain('job-detail')
    expect(names).not.toContain('resume-assessment')
  })

  it('uses the focused workbench, target, and editor routes', () => {
    expect(router.resolve({ name: 'workbench' }).path).toBe('/')
    expect(router.resolve({ name: 'targets' }).path).toBe('/targets')
    expect(router.resolve({ name: 'resume-editor' }).path).toBe('/editor')
  })
})
