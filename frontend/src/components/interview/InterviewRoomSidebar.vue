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

    <div class="sidebar-persona-card">
      <div class="sidebar-persona-avatar" :class="`avatar-${activePersona?.avatar || 'general'}`">
        {{ activeSession.personaName?.charAt(0) || '面' }}
      </div>
      <span>当前面试官</span>
      <strong>{{ activeSession.personaName || '面试官' }}</strong>
      <small>{{ activeSession.personaTitle || '模拟面试官' }}</small>
      <p>{{ activePersonaStyle }}</p>
    </div>

    <div class="sidebar-question-card">
      <span>{{ activeSession.completed ? '本轮已完成' : '当前题目' }}</span>
      <strong>{{ activeSession.completed ? '完成' : `第 ${activeSession.currentQuestionIndex} 题` }}</strong>
      <small>{{ activeSession.currentQuestionIndex }} / {{ activeSession.totalQuestions }}</small>
    </div>
    <div class="sidebar-dots">
      <span
        v-for="step in activeSession.totalQuestions || 3"
        :key="step"
        class="sidebar-dot"
        :class="{
          active: !activeSession.completed && step === activeSession.currentQuestionIndex && viewingHistoryIndex === null,
          completed: completedQuestionSteps.includes(step),
          viewing: step === viewingHistoryIndex,
        }"
      >
        {{ step }}
      </span>
    </div>

    <div v-if="planSessions.length > 1" class="sidebar-round-card">
      <span>{{ reviewMode ? '复盘轮次' : '本次轮次' }}</span>
      <button
        v-for="(session, index) in planSessions"
        :key="session.sessionId"
        :data-test="`switch-round-${session.sessionId}`"
        type="button"
        class="round-switch-button"
        :class="[interviewRoundStatus(session), { active: session.sessionId === activeSessionId }]"
        :disabled="actionLoading"
        @click="emit('switch-session', session.sessionId)"
      >
        <i>{{ index + 1 }}</i>
        <strong>{{ session.personaName || '面试官' }}</strong>
        <small>{{ roundStatusText(session) }}</small>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import type { InterviewerPersona, InterviewStatusResponse } from '../../types/interview'
import { interviewRoundStatus } from '../../utils/interviewRecords'

defineProps<{
  activeSession: InterviewStatusResponse
  activePersona: InterviewerPersona | null
  activePersonaStyle: string
  planSessions: InterviewStatusResponse[]
  activeSessionId: number
  completedQuestionSteps: number[]
  viewingHistoryIndex: number | null
  reviewMode: boolean
  actionLoading: boolean
}>()

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
</script>

<style scoped>
.chat-sidebar{width:220px;flex-shrink:0;padding:16px;border-right:1px solid var(--line,#e5eaf2);background:var(--surface-solid,#fff);overflow:auto}.sidebar-back-btn{display:grid;place-items:center;width:34px;height:34px;border:1px solid var(--line,#e5eaf2);border-radius:11px;background:var(--surface-solid,#fff);color:var(--copy,#475569);cursor:pointer}.sidebar-persona-card{display:grid;justify-items:center;margin-top:18px;text-align:center}.sidebar-persona-avatar{display:grid;place-items:center;width:62px;height:62px;border-radius:20px;background:var(--bg-hover);color:var(--copy);font-size:22px;font-weight:900}.avatar-architect{background:var(--bg-hover)}.avatar-hr{background:var(--bg-hover)}.sidebar-persona-card>span,.sidebar-question-card>span,.sidebar-round-card>span{margin-top:10px;color:var(--muted,#94a3b8);font-size:10px;font-weight:900;letter-spacing:.08em}.sidebar-persona-card strong{margin-top:4px;color:var(--ink,#0f172a)}.sidebar-persona-card small,.sidebar-persona-card p{color:var(--muted,#64748b);font-size:11px}.sidebar-persona-card p{line-height:1.5}.sidebar-question-card{display:grid;gap:4px;margin-top:18px;padding:12px;border-radius:14px;background:var(--surface,#f8fafc)}.sidebar-question-card strong{color:var(--ink,#0f172a)}.sidebar-question-card small{color:var(--muted,#64748b)}.sidebar-dots{display:flex;flex-wrap:wrap;gap:5px;margin-top:10px}.sidebar-dot{display:grid;place-items:center;width:25px;height:25px;border-radius:999px;background:var(--surface,#f1f5f9);color:var(--muted,#64748b);font-size:10px;font-weight:900}.sidebar-dot.active{background:var(--brand);color:#fff}.sidebar-dot.completed{background:var(--brand-soft,#d1fae5);color:var(--brand,#047857)}.sidebar-dot.viewing{outline:2px solid var(--brand,#10b981)}.sidebar-round-card{display:grid;gap:7px;margin-top:18px}.round-switch-button{display:grid;grid-template-columns:24px 1fr;grid-template-rows:auto auto;column-gap:7px;padding:8px;border:1px solid var(--line,#e5eaf2);border-radius:12px;background:var(--surface-solid,#fff);text-align:left;cursor:pointer}.round-switch-button i{grid-row:1/3;display:grid;place-items:center;width:24px;height:24px;border-radius:999px;background:var(--surface,#f1f5f9);font-size:10px;font-style:normal}.round-switch-button strong{font-size:11px}.round-switch-button small{color:var(--muted,#64748b);font-size:10px}.round-switch-button.active{border-color:var(--brand,#10b981);background:var(--brand-soft,#f0fdf4)}.round-switch-button.completed i{background:var(--brand-soft,#d1fae5);color:var(--brand,#047857)}.round-switch-button.failed{border-color:var(--danger-soft,#fecaca)}.round-switch-button.cancelled{opacity:.7}@media(max-width:900px){.chat-sidebar{width:100%;border-right:0;border-bottom:1px solid var(--line,#e5eaf2)}}
</style>
