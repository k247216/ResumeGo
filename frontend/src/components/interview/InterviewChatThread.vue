<template>
  <div class="chat-messages" ref="chatMessagesRef">
    <!-- 聊天消息列表 -->
    <div
      v-for="(msg, idx) in messages"
      :key="idx"
      class="chat-message"
      :class="msg.role"
    >
      <!-- 面试官消息 -->
      <template v-if="msg.role === 'interviewer'">
        <div class="msg-avatar" :class="'avatar-' + (activeSessionPersona?.avatar || 'general')">
          {{ activeSessionPersona?.name?.charAt(0) || '面' }}
        </div>
        <div class="msg-bubble interviewer-bubble">
          <div class="bubble-header">
            <span class="bubble-name">{{ activeSession?.personaName || '面试官' }}</span>
            <span class="bubble-question-num">第 {{ msg.questionIndex }} / {{ activeSession?.totalQuestions }} 题</span>
          </div>
          <div class="bubble-text">{{ msg.text }}</div>
        </div>
      </template>

      <!-- 用户消息 -->
      <template v-else-if="msg.role === 'user'">
        <div class="msg-bubble user-bubble">
          <div class="bubble-text">{{ msg.text }}</div>
        </div>
        <div class="msg-avatar user-avatar">我</div>
      </template>

      <!-- 发送中加载指示器 -->
      <template v-else-if="msg.role === 'sending'">
        <div class="msg-bubble user-bubble">
          <div class="bubble-text">{{ msg.text }}</div>
        </div>
        <div class="msg-avatar user-avatar">我</div>
        <div class="sending-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>{{ formatElapsedTime }}</span>
        </div>
      </template>

      <!-- 评价卡片 -->
      <template v-else-if="msg.role === 'evaluation' && msg.evaluation">
        <div class="evaluation-inline">
          <div class="eval-header">
            <el-icon><Trophy /></el-icon>
            <span>本题评价</span>
          </div>
          <div v-if="msg.evaluation.score" class="eval-overall-card">
            <span>本题综合表现</span>
            <strong>{{ questionEvaluationAverage(msg.evaluation.score) }}<small>/10</small></strong>
            <p>{{ questionEvaluationCopy(msg.evaluation.score) }}</p>
          </div>
          <div v-if="msg.evaluation.score" class="eval-score-row">
            <div class="eval-score-item">
              <span>清晰度</span>
              <el-progress :percentage="msg.evaluation.score.clarity * 10" :show-text="false" :stroke-width="5" color="var(--brand)" />
              <strong>{{ msg.evaluation.score.clarity }}/10</strong>
            </div>
            <div class="eval-score-item">
              <span>相关性</span>
              <el-progress :percentage="msg.evaluation.score.relevance * 10" :show-text="false" :stroke-width="5" color="var(--brand)" />
              <strong>{{ msg.evaluation.score.relevance }}/10</strong>
            </div>
            <div class="eval-score-item">
              <span>深度</span>
              <el-progress :percentage="msg.evaluation.score.depth * 10" :show-text="false" :stroke-width="5" color="var(--brand)" />
              <strong>{{ msg.evaluation.score.depth }}/10</strong>
            </div>
            <div class="eval-score-item">
              <span>准确度</span>
              <el-progress :percentage="msg.evaluation.score.accuracy * 10" :show-text="false" :stroke-width="5" color="var(--warning)" />
              <strong>{{ msg.evaluation.score.accuracy }}/10</strong>
            </div>
          </div>
          <div v-if="msg.evaluation.strengths?.length" class="eval-section">
            <h4><el-icon><CircleCheck /></el-icon> 亮点</h4>
            <ul><li v-for="s in msg.evaluation.strengths" :key="s">{{ s }}</li></ul>
          </div>
          <div v-if="msg.evaluation.weaknesses?.length" class="eval-section">
            <h4><el-icon><Warning /></el-icon> 可加强</h4>
            <ul><li v-for="w in msg.evaluation.weaknesses" :key="w">{{ w }}</li></ul>
          </div>
          <div v-if="msg.evaluation.suggestions?.length" class="eval-section">
            <h4>建议</h4>
            <p>{{ msg.evaluation.suggestions.join('；') }}</p>
          </div>
          <div v-if="msg.evaluation.referenceAnswer" class="eval-section ref-answer">
            <h4><el-icon><Trophy /></el-icon> 参考回答</h4>
            <p>{{ msg.evaluation.referenceAnswer }}</p>
          </div>
        </div>
      </template>

      <!-- 总结卡片 -->
      <template v-else-if="msg.role === 'summary'">
        <div class="summary-inline">
          <div class="summary-header">
            <el-icon><Trophy /></el-icon>
            <h3>练习总结</h3>
          </div>
          <p class="summary-desc">{{ summaryDescription }}</p>
          <div v-if="summaryStrengths.length" class="summary-block">
            <h4>本次亮点</h4>
            <span v-for="s in summaryStrengths" :key="s"><el-icon><CircleCheck /></el-icon>{{ s }}</span>
          </div>
          <div v-if="summarySuggestions.length" class="summary-block">
            <h4>下一步建议</h4>
            <span v-for="s in summarySuggestions" :key="s"><el-icon><ArrowRight /></el-icon>{{ s }}</span>
          </div>
          <div v-if="perQuestionScores.length > 0" class="summary-scores">
            <h4>本轮评分画像</h4>
            <div v-if="roundScoreSummary" class="round-score-overview">
              <div class="round-score-main">
                <span>{{ roundScoreSummary.displayAverage }}</span>
                <small>/10</small>
              </div>
              <div class="round-score-copy">
                <strong>薄弱维度：{{ roundScoreSummary.weakest.label }}</strong>
                <p>{{ trainingHintForDimension(roundScoreSummary.weakest.key) }}</p>
              </div>
            </div>
            <div class="summary-score-cards">
              <div v-for="score in perQuestionScores" :key="score.questionIndex" class="summary-score-card">
                <span class="sq-label">第 {{ score.questionIndex }} 题</span>
                <div class="sq-dims">
                  <span>清晰度 {{ score.clarity }}</span>
                  <span>相关性 {{ score.relevance }}</span>
                  <span>深度 {{ score.depth }}</span>
                  <span>准确度 {{ score.accuracy }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="reviewMode" class="summary-actions-row">
            <span class="review-inline-hint">可点击上方按钮查看整次复盘，也可以切换左侧轮次查看完整对话。</span>
          </div>
          <div v-else class="summary-actions-row">
            <button
              v-if="nextPlannedPersona"
              class="interview-outline-button"
              type="button"
              :disabled="actionLoading"
              @click="$emit('next-persona')"
            >
              进入下一位：{{ nextPlannedPersona.name }}
              <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><ArrowRight /></el-icon>
            </button>
            <button
              v-if="canReturnToWorkspace"
              class="interview-outline-button"
              type="button"
              @click="$emit('go-optimization')"
            >
              回到简历优化
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </template>
    </div>

    <!-- 重试卡片 -->
    <div v-if="!reviewMode && retryable" class="retry-card">
      <div class="retry-message">
        <el-icon><Warning /></el-icon>
        <span>AI 评价暂时不可用，你可以重试提交</span>
      </div>
      <div v-if="lastSubmitAnswer" class="retry-answer-preview">
        <span class="retry-answer-label">已提交的回答：</span>
        <p>{{ lastSubmitAnswer }}</p>
      </div>
      <button class="interview-outline-button retry-button" type="button" :disabled="actionLoading" @click="$emit('retry-submit')">
        重试评价
        <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><RefreshRight /></el-icon>
      </button>
    </div>
  </div>
</template>


<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ArrowRight, CircleCheck, Loading, RefreshRight, Trophy, Warning } from '@element-plus/icons-vue'
import {
  questionEvaluationAverage,
  questionEvaluationCopy,
  trainingHintForDimension,
  type ScoreSummary,
} from '../../utils/interviewReview'
import type { EvaluationSummary, InterviewStatusResponse } from '../../types/interview'

export interface InterviewChatMessage {
  role: 'interviewer' | 'user' | 'sending' | 'evaluation' | 'summary'
  text?: string
  questionIndex?: number
  evaluation?: EvaluationSummary | null
}

const props = defineProps<{
  messages: InterviewChatMessage[]
  activeSession: InterviewStatusResponse | null
  activeSessionPersona: { name?: string; avatar?: string } | null
  formatElapsedTime: string
  summaryDescription: string
  summaryStrengths: string[]
  summarySuggestions: string[]
  perQuestionScores: { questionIndex: number; clarity: number; relevance: number; depth: number; accuracy: number }[]
  roundScoreSummary: ScoreSummary | null
  reviewMode: boolean
  actionLoading: boolean
  nextPlannedPersona: { name: string } | null
  canReturnToWorkspace: boolean
  retryable: boolean
  lastSubmitAnswer: string
}>()

const emit = defineEmits<{
  (e: 'next-persona'): void
  (e: 'go-optimization'): void
  (e: 'retry-submit'): void
}>()

const chatMessagesRef = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    const el = chatMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(
  () => props.messages,
  () => scrollToBottom(),
  { deep: true },
)
watch(
  () => props.actionLoading,
  (val) => {
    if (!val) scrollToBottom()
  },
)
</script>


<style scoped>
.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.chat-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  max-width: 100%;
}

.chat-message.interviewer {
  flex-direction: row;
}

.chat-message.user,
.chat-message.sending {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  color: #fff;
}

.msg-avatar.user-avatar {
  background: var(--brand, #10b981);
}

.bubble-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.bubble-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--ink, #101a33);
}

.bubble-question-num {
  font-size: 11px;
  color: var(--muted, #94a3b8);
}

.bubble-text {
  font-size: 14px;
  line-height: 1.65;
  color: var(--ink, #101a33);
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
}

.interviewer-bubble {
  background: var(--surface, #f1f5f9);
  border: 1px solid var(--line, #e5e7eb);
}

.user-bubble {
  background: var(--brand, #10b981);
  color: #fff;
}

.user-bubble .bubble-text {
  color: #fff;
}

.sending-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--muted, #94a3b8);
  align-self: center;
}

.evaluation-inline {
  width: 100%;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  color: var(--ink, #101a33);
}

.eval-overall-card {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.eval-overall-card strong {
  font-size: 28px;
  font-weight: 700;
  color: var(--brand, #10b981);
}

.eval-overall-card strong small {
  font-size: 13px;
  color: var(--muted, #94a3b8);
}

.eval-score-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.eval-score-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--muted, #64748b);
}

.eval-score-item strong {
  font-size: 12px;
  color: var(--ink, #101a33);
  width: 34px;
  text-align: right;
}

.eval-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.eval-section h4 {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink, #101a33);
  margin: 0;
}

.eval-section ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--copy, #334155);
  line-height: 1.6;
}

.eval-section p {
  margin: 0;
  font-size: 13px;
  color: var(--copy, #334155);
  line-height: 1.6;
}

.ref-answer {
  border-top: 1px solid var(--line, #e5e7eb);
  padding-top: 8px;
}

.summary-inline {
  width: 100%;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  color: var(--ink, #101a33);
}

.summary-desc {
  margin: 0;
  font-size: 13px;
  color: var(--copy, #334155);
  line-height: 1.6;
}

.summary-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-block h4 {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink, #101a33);
}

.summary-block span {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: var(--copy, #334155);
  line-height: 1.5;
}

.summary-block .el-icon {
  color: var(--brand, #10b981);
  margin-top: 2px;
}

.summary-scores {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-scores h4 {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink, #101a33);
}

.round-score-overview {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 10px;
  padding: 12px;
}

.round-score-main {
  display: flex;
  align-items: baseline;
}

.round-score-main span {
  font-size: 30px;
  font-weight: 700;
  color: var(--brand, #10b981);
}

.round-score-main small {
  font-size: 13px;
  color: var(--muted, #94a3b8);
}

.round-score-copy strong {
  font-size: 13px;
  color: var(--ink, #101a33);
}

.round-score-copy p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--muted, #64748b);
}

.summary-score-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-score-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 8px;
  background: var(--surface, #f8fafc);
}

.sq-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink, #101a33);
}

.sq-dims {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--muted, #64748b);
}

.summary-actions-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.review-inline-hint {
  font-size: 12px;
  color: var(--muted, #94a3b8);
  line-height: 1.6;
}

.interview-outline-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--border-default, #e2e8f0);
  border-radius: 10px;
  background: var(--bg-surface, #fff);
  color: var(--ink, #101a33);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.interview-outline-button:hover:not(:disabled) {
  border-color: var(--brand, #10b981);
  color: var(--brand, #047857);
}

.interview-outline-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.retry-card {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border: 1px solid var(--warning, #f59e0b);
  border-radius: 10px;
  background: var(--warning-soft, #fffbeb);
}

.retry-message {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.retry-answer-preview {
  width: 100%;
}

.retry-answer-label {
  font-size: 12px;
  color: var(--warning, #f59e0b);
  font-weight: 600;
}

.retry-answer-preview p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--copy, #334155);
  line-height: 1.5;
  max-height: 80px;
  overflow-y: auto;
}
</style>

