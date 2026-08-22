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
        <button type="button" class="tool-btn primary" data-test="knowledge-create-note" @click="noteOpen = true">新建笔记</button>
      </template>
    </PageHeader>

    <div class="knowledge-body">
      <KnowledgeListRail
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
      </main>
    </div>

    <KnowledgeNoteDialog
      v-if="noteOpen"
      :submitting="store.creating"
      :error="store.errorMessage"
      @close="noteOpen = false"
      @create="handleCreateNote"
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
import { useKnowledgeStore } from '../../stores/knowledge'

const store = useKnowledgeStore()
const noteOpen = ref(false)

const selectedContent = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.contentByDocumentId[id] ?? '' : ''
})

const selectedContentError = computed(() => {
  const id = store.selectedDocumentId
  return id != null ? store.contentErrorsByDocumentId[id] ?? '' : ''
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

function loadSelectedContent() {
  const id = store.selectedDocumentId
  if (id != null) {
    void store.loadContent(id).catch(() => undefined)
  }
}

watch(() => store.selectedDocumentId, () => loadSelectedContent())

onMounted(() => {
  void store.load().catch(() => undefined)
})
</script>

<style scoped>
.knowledge-view{display:flex;flex-direction:column;height:100%;min-height:0}
.knowledge-body{flex:1;min-height:0;display:grid;grid-template-columns:280px minmax(0,1fr);border:1px solid var(--border-subtle);border-radius:18px;background:var(--surface);overflow:hidden}
.detail-pane{min-width:0;min-height:0;display:flex;overflow:hidden}
.tool-btn{padding:8px 13px;border:1px solid var(--border-default);border-radius:10px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer}
.tool-btn:hover{border-color:var(--brand);color:var(--brand)}
.tool-btn.primary{border:0;background:var(--action-bg);color:var(--action-fg);font-weight:600}
.tool-btn.primary:hover{opacity:.92;color:var(--action-fg)}
.tool-btn:disabled{opacity:.55;cursor:not-allowed}
</style>
