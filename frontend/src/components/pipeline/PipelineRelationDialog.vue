<template>
  <div class="overlay" role="dialog" aria-modal="true" aria-label="管理关联">
    <div class="card">
      <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      <h2>管理关联</h2>
      <p class="desc">为这条求职管线关联或解除日程事项与模拟面试计划。</p>
      <h3 class="group-title">日程事项</h3>
      <div v-if="!scheduleEvents.length" class="row"><span class="grow muted">暂无日程事项，请先到日程页创建。</span></div>
      <div v-for="e in scheduleEvents" :key="e.id" class="row">
        <span class="grow">{{ e.title }}</span>
        <button v-if="isScheduleLinked(e.id)" type="button" class="btn ghost" data-test="pipeline-relation-unlink-schedule" :disabled="busy" @click="$emit('toggle-schedule', e.id, true)">解除</button>
        <button v-else type="button" class="btn primary" data-test="pipeline-relation-link-schedule" :disabled="busy" @click="$emit('toggle-schedule', e.id, false)">关联</button>
      </div>
      <h3 class="group-title">模拟面试计划</h3>
      <div v-if="!interviewPlans.length" class="row"><span class="grow muted">暂无面试计划，请先到面试页创建。</span></div>
      <div v-for="p in interviewPlans" :key="p.id" class="row">
        <span class="grow">{{ p.jobLabel }}</span>
        <button v-if="isInterviewLinked(p.id)" type="button" class="btn ghost" data-test="pipeline-relation-unlink-interview" :disabled="busy" @click="$emit('toggle-interview', p.id, true)">解除</button>
        <button v-else type="button" class="btn primary" data-test="pipeline-relation-link-interview" :disabled="busy" @click="$emit('toggle-interview', p.id, false)">关联</button>
      </div>
      <p v-if="error" class="err">{{ error }}</p>
      <div class="actions"><button type="button" class="btn ghost" @click="$emit('close')">完成</button></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CareerPipeline } from '../../types/pipeline'

const props = defineProps<{
  pipeline: CareerPipeline
  scheduleEvents: Array<{ id: number; title: string }>
  interviewPlans: Array<{ id: number; jobLabel: string }>
  busy: boolean
  error: string
}>()
defineEmits<{
  (e: 'close'): void
  (e: 'toggle-schedule', eventId: number, linked: boolean): void
  (e: 'toggle-interview', planId: number, linked: boolean): void
}>()

function isScheduleLinked(id: number) { return props.pipeline.scheduleEventIds.includes(id) }
function isInterviewLinked(id: number) { return props.pipeline.interviewPlanIds.includes(id) }
</script>

<style scoped>
.overlay{position:fixed;z-index:2000;inset:0;display:grid;place-items:center;padding:24px;background:rgba(7,8,8,.5)}
.card{width:min(520px,100%);padding:26px;border:1px solid var(--line,#dce4e2);border-radius:18px;background:var(--surface-solid,#fff);color:var(--ink,#26343d);max-height:86vh;overflow-y:auto}
.card h2{margin:0 0 6px;font-size:20px;font-weight:650}
.card .desc{margin:0 0 18px;color:var(--muted,#687586);font-size:13px;line-height:1.6}
.field{display:grid;gap:6px;margin-bottom:14px}
.field label{font-size:13px;font-weight:600;color:var(--copy,#42515c)}
.field input,.field select,.field textarea{box-sizing:border-box;width:100%;padding:9px 11px;border:1px solid var(--line,#d8e0e1);border-radius:10px;background:var(--bg-surface,#fff);color:var(--ink,#26343d);font:inherit;font-size:14px;resize:vertical}
.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}
.btn{border:0;border-radius:10px;padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer}
.btn.ghost{background:transparent;color:var(--copy,#42515c);border:1px solid var(--line,#d8e0e1)}
.btn.primary{background:var(--brand,#168866);color:#fff}
.btn.primary:disabled{opacity:.5;cursor:not-allowed}
.err{margin:10px 0 0;color:var(--danger,#b53c32);font-size:12px}
.close-x{position:absolute;top:14px;right:16px;border:0;background:none;color:var(--muted,#687586);font-size:20px;cursor:pointer}
.row{display:flex;align-items:center;gap:10px;padding:9px 2px;border-bottom:1px solid var(--line,#eef1f0);font-size:13px}
.row .grow{flex:1}
</style>