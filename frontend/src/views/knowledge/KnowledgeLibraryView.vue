<template>
  <section class="knowledge-view" data-test="knowledge-library-view">
    <PageHeader eyebrow="知识库" title="知识库" subtitle="本地资料：笔记与 .md/.txt 文件，正文只保存在此设备。">
      <template #actions>
        <button type="button" class="tool-btn" data-test="knowledge-refresh" :disabled="store.loading" @click="store.retry">刷新</button>
        <KnowledgeImportControl
          :disabled="store.importing"
          :error="store.importErrorMessage"
          @file-selected="handleImport"
        />
        <span v-if="store.listRefreshError" class="import-error" data-test="knowledge-list-refresh-error">{{ store.listRefreshError }}</span>
        <button type="button" class="tool-btn primary" data-test="knowledge-create-note" @click="noteOpen = true">新建笔记</button>
      </template>
    </PageHeader>

    <KnowledgeSearchBar
      :query="store.searchQuery"
      :category-id="store.searchCategoryId"
      :tag-id="store.searchTagId"
      :categories="store.categories"
      :tags="store.tags"
      @update-query="handleSearchQuery"
      @update-category="handleSearchCategory"
      @update-tag="handleSearchTag"
    />

    <div class="knowledge-body">
      <KnowledgeSearchResults
        v-if="hasSearchQuery"
        :results="store.searchResults"
        :selected-id="store.selectedDocumentId"
        :loading="store.searchLoading"
        :error-message="store.searchErrorMessage"
        @select="handleSelect"
        @retry="store.runSearch"
      />
      <KnowledgeListRail
        v-else
        :documents="store.documents"
        :selected-id="store.selectedDocumentId"
        :loading="store.loading"
        :error-message="store.errorMessage"
        @select="handleSelect"
        @retry="store.retry"
        @create-note="noteOpen = true"
      />
      <main class="detail-pane">
        <KnowledgeDetailPane
          :document="store.selectedDocument"
          :has-documents="store.documents.length > 0"
          :content="selectedContent"
          :content-loading="store.contentLoadingDocumentId === store.selectedDocumentId"
          :content-error="selectedContentError"
          @create-note="noteOpen = true"
          @load-content="loadSelectedContent"
        />
        <KnowledgeClassificationPanel
          v-if="store.selectedDocument"
          :classification="selectedClassification"
          :loading="store.classificationLoadingDocumentId === store.selectedDocumentId"
          :saving="store.classificationSaving"
          :error="selectedClassificationError"
          :categories="store.categories"
          :tags="store.tags"
          @reload="loadSelectedClassification"
          @set-category="handleSetCategory"
          @toggle-tag="handleToggleTag"
          @create-category="nameDialogKind = 'category'"
          @create-tag="nameDialogKind = 'tag'"
        />
      </main>
    </div>

    <KnowledgeNoteDialog
      v-if="noteOpen"
      :submitting="store.creating"
      :error="store.errorMessage"
      @close="noteOpen = false"
      @create="handleCreateNote"
    />
    <KnowledgeNameDialog
      v-if="nameDialogKind"
      :kind="nameDialogKind"
      :submitting="nameDialogBusy"
      :error="store.catalogErrorMessage"
      @close="nameDialogKind = null"
      @create="handleCreateName"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import KnowledgeListRail from '../../components/knowledge/KnowledgeListRail.vue'
import KnowledgeDetailPane from '../../components/knowledge/KnowledgeDetailPane.vue'
import KnowledgeNoteDialog from '../../components/knowledge/KnowledgeNoteDialog.vue'
import KnowledgeImportControl from '../../components/knowledge/KnowledgeImportControl.vue'
import KnowledgeSearchBar from '../../components/knowledge/KnowledgeSearchBar.vue'
import KnowledgeSearchResults from '../../components/knowledge/KnowledgeSearchResults.vue'
import KnowledgeClassificationPanel from '../../components/knowledge/KnowledgeClassificationPanel.vue'
import KnowledgeNameDialog from '../../components/knowledge/KnowledgeNameDialog.vue'
import { useKnowledgeStore } from '../../stores/knowledge'

const store = useKnowledgeStore()
const noteOpen = ref(false)
const nameDialogKind = ref<'category' | 'tag' | null>(null)
const nameDialogBusy = ref(false)

const hasSearchQuery = computed(() => store.searchQuery.trim().length > 0)

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

function handleSelect(id: number) {
  store.select(id)
}

async function handleImport(file: File) {
  try {
    await store.importFile(file)
  } catch {
    // 错误已写入 store.importErrorMessage，只影响导入区域
  }
}

async function handleCreateNote(title: string) {
  try {
    await store.createNote(title)
    noteOpen.value = false
  } catch {
    // 错误已写入 store.errorMessage，对话框保持打开展示
  }
}

async function handleCreateName(name: string) {
  if (!nameDialogKind.value) return
  nameDialogBusy.value = true
  try {
    if (nameDialogKind.value === 'category') {
      await store.createCategory(name)
    } else {
      await store.createTag(name)
    }
    nameDialogKind.value = null
  } catch {
    // 错误已写入 store.catalogErrorMessage，对话框保持打开展示
  } finally {
    nameDialogBusy.value = false
  }
}

function handleSearchQuery(q: string) {
  store.setSearchQuery(q)
}

function handleSearchCategory(id: number | null) {
  store.setSearchFilter('category', id)
}

function handleSearchTag(id: number | null) {
  store.setSearchFilter('tag', id)
}

async function handleSetCategory(categoryId: number | null) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.setCategory(id, categoryId)
  } catch {
    // 错误已写入该文档的 classificationErrorsByDocumentId
  }
}

async function handleToggleTag(tagId: number, add: boolean) {
  const id = store.selectedDocumentId
  if (id == null) return
  try {
    await store.toggleTag(id, tagId, add)
  } catch {
    // 错误已写入该文档的 classificationErrorsByDocumentId
  }
}

function loadSelectedContent() {
  const id = store.selectedDocumentId
  if (id != null) {
    void store.loadContent(id).catch(() => undefined)
  }
}

function loadSelectedClassification() {
  const id = store.selectedDocumentId
  if (id != null) {
    void store.loadClassification(id).catch(() => undefined)
  }
}

watch(() => store.selectedDocumentId, () => {
  loadSelectedContent()
  loadSelectedClassification()
})

onMounted(() => {
  void store.load().catch(() => undefined)
  void store.loadCatalog().catch(() => undefined)
})
</script>

<style scoped>
.knowledge-view{display:flex;flex-direction:column;height:100%;min-height:0}
.knowledge-body{flex:1;min-height:0;display:grid;grid-template-columns:280px minmax(0,1fr);border:1px solid var(--border-subtle);border-radius:18px;background:var(--surface);overflow:hidden}
.detail-pane{min-width:0;min-height:0;display:flex;flex-direction:column;overflow:hidden}
.tool-btn{padding:8px 13px;border:1px solid var(--border-default);border-radius:10px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer}
.tool-btn:hover{border-color:var(--brand);color:var(--brand)}
.tool-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.tool-btn.primary:hover{opacity:.92;color:var(--action-fg)}
.tool-btn:disabled{opacity:.55;cursor:not-allowed}
.import-error{color:var(--danger);font-size:12px;max-width:280px}
</style>
