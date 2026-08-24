<template>
  <section class="identity-panel" data-test="pipeline-identity">
    <div class="identity-head">
      <div>
        <p class="kicker">{{ pipeline.lifecycle }}</p>
        <h2 data-test="pipeline-title">{{ pipeline.name }}</h2>
        <p class="role">{{ pipeline.companyName }} · {{ pipeline.roleTitle }}</p>
      </div>
      <div class="identity-actions">
        <button v-if="editable" type="button" class="soft-btn" data-test="pipeline-edit-open" @click="$emit('edit')">编辑信息</button>
        <button v-if="pipeline.lifecycle === 'ACTIVE' || pipeline.lifecycle === 'PAUSED'" type="button" class="soft-btn" data-test="pipeline-archive" @click="$emit('archive')">归档</button>
        <button v-if="pipeline.lifecycle === 'ARCHIVED'" type="button" class="soft-btn" data-test="pipeline-restore" @click="$emit('restore')">恢复</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CareerPipeline } from '../../types/pipeline'

const props = defineProps<{ pipeline: CareerPipeline; isBusy: boolean }>()
defineEmits<{ (e: 'edit'): void; (e: 'archive'): void; (e: 'restore'): void }>()

const editable = computed(() => props.pipeline.lifecycle === 'ACTIVE' || props.pipeline.lifecycle === 'PAUSED')
</script>

<style scoped>
.identity-panel{padding:6px 2px 18px;border-bottom:1px solid var(--border-subtle)}
.identity-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}
.kicker{margin:0 0 6px;color:var(--brand);font-size:11px;font-weight:700;letter-spacing:.1em}
.identity-head h2{margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.02em}
.role{margin:6px 0 0;color:var(--muted);font-size:13px}
.identity-actions{display:flex;gap:8px;flex-shrink:0}
.soft-btn{padding:7px 12px;border:1px solid var(--border-default);border-radius:10px;background:var(--bg-surface);color:var(--copy);font-size:12px;font-weight:600;cursor:pointer}
.soft-btn:hover{border-color:var(--brand);color:var(--brand)}
</style>