<template>
  <div class="interview-page">
    <header class="interview-command-bar" data-test="interview-command-bar">
      <div class="bar-identity">
        <h1 class="bar-title"><span class="bar-title-icon" aria-hidden="true"><el-icon :size="17"><Microphone /></el-icon></span><span>模拟面试</span></h1>
        <span class="bar-subtitle">岗位模拟 · 知识训练 · 面经模拟</span>
      </div>
    </header>
    <div v-if="fromWorkspace" class="workspace-return-bar">
      <button type="button" :disabled="!canReturnToWorkspace" @click="returnToEditor">← {{ workspaceReturnLabel }}</button>
      <span>{{ canReturnToWorkspace ? '模拟面试将作为当前简历的改进输入' : '请先完成本次多轮面试，再回到简历工作台' }}</span>
    </div>

    <!-- ========== 面试大厅（未选中活跃会话时显示） ========== -->
    <template v-if="!activeSessionId">
      <el-alert
        v-if="errorMessage"
        class="interview-preview-note"
        :title="errorMessage"
        type="error"
        show-icon
        closable
        @close="errorMessage = ''"
      />

      <div v-if="actionLoading" class="interview-loading-bar">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>{{ actionStage }}</span>
        <span class="elapsed-time">{{ formatElapsedTime }}</span>
      </div>

      <!-- ========== 三模式训练入口（英雄区，常驻） ========== -->
      <section class="lobby-three-mode" data-test="three-mode-entry">
        <InterviewResultWorkspace v-if="threeModePlan" :plan="threeModePlan" />
        <InterviewComposer v-else @started="onThreeModeStarted" />
      </section>

      <div class="lobby-shell">
        <section class="interview-context-card lobby-create-card">
          <div class="context-heading">
            <span class="context-step">01</span>
            <div>
              <h2>新建一次面试演练</h2>
              <p>把简历版本、目标岗位和面试官队列固定下来，形成一组可连续复盘的面试计划。</p>
            </div>
          </div>

          <!-- 针对：目标岗位 + 简历版本 -->
          <div class="composer-section">
            <div class="composer-section-head">
              <div>
                <h3>针对</h3>
                <p>本次模拟面试围绕的简历版本与目标岗位。</p>
              </div>
              <div v-if="workspaceContextLocked" class="section-head-actions">
                <button type="button" class="soft-toggle-btn" data-test="change-target" @click="returnToEditor">更换目标<el-icon :size="12"><ArrowRight /></el-icon></button>
                <button type="button" class="soft-toggle-btn" data-test="change-resume" @click="returnToEditor">更换简历<el-icon :size="12"><ArrowRight /></el-icon></button>
              </div>
            </div>
            <div v-if="workspaceContextLocked" class="target-locked-rows">
              <div class="target-locked-row">
                <span class="target-locked-label">目标</span>
                <strong>{{ selectedJobDisplayLabel }}</strong>
              </div>
              <div class="target-locked-row">
                <span class="target-locked-label">简历</span>
                <strong>{{ selectedResumeDisplayLabel }}</strong>
              </div>
            </div>
            <div v-else class="composer-fields">
              <label class="composer-field">
                <span class="composer-field-label">简历</span>
                <el-select v-model="selectedVersionId" placeholder="选择简历版本" :loading="loadingOptions">
                  <el-option v-for="item in resumeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
              <label class="composer-field">
                <span class="composer-field-label">目标</span>
                <el-select v-model="selectedJobId" placeholder="选择目标岗位" :loading="loadingOptions" filterable>
                  <el-option v-for="item in jobOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
          </div>

          <!-- 面试形式：题目数量 -->
          <div class="composer-section">
            <div class="composer-section-head">
              <div>
                <h3>面试形式</h3>
                <p>本次练习的题目数量，单场预计按每题 3–4 分钟估算。</p>
              </div>
            </div>
            <label class="composer-field question-count-slider">
              <span class="composer-field-label">题目</span>
              <div class="question-slider-shell">
                <span class="slider-limit">3</span>
                <el-slider v-model="questionCount" :min="3" :max="10" :step="1" show-stops />
                <span class="slider-limit">10</span>
                <strong class="slider-value">{{ questionCount }} 道</strong>
              </div>
            </label>
          </div>

          <!-- 面试官：队列行 + 展开拾取 -->
          <div class="persona-section">
            <div class="persona-section-head">
              <div>
                <h3>面试官</h3>
                <p>队列第一位先开始；完成后自动进入下一位。</p>
              </div>
              <div class="section-head-actions">
                <button class="soft-toggle-btn" type="button" @click="personaPanelExpanded = !personaPanelExpanded">
                  {{ personaPanelExpanded ? '收起' : '更多面试官 ›' }}
                </button>
                <button class="add-persona-btn compact" type="button" @click="showPersonaDialog = true">
                  <el-icon><Plus /></el-icon> 添加面试官
                </button>
              </div>
            </div>
            <div v-if="selectedPersonaQueue.length" class="persona-queue">
              <div
                v-for="(persona, index) in selectedPersonaQueue"
                :key="persona.id"
                class="persona-queue-item"
              >
                <div class="persona-avatar" :class="'avatar-' + (persona.avatar || 'general')">{{ persona.name.charAt(0) }}</div>
                <div class="persona-queue-copy">
                  <strong>{{ persona.name }}</strong>
                  <span>{{ persona.title || '模拟面试官' }}</span>
                  <span v-if="persona.style" class="persona-queue-style">{{ persona.style }}</span>
                </div>
                <div class="persona-queue-actions">
                  <button type="button" title="前移" :disabled="index === 0" @click.stop="movePersonaInPlan(persona.id, -1)"><el-icon :size="11"><ArrowUp /></el-icon></button>
                  <button type="button" title="后移" :disabled="index === selectedPersonaQueue.length - 1" @click.stop="movePersonaInPlan(persona.id, 1)"><el-icon :size="11"><ArrowDown /></el-icon></button>
                  <button type="button" title="移除" @click.stop="removePersonaFromPlan(persona.id)"><el-icon :size="11"><Close /></el-icon></button>
                </div>
              </div>
            </div>
            <p v-else class="persona-queue-empty">先选择至少一位面试官，本阶段会启动队列第一位。</p>
            <div v-show="personaPanelExpanded" class="persona-cards">
              <div
                v-for="p in personas"
                :key="p.id"
                class="persona-card"
                :class="{ selected: selectedPersonaIds.includes(p.id), primary: selectedPersonaId === p.id }"
                @click="togglePersonaPlan(p.id)"
              >
                <div class="persona-card-top">
                  <div class="persona-avatar" :class="'avatar-' + (p.avatar || 'general')">{{ p.name.charAt(0) }}</div>
                  <span v-if="personaPlanIndex(p.id)" class="persona-order-badge">{{ personaPlanIndex(p.id) }}</span>
                  <el-icon v-if="selectedPersonaIds.includes(p.id)" class="persona-check"><CircleCheck /></el-icon>
                  <button
                    v-if="p.type === 'custom'"
                    type="button"
                    class="persona-delete-btn"
                    title="删除自定义角色"
                    @click.stop="handleDeletePersona(p)"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <div class="persona-info">
                  <span class="persona-name">{{ p.name }}</span>
                  <span class="persona-title">{{ p.title }}</span>
                  <span class="persona-style">{{ p.style }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="lobby-context-extra" :class="{ collapsed: !extraPanelExpanded }">
            <div class="context-extra-head">
              <div>
                <h3>本轮重点</h3>
                <p>标记本次练习最想验证的能力方向，便于结束后统一复盘。</p>
              </div>
              <button class="soft-toggle-btn" type="button" @click="extraPanelExpanded = !extraPanelExpanded">
                {{ extraPanelExpanded ? '完成' : '编辑 ›' }}
              </button>
            </div>
            <div v-if="!extraPanelExpanded" class="extra-collapsed-summary">
              {{ selectedFocusSummary }}
            </div>
            <div v-show="extraPanelExpanded" class="focus-chip-list">
              <button
                v-for="tag in focusTagOptions"
                :key="tag"
                type="button"
                class="focus-chip"
                :class="{ active: selectedFocusTags.includes(tag) }"
                @click="toggleFocusTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
            <label v-show="extraPanelExpanded" class="supplement-field">
              <span>补充信息</span>
              <el-input
                v-model="interviewSupplement"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="例如：希望重点追问分布式项目、实习贡献边界、岗位动机等。"
              />
            </label>
          </div>

          <div class="context-start-row">
            <button
              class="interview-start-button"
              type="button"
              :disabled="!canCreateSession || actionLoading"
              @click="handleCreateAndStart"
            >
              {{ startInterviewButtonLabel }}
              <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><ArrowRight /></el-icon>
            </button>
            <span class="start-hint">当前会启动队列第 1 位面试官；回答评价、单轮总结和综合复盘能力保持不变。</span>          </div>
        </section>

        <InterviewHistoryPanel
          v-model:active-filter="historyFilter"
          :records="visibleInterviewRecords"
          :filtered-records="filteredInterviewRecords"
          :filter-tabs="historyFilterTabs"
          :current-plan="currentPlanSummary"
          @open="openInterviewRecord"
          @delete="deleteInterviewRecord"
        />
      </div>

      <button data-test="view-growth" type="button" class="lobby-growth-button" :disabled="growthLoading" @click="loadGrowthData">
        <el-icon v-if="growthLoading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><Trophy /></el-icon>
        查看成长趋势
      </button>
    </template>

    <!-- 自定义人设弹窗 -->
    <el-dialog v-model="showPersonaDialog" title="创建自定义面试官" width="420px">
      <el-form :model="customPersonaForm" label-position="top">
        <el-form-item label="姓名" required>
          <el-input v-model="customPersonaForm.name" maxlength="20" placeholder="如：张总监" />
        </el-form-item>
        <el-form-item label="职位" required>
          <el-input v-model="customPersonaForm.title" maxlength="50" placeholder="如：资深后端架构师" />
        </el-form-item>
        <el-form-item label="风格描述" required>
          <el-input
            v-model="customPersonaForm.style"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="如：严谨深入，注重系统设计能力，擅长追问技术细节"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPersonaDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!customPersonaFormValid" @click="handleCreatePersona">创建</el-button>
      </template>
    </el-dialog>

    <!-- ========== 成长趋势弹窗 ========== -->
    <GrowthTrendDialog
      v-model="showGrowthDialog"
      :report="growthReport"
      :loading="growthLoading"
      :snapshots="growthSnapshots"
      :changes="growthChanges"
      :resume-label="selectedResumeLabel"
      :dims="dimNames"
    />

    <InterviewPlanReviewDialog
      v-model="showPlanReviewDialog"
      :summary="activePlanReviewSummary"
    />

    <!-- ========== 聊天界面（有活跃会话时显示） ========== -->
    <section v-if="activeSession" class="interview-chat-layout">
      <InterviewRoomSidebar
        :active-session="activeSession"
        :active-persona="activeSessionPersona"
        :active-persona-style="activePersonaStyle"
        :plan-sessions="activePlanSessions"
        :active-session-id="activeSession.sessionId"
        :completed-question-steps="completedQuestionSteps"
        :viewing-history-index="activeState.viewingHistoryIndex"
        :review-mode="activeReviewMode"
        :action-loading="actionLoading"
        @back="backToPersona"
        @switch-session="switchToSession"
      />

      <!-- 聊天框 -->
      <div class="chat-main">
        <div v-if="activeInterviewPlan" class="chat-plan-header">
          <div>
            <span class="chat-plan-kicker">Interview Plan</span>
            <strong>{{ activeInterviewPlan.jobLabel }}</strong>
            <p>{{ activeInterviewPlan.resumeLabel }} · {{ activePlanStepLabel }} · {{ activeInterviewPlan.questionCount }} 道题</p>
          </div>
          <div class="chat-plan-status">
            <strong>{{ activePlanCompletionLabel }}</strong>
            <span>{{ activeReviewMode ? '复盘模式：可切换轮次查看完整对话' : (nextPlannedPersona ? `下一位：${nextPlannedPersona.name}` : '计划内面试官已到末尾') }}</span>
          </div>
          <div class="chat-plan-tags">
            <span v-for="tag in activeInterviewPlan.focusTags" :key="tag">{{ tag }}</span>
          </div>
          <CompanyProfileSignal
            v-if="hasCompanyProfile"
            class="chat-company-focus"
            :profile="selectedCompanyProfile"
            variant="inline"
            label="Interview Signal"
          />
          <div class="chat-plan-steps">
            <span
              v-for="(name, index) in activeInterviewPlan.personaNames"
              :key="`${activeInterviewPlan.planId}-${name}-${index}`"
              :class="{
                done: index < activePlanCompletedSessionIds.length,
                current: index === activeInterviewPlan.currentPersonaIndex,
              }"
            >
              <i>{{ index + 1 }}</i>
              {{ name }}
            </span>
          </div>
          <p v-if="activeInterviewPlan.supplement" class="chat-plan-note">
            {{ activeInterviewPlan.supplement }}
          </p>
        </div>
        <div
          v-if="activePlanReviewSummary && (activeReviewMode || activePlanFinished)"
          class="plan-review-panel"
        >
          <div class="plan-review-head">
            <div>
              <span class="chat-plan-kicker">Review Mode</span>
              <strong>正在复看本次面试对话</strong>
            </div>
            <div v-if="activePlanReviewSummary.overall" class="plan-review-score">
              <span>{{ activePlanReviewSummary.overall.displayAverage }}</span>
              <small>/10</small>
            </div>
          </div>

          <div v-if="activePlanReviewSummary.overall" class="plan-review-insight">
            <span>最高维度：{{ activePlanReviewSummary.overall.strongest.label }}</span>
            <span>最低维度：{{ activePlanReviewSummary.overall.weakest.label }}</span>
          </div>

          <button
            v-if="activePlanReviewSummary.cachedSummary || canSummarizeActivePlan"
            class="plan-review-generate"
            type="button"
            :disabled="multiSummaryLoading"
            @click="openPlanReviewDialog"
          >
            <el-icon v-if="multiSummaryLoading" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Trophy /></el-icon>
            {{
              multiSummaryLoading
                ? '正在生成整次复盘...'
                : activePlanReviewSummary.cachedSummary
                  ? '查看整次复盘'
                  : '生成并查看整次复盘'
            }}
          </button>
        </div>
        <InterviewChatThread
          :messages="chatMessages"
          :active-session="activeSession"
          :active-session-persona="activeSessionPersona"
          :format-elapsed-time="formatElapsedTime"
          :summary-description="summaryDescription"
          :summary-strengths="summaryStrengths"
          :summary-suggestions="summarySuggestions"
          :per-question-scores="activeState.perQuestionScores"
          :round-score-summary="activeRoundScoreSummary"
          :review-mode="activeReviewMode"
          :action-loading="actionLoading"
          :next-planned-persona="nextPlannedPersona"
          :can-return-to-workspace="canReturnToWorkspace"
          :retryable="activeState.retryable"
          :last-submit-answer="activeState.lastSubmitAnswer"
          @next-persona="startNextPlannedPersona"
          @go-optimization="goToOptimization"
          @retry-submit="retrySubmitAnswer"
        />

        <!-- 输入栏 -->
        <div v-if="activeReviewMode" class="review-mode-bar">
          <el-icon><Trophy /></el-icon>
          <span>复盘模式：只能查看本次面试各轮对话与总结，回答、重试和继续面试操作已关闭。</span>
        </div>
        <div v-else class="chat-input-bar">
          <div class="voice-row">
            <el-tooltip
              :content="speechSupported ? '点击开始语音输入，再次点击结束' : '当前浏览器不支持语音识别'"
              placement="top"
            >
              <button
                type="button"
                class="voice-button"
                :class="{ listening: isListening }"
                :disabled="!speechSupported || !canSubmitAnswer || actionLoading"
                @click="toggleVoiceInput"
              >
                <el-icon :class="{ 'is-pulsing': isListening }"><Microphone /></el-icon>
              </button>
            </el-tooltip>
            <span v-if="isListening" class="voice-hint">正在聆听...</span>
          </div>
          <el-input
            v-model="activeState.answerDraft"
            type="textarea"
            :rows="2"
            maxlength="1200"
            show-word-limit
            :disabled="!canSubmitAnswer || actionLoading"
            placeholder="输入你的回答..."
            @keydown.enter="handleEnterKey"
            class="chat-textarea"
          />
          <button
            class="chat-send-btn"
            type="button"
            :disabled="!canSubmitAnswer || !activeState.answerDraft.trim() || actionLoading"
            @click="handleSubmitAnswer"
          >
            <el-icon v-if="actionLoading" class="is-loading"><Loading /></el-icon>
            <span v-else>提交回答</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowRight,
  ArrowUp,
  CircleCheck,
  Close,
  Loading,
  Microphone,
  Plus,
  Trophy,
} from '@element-plus/icons-vue'
import { listJobDescriptions, resolveCompanyProfile } from '../api/job'
import { useTargetsStore } from '../stores/targets'
import type { JobProject } from '../types/project'
import CompanyProfileSignal from '../components/CompanyProfileSignal.vue'
import InterviewHistoryPanel, { type CurrentPlanSummary } from '../components/interview/InterviewHistoryPanel.vue'
import InterviewPlanReviewDialog from '../components/interview/InterviewPlanReviewDialog.vue'
import InterviewRoomSidebar from '../components/interview/InterviewRoomSidebar.vue'
import GrowthTrendDialog from '../components/interview/GrowthTrendDialog.vue'
import InterviewChatThread from '../components/interview/InterviewChatThread.vue'
import InterviewComposer from '../components/interview/InterviewComposer.vue'
import InterviewResultWorkspace from '../components/interview/InterviewResultWorkspace.vue'
import {
  createInterviewPlan,
  deleteInterviewPlan,
  generateInterviewPlanSummary,
  getInterviewStatus,
  getSessionHistory,
  listInterviewerPersonas,
  createInterviewerPersona,
  deleteInterviewerPersona,
  listMyInterviewPlans,
  listMyInterviews,
  startInterview,
  submitInterviewAnswer,
  getInterviewGrowthReport,
} from '../api/interview'
import { getResumeVersion, getResumeVersions, listResumes } from '../api/resume'
import type { CompanyProfile, JobDescription } from '../types/job'
import type {
  EvaluationSummary,
  GrowthReport,
  InterviewPlanResponse,
  InterviewPlanRound,
  InterviewerPersona,
  InterviewStatusResponse,
  MultiSessionSummaryResponse,
} from '../types/interview'
import type { Resume, ResumeVersion } from '../types/resume'
import {
  getWorkspaceSelectedJobId,
  getWorkspaceSelectedResumeId,
  markReturnToEditor,
  setWorkspaceSelectedJobId,
} from '../utils/workspaceContext'
import { buildResumeEditorLocation } from '../utils/editorRoute'
import { filterTargetInterviewRecords } from '../utils/interviewContext'
import {
  buildInterviewRecords,
  interviewRoundStatus,
  type InterviewPlanContext,
  type InterviewRecord,
} from '../utils/interviewRecords'
import {
  summarizeQuestionScores,
  type ScoreSummary,
} from '../utils/interviewReview'
import { useInterviewSessions } from '../composables/useInterviewSessions'

interface ChatMessage {
  role: 'interviewer' | 'user' | 'sending' | 'evaluation' | 'summary'
  text?: string
  questionIndex?: number
  evaluation?: EvaluationSummary | null
}

interface LocalInterviewPlan extends InterviewPlanContext {
  questionCount: number
  focusTags: string[]
  supplement: string
  summary: MultiSessionSummaryResponse | null
  summaryGeneratedAt?: string | null
  createdAt: string
}

interface RoundReviewSummary {
  sessionId: number
  personaName: string
  personaTitle: string
  order: number
  completed: boolean
  questionCount: number
  summary: ScoreSummary | null
}

const route = useRoute()
const router = useRouter()
const targetsStore = useTargetsStore()
const fromEditor = computed(() => route.query.from === 'editor')
const fromTarget = computed(() => route.query.from === 'target')
const fromWorkspace = computed(() => fromEditor.value || fromTarget.value)
const targetId = computed(() => positiveQueryId(route.query.targetId))
// 目标上下文：from=target 且能在 store 中定位到目标实体时，大厅以该目标的岗位/简历版本为准绑定
const boundTarget = computed<JobProject | null>(() => {
  if (!fromTarget.value || !targetId.value) return null
  return targetsStore.targets.find((item) => item.id === targetId.value) ?? null
})
const workspaceReturnLabel = computed(() => fromTarget.value ? '返回求职目标工作台' : '返回当前简历工作台')
const loadingOptions = ref(false)
const actionLoading = ref(false)
const activeReviewMode = ref(false)
const errorMessage = ref('')
const resumes = ref<Resume[]>([])
const versions = ref<ResumeVersion[]>([])
const jobs = ref<JobDescription[]>([])
const selectedCompanyProfile = ref<CompanyProfile | null>(null)
const selectedVersionId = ref<number | null>(null)
const selectedJobId = ref<number | null>(null)
const questionCount = ref(5)
const focusTagOptions = ['项目深挖', '技术基础', '系统设计', '岗位匹配', '表达结构', '职业动机']
const selectedFocusTags = ref<string[]>(['项目深挖', '岗位匹配'])
const interviewSupplement = ref('')
const personas = ref<InterviewerPersona[]>([])
const selectedPersonaId = ref<number | null>(null)
const selectedPersonaIds = ref<number[]>([])
const personaPanelExpanded = ref(false)
const extraPanelExpanded = ref(false)
const showPersonaDialog = ref(false)
const customPersonaForm = ref({ name: '', title: '', style: '' })

// 跨会话总结
const multiSummaryLoading = ref(false)
const deletedSessionIds = ref(new Set<number>())
const deletedSessionStorageKey = 'resumego:deletedInterviewSessionIds'

// 成长趋势
const showGrowthDialog = ref(false)
const growthLoading = ref(false)
const growthReport = ref<GrowthReport | null>(null)
const showPlanReviewDialog = ref(false)

// 当前简历 ID（由 selectedVersionId 推导）
const currentResumeId = computed(() => {
  const version = versions.value.find((v) => v.id === selectedVersionId.value)
  return version?.resumeId ?? null
})

// 维度名称
const dimNames = [
  { key: 'clarity', label: '表达清晰度', color: '#3b82f6' },
  { key: 'relevance', label: '岗位相关性', color: '#10b981' },
  { key: 'depth', label: '技术深度', color: '#f59e0b' },
  { key: 'accuracy', label: '回答准确性', color: '#8b5cf6' },
]

// 成长趋势快照（模板中类型收窄用）
const growthSnapshots = computed(() => growthReport.value?.snapshots ?? [])
const growthChanges = computed(() => growthReport.value?.changes ?? { clarity: 0, relevance: 0, depth: 0, accuracy: 0 })

// 历史会话筛选
const historyFilter = ref<'all' | 'completed' | 'inProgress'>('all')
const historyFilterTabs = computed(() => {
  const completed = visibleInterviewRecords.value.filter((record) => record.isCompleted).length
  const inProgress = visibleInterviewRecords.value.filter((record) => record.isInProgress).length
  return [
    { key: 'all' as const, label: '全部', count: visibleInterviewRecords.value.length },
    { key: 'completed' as const, label: '已完成', count: completed },
    { key: 'inProgress' as const, label: '进行中', count: inProgress },
  ]
})
const filteredInterviewRecords = computed(() => {
  if (historyFilter.value === 'completed') return visibleInterviewRecords.value.filter((record) => record.isCompleted)
  if (historyFilter.value === 'inProgress') return visibleInterviewRecords.value.filter((record) => record.isInProgress)
  return visibleInterviewRecords.value
})

// 多会话状态
const {
  sessions,
  activeSessionId,
  sessionStates,
  activeSession,
  activeState,
  getOrCreateSessionState,
  updateSession: updateSessionInList,
  upsertSession,
  removeSessionState,
} = useInterviewSessions()
const localInterviewPlans = ref<Record<number, LocalInterviewPlan>>({})
const persistedInterviewPlans = ref<InterviewPlanResponse[]>([])

const elapsedTime = ref(0)
const actionStage = ref('正在处理...')
let elapsedTimer: ReturnType<typeof setInterval> | null = null

const speechSupported = ref(false)
const isListening = ref(false)
let recognition: any = null
let recognitionSessionId: number | null = null

// ── 计算属性 ──

const visibleSessions = computed(() =>
  sessions.value.filter((session) => !deletedSessionIds.value.has(session.sessionId)),
)

const interviewRecords = computed<InterviewRecord[]>(() => buildInterviewRecords({
  sessions: visibleSessions.value,
  plansBySessionId: localInterviewPlans.value,
  jobs: jobs.value,
  deletedSessionIds: deletedSessionIds.value,
}))

const visibleInterviewRecords = computed(() => fromTarget.value
  ? filterTargetInterviewRecords(interviewRecords.value, selectedJobId.value, selectedVersionId.value)
  : interviewRecords.value)

const activeInterviewPlan = computed(() => {
  if (!activeSessionId.value) return null
  return localInterviewPlans.value[activeSessionId.value] ?? null
})
const activePlanStepLabel = computed(() => {
  if (!activeInterviewPlan.value) return ''
  const current = activeInterviewPlan.value.currentPersonaIndex + 1
  const total = Math.max(activeInterviewPlan.value.personaIds.length, 1)
  return `第 ${current} / ${total} 位面试官`
})
const nextPlannedPersona = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return null
  const nextPersonaId = plan.personaIds[plan.currentPersonaIndex + 1]
  if (!nextPersonaId) return null
  return personas.value.find((item) => item.id === nextPersonaId) ?? null
})
const activePlanSessions = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return []
  return sessions.value
    .filter((session) => localInterviewPlans.value[session.sessionId]?.planId === plan.planId)
    .sort(comparePlanSessionOrder)
})
const activePlanCompletedSessionIds = computed(() =>
  activePlanSessions.value
    .filter((session) => sessionCompleted(session))
    .map((session) => session.sessionId),
)
const activePlanCompletionLabel = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return ''
  return `已完成 ${activePlanCompletedSessionIds.value.length}/${plan.personaIds.length} 位`
})
const activePlanFinished = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return true
  return activePlanCompletedSessionIds.value.length >= plan.personaIds.length
})
const canSummarizeActivePlan = computed(() => activePlanFinished.value && activePlanCompletedSessionIds.value.length >= 2)
const canReturnToWorkspace = computed(() => {
  if (!activeSession.value) return true
  if (!activeInterviewPlan.value) return sessionCompleted(activeSession.value)
  return activePlanFinished.value
})
const completedQuestionSteps = computed(() => Array.from(
  { length: activeSession.value?.totalQuestions ?? 0 },
  (_, index) => index + 1,
).filter(isQuestionStepCompleted))

const activeSessionPersona = computed(() => {
  if (!activeSession.value?.personaName) return null
  return personas.value.find((p) => p.name === activeSession.value!.personaName) ?? null
})

const resumeOptions = computed(() =>
  versions.value.map((item) => ({
    value: item.id,
    label: `v${item.versionNo} · ${createdByLabel(item.createdByType)} · ${formatDate(item.createdAt)}`,
  })),
)

const jobOptions = computed(() =>
  jobs.value.map((item) => ({
    value: item.id,
    label: item.companyName ? `${item.jobTitle}｜${item.companyName}` : item.jobTitle,
  })),
)

const selectedPersonaQueue = computed(() =>
  selectedPersonaIds.value
    .map((id) => personas.value.find((item) => item.id === id))
    .filter((item): item is InterviewerPersona => Boolean(item)),
)
const selectedFocusSummary = computed(() => {
  const focus = selectedFocusTags.value.length ? selectedFocusTags.value.join('、') : '未选择重点方向'
  return interviewSupplement.value ? `${focus} · 已补充说明` : focus
})
const selectedResumeLabel = computed(
  () => resumeOptions.value.find((item) => item.value === selectedVersionId.value)?.label ?? '待选择',
)
const selectedJobLabel = computed(
  () => jobOptions.value.find((item) => item.value === selectedJobId.value)?.label ?? '待选择',
)
// Composer 锁定态与右侧摘要使用「公司 · 岗位 / 简历名 · Vn」展示，数据来自真实实体。
const selectedJobDisplayLabel = computed(() => {
  const job = selectedJobEntity.value
  if (!job) return selectedJobLabel.value
  return job.companyName ? `${job.companyName} · ${job.jobTitle}` : job.jobTitle
})
const selectedResumeDisplayLabel = computed(() => {
  const version = versions.value.find((item) => item.id === selectedVersionId.value)
  if (!version) return selectedResumeLabel.value
  const resume = resumes.value.find((item) => item.id === version.resumeId)
  return resume ? `${resume.title} · V${version.versionNo}` : selectedResumeLabel.value
})
const selectedJobEntity = computed(() => jobs.value.find((item) => item.id === selectedJobId.value) ?? null)
const companyProfileTags = computed(() => [
  ...(selectedCompanyProfile.value?.preferenceTags ?? []),
  ...(selectedCompanyProfile.value?.interviewFocus ?? []),
].filter(Boolean))
const hasCompanyProfile = computed(() => Boolean(selectedCompanyProfile.value?.companyName && companyProfileTags.value.length))
// 绑定是否完整锁定：目标驱动的会话必须与目标实体完全一致才算「已绑定」，
// 任何回退（找不到目标、缺 JD/简历、目标资源已删除）都如实进入未锁定态。
const workspaceContextLocked = computed(() => {
  if (!fromWorkspace.value) return false
  if (!selectedVersionId.value || !selectedJobId.value) return false
  if (fromTarget.value && boundTarget.value) {
    return selectedVersionId.value === boundTarget.value.resumeVersionId
      && selectedJobId.value === boundTarget.value.jobDescriptionId
  }
  return true
})
const startInterviewButtonLabel = computed(() => {
  if (selectedPersonaQueue.value.length > 1) return `开始第 1 位：${selectedPersonaQueue.value[0].name}`
  return '开始模拟面试'
})

const canCreateSession = computed(
  () => Boolean(selectedVersionId.value && selectedJobId.value && selectedPersonaIds.value.length > 0),
)
// 右侧 Inspector「本次练习」摘要：上下文齐备才渲染，缺一即不显示。
const currentPlanSummary = computed<CurrentPlanSummary | null>(() => {
  if (!selectedVersionId.value || !selectedJobId.value || !selectedPersonaIds.value.length) return null
  const first = selectedPersonaQueue.value[0]
  return {
    jobLabel: selectedJobDisplayLabel.value,
    resumeLabel: selectedResumeDisplayLabel.value,
    questionCount: questionCount.value,
    personaName: first?.name ?? '',
    personaTitle: first?.title ?? '',
  }
})
const currentQuestion = computed(() => activeSession.value?.currentQuestion ?? null)
const currentIndex = computed(() => Math.max(1, activeSession.value?.currentQuestionIndex ?? 1))
const isCompleted = computed(() => Boolean(activeSession.value?.completed))
const canSubmitAnswer = computed(
  () => !activeReviewMode.value && activeSession.value?.status === 'WAITING_ANSWER' && Boolean(currentQuestion.value),
)
const activePersonaStyle = computed(() =>
  activeSessionPersona.value?.style
    || activeSession.value?.personaTitle
    || '关注回答是否围绕岗位要求、项目证据和表达结构展开。',
)
const customPersonaFormValid = computed(
  () =>
    customPersonaForm.value.name.trim() &&
    customPersonaForm.value.title.trim() &&
    customPersonaForm.value.style.trim(),
)
const formatElapsedTime = computed(() => {
  const sec = elapsedTime.value
  if (sec < 60) return `已用 ${sec} 秒`
  const min = Math.floor(sec / 60)
  const s = sec % 60
  return `已用 ${min} 分 ${s} 秒`
})

const chatMessages = computed<ChatMessage[]>(() => {
  const msgs: ChatMessage[] = []
  const hist = activeState.value.history
  for (const h of hist) {
    msgs.push({ role: 'interviewer', text: h.questionText, questionIndex: h.questionIndex })
    // 跳过未回答的问题（answerText 为空），避免渲染空白用户消息
    // 未回答的问题由下方的 currentQuestion 逻辑单独处理
    if (h.answerText) {
      msgs.push({ role: 'user', text: h.answerText })
    }
    if (h.evaluation) {
      msgs.push({ role: 'evaluation', evaluation: h.evaluation })
    }
  }
  // 当前问题（还没回答的）
  if (currentQuestion.value && !hist.some((h) => h.questionIndex === currentIndex.value)) {
    msgs.push({ role: 'interviewer', text: currentQuestion.value.questionText, questionIndex: currentIndex.value })
  }
  // 发送中的消息
  if (activeState.value.pendingAnswer) {
    msgs.push({ role: 'sending', text: activeState.value.pendingAnswer })
  }
  // 总结
  if (isCompleted.value && activeSession.value?.summaryJson) {
    msgs.push({ role: 'summary' })
  }
  return msgs
})

const summaryData = computed<Record<string, unknown>>(() => {
  if (!activeSession.value?.summaryJson) return {}
  try {
    return JSON.parse(activeSession.value.summaryJson) as Record<string, unknown>
  } catch {
    return {}
  }
})
const summaryDescription = computed(() => {
  if (activeReviewMode.value) {
    return '正在复盘本轮面试对话与评价，你可以在左侧切换查看本次面试的其他轮次。'
  }
  if (!canReturnToWorkspace.value && nextPlannedPersona.value) {
    return `本轮面试已完成，建议继续进入下一位面试官「${nextPlannedPersona.value.name}」，最后再生成整次多轮总结。`
  }
  const score = summaryData.value.overallScore
  return typeof score === 'number'
    ? `综合表现评分 ${score}/100，建议回到简历优化页补齐证据与表达。`
    : '总结已生成，建议回到简历优化页继续迭代。'
})
const summaryStrengths = computed(() => parseTextList(summaryData.value.strengths, []))
const summarySuggestions = computed(() => parseTextList(summaryData.value.suggestions, []))
const activeRoundScoreSummary = computed(() => {
  return summarizeQuestionScores(activeState.value.perQuestionScores)
})
const activePlanReviewSummary = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return null
  const summaries = activePlanSessions.value
    .map((session, index): RoundReviewSummary => {
      const scores = scoresForSession(session.sessionId)
      return {
        sessionId: session.sessionId,
        personaName: session.personaName || plan.personaNames[index] || '面试官',
        personaTitle: session.personaTitle || '模拟面试官',
        order: index + 1,
        completed: sessionCompleted(session),
        questionCount: scores.length || session.totalQuestions || plan.questionCount,
        summary: summarizeQuestionScores(scores),
      }
    })
  const completedSummaries = summaries.filter((item) => item.completed && item.summary)
  const allScores = activePlanSessions.value.flatMap((session) => scoresForSession(session.sessionId))
  const overall = summarizeQuestionScores(allScores)
  const cachedSummary = findCachedPlanSummary(plan.planId)

  return {
    plan,
    rounds: summaries,
    completedRounds: completedSummaries.length,
    totalRounds: Math.max(plan.personaIds.length, summaries.length),
    overall,
    cachedSummary,
  }
})

// ── 辅助函数 ──

function scoresForSession(sessionId: number) {
  const stateScores = sessionStates.value[sessionId]?.perQuestionScores ?? []
  if (stateScores.length) return stateScores
  return sessions.value.find((session) => session.sessionId === sessionId)?.perQuestionScores ?? []
}

function sessionCompleted(s: InterviewStatusResponse) {
  return interviewRoundStatus(s) === 'completed'
}

function comparePlanSessionOrder(a: InterviewStatusResponse, b: InterviewStatusResponse) {
  const aIndex = localInterviewPlans.value[a.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
  const bIndex = localInterviewPlans.value[b.sessionId]?.currentPersonaIndex ?? Number.MAX_SAFE_INTEGER
  if (aIndex !== bIndex) return aIndex - bIndex
  return a.sessionId - b.sessionId
}

function isQuestionStepCompleted(step: number) {
  if (!activeSession.value) return false
  if (activeSession.value.completed && step <= activeSession.value.totalQuestions) return true
  return activeState.value.history.some((h) => h.questionIndex === step)
}

async function openInterviewRecord(record: InterviewRecord) {
  const orderedSessions = [...record.sessions].sort(comparePlanSessionOrder)
  if (record.isCompleted) {
    activeReviewMode.value = true
    const target = orderedSessions[0]
    if (!target) return
    await switchToSession(target.sessionId)
    await preloadRecordHistories(orderedSessions)
    return
  }
  activeReviewMode.value = false
  const target = orderedSessions.find((session) => !sessionCompleted(session)) ?? orderedSessions[0]
  if (target) await switchToSession(target.sessionId)
}

async function preloadRecordHistories(recordSessions: InterviewStatusResponse[]) {
  await Promise.allSettled(recordSessions.map(async (session) => {
    const statusRes = await getInterviewStatus(session.sessionId)
    updateSessionInList(session.sessionId, {
      status: statusRes.data.status,
      currentQuestionIndex: statusRes.data.currentQuestionIndex,
      totalQuestions: statusRes.data.totalQuestions,
      currentQuestion: statusRes.data.currentQuestion ?? null,
      completed: statusRes.data.completed,
      summaryJson: statusRes.data.summaryJson,
      perQuestionScores: statusRes.data.perQuestionScores ?? [],
    })
    const state = getOrCreateSessionState(session.sessionId)
    state.perQuestionScores = statusRes.data.perQuestionScores ?? []
    await refreshSessionHistory(session.sessionId)
  }))
}

async function deleteInterviewRecord(record: InterviewRecord) {
  try {
    await ElMessageBox.confirm(
      `将从历史列表移除「${record.title}」及其 ${record.sessions.length} 轮记录。面试问答数据会保留，计划记录将被标记为隐藏。`,
      '删除面试记录',
      {
        confirmButtonText: '删除记录',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  const planId = Number(record.id)
  if (Number.isFinite(planId)) {
    try {
      await deleteInterviewPlan(planId)
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '删除面试计划失败')
      return
    }
  }

  const nextDeleted = new Set(deletedSessionIds.value)
  const deleteIds = record.sessions.map((session) => session.sessionId)
  deleteIds.forEach((sessionId) => {
    nextDeleted.add(sessionId)
    delete localInterviewPlans.value[sessionId]
    removeSessionState(sessionId)
  })
  deletedSessionIds.value = nextDeleted
  persistDeletedSessionIds()
  persistedInterviewPlans.value = persistedInterviewPlans.value.filter((plan) => String(plan.planId) !== record.id)
  sessions.value = sessions.value.filter((session) => !nextDeleted.has(session.sessionId))

  if (activeSessionId.value && nextDeleted.has(activeSessionId.value)) {
    activeSessionId.value = null
  }
  ElMessage.success('已从当前历史列表移除')
}

function loadDeletedSessionIds() {
  try {
    const raw = window.localStorage.getItem(deletedSessionStorageKey)
    const values = raw ? JSON.parse(raw) : []
    deletedSessionIds.value = new Set(Array.isArray(values) ? values.map(Number).filter(Number.isFinite) : [])
  } catch {
    deletedSessionIds.value = new Set()
  }
}

function persistDeletedSessionIds() {
  window.localStorage.setItem(deletedSessionStorageKey, JSON.stringify([...deletedSessionIds.value]))
}

function toggleFocusTag(tag: string) {
  if (selectedFocusTags.value.includes(tag)) {
    selectedFocusTags.value = selectedFocusTags.value.filter((item) => item !== tag)
    return
  }
  selectedFocusTags.value = [...selectedFocusTags.value, tag]
}

function syncPrimaryPersona() {
  selectedPersonaId.value = selectedPersonaIds.value[0] ?? null
}

function personaPlanIndex(personaId: number) {
  const index = selectedPersonaIds.value.indexOf(personaId)
  return index >= 0 ? index + 1 : 0
}

function togglePersonaPlan(personaId: number) {
  if (selectedPersonaIds.value.includes(personaId)) {
    removePersonaFromPlan(personaId)
    return
  }
  if (selectedPersonaIds.value.length >= 5) {
    ElMessage.warning('每次面试最多选择 5 位面试官')
    return
  }
  selectedPersonaIds.value = [...selectedPersonaIds.value, personaId]
  syncPrimaryPersona()
}

function removePersonaFromPlan(personaId: number) {
  selectedPersonaIds.value = selectedPersonaIds.value.filter((id) => id !== personaId)
  syncPrimaryPersona()
}

function movePersonaInPlan(personaId: number, direction: -1 | 1) {
  const currentIndex = selectedPersonaIds.value.indexOf(personaId)
  const nextIndex = currentIndex + direction
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= selectedPersonaIds.value.length) return
  const next = [...selectedPersonaIds.value]
  const [item] = next.splice(currentIndex, 1)
  next.splice(nextIndex, 0, item)
  selectedPersonaIds.value = next
  syncPrimaryPersona()
}

function buildStatusFromPlanRound(round: InterviewPlanRound): InterviewStatusResponse {
  return {
    sessionId: round.sessionId,
    status: round.status,
    currentQuestionIndex: round.currentQuestionIndex,
    totalQuestions: round.totalQuestions,
    currentQuestion: null,
    summaryJson: null,
    completed: round.completed,
    perQuestionScores: null,
    personaName: round.personaName,
    personaTitle: round.personaTitle,
  }
}

function resolvePlanResumeLabel(plan: InterviewPlanResponse) {
  return resumeOptions.value.find((item) => item.value === plan.resumeVersionId)?.label
    ?? `简历版本 #${plan.resumeVersionId}`
}

function resolvePlanJobLabel(plan: InterviewPlanResponse) {
  return jobOptions.value.find((item) => item.value === plan.jobDescriptionId)?.label
    ?? plan.title
    ?? `岗位 #${plan.jobDescriptionId}`
}

function applyBackendPlans(plans: InterviewPlanResponse[]) {
  const nextPlansById = new Map(persistedInterviewPlans.value.map((plan) => [plan.planId, plan]))
  for (const plan of plans) {
    nextPlansById.set(plan.planId, plan)
    const orderedRounds = [...plan.rounds].sort((a, b) => a.roundOrder - b.roundOrder)
    const personaIds = orderedRounds.map((round) => round.personaId)
    const personaNames = orderedRounds.map((round) => round.personaName)
    orderedRounds.forEach((round, index) => {
      if (deletedSessionIds.value.has(round.sessionId)) return
      localInterviewPlans.value[round.sessionId] = {
        planId: String(plan.planId),
        sessionId: round.sessionId,
        resumeVersionId: plan.resumeVersionId,
        resumeLabel: resolvePlanResumeLabel(plan),
        jobDescriptionId: plan.jobDescriptionId,
        jobLabel: resolvePlanJobLabel(plan),
        personaIds,
        personaNames,
        currentPersonaIndex: index,
        questionCount: plan.questionCount,
        focusTags: plan.focusTags ?? [],
        supplement: plan.supplement ?? '',
        summary: plan.summary ?? null,
        summaryGeneratedAt: plan.summaryGeneratedAt ?? null,
        createdAt: plan.createdAt ?? new Date().toISOString(),
      }
      upsertSession(buildStatusFromPlanRound(round))
    })
  }
  persistedInterviewPlans.value = [...nextPlansById.values()]
}

watch(
  () => selectedJobEntity.value?.companyName,
  (companyName) => {
    void loadSelectedCompanyProfile(companyName)
  },
  { immediate: true },
)


// ── 三模式训练入口 ──
const threeModeOpen = ref(false)
const threeModePlan = ref<InterviewPlanResponse | null>(null)
function onThreeModeStarted(plan: InterviewPlanResponse) {
  threeModePlan.value = plan
  threeModeOpen.value = false
}

onMounted(() => {
  loadDeletedSessionIds()
  loadOptions()
  initSpeechRecognition()
})

// ── 语音识别 ──

function initSpeechRecognition() {
  const SpeechRecognitionConstructor =
    (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognitionConstructor) {
    speechSupported.value = false
    return
  }
  speechSupported.value = true
  recognition = new SpeechRecognitionConstructor()
  recognition.continuous = false
  recognition.interimResults = true
  recognition.lang = 'zh-CN'
  recognition.onresult = (event: any) => {
    let transcript = ''
    for (let i = 0; i < event.results.length; i++) transcript += event.results[i][0].transcript
    if (transcript && recognitionSessionId != null) {
      const state = getOrCreateSessionState(recognitionSessionId)
      state.answerDraft = state.answerDraft.trim()
        ? state.answerDraft + ' ' + transcript.trim()
        : transcript.trim()
    }
  }
  recognition.onend = () => {
    isListening.value = false
  }
  recognition.onerror = (event: any) => {
    console.error('语音识别错误:', event.error)
    ElMessage.error(`语音识别失败：${event.error}`)
    isListening.value = false
  }
}

function toggleVoiceInput() {
  if (!recognition) return
  if (isListening.value) {
    recognition.stop()
    isListening.value = false
  } else {
    try {
      recognitionSessionId = activeSessionId.value
      recognition.start()
      isListening.value = true
    } catch (e) {
      console.error('启动语音识别失败:', e)
      ElMessage.error('启动语音识别失败')
    }
  }
}

onUnmounted(() => {
  stopElapsedTimer()
})

function handleEnterKey(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  if (canSubmitAnswer.value && activeState.value.answerDraft.trim() && !actionLoading.value) {
    handleSubmitAnswer()
  }
}

// ── 数据加载 ──

async function loadSelectedCompanyProfile(companyName?: string | null) {
  selectedCompanyProfile.value = null
  if (!companyName) return
  try {
    const response = await resolveCompanyProfile(companyName)
    selectedCompanyProfile.value = response.data?.companyName ? response.data : null
  } catch {
    selectedCompanyProfile.value = null
  }
}

async function loadOptions() {
  loadingOptions.value = true
  errorMessage.value = ''
  try {
    const [resumeRes, jobRes, personaRes, sessionsRes, plansRes] = await Promise.all([
      listResumes(),
      listJobDescriptions(),
      listInterviewerPersonas(),
      listMyInterviews(),
      listMyInterviewPlans(),
    ])
    resumes.value = resumeRes.data
    jobs.value = jobRes.data
    personas.value = personaRes.data
    if (personas.value.length > 0) {
      selectedPersonaIds.value = [personas.value[0].id]
      syncPrimaryPersona()
    }
    sessions.value = sessionsRes.data.filter((session) => !deletedSessionIds.value.has(session.sessionId))

    const queryVersionId = Number(route.query.versionId)
    const queryJobId = Number(route.query.jobId)
    const storedJobId = getWorkspaceSelectedJobId()
    const storedResumeId = getWorkspaceSelectedResumeId()
    if (fromTarget.value && targetId.value && !targetsStore.targets.length) {
      try {
        await targetsStore.load()
      } catch {
        /* store 加载失败则走自由选择 */
      }
    }
    const targetEntity = fromTarget.value && targetId.value
      ? targetsStore.targets.find((item) => item.id === targetId.value) ?? null
      : null

    if (resumes.value[0]?.id || targetEntity?.resumeVersionId) {
      // 目标驱动绑定：目标实体的岗位/简历版本是唯一权威来源；目标缺 JD/简历或资源已删除时
      // 进入自由选择（如实标注未锁定），绝不静默绑定「列表第一项」造成与主界面不一致。
      let targetResumeId: number | null = null
      if (targetEntity?.resumeVersionId) {
        try {
          const targetVersionRes = await getResumeVersion(targetEntity.resumeVersionId)
          targetResumeId = targetVersionRes.data.resumeId
        } catch {
          targetResumeId = null
        }
      }
      if (targetResumeId != null) {
        const versionRes = await getResumeVersions(targetResumeId)
        versions.value = versionRes.data
        selectedVersionId.value = versions.value.find((item) => item.id === targetEntity!.resumeVersionId)?.id
          ?? versions.value[0]?.id
          ?? null
      } else if (resumes.value[0]?.id) {
        let freeResumeId = storedResumeId ?? resumes.value[0].id
        if (fromTarget.value && queryVersionId > 0) {
          try {
            const queryVersionRes = await getResumeVersion(queryVersionId)
            freeResumeId = queryVersionRes.data.resumeId
          } catch {
            /* 版本读取失败则保持默认简历 */
          }
        }
        const versionRes = await getResumeVersions(freeResumeId)
        versions.value = versionRes.data
        selectedVersionId.value = versions.value.find((item) => item.id === queryVersionId)?.id
          ?? resumes.value[0]?.currentVersion?.id
          ?? versions.value[0]?.id
          ?? null
      }

      const targetJobId = targetEntity?.jobDescriptionId ?? null
      if (targetJobId != null) {
        selectedJobId.value = jobs.value.find((item) => item.id === targetJobId)?.id ?? null
      } else {
        selectedJobId.value = jobs.value.find((item) => item.id === queryJobId)?.id
          ?? jobs.value.find((item) => item.id === storedJobId)?.id
          ?? jobs.value.find((item) => item.parseStatus === 'succeeded')?.id
          ?? jobs.value[0]?.id
          ?? null
      }
    }
    applyBackendPlans(plansRes.data)
    if (selectedJobId.value) {
      setWorkspaceSelectedJobId(selectedJobId.value)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载面试上下文失败'
  } finally {
    loadingOptions.value = false
  }
}

// 同一路由换 query（如从工作台切换到另一个目标的「开始模拟面试」）时重新绑定上下文
watch(
  () => `${route.query.from ?? ''}|${route.query.targetId ?? ''}|${route.query.versionId ?? ''}|${route.query.jobId ?? ''}`,
  (next, prev) => {
    if (next !== prev) void loadOptions()
  },
)

// ── 会话切换 ──

async function switchToSession(sessionId: number) {
  const alreadyActive = activeSessionId.value === sessionId

  activeSessionId.value = sessionId
  const state = getOrCreateSessionState(sessionId)

  // 如果该会话还没有加载过历史，从后端加载
  if (!alreadyActive || state.history.length === 0) {
    try {
      // 刷新会话状态
      const statusRes = await getInterviewStatus(sessionId)
      // 如果等待期间用户切换到了其他会话，放弃本次更新
      if (activeSessionId.value !== sessionId) return
      updateSessionInList(sessionId, {
        status: statusRes.data.status,
        currentQuestionIndex: statusRes.data.currentQuestionIndex,
        totalQuestions: statusRes.data.totalQuestions,
        currentQuestion: statusRes.data.currentQuestion ?? null,
        completed: statusRes.data.completed,
        summaryJson: statusRes.data.summaryJson,
      })
      if (statusRes.data.perQuestionScores) {
        state.perQuestionScores = statusRes.data.perQuestionScores
      }

      await refreshSessionHistory(sessionId)
    } catch (e) {
      console.error('加载会话历史失败:', e)
    }
  }
}

async function refreshSessionHistory(sessionId: number) {
  const state = getOrCreateSessionState(sessionId)
  const historyRes = await getSessionHistory(sessionId)
  state.history = historyRes.data.items
}

// ── 单次面试计划总结 ──

async function openPlanReviewDialog() {
  const summary = activePlanReviewSummary.value
  if (!summary) return
  if (summary.cachedSummary) {
    showPlanReviewDialog.value = true
    return
  }
  if (!canSummarizeActivePlan.value) {
    ElMessage.warning('完成本次多轮面试后才能生成整次复盘。')
    return
  }
  await handleGenerateActivePlanSummary(true)
}

async function handleGenerateActivePlanSummary(openAfterGenerate = false) {
  const plan = activeInterviewPlan.value
  if (plan?.planId) {
    await handleGeneratePlanSummary(plan.planId, openAfterGenerate)
    return
  }
  ElMessage.warning('旧版面试记录缺少计划信息，无法生成整次复盘。')
}

async function handleGeneratePlanSummary(planId: string, openAfterGenerate = false) {
  const numericPlanId = Number(planId)
  if (!Number.isFinite(numericPlanId)) {
    ElMessage.warning('旧版面试记录缺少计划信息，无法生成整次总结。')
    return
  }

  const cachedSummary = findCachedPlanSummary(planId)
  if (cachedSummary) {
    if (openAfterGenerate) showPlanReviewDialog.value = true
    else ElMessage.info('整次复盘已生成，可点击复盘入口查看。')
    return
  }

  multiSummaryLoading.value = true
  errorMessage.value = ''

  try {
    const res = await generateInterviewPlanSummary(numericPlanId)
    cachePlanSummary(planId, res.data)
    ElMessage.success('整次复盘已生成')
    if (openAfterGenerate) showPlanReviewDialog.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成整次总结失败'
  } finally {
    multiSummaryLoading.value = false
  }
}

function findCachedPlanSummary(planId: string) {
  const plan = Object.values(localInterviewPlans.value).find((item) => item.planId === planId)
  return plan?.summary ?? null
}

function cachePlanSummary(planId: string, summary: MultiSessionSummaryResponse) {
  const generatedAt = new Date().toISOString()
  Object.keys(localInterviewPlans.value).forEach((sessionId) => {
    const plan = localInterviewPlans.value[Number(sessionId)]
    if (plan?.planId === planId) {
      plan.summary = summary
      plan.summaryGeneratedAt = generatedAt
    }
  })
  persistedInterviewPlans.value = persistedInterviewPlans.value.map((plan) =>
    String(plan.planId) === planId
      ? { ...plan, summary, summaryGeneratedAt: generatedAt }
      : plan,
  )
}

// ── 成长趋势 ──

async function loadGrowthData() {
  if (!selectedVersionId.value || !selectedJobId.value || !currentResumeId.value) {
    ElMessage.warning('请先选择简历和岗位')
    return
  }

  growthLoading.value = true
  growthReport.value = null
  errorMessage.value = ''

  try {
    const res = await getInterviewGrowthReport(currentResumeId.value, selectedJobId.value)
    if (!res.data.snapshots.length) {
      ElMessage.info('当前岗位暂无成长快照，请完成一次多轮面试并生成整次复盘后再查看')
      return
    }
    growthReport.value = res.data

    showGrowthDialog.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载成长数据失败'
  } finally {
    growthLoading.value = false
  }
}

// ── 返回角色卡选择 ──

function backToPersona() {
  activeSessionId.value = null
  activeReviewMode.value = false
}

// ── 创建面试官 ──

async function handleCreatePersona() {
  if (!customPersonaFormValid.value) return
  try {
    const res = await createInterviewerPersona({
      name: customPersonaForm.value.name.trim(),
      title: customPersonaForm.value.title.trim(),
      style: customPersonaForm.value.style.trim(),
    })
    personas.value.push(res.data)
    selectedPersonaIds.value = [...selectedPersonaIds.value, res.data.id]
    syncPrimaryPersona()
    showPersonaDialog.value = false
    customPersonaForm.value = { name: '', title: '', style: '' }
    ElMessage.success('面试官创建成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

// ── 删除自定义面试官 ──

async function handleDeletePersona(persona: InterviewerPersona) {
  try {
    await ElMessageBox.confirm(
      `确定要删除自定义角色「${persona.name}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteInterviewerPersona(persona.id)
    personas.value = personas.value.filter((p) => p.id !== persona.id)
    selectedPersonaIds.value = selectedPersonaIds.value.filter((id) => id !== persona.id)
    if (selectedPersonaId.value === persona.id) {
      selectedPersonaId.value = selectedPersonaIds.value[0] ?? null
    }
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ── 创建并开始面试 ──

/** 新契约要求 jobProjectId：从当前目标反查绑定该 JD 的求职目标（不伪造） */
const selectedProjectIdForPlan = computed(() => {
  const jobId = selectedJobId.value
  const matched = targetsStore.targets.find(
    (target) => target.status === 'active' && target.jobDescriptionId != null && target.jobDescriptionId === jobId,
  )
  return matched?.id ?? null
})

async function handleCreateAndStart() {
  if (!selectedVersionId.value || !selectedJobId.value || !selectedPersonaId.value) return
  if (selectedProjectIdForPlan.value == null) {
    errorMessage.value = '该岗位尚未关联求职目标，请先在求职计划中创建并录入 JD'
    return
  }
  activeReviewMode.value = false
  setWorkspaceSelectedJobId(selectedJobId.value)
  actionLoading.value = true
  actionStage.value = '正在创建多轮面试计划...'
  startElapsedTimer()
  errorMessage.value = ''

  try {
    const planRes = await createInterviewPlan({
      mode: 'ROLE_BASED',
      jobProjectId: selectedProjectIdForPlan.value,
      resumeVersionId: selectedVersionId.value,
      questionCount: questionCount.value,
      personaIds: [...selectedPersonaIds.value],
      focusTags: [...selectedFocusTags.value],
      supplement: interviewSupplement.value.trim(),
    })
    applyBackendPlans([planRes.data])
    const firstRound = [...planRes.data.rounds].sort((a, b) => a.roundOrder - b.roundOrder)[0]
    if (!firstRound) {
      throw new Error('面试计划缺少轮次')
    }
    actionStage.value = '正在启动第一轮面试...'
    const startRes = await startInterview(firstRound.sessionId)

    upsertSession(startRes.data)
    activeSessionId.value = startRes.data.sessionId
    getOrCreateSessionState(startRes.data.sessionId)

    ElMessage.success('面试已开始')
  } catch (error) {
    const message = error instanceof Error ? error.message : '启动面试失败'
    errorMessage.value = message
    if (message.includes('AI 模型服务')) {
      window.dispatchEvent(new CustomEvent('resumego:ai-not-configured'))
    }
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

async function startNextPlannedPersona() {
  activeReviewMode.value = false
  const plan = activeInterviewPlan.value
  const nextPersona = nextPlannedPersona.value
  if (!plan || !nextPersona) return
  const nextIndex = plan.currentPersonaIndex + 1
  const nextSession = activePlanSessions.value.find((session) =>
    localInterviewPlans.value[session.sessionId]?.currentPersonaIndex === nextIndex,
  )
  if (!nextSession) {
    errorMessage.value = '未找到下一轮面试会话，请刷新后重试'
    return
  }

  actionLoading.value = true
  actionStage.value = `正在启动下一位面试官：${nextPersona.name}...`
  startElapsedTimer()
  errorMessage.value = ''
  setWorkspaceSelectedJobId(plan.jobDescriptionId)

  try {
    const startRes = await startInterview(nextSession.sessionId)
    upsertSession(startRes.data)
    activeSessionId.value = startRes.data.sessionId
    getOrCreateSessionState(startRes.data.sessionId)

    ElMessage.success(`已进入下一位面试官：${nextPersona.name}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '启动下一位面试官失败'
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 提交回答 ──

async function handleSubmitAnswer() {
  if (!activeSession.value || !activeState.value.answerDraft.trim()) return
  const currentAnswer = activeState.value.answerDraft.trim()
  const sessionId = activeSessionId.value!
  const state = getOrCreateSessionState(sessionId)

  state.answerDraft = ''

  // 先显示用户消息
  state.pendingAnswer = currentAnswer
  actionLoading.value = true
  errorMessage.value = ''
  state.retryable = false
  startElapsedTimer()

  const currentQ = activeSession.value.currentQuestion
  state.lastSubmitAnswer = currentAnswer

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: currentAnswer })

    // 清除发送中状态
    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = 'AI 评价暂时不可用，请稍后重试'
      return
    }

    state.history.push({
      questionIndex: res.data.currentQuestionIndex > 0 ? res.data.currentQuestionIndex - 1 : currentQ?.questionIndex ?? 0,
      questionText: currentQ?.questionText ?? '',
      questionType: currentQ?.questionType ?? '',
      answerText: currentAnswer,
      evaluation: res.data.evaluation ?? null,
    })

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    if (res.data.completed) {
      const status = await getInterviewStatus(sessionId)
      updateSessionInList(sessionId, {
        status: status.data.status,
        currentQuestionIndex: status.data.currentQuestionIndex,
        totalQuestions: status.data.totalQuestions,
        currentQuestion: status.data.currentQuestion ?? null,
        completed: status.data.completed,
        summaryJson: status.data.summaryJson,
      })
      state.perQuestionScores = status.data.perQuestionScores ?? []
      await refreshSessionHistory(sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    state.pendingAnswer = ''
    errorMessage.value = error instanceof Error ? error.message : '提交回答失败'
    state.retryable = true
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 重试评价 ──

async function retrySubmitAnswer() {
  if (!activeSession.value || !activeState.value.lastSubmitAnswer) return
  const sessionId = activeSessionId.value!
  const state = getOrCreateSessionState(sessionId)

  state.pendingAnswer = state.lastSubmitAnswer
  actionLoading.value = true
  errorMessage.value = ''
  state.retryable = false
  startElapsedTimer()

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: state.lastSubmitAnswer })

    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = 'AI 评价仍然不可用，请稍后再试'
      return
    }

    if (state.history.length > 0) {
      state.history[state.history.length - 1].evaluation = res.data.evaluation ?? null
    }

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    if (res.data.completed) {
      const status = await getInterviewStatus(sessionId)
      updateSessionInList(sessionId, {
        status: status.data.status,
        currentQuestionIndex: status.data.currentQuestionIndex,
        totalQuestions: status.data.totalQuestions,
        currentQuestion: status.data.currentQuestion ?? null,
        completed: status.data.completed,
        summaryJson: status.data.summaryJson,
      })
      state.perQuestionScores = status.data.perQuestionScores ?? []
      await refreshSessionHistory(sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    state.pendingAnswer = ''
    errorMessage.value = error instanceof Error ? error.message : '重试失败'
    state.retryable = true
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

// ── 导航 ──

function goToOptimization() {
  if (!canReturnToWorkspace.value) {
    ElMessage.info('请先完成本次多轮面试，再回到简历优化。')
    return
  }
  if (fromWorkspace.value) {
    returnToEditor()
    return
  }
  if (selectedVersionId.value && selectedJobId.value) {
    setWorkspaceSelectedJobId(selectedJobId.value)
    router.push(buildResumeEditorLocation({ versionId: selectedVersionId.value }))
    return
  }
  router.push({ name: 'workbench' })
}

function returnToEditor() {
  if (!canReturnToWorkspace.value) {
    ElMessage.info('请先完成本次多轮面试，再回到简历工作台。')
    return
  }
  if (fromTarget.value) {
    router.push({ name: 'workbench', query: targetId.value ? { targetId: String(targetId.value) } : {} })
    return
  }
  markReturnToEditor()
  router.push(buildResumeEditorLocation({ versionId: selectedVersionId.value }))
}

// ── 工具函数 ──

function positiveQueryId(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

function parseTextList(value: unknown, fallback: string[]) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (typeof value !== 'string' || !value.trim()) return fallback
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean)
    if (typeof parsed === 'string') return [parsed]
    if (parsed && typeof parsed === 'object')
      return Object.values(parsed).map(String).filter(Boolean)
  } catch {
    return [value]
  }
  return fallback
}

function startElapsedTimer() {
  elapsedTime.value = 0
  stopElapsedTimer()
  elapsedTimer = setInterval(() => {
    elapsedTime.value++
  }, 1000)
}

function stopElapsedTimer() {
  if (elapsedTimer !== null) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

function createdByLabel(type: string) {
  if (type === 'ai_suggestion') return 'AI 建议生成'
  if (type === 'import') return '初始导入'
  if (type === 'user') return '手动创建'
  return type
}

function formatDate(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 10)
}
</script>

<style scoped>
.interview-command-bar{display:flex;align-items:center;gap:18px;min-height:58px;padding:8px 4px 14px;border-bottom:1px solid var(--border-subtle);margin-bottom:16px}
.bar-identity{display:flex;align-items:baseline;gap:14px}
.bar-title{display:inline-flex;align-items:center;gap:10px;margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.bar-title-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;background:var(--brand-soft);color:var(--brand)}
.bar-subtitle{color:var(--muted);font-size:12.5px}
.lobby-three-mode{margin-bottom:20px}
.three-mode-head{display:flex;justify-content:flex-end;margin-bottom:8px}

.interview-page {
  box-sizing: border-box;
  min-height: 100vh;
  background: var(--canvas);
  color: var(--ink);
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  padding: 18px;
}

/* 返回栏 */
.workspace-return-bar {
  display: flex; align-items: center; gap: 14px; margin: 0 0 12px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-panel);
  background: var(--bg-surface); padding: 8px 12px;
}
.workspace-return-bar button {
  border: 0; border-radius: var(--radius-control); background: var(--brand); color: #fff;
  cursor: pointer; font-weight: 600; padding: 8px 12px;
}
.workspace-return-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.workspace-return-bar span { color: var(--muted, #64748b); font-size: 13px; }

/* 面试大厅 */
.lobby-growth-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  gap: 7px;
  margin: 22px 0 0 auto;
  min-height: 36px;
  padding: 8px 15px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--bg-surface);
  color: var(--copy);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.lobby-growth-button:hover:not(:disabled) {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--accent-soft);
}
.lobby-growth-button:disabled { cursor: not-allowed; opacity: 0.48; }
.lobby-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
}
.lobby-side {
  position: sticky;
  top: 14px;
  display: grid;
  max-height: calc(100vh - 28px);
  gap: 12px;
  overflow: auto;
  padding-right: 2px;
}
.lobby-side::-webkit-scrollbar { width: 4px; }
.lobby-side::-webkit-scrollbar-thumb { border-radius: 999px; background: var(--surface, #dbe3ef); }
.lobby-create-card { scroll-margin-top: 18px; }
/* Composer 分节：针对 / 面试形式 / 面试官 / 本轮重点 */
.composer-section{padding:2px 0 4px}
.composer-section + .composer-section{border-top:1px solid var(--border-subtle)}
.composer-section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:10px 0 6px}
.composer-section-head h3{margin:0;color:var(--ink);font-size:14px;font-weight:650;letter-spacing:-.01em}
.composer-section-head p{margin:4px 0 0;color:var(--muted);font-size:12px}
.target-locked-rows{display:grid;padding:6px 0 2px}
.target-locked-row{display:flex;align-items:baseline;gap:12px;padding:9px 2px}
.target-locked-row + .target-locked-row{border-top:1px solid var(--border-subtle)}
.target-locked-label{flex:0 0 44px;color:var(--muted);font-size:13px;font-weight:600}
.target-locked-row strong{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--ink);font-size:14px;font-weight:600}
/* Composer 字段行：label + 控件/值 + 更换链接 */
.composer-fields{display:grid}
.composer-field{display:flex;align-items:center;gap:12px;min-width:0;padding:12px 0;border:0;border-bottom:1px solid var(--border-subtle);background:transparent;border-radius:0}
.composer-field-label{flex:0 0 44px;color:var(--muted);font-size:13px;font-weight:600}
.composer-field :deep(.el-select){flex:1;min-width:0}
.composer-field :deep(.el-select__wrapper){min-height:38px;border-radius:var(--radius-control);background:var(--bg-surface);box-shadow:0 0 0 1px var(--border-default) inset}
.composer-field :deep(.el-select__wrapper.is-focused){box-shadow:0 0 0 1px var(--brand) inset,0 0 0 4px var(--accent-soft)}
.composer-field-value{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--ink);font-size:14px;font-weight:600}
.composer-field-change{display:inline-flex;align-items:center;gap:4px;flex:0 0 auto;padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500;cursor:pointer}
.composer-field-change:hover{color:var(--ink)}
.composer-field.question-count-slider{margin-top:0}
.composer-field.question-count-slider>.composer-field-label{flex:0 0 44px}
.persona-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}
.persona-section-head p { margin: 4px 0 0; color: var(--muted, #94a3b8); font-size: 12px; }
.section-head-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.soft-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 5px 11px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--bg-surface);
  color: var(--copy);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.16s ease;
  white-space: nowrap;
}
.soft-toggle-btn:hover {
  border-color: var(--brand);
  background: var(--accent-soft);
  color: var(--brand);
}
.add-persona-btn.compact { margin-top: 0; white-space: nowrap; }
.extra-collapsed-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  min-height: 20px;
  align-items: center;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}
.persona-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
/* 面试官队列行（Composer）：头像 + 名字/角色 + 顺序调整/移除 */
.persona-queue{display:flex;flex-wrap:wrap;gap:10px;margin-top:2px}
.persona-queue-item{display:flex;align-items:center;gap:10px;min-width:0;padding:7px 10px 7px 7px;border:1px solid var(--border-subtle);border-radius:var(--radius-control);background:var(--bg-surface)}
.persona-queue-item .persona-avatar{width:34px;height:34px;font-size:14px;border-radius:10px}
.persona-queue-copy{display:grid;gap:1px;min-width:0}
.persona-queue-copy strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--ink);font-size:13px;font-weight:600}
.persona-queue-copy span{color:var(--muted);font-size:11px;white-space:nowrap}
.persona-queue-style{overflow:hidden;max-width:230px;text-overflow:ellipsis;color:var(--brand)!important;font-weight:600}
.persona-queue-actions{display:flex;gap:2px;margin-left:6px}
.persona-queue-actions button{display:grid;place-items:center;width:22px;height:22px;border:0;border-radius:6px;background:transparent;color:var(--muted);cursor:pointer}
.persona-queue-actions button:hover:not(:disabled){background:var(--bg-hover);color:var(--ink)}
.persona-queue-actions button:disabled{cursor:not-allowed;opacity:.35}
.persona-queue-empty{margin:0;color:var(--muted);font-size:13px;line-height:1.6}
.lobby-context-extra {
  display: grid;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}
.lobby-context-extra.collapsed {
  gap: 8px;
}
.context-extra-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.lobby-context-extra h3 {
  margin: 0;
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}
.lobby-context-extra p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.focus-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.focus-chip {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--bg-surface);
  color: var(--muted);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 11px;
}
.focus-chip:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--accent-soft);
}
.focus-chip.active {
  border-color: var(--brand);
  background: var(--brand);
  color: #fff;
}
.supplement-field {
  display: grid;
  gap: 6px;
}
.supplement-field > span {
  color: var(--copy);
  font-size: 13px;
  font-weight: 600;
}
.start-hint { color: var(--muted); font-size: 12px; }
.recent-interview-card {
  padding: 16px 0;
  border-top: 1px solid var(--border-subtle);
  background: transparent;
}
.recent-card-head span,
.recent-card-head strong {
  display: block;
}
.recent-card-head span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.recent-card-head strong {
  margin-top: 4px;
  color: var(--ink);
  font-size: 18px;
  font-weight: 650;
}
.recent-record-list {
  display: grid;
  gap: 0;
  margin-top: 10px;
}
.recent-record {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 11px 6px;
  border: 0;
  border-radius: 0;
  background: transparent;
  cursor: pointer;
}
.recent-record + .recent-record {
  border-top: 1px solid var(--border-subtle);
}
.recent-record:hover {
  background: var(--bg-hover);
  border-radius: var(--radius-control);
}
.recent-record strong,
.recent-record span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-record strong { color: var(--ink); font-size: 13px; font-weight: 600; }
.recent-record span { margin-top: 3px; color: var(--muted); font-size: 11px; font-weight: 600; }
.recent-record button {
  border: 0;
  border-radius: var(--radius-control);
  background: var(--bg-hover);
  color: var(--copy);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
}
.recent-empty {
  margin: 12px 0 0;
  color: var(--muted, #64748b);
  font-size: 13px;
  line-height: 1.6;
}

/* 提示 */
.interview-preview-note { margin-bottom: 16px; border-radius: 12px; }

/* 上下文卡片 */
.interview-context-card {
  background: transparent; border-radius: 0; padding: 4px 2px;
  box-shadow: none; border: 0;
}
.context-heading { display: flex; gap: 14px; margin-bottom: 16px; align-items: flex-start; }
.context-step {
  width: 32px; height: 32px; border-radius: var(--radius-control);
  background: var(--bg-hover); color: var(--copy);
  display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 14px; flex-shrink: 0;
}
.context-heading h2 { font-size: 18px; font-weight: 650; color: var(--ink); margin: 0 0 2px; }
.context-heading p { font-size: 13px; color: var(--muted); margin: 0; }
/* 面试官：队列行 + 展开拾取 */
.persona-section { margin-top: 20px; }
.persona-section h3 { font-size: 14px; font-weight: 600; color: var(--ink); margin: 0; }
.persona-cards {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  max-height: 360px;
  gap: 10px;
  overflow: auto;
  padding-right: 2px;
}
.persona-cards::-webkit-scrollbar { width: 4px; }
.persona-cards::-webkit-scrollbar-thumb { border-radius: 999px; background: var(--surface, #dbe3ef); }
.persona-card {
  min-height: 132px; padding: 12px; border-radius: var(--radius-panel); border: 1px solid var(--border-subtle);
  background: var(--bg-surface); cursor: pointer; transition: all 0.2s; position: relative;
}
.persona-card:hover { border-color: var(--border-strong); }
.persona-card.selected { border-color: var(--brand); background: var(--bg-selected); box-shadow: none; }
.persona-card.primary { box-shadow: inset 0 0 0 1px var(--brand); }
.persona-avatar {
  width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: 600; font-size: 16px; color: var(--copy); margin-bottom: 8px;
  background: var(--bg-hover);
}
.persona-card.selected .persona-avatar { background: var(--brand); color: #fff; }

.persona-info { display: flex; flex-direction: column; gap: 2px; }
.persona-name { font-weight: 600; font-size: 14px; color: var(--ink); }
.persona-title { font-size: 12px; color: var(--copy); }
.persona-style { font-size: 11px; color: var(--muted); line-height: 1.4; margin-top: 4px; }
.persona-check { color: var(--brand); font-size: 18px; }
.persona-delete-btn {
  display: grid; place-items: center;
  width: 22px; height: 22px; border: 0; border-radius: 8px;
  background: rgba(239, 68, 68, 0.08); color: var(--danger, #ef4444); cursor: pointer;
  font-size: 14px; transition: all 0.15s; flex-shrink: 0;
}
.persona-delete-btn:hover { background: rgba(239, 68, 68, 0.18); }
.persona-order-badge {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}
.add-persona-btn {
  margin-top: 10px; display: inline-flex; align-items: center; gap: 4px; border: 1px solid var(--border-default);
  border-radius: var(--radius-control); background: var(--bg-surface); color: var(--muted); font-size: 12px; font-weight: 600; padding: 6px 12px; cursor: pointer;
}
.add-persona-btn:hover { border-color: var(--brand); color: var(--brand); background: var(--accent-soft); }
.context-start-row { margin-top: 16px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.interview-start-button {
  display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 42px; padding: 10px 22px; border: 0; border-radius: var(--radius-control);
  background: var(--brand); color: #fff; font-weight: 600; font-size: 15px; cursor: pointer;
  transition: all 0.18s ease;
}
.interview-start-button:disabled { opacity: 0.5; cursor: not-allowed; }
.interview-start-button:hover:not(:disabled) { background: var(--accent-hover); }
/* 历史会话 */
.history-sessions-section {
  margin-top: 14px;
  padding: 0;
  border: 0;
  border-top: 1px solid var(--border-subtle);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}
.side-history-section {
  margin-top: 0;
  padding: 16px 0;
  border-radius: 0;
}
.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.history-head h2 { margin: 0 0 4px; color: var(--ink); font-size: 22px; font-weight: 650; letter-spacing: -0.03em; }
.history-head p { margin: 0; color: var(--muted); font-size: 13px; }
.history-filter-tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.filter-tab {
  padding: 6px 14px; border: 0; border-radius: var(--radius-control); background: transparent;
  font-size: 12px; font-weight: 600; color: var(--muted); cursor: pointer; transition: all 0.15s;
}
.filter-tab:hover { background: var(--bg-hover); color: var(--brand); }
.filter-tab.active { background: var(--accent-soft); color: var(--brand); }
.history-empty-card {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 180px;
  border: 1px dashed var(--line, #cbd5e1);
  border-radius: 22px;
  color: var(--muted, #64748b);
  text-align: center;
  background: var(--surface, #f8fafc);
}
.history-empty-card .el-icon { font-size: 30px; color: var(--muted, #94a3b8); }
.history-empty-card strong { color: var(--ink, #0f172a); font-size: 16px; }
.history-empty-card span { max-width: 360px; font-size: 13px; line-height: 1.6; }
.history-record-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(282px, 1fr)); gap: 12px; margin-top: 12px; }
.history-record-card {
  display: flex; flex-direction: column; align-items: stretch; gap: 12px; padding: 14px; border-radius: 18px;
  border: 1px solid var(--line, #e5eaf2); background: var(--surface-solid, #fff); cursor: pointer; transition: all 0.2s;
}
.history-record-card:hover { border-color: var(--brand, #cfeee2); box-shadow: 0 14px 28px rgba(16, 185, 129, 0.1); transform: translateY(-1px); }
.side-history-section .history-head {
  gap: 10px;
  margin-bottom: 12px;
}
.side-history-section .history-head h2 {
  font-size: 18px;
}
.side-history-section .history-head p {
  font-size: 12px;
  line-height: 1.5;
}
.side-history-section .history-filter-tabs {
  overflow-x: auto;
  padding-bottom: 2px;
}
.side-history-section .filter-tab {
  flex-shrink: 0;
  padding: 6px 10px;
}
.side-history-section .history-record-grid {
  grid-template-columns: 1fr;
  gap: 10px;
  max-height: 46vh;
  overflow: auto;
  padding-right: 2px;
}
.side-history-section .history-record-grid::-webkit-scrollbar { width: 4px; }
.side-history-section .history-record-grid::-webkit-scrollbar-thumb { border-radius: 999px; background: var(--surface, #dbe3ef); }
.side-history-section .history-record-card {
  gap: 8px;
  padding: 10px;
  border-radius: 14px;
  min-width: 0;
  overflow: hidden;
}
.side-history-section .history-empty-card {
  min-height: 132px;
  border-radius: 18px;
}
.side-history-section .history-empty-card .el-icon {
  font-size: 24px;
}
.side-history-section .history-empty-card strong {
  font-size: 14px;
}
.side-history-section .record-round-list {
  max-height: 60px;
  overflow: auto;
}
.side-history-section .record-round-list span {
  font-size: 10px;
  padding: 3px 6px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 12px;
}
.side-history-section .hsc-company-avatar {
  flex-shrink: 0;
  transform: scale(0.82);
  transform-origin: left center;
}
.side-history-section .hsc-name {
  font-size: 13px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-title {
  font-size: 11px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-history-section .hsc-status {
  font-size: 10px;
  padding: 3px 7px;
}
.side-history-section .hsc-open-button {
  min-height: 30px;
  font-size: 12px;
  border-radius: 10px;
  padding: 6px 10px;
}
.side-history-section .hsc-delete-button {
  width: 30px;
  height: 30px;
  border-radius: 10px;
}
.side-history-section .hsc-progress-line {
  font-size: 11px;
  gap: 6px;
}
.side-history-section .hsc-progress-line strong {
  font-size: 12px;
}
.side-history-section .hsc-main {
  gap: 8px;
  min-width: 0;
}
.side-history-section .hsc-info {
  min-width: 0;
  flex: 1;
}
.side-history-section .history-card-top {
  gap: 6px;
}
.side-history-section .record-actions {
  gap: 6px;
}
.history-card-top,
.hsc-main,
.hsc-progress-line { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.hsc-avatar {
  width: 42px; height: 42px; border-radius: var(--radius-panel);
  background: var(--bg-hover); color: var(--copy);
  display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 14px; flex-shrink: 0;
}
.hsc-company-avatar {
  flex-shrink: 0;
}
.hsc-info { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.hsc-name { font-weight: 700; font-size: 14px; color: var(--copy, #1f2937); }
.hsc-title { font-size: 12px; color: var(--copy, #6b7280); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hsc-status {
  font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: var(--radius-control);
  background: var(--bg-hover); color: var(--muted); flex-shrink: 0;
}
.hsc-status.completed { background: var(--accent-soft); color: var(--brand); }
.hsc-status.failed { background: var(--danger-soft); color: var(--danger); }
.hsc-status.cancelled { background: var(--warning-soft); color: var(--warning); }
.hsc-progress-line span { color: var(--muted, #64748b); font-size: 12px; }
.hsc-progress-line strong { color: var(--ink, #0f172a); font-size: 13px; }
.record-round-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.record-round-list span {
  max-width: 100%;
  overflow: hidden;
  padding: 5px 8px;
  border-radius: var(--radius-control);
  background: var(--bg-hover);
  border: 1px solid var(--border-subtle);
  color: var(--muted);
  font-size: 11px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.record-round-list span.completed {
  background: var(--accent-soft);
  border-color: transparent;
  color: var(--brand);
}
.record-round-list span.failed {
  background: var(--danger-soft);
  border-color: transparent;
  color: var(--danger);
}
.record-round-list span.cancelled {
  background: var(--warning-soft);
  border-color: transparent;
  color: var(--warning);
}
.record-actions {
  display: flex;
  gap: 8px;
}
.hsc-open-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  border: 0;
  border-radius: var(--radius-control);
  background: var(--bg-hover);
  color: var(--ink);
  font-weight: 600;
  cursor: pointer;
  flex: 1;
}
.hsc-open-button:hover { background: var(--accent-soft); color: var(--brand); }
.hsc-delete-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: var(--radius-control);
  background: var(--danger-soft);
  color: var(--danger);
  cursor: pointer;
}
.hsc-delete-button:hover { background: var(--danger-soft); opacity: .8; }

/* 聊天布局 */
.interview-chat-layout {
  display: flex; gap: 0; border-radius: 24px; overflow: hidden;
  border: 1px solid var(--line, #e5eaf2);
  box-shadow: 0 18px 54px rgba(15, 23, 42, 0.08); height: calc(100vh - 36px); min-height: 560px;
  background: var(--surface-solid, #fff);
}
.workspace-return-bar + .interview-chat-layout {
  height: calc(100vh - 96px);
}

/* 当前面试侧栏 */
.chat-sidebar {
  width: 188px; flex-shrink: 0; padding: 12px; background: var(--surface-solid, #fff);
  border-right: 1px solid var(--line, #e5eaf2); display: flex; flex-direction: column; align-items: stretch; gap: 10px;
  overflow-y: auto;
}
.sidebar-back-btn {
  display: flex; align-items: center; justify-content: center; width: 32px; height: 32px;
  border: 0; border-radius: 999px; background: transparent; cursor: pointer; color: var(--copy, #334155);
  transition: all 0.2s; flex-shrink: 0;
}
.sidebar-back-btn:hover:not(:disabled) { color: var(--brand, #047857); background: var(--surface, #f1f5f9); }
.sidebar-back-btn:disabled { cursor: not-allowed; color: var(--muted, #cbd5e1); background: var(--surface, #f8fafc); }
.sidebar-persona-card,
.sidebar-question-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--line, #e5eaf2);
  border-radius: 18px;
  background: var(--surface, #f8fafc);
}
.sidebar-persona-avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  color: #fff;
  font-weight: 900;
}
.sidebar-persona-card span,
.sidebar-question-card span {
  color: var(--muted, #94a3b8);
  font-size: 11px;
  font-weight: 900;
}
.sidebar-persona-card strong,
.sidebar-question-card strong {
  color: var(--ink, #101a33);
  font-size: 15px;
  font-weight: 900;
}
.sidebar-persona-card small,
.sidebar-question-card small {
  color: var(--muted, #64748b);
  font-size: 12px;
  font-weight: 800;
}
.sidebar-persona-card p {
  margin: 2px 0 0;
  color: var(--muted, #64748b);
  font-size: 12px;
  line-height: 1.55;
}

/* 题目进度点 */
.sidebar-dots { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.sidebar-dot {
  width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--border-default); font-size: 11px; font-weight: 700; color: var(--muted); background: var(--bg-surface);
}
.sidebar-dot.completed { border-color: var(--brand); color: #fff; background: var(--brand); }
.sidebar-dot.active { border-color: var(--brand); color: var(--brand); background: var(--bg-surface); }
.sidebar-dot.viewing { border-color: var(--warning); color: var(--warning); background: var(--warning-soft); }
.chat-sidebar > small { font-size: 11px; color: var(--muted); text-align: center; }
.sidebar-completed { color: var(--brand) !important; font-weight: 600; }
.sidebar-round-card {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--line, #e5eaf2);
  border-radius: 18px;
  background: var(--surface-solid, #fff);
}
.sidebar-round-card > span {
  color: var(--muted, #94a3b8);
  font-size: 11px;
  font-weight: 900;
}
.round-switch-button {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  grid-template-areas:
    "num name"
    "num status";
  gap: 0 8px;
  align-items: center;
  width: 100%;
  padding: 8px;
  border: 1px solid var(--line, #e5eaf2);
  border-radius: 14px;
  background: var(--surface, #f8fafc);
  color: var(--copy, #334155);
  text-align: left;
  cursor: pointer;
}
.round-switch-button:hover:not(:disabled) { border-color: var(--border-default); background: var(--bg-surface); }
.round-switch-button.active { border-color: var(--brand); background: var(--bg-selected); }
.round-switch-button.completed i { background: var(--brand); color: #fff; }
.round-switch-button.failed i { background: var(--danger); color: #fff; }
.round-switch-button.cancelled i { background: var(--warning); color: #fff; }
.round-switch-button i {
  grid-area: num;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--surface, #e2e8f0);
  color: var(--muted, #64748b);
  font-style: normal;
  font-size: 11px;
  font-weight: 900;
}
.round-switch-button strong {
  grid-area: name;
  min-width: 0;
  overflow: hidden;
  color: var(--ink, #101a33);
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.round-switch-button small {
  grid-area: status;
  color: var(--muted, #94a3b8);
  font-size: 10px;
  font-weight: 800;
}

/* 聊天主区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.chat-plan-header {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line, #e5eaf2);
  background: var(--surface, rgba(255, 255, 255, 0.94));
}
.chat-plan-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 140px;
}
.chat-plan-status strong {
  display: block;
  color: var(--ink, #0f172a);
  font-size: 13px;
  font-weight: 900;
}
.chat-plan-status span {
  max-width: 220px;
  overflow: hidden;
  color: var(--muted, #64748b);
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-kicker {
  display: block;
  margin-bottom: 2px;
  color: var(--brand, #059669);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.chat-plan-header strong {
  display: block;
  overflow: hidden;
  color: var(--ink, #0f172a);
  font-size: 15px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-header p {
  margin: 3px 0 0;
  color: var(--muted, #64748b);
  font-size: 12px;
}
.chat-plan-tags {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}
.chat-plan-tags span {
  border-radius: 999px;
  background: var(--brand-soft, #ecfdf5);
  color: var(--brand, #059669);
  font-size: 11px;
  font-weight: 800;
  padding: 4px 8px;
}
.chat-company-focus {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 12px;
  background: var(--surface, #f8fafc);
  padding: 7px 10px;
}
.chat-company-focus span {
  flex: 0 0 auto;
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
}
.chat-company-focus strong {
  min-width: 0;
  overflow: hidden;
  color: var(--copy, #334155);
  font-size: 12px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-plan-steps {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.chat-plan-steps span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  padding: 5px 9px 5px 5px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 999px;
  background: var(--surface-solid, #fff);
  color: var(--muted, #64748b);
  font-size: 11px;
  font-weight: 800;
}
.chat-plan-steps i {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: var(--surface, #f1f5f9);
  color: var(--muted, #64748b);
  font-style: normal;
  font-size: 10px;
}
.chat-plan-steps span.done {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
}
.chat-plan-steps span.done i {
  background: var(--brand);
  color: #fff;
}
.chat-plan-steps span.current {
  border-color: var(--brand);
  background: var(--brand);
  color: #fff;
}
.chat-plan-steps span.current i {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.chat-plan-note {
  grid-column: 1 / -1;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--surface-solid, #fff);
  border: 1px solid var(--line, #e2e8f0);
}
.plan-review-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin: 8px 18px 0;
  padding: 8px 10px 8px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: var(--surface, rgba(255, 255, 255, 0.82));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.035);
  backdrop-filter: blur(10px);
}
.plan-review-head {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}
.plan-review-head strong {
  display: inline-block;
  margin: 0;
  color: var(--ink, #0f172a);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}
.plan-review-head p {
  margin: 6px 0 0;
  color: var(--muted, #64748b);
  font-size: 12px;
  line-height: 1.5;
}
.plan-review-score {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e2e8f0);
  color: var(--ink, #0f172a);
}
.plan-review-score span {
  font-size: 15px;
  font-weight: 950;
  letter-spacing: -0.04em;
}
.plan-review-score small {
  margin-left: 2px;
  color: var(--muted, #64748b);
  font-size: 11px;
  font-weight: 800;
}
.plan-review-insight {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 6px;
  min-width: 0;
  padding: 0;
  border-radius: 0;
  background: var(--surface, #f8fafc);
  border: 0;
}
.plan-review-insight span {
  display: inline-flex;
  align-items: center;
  max-width: 128px;
  padding: 4px 8px;
  overflow: hidden;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 999px;
  background: var(--surface, #f8fafc);
  color: var(--copy, #475569);
  font-size: 11px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-review-insight p {
  flex-basis: 100%;
  margin: 0;
  color: var(--muted, #64748b);
  font-size: 12px;
}
.round-review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-top: 12px;
}
.round-review-card {
  padding: 12px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 16px;
  background: var(--surface, rgba(255, 255, 255, 0.88));
}
.round-review-card.completed {
  border-color: var(--brand);
}
.round-review-card > span {
  color: var(--brand);
  font-size: 11px;
  font-weight: 900;
}
.round-review-card > strong {
  display: block;
  margin-top: 4px;
  color: var(--ink, #0f172a);
  font-size: 14px;
  font-weight: 900;
}
.round-review-card > small {
  display: block;
  margin-top: 2px;
  color: var(--muted, #64748b);
  font-size: 11px;
}
.round-review-score {
  margin-top: 10px;
}
.round-review-score b {
  color: var(--ink, #0f172a);
  font-size: 22px;
  letter-spacing: -0.04em;
}
.round-review-score em {
  color: var(--muted, #94a3b8);
  font-size: 12px;
  font-style: normal;
}
.round-review-score p,
.round-review-pending {
  margin: 4px 0 0;
  color: var(--muted, #64748b);
  font-size: 11px;
}
.plan-review-ai-summary {
  margin-top: 12px;
  padding: 13px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 16px;
  background: var(--surface-solid, #fff);
}
.plan-review-ai-summary h4 {
  margin: 0 0 6px;
  color: var(--ink, #0f172a);
  font-size: 14px;
}
.plan-review-ai-summary p {
  margin: 0;
  color: var(--copy, #475569);
  font-size: 13px;
  line-height: 1.7;
}
.plan-review-list {
  margin-top: 10px;
}
.plan-review-list span {
  color: var(--ink, #0f172a);
  font-size: 12px;
  font-weight: 900;
}
.plan-review-list ul {
  margin: 6px 0 0;
  padding-left: 18px;
  color: var(--copy, #475569);
  font-size: 12px;
  line-height: 1.6;
}
.plan-review-generate {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--ink, #0f172a);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 6px 10px;
}
.plan-review-generate:hover:not(:disabled) { background: var(--surface, #f1f5f9); }
.plan-review-generate:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}
.plan-review-dialog :deep(.el-dialog) {
  border-radius: 24px;
}
.plan-review-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 68vh;
  overflow-y: auto;
  padding-right: 4px;
}
.plan-review-dialog-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: var(--radius-panel);
  background: var(--bg-surface);
}
.plan-review-dialog-hero h3 {
  margin: 4px 0 6px;
  color: var(--ink, #0f172a);
  font-size: 20px;
  font-weight: 950;
}
.plan-review-dialog-hero p {
  margin: 0;
  color: var(--muted, #64748b);
  font-size: 13px;
}
.plan-review-dialog-score {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: 12px 16px;
  border-radius: var(--radius-control);
  background: var(--brand);
  color: #fff;
}
.plan-review-dialog-score strong {
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.05em;
}
.plan-review-dialog-score span {
  margin-left: 3px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 14px;
}
.plan-review-dialog-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.plan-review-dialog-metric {
  padding: 13px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 16px;
  background: var(--surface-solid, #fff);
}
.plan-review-dialog-metric span {
  display: block;
  color: var(--muted, #64748b);
  font-size: 12px;
  font-weight: 800;
}
.plan-review-dialog-metric strong {
  display: block;
  margin: 6px 0 8px;
  color: var(--ink, #0f172a);
  font-size: 22px;
  letter-spacing: -0.04em;
}
.plan-review-dialog-metric div {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface, #edf2f7);
}
.plan-review-dialog-metric i {
  display: block;
  height: 100%;
  border-radius: inherit;
}
.dialog-round-review-grid {
  margin-top: 0;
}
.plan-review-empty-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px;
  border: 1px dashed var(--line, #cbd5e1);
  border-radius: 18px;
  color: var(--muted, #64748b);
  background: var(--surface, #f8fafc);
}
.plan-review-empty-summary .el-icon {
  color: var(--brand, #10b981);
  font-size: 24px;
}
.plan-review-empty-summary strong {
  color: var(--ink, #0f172a);
  font-size: 15px;
}
.plan-review-empty-summary span {
  font-size: 12px;
}

/* 加载条（设置界面用） */
.interview-loading-bar {
  display: flex; align-items: center; gap: 10px; padding: 10px 16px; margin-bottom: 14px;
  border-radius: 16px; background: var(--brand-soft, #ecfdf5); border: 1px solid var(--brand); color: var(--brand, #047857); font-size: 14px; font-weight: 700;
}
.interview-loading-bar .elapsed-time { margin-left: auto; color: var(--copy, #6b7280); font-weight: 400; font-size: 13px; }

/* 题目数量拖拽条 */
.question-count-slider {
  margin-top: 12px;
}
.question-slider-shell {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(160px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 2px 2px 2px 4px;
}
.question-slider-shell :deep(.el-slider) {
  --el-slider-main-bg-color: var(--brand);
  --el-slider-runway-bg-color: var(--border-default);
  --el-slider-stop-bg-color: rgba(255, 255, 255, 0.96);
}
.question-slider-shell :deep(.el-slider__runway) {
  height: 8px;
  border-radius: 999px;
}
.question-slider-shell :deep(.el-slider__bar) {
  height: 8px;
  border-radius: 999px;
  background: var(--brand);
}
.question-slider-shell :deep(.el-slider__button) {
  width: 18px;
  height: 18px;
  border: 4px solid #fff;
  background: var(--brand);
  box-shadow: 0 7px 18px var(--brand-soft);
}
.slider-limit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  color: var(--muted, #94a3b8);
  font-size: 12px;
  font-weight: 900;
}
.slider-value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  min-height: 32px;
  border-radius: var(--radius-control);
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  text-align: center;
}

@media (max-width: 1180px) {
  .interview-lobby-hero,
  .lobby-shell {
    grid-template-columns: 1fr;
  }
  .lobby-side {
    position: static;
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
  .side-history-section .history-record-grid {
    max-height: none;
  }
  .lobby-hero-panel {
    max-width: 520px;
  }
}

@media (max-width: 760px) {
  .interview-lobby-hero {
    padding: 22px;
    border-radius: 22px;
  }
  .lobby-hero-copy h1 {
    font-size: 34px;
  }
  .composer-field {
    flex-wrap: wrap;
  }
  .composer-field-change {
    margin-left: 56px;
  }
  .persona-cards,
  .history-record-grid {
    grid-template-columns: 1fr;
  }
  .history-head,
  .context-start-row {
    align-items: stretch;
    flex-direction: column;
  }
  .interview-start-button,
  .lobby-growth-button {
    width: 100%;
  }
  .question-count-slider {
    min-width: 0;
  }
  .chat-plan-header {
    grid-template-columns: 1fr;
  }
  .chat-plan-tags {
    justify-content: flex-start;
  }
}
</style>
