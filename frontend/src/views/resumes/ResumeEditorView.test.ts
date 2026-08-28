// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ResumeEditorView from './ResumeEditorView.vue'

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return {
    ...actual,
    useRoute: () => ({ query: { mode: 'blank' } }),
    useRouter: () => ({ push: vi.fn() }),
    onBeforeRouteLeave: vi.fn(),
  }
})

describe('ResumeEditorView', () => {
  it('renders focused editing controls without retired product features', () => {
    const wrapper = mount(ResumeEditorView, {
      global: {
        stubs: {
          EditorSidebar: { template: '<aside>简历模块</aside>' },
          EditorCanvas: { template: '<main>正文编辑</main>' },
          EditorPreviewPanel: { template: '<section>实时预览</section>' },
        },
      },
    })
    const text = wrapper.text()
    expect(text).toContain('返回工作台')
    expect(text).toContain('撤销')
    expect(text).toContain('导出 PDF')
    expect(text).not.toContain('简历评分')
    expect(text).not.toContain('岗位推荐')
    expect(text).not.toContain('岗位库')
    expect(text).not.toContain('AI 建议')
  })

  it('opens an optional change-summary prompt before saving', async () => {
    const wrapper = mount(ResumeEditorView, {
      global: {
        stubs: {
          EditorSidebar: { template: '<aside>简历模块</aside>' },
          EditorCanvas: { template: '<main><button data-test="emit-save" @click="$emit(\'save-draft\')">保存</button></main>' },
          EditorPreviewPanel: { template: '<section>实时预览</section>' },
        },
      },
    })
    await wrapper.get('[data-test="emit-save"]').trigger('click')
    expect(wrapper.get('[data-test="save-summary-dialog"]').text()).toContain('本次修改说明')
  })
})
