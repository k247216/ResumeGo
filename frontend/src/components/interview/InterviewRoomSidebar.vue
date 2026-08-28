<template>
  <aside class="chat-sidebar">
    <button
      data-test="interview-room-back"
      class="sidebar-back-btn"
      type="button"
      title="返回面试大厅"
      @click="emit('back')"
    >
      <el-icon><ArrowLeft /></el-icon>
    </button>

    <div class="sidebar-progress">
      <div class="sidebar-progress-head">
        <span>问题进度</span>
        <small class="sidebar-progress-current">{{ activeSession.completed ? '本轮已完成' : `第 ${displayQuestionIndex} 题` }}</small>
        <strong>{{ activeSession.completed ? activeSession.totalQuestions : displayQuestionIndex }} / {{ activeSession.totalQuestions }}</strong>
      </div>
      <div class="sidebar-progress-track">
        <div
          v-for="step in activeSession.totalQuestions || 3"
          :key="step"
          class="sidebar-progress-item"
          :class="{
            active: !activeSession.completed && step === displayQuestionIndex && viewingHistoryIndex === null,
            completed: completedQuestionSteps.includes(step),
            viewing: step === viewingHistoryIndex,
          }"
        >
          <span
            class="sidebar-dot"
            :class="{
              active: !activeSession.completed && step === displayQuestionIndex && viewingHistoryIndex === null,
              completed: completedQuestionSteps.includes(step),
              viewing: step === viewingHistoryIndex,
            }"
            :aria-label="`问题 ${step}`"
          ></span>
          <span class="sidebar-progress-name">问题 {{ step }}</span>
        </div>
      </div>
    </div>

    <div v-if="mode === 'ROLE_BASED' && planSessions.length" class="sidebar-persona-strip" data-test="interview-room-personas">
      <span class="sidebar-persona-strip-label">{{ reviewMode ? '本次复盘' : '本次面试' }}</span>
      <button
        v-for="session in planSessions"
        :key="session.sessionId"
        :data-test="`switch-round-${session.sessionId}`"
        type="button"
        class="persona-switch-button"
        :class="[interviewRoundStatus(session), { active: session.sessionId === activeSessionId }]"
        :disabled="actionLoading || planSessions.length < 2"
        @click="emit('switch-session', session.sessionId)"
      >
        <span class="persona-avatar" :class="personaAvatarClass(session)" aria-hidden="true">{{ personaInitials(session) }}</span>
        <span class="persona-switch-copy">
          <strong>{{ session.personaName || '面试官' }}</strong>
          <small>{{ roundStatusText(session) }}</small>
        </span>
        <span v-if="session.sessionId === activeSessionId" class="persona-active-mark" aria-label="当前轮次"></span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { InterviewerPersona, InterviewStatusResponse } from '../../types/interview'
import { interviewRoundStatus } from '../../utils/interviewRecords'

const props = defineProps<{
  activeSession: InterviewStatusResponse
  mode?: 'ROLE_BASED' | 'KNOWLEDGE_TRAINING' | 'EXPERIENCE_SIMULATION'
  activePersona: InterviewerPersona | null
  activePersonaStyle: string
  activeModelLabel?: string
  activeStrategyLabel?: string
  planSessions: InterviewStatusResponse[]
  activeSessionId: number
  completedQuestionSteps: number[]
  viewingHistoryIndex: number | null
  reviewMode: boolean
  actionLoading: boolean
}>()
const mode = computed(() => props.mode ?? 'ROLE_BASED')
// 当前题号优先取当前题目 DTO，和房间中央题干使用同一数据源。
// 状态字段只在题目尚未加载时兜底，避免“第 N 题”与题干来自不同题目的短暂错位。
const displayQuestionIndex = computed(() =>
  props.activeSession.currentQuestion?.questionIndex ?? props.activeSession.currentQuestionIndex,
)

const emit = defineEmits<{
  back: []
  'switch-session': [sessionId: number]
}>()

function roundStatusText(session: InterviewStatusResponse) {
  return {
    completed: '已完成',
    failed: '异常中断',
    cancelled: '已取消',
    active: '进行中',
  }[interviewRoundStatus(session)]
}

function personaInitials(session: InterviewStatusResponse) {
  const name = (session.personaName || '面试官').trim()
  const latin = name.match(/[A-Za-z0-9]+/g)?.join('')
  return latin ? latin.slice(0, 2).toUpperCase() : Array.from(name).slice(0, 2).join('')
}

function personaAvatarClass(session: InterviewStatusResponse) {
  const identity = `${session.personaName || ''} ${session.personaTitle || ''}`.toLowerCase()
  if (/hr|人力|招聘|沟通/.test(identity)) return 'persona-avatar-hr'
  if (/架构|系统|总监|负责人/.test(identity)) return 'persona-avatar-architect'
  if (/技术|工程|开发|研发/.test(identity)) return 'persona-avatar-engineer'
  return 'persona-avatar-default'
}
</script>

<style scoped>
.chat-sidebar{width:220px;flex-shrink:0;padding:16px;border-right:1px solid var(--line,#e5eaf2);background:var(--surface-solid,#fff);overflow:auto}.sidebar-back-btn{display:grid;place-items:center;width:34px;height:34px;border:1px solid var(--line,#e5eaf2);border-radius:11px;background:var(--surface-solid,#fff);color:var(--copy,#475569);cursor:pointer}.sidebar-persona-card{display:grid;justify-items:center;margin-top:18px;text-align:center}.sidebar-persona-avatar{display:grid;place-items:center;width:62px;height:62px;border-radius:20px;background:var(--bg-hover);color:var(--copy);font-size:22px;font-weight:900}.avatar-architect{background:var(--bg-hover)}.avatar-hr{background:var(--bg-hover)}.sidebar-persona-card>span,.sidebar-question-card>span,.sidebar-round-card>span{margin-top:10px;color:var(--muted,#94a3b8);font-size:10px;font-weight:900;letter-spacing:.08em}.sidebar-persona-card strong{margin-top:4px;color:var(--ink,#0f172a)}.sidebar-persona-card small,.sidebar-persona-card p{color:var(--muted,#64748b);font-size:11px}.sidebar-persona-card p{line-height:1.5}.sidebar-question-card{display:grid;gap:4px;margin-top:18px;padding:12px;border-radius:14px;background:var(--surface,#f8fafc)}.sidebar-question-card strong{color:var(--ink,#0f172a)}.sidebar-question-card small{color:var(--muted,#64748b)}.sidebar-dots{display:flex;flex-wrap:wrap;gap:5px;margin-top:10px}.sidebar-dot{display:grid;place-items:center;width:25px;height:25px;border-radius:999px;background:var(--surface,#f1f5f9);color:var(--muted,#64748b);font-size:10px;font-weight:900}.sidebar-dot.active{background:var(--brand);color:#fff}.sidebar-dot.completed{background:var(--brand-soft,#d1fae5);color:var(--brand,#047857)}.sidebar-dot.viewing{outline:2px solid var(--brand,#10b981)}.sidebar-round-card{display:grid;gap:7px;margin-top:18px}.round-switch-button{display:grid;grid-template-columns:24px 1fr;grid-template-rows:auto auto;column-gap:7px;padding:8px;border:1px solid var(--line,#e5eaf2);border-radius:12px;background:var(--surface-solid,#fff);text-align:left;cursor:pointer}.round-switch-button i{grid-row:1/3;display:grid;place-items:center;width:24px;height:24px;border-radius:999px;background:var(--surface,#f1f5f9);font-size:10px;font-style:normal}.round-switch-button strong{font-size:11px}.round-switch-button small{color:var(--muted,#64748b);font-size:10px}.round-switch-button.active{border-color:var(--brand,#10b981);background:var(--brand-soft,#f0fdf4)}.round-switch-button.completed i{background:var(--brand-soft,#d1fae5);color:var(--brand,#047857)}.round-switch-button.failed{border-color:var(--danger-soft,#fecaca)}.round-switch-button.cancelled{opacity:.7}@media(max-width:900px){.chat-sidebar{width:100%;border-right:0;border-bottom:1px solid var(--line,#e5eaf2)}}
.sidebar-progress{margin-top:22px;padding:13px 4px 10px;border-top:1px solid var(--line,#e5eaf2);border-bottom:1px solid var(--line,#e5eaf2)}
.sidebar-progress-head{display:flex;align-items:center;gap:7px;margin-bottom:12px;color:var(--muted,#94a3b8);font-size:10px;font-weight:800;letter-spacing:.06em}
.sidebar-progress-current{margin-left:auto;color:var(--muted,#989893);font-size:9px;font-weight:500;letter-spacing:0}
.sidebar-progress-head strong{color:var(--ink,#171717);font-size:11px;font-variant-numeric:tabular-nums;letter-spacing:0}
.sidebar-progress-track{position:relative;display:grid;gap:0;padding:0 0 2px 2px}
.sidebar-progress-track::before{position:absolute;top:10px;bottom:10px;left:12px;width:1px;background:var(--line,#e5eaf2);content:''}
.sidebar-progress-item{position:relative;display:flex;align-items:center;gap:10px;min-height:33px;color:var(--muted,#94a3b8);font-size:11px}
.sidebar-progress-item.active{color:var(--brand,#168b68);font-weight:800}.sidebar-progress-item.completed{color:var(--copy,#6e6e6a)}
.sidebar-progress-name{position:relative;z-index:1;background:var(--surface-solid,#fff);padding-right:5px}
.sidebar-dot{position:relative;z-index:2;width:13px;height:13px;border:1px solid var(--line,#ccd2ce);background:var(--surface-solid,#fff);font-size:8px;line-height:1;color:var(--muted,#989893)}
.sidebar-dot.active{width:15px;height:15px;border:2px solid var(--brand,#168b68);background:var(--brand,#168b68);color:#fff}
.sidebar-dot.completed{border-color:var(--brand,#168b68);background:var(--brand,#168b68);color:#fff}.sidebar-dot.viewing{outline:2px solid var(--brand,#168b68);outline-offset:2px}
.chat-sidebar{display:flex;flex-direction:column;padding:22px 20px}.sidebar-back-btn{display:none}.sidebar-progress{order:1;margin-top:0;padding-top:0;border-top:0}.sidebar-persona-card{order:2;margin-top:auto;padding-top:32px}.sidebar-round-card{order:3}
.sidebar-progress-current{display:none}
.sidebar-persona-strip{order:2;display:grid;gap:7px;margin-top:0;padding-top:12px}
.sidebar-persona-strip-label{color:var(--muted,#989893);font-size:10px;font-weight:800;letter-spacing:.06em}
.persona-switch-button{display:grid;grid-template-columns:28px minmax(0,1fr) auto;align-items:center;gap:8px;width:100%;min-height:38px;padding:4px 0;border:0;border-radius:8px;background:transparent;color:var(--ink,#171717);text-align:left;cursor:pointer}
.persona-switch-button:hover{background:var(--surface,#f7f8f6)}
.persona-switch-button.active{color:var(--ink,#171717)}
.persona-avatar{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;font-size:9px;font-weight:800;letter-spacing:-.02em}
.persona-avatar-default{background:#eef0ed;color:#59605b}
.persona-avatar-engineer{background:#e4eee8;color:#176545}
.persona-avatar-architect{background:#e8e9e5;color:#3f4742}
.persona-avatar-hr{background:#f1ebe6;color:#715646}
.persona-switch-button.active .persona-avatar{box-shadow:inset 0 0 0 2px #168b68}
.persona-switch-copy{display:grid;gap:2px;min-width:0}
.persona-switch-copy strong{overflow:hidden;font-size:11px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}
.persona-switch-copy small{color:var(--muted,#989893);font-size:9px;line-height:1.3}
.persona-active-mark{width:5px;height:5px;border-radius:50%;background:#168b68}
</style>
