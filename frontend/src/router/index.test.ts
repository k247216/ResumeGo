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

  it('routes /targets to the V2 PipelineView', () => {
    const resolved = router.resolve({ name: 'targets' })
    expect(resolved.path).toBe('/targets')
    expect(String(resolved.matched[0]?.components?.default ?? '')).toContain('PipelineView')
  })

  it('does not reference legacy targets view or store', () => {
    const source = router.getRoutes().map((r) => r.components)
    const serialized = JSON.stringify(source)
    expect(serialized).not.toContain('TargetListView')
    expect(serialized).not.toContain('useTargetsStore')
  })
})