import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addDocumentTag,
  createKnowledgeCategory,
  createKnowledgeNote,
  createKnowledgeTag,
  deleteKnowledgeCategory,
  deleteKnowledgeDocument,
  getDocumentClassification,
  getKnowledgeContent,
  getKnowledgeDeletionImpact,
  getKnowledgeDocument,
  importKnowledgeFile,
  KnowledgeHttpError,
  listKnowledgeCategories,
  listKnowledgeCategoryTree,
  listKnowledgeDocuments,
  listKnowledgeTags,
  removeDocumentCategory,
  removeDocumentTag,
  renameKnowledgeDocument,
  retryKnowledgeDocument,
  saveKnowledgeNoteContent,
  searchKnowledge,
  setDocumentCategory,
  updateKnowledgeCategory,
} from '../api/knowledge'
import type {
  KnowledgeCategory,
  KnowledgeCategoryNode,
  KnowledgeDeletionImpact,
  KnowledgeDocument,
  KnowledgeDocumentClassification,
  KnowledgeImportResponse,
  KnowledgeSearchItem,
  KnowledgeTag,
} from '../types/knowledge'
import { openKnowledgeSource, revealKnowledgeSource } from '../api/knowledgeDesktop'

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
  let browseRequestSequence = 0
  // 层级资料库（BE-04 树节点）与 includeDescendants
  const categoryTree = ref<KnowledgeCategoryNode[]>([])
  const categoryTreeLoading = ref(false)
  const categoryTreeError = ref('')
  const searchIncludeDescendants = ref(true)
  // 浏览（无关键词时资料列表的文件夹/标签过滤）
  const browseCategoryId = ref<number | null>(null)
  const browseTagId = ref<number | null>(null)
  const browseIncludeDescendants = ref(true)
  const categorizeWarning = ref('')
  // 重试 / 删除 / NOTE 保存 / 来源操作
  const retryingDocumentId = ref<number | null>(null)
  const retryErrorsByDocumentId = ref<Record<number, string>>({})
  const deletionImpactByDocumentId = ref<Record<number, KnowledgeDeletionImpact>>({})
  const deletingDocumentId = ref<number | null>(null)
  const deleteErrorsByDocumentId = ref<Record<number, string>>({})
  const noteSavingDocumentId = ref<number | null>(null)
  const noteSaveErrorsByDocumentId = ref<Record<number, string>>({})
  const noteMetadataWarningsByDocumentId = ref<Record<number, string>>({})
  const titleErrorsByDocumentId = ref<Record<number, string>>({})

  const selectedDocument = computed(() => (
    documents.value.find((doc) => doc.id === selectedDocumentId.value)
    ?? searchResults.value.find((item) => item.document.id === selectedDocumentId.value)?.document
    ?? null
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

  /** 递增序列丢弃过期浏览响应：快速切换文件夹时较慢的旧响应不得覆盖最新列表。 */
  async function load() {
    const sequence = ++browseRequestSequence
    loading.value = true
    errorMessage.value = ''
    listRefreshError.value = ''
    try {
      const response = await listKnowledgeDocuments(
        browseCategoryId.value, browseTagId.value, browseIncludeDescendants.value)
      if (sequence !== browseRequestSequence) return
      documents.value = response.data
      chooseSelection()
    } catch (error) {
      if (sequence !== browseRequestSequence) return
      errorMessage.value = error instanceof Error ? error.message : '加载知识库失败'
      throw error
    } finally {
      if (sequence === browseRequestSequence) loading.value = false
    }
  }

  /** 无关键词浏览：按文件夹（含后代）或标签过滤，由后端真实关联驱动。 */
  function browseBy(kind: 'category' | 'tag', id: number | null) {
    if (kind === 'category') {
      browseCategoryId.value = id
      browseTagId.value = null
    } else {
      browseTagId.value = id
      browseCategoryId.value = null
    }
    void load().catch(() => undefined)
  }

  function browseAll() {
    browseCategoryId.value = null
    browseTagId.value = null
    void load().catch(() => undefined)
  }

  function retry() {
    return load()
  }

  function select(id: number) {
    if (!documents.value.some((doc) => doc.id === id)
      && !searchResults.value.some((item) => item.document.id === id)) return
    selectedDocumentId.value = id
  }

  async function createNote(title: string): Promise<KnowledgeDocument> {
    creating.value = true
    errorMessage.value = ''
    categorizeWarning.value = ''
    try {
      const response = await createKnowledgeNote(title)
      upsertDocument(response.data)
      selectedDocumentId.value = response.data.id
      return response.data
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '创建笔记失败'
      throw error
    } finally {
      creating.value = false
    }
  }

  function createUntitledNote(): Promise<KnowledgeDocument> {
    return createNote('未命名笔记')
  }

  async function renameDocument(documentId: number, title: string): Promise<KnowledgeDocument> {
    delete titleErrorsByDocumentId.value[documentId]
    try {
      const response = await renameKnowledgeDocument(documentId, title)
      upsertDocument(response.data)
      return response.data
    } catch (error) {
      titleErrorsByDocumentId.value = {
        ...titleErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '修改资料标题失败',
      }
      throw error
    }
  }

  /** 创建/导入后的归档尝试：成功刷新计数；失败保留真实未归档状态并给出局部警告。 */
  async function categorizeCreatedDocument(documentId: number, categoryId: number | null): Promise<boolean> {
    if (categoryId == null) return true
    try {
      await setCategory(documentId, categoryId)
      await loadCategoryTree().catch(() => undefined)
      return true
    } catch {
      categorizeWarning.value = '资料已创建/导入，但归档到当前文件夹失败，可在检查器中重新设置'
      return false
    }
  }

  /**
   * 导入：上传结果与刷新结果分离；归档后保持当前浏览范围（调 load() 带 browse filter + sequence）。
   * 归档失败显示“未归档”警告并保持真实归属，绝不显示其他文件夹文档。
   */
  async function importFile(file: File, categoryId: number | null = null): Promise<KnowledgeImportResponse> {
    importing.value = true
    importErrorMessage.value = ''
    listRefreshError.value = ''
    categorizeWarning.value = ''
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
    if (categoryId != null) {
      await categorizeCreatedDocument(imported.documentId, categoryId)
    }
    // 保持当前浏览范围刷新（带 browse filter + sequence），不是全量替换
    await load().catch(() => {
      listRefreshError.value = '已导入，列表刷新失败，可重试刷新'
    })
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
      void loadCategoryTree().catch(() => undefined)
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

  function setIncludeDescendants(value: boolean) {
    searchIncludeDescendants.value = value
    void runSearch()
  }

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
      const response = await searchKnowledge(query, searchCategoryId.value, searchTagId.value, searchIncludeDescendants.value)
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
    const startsSearch = !searchQuery.value.trim() && query.trim()
    if (startsSearch) {
      searchCategoryId.value = browseCategoryId.value
      searchTagId.value = browseTagId.value
    }
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


  // ---- 层级资料库树（BE-04） ----

  async function loadCategoryTree() {
    categoryTreeLoading.value = true
    categoryTreeError.value = ''
    try {
      const response = await listKnowledgeCategoryTree()
      categoryTree.value = response.data
    } catch (error) {
      categoryTreeError.value = error instanceof Error ? error.message : '加载资料库失败'
      throw error
    } finally {
      categoryTreeLoading.value = false
    }
  }

  async function createCategoryNode(name: string, parentId: number | null = null) {
    categoryTreeError.value = ''
    try {
      await createKnowledgeCategory(name, parentId)
      await loadCategoryTree()
    } catch (error) {
      categoryTreeError.value = error instanceof Error ? error.message : '创建分类失败'
      throw error
    }
  }

  async function updateCategoryNode(id: number, name: string, parentId: number | null) {
    categoryTreeError.value = ''
    try {
      await updateKnowledgeCategory(id, { name, parentId })
      await loadCategoryTree()
      // 树变化后刷新当前筛选下的列表
      if (searchCategoryId.value != null) void runSearch()
    } catch (error) {
      categoryTreeError.value = error instanceof Error ? error.message : '更新分类失败'
      throw error
    }
  }

  async function deleteCategoryNode(id: number) {
    categoryTreeError.value = ''
    try {
      await deleteKnowledgeCategory(id)
      await loadCategoryTree()
      if (browseCategoryId.value === id) {
        browseCategoryId.value = null
        await load()
      }
      if (searchCategoryId.value === id) {
        searchCategoryId.value = null
        void runSearch()
      }
    } catch (error) {
      categoryTreeError.value = error instanceof Error ? error.message : '删除分类失败'
      throw error
    }
  }

  // ---- 重试（BE-03） ----

  async function retryDocument(documentId: number) {
    retryingDocumentId.value = documentId
    delete retryErrorsByDocumentId.value[documentId]
    try {
      const response = await retryKnowledgeDocument(documentId)
      upsertDocument(response.data)
      // 成功后刷新正文缓存
      if (response.data.processingStatus === 'COMPLETED') {
        const next = { ...contentByDocumentId.value }
        delete next[documentId]
        contentByDocumentId.value = next
        void loadContent(documentId).catch(() => undefined)
      }
    } catch (error) {
      retryErrorsByDocumentId.value = {
        ...retryErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '重试失败',
      }
      throw error
    } finally {
      retryingDocumentId.value = null
    }
  }

  // ---- 删除（BE-03） ----

  async function loadDeletionImpact(documentId: number): Promise<KnowledgeDeletionImpact> {
    delete deleteErrorsByDocumentId.value[documentId]
    try {
      const response = await getKnowledgeDeletionImpact(documentId)
      deletionImpactByDocumentId.value = { ...deletionImpactByDocumentId.value, [documentId]: response.data }
      return response.data
    } catch (error) {
      deleteErrorsByDocumentId.value = {
        ...deleteErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '读取删除影响失败',
      }
      throw error
    }
  }

  /** 删除成功：从列表/搜索结果/详情同时移除，保持其他文档与选择不受影响。 */
  async function deleteDocument(documentId: number, confirmationToken: string) {
    deletingDocumentId.value = documentId
    delete deleteErrorsByDocumentId.value[documentId]
    try {
      await deleteKnowledgeDocument(documentId, confirmationToken)
      documents.value = documents.value.filter((d) => d.id !== documentId)
      searchResults.value = searchResults.value.filter((r) => r.document.id !== documentId)
      if (selectedDocumentId.value === documentId) {
        selectedDocumentId.value = documents.value[0]?.id ?? null
      }
      delete contentByDocumentId.value[documentId]
      delete classificationByDocumentId.value[documentId]
      delete deletionImpactByDocumentId.value[documentId]
      delete deleteErrorsByDocumentId.value[documentId]
      await loadCategoryTree().catch(() => undefined)
    } catch (error) {
      deleteErrorsByDocumentId.value = {
        ...deleteErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '删除资料失败',
      }
      throw error
    } finally {
      deletingDocumentId.value = null
    }
  }

  // ---- NOTE 正文保存（BE-05） ----

  /**
   * 明确保存 NOTE 正文：PUT 成功即返回成功并更新正文缓存；
   * 元数据回读失败只给局部提示，绝不把已持久化成功误报为保存失败。
   */
  async function saveNoteContent(documentId: number, content: string) {
    noteSavingDocumentId.value = documentId
    delete noteSaveErrorsByDocumentId.value[documentId]
    delete noteMetadataWarningsByDocumentId.value[documentId]
    try {
      await saveKnowledgeNoteContent(documentId, content)
      contentByDocumentId.value = { ...contentByDocumentId.value, [documentId]: content }
    } catch (error) {
      noteSaveErrorsByDocumentId.value = {
        ...noteSaveErrorsByDocumentId.value,
        [documentId]: error instanceof Error ? error.message : '保存笔记正文失败',
      }
      throw error
    } finally {
      noteSavingDocumentId.value = null
    }
    try {
      const detail = await getKnowledgeDocument(documentId)
      upsertDocument(detail.data)
    } catch {
      noteMetadataWarningsByDocumentId.value = {
        ...noteMetadataWarningsByDocumentId.value,
        [documentId]: '已保存，元数据刷新失败，可重试',
      }
    }
  }

  // ---- 受管原文（IO-02） ----

  async function openSource(documentId: number) {
    return openKnowledgeSource(documentId)
  }

  async function revealSource(documentId: number) {
    return revealKnowledgeSource(documentId)
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
    searchIncludeDescendants,
    searchResults,
    searchLoading,
    searchErrorMessage,
    categoryTree,
    categoryTreeLoading,
    categoryTreeError,
    browseCategoryId,
    browseTagId,
    browseIncludeDescendants,
    categorizeWarning,
    retryingDocumentId,
    retryErrorsByDocumentId,
    deletionImpactByDocumentId,
    deletingDocumentId,
    deleteErrorsByDocumentId,
    noteSavingDocumentId,
    noteSaveErrorsByDocumentId,
    noteMetadataWarningsByDocumentId,
    titleErrorsByDocumentId,
    load,
    retry,
    select,
    createNote,
    createUntitledNote,
    renameDocument,
    importFile,
    loadContent,
    loadCatalog,
    createCategory,
    createTag,
    loadCategoryTree,
    createCategoryNode,
    browseBy,
    browseAll,
    categorizeCreatedDocument,
    updateCategoryNode,
    deleteCategoryNode,
    loadClassification,
    setCategory,
    toggleTag,
    runSearch,
    setSearchQuery,
    setSearchFilter,
    setIncludeDescendants,
    clearSearch,
    retryDocument,
    loadDeletionImpact,
    deleteDocument,
    saveNoteContent,
    openSource,
    revealSource,
  }
})
