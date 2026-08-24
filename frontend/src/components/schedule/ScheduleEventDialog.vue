<template>
  <div v-if="open" class="event-dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="event-dialog" role="dialog" aria-modal="true" aria-labelledby="schedule-event-title">
      <header>
        <div><small>日程</small><h2 id="schedule-event-title">{{ editing ? '编辑日程' : '添加日程' }}</h2></div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>
      <form @submit.prevent="submit">
        <label>标题<input v-model="form.title" data-test="event-title" maxlength="120" placeholder="例如：腾讯 · 技术面" required></label>
        <label>类型
          <div class="type-options">
            <button v-for="type in typeOptions" :key="type.value" type="button"
              :class="['type-option', { selected: form.eventType === type.value }]"
              :data-test="`event-type-${type.value}`"
              @click="form.eventType = type.value">
              <i :style="{ background: type.color }"></i>{{ type.label }}
            </button>
          </div>
        </label>
        <div class="field-row">
          <label>日期<input v-model="form.date" data-test="event-date" type="date" required></label>
          <label>开始时间<input v-model="form.time" data-test="event-time" type="time" required></label>
        </div>
        <div class="field-row">
          <label>结束时间<input v-model="form.endTime" data-test="event-end-time" type="time" placeholder="可选"></label>
          <label>关联岗位
            <select v-model="form.jobId" data-test="event-job">
              <option value="">不关联</option>
              <option v-for="job in jobs" :key="job.id" :value="String(job.id)">{{ job.label }}</option>
            </select>
          </label>
        </div>
        <label>备注<textarea v-model="form.notes" data-test="event-notes" rows="3" maxlength="1000" placeholder="可选：面试官、地点、准备事项…"></textarea></label>
        <p v-if="validationError || errorMessage" class="error" role="alert">{{ validationError || errorMessage }}</p>
        <footer>
          <button v-if="editing" type="button" class="danger" data-test="event-delete" :disabled="submitting" @click="remove">{{ submitting ? '处理中…' : '删除' }}</button>
          <span class="spacer"></span>
          <button type="button" :disabled="submitting" @click="$emit('close')">取消</button>
          <button class="primary" type="submit" data-test="event-save" :disabled="submitting">{{ submitting ? '保存中…' : '保存日程' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { ScheduleEvent, ScheduleEventFormPayload, ScheduleEventType } from '../../types/schedule'
import { SCHEDULE_EVENT_TYPE_COLORS, SCHEDULE_EVENT_TYPE_LABELS } from '../../types/schedule'

export interface ScheduleJobOption {
  id: number
  label: string
}

const props = defineProps<{
  open: boolean
  editing: ScheduleEvent | null
  submitting?: boolean
  errorMessage?: string
  /** 可选的关联岗位候选（来自求职目标），id 即 jobDescriptionId */
  jobs?: ScheduleJobOption[]
  /** 新建时的默认日期；为空时兜底当前时刻 */
  defaultDate?: Date | null
}>()
const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save', payload: ScheduleEventFormPayload): void
  (event: 'delete'): void
}>()

const typeOptions = (Object.keys(SCHEDULE_EVENT_TYPE_LABELS) as ScheduleEventType[]).map((value) => ({
  value,
  label: SCHEDULE_EVENT_TYPE_LABELS[value],
  color: SCHEDULE_EVENT_TYPE_COLORS[value],
}))

const form = reactive({
  title: '',
  eventType: 'interview' as ScheduleEventType,
  date: '',
  time: '10:00',
  endTime: '',
  jobId: '',
  notes: '',
})

watch(() => [props.open, props.editing] as const, ([open, editing]) => {
  if (!open) return
  const base = editing ? new Date(editing.startTime) : (props.defaultDate ?? new Date())
  const pad = (n: number) => String(n).padStart(2, '0')
  form.title = editing?.title ?? ''
  form.eventType = editing?.eventType ?? 'interview'
  form.date = `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())}`
  form.time = `${pad(base.getHours())}:${pad(base.getMinutes())}`
  form.endTime = editing?.endTime ? `${pad(new Date(editing.endTime).getHours())}:${pad(new Date(editing.endTime).getMinutes())}` : ''
  form.jobId = editing?.jobDescriptionId != null ? String(editing.jobDescriptionId) : ''
  form.notes = editing?.notes ?? ''
}, { immediate: true })

const validationError = computed(() => {
  if (form.endTime && form.endTime <= form.time) return '结束时间需晚于开始时间'
  return ''
})

function padSeconds(value: string): string {
  return `${value}:00`
}

function submit() {
  const title = form.title.trim()
  if (!title || validationError.value) return
  emit('save', {
    title,
    eventType: form.eventType,
    startTime: `${form.date}T${padSeconds(form.time)}`,
    // 结束时间与开始时间同一天；不填表示未定时长
    endTime: form.endTime ? `${form.date}T${padSeconds(form.endTime)}` : null,
    notes: form.notes.trim() || null,
    // 空字符串表示用户主动清除关联；后端 SET NULL 会同步解除目标侧展示
    jobDescriptionId: form.jobId ? Number(form.jobId) : null,
  })
}

function remove() {
  emit('delete')
}
</script>

<style scoped>
.event-dialog-backdrop{position:fixed;inset:0;z-index:45;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.event-dialog{width:min(480px,100%);border-radius:16px;background:var(--surface-solid,#fff);color:var(--ink,#141516);box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px}
.event-dialog header{display:flex;justify-content:space-between;gap:16px}
.event-dialog h2{margin:5px 0 20px;font-size:21px}
.event-dialog small{color:var(--brand,#168866);font-weight:700}
.event-dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.event-dialog form,.event-dialog label{display:grid;gap:9px}
.event-dialog form{gap:16px}
.field-row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.event-dialog label{font-weight:700;color:var(--ink,#344552)}
.event-dialog input,.event-dialog textarea,.event-dialog select{box-sizing:border-box;width:100%;border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:11px 12px;background:var(--surface-solid,#fff);color:var(--ink,#22313b);font:inherit}
.event-dialog textarea{resize:vertical;line-height:1.55}
.type-options{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.type-option{display:flex;align-items:center;justify-content:center;gap:7px;height:38px;border:1px solid var(--line,#d6dfe4);border-radius:9px;background:var(--surface-solid,#fff);color:var(--copy,#505357);font-size:13px;font-weight:700;cursor:pointer}
.type-option i{width:8px;height:8px;border-radius:50%}
.type-option.selected{border-color:var(--brand,#168866);color:var(--brand,#168866);background:var(--brand-soft,#edf7f3)}
.error{margin:0;color:var(--danger,#b53c32)}
.event-dialog footer{display:flex;justify-content:flex-end;gap:9px;margin-top:2px}
.event-dialog footer .spacer{flex:1}
.event-dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:var(--surface-solid,#fff);color:var(--ink,#141516);padding:9px 13px;cursor:pointer}
.event-dialog footer button:disabled{opacity:.55;cursor:not-allowed}
.event-dialog footer .danger{border-color:transparent;color:var(--danger,#b53c32)}
.event-dialog footer .primary{border-color:var(--brand,#168866);background:var(--brand,#168866);color:#fff;font-weight:600}
</style>
