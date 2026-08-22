<template>
  <main class="reading" data-test="knowledge-reading-pane">
    <template v-if="!document">
      <div class="reading-empty">
        <strong>选择一份资料</strong>
        <span>从左侧选择要阅读的笔记或文件，或导入 .md/.txt 开始。</span>
      </div>
    </template>
    <template v-else>
      <header class="reading-head">
        <div class="reading-title-row">
          <h2>{{ document.title }}</h2>
          <span class="reading-status" :class="tone(document.processingStatus)">{{ statusLabel(document.processingStatus) }}</span>
          <button type="button" class="inspector-btn" data-test="reading-open-inspector" aria-label="打开来源与属性" @click="$emit('open-inspector')">
            <el-icon :size="14"><Setting /></el-icon><span>来源与属性</span>
          </button>
        </div>
        <p class="reading-meta">{{ metaLine }}</p>
      </header>

      <section class="reading-body" ref="bodyEl">
        <!-- NOTE：编辑模式明确保存 -->
        <template v-if="document.sourceType === 'NOTE'">
          <div v-if="editing" class="note-editor">
            <textarea
              v-model="draft"
              class="note-textarea"
              :data-test="'note-editor-' + document.id"
              placeholder="输入笔记正文…"
              :disabled="saving"
            ></textarea>
            <div class="note-actions">
              <button type="button" class="text-btn" data-test="note-edit-cancel" :disabled="saving" @click="editing = false">取消</button>
              <button type="button" class="save-btn" data-test="note-edit-save" :disabled="saving || draftTooLarge" @click="saveDraft">
                {{ saving ? '保存中…' : '保存正文' }}
              </button>
              <span v-if="draftTooLarge" class="note-hint" data-test="note-size-hint">正文不能超过 1 MiB</span>
            </div>
            <p v-if="error" class="note-error" data-test="note-save-error">{{ error }}</p>
          </div>
          <div v-else class="note-view">
            <KnowledgeMarkdownView :source="content" />
            <button type="button" class="edit-btn" data-test="note-edit-start" @click="startEdit">编辑正文</button>
          </div>
        </template>

        <!-- FILE：只读 -->
        <template v-else>
          <div v-if="contentLoading" class="reading-state">正在读取正文…</div>
          <div v-else-if="contentError" class="reading-state error" data-test="file-content-error">
            <span>{{ contentError }}</span>
            <button type="button" class="text-btn" data-test="file-content-retry" @click="$emit('load-content')">重试</button>
          </div>
          <KnowledgeMarkdownView v-else :source="content" />
        </template>
      </section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { Setting } from '@element-plus/icons-vue'
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
}>()

const emit = defineEmits<{
  (e: 'open-inspector'): void
  (e: 'load-content'): void
  (e: 'save-content', content: string): void
}>()

const editing = ref(false)
const draft = ref('')
const bodyEl = ref<HTMLElement | null>(null)

const MAX_BYTES = 1024 * 1024
const draftTooLarge = computed(() => new TextEncoder().encode(draft.value).length > MAX_BYTES)

const metaLine = computed(() => {
  if (!props.document) return ''
  const updated = new Date(props.document.updatedAt).toLocaleString()
  return `${props.document.sourceType === 'NOTE' ? '本地笔记' : '本地文件'} · 更新于 ${updated}`
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

function startEdit() {
  draft.value = props.content
  editing.value = true
}

function saveDraft() {
  if (draftTooLarge.value || props.saving) return
  emit('save-content', draft.value)
}

// 切换文档时重置编辑状态
watch(() => props.document?.id, () => {
  editing.value = false
  draft.value = ''
})

/** 搜索定位：把正文滚动到命中行（按行号估算）。 */
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

defineExpose({ scrollToLine })
</script>

<style scoped>
.reading{flex:1;min-width:0;min-height:0;display:flex;flex-direction:column;overflow-y:auto}
.reading-empty{display:grid;gap:8px;align-content:center;justify-items:start;padding:40px;color:var(--muted);font-size:13px}
.reading-empty strong{color:var(--ink);font-size:15px}
.reading-head{padding:20px 28px 14px;border-bottom:1px solid var(--border-subtle)}
.reading-title-row{display:flex;align-items:center;gap:10px}
.reading-title-row h2{margin:0;font-size:19px;font-weight:650;color:var(--ink);word-break:break-word;flex:1;min-width:0}
.reading-status{flex:none;font-size:11px;padding:2px 9px;border-radius:999px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.inspector-btn{display:inline-flex;align-items:center;gap:4px;border:0;background:transparent;color:var(--copy);font-size:12px;cursor:pointer;flex:none}
.inspector-btn:hover{color:var(--brand)}
.reading-meta{margin:8px 0 0;color:var(--muted);font-size:12px}
.reading-body{padding:18px 28px 40px;max-width:760px}
.markdown{margin:0;white-space:pre-wrap;word-break:break-word;color:var(--copy);font-size:14px;line-height:1.75}
.reading-state{display:grid;gap:8px;justify-items:start;color:var(--muted);font-size:13px}
.reading-state.error{color:var(--danger)}
.note-view{position:relative}
.edit-btn{margin-top:16px;padding:8px 14px;border:1px solid var(--border-default);border-radius:10px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer}
.edit-btn:hover{border-color:var(--brand);color:var(--brand)}
.note-editor{display:grid;gap:10px}
.note-textarea{min-height:300px;padding:12px;border:1px solid var(--border-default);border-radius:12px;background:var(--bg-subtle);color:var(--ink);font-size:14px;line-height:1.7;resize:vertical;font-family:inherit}
.note-textarea:focus{outline:2px solid var(--brand);border-color:transparent}
.note-actions{display:flex;align-items:center;gap:12px}
.save-btn{padding:8px 16px;border:0;border-radius:10px;background:var(--action-bg);color:var(--action-fg);font-size:13px;font-weight:600;cursor:pointer}
.save-btn:disabled{opacity:.55;cursor:not-allowed}
.note-hint{color:var(--danger);font-size:12px}
.note-error{margin:0;color:var(--danger);font-size:12px}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>
