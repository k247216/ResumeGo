<template>
  <div class="version-rail" data-test="version-rail" role="tablist" aria-label="版本历史">
    <div class="rail-scroll">
      <div class="rail-track">
        <button
          v-for="version in orderedVersions"
          :key="version.id"
          type="button"
          class="rail-node"
          :class="{ current: version.id === selectedVersionId, isCurrentVersion: version.id === currentVersionId }"
          :data-test="`rail-version-${version.versionNo}`"
          role="tab"
          :aria-selected="version.id === selectedVersionId"
          :title="version.changeSummary || ''"
          @click="emit('select-version', version.id)"
        >
          <span class="node-label">V{{ version.versionNo }}</span>
          <span class="node-dot" aria-hidden="true"></span>
          <span class="node-date">{{ shortDate(version.createdAt) }}</span>
        </button>
      </div>
    </div>
    <span v-if="selectedVersion && !selectedVersionIsCurrent" class="readonly-badge" data-test="rail-readonly-badge">只读预览</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeVersion } from '../../types/resume'

const props = defineProps<{
  versions: ResumeVersion[]
  selectedVersionId: number | null
  currentVersionId: number | null
}>()

const emit = defineEmits<{ 'select-version': [id: number] }>()

/** 轨道按 versionNo 单调递增，不依赖接口返回顺序 */
const orderedVersions = computed(() =>
  [...props.versions].sort((left, right) => left.versionNo - right.versionNo))

const selectedVersion = computed(() =>
  props.versions.find((version) => version.id === props.selectedVersionId) ?? null)
const selectedVersionIsCurrent = computed(() =>
  selectedVersion.value?.id === props.currentVersionId)

function shortDate(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}/${date.getDate()}`
}
</script>

<style scoped>
.version-rail{position:relative;display:flex;align-items:center;gap:14px;height:92px;flex:0 0 auto}
.rail-scroll{flex:1;min-width:0;overflow-x:auto;overflow-y:hidden;padding:4px 2px}
.rail-track{position:relative;display:inline-flex;gap:44px;padding:0 10px}
.rail-track::before{content:'';position:absolute;left:14px;right:14px;top:50%;height:2px;background:var(--line-subtle,rgba(28,31,35,.14))}
.rail-node{position:relative;display:grid;justify-items:center;gap:5px;border:0;background:none;padding:2px 6px;cursor:pointer;min-width:56px}
.node-dot{width:12px;height:12px;border-radius:50%;background:var(--surface-solid,#fff);border:2px solid var(--line,rgba(28,31,35,.35));transition:all .15s ease-out;z-index:1}
.rail-node:hover .node-dot{border-color:var(--brand)}
.rail-node.current .node-dot{width:14px;height:14px;background:var(--brand);border-color:var(--brand);box-shadow:0 0 0 4px var(--brand-soft)}
.node-label{font-size:13.5px;font-weight:700;color:var(--copy);font-variant-numeric:tabular-nums}
.rail-node.current .node-label{color:var(--ink);font-weight:700}
.node-date{font-size:11px;color:var(--muted);white-space:nowrap;font-variant-numeric:tabular-nums}
.readonly-badge{flex:0 0 auto;padding:2px 9px;border-radius:999px;background:var(--bg-subtle);color:var(--muted);font-size:10.5px;font-weight:600}
</style>
