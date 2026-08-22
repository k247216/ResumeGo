// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
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

describe('KnowledgeReadingPane', () => {
  it('directly edits local notes and real managed Markdown, keeps TXT read-only', () => {
    const note = mountPane()
    expect(note.find('[data-test="knowledge-body-editor"]').exists()).toBe(true)
    expect(note.find('[data-test="knowledge-title-input"]').exists()).toBe(true)
    expect(note.find('[data-test="knowledge-edit-start"]').exists()).toBe(false)

    const markdown = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'notes.md', sourceExtension: 'md' }) })
    expect(markdown.find('[data-test="knowledge-body-editor"]').exists()).toBe(true)
    expect(markdown.find('[data-test="knowledge-edit-start"]').exists()).toBe(false)

    const text = mountPane({ document: doc({ sourceType: 'FILE', sourceFile: 'notes.txt', sourceExtension: 'txt' }) })
    expect(text.find('[data-test="knowledge-body-editor"]').exists()).toBe(false)
    expect(text.find('[data-test="knowledge-edit-start"]').exists()).toBe(false)
    expect(text.get('[data-test="markdown-view"]').text()).toContain('# 原正文')
  })

  it('beginEdit focuses the inline title and body edits report dirty state', async () => {
    const wrapper = mountPane()
    await (wrapper.vm as unknown as { beginEdit: (options: { focusTitle: boolean }) => Promise<void> }).beginEdit({ focusTitle: true })

    const title = wrapper.get<HTMLInputElement>('[data-test="knowledge-title-input"]')
    expect(document.activeElement).toBe(title.element)
    await wrapper.get('[data-test="knowledge-body-editor"]').setValue('# 新正文')
    expect((wrapper.vm as unknown as { hasUnsavedChanges: () => boolean }).hasUnsavedChanges()).toBe(true)
    expect(wrapper.find('[data-test="knowledge-editor-bar"]').exists()).toBe(true)
  })

  it('emits explicit body save and changed title rename without silently autosaving body', async () => {
    const wrapper = mountPane()
    await wrapper.get('[data-test="knowledge-body-editor"]').setValue('更新正文')
    await wrapper.get('[data-test="knowledge-title-input"]').setValue('Redis 深入复习')
    await wrapper.get('[data-test="knowledge-title-input"]').trigger('blur')

    expect(wrapper.emitted('rename-title')).toEqual([['Redis 深入复习']])
    expect(wrapper.emitted('save-content')).toBeUndefined()

    await wrapper.get('[data-test="knowledge-edit-save"]').trigger('click')
    expect(wrapper.emitted('save-content')).toEqual([['更新正文']])
  })

  it('discards local draft and restores the persisted title and body', async () => {
    const wrapper = mountPane()
    await wrapper.get('[data-test="knowledge-title-input"]').setValue('临时标题')
    await wrapper.get('[data-test="knowledge-body-editor"]').setValue('临时正文')

    ;(wrapper.vm as unknown as { discardChanges: () => void }).discardChanges()
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as unknown as { hasUnsavedChanges: () => boolean }).hasUnsavedChanges()).toBe(false)
    expect((wrapper.get('[data-test="knowledge-title-input"]').element as HTMLInputElement).value).toBe('Redis 笔记')
    expect((wrapper.get('[data-test="knowledge-body-editor"]').element as HTMLTextAreaElement).value).toBe('# 原正文')
  })
})