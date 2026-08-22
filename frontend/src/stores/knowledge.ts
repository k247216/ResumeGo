import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
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
} from '../api/knowledge'
import type {
  KnowledgeCategory,
  KnowledgeDocument,
  KnowledgeDocumentClassification,
  KnowledgeImportResponse,
  KnowledgeSearchItem,
  KnowledgeTag,
} from '../types/knowledge'

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
  // 分类/标签目录
  const categories = ref<KnowledgeCategory[]>([])
  const tags = ref<KnowledgeTag[]>([])
  const catalogLoading = ref(false)
  const catalogErrorMessage = ref('')
  // 文档现有关联（后端为准，不乐观伪造）
  const classificationByDocumentId = ref<Record<number, KnowledgeDocumentClassification>>({})
  const classificationLoadingDocumentId = ref<number | null>(null)
  const classificationErrorsByDocumentId = ref<Record<number, string>>({})
  const classificationSaving = ref(false)
  // 搜索状态（递增 sequence 丢弃过期响应）
  const searchQuery = ref('')
  const searchCategoryId = ref<number | null>(null)
  const searchTagId = ref<number | null>(null)
  const searchResults = ref<KnowledgeSearchItem[]>([])
  const searchLoading = ref(false)
  const searchErrorMessage = ref('')
  let searchRequestSequence = 0

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


  // ---- 分类/标签目录 ----

  async function loadCatalog() {
    catalogLoading.value = true
    catalogErrorMessage.value = ''
    try {
      const [cats, tg] = await Promise.all([listKnowledgeCategories(), listKnowledgeTags()])
      categories.value = cats.data
      tags.value = tg.data
    } catch (error) {
      catalogErrorMessage.value = error instanceof Error ? error.message : '加载分类标签失败'
      throw error
    } finally {
      catalogLoading.value = false
    }
  }

  function upsertCategory(category: KnowledgeCategory) {
    const index = categories.value.findIndex((c) => c.id === category.id)
    if (index >= 0) categories.value[index] = category
    else categories.value.push(category)
  }

  function upsertTag(tag: KnowledgeTag) {
    const index = tags.value.findIndex((t) => t.id === tag.id)
    if (index >= 0) tags.value[index] = tag
    else tags.value.push(tag)
  }

  async function createCategory(name: string): Promise<KnowledgeCategory> {
    catalogErrorMessage.value = ''
    try {
      const response = await createKnowledgeCategory(name)
      upsertCategory(response.data)
      return response.data
    } catch (error) {
      catalogErrorMessage.value = error instanceof Error ? error.message : '创建分类失败'
      throw error
    }
  }

  async function createTag(name: string): Promise<KnowledgeTag> {
    catalogErrorMessage.value = ''
    try {
      const response = await createKnowledgeTag(name)
      upsertTag(response.data)
      return response.data
    } catch (error) {
      catalogErrorMessage.value = error instanceof Error ? error.message : '创建标签失败'
      throw error
    }
  }

  // ---- 文档关联（写入成功后再回读服务端状态） ----

  /** 选择文档后按需读取关联；已有缓存不重复请求，失败只写该文档错误。 */
  async function loadClassification(documentId: number) {
    if (classificationByDocumentId.value[documentId]) return
    if (classificationLoadingDocumentId.value === documentId) return
    classificationLoadingDocumentId.value = documentId
    delete classificationErrorsByDocumentId.value[documentId]
    try {
      const response = await getDocumentClassification(documentId)
      classificationByDocumentId.value = { ...classificationByDocumentId.value, [documentId]: response.data }
    } catch (error) {
      classificationErrorsByDocumentId.value = {
        ...classificationErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '读取文档关联失败',
      }
      throw error
    } finally {
      classificationLoadingDocumentId.value = null
    }
  }

  async function refreshClassification(documentId: number) {
    const response = await getDocumentClassification(documentId)
    classificationByDocumentId.value = { ...classificationByDocumentId.value, [documentId]: response.data }
  }

  /** 设置分类（null 表示无分类）；写入成功后回读，失败保留旧值。错误按文档隔离。 */
  async function setCategory(documentId: number, categoryId: number | null) {
    classificationSaving.value = true
    delete classificationErrorsByDocumentId.value[documentId]
    try {
      if (categoryId == null) {
        const current = classificationByDocumentId.value[documentId]?.category
        if (current) {
          await removeDocumentCategory(documentId, current.id)
        }
      } else {
        await setDocumentCategory(documentId, categoryId)
      }
      await refreshClassification(documentId)
    } catch (error) {
      classificationErrorsByDocumentId.value = {
        ...classificationErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '更新分类失败',
      }
      throw error
    } finally {
      classificationSaving.value = false
    }
  }

  /** 添加/移除标签；写入成功后回读，失败保留旧值。错误按文档隔离。 */
  async function toggleTag(documentId: number, tagId: number, add: boolean) {
    classificationSaving.value = true
    delete classificationErrorsByDocumentId.value[documentId]
    try {
      if (add) await addDocumentTag(documentId, tagId)
      else await removeDocumentTag(documentId, tagId)
      await refreshClassification(documentId)
    } catch (error) {
      classificationErrorsByDocumentId.value = {
        ...classificationErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '更新标签失败',
      }
      throw error
    } finally {
      classificationSaving.value = false
    }
  }

  // ---- 搜索（递增 sequence 丢弃过期响应） ----

  async function runSearch() {
    const query = searchQuery.value.trim()
    if (!query) {
      searchRequestSequence++
      searchResults.value = []
      searchErrorMessage.value = ''
      searchLoading.value = false
      return
    }
    const sequence = ++searchRequestSequence
    searchLoading.value = true
    searchErrorMessage.value = ''
    try {
      const response = await searchKnowledge(query, searchCategoryId.value, searchTagId.value)
      if (sequence !== searchRequestSequence) return
      searchResults.value = response.data
    } catch (error) {
      if (sequence !== searchRequestSequence) return
      searchErrorMessage.value = error instanceof Error ? error.message : '搜索失败'
    } finally {
      if (sequence === searchRequestSequence) searchLoading.value = false
    }
  }

  function setSearchQuery(query: string) {
    searchQuery.value = query
    void runSearch()
  }

  /** filter 失效时清空对应 filter；改变后重新搜索。 */
  function setSearchFilter(kind: 'category' | 'tag', id: number | null) {
    if (kind === 'category') {
      if (id != null && !categories.value.some((c) => c.id === id)) {
        searchCategoryId.value = null
      } else {
        searchCategoryId.value = id
      }
    } else {
      if (id != null && !tags.value.some((t) => t.id === id)) {
        searchTagId.value = null
      } else {
        searchTagId.value = id
      }
    }
    void runSearch()
  }

  function clearSearch() {
    searchRequestSequence++
    searchQuery.value = ''
    searchCategoryId.value = null
    searchTagId.value = null
    searchResults.value = []
    searchErrorMessage.value = ''
    searchLoading.value = false
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
    categories,
    tags,
    catalogLoading,
    catalogErrorMessage,
    classificationByDocumentId,
    classificationLoadingDocumentId,
    classificationErrorsByDocumentId,
    classificationSaving,
    searchQuery,
    searchCategoryId,
    searchTagId,
    searchResults,
    searchLoading,
    searchErrorMessage,
    load,
    retry,
    select,
    createNote,
    importFile,
    loadContent,
    loadCatalog,
    createCategory,
    createTag,
    loadClassification,
    setCategory,
    toggleTag,
    runSearch,
    setSearchQuery,
    setSearchFilter,
    clearSearch,
  }
})
