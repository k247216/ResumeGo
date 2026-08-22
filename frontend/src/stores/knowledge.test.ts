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
})
