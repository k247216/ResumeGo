<template>
  <aside class="change-summary" data-test="change-summary">
    <div class="summary-head" data-test="change-summary-head">
      <strong>{{ changes.length }} 处更新</strong>
      <span>相对于 V{{ parentVersionNo }}</span>
    </div>
    <p v-if="!changes.length" class="change-empty" data-test="change-none">与上一版本内容一致</p>
    <ol v-else class="change-list">
      <li v-for="(change, index) in changes" :key="change.chapterKey + change.changeType" class="change-row">
        <span class="change-num" :class="change.changeType">{{ index + 1 }}</span>
        <span class="change-copy">
          <strong>{{ change.chapterLabel }}</strong>
          <small>{{ typeLabel(change.changeType) }}</small>
        </span>
      </li>
    </ol>
  </aside>
</template>

<script setup lang="ts">
import type { ResumeChapterChange } from '../../utils/resumeVersionDiff'

defineProps<{
  changes: ResumeChapterChange[]
  parentVersionNo: number
}>()

function typeLabel(type: ResumeChapterChange['changeType']) {
  if (type === 'added') return '新增章节'
  if (type === 'removed') return '已移除'
  return '内容已更新'
}
</script>

<style scoped>
.change-summary{align-self:stretch;width:176px;flex:0 0 auto;border-right:1px solid var(--border-subtle);padding:4px 14px 6px 0}
.summary-head{display:grid;gap:2px;padding:2px 0 12px}
.summary-head strong{font-size:14px;font-weight:700;color:var(--ink)}
.summary-head span{font-size:10.5px;color:var(--muted)}
.change-empty{margin:0;color:var(--muted);font-size:11.5px;line-height:1.6}
.change-list{margin:0;padding:0;list-style:none;display:grid;gap:12px}
.change-row{display:flex;align-items:flex-start;gap:9px}
.change-num{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;font-size:10px;font-weight:700;flex:0 0 auto;margin-top:1px}
.change-num.added{background:var(--brand-soft);color:var(--brand)}
.change-num.modified{background:rgba(217,119,6,.12);color:#b45309}
.change-num.removed{background:rgba(194,86,86,.12);color:#c25656}
.change-copy{display:grid;gap:1px;min-width:0}
.change-copy strong{font-size:12px;font-weight:600;color:var(--copy)}
.change-copy small{font-size:10.5px;color:var(--muted)}
</style>
