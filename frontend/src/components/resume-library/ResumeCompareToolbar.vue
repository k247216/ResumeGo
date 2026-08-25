<template>
  <div class="compare-toolbar" data-test="compare-toolbar">
    <span v-if="!hasParent" class="compare-note" data-test="initial-version-note">初始版本，无上一版本可比较</span>
    <template v-else>
      <button
        type="button"
        class="compare-toggle"
        :class="{ on: comparing }"
        data-test="toggle-compare"
        :aria-pressed="comparing"
        @click="emit('update:comparing', !comparing)"
      >与 V{{ parentVersionNo }} 对比</button>
      <span class="toolbar-sep" aria-hidden="true"></span>
    </template>
    <span class="toolbar-note">{{ viewingCurrent ? '当前版本' : '历史版本 · 只读' }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  selectedVersionNo: number | null
  viewingCurrent: boolean
  comparing: boolean
}>()

const emit = defineEmits<{ 'update:comparing': [value: boolean] }>()

const hasParent = computed(() => (props.selectedVersionNo ?? 1) > 1)
const parentVersionNo = computed(() => (props.selectedVersionNo ?? 1) - 1)
</script>

<style scoped>
.compare-toolbar{display:flex;align-items:center;gap:10px;flex:0 0 auto}
.compare-toggle{border:1px solid var(--line,rgba(28,31,35,.18));border-radius:8px;background:transparent;color:var(--copy);padding:5px 12px;font-size:12px;font-weight:550;cursor:pointer;transition:all .14s ease-out}
.compare-toggle:hover{border-color:var(--brand);color:var(--brand)}
.compare-toggle.on{border-color:var(--brand);background:var(--brand-soft);color:var(--brand);font-weight:650}
.toolbar-sep{width:1px;height:14px;background:var(--border-subtle)}
.toolbar-note{color:var(--muted);font-size:11.5px}
.compare-note{color:var(--muted);font-size:11.5px}
</style>
