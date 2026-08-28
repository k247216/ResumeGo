<template>
  <aside class="room-context-panel" data-test="interview-room-context-panel">
    <header class="room-context-header">
      <h2>本次上下文</h2>
      <span class="room-context-lock">开始后锁定</span>
    </header>

    <dl class="room-context-list">
      <div v-for="item in contextItems" :key="item.label" class="room-context-item">
        <span class="room-context-item-icon" aria-hidden="true"><el-icon><component :is="item.icon" /></el-icon></span>
        <div class="room-context-item-copy">
          <dt>{{ item.label }}</dt>
          <dd>{{ item.value }}</dd>
          <small v-if="item.meta">{{ item.meta }}</small>
        </div>
      </div>
    </dl>

    <section class="room-context-focus">
      <span class="room-context-label">当前考察重点</span>
      <div v-if="focusTags.length" class="room-context-tags">
        <span v-for="tag in focusTags" :key="tag">{{ tag }}</span>
      </div>
      <span v-else class="room-context-empty">未设置重点，按当前上下文综合考察</span>
    </section>

    <section class="room-context-system">
      <span class="room-context-label">系统状态</span>
      <div class="room-context-system-status">
        <span class="room-context-status-dot" :class="{ warning: (systemStatus || '').includes('未配置') }" aria-hidden="true"></span>
        <strong>{{ systemStatus || 'API 状态待检测' }}</strong>
      </div>
      <small>模型：{{ modelLabel || '当前 AI 模型' }}</small>
      <div class="room-context-system-meta">
        <span>响应 {{ responseLatencyMs == null ? '—' : `${responseLatencyMs} ms` }}</span>
        <span>练习 {{ practiceElapsed || '0 秒' }}</span>
      </div>
    </section>

    <footer class="room-context-footer">
      <span>{{ modeLabel }} · 上下文已锁定</span>
    </footer>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Aim, Briefcase, Collection, Document, Reading, Tickets, User } from '@element-plus/icons-vue'
import type { InterviewMode, InterviewStatusResponse } from '../../types/interview'

interface RoomPlan {
  mode?: InterviewMode
  jobLabel?: string | null
  resumeLabel?: string | null
  questionCount?: number | null
  focusTags?: string[] | null
  startContextSnapshot?: Record<string, unknown> | null
}

interface ContextItem {
  label: string
  value: string
  meta?: string
  icon: unknown
}

const props = defineProps<{
  mode: InterviewMode
  plan: RoomPlan
  session: InterviewStatusResponse
  activePersona: { name?: string; title?: string } | null
  modelLabel?: string
  systemStatus?: string
  responseLatencyMs?: number | null
  practiceElapsed?: string
}>()

const snapshot = computed(() => props.plan.startContextSnapshot ?? {})
const modeLabel = computed(() => ({
  ROLE_BASED: '自由面试',
  KNOWLEDGE_TRAINING: '知识训练',
  EXPERIENCE_SIMULATION: '真题演练',
}[props.mode]))
function text(key: string): string {
  const value = snapshot.value[key]
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

function list(key: string): string[] {
  const value = snapshot.value[key]
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    : []
}

function firstAvailable(...values: Array<string | null | undefined>): string {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() || '未记录'
}

function resumeLabel() {
  const title = text('resumeTitle') || props.plan.resumeLabel || ''
  const version = snapshot.value.resumeVersionNo
  if (title && typeof version === 'number') return `${title} · V${version}`
  return firstAvailable(title)
}

const contextItems = computed<ContextItem[]>(() => {
  if (props.mode === 'ROLE_BASED') {
    return [
      { label: '岗位', value: firstAvailable(text('jobProjectName'), props.plan.jobLabel), icon: Briefcase },
      { label: '简历', value: resumeLabel(), icon: Document },
      { label: '面试官', value: firstAvailable(props.activePersona?.name, props.session.personaName), meta: props.activePersona?.title || props.session.personaTitle || '', icon: User },
    ]
  }

  if (props.mode === 'KNOWLEDGE_TRAINING') {
    const documents = list('knowledgeDocumentTitles')
    const difficulty = firstAvailable(text('difficulty'), text('questionStyle'))
    return [
      { label: '知识资料', value: documents.length ? documents.join('、') : '未记录资料', meta: 'Knowledge Base', icon: Reading },
      { label: '训练策略', value: difficulty, meta: props.plan.questionCount ? `${props.plan.questionCount} 题` : '', icon: Aim },
    ]
  }

  const sourceType = text('questionSetSourceType')
  return [
    { label: '真实题集', value: firstAvailable(text('questionSetTitle')), meta: sourceType === 'IMPORTED_EXPERIENCE' ? '外部导入' : sourceType === 'USER_MANUAL' ? '手动面经' : '知识库来源', icon: Tickets },
    { label: '原题数量', value: props.plan.questionCount ? `${props.plan.questionCount} 题` : '未记录', icon: Collection },
    { label: '答题回顾', value: reviewModeLabel(text('reviewMode')), icon: Aim },
  ]
})

function reviewModeLabel(value: string) {
  if (value === 'PER_QUESTION') return '逐题回顾'
  if (value === 'SOURCE_ONLY') return '只看原题'
  if (value === 'END_OF_SESSION') return '结束后复盘'
  return '结束后复盘'
}

const focusTags = computed(() => {
  const fromSnapshot = list('focusTags')
  return fromSnapshot.length ? fromSnapshot : (props.plan.focusTags ?? []).filter(Boolean)
})
</script>

<style scoped>
.room-context-panel {
  width: 224px;
  flex: 0 0 224px;
  min-width: 0;
  overflow-y: auto;
  padding: 22px 18px 18px;
  border-left: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
  color: var(--ink, #171717);
}
.room-context-header { display: grid; gap: 4px; }
.room-context-kicker,
.room-context-label { color: var(--muted, #989893); font-size: 9px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
.room-context-header h2 { margin: 0; font-size: 17px; letter-spacing: -.03em; }
.room-context-header p { margin: 2px 0 0; color: var(--muted, #989893); font-size: 10px; line-height: 1.5; }
.room-context-mode { display: flex; align-items: center; gap: 9px; margin-top: 22px; padding-bottom: 15px; border-bottom: 1px solid var(--border-subtle, #e4e7e5); }
.room-context-mode-icon { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 9px; background: var(--bg-surface, #f7f8f7); color: var(--ink, #171717); }
.room-context-mode.mode-KNOWLEDGE_TRAINING .room-context-mode-icon { color: #7560a8; }
.room-context-mode.mode-EXPERIENCE_SIMULATION .room-context-mode-icon { color: #226eb4; }
.room-context-mode > span:last-child { display: grid; min-width: 0; gap: 2px; }
.room-context-mode strong { font-size: 12px; }
.room-context-mode small { color: var(--muted, #989893); font-size: 10px; }
.room-context-list { display: grid; gap: 0; margin: 0; }
.room-context-item { display: grid; gap: 4px; padding: 14px 0; border-bottom: 1px solid var(--border-subtle, #e4e7e5); }
.room-context-item dt { color: var(--muted, #989893); font-size: 10px; }
.room-context-item dd { margin: 0; color: var(--ink, #171717); font-size: 12px; font-weight: 700; line-height: 1.45; overflow-wrap: anywhere; }
.room-context-item small { color: var(--muted, #989893); font-size: 10px; line-height: 1.45; }
.room-context-focus { display: grid; gap: 8px; padding-top: 16px; }
.room-context-tags { display: flex; flex-wrap: wrap; gap: 5px; }
.room-context-tags span { padding: 4px 7px; border-radius: 999px; background: var(--bg-surface, #f7f8f7); color: var(--copy, #6e6e6a); font-size: 10px; }
.room-context-empty { color: var(--muted, #989893); font-size: 11px; line-height: 1.45; }
.room-context-system { display: grid; gap: 8px; padding: 18px 0 0; border-top: 1px solid var(--border-subtle, #e4e7e5); }
.room-context-system-status { display: flex; align-items: center; gap: 7px; color: var(--ink, #171717); font-size: 12px; }
.room-context-system-status strong { font-weight: 650; }
.room-context-system > small { color: var(--muted, #989893); font-size: 10px; line-height: 1.45; overflow-wrap: anywhere; }
.room-context-system-meta { display: flex; flex-wrap: wrap; gap: 8px 12px; color: var(--muted, #989893); font-size: 10px; line-height: 1.45; }
.room-context-footer { display: flex; align-items: center; gap: 6px; margin-top: 22px; color: var(--muted, #989893); font-size: 10px; }
.room-context-status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--brand, #168b68); }
.room-context-status-dot.warning { background: #b7791f; }
@media (max-width: 1120px) { .room-context-panel { width: 204px; flex-basis: 204px; padding-inline: 14px; } }
@media (max-width: 860px) { .room-context-panel { display: none; } }

/* Room reference alignment: the inspector is a quiet document rail, not a card. */
.room-context-panel { position: relative; width: auto; flex-basis: auto; padding: 14px 18px 16px; border-left: 1px solid #e4e7e5; }
.room-context-panel::before { position: absolute; top: -49px; bottom: auto; left: -1px; width: 1px; height: 49px; background: #e4e7e5; content: ''; }
.room-context-header { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #e4e7e5; }
.room-context-header h2 { font-size: 18px; letter-spacing: -.035em; }
.room-context-lock { color: #989893; font-size: 10px; }
.room-context-mode { display: none; }
.room-context-list { display: grid; }
.room-context-item { display: flex; align-items: flex-start; gap: 12px; min-width: 0; padding: 14px 0; }
.room-context-item-icon { display: grid; width: 34px; height: 34px; flex: 0 0 34px; place-items: center; border-radius: 10px; background: #f5f6f4; color: #272927; }
.room-context-item-copy { display: grid; min-width: 0; gap: 4px; }
.room-context-item dt { font-size: 10px; }
.room-context-item dd { font-size: 12px; font-weight: 700; }
.room-context-item small { font-size: 10px; }
.room-context-focus { padding: 14px 0; border-top: 1px solid #e4e7e5; }
.room-context-system { padding-top: 14px; }
.room-context-footer { margin-top: 16px; padding-top: 14px; border-top: 1px solid #e4e7e5; }
@media (max-width: 1120px) { .room-context-panel { padding-inline: 14px; } }
</style>
