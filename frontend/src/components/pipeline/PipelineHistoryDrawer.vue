<template>
  <div class="drawer-overlay" role="dialog" aria-modal="true" aria-label="阶段历史">
    <aside class="drawer" data-test="pipeline-history-drawer">
      <div class="drawer-head">
        <h2>阶段历史</h2>
        <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      </div>
      <p v-if="loading" class="note">正在加载…</p>
      <p v-else-if="error" class="note error">{{ error }} <button type="button" class="text-btn" @click="$emit('load')">重试</button></p>
      <p v-else-if="!history.length" class="note">暂无阶段变化记录。</p>
      <ol v-else class="history-list">
        <li v-for="h in history" :key="h.id" class="history-item">
          <span class="history-dot"></span>
          <div class="history-copy">
            <strong>{{ stageName(h.toStageId) }}</strong>
            <span>{{ formatTime(h.occurredAt) }} · {{ h.actor }}</span>
            <em v-if="h.note">{{ h.note }}</em>
          </div>
        </li>
      </ol>
    </aside>
  </div>
</template>

<script setup lang="ts">
import type { CareerPipeline, PipelineStageTransition } from '../../types/pipeline'

defineProps<{
  pipeline: CareerPipeline
  history: PipelineStageTransition[]
  loading: boolean
  error: string
}>()
defineEmits<{ (e: 'close'): void; (e: 'load'): void }>()

function stageName(id: number) {
  return '阶段 #' + id
}
function formatTime(iso: string) {
  if (!iso) return '时间未知'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}
</script>

<style scoped>
.drawer-overlay{position:fixed;z-index:2000;inset:0;background:rgba(7,8,8,.35);display:flex;justify-content:flex-end}
.drawer{width:min(420px,92vw);height:100%;background:var(--surface-solid,#fff);border-left:1px solid var(--line,#dce4e2);padding:22px 24px;overflow-y:auto}
.drawer-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
.drawer-head h2{margin:0;font-size:18px;font-weight:650;color:var(--ink)}
.close-x{border:0;background:none;color:var(--muted);font-size:22px;cursor:pointer}
.note{margin:20px 0;color:var(--muted);font-size:13px}
.note.error{color:var(--danger)}
.text-btn{border:0;background:none;color:var(--brand);cursor:pointer;font-size:12px}
.history-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column}
.history-item{display:flex;gap:12px;padding:12px 2px;border-left:2px solid var(--line,#e5e9e7);position:relative}
.history-dot{width:10px;height:10px;border-radius:50%;background:var(--brand);flex-shrink:0;margin-top:4px;margin-left:-6px}
.history-copy{display:grid;gap:2px}
.history-copy strong{font-size:14px;color:var(--ink)}
.history-copy span{font-size:11px;color:var(--muted)}
.history-copy em{font-style:normal;font-size:13px;color:var(--copy);margin-top:4px}
</style>