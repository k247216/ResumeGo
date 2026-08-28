// @vitest-environment happy-dom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import KnowledgeLibraryView from './KnowledgeLibraryView.vue'

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => reactive({ query: {} })),
}))

vi.mock('../../stores/knowledge', () => ({ useKnowledgeStore: vi.fn() }))
vi.mock('../../api/knowledgeDesktop', () => ({
  openKnowledgeSource: vi.fn().mockResolvedValue({ ok: true }),
  revealKnowledgeSource: vi.fn().mockResolvedValue({ ok: true }),
}))
const interviewApi = vi.hoisted(() => ({ previewInterviewQuestionSetFromKnowledgeDocument: vi.fn() }))
vi.mock('../../api/interview', () => interviewApi)

import { useKnowledgeStore } from '../../stores/knowledge'

let viewportWidth = 1440

function doc(id: number, status = 'COMPLETED', title = '文档 ' + id) {
  return { id, title, sourceType: 'FILE', processingStatus: status, sourceFile: null, sourceExtension: null, createdAt: 't', updatedAt: 't' }
}

function storeStub(overrides: Record<string, unknown> = {}) {
  return reactive({
    documents: [doc(1), doc(2, 'FAILED', '失败文件')],
    selectedDocumentId: 1,
    selectedDocument: doc(1),
    loading: false,
    errorMessage: '',
    creating: false,
    importing: false,
    importErrorMessage: '',
    listRefreshError: '',
    contentByDocumentId: {} as Record<number, string>,
    contentLoadingDocumentId: null,
    contentErrorsByDocumentId: {} as Record<number, string>,
    categories: [] as { id: number; name: string }[],
    tags: [] as { id: number; name: string }[],
    catalogLoading: false,
    catalogErrorMessage: '',
    classificationByDocumentId: {} as Record<number, { category: unknown | null; tags: unknown[] }>,
    classificationLoadingDocumentId: null,
    classificationErrorsByDocumentId: {} as Record<number, string>,
    classificationSaving: false,
    searchQuery: '',
    searchCategoryId: null,
    searchTagId: null,
    searchIncludeDescendants: true,
    searchResults: [] as { document: { id: number; title: string }; matchedField: string; snippet: string; lineNumber: number | null }[],
    searchLoading: false,
    searchErrorMessage: '',
    categoryTree: [] as { id: number; name: string; parentId: number | null }[],
    categoryTreeLoading: false,
    categoryTreeError: '',
    browseCategoryId: null,
    browseTagId: null,
    browseIncludeDescendants: true,
    categorizeWarning: '',
    retryingDocumentId: null,
    retryErrorsByDocumentId: {} as Record<number, string>,
    deletionImpactByDocumentId: {} as Record<number, unknown>,
    deletingDocumentId: null,
    deleteErrorsByDocumentId: {} as Record<number, string>,
    noteSavingDocumentId: null,
    noteSaveErrorsByDocumentId: {} as Record<number, string>,
    noteMetadataWarningsByDocumentId: {} as Record<number, string>,
    titleErrorsByDocumentId: {} as Record<number, string>,
    load: vi.fn().mockResolvedValue(undefined),
    retry: vi.fn().mockResolvedValue(undefined),
    select: vi.fn(),
    createNote: vi.fn().mockResolvedValue(undefined),
    createUntitledNote: vi.fn().mockResolvedValue(doc(9, 'COMPLETED', '未命名笔记')),
    renameDocument: vi.fn().mockResolvedValue(undefined),
    importFile: vi.fn().mockResolvedValue(undefined),
    loadContent: vi.fn().mockResolvedValue(undefined),
    loadCatalog: vi.fn().mockResolvedValue(undefined),
    clearCatalogError: vi.fn(),
    createCategory: vi.fn().mockResolvedValue(undefined),
    createTag: vi.fn().mockResolvedValue(undefined),
    loadCategoryTree: vi.fn().mockResolvedValue(undefined),
    browseBy: vi.fn(),
    browseAll: vi.fn(),
    categorizeCreatedDocument: vi.fn().mockResolvedValue(true),
    createCategoryNode: vi.fn().mockResolvedValue(undefined),
    updateCategoryNode: vi.fn().mockResolvedValue(undefined),
    deleteCategoryNode: vi.fn().mockResolvedValue(undefined),
    loadClassification: vi.fn().mockResolvedValue(undefined),
    setCategory: vi.fn().mockResolvedValue(undefined),
    toggleTag: vi.fn().mockResolvedValue(undefined),
    runSearch: vi.fn().mockResolvedValue(undefined),
    setSearchQuery: vi.fn(),
    setSearchFilter: vi.fn(),
    setIncludeDescendants: vi.fn(),
    clearSearch: vi.fn(),
    retryDocument: vi.fn().mockResolvedValue(undefined),
    loadDeletionImpact: vi.fn().mockResolvedValue({ title: 'x', confirmationToken: 't', expiresAt: 't', hasSource: true, hasContent: true, hasCategory: true, hasTags: true }),
    deleteDocument: vi.fn().mockResolvedValue(undefined),
    saveNoteContent: vi.fn().mockResolvedValue(undefined),
    openSource: vi.fn().mockResolvedValue({ ok: true }),
    revealSource: vi.fn().mockResolvedValue({ ok: true }),
    ...overrides,
  })
}

function mountView(store: ReturnType<typeof storeStub>) {
  vi.mocked(useKnowledgeStore).mockReturnValue(store as never)
  return mount(KnowledgeLibraryView, {
    global: {
      plugins: [createPinia()],
      stubs: {
        KnowledgeNavigator: { template: '<div data-test="stub-navigator" />' },
        KnowledgeDocumentList: {
          emits: ['select', 'close'],
          template: '<div data-test="stub-list"><button data-test="stub-list-close" @click="$emit(\'close\')">收起</button><button data-test="stub-select-2" @click="$emit(\'select\', 2)">资料二</button></div>',
        },
        KnowledgeSourceInspector: { template: '<div data-test="stub-inspector" />' },
        KnowledgeNameDialog: { template: '<div data-test="stub-name-dialog" />' },
        KnowledgeFolderDialog: { template: '<div data-test="stub-folder-dialog" />' },
        KnowledgeDeleteDialog: { template: '<div data-test="stub-delete-dialog" />' },
        KnowledgeImportControl: { template: '<button data-test="stub-import">导入</button>' },
      },
    },
  })
}

describe('KnowledgeLibraryView', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    viewportWidth = 1440
    vi.spyOn(window, 'innerWidth', 'get').mockImplementation(() => viewportWidth)
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads documents, catalog, and category tree on mount', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(store.load).toHaveBeenCalled()
    expect(store.loadCatalog).toHaveBeenCalled()
    expect(store.loadCategoryTree).toHaveBeenCalled()
    expect(wrapper.find('[data-test="stub-navigator"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="stub-list"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="knowledge-command-search"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="knowledge-experience-format"]').exists()).toBe(true)
    // ReadingPane 未 stub → 真实渲染
    expect(wrapper.find('[data-test="knowledge-reading-pane"]').exists()).toBe(true)
  })

  it('opens the real-interview format guide from the knowledge command bar', async () => {
    const wrapper = mountView(storeStub())
    await flushPromises()
    await wrapper.get('[data-test="knowledge-experience-format"]').trigger('click')
    expect(wrapper.find('[data-test="knowledge-experience-format-dialog"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="knowledge-experience-format-template"]').text()).toContain('company:')
  })

  it('shows the parsed question count after a real-interview document is refreshed', async () => {
    interviewApi.previewInterviewQuestionSetFromKnowledgeDocument.mockResolvedValue({
      success: true,
      data: { documentId: 2, status: 'READY', questionCount: 3, message: '已按真实面经格式识别，选择真题演练后即可使用' },
    })
    const store = storeStub({
      classificationByDocumentId: {
        2: { category: { id: 5, name: '真实面经', parentId: null }, tags: [] },
      },
      categoryTree: [{ id: 5, name: '真实面经', parentId: null }],
    })
    const wrapper = mountView(store)
    await flushPromises()
    store.selectedDocumentId = 2
    await flushPromises()
    expect(wrapper.get('[data-test="experience-format-status"]').text()).toContain('已识别 3 道面经题目')
  })

  it('opens the document requested by the knowledge route query', async () => {
    const route = await import('vue-router')
    vi.mocked(route.useRoute).mockReturnValue(reactive({ query: { documentId: '2' } }) as never)
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(store.select).toHaveBeenCalledWith(2)
    expect(wrapper.find('[data-test="stub-inspector"]').exists()).toBe(true)
  })

  it('searches from the command bar and shows results scope', async () => {
    const store = storeStub({ searchQuery: '笔记', searchResults: [{ document: doc(7), matchedField: 'TITLE', snippet: 'x', lineNumber: null }] })
    const wrapper = mountView(store)
    await flushPromises()
    await wrapper.get('[data-test="knowledge-command-search"]').setValue('TensorFlow')
    expect(store.setSearchQuery).toHaveBeenCalledWith('TensorFlow')
  })

  it('creates a real note with one click and enters inline title editing without a dialog', async () => {
    const store = storeStub()
    store.createUntitledNote = vi.fn(async () => {
      const created = { ...doc(9, 'COMPLETED', '未命名笔记'), sourceType: 'NOTE' }
      store.documents.unshift(created)
      store.selectedDocumentId = created.id
      store.selectedDocument = created
      return created
    })
    const wrapper = mountView(store)
    await flushPromises()
    await wrapper.get('[data-test="knowledge-command-create-note"]').trigger('click')
    await flushPromises()
    expect(store.createUntitledNote).toHaveBeenCalledOnce()
    expect(wrapper.find('[data-test="knowledge-note-dialog"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="knowledge-title-input"]').exists()).toBe(true)
  })

  it('shows the inspector by default on a wide window', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(wrapper.find('[data-test="stub-inspector"]').exists()).toBe(true)
  })

  it('switches documents immediately with autosave (no reminder dialog)', async () => {
    const note1 = { ...doc(1), sourceType: 'NOTE', title: '笔记一' }
    const note2 = { ...doc(2), sourceType: 'NOTE', title: '笔记二' }
    const store = storeStub({
      documents: [note1, note2],
      selectedDocument: note1,
      contentByDocumentId: { 1: '原正文' },
    })
    const wrapper = mountView(store)
    await flushPromises()

    await wrapper.get('[data-test="stub-select-2"]').trigger('click')
    await flushPromises()

    expect(store.select).toHaveBeenCalledWith(2)
    expect(wrapper.find('[data-test="knowledge-unsaved-dialog"]').exists()).toBe(false)
  })

  it('recomputes inspector overlay mode after window resize', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()
    expect(wrapper.find('.inspector-wrap').classes()).not.toContain('is-overlay')

    viewportWidth = 1200
    window.dispatchEvent(new Event('resize'))
    await wrapper.vm.$nextTick()
    await wrapper.get('[data-test="reading-open-inspector"]').trigger('click')
    expect(wrapper.find('.inspector-wrap').classes()).toContain('is-overlay')
  })

  it('fully removes a closed document list and restores it from the command bar', async () => {
    const store = storeStub()
    const wrapper = mountView(store)
    await flushPromises()

    await wrapper.get('[data-test="stub-list-close"]').trigger('click')
    expect(wrapper.find('[data-test="stub-list"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="knowledge-restore-list"]').exists()).toBe(true)

    await wrapper.get('[data-test="knowledge-restore-list"]').trigger('click')
    expect(wrapper.find('[data-test="stub-list"]').exists()).toBe(true)
  })

  it('persists pane collapse state locally and restores it on the next open', async () => {
    const store = storeStub()
    const first = mountView(store)
    await flushPromises()
    await first.get('[data-test="stub-list-close"]').trigger('click')
    expect(first.find('[data-test="stub-list"]').exists()).toBe(false)
    const saved = localStorage.getItem('resumego:knowledge:pane-state')
    expect(saved).toBeTruthy()
    expect(saved).toContain('"listCollapsed":true')

    const second = mountView(store)
    await flushPromises()
    expect(second.find('[data-test="stub-list"]').exists()).toBe(false)
    expect(second.find('[data-test="knowledge-restore-list"]').exists()).toBe(true)
  })
})
