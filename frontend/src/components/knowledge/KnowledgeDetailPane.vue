<template>
  <div class="detail" data-test="knowledge-detail">
    <div v-if="!document" class="detail-empty">
      <template v-if="!hasDocuments">
        <strong>还没有资料</strong>
        <span>新建一条笔记或导入 .md/.txt，开始积累你的本地知识库。</span>
        <button type="button" class="state-primary" data-test="knowledge-detail-empty-create" @click="$emit('create-note')">新建笔记</button>
      </template>
      <template v-else>
        <strong>选择一份资料</strong>
        <span>从左侧列表选择要查看的笔记或文件。</span>
      </template>
    </div>

    <template v-else>
      <header class="detail-head">
        <h2>{{ document.title }}</h2>
        <p class="meta">
          <span class="kind">{{ document.sourceType === 'FILE' ? '文件' : '笔记' }}</span>
          <span class="status" :class="statusTone(document.processingStatus)">{{ knowledgeStatusLabel(document.processingStatus) }}</span>
        </p>
      </header>

      <section class="content-area">
        <template v-if="document.processingStatus === 'COMPLETED'">
          <div v-if="contentLoading" class="content-state" data-test="knowledge-content-loading">正在读取正文…</div>
          <div v-else-if="contentError" class="content-state error" data-test="knowledge-content-error">
            <strong>正文读取失败</strong>
            <span>{{ contentError }}</span>
            <button type="button" class="text-btn" data-test="knowledge-content-retry" @click="$emit('load-content')">重试</button>
          </div>
          <pre v-else class="content" data-test="knowledge-content">{{ content }}</pre>
        </template>

        <div v-else-if="document.processingStatus === 'FAILED'" class="content-state error" data-test="knowledge-content-failed">
          <strong>处理失败</strong>
          <span>文件未能提取文本，原始副本仍保留在本设备。</span>
        </div>

        <div v-else-if="document.processingStatus === 'PENDING' || document.processingStatus === 'RUNNING'" class="content-state" data-test="knowledge-content-pending">
          <strong>内容仍在处理中</strong>
          <span>当前没有自动刷新，处理完成后点击顶部“刷新”查看提取结果。</span>
        </div>

        <div v-else class="content-state" data-test="knowledge-content-not-started">
          <strong>笔记已保存</strong>
          <span>当前已保存笔记标题，正文编辑将在后续版本开放。</span>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { KnowledgeDocument, KnowledgeProcessingStatus } from '../../types/knowledge'
import { knowledgeStatusLabel } from './status'

defineProps<{
  document: KnowledgeDocument | null
  hasDocuments: boolean
  content: string
  contentLoading: boolean
  contentError: string
}>()

defineEmits<{
  (e: 'create-note'): void
  (e: 'load-content'): void
}>()

function statusTone(status: KnowledgeProcessingStatus): string {
  if (status === 'FAILED') return 'tone-danger'
  if (status === 'COMPLETED') return 'tone-ok'
  if (status === 'PENDING' || status === 'RUNNING') return 'tone-busy'
  return 'tone-idle'
}
</script>

<style scoped>
.detail{flex:1;display:flex;flex-direction:column;min-width:0;min-height:0;padding:22px 26px 16px;overflow-y:auto}
.detail-empty{display:grid;gap:8px;justify-items:start;align-content:center;flex:1;color:var(--muted);font-size:13px}
.detail-empty strong{color:var(--ink);font-size:15px}
.detail-head{flex:none;padding-bottom:14px;border-bottom:1px solid var(--border-subtle)}
.detail-head h2{margin:0;font-size:19px;font-weight:650;color:var(--ink);word-break:break-word}
.meta{display:flex;align-items:center;gap:8px;margin:8px 0 0}
.kind{font-size:12px;color:var(--muted)}
.status{display:inline-block;padding:1px 8px;border-radius:999px;font-size:11px}
.tone-ok{color:var(--brand);background:var(--brand-soft)}
.tone-busy{color:var(--copy);background:var(--bg-subtle)}
.tone-danger{color:var(--danger);background:var(--danger-soft)}
.tone-idle{color:var(--muted);background:var(--bg-subtle)}
.content-area{flex:1;min-height:0;padding-top:16px}
.content{margin:0;white-space:pre-wrap;word-break:break-word;color:var(--copy);font-size:13px;line-height:1.7}
.content-state{display:grid;gap:8px;justify-items:start;padding:16px 0;color:var(--muted);font-size:13px}
.content-state strong{color:var(--ink);font-size:14px}
.content-state.error strong{color:var(--danger)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
.state-primary{display:inline-flex;align-items:center;gap:6px;margin-top:6px;padding:9px 14px;border:0;border-radius:11px;background:var(--action-bg);color:var(--action-fg);font-size:13px;font-weight:600;cursor:pointer}
</style>
