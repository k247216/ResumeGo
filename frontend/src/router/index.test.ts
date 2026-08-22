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

  it('routes /knowledge to the V2 KnowledgeLibraryView with fill meta', () => {
    const resolved = router.resolve({ name: 'knowledge' })
    expect(resolved.path).toBe('/knowledge')
    expect(resolved.matched[0]?.meta?.fill).toBe(true)
    expect(String(resolved.matched[0]?.components?.default ?? '')).toContain('KnowledgeLibraryView')
  })

  it('keeps legacy evidences route untouched and never reuses it for Knowledge', () => {
    const names = router.getRoutes().map((route) => route.name)
    expect(names).toContain('evidences')
    expect(names).toContain('knowledge')
    const resolved = router.resolve({ name: 'knowledge' })
    expect(String(resolved.matched[0]?.components?.default ?? '')).not.toContain('EvidenceLibraryView')
  })
})