<template>
  <main class="reading" data-test="knowledge-reading-pane">
    <div v-if="!document" class="reading-empty">
      <strong>选择一份资料</strong>
      <span>从资料列表选择内容，或导入 Markdown/Word/PDF、新建一篇本地笔记。</span>
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
            @blur="flushPendingSave"
            @keydown.enter.prevent="flushPendingSave"
          />
          <h2 v-else>{{ document.title }}</h2>
          <span class="reading-status" :class="tone(document.processingStatus)">{{ statusLabel(document.processingStatus) }}</span>
          <span v-if="editable" class="mode-toggle" role="tablist" aria-label="编辑模式">
            <button type="button" class="mode-btn" :class="{ active: mode === 'read' }" data-test="mode-read" @click="mode = 'read'">阅读</button>
            <button type="button" class="mode-btn" :class="{ active: mode === 'edit' }" data-test="mode-edit" @click="enterEdit()">编辑</button>
          </span>
          <span v-if="saveStatusText" class="save-status" :class="'st-' + saveStatus" data-test="autosave-status">{{ saveStatusText }}</span>
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

        <!-- 仅收录元数据（PDF/图片等）：文件内预览；无法预览的类型给出诚实提示 -->
        <div v-else-if="metadataOnly" class="source-preview" data-test="source-preview">
          <iframe
            v-if="previewType === 'pdf'"
            :src="sourceUrl"
            class="preview-frame"
            data-test="source-preview-pdf"
            title="PDF 预览"
          ></iframe>
          <img v-else-if="previewType === 'image'" :src="sourceUrl" class="preview-image" data-test="source-preview-image" alt="文件预览" />
          <div v-else-if="sourceLoading" class="reading-state">正在读取文件…</div>
          <div v-else class="reading-state" data-test="metadata-only-notice">
            <strong>{{ metadataTitle }}</strong>
            <span>文件已收录进知识库。该类型暂不支持在应用内预览，可在右侧"属性"中通过桌面端打开原文。</span>
          </div>
        </div>

        <!-- 可编辑（NOTE / 受管 Markdown）：阅读阶段渲染 Markdown，编辑阶段源码 + 实时预览，自动保存 -->
        <template v-else-if="editable">
          <div v-if="mode === 'read'" class="document-view" data-test="document-view">
            <KnowledgeMarkdownView :source="content" />
          </div>
          <div v-else class="edit-column" data-test="edit-column">
            <div class="md-toolbar" data-test="md-toolbar" role="toolbar" aria-label="Markdown 格式">
              <span class="fmt-group" role="group">
                <button type="button" class="fmt-btn" title="加粗" aria-label="加粗" data-test="fmt-bold" @mousedown.prevent="applyFormat('**', '**', '加粗文字')">B</button>
                <button type="button" class="fmt-btn" title="斜体" aria-label="斜体" data-test="fmt-italic" @mousedown.prevent="applyFormat('*', '*', '斜体文字')">I</button>
                <button type="button" class="fmt-btn" title="删除线" aria-label="删除线" data-test="fmt-strike" @mousedown.prevent="applyFormat('~~', '~~', '删除线文字')">S</button>
              </span>
              <span class="fmt-sep" aria-hidden="true"></span>
              <span class="fmt-group" role="group">
                <button type="button" class="fmt-btn" title="一级标题" aria-label="一级标题" data-test="fmt-h1" @mousedown.prevent="applyFormat('# ', '', '标题')">H1</button>
                <button type="button" class="fmt-btn" title="二级标题" aria-label="二级标题" data-test="fmt-h2" @mousedown.prevent="applyFormat('## ', '', '标题')">H2</button>
                <button type="button" class="fmt-btn" title="三级标题" aria-label="三级标题" data-test="fmt-h3" @mousedown.prevent="applyFormat('### ', '', '标题')">H3</button>
              </span>
              <span class="fmt-sep" aria-hidden="true"></span>
              <span class="fmt-group" role="group">
                <button type="button" class="fmt-btn" title="无序列表" aria-label="无序列表" data-test="fmt-list" @mousedown.prevent="applyFormat('- ', '', '列表项')">•</button>
                <button type="button" class="fmt-btn" title="有序列表" aria-label="有序列表" data-test="fmt-ol" @mousedown.prevent="applyFormat('1. ', '', '列表项')">1.</button>
                <button type="button" class="fmt-btn" title="任务列表" aria-label="任务列表" data-test="fmt-task" @mousedown.prevent="applyFormat('- [ ] ', '', '任务')">☑</button>
              </span>
              <span class="fmt-sep" aria-hidden="true"></span>
              <span class="fmt-group" role="group">
                <button type="button" class="fmt-btn" title="引用" aria-label="引用" data-test="fmt-quote" @mousedown.prevent="applyFormat('> ', '', '引用文字')">❝</button>
                <button type="button" class="fmt-btn" title="行内代码" aria-label="行内代码" data-test="fmt-code" @mousedown.prevent="applyFormat('`', '`', '代码')">&lt;/&gt;</button>
                <button type="button" class="fmt-btn" title="代码块" aria-label="代码块" data-test="fmt-codeblock" @mousedown.prevent="applyFormat('```\n', '\n```', '代码')">代码块</button>
                <button type="button" class="fmt-btn" title="链接" aria-label="链接" data-test="fmt-link" @mousedown.prevent="applyFormat('[', '](https://)', '链接文字')">🔗</button>
                <button type="button" class="fmt-btn" title="分隔线" aria-label="分隔线" data-test="fmt-hr" @mousedown.prevent="applyFormat('\n---\n', '', '')">—</button>
              </span>
            </div>
            <textarea
              v-model="draft"
              ref="editorEl"
              class="body-editor"
              data-test="knowledge-body-editor"
              placeholder="直接输入 Markdown 源码，自动保存…"
              :disabled="saving"
              spellcheck="false"
            ></textarea>
          </div>
          <p v-if="draftTooLarge" class="editor-hint" data-test="note-size-hint">正文不能超过 1 MiB，已停止自动保存</p>
          <p v-if="error" class="editor-error" data-test="note-save-error">{{ error }}</p>
        </template>

        <!-- 只读但有正文（TXT 等）：渲染视图，无编辑入口 -->
        <div v-else class="document-view">
          <KnowledgeMarkdownView :source="content" />
        </div>
      </section>
    </template>
  </main>
</template>
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
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

type SaveStatus = 'idle' | 'saving' | 'saved' | 'error'
const draft = ref('')
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
const bodyEl = ref<HTMLElement | null>(null)
const editorEl = ref<HTMLTextAreaElement | null>(null)
const mode = ref<'read' | 'edit'>('read')
const saveStatus = ref<SaveStatus>('idle')

let saveTimer: number | undefined
let savedFlashTimer: number | undefined

const MAX_BYTES = 1024 * 1024
const AUTO_SAVE_MS = 900
const draftTooLarge = computed(() => new TextEncoder().encode(draft.value).length > MAX_BYTES)
const editable = computed(() => props.document?.sourceType === 'NOTE'
  || (props.document?.sourceType === 'FILE' && props.document.sourceExtension?.toLowerCase() === 'md'))
const metadataOnly = computed(() => props.document?.processingStatus === 'METADATA_ONLY')
const previewType = computed(() => {
  if (!props.document) return null
  const ext = props.document.sourceExtension?.toLowerCase()
  if (ext === 'pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico'].includes(ext ?? '')) return 'image'
  return null
})
const sourceUrl = ref('')
const sourceLoading = ref(false)
const metadataTitle = computed(() => {
  const ext = props.document?.sourceExtension?.toLowerCase()
  return ext === 'pdf' ? 'PDF 文档已收录' : ext === 'doc' ? 'Word 文档已收录' : '资料已收录'
})
const saveStatusText = computed(() => {
  if (saveStatus.value === 'saving') return '保存中…'
  if (saveStatus.value === 'saved') return '已保存'
  if (saveStatus.value === 'error') return '保存失败，请重试'
  return ''
})

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

/** 格式工具栏：在光标处包裹/插入 Markdown 语法，保持焦点与选区。 */
function applyFormat(before: string, after: string, placeholder: string) {
  const el = editorEl.value
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = draft.value.slice(start, end) || placeholder
  draft.value = draft.value.slice(0, start) + before + selected + after + draft.value.slice(end)
  void nextTick(() => {
    el.focus()
    const newStart = start + before.length
    el.setSelectionRange(newStart, newStart + selected.length)
  })
}

/** 进入编辑（阅读阶段 → 编辑阶段）；新建笔记后聚焦标题。 */
async function enterEdit(options: { focusTitle?: boolean } = {}) {
  if (!props.document || !editable.value) return
  mode.value = 'edit'
  await nextTick()
  if (options.focusTitle) {
    titleInput.value?.focus()
    titleInput.value?.select()
  }
}

async function beginEdit(options: { focusTitle?: boolean } = {}) {
  await enterEdit(options)
}

function evaluateSync() {
  if (!props.document) return
  const titleSync = titleDraft.value === props.document.title
  const contentSync = draft.value === props.content
  if (titleSync && contentSync) {
    if (saveStatus.value !== 'idle') {
      saveStatus.value = 'saved'
      if (savedFlashTimer) clearTimeout(savedFlashTimer)
      savedFlashTimer = window.setTimeout(() => { saveStatus.value = 'idle' }, 2000)
    }
  } else {
    saveStatus.value = 'saving'
  }
}

function doSave() {
  if (!props.document || !editable.value) return
  const title = normalizedTitle()
  const content = draft.value
  const titleChanged = title !== props.document.title
  const contentChanged = content !== props.content
  if (!titleChanged && !contentChanged) return
  saveStatus.value = 'saving'
  if (titleChanged) emit('rename-title', title)
  if (contentChanged) emit('save-content', content)
}

function scheduleSave() {
  if (!props.document || !editable.value || props.saving) return
  const title = normalizedTitle()
  if (title === props.document.title && draft.value === props.content) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    saveTimer = undefined
    doSave()
  }, AUTO_SAVE_MS)
}

/** 切换文档前冲刷未保存的自动保存（先落盘再切换，杜绝弹窗）。 */
function flushPendingSave(): Promise<void> {
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = undefined
    doSave()
  }
  if (!props.saving) return Promise.resolve()
  return new Promise((resolve) => {
    const stop = watch(() => props.saving, (s, was) => {
      if (was && !s) {
        stop()
        resolve()
      }
    })
  })
}

function hasUnsavedChanges(): boolean {
  if (!props.document) return false
  return draft.value !== props.content || normalizedTitle() !== props.document.title
}

// 加载来源文件预览（METADATA_ONLY 的 PDF/图片），切换/卸载时回收
async function loadSourcePreview() {
  if (!props.document || !metadataOnly.value || !previewType.value) return
  sourceLoading.value = true
  try {
    const { loadKnowledgeSourceBlob } = await import('../../api/knowledge')
    const url = await loadKnowledgeSourceBlob(props.document.id)
    if (sourceUrl.value) URL.revokeObjectURL(sourceUrl.value)
    sourceUrl.value = url
  } catch {
    // 预览失败保留诚实提示
  } finally {
    sourceLoading.value = false
  }
}
function clearSourcePreview() {
  if (sourceUrl.value) {
    URL.revokeObjectURL(sourceUrl.value)
    sourceUrl.value = ''
  }
}

// 切换文档/首次挂载：重置草稿与模式（新文档默认阅读阶段）
watch(() => props.document?.id, () => {
  draft.value = props.content
  titleDraft.value = props.document?.title ?? ''
  mode.value = 'read'
  saveStatus.value = 'idle'
  clearSourcePreview()
  void loadSourcePreview()
}, { immediate: true })

// 正文异步到达：未改动时同步；已改动绝不覆盖用户输入
watch(() => props.content, (content) => {
  if (draft.value === '' && content !== '') {
    draft.value = content
  }
  evaluateSync()
})

watch(() => props.document?.title, () => {
  evaluateSync()
})

// 自动保存：停止输入 900ms 后落盘
watch(() => [draft.value, titleDraft.value], () => {
  scheduleSave()
})

// 保存结束（内容）：同步成功则闪「已保存」，失败则显示错误
watch(() => props.saving, (s, was) => {
  if (was && !s) evaluateSync()
})

watch(() => props.error, (e) => {
  if (e) saveStatus.value = 'error'
})

watch(() => props.titleError, (e) => {
  if (e) saveStatus.value = 'error'
})

onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
  if (savedFlashTimer) clearTimeout(savedFlashTimer)
  clearSourcePreview()
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

defineExpose({ scrollToLine, beginEdit, enterEdit, flushPendingSave, hasUnsavedChanges })
</script>
<style scoped>
.reading{flex:1;min-width:0;min-height:0;display:flex;flex-direction:column;background:var(--surface-solid);overflow:hidden}
.reading-empty{display:grid;gap:8px;align-content:center;justify-items:start;min-height:100%;padding:48px;color:var(--muted);font-size:13px}
.reading-head{flex:none;padding:20px 32px 12px;border-bottom:1px solid var(--border-subtle)}
.reading-title-row{display:flex;align-items:center;gap:10px}
.title-input{flex:1;min-width:0;border:0;border-bottom:1px solid var(--border-default);background:transparent;color:var(--ink);font-family:inherit;font-size:23px;font-weight:650;line-height:1.35;letter-spacing:-.015em;padding:2px 0 5px}
.title-input:focus{outline:0;border-bottom-color:var(--brand)}
.title-input:disabled{opacity:.6}
.reading-title-row h2{margin:0;font-size:23px;font-weight:650;letter-spacing:-.015em;color:var(--ink)}
.reading-status{flex:none;font-size:11px;padding:2px 9px;border-radius:999px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.mode-toggle{flex:none;display:inline-flex;padding:2px;border:1px solid var(--border-default);border-radius:8px;background:var(--bg-subtle)}
.mode-btn{border:0;background:transparent;color:var(--muted);font-size:12px;padding:4px 10px;border-radius:6px;cursor:pointer;transition:background .14s ease,color .14s ease}
.mode-btn:hover{color:var(--ink)}
.mode-btn.active{background:var(--surface-solid);color:var(--brand);font-weight:600;box-shadow:0 1px 3px rgba(0,0,0,.08)}
.save-status{flex:none;font-size:11.5px;min-width:52px;text-align:right}
.save-status.st-saving{color:var(--muted)}
.save-status.st-saved{color:var(--brand)}
.save-status.st-error{color:var(--danger)}
.inspector-btn{display:inline-flex;align-items:center;gap:5px;border:0;background:transparent;color:var(--muted);font-size:12px;cursor:pointer;flex:none;padding:5px}
.inspector-btn:hover{color:var(--brand)}
.reading-meta{margin:8px 0 0;font-size:12px;color:var(--muted)}
.title-error{margin:8px 0 0;font-size:12px;color:var(--danger)}
.reading-body{flex:1;min-height:0;overflow:auto;padding:0}
.reading-state{display:grid;gap:8px;align-content:center;justify-items:start;min-height:100%;padding:48px 34px;color:var(--muted);font-size:13px}
.reading-state strong{font-size:16px;color:var(--ink)}
.reading-state.error{color:var(--danger)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
.text-btn:disabled{opacity:.5;cursor:default}
/* 阅读阶段：渲染视图 */
.document-view{padding:26px 38px 60px;min-height:100%;box-sizing:border-box}
/* 编辑阶段：源码 + 实时预览（Obsidian 式双阶段） */
.edit-column{display:flex;flex-direction:column;height:100%;min-height:0}
.md-toolbar{flex:none;display:flex;align-items:center;gap:0;padding:7px 34px 5px;border-bottom:1px solid var(--border-subtle);background:var(--surface-solid)}
.fmt-group{display:inline-flex;align-items:center;gap:1px}
.fmt-btn{border:0;background:transparent;color:var(--muted);font-size:11.5px;font-weight:500;min-width:26px;height:24px;padding:0 6px;border-radius:5px;cursor:pointer;font-family:inherit;line-height:1;transition:background .12s ease,color .12s ease}
.fmt-btn:hover{background:var(--bg-hover);color:var(--ink)}
.fmt-btn:active{background:var(--brand-soft);color:var(--brand)}
.fmt-sep{width:1px;height:14px;background:var(--border-subtle);margin:0 7px;flex:none}
.body-editor{box-sizing:border-box;display:block;width:100%;flex:1;min-height:0;padding:22px 38px 40px;border:0;background:transparent;color:var(--ink);font-family:inherit;font-size:16px;line-height:1.85;resize:none;outline:0;caret-color:var(--brand)}
.body-editor:disabled{opacity:.7}
.body-editor::placeholder{color:var(--muted)}
.editor-hint{margin:0 38px 12px;font-size:12.5px;color:var(--danger)}
.editor-error{margin:0 38px 14px;font-size:12.5px;color:var(--danger)}
.source-preview{display:flex;flex-direction:column;height:100%;min-height:0;padding:0}
.preview-frame{flex:1;min-height:0;width:100%;border:0;background:var(--surface-subtle)}
.preview-image{max-width:100%;max-height:100%;object-fit:contain;margin:0 auto;display:block}
</style>