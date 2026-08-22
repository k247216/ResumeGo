// @vitest-environment happy-dom

import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  addDocumentTag,
  createKnowledgeCategory,
  createKnowledgeNote,
  createKnowledgeTag,
  getDocumentClassification,
  getKnowledgeContent,
  getKnowledgeDocument,
  importKnowledgeFile,
  KnowledgeHttpError,
  listKnowledgeCategories,
  listKnowledgeDocuments,
  listKnowledgeTags,
  removeDocumentCategory,
  removeDocumentTag,
  searchKnowledge,
  setDocumentCategory,
} from './knowledge'
import type { KnowledgeDocument, KnowledgeImportResponse } from '../types/knowledge'

vi.mock('./http', () => {
  const apiFetch = vi.fn()
  return { apiFetch }
})

import { apiFetch } from './http'

const mockedFetch = vi.mocked(apiFetch)

function okResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify({ success: true, data, message: null }), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function errResponse(status: number, message: string): Response {
  return new Response(JSON.stringify({ success: false, data: null, message }), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const doc = (id: number, status: KnowledgeDocument['processingStatus']): KnowledgeDocument => ({
  id,
  title: '笔记 ' + id,
  sourceType: 'NOTE',
  processingStatus: status,
  sourceFile: null,
  createdAt: '2026-08-22T10:00:00',
  updatedAt: '2026-08-22T10:00:00',
})

describe('knowledge api client', () => {
  beforeEach(() => {
    mockedFetch.mockReset()
  })

  it('FE-00 lists knowledge documents with a typed GET', async () => {
    mockedFetch.mockResolvedValue(okResponse([doc(1, 'COMPLETED')]))
    const result = await listKnowledgeDocuments()
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/documents')
    expect(result.data[0].title).toBe('笔记 1')
  })

  it('FE-00 gets a document detail', async () => {
    mockedFetch.mockResolvedValue(okResponse(doc(7, 'PENDING')))
    const result = await getKnowledgeDocument(7)
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/documents/7')
    expect(result.data.id).toBe(7)
  })

  it('FE-00 creates a NOTE with title and sourceType NOTE', async () => {
    mockedFetch.mockResolvedValue(okResponse(doc(5, 'NOT_STARTED')))
    await createKnowledgeNote('TensorFlow 学习笔记')
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/knowledge/documents')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ title: 'TensorFlow 学习笔记', sourceType: 'NOTE' })
  })

  it('FE-00 imports a file as multipart with the single "file" field and no content-type header', async () => {
    mockedFetch.mockResolvedValue(okResponse({
      documentId: 9, sourceType: 'FILE', processingStatus: 'COMPLETED', duplicate: false, errorCode: null,
    } satisfies KnowledgeImportResponse))
    const file = new File(['# 内容'], 'notes.md', { type: 'text/markdown' })
    await importKnowledgeFile(file)
    const [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/knowledge/imports')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBeInstanceOf(FormData)
    const form = init?.body as FormData
    expect(form.get('file')).toBe(file)
    // 不手动设置 Content-Type，交给浏览器生成 multipart boundary
    expect(init?.headers).toBeUndefined()
  })

  it('FE-00 loads content on demand from the content endpoint', async () => {
    mockedFetch.mockResolvedValue(okResponse({ documentId: 9, content: '提取正文' }))
    const result = await getKnowledgeContent(9)
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/documents/9/content')
    expect(result.data.content).toBe('提取正文')
  })

  it('FE-00 surfaces server error messages', async () => {
    mockedFetch.mockResolvedValue(errResponse(400, '标题不能为空'))
    await expect(createKnowledgeNote('')).rejects.toThrow('标题不能为空')
  })

  it('FE-00 maps 409 and 404 to KnowledgeHttpError with status', async () => {
    mockedFetch.mockResolvedValue(errResponse(409, '知识文档尚未完成文本提取'))
    await expect(getKnowledgeContent(9)).rejects.toMatchObject({
      name: 'KnowledgeHttpError',
      status: 409,
    })

    mockedFetch.mockResolvedValue(errResponse(404, '知识文档不存在'))
    await expect(getKnowledgeContent(9)).rejects.toMatchObject({
      name: 'KnowledgeHttpError',
      status: 404,
    })
  })

  it('FE-00 maps non-ok responses without a JSON body to the fallback message', async () => {
    mockedFetch.mockResolvedValue(new Response('boom', { status: 500 }))
    const error = await getKnowledgeContent(9).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(KnowledgeHttpError)
    expect((error as KnowledgeHttpError).message).toBe('读取提取内容失败')
  })

  it('FE-01 lists and creates categories with exact paths and body', async () => {
    mockedFetch.mockImplementation(() => Promise.resolve(okResponse({ id: 1, name: '求职', normalizedName: '求职', createdAt: 't', updatedAt: 't' })))
    await listKnowledgeCategories()
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/categories')

    await createKnowledgeCategory('求职')
    const [url, init] = mockedFetch.mock.calls[1]!
    expect(url).toBe('/api/v2/knowledge/categories')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ name: '求职', parentId: null })
  })

  it('FE-01 lists and creates tags with exact paths and body', async () => {
    mockedFetch.mockImplementation(() => Promise.resolve(okResponse({ id: 2, name: '机器学习', normalizedName: '机器学习', createdAt: 't', updatedAt: 't' })))
    await listKnowledgeTags()
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/tags')

    await createKnowledgeTag('机器学习')
    const [url, init] = mockedFetch.mock.calls[1]!
    expect(url).toBe('/api/v2/knowledge/tags')
    expect(init?.method).toBe('POST')
    expect(JSON.parse((init?.body ?? '') as string)).toEqual({ name: '机器学习' })
  })

  it('FE-01 reads document classification from the frozen path', async () => {
    mockedFetch.mockResolvedValue(okResponse({ category: null, tags: [] }))
    const result = await getDocumentClassification(9)
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/documents/9/classification')
    expect(result.data.category).toBeNull()
    expect(result.data.tags).toEqual([])
  })

  it('FE-01 sets and removes document category with PUT/DELETE', async () => {
    mockedFetch.mockImplementation(() => Promise.resolve(okResponse(null)))
    await setDocumentCategory(9, 3)
    let [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/knowledge/documents/9/category/3')
    expect(init?.method).toBe('PUT')

    await removeDocumentCategory(9, 3)
    ;[url, init] = mockedFetch.mock.calls[1]!
    expect(url).toBe('/api/v2/knowledge/documents/9/category/3')
    expect(init?.method).toBe('DELETE')
  })

  it('FE-01 adds and removes document tags with PUT/DELETE', async () => {
    mockedFetch.mockImplementation(() => Promise.resolve(okResponse(null)))
    await addDocumentTag(9, 5)
    let [url, init] = mockedFetch.mock.calls[0]!
    expect(url).toBe('/api/v2/knowledge/documents/9/tags/5')
    expect(init?.method).toBe('PUT')

    await removeDocumentTag(9, 5)
    ;[url, init] = mockedFetch.mock.calls[1]!
    expect(url).toBe('/api/v2/knowledge/documents/9/tags/5')
    expect(init?.method).toBe('DELETE')
  })

  it('FE-01 searches with query and optional filters', async () => {
    mockedFetch.mockImplementation(() => Promise.resolve(okResponse([])))
    await searchKnowledge('笔记')
    expect(mockedFetch).toHaveBeenCalledWith('/api/v2/knowledge/search?q=%E7%AC%94%E8%AE%B0')

    await searchKnowledge('笔记', 3, 5)
    const [url] = mockedFetch.mock.calls[1]!
    expect(url).toBe('/api/v2/knowledge/search?q=%E7%AC%94%E8%AE%B0&categoryId=3&tagId=5&includeDescendants=true')

    await searchKnowledge('x', null, null)
    const [url3] = mockedFetch.mock.calls[2]!
    expect(url3).toBe('/api/v2/knowledge/search?q=x')
  })

  it('FE-01 surfaces ownership errors from classification reads', async () => {
    mockedFetch.mockResolvedValue(errResponse(404, '知识文档不存在'))
    const error = await getDocumentClassification(404).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(KnowledgeHttpError)
    expect((error as KnowledgeHttpError).status).toBe(404)
    expect((error as KnowledgeHttpError).message).toBe('知识文档不存在')
  })
})
