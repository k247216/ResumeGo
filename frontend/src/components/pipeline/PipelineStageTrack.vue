<template>
  <section class="stage-track" data-test="pipeline-stage-track">
    <div class="section-head">
      <h3 class="section-title">阶段</h3>
      <div class="head-actions">
        <button type="button" class="soft-btn" data-test="pipeline-history-open" @click="$emit('load-history'); historyOpen = true">查看阶段历史</button>
        <button v-if="editable" type="button" class="soft-btn" data-test="pipeline-manage-stages" @click="$emit('manage-stages')">管理阶段</button>
      </div>
    </div>

    <ol class="stage-list" data-test="pipeline-stages">
      <li
        v-for="(stage, idx) in pipeline.stages"
        :key="stage.id"
        class="stage-row"
        :class="{ current: stage.state === 'CURRENT', completed: stage.state === 'COMPLETED' || stage.state === 'SKIPPED' }"
      >
        <span class="stage-node"><i>{{ idx + 1 }}</i></span>
        <div class="stage-copy">
          <strong>{{ stage.name }}</strong>
          <em>{{ stateLabel(stage.state) }}</em>
        </div>
        <button
          v-if="stage.state === 'PENDING' && editable"
          type="button"
          class="advance-btn"
          data-test="pipeline-advance"
          @click="$emit('transition')"
        >推进 →</button>
      </li>
    </ol>

    <p v-if="historyLoading" class="history-note">正在加载阶段历史…</p>
    <p v-else-if="historyError" class="history-note error">{{ historyError }}</p>
    <p v-else-if="history.length" class="history-note">共 {{ history.length }} 次阶段变化，点击「查看阶段历史」查看详情。</p>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CareerPipeline, PipelineStageTransition } from '../../types/pipeline'

const props = defineProps<{
  pipeline: CareerPipeline
  history: PipelineStageTransition[]
  historyLoading: boolean
  historyError: string
}>()
defineEmits<{ (e: 'transition'): void; (e: 'manage-stages'): void; (e: 'load-history'): void }>()

const historyOpen = ref(false)
const editable = computed(() => props.pipeline.lifecycle === 'ACTIVE' || props.pipeline.lifecycle === 'PAUSED')

function stateLabel(s: string) {
  return ({ CURRENT: '当前', COMPLETED: '已完成', SKIPPED: '已跳过', PENDING: '待进入' } as Record<string, string>)[s] ?? s
}
</script>

<style scoped>
.stage-track{padding:18px 2px;border-bottom:1px solid var(--border-subtle)}
.section-head{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:10px}
.section-title{margin:0;font-size:13px;font-weight:600;color:var(--ink)}
.head-actions{display:flex;gap:8px}
.soft-btn{padding:6px 10px;border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);color:var(--copy);font-size:12px;cursor:pointer}
.soft-btn:hover{border-color:var(--brand);color:var(--brand)}
.stage-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px}
.stage-row{display:flex;align-items:center;gap:12px;padding:9px 10px;border-radius:11px;background:var(--bg-surface)}
.stage-row.current{border:1px solid var(--brand);background:var(--brand-soft)}
.stage-node{display:grid;place-items:center;width:28px;height:28px;border-radius:9px;background:var(--bg-subtle);color:var(--muted);font-size:12px;font-weight:700;flex-shrink:0}
.stage-row.current .stage-node{background:var(--brand);color:#fff}
.stage-copy{flex:1;min-width:0}
.stage-copy strong{display:block;font-size:14px;color:var(--ink)}
.stage-copy em{font-style:normal;font-size:11px;color:var(--muted)}
.stage-row.completed .stage-copy strong{color:var(--muted);text-decoration:line-through}
.advance-btn{padding:6px 12px;border:0;border-radius:9px;background:var(--brand);color:#fff;font-size:12px;font-weight:600;cursor:pointer}
.history-note{margin:10px 0 0;font-size:12px;color:var(--muted)}
.history-note.error{color:var(--danger)}
</style>