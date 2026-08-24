<template>
  <article :class="['day-event', { external: isExternal }]" :data-test="`event-card-${event.key}`">
    <component
      :is="isExternal ? 'div' : 'button'"
      :type="isExternal ? undefined : 'button'"
      class="day-event-main"
      @click="!isExternal && $emit('edit', event)"
    >
      <span class="event-time" data-test="event-time">
        <strong>{{ startTimeLabel }}</strong>
        <small v-if="endTimeLabel">{{ endTimeLabel }}</small>
      </span>
      <span class="event-bar" :style="{ background: barColor }" aria-hidden="true" />
      <span class="event-copy">
        <span class="event-title-row">
          <strong>{{ event.title }}</strong>
          <i v-if="isExternal" class="event-source">{{ event.sourceName }}</i>
        </span>
        <small v-if="!isExternal" class="event-meta">
          {{ metaLabel }}<template v-if="endTimeLabel"> · 至 {{ endTimeLabel }}</template>
        </small>
        <p v-if="event.notes">{{ event.notes }}</p>
      </span>
      <el-icon v-if="!isExternal" class="event-arrow"><ArrowRight /></el-icon>
    </component>
    <div v-if="linkedTarget" class="day-event-context" data-test="event-target-context">
      <span class="day-event-context-copy">{{ linkedTarget.name }}</span>
      <button type="button" class="day-event-context-action" data-test="event-target-link" @click="openTarget">目标详情<el-icon :size="12"><ArrowRight /></el-icon></button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { useTargetsStore } from '../../stores/targets'
import { SCHEDULE_EVENT_TYPE_COLORS, SCHEDULE_EVENT_TYPE_LABELS } from '../../types/schedule'
import type { DisplayCalendarEvent, ScheduleEventType } from '../../types/schedule'

const props = defineProps<{ event: DisplayCalendarEvent }>()
defineEmits<{ (event: 'edit', value: DisplayCalendarEvent): void }>()

const targetsStore = useTargetsStore()
const router = useRouter()

const isExternal = computed(() => props.event.kind === 'external')

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function clockTime(iso: string): string {
  const date = new Date(iso)
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const startTimeLabel = computed(() => (props.event.allDay ? '全天' : clockTime(props.event.startTime)))
const endTimeLabel = computed(() => (!props.event.allDay && props.event.endTime ? clockTime(props.event.endTime) : ''))

const barColor = computed(() => {
  if (props.event.kind === 'external') return 'var(--external-accent)'
  return SCHEDULE_EVENT_TYPE_COLORS[(props.event.eventType ?? 'other') as ScheduleEventType]
})

const metaLabel = computed(() => {
  if (props.event.kind === 'external') return '来自外部日历 · 只读'
  return SCHEDULE_EVENT_TYPE_LABELS[(props.event.eventType ?? 'other') as ScheduleEventType]
})

// 已关联目标的事件在卡片下方给出目标上下文；目标已删除则如实不显示。
const linkedTarget = computed(() => {
  if (props.event.jobDescriptionId == null) return null
  return targetsStore.targets.find((target) => target.jobDescriptionId === props.event.jobDescriptionId) ?? null
})

function openTarget() {
  if (!linkedTarget.value) return
  void router.push({ name: 'targets', query: { targetId: String(linkedTarget.value.id) } })
}
</script>

<style scoped>
.day-event{display:block}
.day-event-main{display:flex;align-items:flex-start;gap:9px;width:100%;padding:8px 8px;border:0;border-radius:9px;background:transparent;text-align:left;cursor:pointer;color:var(--ink)}
.day-event-main:hover{background:var(--bg-hover)}
.external .day-event-main{cursor:default}
.event-time{display:grid;gap:2px;align-content:flex-start;flex:0 0 auto;min-width:40px;text-align:right}
.event-time strong{color:var(--copy);font-size:12px;font-weight:600;font-variant-numeric:tabular-nums;line-height:1.35}
.event-time small{color:var(--muted);font-size:10.5px;font-variant-numeric:tabular-nums;line-height:1.2}
.event-bar{flex:0 0 auto;width:3px;align-self:stretch;border-radius:2px;background:var(--border-default)}
.event-copy{display:grid;gap:3px;min-width:0;flex:1;align-content:flex-start}
.event-title-row{display:flex;align-items:baseline;gap:8px;min-width:0}
.event-title-row strong{overflow:hidden;flex:0 1 auto;min-width:0;color:var(--ink);font-size:13px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}
.event-source{flex:0 0 auto;padding:1px 7px;border-radius:999px;color:var(--external-accent);background:color-mix(in srgb,var(--external-accent) 10%,transparent);font-size:10px;font-style:normal;font-weight:600;white-space:nowrap}
.event-meta{color:var(--muted);font-size:11px}
.event-copy p{display:-webkit-box;overflow:hidden;margin:0;color:var(--copy);font-size:11.5px;line-height:1.5;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.event-arrow{align-self:center;margin-left:auto;color:var(--text-tertiary);flex:0 0 auto}
.day-event-context{display:flex;align-items:center;gap:10px;margin-top:-2px;padding:0 8px 6px 62px}
.day-event-context-copy{overflow:hidden;flex:1;min-width:0;color:var(--muted);font-size:11.5px;text-overflow:ellipsis;white-space:nowrap}
.day-event-context-action{display:inline-flex;align-items:center;gap:4px;flex:0 0 auto;padding:0;border:0;background:transparent;color:var(--copy);font-size:11.5px;font-weight:500;cursor:pointer}
.day-event-context-action:hover{color:var(--brand)}
</style>
