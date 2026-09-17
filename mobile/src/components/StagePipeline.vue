<script setup lang="ts">
import { computed } from 'vue'
import type { TargetStage } from '../types/project'
import { TARGET_STAGE_LABELS, isTerminalStage, stageFlowRank } from '../types/project'

const props = defineProps<{
  stage: TargetStage
  /** 时间按节点 key 记录（applied / exam / interview-1 / interview-2 … / hr / offer）——每一面都是独立节点、有独立时间。 */
  times?: Record<string, string>
  locked?: boolean
  interviewRounds?: number
  interviewRound?: number
  /** 允许点已完成的阶段退回。列表卡片不开（轻点就会改进度，太容易误伤），详情面板开并由父层加确认。 */
  allowBackward?: boolean
  /** 紧凑形态（双列卡片）：双行蛇形时间轴——上行从左到右、下行从右到左，节点带标签和时间。 */
  mini?: boolean
}>()
const emit = defineEmits<{
  (e: 'change', stage: TargetStage): void
  (e: 'round', round: number): void
}>()

interface PipelineNode { key: string; stage: TargetStage; label: string; round?: number }
const rounds = computed(() => Math.min(8, Math.max(1, Math.round(props.interviewRounds ?? 2))))
const currentRound = computed(() => Math.min(rounds.value, Math.max(1, Math.round(props.interviewRound ?? 1))))
const nodes = computed<PipelineNode[]>(() => [
  { key: 'applied', stage: 'applied' as TargetStage, label: '投递' },
  { key: 'exam', stage: 'exam' as TargetStage, label: TARGET_STAGE_LABELS.exam },
  ...Array.from({ length: rounds.value }, (_, index) => ({
    key: `interview-${index + 1}`,
    stage: 'interview' as TargetStage,
    round: index + 1,
    label: `${index + 1}面`,
  })),
  { key: 'hr', stage: 'hr' as TargetStage, label: TARGET_STAGE_LABELS.hr },
  { key: 'offer', stage: 'offer' as TargetStage, label: 'Offer' },
])

function indexOfCurrent(): number {
  if (isTerminalStage(props.stage) && props.stage !== 'offer') return -1
  if (props.stage === 'interview') return nodes.value.findIndex((node) => node.stage === 'interview' && node.round === currentRound.value)
  return nodes.value.findIndex((node) => node.stage === props.stage)
}
/** 连线点亮：当前节点之前的每一段。等宽几何下所有线段天生一样长。 */
function filledOf(index: number): boolean {
  const cur = indexOfCurrent()
  return cur >= 0 && index < cur
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

// ── 双行蛇形（mini）：上半行 0..k-1 从左到右，下半行 k..n-1 从右到左，右端竖线拐弯 ──
const splitAt = computed(() => Math.ceil(nodes.value.length / 2))
function gridRowOf(i: number): number {
  return i < splitAt.value ? 1 : 2
}
function gridColOf(i: number): number {
  if (i < splitAt.value) return i + 1
  return 2 * splitAt.value - i
}
/** 上行节点向右连线（同行还有下一个）；下行节点向左连线（逻辑下一个排在左边）。 */
function linkDir(i: number): 'r' | 'l' | null {
  if (i < splitAt.value - 1) return 'r'
  if (i >= splitAt.value && i < nodes.value.length - 1) return 'l'
  return null
}
</script>

<template>
  <!-- 等宽网格：每个节点占一列，连线锚定在圆点之间——标签长短不再影响线的长度 -->
  <ol
    class="pipeline" :class="{ serp: mini }"
    :style="mini ? { gridTemplateColumns: `repeat(${splitAt}, 1fr)` } : undefined"
  >
    <li
      v-for="(node, i) in nodes" :key="node.key" class="pipeline-item"
      :class="{ filled: filledOf(i), link: mini && linkDir(i) != null, [`link-${linkDir(i)}`]: !!linkDir(i), elbow: mini && i === splitAt - 1 && splitAt < nodes.length }"
      :style="mini ? { gridRow: gridRowOf(i), gridColumn: gridColOf(i) } : undefined"
    >
      <button
        type="button"
        class="node"
        :class="stepState(node)"
        :disabled="disabled(node)"
        @click.stop="node.stage === 'interview' ? emit('round', node.round!) : emit('change', node.stage)"
      >
        <span class="dot" />
        <span class="node-label">{{ node.label }}</span>
        <span v-if="props.times?.[node.key]" class="node-time">{{ props.times[node.key] }}</span>
      </button>
    </li>
  </ol>
</template>
