<script setup lang="ts">
import type { TargetStage } from '../types/project'
import { TARGET_STAGE_LABELS, isTerminalStage, stageFlowRank } from '../types/project'

const props = defineProps<{
  stage: TargetStage
  times?: Partial<Record<TargetStage, string>>
  locked?: boolean
  interviewRounds?: number
}>()
const emit = defineEmits<{ (e: 'change', stage: TargetStage): void }>()

const FLOW: TargetStage[] = ['applied', 'exam', 'interview', 'hr', 'offer']

function indexOf(stage: TargetStage): number {
  return isTerminalStage(stage) && stage !== 'offer' ? -1 : FLOW.indexOf(stage)
}
function stepState(key: TargetStage) {
  const cur = indexOf(props.stage)
  const i = FLOW.indexOf(key)
  return { current: key === props.stage, done: cur >= 0 && i <= cur && key !== props.stage }
}
function disabled(key: TargetStage): boolean {
  if (props.locked) return true
  const cur = stageFlowRank(props.stage)
  const next = stageFlowRank(key)
  return cur > 0 && next > 0 && next < cur
}
function labelOf(key: TargetStage): string {
  if (key === 'interview' && props.interviewRounds && props.interviewRounds !== 2) return `面试 · ${props.interviewRounds}轮`
  return TARGET_STAGE_LABELS[key]
}
</script>

<template>
  <ol class="pipeline">
    <li v-for="(key, i) in FLOW" :key="key" class="pipeline-item">
      <button
        type="button"
        class="node"
        :class="stepState(key)"
        :disabled="disabled(key)"
        @click="emit('change', key)"
      >
        <span class="dot" />
        <span class="node-label">{{ labelOf(key) }}</span>
        <span v-if="props.times?.[key]" class="node-time">{{ props.times[key] }}</span>
      </button>
      <span v-if="i < FLOW.length - 1" class="connector" :class="{ filled: indexOf(props.stage) >= 0 && i < indexOf(props.stage) }" />
    </li>
  </ol>
</template>
