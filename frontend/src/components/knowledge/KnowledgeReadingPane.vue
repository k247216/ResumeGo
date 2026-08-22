<template>
  <main class="reading" data-test="knowledge-reading-pane">
    <div v-if="!document" class="reading-empty">
      <strong>选择一份资料</strong>
      <span>从资料列表选择内容，或导入 .md/.txt、新建一篇本地笔记。</span>
    </div>

    <template v-else>
      <header class="reading-head">
        <div class="reading-title-row">
          <input
            v-if="editing"
            ref="titleInput"
            v-model="titleDraft"
            class="title-input"
            data-test="knowledge-title-input"
            maxlength="120"
            aria-label="资料标题"
            :disabled="saving"
            @blur="commitTitle"
            @keydown.enter.prevent="commitTitle"
          />
          <h2 v-else>{{ document.title }}</h2>
          <span class="reading-status" :class="tone(document.processingStatus)">{{ statusLabel(document.processingStatus) }}</span>
          <button
            type="button"
            class="inspector-btn"
            data-test="reading-open-inspector"
            aria-label="打开来源与属性"
            @click="$emit('open-inspector')"
          >
            <el-icon :size="15"><InfoFilled /></el-icon><span>属性</span>
          </button>
        </div>
        <p class="reading-meta">{{ metaLine }}</p>
        <p v-if="titleError" class="title-error" data-test="knowledge-title-error">{{ titleError }}</p>
      </header>

      <section ref="bodyEl" class="reading-body">
        <div v-if="contentLoading" class="reading-state">正在读取正文…</div>
        <div v-else-if="contentError" class="reading-state error" data-test="file-content-error">
          <span>{{ contentError }}</span>
          <button type="button" class="text-btn" data-test="file-content-retry" @click="$emit('load-content')">重试</button>
        </div>

        <div v-else-if="editing" class="document-editor">
          <textarea
            v-model="draft"
            class="body-editor"
            data-test="knowledge-body-editor"
            placeholder="输入正文…"
            :disabled="saving"
          ></textarea>
          <div class="editor-actions">
            <button type="button" class="text-btn" data-test="knowledge-edit-cancel" :disabled="saving" @click="discardChanges">取消</button>
            <button
              type="button"
              class="save-btn"
              data-test="knowledge-edit-save"
              :disabled="saving || draftTooLarge || !hasUnsavedChanges()"
              @click="saveChanges"
            >
              {{ saving ? '保存中…' : '保存更改' }}
            </button>
            <span v-if="draftTooLarge" class="editor-hint" data-test="note-size-hint">正文不能超过 1 MiB</span>
          </div>
          <p v-if="error" class="editor-error" data-test="note-save-error">{{ error }}</p>
        </div>

        <div v-else class="document-view">
          <KnowledgeMarkdownView :source="content" />
          <button
            v-if="editable"
            type="button"
            class="edit-btn"
            data-test="knowledge-edit-start"
            @click="beginEdit()"
          >
            <el-icon><EditPen /></el-icon><span>编辑</span>
          </button>
        </div>
      </section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { EditPen, InfoFilled } from '@element-plus/icons-vue'
import KnowledgeMarkdownView from './KnowledgeMarkdownView.vue'
import { knowledgeStatusLabel } from './status'
import type { KnowledgeDocument } from '../../types/knowledge'

const props = defineProps<{
  document: KnowledgeDocument | null
  content: string
  contentLoading: boolean
  contentError: string
  saving: boolean
  error: string
  titleError: string
}>()

const emit = defineEmits<{
  (e: 'open-inspector'): void
  (e: 'load-content'): void
  (e: 'save-content', content: string): void
  (e: 'rename-title', title: string): void
}>()

const editing = ref(false)
const draft = ref('')
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
const bodyEl = ref<HTMLElement | null>(null)

const MAX_BYTES = 1024 * 1024
const draftTooLarge = computed(() => new TextEncoder().encode(draft.value).length > MAX_BYTES)
const editable = computed(() => props.document?.sourceType === 'NOTE'
  || (props.document?.sourceType === 'FILE' && props.document.sourceExtension?.toLowerCase() === 'md'))

const metaLine = computed(() => {
  if (!props.document) return ''
  const updated = new Date(props.document.updatedAt).toLocaleString()
  const type = props.document.sourceType === 'NOTE'
    ? '本地笔记'
    : props.document.sourceExtension?.toLowerCase() === 'md' ? 'Markdown 受管副本' : '本地文件 · 只读'
  return `${type} · 更新于 ${updated}`
})

function statusLabel(status: KnowledgeDocument['processingStatus']): string {
  return knowledgeStatusLabel(status)
}

function tone(status: KnowledgeDocument['processingStatus']): string {
  if (status === 'FAILED') return 'tone-danger'
  if (status === 'COMPLETED') return 'tone-ok'
  if (status === 'PENDING' || status === 'RUNNING') return 'tone-busy'
  return 'tone-idle'
}

async function beginEdit(options: { focusTitle?: boolean } = {}) {
  if (!editable.value || !props.document) return
  draft.value = props.content
  titleDraft.value = props.document.title
  editing.value = true
  await nextTick()
  if (options.focusTitle) {
    titleInput.value?.focus()
    titleInput.value?.select()
  }
}

function normalizedTitle(): string {
  return titleDraft.value.trim()
}

function commitTitle() {
  if (!props.document || !editing.value) return
  const title = normalizedTitle()
  if (!title) {
    titleDraft.value = props.document.title
    return
  }
  if (title !== props.document.title) emit('rename-title', title)
}

function hasUnsavedChanges(): boolean {
  if (!editing.value || !props.document) return false
  return draft.value !== props.content || normalizedTitle() !== props.document.title
}

function pendingChanges() {
  if (!editing.value || !props.document) return null
  const title = normalizedTitle() || props.document.title
  return {
    title,
    content: draft.value,
    titleChanged: title !== props.document.title,
    contentChanged: draft.value !== props.content,
  }
}

function discardChanges() {
  draft.value = props.content
  titleDraft.value = props.document?.title ?? ''
  editing.value = false
}

function saveChanges() {
  if (!editable.value || !props.document || draftTooLarge.value || props.saving) return
  commitTitle()
  if (draft.value !== props.content) emit('save-content', draft.value)
}

watch(() => props.document?.id, () => {
  discardChanges()
})

watch(() => props.document?.title, (title) => {
  if (editing.value && title) titleDraft.value = title
})

watch(() => props.saving, (saving, wasSaving) => {
  if (wasSaving && !saving && !props.error) discardChanges()
})

async function scrollToLine(lineNumber: number | null) {
  if (!props.document || lineNumber == null || !bodyEl.value) return
  await nextTick()
  const view = bodyEl.value.querySelector('.markdown-view')
  const pre = view?.querySelector('pre') ?? view
  if (!pre) return
  const lines = pre.textContent?.split('\n') ?? []
  const target = Math.min(lineNumber, lines.length)
  let offset = 0
  for (let i = 0; i < target - 1 && i < lines.length; i++) offset += lines[i].length + 1
  const total = pre.textContent?.length ?? 1
  const ratio = lines.length ? offset / Math.max(total, 1) : 0
  bodyEl.value.scrollTop = ratio * (pre.scrollHeight - bodyEl.value.clientHeight)
}

defineExpose({ scrollToLine, beginEdit, hasUnsavedChanges, pendingChanges, discardChanges, saveChanges })
</script>

<style scoped>
.reading{flex:1;min-width:0;min-height:0;display:flex;flex-direction:column;background:var(--surface-solid);overflow:hidden}
.reading-empty{display:grid;gap:8px;align-content:center;justify-items:start;min-height:100%;padding:48px;color:var(--muted);font-size:13px}
.reading-empty strong{color:var(--ink);font-size:16px}
.reading-head{flex:none;padding:22px 34px 14px;border-bottom:1px solid var(--border-subtle)}
.reading-title-row{display:flex;align-items:center;gap:10px}
.reading-title-row h2{margin:0;font-size:21px;font-weight:650;letter-spacing:-.015em;color:var(--ink);word-break:break-word;flex:1;min-width:0}
.title-input{flex:1;min-width:0;border:0;border-bottom:1px solid var(--border-default);background:transparent;color:var(--ink);font:650 21px/1.35 inherit;letter-spacing:-.015em;padding:2px 0 5px}
.title-input:focus{outline:0;border-bottom-color:var(--brand)}
.reading-status{flex:none;font-size:11px;padding:2px 9px;border-radius:999px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.inspector-btn{display:inline-flex;align-items:center;gap:5px;border:0;background:transparent;color:var(--muted);font-size:12px;cursor:pointer;flex:none;padding:5px}
.inspector-btn:hover{color:var(--ink)}
.reading-meta{margin:8px 0 0;color:var(--muted);font-size:12px}
.title-error,.editor-error{margin:8px 0 0;color:var(--danger);font-size:12px}
.reading-body{flex:1;min-height:0;overflow:auto;padding:24px 34px 56px}
.reading-body>*{width:min(100%,760px)}
.reading-state{display:grid;gap:8px;justify-items:start;color:var(--muted);font-size:13px}
.reading-state.error{color:var(--danger)}
.document-view{position:relative;min-height:240px}
.edit-btn{display:inline-flex;align-items:center;gap:6px;margin-top:24px;padding:7px 12px;border:1px solid var(--border-default);border-radius:8px;background:transparent;color:var(--copy);font-size:12px;cursor:pointer}
.edit-btn:hover{border-color:var(--ink);color:var(--ink)}
.document-editor{display:grid;gap:12px}
.body-editor{box-sizing:border-box;width:100%;min-height:420px;padding:4px 0 24px;border:0;border-bottom:1px solid var(--border-subtle);background:transparent;color:var(--ink);font-size:14px;line-height:1.75;resize:vertical;font-family:inherit}
.body-editor:focus{outline:0;border-bottom-color:var(--brand)}
.editor-actions{display:flex;align-items:center;gap:12px}
.save-btn{padding:8px 16px;border:0;border-radius:8px;background:var(--action-bg);color:var(--action-fg);font-size:13px;font-weight:600;cursor:pointer}
.save-btn:disabled{opacity:.5;cursor:not-allowed}
.editor-hint{color:var(--danger);font-size:12px}
.text-btn{border:0;background:transparent;color:var(--copy);font-size:13px;cursor:pointer;padding:0}
.text-btn:hover{color:var(--ink)}
</style>
