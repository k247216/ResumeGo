<template>
  <aside class="rail" data-test="pipeline-rail">
    <div v-if="loading && !pipelines.length" class="rail-state">正在读取…</div>
    <div v-else-if="errorMessage && !pipelines.length" class="rail-state error">
      <strong>无法读取求职管线</strong>
      <span>{{ errorMessage }}</span>
      <button type="button" class="text-btn" data-test="pipeline-rail-retry" @click="$emit('retry')">重试</button>
    </div>
    <div v-else-if="!pipelines.length" class="rail-state empty">
      <strong>还没有求职管线</strong>
      <span>创建第一条开始管理机会。</span>
      <button type="button" class="text-btn" data-test="pipeline-rail-create" @click="$emit('create')">新建管线</button>
    </div>
    <ul v-else class="rail-list">
      <li v-for="p in pipelines" :key="p.id">
        <button
          type="button"
          class="rail-item"
          :class="{ selected: p.id === selectedId }"
          :data-test="'pipeline-rail-item-' + p.id"
          @click="$emit('select', p.id)"
        >
          <strong>{{ p.companyName || p.name }}</strong>
          <span>{{ p.roleTitle }}</span>
          <em>{{ stageLabel(p) }}<i v-if="p.lifecycle === 'ARCHIVED'" class="badge">已归档</i></em>
        </button>
      </li>
    </ul>
    <button v-if="pipelines.length" type="button" class="rail-add" data-test="pipeline-rail-add" @click="$emit('create')">＋ 新建求职管线</button>
  </aside>
</template>

<script setup lang="ts">
import type { CareerPipeline } from '../../types/pipeline'

defineProps<{
  pipelines: CareerPipeline[]
  selectedId: number | null
  loading: boolean
  errorMessage: string
}>()

defineEmits<{ (e: 'select', id: number): void; (e: 'retry'): void; (e: 'create'): void }>()

function stageLabel(p: CareerPipeline) {
  const current = p.stages.find((s) => s.state === 'CURRENT')
  return current ? current.name : '无当前阶段'
}
</script>

<style scoped>
.rail{width:280px;min-height:0;display:flex;flex-direction:column;border-right:1px solid var(--border-subtle);padding:14px 10px 20px;overflow-y:auto}
.rail-state{display:grid;gap:8px;justify-items:start;padding:18px 8px;color:var(--muted);font-size:13px}
.rail-state strong{color:var(--ink);font-size:14px}
.rail-state.error strong{color:var(--danger)}
.rail-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:2px}
.rail-item{display:grid;gap:2px;text-align:left;width:100%;padding:10px 12px;border:0;border-radius:12px;background:transparent;color:var(--copy);cursor:pointer}
.rail-item:hover{background:var(--bg-hover)}
.rail-item.selected{background:var(--bg-selected)}
.rail-item strong{font-size:14px;font-weight:600;color:var(--ink)}
.rail-item span{font-size:12px;color:var(--muted)}
.rail-item em{display:flex;align-items:center;gap:6px;font-style:normal;font-size:12px;color:var(--copy)}
.badge{font-size:10px;padding:1px 6px;border-radius:999px;background:var(--bg-subtle);color:var(--muted)}
.rail-add{margin-top:auto;padding:9px 12px;border:1px dashed var(--border-default);border-radius:12px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer}
.rail-add:hover{border-color:var(--brand);color:var(--brand)}
.text-btn{border:0;background:transparent;color:var(--brand);font-size:13px;cursor:pointer;padding:0}
</style>