<script setup lang="ts">
import { computed } from 'vue'
import type { TargetStage } from '../types/project'
import { TARGET_STAGE_LABELS, isTerminalStage, stageFlowRank } from '../types/project'

const props = defineProps<{
  stage: TargetStage
  times?: Partial<Record<TargetStage, string>>
  locked?: boolean
  interviewRounds?: number
  interviewRound?: number
}>()
const emit = defineEmits<{
  (e: 'change', stage: TargetStage): void
  (e: 'round', round: number): void
}>()

interface PipelineNode { key: string; stage: TargetStage; label: string; round?: number }
const rounds = computed(() => Math.min(8, Math.max(1, Math.round(props.interviewRounds ?? 2))))
const currentRound = computed(() => Math.min(rounds.value, Math.max(1, Math.round(props.interviewRound ?? 1))))
const nodes = computed<PipelineNode[]>(() => [
  { key: 'applied', stage: 'applied' as TargetStage, label: TARGET_STAGE_LABELS.applied },
  { key: 'exam', stage: 'exam' as TargetStage, label: TARGET_STAGE_LABELS.exam },
  ...Array.from({ length: rounds.value }, (_, index) => ({
    key: `interview-${index + 1}`,
    stage: 'interview' as TargetStage,
    round: index + 1,
    label: `${index + 1}面`,
  })),
  { key: 'hr', stage: 'hr' as TargetStage, label: TARGET_STAGE_LABELS.hr },
  { key: 'offer', stage: 'offer' as TargetStage, label: TARGET_STAGE_LABELS.offer },
])

function indexOfCurrent(): number {
  if (isTerminalStage(props.stage) && props.stage !== 'offer') return -1
  if (props.stage === 'interview') return nodes.value.findIndex((node) => node.stage === 'interview' && node.round === currentRound.value)
  return nodes.value.findIndex((node) => node.stage === props.stage)
}
function stepState(node: { stage: TargetStage; round?: number }) {
  const cur = indexOfCurrent()
  const i = nodes.value.findIndex((item) => item.stage === node.stage && item.round === node.round)
  const isCurrent = node.stage === props.stage && (node.stage !== 'interview' || node.round === currentRound.value)
  return { current: isCurrent, done: cur >= 0 && i < cur }
}
function disabled(node: { stage: TargetStage }): boolean {
  if (props.locked) return true
  const cur = stageFlowRank(props.stage)
  const next = stageFlowRank(node.stage)
  return cur > 0 && next > 0 && next < cur
}
</script>

<template>
  <ol class="pipeline">
    <li v-for="(node, i) in nodes" :key="node.key" class="pipeline-item">
      <button
        type="button"
        class="node"
        :class="stepState(node)"
        :disabled="disabled(node)"
        @click="node.stage === 'interview' ? emit('round', node.round!) : emit('change', node.stage)"
      >
        <span class="dot" />
        <span class="node-label">{{ node.label }}</span>
        <span v-if="props.times?.[node.stage] && node.stage !== 'interview'" class="node-time">{{ props.times[node.stage] }}</span>
      </button>
      <span v-if="i < nodes.length - 1" class="connector" :class="{ filled: indexOfCurrent() >= 0 && i < indexOfCurrent() }" />
    </li>
  </ol>
</template>
