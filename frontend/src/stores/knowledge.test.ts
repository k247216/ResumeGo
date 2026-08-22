import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useKnowledgeStore } from './knowledge'
import type { KnowledgeDocument } from '../types/knowledge'

vi.mock('../api/knowledge', () => {
  const api = {
    listKnowledgeDocuments: vi.fn(),
    createKnowledgeNote: vi.fn(),
    importKnowledgeFile: vi.fn(),
    getKnowledgeDocument: vi.fn(),
    getKnowledgeContent: vi.fn(),
    listKnowledgeCategories: vi.fn(),
    createKnowledgeCategory: vi.fn(),
    listKnowledgeTags: vi.fn(),
    createKnowledgeTag: vi.fn(),
    getDocumentClassification: vi.fn(),
    setDocumentCategory: vi.fn(),
    removeDocumentCategory: vi.fn(),
    addDocumentTag: vi.fn(),
    removeDocumentTag: vi.fn(),
    searchKnowledge: vi.fn(),
    retryKnowledgeDocument: vi.fn(),
    getKnowledgeDeletionImpact: vi.fn(),
    deleteKnowledgeDocument: vi.fn(),
    listKnowledgeCategoryTree: vi.fn(),
    updateKnowledgeCategory: vi.fn(),
    deleteKnowledgeCategory: vi.fn(),
    saveKnowledgeNoteContent: vi.fn(),
    KnowledgeHttpError: class KnowledgeHttpError extends Error {
      status: number
      constructor(status: number, message: string) {
        super(message)
        this.status = status
      }
    },
  }
  return api
})

import * as knowledgeApi from '../api/knowledge'

const api = vi.mocked(knowledgeApi)

function doc(id: number, status: KnowledgeDocument['processingStatus'], title = '笔记 ' + id): KnowledgeDocument {
  return {
    id, title, sourceType: id % 2 === 0 ? 'FILE' : 'NOTE', processingStatus: status,
    sourceFile: null, createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function ok<T>(data: T): { success: true; data: T; message: null } {
  return { success: true, data, message: null }
}

const file = (name = 'notes.md') => new File(['# 内容'], name, { type: 'text/markdown' })

describe('useKnowledgeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('FE-00 loads documents in API order and selects the first', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(2, 'PENDING'), doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    expect(store.documents.map((d) => d.id)).toEqual([2, 1])
    expect(store.selectedDocumentId).toBe(2)
  })

  it('FE-00 keeps an existing valid selection after reload and clears error on retry', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'PENDING')]))
    const store = useKnowledgeStore()
    await store.load()
    store.select(2)
    expect(store.selectedDocumentId).toBe(2)

    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(1, 'COMPLETED'), doc(2, 'PENDING')]))
    await store.load()
    expect(store.selectedDocumentId).toBe(2)

    api.listKnowledgeDocuments.mockRejectedValueOnce(new Error('网络错误'))
    await expect(store.load()).rejects.toThrow('网络错误')
    expect(store.documents).toHaveLength(2)
    expect(store.errorMessage).toContain('网络错误')

    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(1, 'COMPLETED')]))
    await store.retry()
    expect(store.errorMessage).toBe('')
  })

  it('FE-00 creating a NOTE inserts it, selects it, and sorts newest first', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    const created = { ...doc(5, 'NOT_STARTED', '新笔记'), updatedAt: '2026-08-22T11:00:00' }
    api.createKnowledgeNote.mockResolvedValue(ok(created))
    await store.createNote('新笔记')

    expect(store.documents[0].id).toBe(5)
    expect(store.selectedDocumentId).toBe(5)
  })

  it('FE-00 create failure keeps list and rethrows', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    const before = store.documents

    api.createKnowledgeNote.mockRejectedValue(new Error('标题不能为空'))
    await expect(store.createNote('')).rejects.toThrow('标题不能为空')
    expect(store.documents).toEqual(before)
    expect(store.errorMessage).toContain('标题不能为空')
  })

  it('FE-00 import success refreshes list and selects the imported document', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(9, 'COMPLETED', 'notes.md')))
    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(9, 'COMPLETED', 'notes.md'), doc(1, 'COMPLETED')]))

    const result = await store.importFile(file())
    expect(result.documentId).toBe(9)
    expect(store.listRefreshError).toBe('')
    expect(api.listKnowledgeDocuments).toHaveBeenCalledTimes(2)
    expect(store.selectedDocumentId).toBe(9)
    expect(store.documents.map((d) => d.id)).toContain(9)
  })

  it('FE-00 duplicate import still refreshes and selects the existing document', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 1, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: true, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(1, 'COMPLETED')))
    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(1, 'COMPLETED')]))

    const result = await store.importFile(file())
    expect(result.duplicate).toBe(true)
    expect(store.listRefreshError).toBe('')
    expect(store.selectedDocumentId).toBe(1)
  })

  it('FE-00 import failure only touches importErrorMessage and keeps list', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockRejectedValue(new Error('仅支持 .md/.txt 文件'))
    await expect(store.importFile(file('doc.pdf'))).rejects.toThrow('仅支持 .md/.txt 文件')
    expect(store.documents).toHaveLength(1)
    expect(store.importErrorMessage).toContain('仅支持 .md/.txt 文件')
    expect(store.errorMessage).toBe('')
  })

  it('FE-00 loads content once for COMPLETED documents and caches per document', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.getKnowledgeContent.mockResolvedValue(ok({ documentId: 1, content: '正文一' }))
    await store.loadContent(1)
    await store.loadContent(1)
    expect(api.getKnowledgeContent).toHaveBeenCalledTimes(1)
    expect(store.contentByDocumentId[1]).toBe('正文一')

    api.getKnowledgeContent.mockResolvedValue(ok({ documentId: 2, content: '正文二' }))
    await store.loadContent(2)
    expect(store.contentByDocumentId[2]).toBe('正文二')
    expect(store.contentByDocumentId[1]).toBe('正文一')
  })

  it('FE-00 does not request content for non-COMPLETED documents', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'PENDING'), doc(2, 'FAILED')]))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadContent(1)
    await store.loadContent(2)
    expect(api.getKnowledgeContent).not.toHaveBeenCalled()
  })

  it('FE-00 maps 409 to "still processing" and 404 to unavailable per document', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.getKnowledgeContent.mockRejectedValueOnce(new api.KnowledgeHttpError(409, '尚未完成'))
    await expect(store.loadContent(1)).rejects.toThrow()
    expect(store.contentErrorsByDocumentId[1]).toBe('内容仍在处理中，请稍后重试')

    api.getKnowledgeContent.mockRejectedValueOnce(new api.KnowledgeHttpError(404, '不存在'))
    await expect(store.loadContent(1)).rejects.toThrow()
    expect(store.contentErrorsByDocumentId[1]).toBe('内容暂不可用')
  })

  it('FE-00 content failure does not pollute other documents', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.getKnowledgeContent.mockResolvedValueOnce(ok({ documentId: 1, content: '正文一' }))
    api.getKnowledgeContent.mockRejectedValueOnce(new Error('网络错误'))
    await store.loadContent(1)
    await expect(store.loadContent(2)).rejects.toThrow('网络错误')

    expect(store.contentByDocumentId[1]).toBe('正文一')
    expect(store.contentByDocumentId[2]).toBeUndefined()
    expect(store.contentErrorsByDocumentId[2]).toContain('网络错误')
  })

  it('FE-00 import success is not rewritten as failure when list refresh fails', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(9, 'COMPLETED', 'notes.md')))
    api.listKnowledgeDocuments.mockRejectedValueOnce(new Error('网络错误'))

    const result = await store.importFile(file())

    expect(result.documentId).toBe(9)
    expect(store.importErrorMessage).toBe('')
    expect(store.listRefreshError).toContain('已导入，列表刷新失败，可重试刷新')
    expect(store.selectedDocumentId).toBe(9)
    expect(store.documents.map((d) => d.id)).toContain(9)
  })

  it('FE-00 detail fetch failure after upload keeps success and shows refresh notice', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockRejectedValue(new Error('网络错误'))

    const result = await store.importFile(file())

    expect(result.documentId).toBe(9)
    expect(store.importErrorMessage).toBe('')
    expect(store.listRefreshError).toContain('已导入，列表刷新失败')
  })

  it('FE-00 a successful retry clears the list refresh notice', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(9, 'COMPLETED', 'notes.md')))
    api.listKnowledgeDocuments.mockRejectedValueOnce(new Error('网络错误'))
    await store.importFile(file())
    expect(store.listRefreshError).toContain('已导入')

    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(9, 'COMPLETED'), doc(1, 'COMPLETED')]))
    await store.retry()
    expect(store.listRefreshError).toBe('')
  })

  it('FE-01 loads the category and tag catalog', async () => {
    api.listKnowledgeCategories.mockResolvedValue(ok([{ id: 1, name: '求职', normalizedName: '求职', parentId: null, createdAt: 't', updatedAt: 't' }]))
    api.listKnowledgeTags.mockResolvedValue(ok([{ id: 2, name: '机器学习', normalizedName: '机器学习', parentId: null, createdAt: 't', updatedAt: 't' }]))
    const store = useKnowledgeStore()
    await store.loadCatalog()
    expect(store.categories.map((c) => c.name)).toEqual(['求职'])
    expect(store.tags.map((t) => t.name)).toEqual(['机器学习'])
  })

  it('FE-01 empty search does not call the API and clears results', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    store.searchResults = [{ document: doc(1, 'COMPLETED'), matchedField: 'TITLE', snippet: 'x', lineNumber: null }]

    store.setSearchQuery('')
    await vi.waitFor(() => expect(store.searchResults).toEqual([]))
    expect(api.searchKnowledge).not.toHaveBeenCalled()
  })

  it('FE-01 discards stale search responses with request sequence', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    let resolveFirst: (v: { success: true; data: never[]; message: null }) => void = () => {}
    const first = new Promise<{ success: true; data: never[]; message: null }>((resolve) => { resolveFirst = resolve })
    api.searchKnowledge.mockImplementationOnce(() => first)
      .mockResolvedValueOnce(ok([{ document: doc(1, 'COMPLETED'), matchedField: 'TITLE', snippet: '第二条', lineNumber: null }]))

    store.setSearchQuery('a')
    store.setSearchQuery('b')
    await vi.waitFor(() => expect(store.searchResults).toHaveLength(1))
    // 晚到的第一条响应不得覆盖第二条结果
    resolveFirst({ success: true, data: [], message: null })
    await vi.waitFor(() => expect(store.searchLoading).toBe(false))
    expect(store.searchResults).toHaveLength(1)
    expect(store.searchResults[0].snippet).toBe('第二条')
  })

  it('FE-01 search failure is local and does not clear documents', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.searchKnowledge.mockRejectedValueOnce(new Error('搜索失败'))
    store.setSearchQuery('词')
    await vi.waitFor(() => expect(store.searchErrorMessage).toContain('搜索失败'))
    expect(store.documents).toHaveLength(1)
  })

  it('FE-01 clears invalid filters instead of applying them', async () => {
    api.listKnowledgeCategories.mockResolvedValue(ok([{ id: 1, name: '求职', normalizedName: '求职', parentId: null, createdAt: 't', updatedAt: 't' }]))
    api.listKnowledgeTags.mockResolvedValue(ok([{ id: 2, name: '机器学习', normalizedName: '机器学习', parentId: null, createdAt: 't', updatedAt: 't' }]))
    api.searchKnowledge.mockResolvedValue(ok([]))
    const store = useKnowledgeStore()
    await store.loadCatalog()

    store.setSearchQuery('词')
    store.setSearchFilter('category', 99) // 失效
    expect(store.searchCategoryId).toBeNull()
    store.setSearchFilter('category', 1)
    expect(store.searchCategoryId).toBe(1)
    store.setSearchFilter('tag', 2)
    expect(store.searchTagId).toBe(2)
    expect(api.searchKnowledge).toHaveBeenCalledWith('词', 1, 2, true)
  })

  it('FE-01 loads classification per document on demand', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    api.getDocumentClassification.mockResolvedValue(ok({ category: null, tags: [] }))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadClassification(1)
    await store.loadClassification(1)
    expect(api.getDocumentClassification).toHaveBeenCalledTimes(1)
    expect(store.classificationByDocumentId[1]).toEqual({ category: null, tags: [] })
  })

  it('FE-01 category write re-reads server state on success', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadClassification(1)

    api.setDocumentCategory.mockResolvedValue(ok(null))
    api.getDocumentClassification.mockResolvedValueOnce(ok({
      category: { id: 3, name: '求职', normalizedName: '求职', parentId: null, createdAt: 't', updatedAt: 't' },
      tags: [],
    }))

    await store.setCategory(1, 3)
    expect(api.setDocumentCategory).toHaveBeenCalledWith(1, 3)
    expect(store.classificationByDocumentId[1].category?.id).toBe(3)
    expect(store.classificationErrorsByDocumentId[1]).toBeUndefined()
  })

  it('FE-01 category write failure keeps the previous state', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadClassification(1)
    const before = store.classificationByDocumentId[1]

    api.setDocumentCategory.mockRejectedValue(new Error('设置分类失败'))
    await expect(store.setCategory(1, 3)).rejects.toThrow('设置分类失败')
    expect(store.classificationByDocumentId[1]).toEqual(before)
    expect(store.classificationErrorsByDocumentId[1]).toContain('设置分类失败')
  })

  it('FE-01 tag toggle re-reads server state on success and keeps old on failure', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadClassification(1)

    api.addDocumentTag.mockResolvedValue(ok(null))
    api.getDocumentClassification.mockResolvedValueOnce(ok({
      category: null,
      tags: [{ id: 2, name: '机器学习', normalizedName: '机器学习', parentId: null, createdAt: 't', updatedAt: 't' }],
    }))
    await store.toggleTag(1, 2, true)
    expect(store.classificationByDocumentId[1].tags.map((t) => t.id)).toEqual([2])

    api.addDocumentTag.mockRejectedValueOnce(new Error('添加标签失败'))
    await expect(store.toggleTag(1, 3, true)).rejects.toThrow('添加标签失败')
    expect(store.classificationByDocumentId[1].tags.map((t) => t.id)).toEqual([2])
  })

  it('FE-01 save errors are scoped per document and do not leak across selection', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    await store.loadClassification(1)
    await store.loadClassification(2)

    // 文档 1 保存失败
    api.setDocumentCategory.mockRejectedValueOnce(new Error('设置分类失败'))
    await expect(store.setCategory(1, 3)).rejects.toThrow('设置分类失败')
    expect(store.classificationErrorsByDocumentId[1]).toContain('设置分类失败')
    // 文档 2 无错误
    expect(store.classificationErrorsByDocumentId[2]).toBeUndefined()

    // 切换保存文档 2 成功：只清文档 2，文档 1 的错误保留
    api.setDocumentCategory.mockResolvedValueOnce(ok(null))
    api.getDocumentClassification.mockResolvedValueOnce(ok({
      category: { id: 4, name: '技能', normalizedName: '技能', parentId: null, createdAt: 't', updatedAt: 't' },
      tags: [],
    }))
    await store.setCategory(2, 4)
    expect(store.classificationErrorsByDocumentId[1]).toContain('设置分类失败')
    expect(store.classificationErrorsByDocumentId[2]).toBeUndefined()
  })

  it('FE-01 search failure keeps old results and marks them stale', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.searchKnowledge.mockResolvedValueOnce(ok([{
      document: doc(1, 'COMPLETED'), matchedField: 'TITLE', snippet: '旧结果', lineNumber: null,
    }]))
    store.setSearchQuery('a')
    await vi.waitFor(() => expect(store.searchResults).toHaveLength(1))

    api.searchKnowledge.mockRejectedValueOnce(new Error('搜索失败'))
    store.setSearchQuery('b')
    await vi.waitFor(() => expect(store.searchErrorMessage).toContain('搜索失败'))
    // 旧结果保留（由组件标注"上一次结果"），不伪装成当前结果
    expect(store.searchResults).toHaveLength(1)
    expect(store.searchResults[0].snippet).toBe('旧结果')
  })

  it('FE-02 starts search within the active browse scope and can select a result outside the browse list', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    store.browseBy('category', 3)
    await vi.waitFor(() => expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true))

    api.searchKnowledge.mockResolvedValueOnce(ok([{
      document: doc(9, 'COMPLETED', '范围内命中'), matchedField: 'TITLE', snippet: '命中', lineNumber: null,
    }]))
    store.setSearchQuery('命中')
    await vi.waitFor(() => expect(store.searchResults).toHaveLength(1))

    expect(api.searchKnowledge).toHaveBeenLastCalledWith('命中', 3, null, true)
    store.select(9)
    expect(store.selectedDocumentId).toBe(9)
    expect(store.selectedDocument?.title).toBe('范围内命中')
  })

  it('FE-02 loads the hierarchical category tree', async () => {
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([
      { id: 1, name: 'A', normalizedName: 'a', parentId: null, depth: 0, directDocumentCount: 1, descendantDocumentCount: 3, createdAt: 't', updatedAt: 't' },
    ]))
    const store = useKnowledgeStore()
    await store.loadCategoryTree()
    expect(store.categoryTree).toHaveLength(1)
    expect(store.categoryTree[0].descendantDocumentCount).toBe(3)
  })

  it('FE-02 retries a failed document and refreshes content when completed', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'FAILED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.retryKnowledgeDocument.mockResolvedValue(ok(doc(1, 'COMPLETED')))
    api.getKnowledgeContent.mockResolvedValue(ok({ documentId: 1, content: '重试正文' }))
    await store.retryDocument(1)

    expect(store.documents.find((d) => d.id === 1)?.processingStatus).toBe('COMPLETED')
    expect(store.retryErrorsByDocumentId[1]).toBeUndefined()
    expect(api.getKnowledgeContent).toHaveBeenCalledWith(1)
  })

  it('FE-02 retry failure keeps state and records error', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'FAILED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.retryKnowledgeDocument.mockRejectedValue(new Error('重试失败'))
    await expect(store.retryDocument(1)).rejects.toThrow('重试失败')
    expect(store.retryErrorsByDocumentId[1]).toContain('重试失败')
    expect(store.retryErrorsByDocumentId[2]).toBeUndefined()
  })

  it('FE-02 delete removes the document everywhere and keeps others', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    store.searchResults = [{ document: doc(1, 'COMPLETED'), matchedField: 'TITLE', snippet: 'x', lineNumber: null }]
    store.select(1)

    api.deleteKnowledgeDocument.mockResolvedValue(ok(null))
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([]))
    await store.deleteDocument(1, 'tok')

    expect(store.documents.map((d) => d.id)).toEqual([2])
    expect(store.searchResults.map((r) => r.document.id)).toEqual([])
    expect(store.selectedDocumentId).toBe(2)
  })

  it('FE-02 delete failure keeps the document in the list', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.deleteKnowledgeDocument.mockRejectedValue(new Error('删除失败'))
    await expect(store.deleteDocument(1, 'tok')).rejects.toThrow('删除失败')
    expect(store.documents).toHaveLength(1)
    expect(store.deleteErrorsByDocumentId[1]).toContain('删除失败')
    expect(store.deleteErrorsByDocumentId[2]).toBeUndefined()
  })

  it('FE-02 saves NOTE content and updates the cache', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'NOT_STARTED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.saveKnowledgeNoteContent.mockResolvedValue(ok({ documentId: 1, content: '新正文' }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(1, 'COMPLETED')))
    await store.saveNoteContent(1, '新正文')
    expect(store.contentByDocumentId[1]).toBe('新正文')
    expect(store.documents.find((d) => d.id === 1)?.processingStatus).toBe('COMPLETED')
  })

  it('FE-02 creates a category node under a parent and refreshes the tree', async () => {
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([]))
    const store = useKnowledgeStore()
    await store.loadCategoryTree()
    api.createKnowledgeCategory.mockResolvedValue(ok({ id: 9, name: '子', normalizedName: '子', parentId: 1, createdAt: 't', updatedAt: 't' }))
    api.listKnowledgeCategoryTree.mockResolvedValueOnce(ok([{ id: 9, name: '子', normalizedName: '子', parentId: 1, depth: 1, directDocumentCount: 0, descendantDocumentCount: 0, createdAt: 't', updatedAt: 't' }]))
    await store.createCategoryNode('子', 1)
    expect(api.createKnowledgeCategory).toHaveBeenCalledWith('子', 1)
    expect(store.categoryTree).toHaveLength(1)
  })

  it('FE-02 browse by folder loads filtered documents from the backend', async () => {
    const store = useKnowledgeStore()
    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(1, 'COMPLETED')]))
    await store.load()
    expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(null, null, true)

    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(2, 'COMPLETED')]))
    store.browseBy('category', 3)
    await vi.waitFor(() => expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true))
    expect(store.documents.map((d) => d.id)).toEqual([2])

    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(1, 'COMPLETED')]))
    store.browseAll()
    await vi.waitFor(() => expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(null, null, true))
  })

  it('FE-02 categorize failure keeps created document and shows a local warning', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.getDocumentClassification.mockResolvedValue(ok({ category: null, tags: [] }))
    await store.loadClassification(1)
    api.setDocumentCategory.mockRejectedValue(new Error('设置分类失败'))

    const result = await store.categorizeCreatedDocument(1, 5)
    expect(result).toBe(false)
    expect(store.categorizeWarning).toContain('归档')
    // 文档保持存在且未归档（不提前改计数）
    expect(store.documents).toHaveLength(1)
    expect(api.listKnowledgeCategoryTree).not.toHaveBeenCalled()
  })

  it('FE-02 categorize success refreshes the tree counts', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.getDocumentClassification.mockResolvedValue(ok({ category: null, tags: [] }))
    await store.loadClassification(1)
    api.setDocumentCategory.mockResolvedValue(ok(null))
    api.getDocumentClassification.mockResolvedValue(ok({ category: { id: 5, name: '文件夹', normalizedName: '文件夹', parentId: null, createdAt: 't', updatedAt: 't' }, tags: [] }))
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([]))

    const result = await store.categorizeCreatedDocument(1, 5)
    expect(result).toBe(true)
    expect(api.listKnowledgeCategoryTree).toHaveBeenCalled()
    expect(store.categorizeWarning).toBe('')
  })

  it('FE-02 discards stale browse responses when switching folders quickly', async () => {
    const store = useKnowledgeStore()
    let resolveA: (v: { success: true; data: never[]; message: null }) => void = () => {}
    const slowA = new Promise<{ success: true; data: never[]; message: null }>((resolve) => { resolveA = resolve })
    // 第一次浏览 A：慢响应；第二次浏览 B：快响应
    api.listKnowledgeDocuments.mockImplementationOnce(() => slowA)
      .mockResolvedValueOnce(ok([doc(9, 'COMPLETED', 'B 文件夹资料')]))

    store.browseBy('category', 1)  // A：慢
    store.browseBy('category', 2)  // B：快
    await vi.waitFor(() => expect(store.documents.map((d) => d.id)).toEqual([9]))

    // 迟到的 A 响应不得覆盖 B 的结果
    resolveA({ success: true, data: [], message: null })
    await vi.waitFor(() => expect(store.loading).toBe(false))
    expect(store.documents.map((d) => d.id)).toEqual([9])
  })

  it('FE-02 save success with metadata refresh failure is not reported as save failure', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'NOT_STARTED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.saveKnowledgeNoteContent.mockResolvedValue(ok({ documentId: 1, content: '已保存' }))
    api.getKnowledgeDocument.mockRejectedValue(new Error('网络错误'))

    await expect(store.saveNoteContent(1, '已保存')).resolves.toBeUndefined()
    expect(store.contentByDocumentId[1]).toBe('已保存')
    expect(store.noteSaveErrorsByDocumentId[1]).toBeUndefined()
    expect(store.noteMetadataWarningsByDocumentId[1]).toContain('已保存，元数据刷新失败')
  })

  it('FE-02 save PUT failure is still reported as save failure', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'NOT_STARTED')]))
    const store = useKnowledgeStore()
    await store.load()
    api.saveKnowledgeNoteContent.mockRejectedValue(new Error('保存失败'))
    await expect(store.saveNoteContent(1, 'x')).rejects.toThrow('保存失败')
    expect(store.noteSaveErrorsByDocumentId[1]).toContain('保存失败')
    expect(store.noteMetadataWarningsByDocumentId[1]).toBeUndefined()
  })

  it('FE-02 deleting the active browse folder returns to all documents', async () => {
    const store = useKnowledgeStore()
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    store.browseBy('category', 3)
    await vi.waitFor(() => expect(store.browseCategoryId).toBe(3))
    api.deleteKnowledgeCategory.mockResolvedValue(ok(null))
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([]))

    await store.deleteCategoryNode(3)

    expect(store.browseCategoryId).toBeNull()
    expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(null, null, true)
  })

  it('FE-02 import within a folder browse keeps the scoped list', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    store.browseBy('category', 3)
    await vi.waitFor(() => expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true))

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(9, 'COMPLETED', '新导入')))
    api.getDocumentClassification.mockResolvedValue(ok({
      category: { id: 3, name: '文件夹', normalizedName: '文件夹', parentId: null, createdAt: 't', updatedAt: 't' },
      tags: [],
    }))
    api.setDocumentCategory.mockResolvedValue(ok(null))
    api.listKnowledgeCategoryTree.mockResolvedValue(ok([]))
    // 归档后的列表刷新仍保持 browse filter
    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([doc(9, 'COMPLETED', '新导入')]))

    await store.importFile(file(), 3)

    expect(api.setDocumentCategory).toHaveBeenCalledWith(9, 3)
    expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true)
    expect(store.documents.map((d) => d.id)).toEqual([9])
    expect(store.documents.map((d) => d.title)).not.toContain('文档 1')
  })

  it('FE-02 import categorize failure keeps scoped list with a warning', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()
    store.browseBy('category', 3)
    await vi.waitFor(() => expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true))

    api.importKnowledgeFile.mockResolvedValue(ok({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    }))
    api.getKnowledgeDocument.mockResolvedValue(ok(doc(9, 'COMPLETED')))
    api.getDocumentClassification.mockResolvedValue(ok({ category: null, tags: [] }))
    api.setDocumentCategory.mockRejectedValue(new Error('设置分类失败'))
    api.listKnowledgeDocuments.mockResolvedValueOnce(ok([]))

    await store.importFile(file(), 3)

    expect(store.categorizeWarning).toContain('归档')
    expect(store.importErrorMessage).toBe('')
    expect(api.listKnowledgeDocuments).toHaveBeenLastCalledWith(3, null, true)
  })

  it('FE-02 note metadata warning is isolated per document', async () => {
    api.listKnowledgeDocuments.mockResolvedValue(ok([doc(1, 'COMPLETED'), doc(2, 'COMPLETED')]))
    const store = useKnowledgeStore()
    await store.load()

    api.saveKnowledgeNoteContent.mockResolvedValue(ok({ documentId: 1, content: '正文' }))
    api.getKnowledgeDocument.mockRejectedValueOnce(new Error('网络错误'))
    await store.saveNoteContent(1, '正文')

    expect(store.noteMetadataWarningsByDocumentId[1]).toContain('已保存，元数据刷新失败')
    expect(store.noteMetadataWarningsByDocumentId[2]).toBeUndefined()
  })
})
