<template>
  <div class="version-rail" data-test="version-rail" role="tablist" aria-label="版本历史">
    <div class="rail-heading">
      <span class="rail-kicker">版本演进</span>
      <span class="rail-count">{{ orderedVersions.length }} 个版本</span>
    </div>
    <div class="rail-scroll" data-test="version-rail-scroll">
      <div class="rail-track" :style="trackStyle">
        <button
          v-for="version in orderedVersions"
          :key="version.id"
          type="button"
          class="rail-node"
          :class="nodeClasses(version)"
          :data-test="`rail-version-${version.versionNo}`"
          role="tab"
          :aria-selected="version.id === selectedVersionId"
          :title="version.changeSummary || ''"
          @click="emit('select-version', version.id)"
        >
          <span class="node-version">V{{ version.versionNo }}</span>
          <span class="node-dot" aria-hidden="true"></span>
          <span class="node-date">{{ shortDate(version.createdAt) }}</span>
        </button>
      </div>
    </div>
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

const progressPercent = computed(() => {
  if (orderedVersions.value.length <= 1) return orderedVersions.value.length ? 100 : 0
  const currentIndex = orderedVersions.value.findIndex((version) => version.id === props.currentVersionId)
  if (currentIndex < 0) return 0
  return Math.round((currentIndex / (orderedVersions.value.length - 1)) * 100)
})

const trackStyle = computed(() => ({ '--progress': `${progressPercent.value / 100}` }))

function statusOf(version: ResumeVersion) {
  if (version.id === props.currentVersionId) return 'current'
  if (version.createdByType === 'fork') return 'fork'
  if (version.parentVersionId) return 'updated'
  return 'created'
}

function nodeClasses(version: ResumeVersion) {
  const status = statusOf(version)
  return {
    current: version.id === props.selectedVersionId,
    isCurrentVersion: version.id === props.currentVersionId,
    isFork: status === 'fork',
    isUpdated: status === 'updated',
    [`status-${status}`]: true,
  }
}

function shortDate(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${date.getMonth() + 1}月${date.getDate()}日 ${hour}:${minute}`
}
</script>

<style scoped>
.version-rail{position:relative;display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:end;gap:12px;height:58px;flex:0 0 auto}
.rail-heading{display:grid;align-content:center;gap:2px;min-width:62px;padding-bottom:8px}
.rail-kicker{font-size:10px;color:var(--muted);font-weight:750;letter-spacing:.08em}
.rail-count{font-size:9.5px;color:var(--copy);font-variant-numeric:tabular-nums}
.rail-scroll{align-self:stretch;min-width:0;overflow-x:auto;overflow-y:hidden;padding:4px 0 5px;scrollbar-width:thin}
.rail-track{position:relative;display:flex;min-width:100%;height:100%;padding:0 18px}
.rail-track::before,.rail-track::after{content:'';position:absolute;left:34px;right:34px;top:35px;height:2px;border-radius:999px}
.rail-track::before{background:var(--line-subtle,rgba(28,31,35,.15))}
.rail-track::after{right:auto;width:calc(100% - 68px);transform:scaleX(var(--progress));transform-origin:left center;background:linear-gradient(90deg,var(--brand-soft),var(--brand));transition:transform .25s ease-out}
.rail-node{position:relative;display:grid;grid-template-rows:15px 22px 15px;justify-items:center;align-items:center;flex:1;min-width:76px;border:0;background:none;padding:0 5px;cursor:pointer;z-index:1}
.node-version{font-size:10px;font-weight:600;color:var(--muted);font-variant-numeric:tabular-nums;line-height:1}
.rail-node.current .node-version{color:var(--ink);font-weight:800}
.node-dot{display:block;width:8px;height:8px;border-radius:50%;background:#a7aaa7;transition:box-shadow .18s ease-out;z-index:1}
.status-current .node-dot{background:var(--brand)}
.status-fork .node-dot,.status-updated .node-dot,.status-created .node-dot{background:#a7aaa7}
.rail-node.current .node-dot{box-shadow:0 0 0 2px var(--surface-solid,#fff),0 0 0 3px var(--copy)}
.rail-node.current.status-current .node-dot{box-shadow:0 0 0 2px var(--surface-solid,#fff),0 0 0 3px var(--brand)}
.node-date{font-size:9px;color:var(--muted);white-space:nowrap;font-variant-numeric:tabular-nums}
.rail-node.current .node-date{color:var(--copy);font-weight:600}
</style>
