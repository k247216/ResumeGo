<template>
  <div class="overlay" role="dialog" aria-modal="true" aria-label="推进阶段">
    <div class="card">
      <button type="button" class="close-x" aria-label="关闭" @click="$emit('close')">×</button>
      <h2>推进阶段</h2>
      <p class="desc">将 <strong>{{ current?.name }}</strong> 推进到 <strong>{{ next?.name }}</strong>。可填写一条说明，留空也可以。</p>
      <div class="field"><label>说明（可选）</label><textarea v-model="note" data-test="pipeline-transition-note" rows="3" maxlength="300" placeholder="如：进入技术面"></textarea></div>
      <p v-if="error" class="err">{{ error }}</p>
      <div class="actions">
        <button type="button" class="btn ghost" @click="$emit('close')">取消</button>
        <button type="button" class="btn primary" :disabled="busy || !next" data-test="pipeline-transition-confirm" @click="confirm">确认推进</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'

const props = defineProps<{ pipeline: CareerPipeline; busy: boolean; error: string }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'confirm', note: string | null): void }>()

const note = ref('')
const current = computed(() => props.pipeline.stages.find((s) => s.state === 'CURRENT') ?? null)
const next = computed(() => props.pipeline.stages.find((s) => s.state === 'PENDING') ?? null)
function confirm() { emit('confirm', note.value.trim() ? note.value.trim() : null) }
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