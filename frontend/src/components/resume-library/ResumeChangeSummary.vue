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
        <el-icon class="change-icon" :size="12" aria-hidden="true"><Document /></el-icon>
        <span class="change-copy">
          <strong>{{ change.chapterLabel }}</strong>
          <small>{{ typeLabel(change.changeType) }}</small>
          <small v-for="detail in detailLines(change)" :key="detail">{{ detail }}</small>
        </span>
      </li>
    </ol>
  </aside>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
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

function detailLines(change: ResumeChapterChange) {
  const values = change.details?.map((detail) => detail.value).filter(Boolean) ?? []
  if (!values.length) return []
  const rows = values.slice(0, 2).map((value) => {
    const normalized = value.replace(/\s+/g, ' ')
    return normalized.length > 30 ? `${normalized.slice(0, 30)}…` : normalized
  })
  if (values.length > 2) rows.push(`等 ${values.length} 处内容`)
  return rows
}
</script>

<style scoped>
.change-summary{position:sticky;top:0;align-self:start;width:112px;flex:0 0 auto;padding:2px 8px 4px 0}
.summary-head{display:grid;gap:1px;padding:1px 0 8px}
.summary-head strong{font-size:14px;font-weight:700;color:var(--ink)}
.summary-head span{font-size:10.5px;color:var(--muted)}
.change-empty{margin:0;color:var(--muted);font-size:11.5px;line-height:1.6}
.change-list{margin:0;padding:0;list-style:none;display:grid;gap:8px}
.change-row{display:flex;align-items:flex-start;gap:6px}
.change-num{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;font-size:10px;font-weight:700;flex:0 0 auto;margin-top:1px}
.change-num.added{background:var(--brand-soft);color:var(--brand)}
.change-num.modified{background:rgba(217,119,6,.12);color:#b45309}
.change-num.removed{background:rgba(194,86,86,.12);color:#c25656}
.change-icon{flex:0 0 auto;margin-top:4px;color:var(--muted)}
.change-copy{display:grid;gap:0;min-width:0;line-height:1.25}
.change-copy strong{font-size:11.5px;font-weight:600;color:var(--copy)}
.change-copy small{font-size:10px;color:var(--muted);line-height:1.25}
</style>
