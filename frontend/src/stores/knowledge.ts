import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createKnowledgeNote,
  getKnowledgeContent,
  getKnowledgeDocument,
  importKnowledgeFile,
  KnowledgeHttpError,
  listKnowledgeDocuments,
} from '../api/knowledge'
import type { KnowledgeDocument, KnowledgeImportResponse } from '../types/knowledge'

/**
 * 知识库 store：列表/选择/笔记创建/文件导入/正文按需加载。
 * 失败只影响对应区域：load 失败保留旧列表，import 失败只写 importErrorMessage，
 * content 失败只写该文档的 contentError，均不伪造成功。
 */
export const useKnowledgeStore = defineStore('knowledge', () => {
  const documents = ref<KnowledgeDocument[]>([])
  const selectedDocumentId = ref<number | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')
  const creating = ref(false)
  const importing = ref(false)
  const importErrorMessage = ref('')
  const listRefreshError = ref('')
  const contentByDocumentId = ref<Record<number, string>>({})
  const contentLoadingDocumentId = ref<number | null>(null)
  const contentErrorsByDocumentId = ref<Record<number, string>>({})

  const selectedDocument = computed(() => (
    documents.value.find((doc) => doc.id === selectedDocumentId.value) ?? null
  ))

  function chooseSelection() {
    if (selectedDocumentId.value != null && documents.value.some((d) => d.id === selectedDocumentId.value)) {
      return
    }
    selectedDocumentId.value = documents.value[0]?.id ?? null
  }

  function upsertDocument(doc: KnowledgeDocument) {
    const index = documents.value.findIndex((d) => d.id === doc.id)
    if (index >= 0) {
      documents.value[index] = doc
    } else {
      documents.value.push(doc)
    }
    documents.value.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || b.id - a.id)
  }

  async function load() {
    loading.value = true
    errorMessage.value = ''
    listRefreshError.value = ''
    try {
      const response = await listKnowledgeDocuments()
      documents.value = response.data
      chooseSelection()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '加载知识库失败'
      throw error
    } finally {
      loading.value = false
    }
  }

  function retry() {
    return load()
  }

  function select(id: number) {
    if (!documents.value.some((doc) => doc.id === id)) return
    selectedDocumentId.value = id
  }

  async function createNote(title: string) {
    creating.value = true
    errorMessage.value = ''
    try {
      const response = await createKnowledgeNote(title)
      upsertDocument(response.data)
      selectedDocumentId.value = response.data.id
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '创建笔记失败'
      throw error
    } finally {
      creating.value = false
    }
  }

  /**
   * 导入：上传结果与刷新结果分离。
   * 上传成功后绝不改写为“导入失败”；detail 获取目标文档并 upsert/select，
   * 列表刷新失败仅显示“已导入，列表刷新失败，可重试刷新”。
   */
  async function importFile(file: File): Promise<KnowledgeImportResponse> {
    importing.value = true
    importErrorMessage.value = ''
    listRefreshError.value = ''
    let imported: KnowledgeImportResponse
    try {
      imported = (await importKnowledgeFile(file)).data
    } catch (error) {
      importErrorMessage.value = error instanceof Error ? error.message : '导入文件失败'
      throw error
    } finally {
      importing.value = false
    }
    try {
      const detail = await getKnowledgeDocument(imported.documentId)
      upsertDocument(detail.data)
      selectedDocumentId.value = detail.data.id
    } catch {
      listRefreshError.value = '已导入，列表刷新失败，可重试刷新'
      return imported
    }
    try {
      const response = await listKnowledgeDocuments()
      documents.value = response.data
    } catch {
      listRefreshError.value = '已导入，列表刷新失败，可重试刷新'
    }
    return imported
  }

  /** COMPLETED 按需加载正文；409 显示处理中，404 显示不可用；已有正文不重复请求。 */
  async function loadContent(documentId: number) {
    const doc = documents.value.find((d) => d.id === documentId)
    if (!doc || doc.processingStatus !== 'COMPLETED') return
    if (contentByDocumentId.value[documentId] !== undefined) return
    if (contentLoadingDocumentId.value === documentId) return
    contentLoadingDocumentId.value = documentId
    delete contentErrorsByDocumentId.value[documentId]
    try {
      const response = await getKnowledgeContent(documentId)
      contentByDocumentId.value = { ...contentByDocumentId.value, [documentId]: response.data.content }
    } catch (error) {
      contentErrorsByDocumentId.value = {
        ...contentErrorsByDocumentId.value,
        [documentId]: contentErrorMessage(error),
      }
      throw error
    } finally {
      contentLoadingDocumentId.value = null
    }
  }

  function contentErrorMessage(error: unknown): string {
    if (error instanceof KnowledgeHttpError) {
      if (error.status === 409) return '内容仍在处理中，请稍后重试'
      if (error.status === 404) return '内容暂不可用'
    }
    return error instanceof Error ? error.message : '读取内容失败'
  }

  return {
    documents,
    selectedDocumentId,
    selectedDocument,
    loading,
    errorMessage,
    creating,
    importing,
    importErrorMessage,
    listRefreshError,
    contentByDocumentId,
    contentLoadingDocumentId,
    contentErrorsByDocumentId,
    load,
    retry,
    select,
    createNote,
    importFile,
    loadContent,
  }
})
