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
  /** 允许点已完成的阶段退回。列表卡片不开（轻点就会改进度，太容易误伤），详情面板开并由父层加确认。 */
  allowBackward?: boolean
  /** 双列卡片的蛇形布局：一行放不下时把时间轴拐个弯，全部阶段名都能显示。 */
  wrap?: boolean
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

/** 蛇形两行的切分点：上排 ceil(n/2) 个，下排剩下的倒序排（从右往左走）。 */
const wrapMid = computed(() => Math.ceil(nodes.value.length / 2))
const rowTop = computed(() => nodes.value.slice(0, wrapMid.value))
const rowBottom = computed(() => nodes.value.slice(wrapMid.value).slice().reverse())
const turnFilled = computed(() => {
  const cur = indexOfCurrent()
  return cur >= wrapMid.value - 1
})
/** 蛇形下排的连线：下排从右往左走，节点 i（原始序）左侧那段在进度过了 i 时点亮。 */
function stepBack(node: { stage: TargetStage; round?: number }): boolean {
  const i = nodes.value.findIndex((item) => item.stage === node.stage && item.round === node.round)
  return indexOfCurrent() >= i
}
/** 蛇形模式下每列只有半张卡的 1/3~1/4 宽，用短标签保证「已拿 Offer」这类长名不截断。 */
function shortLabel(node: PipelineNode): string {
  if (node.stage === 'applied') return '投递'
  if (node.stage === 'offer') return 'Offer'
  return node.label
}
function labelOf(node: PipelineNode): string {
  return props.wrap ? shortLabel(node) : node.label
}

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
function backward(node: { stage: TargetStage }): boolean {
  const cur = stageFlowRank(props.stage)
  const next = stageFlowRank(node.stage)
  return cur > 0 && next > 0 && next < cur
}
function disabled(node: { stage: TargetStage }): boolean {
  if (props.locked) return true
  return !props.allowBackward && backward(node)
}
</script>

<template>
  <!-- 蛇形两行：上排从左到右，拐弯下去，下排从右到左收在 Offer -->
  <ol v-if="wrap" class="pipeline pipeline-wrap" :style="{ '--mid': wrapMid }">
    <li v-for="(node, i) in rowTop" :key="node.key" class="pipeline-item">
      <button
        type="button"
        class="node"
        :class="stepState(node)"
        :disabled="disabled(node)"
        @click.stop="node.stage === 'interview' ? emit('round', node.round!) : emit('change', node.stage)"
      >
        <span class="dot" />
        <span class="node-label">{{ labelOf(node) }}</span>
      </button>
      <span v-if="i < rowTop.length - 1" class="connector" :class="{ filled: indexOfCurrent() >= 0 && i < indexOfCurrent() }" />
    </li>
    <li class="pipeline-turn" :class="{ filled: turnFilled }" aria-hidden="true" />
    <li v-for="(node, i) in rowBottom" :key="node.key" class="pipeline-item">
      <button
        type="button"
        class="node"
        :class="stepState(node)"
        :disabled="disabled(node)"
        @click.stop="node.stage === 'interview' ? emit('round', node.round!) : emit('change', node.stage)"
      >
        <span class="dot" />
        <span class="node-label">{{ labelOf(node) }}</span>
      </button>
      <span v-if="i < rowBottom.length - 1" class="connector" :class="{ filled: stepBack(node) }" />
    </li>
  </ol>

  <ol v-else class="pipeline">
    <li v-for="(node, i) in nodes" :key="node.key" class="pipeline-item">
      <button
        type="button"
        class="node"
        :class="stepState(node)"
        :disabled="disabled(node)"
        @click.stop="node.stage === 'interview' ? emit('round', node.round!) : emit('change', node.stage)"
      >
        <span class="dot" />
        <span class="node-label">{{ labelOf(node) }}</span>
        <span v-if="props.times?.[node.stage] && node.stage !== 'interview'" class="node-time">{{ props.times[node.stage] }}</span>
      </button>
      <span v-if="i < nodes.length - 1" class="connector" :class="{ filled: indexOfCurrent() >= 0 && i < indexOfCurrent() }" />
    </li>
  </ol>
</template>
