<script setup lang="ts">
import { computed } from 'vue'
import type { JobProject, TargetStage } from '../types/project'
import { TARGET_STAGE_COLORS, TARGET_STAGE_LABELS, isTerminalStage, normalizeTargetStage } from '../types/project'
import { companyMark } from '../constants/companyBrands'
import StagePipeline from './StagePipeline.vue'
import AppIcon from './AppIcon.vue'

const props = defineProps<{
  target: JobProject
  index: number
  resumeLabel?: string | null
  stageTimes?: Partial<Record<TargetStage, string>>
}>()
const emit = defineEmits<{
  (e: 'open'): void
  (e: 'stage', stage: TargetStage): void
  (e: 'menu', anchor: MouseEvent): void
  (e: 'link-resume'): void
}>()

const stage = computed(() => normalizeTargetStage(props.target.stage))
const mark = computed(() => companyMark(props.target.name))
const pillStyle = computed(() => {
  const color = TARGET_STAGE_COLORS[stage.value]
  return { background: `color-mix(in srgb, ${color} 12%, transparent)`, color }
})
const locked = computed(() => props.target.status === 'archived' || isTerminalStage(stage.value))

function shortDate(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}
function recentLabel(): string {
  if (props.target.stageUpdatedAt) {
    const days = Math.round((new Date(new Date().toDateString()).getTime() - new Date(new Date(props.target.stageUpdatedAt).toDateString()).getTime()) / 86400000)
    if (days === 0) return '今天更新状态'
    if (days === 1) return '昨天更新状态'
    return `${shortDate(props.target.stageUpdatedAt)} 更新状态`
  }
  return `${shortDate(props.target.createdAt)} 创建`
}
</script>

<template>
  <article class="card" :class="{ archived: target.status === 'archived' }" :style="{ '--i': Math.min(index, 8) }" @click="emit('open')">
    <header class="card-head">
      <img v-if="mark.icon" class="logo-img" :src="mark.icon" alt="" aria-hidden="true">
      <span v-else class="logo-mark" :style="{ background: mark.color, color: mark.lightText ? '#fff' : '#1b1b1b' }">{{ mark.letter }}</span>
      <div class="head-copy">
        <h3>{{ target.name }}</h3>
        <small>{{ target.targetRole || target.industry || '求职目标' }}<template v-if="target.location"> · {{ target.location }}</template></small>
      </div>
      <div class="head-side">
        <button class="stage-pill" :style="pillStyle" @click.stop="emit('open')">{{ TARGET_STAGE_LABELS[stage] }}</button>
        <button class="menu-trigger" aria-label="更多操作" @click.stop="emit('menu', $event)"><AppIcon name="more" :size="18" /></button>
      </div>
    </header>

    <StagePipeline :stage="stage" :times="stageTimes" :locked="locked" @change="(s) => emit('stage', s)" />

    <div class="chip-row">
      <button v-if="resumeLabel" class="chip" @click.stop="emit('open')"><AppIcon name="file" :size="14" /> {{ resumeLabel }}</button>
      <button v-else class="chip dashed" @click.stop="emit('link-resume')"><AppIcon name="plus" :size="13" /> 绑定简历</button>
      <span v-if="locked" class="chip-meta">已标记「{{ TARGET_STAGE_LABELS[stage] }}」· 锁定</span>
    </div>

    <footer class="card-foot">
      <span>创建于 {{ shortDate(target.createdAt) }}</span>
      <span>·</span>
      <span>{{ recentLabel() }}</span>
    </footer>
  </article>
</template>
