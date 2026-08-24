<template>
  <div class="overlay" role="dialog" aria-modal="true" aria-label="管理阶段">
    <div class="card">
      <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      <h2>管理阶段</h2>
      <p class="desc">新增、重命名或调整阶段顺序。当前阶段以突出样式展示，只有待进入阶段可推进。</p>
      <div class="add-row">
        <input v-model="newName" data-test="pipeline-stage-new" placeholder="新阶段名称" maxlength="120" />
        <button type="button" class="btn primary" :disabled="busy || !newName.trim()" data-test="pipeline-stage-add" @click="add">添加</button>
      </div>
      <div v-for="(s, idx) in pipeline.stages" :key="s.id" class="row">
        <input v-model="renames[s.id]" class="grow" data-test="pipeline-stage-rename" />
        <button type="button" class="btn ghost" :disabled="busy" data-test="pipeline-stage-rename-save" @click="rename(s.id)">保存</button>
        <button type="button" class="btn ghost" :disabled="busy || idx === 0" data-test="pipeline-stage-up" @click="$emit('move', s.id, 'up')">↑</button>
        <button type="button" class="btn ghost" :disabled="busy || idx === pipeline.stages.length - 1" data-test="pipeline-stage-down" @click="$emit('move', s.id, 'down')">↓</button>
      </div>
      <p v-if="error" class="err">{{ error }}</p>
      <div class="actions"><button type="button" class="btn ghost" @click="$emit('close')">完成</button></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'

const props = defineProps<{ pipeline: CareerPipeline; busy: boolean; error: string }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'add', name: string): void
  (e: 'rename', stageId: number, name: string): void
  (e: 'move', stageId: number, direction: 'up' | 'down'): void
}>()

const newName = ref('')
const renames = reactive<Record<number, string>>({})
watch(() => props.pipeline, (p) => {
  for (const s of p.stages) renames[s.id] = s.name
}, { immediate: true, deep: true })

function add() { emit('add', newName.value.trim()); newName.value = '' }
function rename(stageId: number) { const n = renames[stageId]?.trim(); if (n) emit('rename', stageId, n) }
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