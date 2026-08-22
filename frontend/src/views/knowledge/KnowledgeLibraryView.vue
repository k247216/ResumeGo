<template>
  <section class="knowledge-library" data-test="knowledge-library-view">
    <KnowledgeCommandBar
      :query="store.searchQuery"
      :importing="store.importing"
      :import-error-message="store.importErrorMessage"
      :show-navigator-restore="navigatorCollapsed"
      :show-list-restore="listCollapsed"
      :show-inspector-restore="!inspectorOpen"
      @update-query="store.setSearchQuery"
      @import-file="handleImport"
      @create-note="handleCreateNote"
      @restore-navigator="openNavigator"
      @restore-list="openDocumentList"
      @restore-inspector="openInspector"
    />
    <p v-if="store.categorizeWarning" class="categorize-warning" data-test="categorize-warning">{{ store.categorizeWarning }}</p>

    <div class="library-body">
      <KnowledgeNavigator
        v-if="!navigatorCollapsed"
        :nodes="store.categoryTree"
        :tags="store.tags"
        :expanded-ids="expandedFolderIds"
        :selected-id="selectedFolderId"
        :active-tag-id="activeTagId"
        :tree-error="store.categoryTreeError"
        @close="closeNavigator"
        @new-folder="openFolderCreate(null)"
        @toggle-folder="toggleFolder"
        @select-folder="selectFolder"
        @new-child="openFolderCreate"
        @rename-folder="openFolderEdit"
        @move-folder="openFolderEdit"
        @delete-folder="handleDeleteFolder"
        @select-tag="handleTag"
        @new-tag="nameDialogKind = 'tag'"
        @retry-tree="store.loadCategoryTree"
      />

      <KnowledgeDocumentList
        v-if="!listCollapsed"
        :documents="store.documents"
        :results="store.searchResults"
        :has-search="hasSearch"
        :selected-id="store.selectedDocumentId"
        :loading="store.searchQuery.trim() ? store.searchLoading : store.loading"
        :error-message="store.searchQuery.trim() ? store.searchErrorMessage : store.errorMessage"
        :scope-label="scopeLabel"
        :classification-by-document-id="store.classificationByDocumentId"
        :category-paths="categoryPaths"
        @close="closeDocumentList"
        @select="handleSelectDocument"
        @retry="store.retry"
        @retry-search="store.runSearch"
        @retry-doc="handleRetry"
      />

      <div class="reading-column">
        <KnowledgeReadingPane
          ref="readingPane"
          :document="store.selectedDocument"
          :content="selectedContent"
          :content-loading="store.contentLoadingDocumentId === store.selectedDocumentId"
          :content-error="selectedContentError"
          :saving="store.noteSavingDocumentId === store.selectedDocumentId"
          :error="selectedNoteSaveError"
          :title-error="selectedTitleError"
          @open-inspector="openInspector"
          @load-content="loadSelectedContent"
          @save-content="handleSaveNote"
          @rename-title="handleRenameTitle"
        />
        <p v-if="selectedMetadataWarning" class="metadata-warning" data-test="note-metadata-warning">{{ selectedMetadataWarning }}</p>
      </div>

      <div v-if="inspectorOpen && store.selectedDocument" class="inspector-wrap" :class="{ 'is-overlay': inspectorOverlay }">
      <KnowledgeSourceInspector
        :document="store.selectedDocument"
        :classification="selectedClassification"
        :classification-error="selectedClassificationError"
        :categories="store.categoryTree"
        :tags="store.tags"
        :category-paths="categoryPaths"
        :saving="store.classificationSaving"
        :retrying="store.retryingDocumentId === store.selectedDocument.id"
        :retry-error="selectedRetryError"
        :delete-error="selectedDeleteError"
        :source-result-message="selectedSourceResultMessage"
        @close="closeInspector"
        @set-category="handleSetCategory"
        @toggle-tag="handleToggleTag"
        @open-source="handleOpenSource"
        @reveal-source="handleRevealSource"
        @retry="handleRetry(store.selectedDocument.id)"
        @delete="openDelete"
      />
      </div>
    </div>

    <KnowledgeNameDialog v-if="nameDialogKind" :kind="nameDialogKind" :submitting="nameDialogBusy" :error="store.catalogErrorMessage" @close="nameDialogKind = null" @create="handleCreateName" />
    <KnowledgeFolderDialog
      v-if="folderDialog"
      :kind="folderDialog.kind"
      :initial-name="folderDialog.name"
      :initial-parent-id="folderDialog.parentId"
      :excluded-ids="folderDialog.excludedIds"
      :parent-options="folderParentOptions"
      :submitting="folderBusy"
      :error="store.categoryTreeError"
      @close="folderDialog = null"
      @submit="handleFolderSubmit"
    />
    <KnowledgeDeleteDialog
      v-if="deleteOpen"
      :impact="selectedImpact"
      :loading="impactLoading"
      :deleting="store.deletingDocumentId != null"
      :error="selectedDeleteError"
      @close="deleteOpen = false"
      @confirm="handleDeleteConfirm"
    />
    <KnowledgeUnsavedDialog
      v-if="unsavedOpen"
      :saving="pendingSaveBusy"
      :error="pendingSaveError"
      @keep-editing="cancelPendingSelection"
      @discard="discardAndSelect"
      @save="saveAndSelect"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import KnowledgeCommandBar from '../../components/knowledge/KnowledgeCommandBar.vue'
import KnowledgeNavigator from '../../components/knowledge/KnowledgeNavigator.vue'
import KnowledgeDocumentList from '../../components/knowledge/KnowledgeDocumentList.vue'
import KnowledgeReadingPane from '../../components/knowledge/KnowledgeReadingPane.vue'
import KnowledgeSourceInspector from '../../components/knowledge/KnowledgeSourceInspector.vue'
import KnowledgeNameDialog from '../../components/knowledge/KnowledgeNameDialog.vue'
import KnowledgeFolderDialog from '../../components/knowledge/KnowledgeFolderDialog.vue'
import KnowledgeDeleteDialog from '../../components/knowledge/KnowledgeDeleteDialog.vue'
import KnowledgeUnsavedDialog from '../../components/knowledge/KnowledgeUnsavedDialog.vue'
import { useKnowledgeStore } from '../../stores/knowledge'
import type { KnowledgeDeletionImpact } from '../../types/knowledge'

const store = useKnowledgeStore()
const nameDialogKind = ref<'category' | 'tag' | null>(null)
const nameDialogBusy = ref(false)
const PANE_STATE_KEY = 'resumego:knowledge:pane-state'
function readPaneState(): { navigatorCollapsed: boolean; listCollapsed: boolean; inspectorOpen: boolean } | null {
  try {
    const raw = localStorage.getItem(PANE_STATE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Record<string, unknown>
    if (typeof parsed.navigatorCollapsed !== 'boolean' || typeof parsed.listCollapsed !== 'boolean' || typeof parsed.inspectorOpen !== 'boolean') {
      return null
    }
    return parsed as { navigatorCollapsed: boolean; listCollapsed: boolean; inspectorOpen: boolean }
  } catch {
    return null
  }
}
function writePaneState() {
  try {
    localStorage.setItem(PANE_STATE_KEY, JSON.stringify({
      navigatorCollapsed: navigatorCollapsed.value,
      listCollapsed: listCollapsed.value,
      inspectorOpen: inspectorOpen.value,
    }))
  } catch {
    // 本地存储不可用时静默降级
  }
}
const savedPaneState = readPaneState()
const inspectorOpen = ref(savedPaneState?.inspectorOpen ?? true)
const navigatorCollapsed = ref(savedPaneState?.navigatorCollapsed ?? false)
const listCollapsed = ref(savedPaneState?.listCollapsed ?? false)
const navigatorPreferenceSet = ref(savedPaneState != null)
const listPreferenceSet = ref(savedPaneState != null)
const inspectorPreferenceSet = ref(savedPaneState != null)
const expandedFolderIds = ref<Set<number>>(new Set())
const selectedFolderId = ref<number | null>(null)
const activeTagId = ref<number | null>(null)
const sourceResultsByDocumentId = ref<Record<number, string>>({})
const readingPane = ref<InstanceType<typeof KnowledgeReadingPane> | null>(null)
const deleteOpen = ref(false)
const impactLoading = ref(false)
const folderBusy = ref(false)
const folderDialog = ref<{ kind: 'create' | 'edit'; id: number | null; name: string; parentId: number | null; excludedIds: number[] } | null>(null)
const viewportWidth = ref(window.innerWidth)
const unsavedOpen = ref(false)
const pendingSelectionId = ref<number | null>(null)
const pendingSaveBusy = ref(false)
const pendingSaveError = ref('')

const hasSearch = computed(() => store.searchQuery.trim().length > 0)

const inspectorOverlay = computed(() => viewportWidth.value < 1320)

const selectedNoteSaveError = computed(() => {
  const id = store.selectedDocumentId
  return id == null ? '' : store.noteSaveErrorsByDocumentId[id] ?? ''
})

const selectedTitleError = computed(() => {
  const id = store.selectedDocumentId
  return id == null ? '' : store.titleErrorsByDocumentId[id] ?? ''
})

const selectedRetryError = computed(() => {
  const id = store.selectedDocumentId
  return id == null ? '' : store.retryErrorsByDocumentId[id] ?? ''
})

const selectedDeleteError = computed(() => {
  const id = store.selectedDocumentId
  return id == null ? '' : store.deleteErrorsByDocumentId[id] ?? ''
})

const selectedSourceResultMessage = computed(() => {
  const id = store.selectedDocumentId
  return id == null ? '' : sourceResultsByDocumentId.value[id] ?? ''
})

const scopeLabel = computed(() => {
  if (hasSearch.value) return '搜索结果'
  if (activeTagId.value != null) {
    const tag = store.tags.find((t) => t.id === activeTagId.value)
    return tag ? '标签：' + tag.name : '资料'
  }
  if (selectedFolderId.value != null) {
    const folder = store.categoryTree.find((n) => n.id === selectedFolderId.value)
    return folder ? folder.name : '资料'
  }
  return '全部资料'
})

const selectedMetadataWarning = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.noteMetadataWarningsByDocumentId[id] ?? '' : ''
})

const selectedContent = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.contentByDocumentId[id] ?? '' : ''
})

const selectedContentError = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.contentErrorsByDocumentId[id] ?? '' : ''
})

const selectedClassification = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.classificationByDocumentId[id] ?? null : null
})

const selectedClassificationError = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.classificationErrorsByDocumentId[id] ?? '' : ''
})

const selectedImpact = computed<KnowledgeDeletionImpact | null>(() => {
  const id = store.selectedDocumentId
  return id != null ? store.deletionImpactByDocumentId[id] ?? null : null
})

const categoryPaths = computed(() => {
  const map: Record<number, string> = {}
  const byId = new Map(store.categoryTree.map((n) => [n.id, n]))
  for (const node of store.categoryTree) {
    const parts: string[] = []
    let cur: { id: number; name: string; parentId: number | null } | undefined = node
    const seen = new Set<number>()
    while (cur && !seen.has(cur.id)) {
      seen.add(cur.id)
      parts.unshift(cur.name)
      cur = cur.parentId != null ? byId.get(cur.parentId) : undefined
    }
    map[node.id] = parts.join(' / ')
  }
  return map
})

const folderParentOptions = computed(() => {
  const excluded = new Set(folderDialog.value?.excludedIds ?? [])
  return store.categoryTree.filter((n) => !excluded.has(n.id))
})

function toggleFolder(id: number) {
  const next = new Set(expandedFolderIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedFolderIds.value = next
}

function closeNavigator() {
  navigatorPreferenceSet.value = true
  navigatorCollapsed.value = true
  writePaneState()
}

function openNavigator() {
  navigatorPreferenceSet.value = true
  navigatorCollapsed.value = false
  writePaneState()
}

function closeDocumentList() {
  listPreferenceSet.value = true
  listCollapsed.value = true
  writePaneState()
}

function openDocumentList() {
  listPreferenceSet.value = true
  listCollapsed.value = false
  writePaneState()
}

function closeInspector() {
  inspectorPreferenceSet.value = true
  inspectorOpen.value = false
  writePaneState()
}

function openInspector() {
  inspectorPreferenceSet.value = true
  inspectorOpen.value = true
  writePaneState()
}

function selectFolder(id: number) {
  selectedFolderId.value = id
  activeTagId.value = null
  if (hasSearch.value) {
    store.setSearchFilter('category', id)
  } else {
    store.browseBy('category', id)
  }
}

function handleTag(id: number | null) {
  activeTagId.value = id
  if (id != null) selectedFolderId.value = null
  if (hasSearch.value) {
    store.setSearchFilter('tag', id)
  } else {
    if (id == null) store.browseAll()
    else store.browseBy('tag', id)
  }
}

const pendingScrollLine = ref<number | null>(null)

function handleSelectDocument(id: number) {
  if (id === store.selectedDocumentId) return
  if (readingPane.value?.hasUnsavedChanges()) {
    pendingSelectionId.value = id
    pendingSaveError.value = ''
    unsavedOpen.value = true
    return
  }
  performSelectDocument(id)
}

function performSelectDocument(id: number) {
  store.select(id)
  if (hasSearch.value) {
    const item = store.searchResults.find((r) => r.document.id === id)
    pendingScrollLine.value = item?.lineNumber ?? null
  }
}

function cancelPendingSelection() {
  unsavedOpen.value = false
  pendingSelectionId.value = null
  pendingSaveError.value = ''
}

function discardAndSelect() {
  const id = pendingSelectionId.value
  readingPane.value?.discardChanges()
  cancelPendingSelection()
  if (id != null) performSelectDocument(id)
}

async function saveAndSelect() {
  const id = pendingSelectionId.value
  const documentId = store.selectedDocumentId
  const pending = readingPane.value?.pendingChanges()
  if (id == null || documentId == null || !pending) {
    cancelPendingSelection()
    return
  }
  pendingSaveBusy.value = true
  pendingSaveError.value = ''
  try {
    if (pending.titleChanged) await store.renameDocument(documentId, pending.title)
    if (pending.contentChanged) await store.saveNoteContent(documentId, pending.content)
    readingPane.value?.discardChanges()
    cancelPendingSelection()
    performSelectDocument(id)
  } catch (error) {
    pendingSaveError.value = error instanceof Error ? error.message : '保存失败，请重试'
  } finally {
    pendingSaveBusy.value = false
  }
}

// 正文就绪后执行待处理定位（避免 DOM 未切换导致定位无效）
watch(selectedContent, (content) => {
  if (!content || pendingScrollLine.value == null) return
  const line = pendingScrollLine.value
  pendingScrollLine.value = null
  void readingPane.value?.scrollToLine(line)
})

function openFolderCreate(parentId: number | null) {
  folderDialog.value = { kind: 'create', id: null, name: '', parentId, excludedIds: [] }
}

function openFolderEdit(id: number) {
  const node = store.categoryTree.find((n) => n.id === id)
  if (!node) return
  const excluded = descendantsOf(id)
  folderDialog.value = { kind: 'edit', id: node.id, name: node.name, parentId: node.parentId, excludedIds: excluded }
}

function descendantsOf(id: number): number[] {
  const result: number[] = [id]
  const queue = [id]
  while (queue.length) {
    const cur = queue.shift()!
    const children = store.categoryTree.filter((n) => n.parentId === cur)
    for (const child of children) {
      result.push(child.id)
      queue.push(child.id)
    }
  }
  return result
}

async function handleFolderSubmit(name: string, parentId: number | null) {
  if (!folderDialog.value) return
  folderBusy.value = true
  try {
    if (folderDialog.value.kind === 'create') {
      await store.createCategoryNode(name, parentId)
    } else {
      const id = folderDialog.value.id
      if (id == null) return
      await store.updateCategoryNode(id, name, parentId)
    }
    folderDialog.value = null
  } catch {
    // 错误在 store.categoryTreeError
  } finally {
    folderBusy.value = false
  }
}

async function handleDeleteFolder(id: number) {
  try {
    await store.deleteCategoryNode(id)
    if (selectedFolderId.value === id) selectedFolderId.value = null
  } catch {
    // 错误在 store.categoryTreeError
  }
}

async function handleCreateNote() {
  try {
    const created = await store.createUntitledNote()
    if (selectedFolderId.value != null) {
      await store.categorizeCreatedDocument(created.id, selectedFolderId.value)
    }
    await nextTick()
    await readingPane.value?.beginEdit({ focusTitle: true })
  } catch {
    // 错误在 store.errorMessage
  }
}

async function handleRenameTitle(title: string) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.renameDocument(id, title)
  } catch {
    // 错误按文档隔离到 titleErrorsByDocumentId
  }
}

async function handleCreateName(name: string) {
  if (!nameDialogKind.value) return
  nameDialogBusy.value = true
  try {
    if (nameDialogKind.value === 'category') await store.createCategoryNode(name, selectedFolderId.value)
    else await store.createTag(name)
    nameDialogKind.value = null
  } catch {
    // 错误在 store.catalogErrorMessage
  } finally {
    nameDialogBusy.value = false
  }
}

async function handleImport(file: File) {
  try {
    await store.importFile(file, selectedFolderId.value)
  } catch {
    // 错误在 store.importErrorMessage
  }
}

async function handleSaveNote(content: string) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.saveNoteContent(id, content)
  } catch {
    // 错误在 store.noteSaveErrorMessage
  }
}

async function handleRetry(id: number) {
  try {
    await store.retryDocument(id)
  } catch {
    // 错误在 store.retryErrorMessage
  }
}

async function handleSetCategory(categoryId: number | null) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.setCategory(id, categoryId)
  } catch {
    // 错误按文档隔离
  }
}

async function handleToggleTag(tagId: number, add: boolean) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.toggleTag(id, tagId, add)
  } catch {
    // 错误按文档隔离
  }
}

async function handleOpenSource() {
  const id = store.selectedDocumentId
  if (id == null) return
  const result = await store.openSource(id)
  sourceResultsByDocumentId.value = { ...sourceResultsByDocumentId.value, [id]: result.ok ? '' : (result.message ?? '无法打开原文') }
}

async function handleRevealSource() {
  const id = store.selectedDocumentId
  if (id == null) return
  const result = await store.revealSource(id)
  sourceResultsByDocumentId.value = { ...sourceResultsByDocumentId.value, [id]: result.ok ? '' : (result.message ?? '无法定位原文') }
}

async function openDelete() {
  const id = store.selectedDocumentId
  if (id == null) return
  deleteOpen.value = true
  impactLoading.value = true
  try {
    await store.loadDeletionImpact(id)
  } catch {
    // 错误在 store.deleteErrorMessage
  } finally {
    impactLoading.value = false
  }
}

async function handleDeleteConfirm(token: string) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.deleteDocument(id, token)
    deleteOpen.value = false
  } catch {
    // 错误在 store.deleteErrorMessage；失败时文档保持列表
  }
}

function loadSelectedContent() {
  const id = store.selectedDocumentId
  if (id != null) void store.loadContent(id).catch(() => undefined)
}

// 可见文档预取分类（有缓存，用于列表行显示文件夹/标签）
watch(() => store.documents.map((d) => d.id).join(','), (ids) => {
  if (!ids) return
  for (const id of store.documents) {
    void store.loadClassification(id.id).catch(() => undefined)
  }
})

watch(() => store.selectedDocumentId, () => {
  loadSelectedContent()
  const id = store.selectedDocumentId
  if (id != null) void store.loadClassification(id).catch(() => undefined)
})

function applyResponsive() {
  const width = window.innerWidth || 1440
  viewportWidth.value = width
  if (width >= 1320) {
    if (!inspectorPreferenceSet.value) inspectorOpen.value = true
    if (!navigatorPreferenceSet.value) navigatorCollapsed.value = false
    if (!listPreferenceSet.value) listCollapsed.value = false
  } else if (width >= 1160) {
    if (!inspectorPreferenceSet.value) inspectorOpen.value = false
    if (!navigatorPreferenceSet.value) navigatorCollapsed.value = false
    if (!listPreferenceSet.value) listCollapsed.value = false
  } else {
    if (!inspectorPreferenceSet.value) inspectorOpen.value = false
    if (!navigatorPreferenceSet.value) navigatorCollapsed.value = true
    if (!listPreferenceSet.value) listCollapsed.value = width < 1080
  }
}

// 初始：加载列表/目录/分类树 + 响应式（卸载时清理监听避免累积）
onMounted(() => {
  applyResponsive()
  window.addEventListener('resize', applyResponsive)
  void store.load().catch(() => undefined)
  void store.loadCatalog().catch(() => undefined)
  void store.loadCategoryTree().catch(() => undefined)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', applyResponsive)
})
</script>

<style scoped>
.knowledge-library{position:relative;display:flex;flex-direction:column;height:100%;min-height:0;background:var(--surface-solid);overflow:hidden}
.categorize-warning{margin:0 24px 8px;padding:8px 12px;border:1px solid var(--border-default);border-radius:10px;background:var(--danger-soft);color:var(--danger);font-size:12px}
.library-body{position:relative;flex:1;min-height:0;display:flex;background:var(--surface-solid);overflow:hidden}
.reading-column{flex:1;min-width:0;display:flex;flex-direction:column}
.inspector-wrap{position:relative;flex:none;align-self:stretch}
.inspector-wrap.is-overlay{position:absolute;top:0;right:0;bottom:0;z-index:20;background:var(--surface-solid);box-shadow:-8px 0 24px rgba(0,0,0,.08)}
.metadata-warning{margin:0 28px 10px;padding:7px 11px;border:1px solid var(--border-default);border-radius:9px;background:var(--danger-soft);color:var(--danger);font-size:12px}
</style>
