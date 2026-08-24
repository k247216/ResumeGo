<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="stage-timeline-title">
      <header>
        <div>
          <small>阶段时间轴</small>
          <h2 id="stage-timeline-title">{{ target?.name }}</h2>
          <p class="sub">单向推进 · 每次变更都会留痕</p>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <div v-if="loading" class="state">正在读取阶段记录…</div>

      <!-- 竖向彩色时间轴 -->
      <ol v-else class="vtimeline" data-test="stage-timeline">
        <li
          v-for="(step, index) in steps"
          :key="step.key"
          class="v-row"
          :class="{ current: step.current, ghost: !step.time }"
          :style="{ '--i': index, '--c': step.color }"
        >
          <span class="v-dot" aria-hidden="true">
            <svg viewBox="0 0 16 16" width="15" height="15"><path fill="#fff" :d="iconPath(step.key)" /></svg>
          </span>
          <div class="v-copy">
            <strong>{{ step.label }}</strong>
            <small>{{ step.time || '未记录' }}</small>
          </div>
          <span v-if="index === 0" class="v-badge">最新</span>
        </li>
      </ol>

      <p class="hint">阶段只能向前推进；标记结果后状态锁定。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { JobProject, StageEvent, TargetStage } from '../../types/project'
import { TARGET_STAGE_COLORS, TARGET_STAGE_LABELS } from '../../types/project'

const GLYPHS: Record<TargetStage, string> = {
  applied: 'M2 6.5 14 1.5 8.5 14.5 7 9 2 6.5Z',
  exam: 'M3 1h7l4 4v9a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1Zm7 1v3h3l-3-3ZM5 8h6v1.2H5V8Zm0 3h4v1.2H5V11Z',
  interview: 'M2 2h12a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H7l-4 3.5V11H2a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z',
  hr: 'M8 1a3.2 3.2 0 1 1 0 6.4A3.2 3.2 0 0 1 8 1ZM2 15c0-3 2.7-5 6-5s6 2 6 5H2Z',
  offer: 'M8 .8 10 6h5.4L11 9.4 12.8 15 8 11.6 3.2 15 5 9.4.6 6H6L8 .8Z',
  pool: 'M8 1a7 7 0 0 1 7 7h-2A5 5 0 0 0 8 3V1ZM1 8a7 7 0 0 1 7-7v2a5 5 0 0 0-5 5H1Zm7 7a7 7 0 0 1-7-7h2a5 5 0 0 0 5 5v2Zm7-7a7 7 0 0 1-7 7v-2a5 5 0 0 0 5-5h2Z',
  screened_out: 'M1 2h14L10 8.5V14l-4 1.5V8.5L1 2Z',
  rejected: 'M8 1a7 7 0 1 1 0 14A7 7 0 0 1 8 1Zm2.8 4.2L8 8l-2.8-2.8-1 1L7 9l-2.8 2.8 1 1L8 10l2.8 2.8 1-1L9 9l2.8-2.8-1-1Z',
  closed: 'M3 1.5h2V7h6.6L9.2 4.6l1.4-1.4L15 7.6l-4.4 4.4-1.4-1.4 2.4-2.4H4a1 1 0 0 1-1-1V1.5Z',
}

function iconPath(stage: TargetStage): string {
  return GLYPHS[stage] ?? '·'
}

const props = defineProps<{
  open: boolean
  target: JobProject | null
  events: StageEvent[]
  loading?: boolean
}>()

defineEmits<{ (event: 'close'): void }>()

// 最新在前
const steps = computed(() => {
  const sorted = [...props.events].sort((left, right) => right.id - left.id)
  return sorted.map((event, index) => ({
    key: event.stage,
    label: TARGET_STAGE_LABELS[event.stage] ?? event.stage,
    color: TARGET_STAGE_COLORS[event.stage] ?? '#989893',
    time: formatTime(event.occurredAt),
    current: index === 0,
  }))
})

function formatTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const sameYear = date.getFullYear() === new Date().getFullYear()
  const day = `${date.getMonth() + 1}月${date.getDate()}日`
  return sameYear ? `${day} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}` : `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.dialog{width:min(460px,100%);max-height:min(72vh,640px);overflow-y:auto;border-radius:16px;background:#fff;color:#141516;box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px;animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.dialog{animation:none}}
.dialog header{display:flex;justify-content:space-between;gap:16px;margin-bottom:16px}
.dialog h2{margin:5px 0 2px;font-size:19px;font-weight:650}
.sub{margin:0;color:var(--muted,#989893);font-size:12px}
.dialog small{color:var(--brand,#168866);font-weight:700}
.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.state{padding:24px 2px;color:var(--muted,#989893);font-size:13.5px}

/* 竖向彩色时间轴 */
.vtimeline{margin:0;padding:2px 2px 0;list-style:none}
.v-row{position:relative;display:flex;align-items:flex-start;gap:13px;padding:6px 0 18px 44px;animation:v-in .34s cubic-bezier(.2,.7,.3,1) both;animation-delay:calc(var(--i,0)*80ms)}
@keyframes v-in{from{opacity:0;transform:translateX(-10px)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.v-row{animation:none}}
/* 连接线：从节点下方延伸到下一节点 */
.v-row::before{content:'';position:absolute;left:21px;top:38px;bottom:-4px;width:2.5px;border-radius:2px;background:var(--line-subtle,rgba(28,31,35,.09))}
.v-row.current::before{background:linear-gradient(180deg,var(--c) 0%,var(--line-subtle,rgba(28,31,35,.09)) 100%)}
.v-row:last-child::before{display:none}
.v-dot{position:absolute;left:4px;top:2px;display:grid;place-items:center;width:36px;height:36px;border-radius:12px;background:var(--c);color:#fff;font-size:14px;font-weight:700;box-shadow:0 3px 10px color-mix(in srgb,var(--c) 35%,transparent)}
.v-row.current .v-dot{animation:v-pop .4s cubic-bezier(.2,.7,.3,1.4) both}
@keyframes v-pop{from{transform:scale(.6)}to{transform:scale(1)}}
@media (prefers-reduced-motion: reduce){.v-row.current .v-dot{animation:none}}
.v-copy{min-width:0;flex:1;display:grid;gap:2px;padding-top:2px}
.v-copy strong{font-size:14px;font-weight:650;color:#23292e}
.v-row:not(.current) .v-copy strong{color:#4a5560;font-weight:600}
.v-copy small{font-size:11.5px;color:var(--muted,#989893);font-variant-numeric:tabular-nums}
.v-badge{flex:0 0 auto;margin-top:4px;padding:2px 9px;border-radius:999px;background:color-mix(in srgb,var(--c) 14%,transparent);color:var(--c);font-size:10.5px;font-weight:700}
.hint{margin:16px 0 0;padding-top:12px;border-top:1px solid var(--line-subtle,rgba(28,31,35,.07));color:#b0b0ab;font-size:11.5px;line-height:1.6}
</style>
