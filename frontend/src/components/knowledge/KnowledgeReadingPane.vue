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
            v-if="editable"
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

        <!-- 可编辑（NOTE / 受管 Markdown）：像编辑文件一样直接修改，无需先进入编辑模式 -->
        <template v-else-if="editable">
          <textarea
            v-model="draft"
            class="body-editor"
            data-test="knowledge-body-editor"
            placeholder="直接输入正文，与文件内容同步…"
            :disabled="saving"
            spellcheck="false"
          ></textarea>
          <div v-if="dirty || saving" class="editor-bar" data-test="knowledge-editor-bar">
            <span class="editor-dirty"><el-icon :size="13"><EditPen /></el-icon><span>有未保存的修改</span></span>
            <span class="editor-actions">
              <button type="button" class="text-btn" data-test="knowledge-edit-cancel" :disabled="saving" @click="discardChanges">放弃修改</button>
              <button
                type="button"
                class="save-btn"
                data-test="knowledge-edit-save"
                :disabled="saving || draftTooLarge || !dirty"
                @click="saveChanges"
              >
                {{ saving ? '保存中…' : '保存更改' }}
              </button>
            </span>
            <span v-if="draftTooLarge" class="editor-hint" data-test="note-size-hint">正文不能超过 1 MiB</span>
          </div>
          <p v-if="error" class="editor-error" data-test="note-save-error">{{ error }}</p>
        </template>

        <!-- 只读（TXT / 未知类型 / 未完成）：渲染视图，无编辑入口 -->
        <div v-else class="document-view">
          <KnowledgeMarkdownView :source="content" />
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

const draft = ref('')
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
const bodyEl = ref<HTMLElement | null>(null)

const MAX_BYTES = 1024 * 1024
const draftTooLarge = computed(() => new TextEncoder().encode(draft.value).length > MAX_BYTES)
const editable = computed(() => props.document?.sourceType === 'NOTE'
  || (props.document?.sourceType === 'FILE' && props.document.sourceExtension?.toLowerCase() === 'md'))
const dirty = computed(() => !props.document ? false
  : draft.value !== props.content || normalizedTitle() !== props.document.title)

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

function normalizedTitle(): string {
  return titleDraft.value.trim()
}

function syncDraftFromContent() {
  draft.value = props.content
  titleDraft.value = props.document?.title ?? ''
}

/** 直接编辑态：同步草稿并聚焦标题（新建笔记后由视图调用）。 */
async function beginEdit(options: { focusTitle?: boolean } = {}) {
  if (!props.document || !editable.value) return
  await nextTick()
  if (options.focusTitle) {
    titleInput.value?.focus()
    titleInput.value?.select()
  }
}

function commitTitle() {
  if (!props.document || !editable.value) return
  const title = normalizedTitle()
  if (!title) {
    titleDraft.value = props.document.title
    return
  }
  if (title !== props.document.title) emit('rename-title', title)
}

function hasUnsavedChanges(): boolean {
  if (!props.document) return false
  return dirty.value
}

function pendingChanges() {
  if (!props.document) return null
  const title = normalizedTitle() || props.document.title
  return {
    title,
    content: draft.value,
    titleChanged: title !== props.document.title,
    contentChanged: draft.value !== props.content,
  }
}

function discardChanges() {
  syncDraftFromContent()
}

function saveChanges() {
  if (!editable.value || !props.document || draftTooLarge.value || props.saving) return
  commitTitle()
  if (draft.value !== props.content) emit('save-content', draft.value)
}

// 切换文档：重置草稿到新文档的已保存状态
watch(() => props.document?.id, () => {
  syncDraftFromContent()
})

// 正文异步到达：未改动时保持同步；已改动绝不覆盖用户输入
watch(() => props.content, (content) => {
  if (draft.value === '' && content !== '') {
    draft.value = content
  }
})

watch(() => props.document?.title, (title) => {
  if (title && titleDraft.value === '') titleDraft.value = title
})

// 保存完成后清除脏状态（error 为空才视为成功）
watch(() => props.saving, (saving, wasSaving) => {
  if (wasSaving && !saving && !props.error) {
    draft.value = props.content
    titleDraft.value = props.document?.title ?? ''
  }
})

async function scrollToLine(lineNumber: number | null) {
  if (!props.document || lineNumber == null || !bodyEl.value) return
  await nextTick()
  const editor = bodyEl.value.querySelector<HTMLTextAreaElement>('.body-editor')
  if (editor) {
    const lines = editor.value.split('\n')
    const target = Math.min(lineNumber, lines.length)
    let offset = 0
    for (let i = 0; i < target - 1 && i < lines.length; i++) offset += lines[i].length + 1
    const total = editor.value.length || 1
    const ratio = lines.length ? offset / Math.max(total, 1) : 0
    editor.scrollTop = ratio * (editor.scrollHeight - editor.clientHeight)
    return
  }
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
.reading-head{flex:none;padding:22px 34px 14px;border-bottom:1px solid var(--border-subtle)}
.reading-title-row{display:flex;align-items:center;gap:12px}
.title-input{flex:1;min-width:0;border:0;border-bottom:1px solid var(--border-default);background:transparent;color:var(--ink);font:650 21px/1.35 inherit;letter-spacing:-.015em;padding:2px 0 5px}
.title-input:focus{outline:0;border-bottom-color:var(--brand)}
.title-input:disabled{opacity:.6}
.reading-title-row h2{margin:0;font-size:21px;font-weight:650;letter-spacing:-.015em;color:var(--ink)}
.reading-status{flex:none;font-size:11px;padding:2px 9px;border-radius:999px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.inspector-btn{display:inline-flex;align-items:center;gap:5px;border:0;background:transparent;color:var(--muted);font-size:12px;cursor:pointer;flex:none;padding:5px}
.inspector-btn:hover{color:var(--brand)}
.reading-meta{margin:8px 0 0;font-size:12px;color:var(--muted)}
.title-error{margin:8px 0 0;font-size:12px;color:var(--danger)}
.reading-body{flex:1;min-height:0;overflow:auto;padding:0}
.reading-state{display:grid;gap:8px;align-content:center;justify-items:start;min-height:100%;padding:48px 34px;color:var(--muted);font-size:13px}
.reading-state.error{color:var(--danger)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
.text-btn:disabled{opacity:.5;cursor:default}
/* 文档式直接编辑：textarea 与只读渲染视图同排版，无边框、无卡片 */
.body-editor{box-sizing:border-box;display:block;width:100%;min-height:100%;padding:24px 34px 96px;border:0;background:transparent;color:var(--ink);font:14px/1.78 inherit;resize:none;outline:0;caret-color:var(--brand)}
.body-editor:disabled{opacity:.7}
.body-editor::placeholder{color:var(--muted)}
/* 底部保存条：仅脏状态出现 */
.editor-bar{position:sticky;bottom:0;display:flex;align-items:center;gap:12px;margin:0 34px 18px;padding:9px 14px;border:1px solid var(--border-default);border-radius:10px;background:var(--surface-solid);box-shadow:0 -2px 12px rgba(0,0,0,.05)}
.editor-dirty{display:inline-flex;align-items:center;gap:6px;color:var(--copy);font-size:12.5px;font-weight:550}
.editor-actions{display:inline-flex;align-items:center;gap:10px;margin-left:auto}
.save-btn{padding:7px 16px;border:0;border-radius:8px;background:var(--action-bg);color:var(--action-fg);font-size:13px;font-weight:600;cursor:pointer}
.save-btn:disabled{opacity:.5;cursor:default}
.editor-hint{color:var(--danger);font-size:12px}
.editor-error{margin:0 34px 18px;font-size:12.5px;color:var(--danger)}
.document-view{padding:24px 34px 56px;min-height:100%;box-sizing:border-box}
</style>