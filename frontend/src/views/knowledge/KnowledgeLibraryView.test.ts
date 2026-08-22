// @vitest-environment happy-dom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import KnowledgeLibraryView from './KnowledgeLibraryView.vue'
import type { KnowledgeDocument } from '../../types/knowledge'

vi.mock('../../stores/knowledge', () => ({ useKnowledgeStore: vi.fn() }))

import { useKnowledgeStore } from '../../stores/knowledge'

function doc(id: number, status: KnowledgeDocument['processingStatus'], title = '文档 ' + id): KnowledgeDocument {
  return {
    id, title, sourceType: 'FILE', processingStatus: status,
    sourceFile: null, createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function storeStub(overrides: Record<string, unknown> = {}) {
  return reactive({
    documents: [doc(1, 'COMPLETED'), doc(2, 'PENDING')],
    selectedDocumentId: 1,
    selectedDocument: doc(1, 'COMPLETED'),
    loading: false,
    errorMessage: '',
    creating: false,
    importing: false,
    importErrorMessage: '',
    listRefreshError: '',
    contentByDocumentId: {} as Record<number, string>,
    contentLoadingDocumentId: null,
    contentErrorsByDocumentId: {} as Record<number, string>,
    load: vi.fn().mockResolvedValue(undefined),
    retry: vi.fn().mockResolvedValue(undefined),
    select: vi.fn(),
    createNote: vi.fn().mockResolvedValue(undefined),
    importFile: vi.fn().mockResolvedValue(undefined),
    loadContent: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  })
}

function mountView(store: ReturnType<typeof storeStub>) {
  vi.mocked(useKnowledgeStore).mockReturnValue(store as never)
  return mount(KnowledgeLibraryView, {
    global: {
      plugins: [createPinia()],
      stubs: {
        PageHeader: { template: '<header><slot name="actions" /></header>' },
        KnowledgeListRail: { template: '<div data-test="stub-rail" />' },
        KnowledgeDetailPane: {
          props: ['content', 'contentError', 'contentLoading'],
          template: '<div data-test="stub-detail" :data-content="content" :data-error="contentError" :data-loading="String(contentLoading)" />',
        },
      },
    },
  })
}

describe('KnowledgeLibraryView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads the library on mount and renders the two panes', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(store.load).toHaveBeenCalled()
    expect(wrapper.find('[data-test="stub-rail"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stub-detail"]').exists()).toBe(true)
  })

  it('refreshes via the header action', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    await wrapper.get('[data-test="knowledge-refresh"]').trigger('click')
    expect(store.retry).toHaveBeenCalled()
  })

  it('creates a NOTE through the dialog and closes it on success', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()

    await wrapper.get('[data-test="knowledge-create-note"]').trigger('click')
    const dialog = wrapper.get('[data-test="knowledge-note-dialog"]')
    await dialog.get('[data-test="knowledge-note-title"]').setValue('  新笔记  ')
    await dialog.find('form').trigger('submit')

    expect(store.createNote).toHaveBeenCalledWith('新笔记')
    await flushPromises()
    expect(wrapper.find('[data-test="knowledge-note-dialog"]').exists()).toBe(false)
  })

  it('keeps the dialog open when note creation fails', async () => {
    const store = storeStub({ createNote: vi.fn().mockRejectedValue(new Error('标题不能为空')) })
    const wrapper = mountView(store)
    await flushPromises()

    await wrapper.get('[data-test="knowledge-create-note"]').trigger('click')
    await wrapper.get('[data-test="knowledge-note-title"]').setValue('x')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-test="knowledge-note-dialog"]').exists()).toBe(true)
  })

  it('imports only the picked File through the hidden input', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()

    const file = new File(['# 内容'], 'notes.md', { type: 'text/markdown' })
    const input = wrapper.get('[data-test="knowledge-file-input"]')
    Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
    await input.trigger('change')

    expect(store.importFile).toHaveBeenCalledTimes(1)
    expect(store.importFile.mock.calls[0][0]).toBe(file)
  })

  it('loads content when the selected COMPLETED document changes', async () => {
    const store = storeStub()
    mountView(store)
    await flushPromises()

    store.selectedDocumentId = 2
    await flushPromises()
    expect(store.loadContent).toHaveBeenCalledWith(2)
  })

  it('passes cached content and errors of the selected document to the detail pane', async () => {
    const store = storeStub({
      contentByDocumentId: { 1: '提取的正文' },
      contentLoadingDocumentId: null,
    })
    const wrapper = mountView(store)
    await flushPromises()
    expect(wrapper.get('[data-test="stub-detail"]').attributes('data-content')).toBe('提取的正文')

    store.contentErrorsByDocumentId = { 1: '内容仍在处理中，请稍后重试' }
    await flushPromises()
    expect(wrapper.get('[data-test="stub-detail"]').attributes('data-error')).toBe('内容仍在处理中，请稍后重试')
  })

  it('shows the refresh notice after a successful import with failed list refresh', async () => {
    const store = storeStub({ listRefreshError: '已导入，列表刷新失败，可重试刷新' })
    const wrapper = mountView(store)
    await flushPromises()
    const notice = wrapper.get('[data-test="knowledge-list-refresh-error"]')
    expect(notice.text()).toContain('已导入，列表刷新失败')
    expect(store.importErrorMessage).toBe('')
  })

  it('hides the refresh notice when the list is healthy', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(wrapper.find('[data-test="knowledge-list-refresh-error"]').exists()).toBe(false)
  })
})
