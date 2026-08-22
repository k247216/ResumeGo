<template>
  <section class="relations-panel" data-test="pipeline-relations">
    <div class="section-head"><h3 class="section-title">关联事项</h3></div>
    <div class="relation-row">
      <span class="label">日程</span>
      <span class="value">{{ scheduleCount }} 项</span>
    </div>
    <div class="relation-row">
      <span class="label">模拟面试</span>
      <span class="value">{{ interviewCount }} 项</span>
    </div>
    <div class="relation-actions">
      <button type="button" class="soft-btn" data-test="pipeline-manage-relations" @click="$emit('manage')">管理关联</button>
      <button type="button" class="soft-btn" data-test="pipeline-go-schedule" @click="$emit('go-schedule')">查看全部日程 →</button>
      <button type="button" class="soft-btn" data-test="pipeline-go-interview" @click="$emit('go-interview')">查看全部面试 →</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'

const props = defineProps<{
  pipeline: CareerPipeline
  scheduleEvents: Array<{ id: number; title: string }>
  interviewPlans: Array<{ id: number; jobLabel: string }>
}>()
defineEmits<{ (e: 'manage'): void; (e: 'go-schedule'): void; (e: 'go-interview'): void }>()

const scheduleCount = computed(() => props.scheduleEvents.filter((e) => props.pipeline.scheduleEventIds.includes(e.id)).length)
const interviewCount = computed(() => props.interviewPlans.filter((p) => props.pipeline.interviewPlanIds.includes(p.id)).length)
</script>

<style scoped>
.relations-panel{padding:18px 2px;border:1px solid var(--border-subtle);border-radius:16px;background:var(--bg-surface);display:flex;flex-direction:column;gap:12px}
.section-head{margin-bottom:2px}
.section-title{margin:0;font-size:13px;font-weight:600;color:var(--ink)}
.relation-row{display:flex;gap:10px;align-items:baseline;font-size:13px}
.label{flex:0 0 72px;color:var(--muted)}
.value{color:var(--ink)}
.relation-actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:4px}
.soft-btn{padding:6px 10px;border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);color:var(--copy);font-size:12px;cursor:pointer}
.soft-btn:hover{border-color:var(--brand);color:var(--brand)}
</style>