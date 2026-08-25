<template>
  <aside class="change-summary" data-test="change-summary">
    <button type="button" class="summary-head" data-test="toggle-summary" @click="collapsed = !collapsed">
      <span>相对上一版本</span>
      <em>{{ collapsed ? '展开' : '收起' }}</em>
    </button>
    <div v-if="!collapsed" class="summary-body">
      <p v-if="!hasParent" class="change-empty" data-test="change-initial">初始版本，无上一版本可比较</p>
      <p v-else-if="!changes.length" class="change-empty" data-test="change-none">与上一版本内容一致</p>
      <ul v-else class="change-list">
        <li v-for="change in changes" :key="change.chapterKey + change.changeType" class="change-row" :data-test="`change-${change.changeType}`">
          <span class="change-dot" :class="change.changeType" aria-hidden="true"></span>
          <span class="change-label">{{ change.chapterLabel }}</span>
          <span class="change-type">{{ typeLabel(change.changeType) }}</span>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ResumeChapterChange } from '../../utils/resumeVersionDiff'

defineProps<{
  changes: ResumeChapterChange[]
  hasParent: boolean
}>()

const collapsed = ref(false)

function typeLabel(type: ResumeChapterChange['changeType']) {
  if (type === 'added') return '新增'
  if (type === 'removed') return '删除'
  return '修改'
}
</script>

<style scoped>
.change-summary{align-self:flex-start;width:170px;flex:0 0 auto;border-right:1px solid var(--border-subtle);padding:6px 14px 6px 0}
.summary-head{display:flex;align-items:baseline;justify-content:space-between;width:100%;border:0;background:none;padding:0 0 8px;font-size:10.5px;font-weight:650;letter-spacing:.06em;color:var(--muted);cursor:pointer}
.summary-head em{font-style:normal;font-size:10.5px}
.summary-body{display:grid;gap:6px}
.change-empty{margin:0;color:var(--muted);font-size:11.5px;line-height:1.6}
.change-list{margin:0;padding:0;list-style:none;display:grid;gap:6px}
.change-row{display:flex;align-items:center;gap:7px;font-size:11.5px}
.change-dot{width:7px;height:7px;border-radius:50%;flex:0 0 auto}
.change-dot.added{background:var(--brand)}
.change-dot.modified{background:var(--warning,#d97706)}
.change-dot.removed{background:#c25656}
.change-label{flex:1;color:var(--copy);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.change-type{color:var(--muted);font-size:10.5px}
</style>
