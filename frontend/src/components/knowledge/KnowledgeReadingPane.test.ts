// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import KnowledgeReadingPane from './KnowledgeReadingPane.vue'
import type { KnowledgeDocument } from '../../types/knowledge'

function doc(overrides: Partial<KnowledgeDocument> = {}): KnowledgeDocument {
  return {
    id: 1,
    title: 'Redis 笔记',
    sourceType: 'NOTE',
    processingStatus: 'COMPLETED',
    sourceFile: null,
    sourceExtension: null,
    createdAt: '2026-08-23T00:00:00',
    updatedAt: '2026-08-23T00:00:00',
    ...overrides,
  }
}

function mountPane(overrides: Record<string, unknown> = {}) {
  return mount(KnowledgeReadingPane, {
    attachTo: document.body,
    props: {
      document: doc(),
      content: '# 原正文',
      contentLoading: false,
      contentError: '',
      saving: false,
      error: '',
      titleError: '',
      ...overrides,
    },
    global: {
      stubs: {
        'el-icon': { template: '<i><slot /></i>' },
        KnowledgeMarkdownView: { props: ['source'], template: '<pre data-test="markdown-view">{{ source }}</pre>' },
      },
    },
  })
}

afterEach(() => {
  vi.useRealTimers()
})

describe('KnowledgeReadingPane', () => {
  it('renders Markdown in reading mode by default and offers edit mode for NOTE/md only', async () => {
    const note = mountPane()
    expect(note.find('[data-test="document-view"]').exists()).toBe(true)
    expect(note.get('[data-test="markdown-view"]').text()).toContain('# 原正文')
    expect(note.find('[data-test="mode-edit"]').exists()).toBe(true)
    expect(note.find('[data-test="knowledge-body-editor"]').exists()).toBe(false)

    const markdown = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'notes.md', sourceExtension: 'md' }) })
    expect(markdown.find('[data-test="mode-edit"]').exists()).toBe(true)

    const text = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'notes.txt', sourceExtension: 'txt' }) })
    expect(text.find('[data-test="mode-edit"]').exists()).toBe(false)
    expect(text.find('[data-test="knowledge-body-editor"]').exists()).toBe(false)
  })

  it('edit mode keeps source editing only (no extra preview pane) and autosaves', async () => {
    vi.useFakeTimers()
    const wrapper = mountPane()
    await wrapper.get('[data-test="mode-edit"]').trigger('click')
    const editor = wrapper.get('[data-test="knowledge-body-editor"]')
    expect(wrapper.find('[data-test="live-preview"]').exists()).toBe(false)
    await editor.setValue('# 新正文\n\n**加粗**')
    expect(wrapper.emitted('save-content')).toBeUndefined()
    await vi.advanceTimersByTimeAsync(1000)
    expect(wrapper.emitted('save-content')).toEqual([['# 新正文\n\n**加粗**']])
  })

  it('autosaves an inline title change as an explicit rename', async () => {
    vi.useFakeTimers()
    const wrapper = mountPane()
    await wrapper.get('[data-test="knowledge-title-input"]').setValue('Redis 深入复习')
    await vi.advanceTimersByTimeAsync(1000)
    expect(wrapper.emitted('rename-title')).toEqual([['Redis 深入复习']])
    expect(wrapper.emitted('save-content')).toBeUndefined()
  })

  it('shows an honest notice for metadata-only xlsx and a preview slot for pdf', () => {
    const xlsx = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'a.xlsx', sourceExtension: 'xlsx', processingStatus: 'METADATA_ONLY' }), content: '' })
    expect(xlsx.find('[data-test="metadata-only-notice"]').exists()).toBe(true)
    expect(xlsx.find('[data-test="mode-edit"]').exists()).toBe(false)
    expect(xlsx.find('[data-test="knowledge-body-editor"]').exists()).toBe(false)

    const pdf = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'a.pdf', sourceExtension: 'pdf', processingStatus: 'METADATA_ONLY' }), content: '' })
    expect(pdf.find('[data-test="source-preview-pdf"]').exists()).toBe(true)
  })

  it('flushPendingSave persists pending edits before a document switch', async () => {
    vi.useFakeTimers()
    const wrapper = mountPane()
    await wrapper.get('[data-test="mode-edit"]').trigger('click')
    await wrapper.get('[data-test="knowledge-body-editor"]').setValue('未保存的正文')
    await (wrapper.vm as unknown as { flushPendingSave: () => Promise<void> }).flushPendingSave()
    expect(wrapper.emitted('save-content')).toEqual([['未保存的正文']])
  })

  it('formatting toolbar wraps selection with markdown syntax', async () => {
    const wrapper = mountPane()
    await wrapper.get('[data-test="mode-edit"]').trigger('click')
    const editor = wrapper.get<HTMLTextAreaElement>('[data-test="knowledge-body-editor"]')
    await editor.setValue('核心内容')
    editor.element.setSelectionRange(0, 2)
    await wrapper.get('[data-test="fmt-bold"]').trigger('mousedown')
    expect((wrapper.get('[data-test="knowledge-body-editor"]').element as HTMLTextAreaElement).value).toBe('**核心**内容')
    await wrapper.get('[data-test="fmt-list"]').trigger('mousedown')
    expect((wrapper.get('[data-test="knowledge-body-editor"]').element as HTMLTextAreaElement).value).toBe('**- 核心**内容')
  })

  it('beginEdit focuses the inline title and enters edit stage for a new note', async () => {
    const wrapper = mountPane()
    await (wrapper.vm as unknown as { beginEdit: (options: { focusTitle: boolean }) => Promise<void> }).beginEdit({ focusTitle: true })
    expect(wrapper.find('[data-test="knowledge-body-editor"]').exists()).toBe(true)
    const title = wrapper.get<HTMLInputElement>('[data-test="knowledge-title-input"]')
    expect(document.activeElement).toBe(title.element)
  })
})