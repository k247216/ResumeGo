<template>
  <div class="interview-page" :data-engine-state="engineState">
    <header class="interview-command-bar" :class="{ 'interview-command-bar-home': engineState === 'home' }" data-test="interview-command-bar">
      <div v-if="showEngineIdentity" class="bar-identity">
        <span class="engine-breadcrumb">{{ engineState === 'setup' ? '开始练习' : '面试' }}</span>
        <span class="engine-breadcrumb-separator">/</span>
        <h1 class="bar-title hero-title">{{ engineState === 'setup' ? setupModeLabel : engineStateLabel }}</h1>
        <span v-if="engineState !== 'setup'" class="bar-subtitle">{{ engineStateDescription }}</span>
      </div>
      <button v-if="engineState !== 'home' && (!activeSessionId || engineState === 'room')" class="engine-back-button" type="button" @click="returnToInterviewHome">
        <el-icon><ArrowLeft /></el-icon>
        返回面试主页
      </button>
      <div v-if="engineState === 'room' && activeSession" class="room-command-actions">
        <button type="button" class="room-command-button" @click="toggleRoomPause">
          <el-icon><VideoPause v-if="!roomPaused" /><VideoPlay v-else /></el-icon>
          {{ roomPaused ? '继续' : '暂停' }}
        </button>
        <button type="button" class="room-command-button room-command-end" @click="leaveRoom">
          <el-icon><RemoveFilled /></el-icon>
          结束练习
        </button>
      </div>
    </header>
    <div v-if="fromWorkspace" class="workspace-return-bar">
      <button type="button" :disabled="!canReturnToWorkspace" @click="returnToEditor">← {{ workspaceReturnLabel }}</button>
      <span>{{ canReturnToWorkspace ? '模拟面试将作为当前简历的改进输入' : '请先完成本次多轮面试，再回到简历工作台' }}</span>
    </div>

    <!-- ========== 面试主页 / 练习准备 ========== -->
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

      <template v-if="engineState === 'home'">
        <section class="engine-home-shell" data-test="interview-home">
          <div class="engine-home-topline">
            <div class="engine-home-identity">
              <div class="engine-home-date-line">
                <span class="engine-eyebrow">{{ todayHeader }}</span>
                <span class="engine-home-motto">把每一次准备，都变成下一次更从容的回答。</span>
              </div>
              <span class="engine-home-focus">
                <span>当前专注：</span>
                <span class="engine-focus-target">
                  <span class="engine-focus-target-mark" aria-hidden="true">
                    <img v-if="latestFocusMark.icon" :src="latestFocusMark.icon" alt="" />
                    <span v-else :style="{ background: latestFocusMark.color, color: latestFocusMark.lightText ? '#fff' : '#171717' }">{{ latestFocusMark.letter }}</span>
                  </span>
                  <strong>{{ latestFocusTargetLabel }}</strong>
                  <small>{{ latestFocusStageLabel }}</small>
                </span>
              </span>
            </div>
            <div class="engine-home-utilities">
              <button type="button" @click="router.push('/schedule')"><el-icon><Calendar /></el-icon>日历</button>
              <div class="engine-export-history-control">
                <button type="button" @click.stop="showExportHistory = !showExportHistory; showExportMenu = false"><el-icon><Download /></el-icon>导出记录</button>
                <div v-if="showExportHistory" class="engine-export-history-menu" role="dialog" aria-label="导出记录">
                  <span>最近导出</span>
                  <div v-if="exportHistory.length" class="engine-export-history-list">
                    <div v-for="entry in exportHistory" :key="entry.id" class="engine-export-history-item">
                      <strong>{{ entry.fileName }}</strong>
                      <small>{{ entry.format === 'md' ? 'Markdown 复盘' : '纯文本回答' }} · {{ entry.recordCount }} 条记录 · {{ formatExportHistoryTime(entry.createdAt) }}</small>
                    </div>
                  </div>
                  <p v-else>还没有导出过回答文件。</p>
                </div>
              </div>
              <div class="engine-export-control">
                <button type="button" :disabled="!visibleInterviewRecords.length" @click.stop="showExportMenu = !showExportMenu; showExportHistory = false"><el-icon><Document /></el-icon>导出</button>
                <div v-if="showExportMenu" class="engine-export-menu" role="menu">
                  <span>选择回答文件格式</span>
                  <button type="button" role="menuitem" @click="exportInterviewRecords('md')">
                    <strong>Markdown 复盘（.md）</strong>
                    <small>保留问题、回答和反馈层级</small>
                  </button>
                  <button type="button" role="menuitem" @click="exportInterviewRecords('txt')">
                    <strong>纯文本回答（.txt）</strong>
                    <small>便于复制到其他工具继续整理</small>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="engine-home-layout">
            <main class="engine-timeline-panel">
              <div class="engine-panel-head engine-timeline-head">
                <div><span class="engine-section-label">Interview log</span><h2>近期面试记录</h2></div>
                <button v-if="visibleInterviewRecords.length" type="button" class="engine-view-all" @click="showAllInterviewRecords = !showAllInterviewRecords">
                  {{ showAllInterviewRecords ? '收起记录' : '查看全部记录' }} <el-icon><ArrowRight /></el-icon>
                </button>
              </div>
              <div v-if="!loadingOptions && displayedInterviewRecords.length" class="engine-timeline-list" :data-expanded="showAllInterviewRecords ? 'true' : undefined">
                <div
                  v-for="record in displayedInterviewRecords"
                  :key="record.id"
                  class="engine-timeline-row"
                  role="button"
                  tabindex="0"
                  @click="openInterviewRecord(record)"
                  @keydown.enter="openInterviewRecord(record)"
                >
                  <span class="engine-timeline-marker" :class="`mode-${modeForRecord(record) ?? 'ROLE_BASED'}`">
                    <el-icon><component :is="modeIconForRecord(record)" /></el-icon>
                  </span>
                  <span class="engine-timeline-kind">
                    <strong :class="`mode-${modeForRecord(record) ?? 'ROLE_BASED'}`">{{ modeLabelForRecord(record) }}</strong>
                    <small>{{ recordModeContext(record) }}</small>
                  </span>
                  <span class="engine-timeline-copy">
                    <strong class="engine-timeline-title">{{ record.title }}</strong>
                    <small class="engine-timeline-subtitle">{{ record.subtitle }}</small>
                    <span v-if="recordBindingLabel(record)" class="engine-record-binding">{{ recordBindingLabel(record) }}</span>
                  </span>
                  <span class="engine-timeline-facts"><strong>{{ record.dateLabel || '最近' }}</strong><small>{{ recordTimeLabel(record) }}</small></span>
                  <span class="engine-timeline-review"><small>复盘进度</small><strong>{{ reviewProgressLabel(record) }}</strong></span>
                  <span class="engine-timeline-issue"><small>核心问题</small><strong>{{ recordIssue(record) }}</strong></span>
                  <span class="engine-timeline-action"><el-icon><ArrowRight /></el-icon></span>
                  <button
                    type="button"
                    class="engine-timeline-delete"
                    :aria-label="`删除${record.title}`"
                    @click.stop="deleteInterviewRecord(record, $event)"
                  ><el-icon><Delete /></el-icon></button>
                </div>
              </div>
              <div v-else-if="!loadingOptions" class="engine-timeline-empty">
                <span class="engine-empty-symbol">＋</span>
                <strong>还没有练习记录</strong>
                <p>开始一次练习后，问题、来源和复盘会按时间保存在这里。</p>
                <button class="engine-primary-action" type="button" @click="openPracticeSetup('ROLE_BASED')">开始第一次练习 <el-icon><ArrowRight /></el-icon></button>
              </div>

              <section class="engine-practice-strip">
                <div class="engine-practice-strip-head"><div><span class="engine-section-label">Start a session</span><h3>开始练习</h3></div><span>选择后再配置岗位、资料或题集</span></div>
                <div class="engine-practice-links">
                  <button v-for="entry in practiceEntries" :key="entry.mode" type="button" class="engine-practice-link" :data-test="`practice-${entry.mode}`" @click="openPracticeSetup(entry.mode)">
                    <span class="engine-practice-icon"><el-icon><component :is="entry.icon" /></el-icon></span>
                    <span><strong>{{ entry.label }}</strong><small>{{ entry.description }}</small></span>
                    <el-icon class="engine-practice-arrow"><ArrowRight /></el-icon>
                  </button>
                </div>
              </section>
            </main>

            <aside class="engine-insight-rail">
              <section class="engine-insight-section" data-test="growth-summary">
                <div class="engine-panel-head engine-growth-head">
                  <div><h3>能力变化</h3><small>近 5 次练习</small></div>
                  <div class="engine-growth-switch" role="tablist" aria-label="能力变化图表">
                    <button type="button" :class="{ active: growthChartView === 'line' }" @click="growthChartView = 'line'">趋势</button>
                    <button type="button" :class="{ active: growthChartView === 'radar' }" @click="growthChartView = 'radar'">五维</button>
                  </div>
                </div>
                <div class="engine-trend-widget">
                  <template v-if="growthChartView === 'line'">
                    <div class="engine-trend-tabs" role="tablist" aria-label="复盘维度">
                      <button v-for="metric in abilityMetricDefs" :key="metric.key" type="button" :class="{ active: selectedAbilityMetric === metric.key }" @click="selectedAbilityMetric = metric.key">{{ metric.label }}</button>
                    </div>
                    <p v-if="!hasAbilityTrend" class="engine-trend-empty-note">完成带评分的面试后，这里才显示真实趋势。</p>
                    <div class="engine-trend-chart" aria-label="真实面试评分趋势">
                      <div class="engine-trend-y-labels"><span>优秀</span><span>良好</span><span>一般</span><span>待提升</span></div>
                      <svg viewBox="0 0 360 176" role="img" aria-label="能力趋势折线图" preserveAspectRatio="none">
                        <line v-for="y in [16, 61, 106, 151]" :key="y" x1="42" :y1="y" x2="354" :y2="y" class="engine-trend-grid-line" />
                        <polyline v-if="abilityTrendPolyline" :points="abilityTrendPolyline" class="engine-trend-line" />
                        <template v-for="point in abilityTrendPoints" :key="point?.index ?? 'empty'">
                          <circle v-if="point" :cx="point.x" :cy="point.y" r="5" class="engine-trend-point" />
                        </template>
                      </svg>
                    </div>
                    <div class="engine-trend-scale"><span v-for="(label, index) in abilityTrendLabels" :key="`${label}-${index}`">{{ label }}</span></div>
                  </template>
                  <template v-else>
                    <p v-if="!hasRadarTrend" class="engine-radar-empty-note">完成多维评分后，这里显示能力分布。</p>
                    <p v-else-if="radarMissingLabels.length" class="engine-radar-partial-note">{{ radarMissingLabels.join('、') }}暂无评分，已记录维度仍正常展示。</p>
                    <div class="engine-radar-chart" aria-label="五维能力变化">
                      <svg viewBox="0 0 300 236" role="img" aria-label="五维能力雷达图">
                        <polygon v-for="scale in [1, .75, .5, .25]" :key="scale" :points="radarGridPoints(scale)" class="engine-radar-grid" />
                        <line v-for="axis in radarAxes" :key="axis.key" x1="150" y1="118" :x2="axisPoint(axis, 1).x" :y2="axisPoint(axis, 1).y" class="engine-radar-axis" />
                        <polygon v-if="radarPolygon" :points="radarPolygon" class="engine-radar-fill" />
                        <circle v-for="point in radarPoints" :key="point.key" :cx="point.x" :cy="point.y" r="4" class="engine-radar-point" />
                        <text v-for="axis in radarAxes" :key="axis.key + '-label'" :x="axisLabelPoint(axis).x" :y="axisLabelPoint(axis).y" :text-anchor="axisLabelPoint(axis).anchor" class="engine-radar-label">{{ axis.label }}</text>
                      </svg>
                    </div>
                  </template>
                </div>
              </section>

              <section class="engine-insight-section engine-note-section">
                <div class="engine-note"><span class="engine-note-kicker">Needs attention</span><h3>需要关注的点</h3><p v-if="attentionItems.length">{{ attentionItems[0] }}</p><p v-else>完成一次复盘后，这里会记录最需要关注的问题。</p></div>
                <div class="engine-note"><span class="engine-note-kicker">This week</span><h3>本周计划</h3><p v-if="weeklyPlanItems.length">{{ weeklyPlanItems[0] }}</p><p v-else>完成一次练习，形成本周第一个可执行计划。</p></div>
                <div class="engine-note engine-note-next"><span class="engine-note-kicker">Next step</span><h3>下一步目标</h3><p v-if="nextGoal">{{ nextGoal }}</p><p v-else>从一次自由面试开始，建立第一条复盘记录。</p><button v-if="latestInterviewRecord" type="button" @click="openInterviewRecord(latestInterviewRecord)">查看复盘 <el-icon><ArrowRight /></el-icon></button></div>
              </section>
            </aside>
          </div>
        </section>
      </template>

      <section v-else-if="engineState === 'setup' && setupMode" class="engine-setup-shell" :data-setup-mode="setupMode" data-test="interview-setup">
        <InterviewComposer
          :mode="setupMode"
          @started="onThreeModeStarted"
          @open-knowledge-document="openKnowledgeDocument"
          @open-settings="openAiSettings"
        />
      </section>
    </template>

    <InterviewPlanReviewDialog
      v-model="showPlanReviewDialog"
      :summary="activePlanReviewSummary"
    />

    <!-- ========== 复盘报告（结束后或从历史进入） ========== -->
    <section v-if="activeSession && engineState === 'review' && activeSession.status === 'COMPLETED'" class="engine-review-shell" data-test="interview-review">
      <div v-if="sessionLoading || sessionLoadError" class="engine-review-load-state" :class="{ 'is-error': Boolean(sessionLoadError) }" role="status">
        <span>{{ sessionLoadError || '正在加载本次面试的题目、回答与评分…' }}</span>
        <button v-if="sessionLoadError" type="button" @click="retryActiveSessionLoad">重新加载</button>
      </div>
      <InterviewReviewPage
        v-if="!sessionLoading"
        :session="activeSession"
        :plan="activeReviewPlan"
        :history="activeState.history"
        :scores="activeState.perQuestionScores"
        @back-home="returnToInterviewHome"
        @re-practice="rePractice"
      />
    </section>

    <!-- ========== 聊天界面（有活跃会话时显示） ========== -->
    <section v-else-if="activeSession" class="interview-chat-layout">
      <div v-if="activeInterviewPlan" class="room-plan-header" data-test="interview-room-context">
        <div
          v-for="(item, index) in roomPlanChipItems"
          :key="`${item.label}-${index}`"
          class="room-plan-chip"
          :class="{ 'room-plan-mode': index === 0 }"
        >
          <span class="room-plan-chip-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span><small>{{ item.label }}</small><strong>{{ item.value }}</strong></span>
        </div>
        <div class="room-plan-progress">
          <small>练习进度</small>
          <strong>第 {{ currentIndex }} / {{ activeSession?.totalQuestions || activeInterviewPlan.questionCount }} 题</strong>
        </div>
      </div>
      <InterviewRoomSidebar
        :active-session="activeSession"
        :mode="activeInterviewPlan?.mode ?? 'ROLE_BASED'"
        :active-persona="activeSessionPersona"
        :active-persona-style="activePersonaStyle"
        :active-model-label="activeModelLabel"
        :active-strategy-label="activeStrategyLabel"
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
        <div v-if="sessionLoading" class="chat-session-loading" role="status">正在加载本次面试的题目与回答…</div>
        <div v-if="sessionLoadError" class="chat-session-load-error" role="alert">
          <span>{{ sessionLoadError }}</span>
          <button type="button" @click="retryActiveSessionLoad">重新加载</button>
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
          <div v-if="isExperienceSimulationRoom" class="voice-row">
            <el-tooltip
              :content="speechSupported ? '点击开始语音输入，再次点击结束；语音由浏览器处理，职达不保存音频' : '当前浏览器不支持语音识别'"
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
            <button
              type="button"
              class="question-read-button"
              :disabled="!speechSynthesisSupported || !currentQuestion?.questionText"
              @click="toggleQuestionReading"
            >{{ isReadingQuestion ? '停止朗读' : '朗读问题' }}</button>
          </div>
          <el-input
            v-model="activeState.answerDraft"
            type="textarea"
            :rows="2"
            maxlength="1200"
            show-word-limit
            :disabled="!canSubmitAnswer || actionLoading"
            placeholder="在此输入你的回答（支持 Markdown）"
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
          <button class="chat-room-inline-control" type="button" @click="toggleRoomPause">
            <el-icon><VideoPlay v-if="roomPaused" /><VideoPause v-else /></el-icon>
            <span>{{ roomPaused ? '继续' : '暂停' }}</span>
          </button>
          <button class="chat-room-inline-control chat-room-inline-end" type="button" @click="leaveRoom">
            <el-icon><RemoveFilled /></el-icon>
            <span>结束练习</span>
          </button>
        </div>
        <div v-if="errorMessage" class="chat-room-error" role="alert" data-test="interview-room-error">
          <span>{{ aiConfigurationRequired ? '需要先配置 AI 服务后才能继续回答。' : errorMessage }}</span>
          <button v-if="aiConfigurationRequired" type="button" @click="openAiSettings">去设置</button>
        </div>
      </div>
      <InterviewRoomContextPanel
        v-if="activeInterviewPlan"
        :mode="activeInterviewPlan.mode ?? 'ROLE_BASED'"
        :plan="activeInterviewPlan"
        :session="activeSession"
        :active-persona="activeSessionPersona"
        :model-label="activeModelLabel"
        :system-status="roomSystemStatus"
        :response-latency-ms="lastAiLatencyMs"
        :practice-elapsed="practiceElapsedLabel"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Aim,
  ArrowLeft,
  ArrowRight,
  Briefcase,
  Calendar,
  ChatLineSquare,
  Collection,
  Delete,
  Document,
  Download,
  Loading,
  Microphone,
  Reading,
  RemoveFilled,
  Suitcase,
  Tickets,
  Trophy,
  VideoPause,
  VideoPlay,
} from '@element-plus/icons-vue'
import { listJobDescriptions, resolveCompanyProfile } from '../api/job'
import { listAiProviders } from '../api/aiProviders'
import { useTargetsStore } from '../stores/targets'
import { companyMark } from '../constants/companyBrands'
import InterviewPlanReviewDialog from '../components/interview/InterviewPlanReviewDialog.vue'
import InterviewRoomSidebar from '../components/interview/InterviewRoomSidebar.vue'
import InterviewRoomContextPanel from '../components/interview/InterviewRoomContextPanel.vue'
import InterviewChatThread from '../components/interview/InterviewChatThread.vue'
import InterviewComposer from '../components/interview/InterviewComposer.vue'
import InterviewReviewPage from '../components/interview/InterviewReviewPage.vue'
import {
  generateInterviewPlanSummary,
  getInterviewStatus,
  getSessionHistory,
  deleteInterviewPlan,
  listInterviewerPersonas,
  listMyInterviewPlans,
  listMyInterviews,
  startInterview,
  submitInterviewAnswer,
} from '../api/interview'
import { getResumeVersion, getResumeVersions, listResumes } from '../api/resume'
import type { CompanyProfile, JobDescription } from '../types/job'
import type {
  EvaluationSummary,
  InterviewPlanResponse,
  InterviewPlanRound,
  InterviewerPersona,
  InterviewMode,
  InterviewStatusResponse,
  MultiSessionSummaryResponse,
  SessionHistoryItem,
} from '../types/interview'
import { TARGET_STAGE_LABELS } from '../types/project'
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
import {
  interviewEngineStateFromQuery,
  interviewEngineStateToQuery,
  type InterviewEngineState,
} from '../utils/interviewEngineState'
import { useInterviewSessions } from '../composables/useInterviewSessions'
import { canUseSpeechRecognition, speakInterviewQuestion, stopInterviewQuestionSpeech } from '../utils/interviewVoice'

interface ChatMessage {
  role: 'interviewer' | 'user' | 'sending' | 'evaluation' | 'summary'
  text?: string
  questionIndex?: number
  provenanceLabel?: string | null
  submittedAt?: string | null
  evaluation?: EvaluationSummary | null
}

interface LocalInterviewPlan extends InterviewPlanContext {
  mode?: InterviewMode
  startContextSnapshot?: Record<string, unknown> | null
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
const initialEngineLocation = interviewEngineStateFromQuery(route.query)
const engineState = ref<InterviewEngineState>(initialEngineLocation.state)
const engineSessionId = ref<number | null>(initialEngineLocation.sessionId)
const setupMode = ref<InterviewMode | null>(initialEngineLocation.mode)
const fromEditor = computed(() => route.query.from === 'editor')
const fromTarget = computed(() => route.query.from === 'target')
const fromWorkspace = computed(() => fromEditor.value || fromTarget.value)
const targetId = computed(() => positiveQueryId(route.query.targetId))
const workspaceReturnLabel = computed(() => fromTarget.value ? '返回求职目标工作台' : '返回当前简历工作台')
const loadingOptions = ref(false)
const actionLoading = ref(false)
const sessionLoading = ref(false)
const sessionLoadError = ref('')
const roomPaused = ref(false)
const activeReviewMode = ref(false)
const errorMessage = ref('')
const resumes = ref<Resume[]>([])
const versions = ref<ResumeVersion[]>([])
const jobs = ref<JobDescription[]>([])
const selectedCompanyProfile = ref<CompanyProfile | null>(null)
const selectedVersionId = ref<number | null>(null)
const selectedJobId = ref<number | null>(null)
const personas = ref<InterviewerPersona[]>([])
// 面试房间只读当前默认模型的公开名称；密钥永远不进入前端状态快照或日志。
const configuredAiModel = ref('')
const aiProviderReady = ref<boolean | null>(null)
const lastAiLatencyMs = ref<number | null>(null)

// 跨会话总结
const multiSummaryLoading = ref(false)
const deletedSessionIds = ref(new Set<number>())
const deletedSessionStorageKey = 'resumego:deletedInterviewSessionIds'

const showPlanReviewDialog = ref(false)
const showExportMenu = ref(false)
const showExportHistory = ref(false)
type InterviewExportHistoryEntry = {
  id: string
  fileName: string
  format: InterviewExportFormat
  recordCount: number
  createdAt: string
}
const exportHistoryStorageKey = 'resumego:interview-export-history'
const exportHistory = ref<InterviewExportHistoryEntry[]>(readExportHistory())

const engineStateLabel = computed(() => ({
  home: '面试主页',
  setup: '开始练习',
  room: '面试房间',
  review: '复盘报告',
}[engineState.value]))
const engineStateDescription = computed(() => ({
  home: '练习记录与下一步行动',
  setup: '选择来源，锁定本次练习上下文',
  room: '专注回答，完成后进入复盘',
  review: '把一次面试转化为下一步行动',
}[engineState.value]))
const setupModeLabel = computed(() => {
  if (setupMode.value === 'ROLE_BASED') return '岗位模拟'
  if (setupMode.value === 'KNOWLEDGE_TRAINING') return '知识训练'
  if (setupMode.value === 'EXPERIENCE_SIMULATION') return '真题演练'
  return '选择本次练习方式'
})
// 配置页与复盘页由各自的工作区负责标题和上下文；外层只在主页/房间保留身份栏，
// 避免复盘页出现“面试 / 复盘报告”与内部标题重复的左上角文案。
const showEngineIdentity = computed(() => engineState.value === 'home' || engineState.value === 'room')
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
const practiceElapsedSeconds = ref(0)
let practiceTimer: ReturnType<typeof setInterval> | null = null
let practiceStartedAt: number | null = null

const speechSupported = ref(false)
const isListening = ref(false)
const speechSynthesisSupported = ref(false)
const isReadingQuestion = ref(false)
let recognition: any = null
let recognitionSessionId: number | null = null
let voiceBaseDraft = ''
let voiceTranscript = ''

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

const latestInterviewRecord = computed(() => visibleInterviewRecords.value[0] ?? null)
// 首页固定保留最近五条；展开后使用完整列表，列表自身负责滚动浏览。
const recentInterviewRecords = computed(() => visibleInterviewRecords.value.slice(0, 5))
const showAllInterviewRecords = ref(false)
const displayedInterviewRecords = computed(() => showAllInterviewRecords.value
  ? visibleInterviewRecords.value
  : recentInterviewRecords.value)

const latestFocusTarget = computed(() => [...targetsStore.targets].sort((left, right) => {
  const leftTime = Date.parse(left.createdAt)
  const rightTime = Date.parse(right.createdAt)
  return (Number.isFinite(rightTime) ? rightTime : 0) - (Number.isFinite(leftTime) ? leftTime : 0)
})[0] ?? null)
const latestFocusJob = computed(() => {
  const jobId = latestFocusTarget.value?.jobDescriptionId
  return jobId == null ? null : jobs.value.find((job) => job.id === jobId) ?? null
})
const latestFocusCompanyName = computed(() => latestFocusJob.value?.companyName || latestFocusTarget.value?.name || '')
const latestFocusMark = computed(() => companyMark(latestFocusCompanyName.value))
const latestFocusTargetLabel = computed(() => {
  const target = latestFocusTarget.value
  if (!target) return '还没有求职目标'
  const company = latestFocusJob.value?.companyName
  const role = target.targetRole || latestFocusJob.value?.jobTitle
  if (company && role) return company + ' · ' + role
  return target.name || company || role || '未命名求职目标'
})
const latestFocusStageLabel = computed(() => {
  const target = latestFocusTarget.value
  return target ? (TARGET_STAGE_LABELS[target.stage] || '准备中') : '创建一个目标开始准备'
})

const todayHeader = computed(() => {
  const now = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getMonth() + 1}月${now.getDate()}日 · 星期${weekdays[now.getDay()]}`
})

type AbilityMetricKey = 'clarity' | 'depth' | 'structure' | 'evidence' | 'relevance'
const abilityMetricDefs: Array<{ key: AbilityMetricKey; label: string; scoreKey?: AbilityMetricKey }> = [
  { key: 'clarity', label: '清晰度', scoreKey: 'clarity' },
  { key: 'depth', label: '深度', scoreKey: 'depth' },
  { key: 'structure', label: '结构', scoreKey: 'structure' },
  { key: 'evidence', label: '证据', scoreKey: 'evidence' },
  { key: 'relevance', label: '相关性', scoreKey: 'relevance' },
]
const selectedAbilityMetric = ref<AbilityMetricKey>('clarity')
const selectedAbilityMetricDefinition = computed(() => abilityMetricDefs.find((item) => item.key === selectedAbilityMetric.value))
type GrowthChartView = 'line' | 'radar'
const growthChartView = ref<GrowthChartView>('line')

type RadarAxis = {
  key: 'clarity' | 'depth' | 'structure' | 'evidence' | 'relevance'
  label: string
  scoreKey?: 'clarity' | 'depth' | 'structure' | 'evidence' | 'relevance'
}
const radarAxes: RadarAxis[] = [
  { key: 'clarity', label: '表达清晰度', scoreKey: 'clarity' },
  { key: 'depth', label: '技术深度', scoreKey: 'depth' },
  { key: 'structure', label: '回答结构', scoreKey: 'structure' },
  { key: 'evidence', label: '证据具体性', scoreKey: 'evidence' },
  { key: 'relevance', label: '岗位相关性', scoreKey: 'relevance' },
]

function scoreValueForDimension(
  score: { clarity?: number; relevance?: number; depth?: number; structure?: number; evidence?: number; accuracy?: number },
  key: AbilityMetricKey,
) {
  // accuracy 是旧版“准确性”字段，只有在证据具体性缺失时作为兼容回退；
  // 结构分数没有旧字段，缺失时保持空值，不用默认值填充图表。
  if (key === 'evidence') return Number.isFinite(score.evidence) ? score.evidence : score.accuracy
  return score[key]
}

const abilityTrend = computed<Array<number | null>>(() => {
  const scoreKey = selectedAbilityMetricDefinition.value?.scoreKey
  if (!scoreKey) return visibleInterviewRecords.value.slice(0, 5).reverse().map(() => null)
  return visibleInterviewRecords.value.slice(0, 5).reverse().map((record) => {
    const scores = record.sessions.flatMap((session) => session.perQuestionScores ?? [])
    const values = scores
      .map((score) => scoreValueForDimension(score, scoreKey))
      .filter((value): value is number => Number.isFinite(value))
    if (!values.length) return null
    return Math.round((values.reduce((sum, value) => sum + value, 0) / values.length) * 10)
  })
})
const hasAbilityTrend = computed(() => abilityTrend.value.some((value) => value != null))
const abilityTrendPoints = computed(() => {
  const values = abilityTrend.value
  const step = values.length > 1 ? 312 / (values.length - 1) : 0
  return values.map((value, index) => value == null ? null : {
    index,
    value,
    x: 42 + index * step,
    y: 151 - (value / 100) * 135,
  })
})
const abilityTrendPolyline = computed(() => abilityTrendPoints.value.filter(Boolean).map((point) => `${point!.x},${point!.y}`).join(' '))
const abilityTrendLabels = computed(() => visibleInterviewRecords.value.slice(0, 5).reverse().map((record) => record.dateLabel || '—'))

const radarValues = computed<Array<number | null>>(() => radarAxes.map((axis) => {
  if (!axis.scoreKey) return null
  const scores = visibleInterviewRecords.value
    .slice(0, 5)
    .flatMap((record) => record.sessions.flatMap((session) => session.perQuestionScores ?? []))
  const values = scores
    .map((score) => scoreValueForDimension(score, axis.scoreKey!))
    .filter((value): value is number => Number.isFinite(value))
  return values.length
    ? Math.round((values.reduce((sum, value) => sum + value, 0) / values.length) * 10)
    : null
}))
const hasRadarTrend = computed(() => radarValues.value.some((value) => value != null))
const radarMissingLabels = computed(() => radarAxes
  .filter((_, index) => radarValues.value[index] == null)
  .map((axis) => axis.label))
const radarPointFor = (axis: RadarAxis, scale: number, value?: number | null) => {
  const index = radarAxes.findIndex((item) => item.key === axis.key)
  const angle = -Math.PI / 2 + index * ((Math.PI * 2) / radarAxes.length)
  const radius = 78 * scale * (value == null ? 1 : Math.max(0, Math.min(100, value)) / 100)
  return { x: 150 + Math.cos(angle) * radius, y: 118 + Math.sin(angle) * radius }
}
const radarGridPoints = (scale: number) => radarAxes.map((axis) => {
  const point = radarPointFor(axis, scale)
  return point.x + ',' + point.y
}).join(' ')
const axisPoint = (axis: RadarAxis, scale: number) => radarPointFor(axis, scale)
const axisLabelPoint = (axis: RadarAxis) => {
  const index = radarAxes.findIndex((item) => item.key === axis.key)
  const angle = -Math.PI / 2 + index * ((Math.PI * 2) / radarAxes.length)
  const point = { x: 150 + Math.cos(angle) * 101, y: 118 + Math.sin(angle) * 101 }
  return { x: point.x, y: point.y + (index === 0 ? -2 : index === 3 || index === 2 ? 4 : 2), anchor: Math.abs(point.x - 150) < 8 ? 'middle' : point.x < 150 ? 'end' : 'start' }
}
const radarPoints = computed(() => radarValues.value.flatMap((value, index) => {
  if (value == null) return []
  const axis = radarAxes[index]
  const point = radarPointFor(axis, 1, value)
  return [{ key: axis.key, x: point.x, y: point.y }]
}))
const radarPolygon = computed(() => {
  const measuredPoints = radarAxes.flatMap((axis, index) => {
    const value = radarValues.value[index]
    if (value == null) return []
    const point = radarPointFor(axis, 1, value)
    return [point.x + ',' + point.y]
  })
  return measuredPoints.length >= 3 ? measuredPoints.join(' ') : ''
})

const latestPlanSummary = computed(() => {
  const record = latestInterviewRecord.value
  if (!record) return null
  const plan = record.sessions.map((session) => localInterviewPlans.value[session.sessionId]).find(Boolean)
  return plan?.summary ?? null
})
const attentionItems = computed(() => (latestPlanSummary.value?.crossWeaknesses ?? []).filter(Boolean).slice(0, 2))
const weeklyPlanItems = computed(() => (latestPlanSummary.value?.suggestions ?? []).filter(Boolean).slice(0, 2))
const nextGoal = computed(() => weeklyPlanItems.value[0] ?? '')

const practiceEntries = [
  { mode: 'ROLE_BASED' as InterviewMode, label: '自由面试', description: '选择岗位简历 → 自动带出求职计划', icon: ChatLineSquare },
  { mode: 'KNOWLEDGE_TRAINING' as InterviewMode, label: '知识训练', description: '按知识点刻意练习与巩固', icon: Reading },
  { mode: 'EXPERIENCE_SIMULATION' as InterviewMode, label: '真题演练', description: '精选企业真题还原面试现场', icon: Tickets },
]

const activeInterviewPlan = computed(() => {
  if (!activeSessionId.value) return null
  return localInterviewPlans.value[activeSessionId.value] ?? null
})
const activeModeLabel = computed(() => ({
  ROLE_BASED: '自由面试',
  KNOWLEDGE_TRAINING: '知识训练',
  EXPERIENCE_SIMULATION: '真题演练',
}[activeInterviewPlan.value?.mode ?? 'ROLE_BASED']))
const activeRoomModeIcon = computed(() => ({
  ROLE_BASED: ChatLineSquare,
  KNOWLEDGE_TRAINING: Reading,
  EXPERIENCE_SIMULATION: Document,
}[activeInterviewPlan.value?.mode ?? 'ROLE_BASED']))
const activePlanCompanyLabel = computed(() => {
  const snapshot = activeInterviewPlan.value?.startContextSnapshot ?? {}
  const snapshotCompany = snapshot.companyName
  if (typeof snapshotCompany === 'string' && snapshotCompany.trim()) return snapshotCompany.trim()
  // 自由面试的计划快照只保存岗位实体 ID/项目名称时，优先从当前岗位实体解析公司。
  // 不要因为快照缺少 companyName 就把已绑定的岗位误显示成“未绑定公司”。
  const jobDescriptionId = activeInterviewPlan.value?.jobDescriptionId
  const linkedJob = jobDescriptionId == null
    ? null
    : jobs.value.find((job) => Number(job.id) === Number(jobDescriptionId)) ?? null
  if (linkedJob?.companyName?.trim()) return linkedJob.companyName.trim()
  if (selectedJobEntity.value && jobDescriptionId != null
      && Number(selectedJobEntity.value.id) === Number(jobDescriptionId)
      && selectedJobEntity.value.companyName?.trim()) {
    return selectedJobEntity.value.companyName.trim()
  }
  const snapshotProject = snapshot.jobProjectName
  if (typeof snapshotProject === 'string' && snapshotProject.trim()) {
    const projectValue = snapshotProject.trim()
    const jobTitles = [
      snapshot.jobTitle,
      linkedJob?.jobTitle,
      activeInterviewPlan.value?.jobLabel,
    ].filter((value): value is string => typeof value === 'string' && value.trim().length > 0).map((value) => value.trim())
    const matchingTitle = jobTitles.find((title) => projectValue.endsWith(title))
    if (matchingTitle) {
      const inferredCompany = projectValue.slice(0, -matchingTitle.length).trim().replace(/[·｜|]+$/, '').trim()
      if (inferredCompany) return inferredCompany
    }
    const projectParts = projectValue.split(/[｜|·]/).map((part) => part.trim()).filter(Boolean)
    if (projectParts.length > 1) return projectParts[projectParts.length - 1]
  }
  const jobLabel = activeInterviewPlan.value?.jobLabel || ''
  const parts = jobLabel.split(/[｜|·]/).map((part) => part.trim()).filter(Boolean)
  return parts.length > 1 ? parts[parts.length - 1] : '未绑定公司'
})
const activePlanJobLabel = computed(() => {
  const snapshot = activeInterviewPlan.value?.startContextSnapshot ?? {}
  const snapshotJob = snapshot.jobTitle
  if (typeof snapshotJob === 'string' && snapshotJob.trim()) return snapshotJob.trim()
  const jobLabel = activeInterviewPlan.value?.jobLabel || ''
  const parts = jobLabel.split(/[｜|·]/).map((part) => part.trim()).filter(Boolean)
  return parts[0] || jobLabel || '未绑定岗位'
})
const roomPrimarySourceLabel = computed(() => {
  const plan = activeInterviewPlan.value
  const snapshot = plan?.startContextSnapshot ?? {}
  if (plan?.mode === 'KNOWLEDGE_TRAINING') {
    const docs = snapshot.knowledgeDocumentTitles
    if (Array.isArray(docs) && docs.length) return docs.slice(0, 2).join('、')
    return '知识库资料'
  }
  if (plan?.mode === 'EXPERIENCE_SIMULATION') {
    return typeof snapshot.questionSetTitle === 'string' && snapshot.questionSetTitle.trim()
      ? snapshot.questionSetTitle.trim()
      : '真实面经题集'
  }
  const resumeTitle = snapshot.resumeTitle
  const resumeVersion = snapshot.resumeVersionNo
  if (typeof resumeTitle === 'string' && resumeTitle.trim()) {
    return typeof resumeVersion === 'number' ? `${resumeTitle.trim()} V${resumeVersion}` : resumeTitle.trim()
  }
  return plan?.resumeLabel || '未绑定简历版本'
})
const roomPlanChipItems = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return []
  if (plan.mode === 'KNOWLEDGE_TRAINING') {
    const snapshot = plan.startContextSnapshot ?? {}
    const focus = Array.isArray(snapshot.focusTags)
      ? snapshot.focusTags.filter((item): item is string => typeof item === 'string' && item.trim().length > 0).slice(0, 2).join('、')
      : ''
    return [
      { label: '模式', value: activeModeLabel.value, icon: activeRoomModeIcon.value },
      { label: '资料库', value: '本地知识库', icon: Reading },
      { label: '训练重点', value: focus || String(snapshot.questionStyle || snapshot.difficulty || '综合理解'), icon: Aim },
      { label: '资料', value: roomPrimarySourceLabel.value, icon: Document },
    ]
  }
  if (plan.mode === 'EXPERIENCE_SIMULATION') {
    const snapshot = plan.startContextSnapshot ?? {}
    const sourceType = snapshot.questionSetSourceType === 'IMPORTED_EXPERIENCE' ? '外部导入' : snapshot.questionSetSourceType === 'USER_MANUAL' ? '手动面经' : '知识库来源'
    return [
      { label: '模式', value: activeModeLabel.value, icon: activeRoomModeIcon.value },
      { label: '题集', value: roomPrimarySourceLabel.value, icon: Tickets },
      { label: '原题', value: `${plan.questionCount || activeSession.value?.totalQuestions || 0} 题`, icon: Collection },
      { label: '来源', value: sourceType, icon: Document },
    ]
  }
  return [
    { label: '模式', value: activeModeLabel.value, icon: activeRoomModeIcon.value },
    { label: '公司', value: activePlanCompanyLabel.value, icon: Briefcase },
    { label: '岗位', value: activePlanJobLabel.value, icon: Suitcase },
    { label: '简历', value: roomPrimarySourceLabel.value, icon: Document },
  ]
})
function snapshotText(...keys: string[]) {
  const snapshot = activeInterviewPlan.value?.startContextSnapshot ?? {}
  for (const key of keys) {
    const value = snapshot[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
  }
  return ''
}
const activeModelLabel = computed(() => {
  const configuredNow = configuredAiModel.value.trim()
  const snapshot = snapshotText('modelName', 'model', 'providerModel', 'aiModel')
  // 面试房间展示“当前配置”的公开模型名称；API Key 永远不进入界面。
  // 复盘则保留开始面试时的快照，避免历史记录随着设置变化而被改写。
  if (!activeReviewMode.value && configuredNow) return configuredNow
  return snapshot || configuredNow || '当前 AI 模型'
})
const activeStrategyLabel = computed(() => {
  const plan = activeInterviewPlan.value
  if (!plan) return '本次策略已锁定'
  const snapshot = plan.startContextSnapshot ?? {}
  if (plan.mode === 'ROLE_BASED') {
    const focus = Array.isArray(snapshot.focusTags)
      ? snapshot.focusTags.filter((item): item is string => typeof item === 'string' && item.trim().length > 0).slice(0, 2).join('、')
      : ''
    return focus || '岗位能力与项目证据'
  }
  if (plan.mode === 'KNOWLEDGE_TRAINING') return String(snapshot.questionStyle || snapshot.difficulty || '结构化理解')
  return String(snapshot.followUpIntensity || snapshot.reviewMode || '基于原题追问')
})
const roomSystemStatus = computed(() => {
  if (aiConfigurationRequired.value || aiProviderReady.value === false) return 'API 未配置'
  if (aiProviderReady.value === true) return 'API 已连接'
  return 'API 状态待检测'
})
const roomDefaultProvenanceLabel = computed(() => {
  const mode = activeInterviewPlan.value?.mode
  if (mode === 'KNOWLEDGE_TRAINING') return roomPrimarySourceLabel.value === '知识库资料' ? '知识库资料' : roomPrimarySourceLabel.value
  if (mode === 'EXPERIENCE_SIMULATION') return roomPrimarySourceLabel.value === '真实面经题集' ? '真实面经题集' : `${roomPrimarySourceLabel.value} · 原题`
  return '当前岗位与简历上下文'
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

const selectedJobEntity = computed(() => jobs.value.find((item) => item.id === selectedJobId.value) ?? null)
const currentQuestion = computed(() => activeSession.value?.currentQuestion ?? null)
// 题号与题干必须来自同一个 DTO。状态接口的 currentQuestionIndex 仅作无题干时
// 的恢复兜底，避免状态先更新而题干仍停留在上一题时出现视觉错位。
const currentIndex = computed(() => Math.max(
  1,
  activeSession.value?.currentQuestion?.questionIndex
    ?? activeSession.value?.currentQuestionIndex
    ?? 1,
))
// 后端的 `completed` 字段对 FAILED/CANCELLED 也表示终态，不能直接拿来
// 渲染“练习总结”；只有真正 COMPLETED 的会话才有可展示的总结。
const isCompleted = computed(() => activeSession.value?.status === 'COMPLETED')
const canSubmitAnswer = computed(
  () => !roomPaused.value && !activeReviewMode.value && activeSession.value?.status === 'WAITING_ANSWER' && Boolean(currentQuestion.value),
)
const isExperienceSimulationRoom = computed(() => activeInterviewPlan.value?.mode === 'EXPERIENCE_SIMULATION')
const aiConfigurationRequired = computed(() => /NOT_CONFIGURED|尚未配置\s*AI\s*模型服务|尚未配置\s*模型服务/i.test(errorMessage.value))
watch(() => currentQuestion.value?.questionText, () => {
  if (!isReadingQuestion.value) return
  stopInterviewQuestionSpeech()
  isReadingQuestion.value = false
})
const activePersonaStyle = computed(() =>
  activeSessionPersona.value?.style
    || activeSession.value?.personaTitle
    || '关注回答是否围绕岗位要求、项目证据和表达结构展开。',
)
const formatElapsedTime = computed(() => {
  const sec = elapsedTime.value
  if (sec < 60) return `已用 ${sec} 秒`
  const min = Math.floor(sec / 60)
  const s = sec % 60
  return `已用 ${min} 分 ${s} 秒`
})
const practiceElapsedLabel = computed(() => {
  const sec = practiceElapsedSeconds.value
  if (sec < 60) return `${sec} 秒`
  const min = Math.floor(sec / 60)
  const seconds = sec % 60
  if (min < 60) return `${min} 分 ${seconds} 秒`
  return `${Math.floor(min / 60)} 小时 ${min % 60} 分`
})

const chatMessages = computed<ChatMessage[]>(() => {
  const msgs: ChatMessage[] = []
  const hist = activeState.value.history
  // 房间始终以 activeSession.currentQuestion 作为题干和题号的唯一来源。
  // 提交后后端会推进到下一题，上一题的回答/评价仍保留在下方，但不再拿它
  // 覆盖当前题目，否则就会出现“第 2 题却显示第 1 题题干”的错位。
  const latestAnsweredHistory = [...hist].reverse().find((item) => item.answerText?.trim()) ?? null
  const question = currentQuestion.value
  if (question) {
    msgs.push({
      role: 'interviewer',
      text: question.questionText,
      questionIndex: question.questionIndex ?? currentIndex.value,
      provenanceLabel: question.provenanceLabel || roomDefaultProvenanceLabel.value,
    })
  } else if (latestAnsweredHistory) {
    // 完成后的会话没有 currentQuestion，用最后一条已回答记录恢复题干。
    msgs.push({
      role: 'interviewer',
      text: latestAnsweredHistory.questionText,
      questionIndex: latestAnsweredHistory.questionIndex,
      provenanceLabel: latestAnsweredHistory.provenanceLabel || roomDefaultProvenanceLabel.value,
    })
  }
  if (latestAnsweredHistory?.answerText) {
    msgs.push({
      role: 'user',
      text: latestAnsweredHistory.answerText,
      questionIndex: latestAnsweredHistory.questionIndex,
      submittedAt: formatSubmittedAt(latestAnsweredHistory.submittedAt),
    })
    if (latestAnsweredHistory.evaluation) {
      msgs.push({
        role: 'evaluation',
        questionIndex: latestAnsweredHistory.questionIndex,
        evaluation: latestAnsweredHistory.evaluation,
      })
    }
  }
  // 发送中的消息
  if (activeState.value.pendingAnswer) {
    msgs.push({
      role: 'sending',
      text: activeState.value.pendingAnswer,
      questionIndex: question?.questionIndex ?? currentIndex.value,
    })
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
const activeReviewPlan = computed<InterviewPlanResponse | null>(() => {
  const planId = activeInterviewPlan.value?.planId
  if (!planId) return null
  return persistedInterviewPlans.value.find((plan) => String(plan.planId) === planId) ?? null
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

function modeForRecord(record: InterviewRecord): InterviewMode | null {
  const plan = record.sessions.map((session) => localInterviewPlans.value[session.sessionId]).find(Boolean)
  return plan?.mode ?? null
}

function modeLabelForRecord(record: InterviewRecord) {
  return {
    ROLE_BASED: '自由面试',
    KNOWLEDGE_TRAINING: '知识训练',
    EXPERIENCE_SIMULATION: '真题演练',
  }[modeForRecord(record) ?? 'ROLE_BASED']
}

function modeIconForRecord(record: InterviewRecord) {
  return {
    ROLE_BASED: ChatLineSquare,
    KNOWLEDGE_TRAINING: Reading,
    EXPERIENCE_SIMULATION: Document,
  }[modeForRecord(record) ?? 'ROLE_BASED']
}

function planForRecord(record: InterviewRecord) {
  return record.sessions.map((session) => localInterviewPlans.value[session.sessionId]).find(Boolean) ?? null
}

function recordModeContext(record: InterviewRecord) {
  return {
    ROLE_BASED: '岗位训练',
    KNOWLEDGE_TRAINING: '知识库考察',
    EXPERIENCE_SIMULATION: '真实面经',
  }[modeForRecord(record) ?? 'ROLE_BASED']
}

function recordBindingLabel(record: InterviewRecord) {
  const mode = modeForRecord(record)
  if (mode === 'ROLE_BASED') {
    const jobLabel = planForRecord(record)?.jobLabel
    return jobLabel ? '关联：' + jobLabel + '求职计划' : '未关联求职计划'
  }
  return mode === 'KNOWLEDGE_TRAINING' ? '来源：知识库资料' : '来源：真实面经题集'
}

function recordTimeLabel(record: InterviewRecord) {
  const createdAt = planForRecord(record)?.createdAt
  if (!createdAt) return '时间未记录'
  const date = new Date(createdAt)
  if (Number.isNaN(date.getTime())) return '时间未记录'
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatSubmittedAt(value?: string | null) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).replace(/\//g, '-')
}

function recordIssue(record: InterviewRecord) {
  const plan = planForRecord(record)
  if (!record.isCompleted) return '这次练习尚未完成，继续回答后生成复盘重点。'
  return plan?.summary?.crossWeaknesses?.[0]
    ?? plan?.summary?.suggestions?.[0]
    ?? '已完成练习，打开记录查看逐题证据。'
}

function reviewProgressLabel(record: InterviewRecord) {
  if (record.totalCount <= 0) return '未开始'
  const failedCount = record.sessions.filter((session) => session.status === 'FAILED').length
  const activeCount = record.sessions.filter((session) => session.status === 'WAITING_ANSWER' || session.status === 'EVALUATING' || session.status === 'ASKING').length
  if (failedCount > 0 && record.completedCount === 0 && activeCount === 0) return `已中断 ${failedCount} 轮`
  const progress = Math.round((record.completedCount / record.totalCount) * 100)
  if (progress > 0) return `已复盘 ${progress}%`
  return activeCount > 0 ? '进行中' : '未开始'
}

type InterviewExportFormat = 'md' | 'txt'
type ExportSessionSection = { session: InterviewStatusResponse; history: SessionHistoryItem[] }

async function exportInterviewRecords(format: InterviewExportFormat = 'md') {
  if (!visibleInterviewRecords.value.length) return
  showExportMenu.value = false
  showExportHistory.value = false
  const sections = await Promise.all(visibleInterviewRecords.value.map(async (record) => {
    const sessionSections = await Promise.all(record.sessions.map(async (session) => {
      let history = sessionStates.value[session.sessionId]?.history ?? []
      if (!history.length) {
        try {
          history = (await getSessionHistory(session.sessionId)).data.items
        } catch {
          // 导出仍保留记录摘要；逐题内容缺失时在文件中如实说明。
        }
      }
      return { session, history }
    }))
    return formatInterviewRecordForExport(record, sessionSections, format)
  }))
  const separator = format === 'md' ? '\n\n---\n\n' : '\n\n'
  const mime = format === 'md' ? 'text/markdown;charset=utf-8' : 'text/plain;charset=utf-8'
  const blob = new Blob([sections.join(separator)], { type: mime })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `职达-面试回答-${new Date().toISOString().slice(0, 10)}.${format}`
  anchor.click()
  URL.revokeObjectURL(url)
  const historyEntry: InterviewExportHistoryEntry = {
    id: `${Date.now()}-${format}`,
    fileName: anchor.download,
    format,
    recordCount: sections.length,
    createdAt: new Date().toISOString(),
  }
  exportHistory.value = [historyEntry, ...exportHistory.value].slice(0, 10)
  localStorage.setItem(exportHistoryStorageKey, JSON.stringify(exportHistory.value))
}

function formatInterviewRecordForExport(
  record: InterviewRecord,
  sections: ExportSessionSection[],
  format: InterviewExportFormat,
) {
  const plan = planForRecord(record)
  const header = [
    format === 'md' ? '# ' + record.title : record.title,
    '模式：' + modeLabelForRecord(record),
    '来源：' + recordBindingLabel(record),
    '时间：' + (record.dateLabel || '最近') + ' ' + recordTimeLabel(record),
    '复盘进度：' + reviewProgressLabel(record),
    '核心问题：' + recordIssue(record),
  ]
  const blocks = sections.flatMap(({ history }) => {
    if (!history.length) return ['当前未加载到逐题回答；请先打开该记录完成历史回放。']
    return history.map((item) => {
      // 后端题号从 1 开始；导出与房间/复盘保持同一题号，不能再次 +1。
      const title = (format === 'md' ? '### ' : '') + '第 ' + item.questionIndex + ' 题'
      const questionLabel = format === 'md' ? '**问题**' : '问题：'
      const answerLabel = format === 'md' ? '**我的回答**' : '我的回答：'
      const lines = [title, questionLabel, item.questionText || '—', answerLabel, item.answerText || '—']
      const evaluation = item.evaluation
      if (evaluation) {
        const score = evaluation.score
        const summary = [
          evaluation.strengths?.length ? '优点：' + evaluation.strengths.join('；') : '',
          evaluation.weaknesses?.length ? '待改进：' + evaluation.weaknesses.join('；') : '',
          evaluation.suggestions?.length ? '建议：' + evaluation.suggestions.join('；') : '',
          score ? [
                            `评分：清晰度 ${score.clarity ?? '—'}`,
                            `相关性 ${score.relevance ?? '—'}`,
                            `深度 ${score.depth ?? '—'}`,
                            `回答结构 ${score.structure ?? '—'}`,
                            `证据具体性 ${score.evidence ?? score.accuracy ?? '—'}`,
          ].filter(Boolean).join('，') : '',
        ].filter(Boolean)
        if (summary.length) lines.push(format === 'md' ? '**反馈**\n' + summary.join('\n') : summary.join('\n'))
      }
      return lines.join('\n\n')
    })
  })
  if (plan?.summary) {
    const summary = format === 'md'
      ? ['## 本次复盘摘要', plan.summary.overallSummary, plan.summary.suggestions?.length ? '下一步：' + plan.summary.suggestions[0] : ''].filter(Boolean)
      : ['本次复盘摘要：' + plan.summary.overallSummary, plan.summary.suggestions?.length ? '下一步：' + plan.summary.suggestions[0] : ''].filter(Boolean)
    blocks.push(summary.join('\n\n'))
  }
  return [...header, ...blocks].join(format === 'md' ? '\n\n' : '\n')
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
    syncEngineState('review', target.sessionId)
    await switchToSession(target.sessionId)
    await preloadRecordHistories(orderedSessions)
    return
  }
  activeReviewMode.value = false
  // 多轮计划可能包含已失败的旧轮次和尚未开始的下一轮；优先恢复可继续的
  // WAITING_ANSWER/READY 轮次，避免点击历史记录后落到 FAILED 空房间。
  const target = orderedSessions.find((session) => session.status === 'WAITING_ANSWER')
    ?? orderedSessions.find((session) => session.status === 'READY')
    ?? orderedSessions.find((session) => !sessionCompleted(session))
    ?? orderedSessions[0]
  if (target) {
    syncEngineState('room', target.sessionId)
    if (target.status === 'READY') {
      await startExistingInterviewSession(target.sessionId)
    } else {
      await switchToSession(target.sessionId)
    }
  }
}

/**
 * 从历史记录继续一个尚未启动的会话。
 *
 * 计划列表只返回轮次摘要（没有 currentQuestion），因此 READY 会话不能仅靠
 * switchToSession 恢复。这里复用创建计划后的正式启动链路，并保留清晰错误，
 * 让缺少 AI 配置或首题生成失败时回到可重试的房间，而不是展示空白画布。
 */
async function startExistingInterviewSession(sessionId: number) {
  actionLoading.value = true
  actionStage.value = '正在恢复面试上下文...'
  errorMessage.value = ''
  sessionLoadError.value = ''
  startElapsedTimer()
  try {
    await loadAiModelProfile()
    if (aiProviderReady.value === false) {
      throw new Error('尚未配置 AI 模型服务，请先配置后再继续面试。')
    }
    const started = await startInterview(sessionId)
    if (started.data.status === 'FAILED' || !started.data.currentQuestion) {
      throw new Error('首题生成失败，暂时无法恢复面试；请检查 AI 配置后重试。')
    }
    upsertSession(started.data)
    activeSessionId.value = sessionId
    engineSessionId.value = sessionId
    activeReviewMode.value = false
    roomPaused.value = false
    syncEngineState('room', sessionId)
    startPracticeTimer()
    getOrCreateSessionState(sessionId)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '恢复面试失败，请重试'
    sessionLoadError.value = errorMessage.value
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

async function preloadRecordHistories(recordSessions: InterviewStatusResponse[]) {
  const results = await Promise.allSettled(recordSessions.map(async (session) => {
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
  const failed = results.filter((result): result is PromiseRejectedResult => result.status === 'rejected')
  if (failed.length) {
    const reason = failed[0].reason
    const message = reason instanceof Error ? reason.message : '部分面试记录加载失败，请点击重新加载'
    sessionLoadError.value = message
    errorMessage.value = message
  }
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

function saveDeletedSessionIds() {
  try {
    window.localStorage.setItem(
      deletedSessionStorageKey,
      JSON.stringify([...deletedSessionIds.value].filter((id) => Number.isFinite(id))),
    )
  } catch {
    // 本地存储不可用时仍保留当前页面内的隐藏状态，不阻断其他面试记录。
  }
}

async function deleteInterviewRecord(record: InterviewRecord, event?: Event) {
  event?.stopPropagation()
  if (typeof window !== 'undefined' && !window.confirm(`将“${record.title}”移入回收站？`)) return

  const plan = planForRecord(record)
  try {
    if (plan?.planId) {
      await deleteInterviewPlan(Number(plan.planId))
    }
    const sessionIds = record.sessions.map((session) => session.sessionId)
    deletedSessionIds.value = new Set([...deletedSessionIds.value, ...sessionIds])
    saveDeletedSessionIds()
    sessionIds.forEach((sessionId) => {
      delete localInterviewPlans.value[sessionId]
      removeSessionState(sessionId)
    })
    if (plan?.planId) {
      persistedInterviewPlans.value = persistedInterviewPlans.value.filter((item) => String(item.planId) !== plan.planId)
    }
    if (activeSessionId.value != null && sessionIds.includes(activeSessionId.value)) {
      returnToInterviewHome()
    }
    showAllInterviewRecords.value = false
    ElMessage.success('面试记录已移入回收站')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '移入回收站失败，请稍后重试'
  }
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
  if (plan.resumeVersionId == null) {
    return plan.mode === 'KNOWLEDGE_TRAINING' || plan.mode === 'EXPERIENCE_SIMULATION'
      ? '本次练习不绑定简历'
      : '未绑定简历版本'
  }
  return resumeOptions.value.find((item) => item.value === plan.resumeVersionId)?.label
    ?? `简历版本 #${plan.resumeVersionId}`
}

function resolvePlanJobLabel(plan: InterviewPlanResponse) {
  if (plan.jobDescriptionId == null) {
    return plan.mode === 'KNOWLEDGE_TRAINING' ? '知识训练' : plan.mode === 'EXPERIENCE_SIMULATION' ? '真题演练' : plan.title
  }
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
        mode: plan.mode ?? 'ROLE_BASED',
        startContextSnapshot: plan.startContextSnapshot ?? null,
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
      // /interview-plans/my 只返回轮次摘要，currentQuestion、summary 和逐题评分
      // 本来就是 null；不能让它覆盖前面 /interviews/my 已经加载的完整会话数据。
      // 否则刷新主页后，正在进行的会话会被误变成“空房间”，评分趋势也会消失。
      const existingSession = sessions.value.find((session) => session.sessionId === round.sessionId)
      if (!existingSession) {
        upsertSession(buildStatusFromPlanRound(round))
      } else {
        updateSessionInList(round.sessionId, {
          status: round.status,
          currentQuestionIndex: round.currentQuestionIndex,
          totalQuestions: round.totalQuestions,
          currentQuestion: round.status === 'WAITING_ANSWER' || round.status === 'ASKING'
            ? existingSession.currentQuestion
            : null,
          completed: round.completed,
          summaryJson: existingSession.summaryJson,
          perQuestionScores: existingSession.perQuestionScores,
        })
      }
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
function onThreeModeStarted(plan: InterviewPlanResponse) {
  void enterInterviewRoomFromPlan(plan)
}

/** The setup composer creates a plan only; the first round is started here so
 * every mode follows the same home → setup → room transition. */
async function enterInterviewRoomFromPlan(plan: InterviewPlanResponse) {
  const firstRound = [...plan.rounds].sort((a, b) => a.roundOrder - b.roundOrder)[0]
  if (!firstRound) {
    errorMessage.value = '练习计划缺少可启动的题目轮次'
    return
  }
  // 创建会话前刷新运行时状态，避免“配置记录已保存但密钥未装载”时进入一个
  // 没有题目、只能在提交阶段才暴露错误的空房间。
  await loadAiModelProfile()
  if (aiProviderReady.value === false) {
    errorMessage.value = '尚未配置 AI 模型服务，请先配置后再开始练习。'
    return
  }
  actionLoading.value = true
  actionStage.value = '正在锁定本次练习上下文...'
  startElapsedTimer()
  errorMessage.value = ''
  try {
    applyBackendPlans([plan])
    actionStage.value = '正在进入面试房间...'
    const started = await startInterview(firstRound.sessionId)
    if (started.data.status === 'FAILED' || !started.data.currentQuestion) {
      throw new Error('首题生成失败，暂时无法进入面试房间；请检查 AI 配置后重试。')
    }
    upsertSession(started.data)
  activeReviewMode.value = false
  activeSessionId.value = started.data.sessionId
  engineSessionId.value = started.data.sessionId
  syncEngineState('room', started.data.sessionId)
    startPracticeTimer()
    getOrCreateSessionState(started.data.sessionId)
    ElMessage.success('已进入面试房间')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '进入面试房间失败'
    // 创建失败时回到本次计划对应的配置页，不能丢失独立模式上下文。
    syncEngineState('setup', null, plan.mode ?? 'ROLE_BASED')
  } finally {
    actionLoading.value = false
    stopElapsedTimer()
  }
}

onMounted(() => {
  loadDeletedSessionIds()
  loadOptions()
  void loadAiModelProfile()
  initSpeechRecognition()
  window.addEventListener('focus', refreshAiModelProfile)
  document.addEventListener('visibilitychange', handleAiModelVisibilityChange)
})

// ── 语音识别 ──

function initSpeechRecognition() {
  const SpeechRecognitionConstructor =
    (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  speechSupported.value = canUseSpeechRecognition(window)
  speechSynthesisSupported.value = Boolean(window.speechSynthesis && window.SpeechSynthesisUtterance)
  if (!SpeechRecognitionConstructor) return
  recognition = new SpeechRecognitionConstructor()
  recognition.continuous = true
  recognition.interimResults = true
  recognition.lang = 'zh-CN'
  recognition.onresult = (event: any) => {
    let finalTranscript = ''
    let interimTranscript = ''
    const startIndex = Number.isInteger(event.resultIndex) ? event.resultIndex : 0
    for (let i = startIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0]?.transcript ?? ''
      if (event.results[i].isFinal) finalTranscript += transcript
      else interimTranscript += transcript
    }
    if (recognitionSessionId != null) {
      const state = getOrCreateSessionState(recognitionSessionId)
      if (finalTranscript.trim()) {
        voiceTranscript = `${voiceTranscript} ${finalTranscript}`.trim()
      }
      const combined = `${voiceBaseDraft} ${voiceTranscript} ${interimTranscript}`.trim()
      state.answerDraft = combined
    }
  }
  recognition.onend = () => {
    isListening.value = false
    voiceTranscript = ''
  }
  recognition.onerror = (event: any) => {
    ElMessage.error(`语音识别失败：${event.error}`)
    isListening.value = false
    voiceTranscript = ''
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
      voiceBaseDraft = activeState.value.answerDraft.trim()
      voiceTranscript = ''
      recognition.start()
      isListening.value = true
    } catch (e) {
      console.error('启动语音识别失败:', e)
      ElMessage.error('启动语音识别失败')
    }
  }
}

function toggleQuestionReading() {
  if (isReadingQuestion.value) {
    stopInterviewQuestionSpeech()
    isReadingQuestion.value = false
    return
  }
  const question = currentQuestion.value?.questionText
  if (!question) return
  isReadingQuestion.value = speakInterviewQuestion(question)
}

function openAiSettings() {
  void router.push({ name: 'settings', query: { from: 'interview' } })
}

onUnmounted(() => {
  stopElapsedTimer()
  stopPracticeTimer()
  stopInterviewQuestionSpeech()
  window.removeEventListener('focus', refreshAiModelProfile)
  document.removeEventListener('visibilitychange', handleAiModelVisibilityChange)
})

function handleEnterKey(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  if (canSubmitAnswer.value && activeState.value.answerDraft.trim() && !actionLoading.value) {
    handleSubmitAnswer()
  }
}

// ── 数据加载 ──

async function loadAiModelProfile() {
  try {
    const profiles = await listAiProviders()
    // 运行时真正可用的配置优先于“默认但没有密钥”的占位配置。
    // 否则用户已经为第二个服务装载 API Key 时，房间仍会显示默认服务的
    // “未配置”，提交评价也会错误地走未配置客户端。
    const active = profiles.find((profile) => profile.defaultProfile && profile.apiKeyConfigured)
      ?? profiles.find((profile) => profile.apiKeyConfigured)
      ?? profiles.find((profile) => profile.defaultProfile)
      ?? profiles[0]
    const desktopKeyLoaded = active && window.resumeGoDesktop
      ? await window.resumeGoDesktop.hasApiKey(active.id)
      : false
    let runtimeReady = Boolean(active?.apiKeyConfigured)
    // Electron 将密钥放在安全存储中。后端重启后内存运行时会清空，
    // 这里在进入面试前重新装载，避免“设置页显示已保存、房间首次回答才失败”。
    if (active && desktopKeyLoaded && !active.apiKeyConfigured) {
      runtimeReady = await window.resumeGoDesktop?.applyApiKey(active.id) ?? false
    }
    configuredAiModel.value = active?.defaultModel?.trim() ?? ''
    aiProviderReady.value = active
      // 上一次“测试连接失败”可能对应旧密钥或旧 Base URL；只要当前运行时确实
      // 装载了密钥，就允许进入房间，让真实调用结果负责反馈，而不是被陈旧状态拦截。
      ? Boolean(runtimeReady || desktopKeyLoaded)
      : false
  } catch {
    // 模型配置读取失败不阻断房间；状态会诚实地保留为待检测。
    configuredAiModel.value = ''
    aiProviderReady.value = null
  }
}

/**
 * Settings can update the active provider while the interview route remains mounted.
 * Refresh on returning to the window so the room reflects the model configured now,
 * while review mode can still use the historical snapshot label.
 */
function refreshAiModelProfile() {
  void loadAiModelProfile()
}

function handleAiModelVisibilityChange() {
  if (document.visibilityState === 'visible') refreshAiModelProfile()
}

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
    sessions.value = sessionsRes.data.filter((session) => !deletedSessionIds.value.has(session.sessionId))

    if (!targetsStore.targets.length) {
      await targetsStore.load()
    }

    const queryVersionId = Number(route.query.versionId)
    const queryJobId = Number(route.query.jobId)
    const storedJobId = getWorkspaceSelectedJobId()
    const storedResumeId = getWorkspaceSelectedResumeId()
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
    const initialSession = initialEngineLocation.sessionId == null
      ? null
      : sessions.value.find((session) => session.sessionId === initialEngineLocation.sessionId) ?? null
    if (initialSession) {
      // 复盘只能从已完成的会话进入；即使用户手动拼接 review 查询参数，
      // 未完成的会话也必须回到可继续回答的房间。
      activeReviewMode.value = initialEngineLocation.state === 'review' && initialSession.status === 'COMPLETED'
      await switchToSession(initialSession.sessionId)
    } else if (initialEngineLocation.state === 'setup' && initialEngineLocation.mode) {
      syncEngineState('setup', null, initialEngineLocation.mode)
    } else {
      syncEngineState('home')
    }
    if (engineState.value === 'home') {
      void hydrateHomepageScoreData()
    }
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

async function switchToSession(sessionId: number, force = false) {
  const alreadyActive = activeSessionId.value === sessionId

  activeSessionId.value = sessionId
  engineSessionId.value = sessionId
  syncEngineState(activeReviewMode.value ? 'review' : 'room', sessionId)
  if (activeReviewMode.value) stopPracticeTimer()
  else startPracticeTimer()
  const state = getOrCreateSessionState(sessionId)
  sessionLoadError.value = ''

  // 如果该会话还没有加载过历史，从后端加载
  if (force || !alreadyActive || state.history.length === 0) {
    sessionLoading.value = true
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
    } catch (error) {
      const message = error instanceof Error ? error.message : '加载会话历史失败，请重试'
      sessionLoadError.value = message
      errorMessage.value = message
      console.error('加载会话历史失败:', error)
    } finally {
      if (activeSessionId.value === sessionId) sessionLoading.value = false
    }
  } else {
    sessionLoading.value = false
  }
}

function retryActiveSessionLoad() {
  if (activeSessionId.value == null) return
  void switchToSession(activeSessionId.value, true)
}

async function refreshSessionHistory(sessionId: number) {
  const state = getOrCreateSessionState(sessionId)
  const historyRes = await getSessionHistory(sessionId)
  state.history = historyRes.data.items
}

/** 列表接口较旧时可能没有携带逐题评分；主页异步补齐历史评分，避免把“有记录”误判成“无趋势”。 */
async function hydrateHomepageScoreData() {
  const candidates = sessions.value
    .filter((session) => !(session.perQuestionScores?.length))
    .slice(0, 8)
  await Promise.allSettled(candidates.map(async (session) => {
    const state = getOrCreateSessionState(session.sessionId)
    if (state.history.length) return
    try {
      const history = (await getSessionHistory(session.sessionId)).data.items
      state.history = history
      const scores = history.flatMap((item) => {
        const score = item.evaluation?.score
        if (!score || ![score.clarity, score.relevance, score.depth].every(Number.isFinite)) return []
        const evidence = Number.isFinite(score.evidence) ? score.evidence : score.accuracy
        if (!Number.isFinite(evidence)) return []
        return [{
          questionIndex: item.questionIndex,
          questionText: item.questionText,
          clarity: score.clarity,
          relevance: score.relevance,
          depth: score.depth,
          structure: Number.isFinite(score.structure) ? score.structure : undefined,
          evidence,
          accuracy: score.accuracy,
        }]
      })
      if (scores.length) {
        updateSessionInList(session.sessionId, { perQuestionScores: scores })
      }
    } catch {
      // 评分补齐失败不阻断主页；图表继续保持真实空状态。
    }
  }))
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

function syncEngineState(
  nextState: InterviewEngineState,
  sessionId: number | null = null,
  mode: InterviewMode | null = nextState === 'setup' ? setupMode.value : null,
) {
  engineState.value = nextState
  engineSessionId.value = sessionId
  setupMode.value = nextState === 'setup' ? mode : null
  const { view: _view, sessionId: _sessionId, mode: _mode, ...stableQuery } = route.query
  const nextQuery = {
    ...stableQuery,
    ...interviewEngineStateToQuery(nextState, sessionId, setupMode.value),
  }
  if (typeof (router as any).replace === 'function') {
    void (router as any).replace({ query: nextQuery }).catch(() => undefined)
  }
}

function openPracticeSetup(mode: InterviewMode) {
  if (activeSessionId.value) return
  // 错误提示只属于当前一次启动尝试，切换模式时不能把上一个模式的 API
  // 或上下文错误带到新的配置页。
  errorMessage.value = ''
  syncEngineState('setup', null, mode)
}

function openKnowledgeDocument(documentId: number) {
  void router.push({ name: 'knowledge', query: { documentId: String(documentId) } })
}

function returnToInterviewHome() {
  activeSessionId.value = null
  activeReviewMode.value = false
  roomPaused.value = false
  stopPracticeTimer()
  errorMessage.value = ''
  syncEngineState('home')
}

function toggleRoomPause() {
  roomPaused.value = !roomPaused.value
  if (roomPaused.value) stopInterviewQuestionSpeech()
}

function leaveRoom() {
  roomPaused.value = false
  returnToInterviewHome()
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

// ── 返回角色卡选择 ──

function backToPersona() {
  returnToInterviewHome()
}

function rePractice() {
  const mode = activeInterviewPlan.value?.mode ?? 'ROLE_BASED'
  activeSessionId.value = null
  activeReviewMode.value = false
  syncEngineState('setup', null, mode)
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
  if (plan.jobDescriptionId != null) setWorkspaceSelectedJobId(plan.jobDescriptionId)

  try {
    const startRes = await startInterview(nextSession.sessionId)
    if (startRes.data.status === 'FAILED' || !startRes.data.currentQuestion) {
      throw new Error('下一轮首题生成失败，请检查 AI 配置后重试。')
    }
    upsertSession(startRes.data)
    activeSessionId.value = startRes.data.sessionId
    syncEngineState('room', startRes.data.sessionId)
    startPracticeTimer()
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
  const aiRequestStartedAt = Date.now()

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: currentAnswer })
    lastAiLatencyMs.value = Math.max(0, Date.now() - aiRequestStartedAt)

    // 清除发送中状态
    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = res.data.errorMessage || 'AI 评价暂时不可用，请稍后重试'
      return
    }

    state.history.push({
      questionIndex: currentQ?.questionIndex ?? (res.data.currentQuestionIndex > 0 ? res.data.currentQuestionIndex - 1 : 0),
      questionText: currentQ?.questionText ?? '',
      questionType: currentQ?.questionType ?? '',
      answerText: currentAnswer,
      evaluation: res.data.evaluation ?? null,
      submittedAt: new Date().toISOString(),
    })

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    // 后端的 completed 字段兼容地表示所有终态；只有 COMPLETED 才进入复盘，
    // FAILED/CANCELLED 必须留在房间展示可重试错误，不能把失败误导成已完成。
    if (res.data.completed && res.data.status === 'COMPLETED') {
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
      activeReviewMode.value = true
      syncEngineState('review', sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    lastAiLatencyMs.value = Math.max(0, Date.now() - aiRequestStartedAt)
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
  const retryAnswer = state.lastSubmitAnswer
  const currentQuestion = activeSession.value.currentQuestion

  state.pendingAnswer = retryAnswer
  actionLoading.value = true
  errorMessage.value = ''
  state.retryable = false
  startElapsedTimer()
  const aiRequestStartedAt = Date.now()

  try {
    const res = await submitInterviewAnswer(sessionId, { answerText: retryAnswer })
    lastAiLatencyMs.value = Math.max(0, Date.now() - aiRequestStartedAt)

    state.pendingAnswer = ''

    if (res.data.retryable) {
      state.retryable = true
      stopElapsedTimer()
      errorMessage.value = res.data.errorMessage || 'AI 评价仍然不可用，请稍后再试'
      return
    }

    if (state.history.length > 0) {
      state.history[state.history.length - 1].evaluation = res.data.evaluation ?? null
    } else {
      // 首次评价失败时，后端已经保存回答但前端没有历史条目；重试成功后补齐本地记录，
      // 让用户立即看到评价，避免必须离开房间或刷新后才出现复盘数据。
      state.history.push({
        questionIndex: currentQuestion?.questionIndex ?? Math.max(0, res.data.currentQuestionIndex - 1),
        questionText: currentQuestion?.questionText ?? '',
        questionType: currentQuestion?.questionType ?? '',
        answerText: retryAnswer,
        evaluation: res.data.evaluation ?? null,
        submittedAt: new Date().toISOString(),
      })
    }

    updateSessionInList(sessionId, {
      status: res.data.status,
      currentQuestionIndex: res.data.currentQuestionIndex,
      totalQuestions: res.data.totalQuestions,
      currentQuestion: res.data.nextQuestion ?? null,
      completed: res.data.completed,
    })

    if (res.data.completed && res.data.status === 'COMPLETED') {
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
      activeReviewMode.value = true
      syncEngineState('review', sessionId)
      ElMessage.success('模拟面试已完成')
    }
  } catch (error) {
    lastAiLatencyMs.value = Math.max(0, Date.now() - aiRequestStartedAt)
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

function startPracticeTimer() {
  if (practiceTimer !== null) return
  if (practiceStartedAt === null) practiceStartedAt = Date.now() - practiceElapsedSeconds.value * 1000
  practiceTimer = setInterval(() => {
    if (practiceStartedAt === null) return
    practiceElapsedSeconds.value = Math.max(0, Math.floor((Date.now() - practiceStartedAt) / 1000))
  }, 1000)
}

function stopPracticeTimer() {
  if (practiceTimer !== null) {
    clearInterval(practiceTimer)
    practiceTimer = null
  }
  practiceStartedAt = null
  practiceElapsedSeconds.value = 0
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

function readExportHistory(): InterviewExportHistoryEntry[] {
  try {
    const raw = localStorage.getItem('resumego:interview-export-history')
    const parsed = raw ? JSON.parse(raw) : []
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is InterviewExportHistoryEntry => (
      item && typeof item.id === 'string' && typeof item.fileName === 'string'
      && (item.format === 'md' || item.format === 'txt')
      && Number.isFinite(item.recordCount) && typeof item.createdAt === 'string'
    )).slice(0, 10)
  } catch {
    return []
  }
}

function formatExportHistoryTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间未记录'
  return date.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.interview-command-bar{display:flex;align-items:center;gap:18px;min-height:58px;padding:8px 4px 14px;border-bottom:1px solid var(--border-subtle);margin-bottom:16px}
.bar-identity{display:flex;align-items:baseline;gap:14px}
.bar-title.hero-title{font-size:26px;font-weight:700;letter-spacing:-.02em;color:#17181a;margin:0}
.bar-title{display:inline-flex;align-items:center;gap:10px;margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.bar-title-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;background:var(--brand-soft);color:var(--brand)}
.bar-subtitle{color:var(--muted);font-size:12.5px}
.lobby-three-mode{margin-bottom:20px}
.three-mode-head{display:flex;justify-content:flex-end;margin-bottom:8px}

.interview-page {
  box-sizing: border-box;
  min-height: 100vh;
  --canvas: #fff;
  background: #fff;
  color: var(--ink);
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  padding: 18px;
}
body[data-theme='dark'] .interview-page {
  --canvas: #111212;
  background: #111212;
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
/* Interview Engine v2: state-led desktop workspace */
.engine-breadcrumb{color:var(--muted);font-size:12px;font-weight:650;letter-spacing:.04em}
.engine-breadcrumb-separator{color:var(--border-strong);font-size:13px}
.engine-back-button{display:inline-flex;align-items:center;gap:7px;margin-left:auto;border:1px solid var(--border-subtle);border-radius:7px;background:var(--surface-solid,#fff);color:var(--ink);padding:8px 12px;font-size:12px;font-weight:650;cursor:pointer}.engine-back-button:hover{border-color:var(--ink);color:var(--ink)}
.engine-home-grid{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(320px,.7fr);gap:72px;max-width:1180px;margin:42px auto 0;padding:0 28px}.engine-home-main{min-height:500px;padding:34px 0 0}
.engine-eyebrow,.engine-section-label{display:block;color:var(--muted);font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase}.engine-home-main h2,.engine-setup-intro h2,.engine-review-main h2{max-width:680px;margin:16px 0 12px;color:var(--ink);font-size:34px;font-weight:700;letter-spacing:-.04em;line-height:1.18}.engine-home-lede{max-width:540px;margin:0;color:var(--muted);font-size:14px;line-height:1.8}
.engine-primary-action{display:inline-flex;align-items:center;gap:8px;margin-top:30px;border:0;border-radius:9px;background:var(--brand);color:#fff;padding:12px 18px;font-size:13px;font-weight:700;cursor:pointer;transition:transform .16s ease,opacity .16s ease}.engine-primary-action:hover{transform:translateY(-1px);opacity:.9}.engine-primary-action.compact{margin-top:0;padding:9px 13px;font-size:12px}
.engine-next-action{display:grid;gap:8px;max-width:600px;margin-top:72px;padding-top:18px;border-top:1px solid var(--border-subtle)}.engine-next-action strong{color:var(--ink);font-size:16px;font-weight:650}.engine-next-action button,.engine-text-action{display:inline-flex;align-items:center;gap:4px;width:max-content;padding:0;border:0;background:transparent;color:var(--brand);font-size:12px;font-weight:650;cursor:pointer}.engine-empty-action{opacity:.82}
.engine-home-aside{display:grid;align-content:start;gap:22px;padding-top:20px}.engine-activity-panel,.engine-review-panel{padding:18px 0;border-top:1px solid var(--border-subtle)}.engine-panel-head{display:flex;align-items:flex-start;justify-content:space-between;gap:14px}.engine-panel-head h3{margin:7px 0 0;color:var(--ink);font-size:17px;font-weight:650}.engine-panel-count{color:var(--muted);font-size:13px;font-variant-numeric:tabular-nums}.engine-activity-list{display:grid;margin-top:13px}
.engine-activity-row{display:grid;grid-template-columns:8px minmax(0,1fr) auto;align-items:center;gap:10px;padding:12px 0;border:0;border-top:1px solid var(--border-subtle);background:transparent;text-align:left;cursor:pointer}.engine-activity-row:hover .engine-activity-copy strong{color:var(--brand)}.engine-status-dot{width:7px;height:7px;border-radius:50%;background:var(--muted)}.engine-status-dot.completed{background:var(--brand)}.engine-status-dot.active{background:var(--warning,#c58b28)}.engine-status-dot.failed{background:var(--danger,#c64b4b)}.engine-status-dot.cancelled{background:var(--muted)}.engine-activity-copy{display:grid;gap:4px;min-width:0}.engine-activity-copy strong{overflow:hidden;color:var(--ink);font-size:13px;font-weight:650;text-overflow:ellipsis;white-space:nowrap}.engine-activity-copy small{overflow:hidden;color:var(--muted);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.engine-activity-state{display:inline-flex;align-items:center;gap:3px;color:var(--muted);font-size:11px;white-space:nowrap}.engine-empty-copy{margin:16px 0 0;color:var(--muted);font-size:12px;line-height:1.7}
.engine-review-panel>p{margin:16px 0 0;color:var(--copy);font-size:13px;line-height:1.8}.engine-review-next{display:grid;gap:5px;margin-top:16px;padding-top:12px;border-top:1px solid var(--border-subtle)}.engine-review-next span{color:var(--muted);font-size:11px}.engine-review-next strong{color:var(--ink);font-size:12px;font-weight:650;line-height:1.6}.engine-growth-line{display:flex;align-items:center;justify-content:space-between;gap:12px;padding-top:5px;color:var(--muted);font-size:11px}.engine-growth-line strong{color:var(--copy);font-size:11px;font-weight:650}
/* 复盘时间线主页：保持桌面工作区的层级和呼吸感，不用卡片堆叠。 */
.bar-title.hero-title{font-size:18px;font-weight:650;letter-spacing:-.015em}
.engine-home-shell{max-width:1180px;margin:30px auto 0;padding:0 28px 48px}
.engine-home-topline{display:flex;align-items:flex-end;justify-content:space-between;gap:28px;padding:0 0 22px;border-bottom:1px solid var(--border-subtle)}
.engine-home-focus{margin:8px 0 0;color:var(--copy);font-size:14px;line-height:1.5}
.engine-home-topline>.engine-text-action{margin-bottom:2px;padding:8px 0;font-size:13px}
.engine-home-layout{display:grid;grid-template-columns:minmax(0,1fr) 286px;gap:62px;margin-top:38px}
.engine-timeline-panel{min-width:0}
.engine-timeline-head{padding-bottom:15px;border-bottom:1px solid var(--border-subtle)}
.engine-panel-head h2{margin:7px 0 0;color:var(--ink);font-size:24px;font-weight:650;letter-spacing:-.03em}
.engine-panel-head h3{margin:7px 0 0;color:var(--ink);font-size:16px;font-weight:650;letter-spacing:-.02em}
.engine-timeline-list{position:relative;margin-top:0}
.engine-timeline-list::before{position:absolute;top:23px;bottom:23px;left:18px;width:1px;background:var(--border-subtle);content:''}
.engine-timeline-row{position:relative;display:grid;grid-template-columns:38px minmax(0,1fr) auto;align-items:start;gap:16px;width:100%;padding:22px 0;border:0;border-bottom:1px solid var(--border-subtle);background:transparent;text-align:left;cursor:pointer}
.engine-timeline-row:hover .engine-timeline-title,.engine-timeline-row:hover .engine-timeline-action{color:var(--brand)}
.engine-timeline-marker{z-index:1;display:grid;width:36px;height:36px;place-items:center;border:1px solid var(--border-subtle);border-radius:50%;background:var(--canvas);color:var(--muted);font-size:15px}
.engine-timeline-marker.completed{border-color:color-mix(in srgb,var(--brand) 35%,var(--border-subtle));background:var(--brand-soft);color:var(--brand)}
.engine-timeline-marker.active{border-color:var(--warning,#b9812d);color:var(--warning,#b9812d)}
.engine-timeline-copy{display:grid;gap:5px;min-width:0;padding-top:1px}
.engine-timeline-meta{display:flex;align-items:center;gap:9px;color:var(--muted);font-size:11px}
.engine-timeline-meta strong{color:var(--brand);font-size:11px;font-weight:700}
.engine-timeline-meta small{font-size:11px}
.engine-timeline-title{overflow:hidden;color:var(--ink);font-size:15px;font-weight:650;text-overflow:ellipsis;white-space:nowrap}
.engine-timeline-subtitle{overflow:hidden;color:var(--muted);font-size:12px;text-overflow:ellipsis;white-space:nowrap}
.engine-timeline-issue{overflow:hidden;color:var(--copy);font-size:12px;line-height:1.55;text-overflow:ellipsis;white-space:nowrap}
.engine-timeline-action{display:inline-flex;align-items:center;gap:4px;padding-top:8px;color:var(--muted);font-size:11px;white-space:nowrap}
.engine-timeline-empty{display:grid;justify-items:start;gap:8px;padding:66px 0 52px;border-bottom:1px solid var(--border-subtle)}
.engine-empty-symbol{display:grid;width:38px;height:38px;place-items:center;border:1px solid var(--border-subtle);border-radius:50%;color:var(--muted);font-size:19px}
.engine-timeline-empty strong{color:var(--ink);font-size:15px;font-weight:650}.engine-timeline-empty p{max-width:430px;margin:0;color:var(--muted);font-size:12px;line-height:1.7}
.engine-timeline-empty .engine-primary-action{margin-top:10px}
.engine-insight-rail{display:grid;align-content:start;gap:36px;border-left:1px solid var(--border-subtle);padding-left:30px}
.engine-insight-section{min-width:0}.engine-insight-caption{color:var(--muted);font-size:11px}
.engine-trend-widget{margin-top:18px}.engine-trend-tabs{display:flex;flex-wrap:wrap;gap:5px;margin-bottom:19px}.engine-trend-tabs button{padding:4px 7px;border:0;border-radius:5px;background:transparent;color:var(--muted);font-size:10px;cursor:pointer}.engine-trend-tabs button.active{background:var(--bg-selected,#f0f2ee);color:var(--ink);font-weight:650}
.engine-trend-chart{display:flex;align-items:flex-end;gap:10px;height:118px;padding:0 8px 0 2px;border-bottom:1px solid var(--border-subtle)}
.engine-trend-bar{position:relative;display:flex;flex:1;align-items:flex-start;justify-content:center;min-height:4px;border-radius:4px 4px 0 0;background:linear-gradient(180deg,var(--brand),color-mix(in srgb,var(--brand) 40%,var(--canvas)));opacity:.88;transition:height .24s ease}
.engine-trend-bar em{position:absolute;top:-17px;color:var(--muted);font-size:9px;font-style:normal;font-variant-numeric:tabular-nums}.engine-trend-scale{display:flex;justify-content:space-between;gap:8px;margin-top:9px;color:var(--muted);font-size:10px}.engine-trend-scale strong{color:var(--copy);font-weight:600}
.engine-focus-list{display:grid;gap:13px;margin:18px 0 0;padding:0;list-style:none}.engine-focus-list li{display:grid;grid-template-columns:7px minmax(0,1fr);gap:9px;align-items:start;color:var(--copy);font-size:12px;line-height:1.65}.engine-focus-dot{width:6px;height:6px;margin-top:6px;border-radius:50%;background:var(--brand)}
.engine-practice-strip{margin-top:44px;padding-top:24px;border-top:1px solid var(--border-subtle)}.engine-practice-strip-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.engine-practice-strip-head h3{margin:7px 0 0;color:var(--ink);font-size:16px;font-weight:650}.engine-practice-strip-head>span{color:var(--muted);font-size:11px}
.engine-practice-links{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));margin-top:17px;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}
.engine-practice-link{display:grid;grid-template-columns:30px minmax(0,1fr) 16px;align-items:center;gap:12px;min-width:0;padding:17px 16px;border:0;border-right:1px solid var(--border-subtle);background:transparent;text-align:left;cursor:pointer}.engine-practice-link:last-child{border-right:0}.engine-practice-link:hover{background:var(--bg-hover)}
.engine-practice-icon{display:grid;width:28px;height:28px;place-items:center;border:1px solid var(--border-subtle);border-radius:8px;color:var(--brand);font-size:15px}.engine-practice-link strong{display:block;color:var(--ink);font-size:13px;font-weight:650}.engine-practice-link small{display:block;margin-top:4px;overflow:hidden;color:var(--muted);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.engine-practice-arrow{color:var(--muted);font-size:13px}
/* 目标图像素基线：大留白、左宽右窄、无卡片包裹。 */
.interview-command-bar-home{display:none}
.engine-home-shell{max-width:none;margin:28px 0 0;padding:0 0 48px}
.engine-home-topline{align-items:center;padding:0 0 20px;border-bottom:0}
.engine-home-identity{display:flex;align-items:center;gap:40px}.engine-home-focus{margin:0;color:var(--copy);font-size:13px}.engine-home-focus strong{color:var(--ink);font-weight:650}
.engine-home-utilities{display:flex;align-items:center;gap:22px}.engine-home-utilities>button{display:inline-flex;align-items:center;gap:6px;padding:4px 0;border:0;background:transparent;color:var(--copy);font-size:12px;cursor:pointer}.engine-home-utilities>button:disabled{color:var(--muted);cursor:not-allowed}.engine-home-utilities .engine-text-action{margin-left:10px;color:var(--brand);font-size:12px}
.engine-home-layout{grid-template-columns:minmax(0,1fr) 390px;gap:44px;margin-top:28px}
.engine-timeline-head{padding-bottom:16px;border-bottom:0}.engine-timeline-head h2{font-size:22px}.engine-timeline-head .engine-section-label{display:none}
.engine-timeline-list{padding-left:0}.engine-timeline-list::before{top:27px;bottom:28px;left:17px;background:#d9dcd9}
.engine-timeline-row{grid-template-columns:38px 56px 220px minmax(105px,1fr) 112px 22px;grid-template-rows:auto auto;column-gap:10px;row-gap:7px;padding:27px 0;border-bottom:0}
.engine-timeline-node{grid-column:1;grid-row:1;z-index:2;width:12px;height:12px;margin:13px 0 0 11px;border:2px solid var(--ink);border-radius:50%;background:var(--canvas)}
.engine-timeline-marker{grid-column:2;grid-row:1;width:54px;height:54px;border-color:#d8ddd9;background:var(--canvas);color:var(--brand);font-size:20px}.engine-timeline-marker.mode-KNOWLEDGE_TRAINING{color:#3158d4}.engine-timeline-marker.mode-EXPERIENCE_SIMULATION{color:#7c3aed}
.engine-timeline-copy{grid-column:3;grid-row:1;gap:4px}.engine-timeline-meta{gap:10px}.engine-timeline-meta strong{color:var(--brand)}.engine-timeline-meta strong.mode-KNOWLEDGE_TRAINING{color:#3158d4}.engine-timeline-meta strong.mode-EXPERIENCE_SIMULATION{color:#7c3aed}.engine-timeline-title{font-size:14px}.engine-timeline-subtitle{font-size:12px}
.engine-timeline-facts{grid-column:4;grid-row:1;display:grid;gap:5px;padding-top:2px}.engine-timeline-facts strong{color:var(--ink);font-size:13px;font-weight:500}.engine-timeline-facts small,.engine-timeline-review small{color:var(--muted);font-size:11px}
.engine-timeline-review{grid-column:5;grid-row:1;display:grid;gap:5px;padding-top:2px}.engine-timeline-review strong{color:var(--brand);font-size:13px;font-weight:600}.engine-timeline-action{grid-column:6;grid-row:1;padding-top:18px;color:var(--ink);font-size:16px}.engine-timeline-issue{grid-column:3 / 6;grid-row:2;overflow:hidden;color:var(--muted);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.engine-timeline-row:hover .engine-timeline-action{color:var(--brand)}
.engine-insight-rail{gap:46px;border-left:1px solid var(--border-subtle);padding:14px 0 0 38px}.engine-insight-section .engine-panel-head h3{margin:0;font-size:18px}.engine-insight-section .engine-panel-head h3 small{color:var(--muted);font-size:13px;font-weight:500}.engine-trend-tabs{justify-content:space-between;gap:0;margin:22px 0 18px;border-bottom:1px solid var(--border-subtle)}.engine-trend-tabs button{position:relative;padding:0 5px 10px;border-radius:0;font-size:12px}.engine-trend-tabs button.active{background:transparent;color:var(--brand)}.engine-trend-tabs button.active::after{position:absolute;right:0;bottom:-1px;left:0;height:3px;background:var(--brand);content:''}
.engine-trend-widget{position:relative}.engine-trend-chart{position:relative;height:176px;padding:0;border:0;overflow:visible}.engine-trend-y-labels{position:absolute;top:0;bottom:18px;left:0;display:flex;flex-direction:column;justify-content:space-between;color:var(--muted);font-size:11px}.engine-trend-chart svg{position:absolute;top:0;right:0;bottom:0;left:41px;width:calc(100% - 41px);height:176px;overflow:visible}.engine-trend-grid-line{stroke:#e3e7e3;stroke-width:1;stroke-dasharray:3 3}.engine-trend-line{fill:none;stroke:var(--brand);stroke-width:2.5;vector-effect:non-scaling-stroke}.engine-trend-point{fill:var(--brand);stroke:var(--canvas);stroke-width:2}.engine-trend-scale{margin:2px 0 0 42px;gap:0;color:var(--muted);font-size:10px}.engine-trend-scale span{flex:1;text-align:center}.engine-trend-scale span:first-child{text-align:left}.engine-trend-scale span:last-child{text-align:right}
.engine-trend-empty-note{position:absolute;z-index:1;top:76px;right:0;left:42px;margin:0;text-align:center;color:var(--muted);font-size:11px;line-height:1.5}
.engine-focus-list{gap:12px;margin-top:20px}.engine-focus-list li{font-size:12px}.engine-focus-action{display:inline-flex;align-items:center;gap:5px;margin-top:18px;padding:0;border:0;background:transparent;color:var(--brand);font-size:12px;cursor:pointer}
.engine-practice-strip{margin-top:26px;padding-top:27px}.engine-practice-strip-head{align-items:center}.engine-practice-strip-head .engine-section-label{display:none}.engine-practice-strip-head h3{font-size:18px}.engine-practice-strip-head>span{display:none}.engine-practice-links{margin-top:19px;border-top:0;border-bottom:0}.engine-practice-link{grid-template-columns:42px minmax(0,1fr) 16px;gap:14px;padding:14px 15px;border-right:1px solid var(--border-subtle)}.engine-practice-icon{width:34px;height:34px;border:0;border-radius:0;background:transparent;color:var(--brand);font-size:28px}.engine-practice-link:nth-child(2) .engine-practice-icon{color:#3158d4}.engine-practice-link:nth-child(3) .engine-practice-icon{color:#7c3aed}.engine-practice-link strong{font-size:14px}.engine-practice-link small{font-size:12px}
.engine-setup-shell{max-width:1240px;margin:24px auto 0;padding:0 34px 40px}.engine-setup-intro{margin-bottom:28px}.engine-setup-intro h2{margin-top:12px;font-size:30px}.engine-setup-intro p{max-width:600px;margin:0;color:var(--muted);font-size:13px;line-height:1.8}.engine-setup-shell :deep(.mode-picker){grid-template-columns:repeat(3,minmax(0,1fr));gap:0;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}.engine-setup-shell :deep(.mode-card){display:flex;align-items:baseline;gap:10px;min-height:0;padding:15px 14px;border:0;border-radius:0;background:transparent;box-shadow:none;transform:none}.engine-setup-shell :deep(.mode-card + .mode-card){border-left:1px solid var(--border-subtle)}.engine-setup-shell :deep(.mode-card:hover){background:var(--bg-hover);box-shadow:none}.engine-setup-shell :deep(.mode-card.selected){background:var(--bg-selected);box-shadow:inset 0 -2px 0 var(--brand)}.engine-setup-shell :deep(.mode-step){font-size:10px}.engine-setup-shell :deep(.mode-label){font-size:13px}.engine-setup-shell :deep(.mode-desc){display:none}.engine-setup-shell :deep(.composer-body){margin-top:0;border:0;border-top:0;border-radius:0;background:transparent;padding:0}.engine-setup-shell :deep(.composer-start){border-radius:0;background:#111}
.interview-page[data-engine-state='setup']{padding-top:18px}.interview-page[data-engine-state='setup'] .interview-command-bar{min-height:36px;padding:0 4px;border-bottom:0;margin-bottom:0}.interview-page[data-engine-state='setup'] .engine-setup-shell{margin-top:12px;padding-bottom:24px}.interview-page[data-engine-state='setup'] .composer-progress{padding-top:0}.interview-page[data-engine-state='setup'] .composer-body{border-top:0;padding-top:16px}
.interview-page[data-engine-state='setup'] :deep(.composer-body){position:relative}.interview-page[data-engine-state='setup'] :deep(.composer-footer){position:absolute;right:0;bottom:-20px;display:block;width:min(320px,40%);border-top:0;padding:0}.interview-page[data-engine-state='setup'] :deep(.composer-footer)::after{display:block;margin-top:9px;color:var(--muted);font-size:9.5px;content:'在本地处理，资料不上传设备'}.interview-page[data-engine-state='setup'] :deep(.composer-footer-copy){display:none}.interview-page[data-engine-state='setup'] :deep(.composer-start){width:100%;min-width:0;border-radius:0}
.engine-review-shell{display:grid;grid-template-columns:minmax(0,1fr) 220px;gap:54px;max-width:1080px;margin:42px auto 0;padding:0 28px 48px}.engine-review-main{min-width:0}.engine-review-main h2{font-size:30px}.engine-review-context{margin:0;color:var(--muted);font-size:12px}.engine-review-conclusion{display:grid;gap:8px;margin-top:40px;padding:20px 0;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}.engine-review-conclusion strong{color:var(--ink);font-size:18px;font-weight:700;line-height:1.45}.engine-review-conclusion p{margin:0;color:var(--copy);font-size:13px;line-height:1.8}.engine-review-next-step{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin:26px 0 36px}.engine-review-next-step>div{display:grid;gap:8px}.engine-review-next-step strong{max-width:600px;color:var(--ink);font-size:14px;font-weight:650;line-height:1.7}.engine-review-aside{display:grid;align-content:start;gap:18px;padding-top:38px}.engine-review-meta{display:grid;gap:6px;padding-bottom:13px;border-bottom:1px solid var(--border-subtle)}.engine-review-meta span{color:var(--muted);font-size:11px}.engine-review-meta strong{color:var(--ink);font-size:13px;font-weight:650;line-height:1.5}
@media (max-width:900px){.engine-home-grid,.engine-review-shell{grid-template-columns:1fr;gap:28px;margin-top:24px}.engine-home-main{min-height:0;padding-top:12px}.engine-home-aside{padding-top:0}.engine-review-aside{padding-top:0}.engine-setup-shell{margin-top:24px}.engine-setup-shell :deep(.mode-picker){grid-template-columns:1fr}.engine-setup-shell :deep(.mode-card + .mode-card){border-left:0;border-top:1px solid var(--border-subtle)}.engine-home-layout{grid-template-columns:1fr;gap:34px}.engine-insight-rail{border-left:0;border-top:1px solid var(--border-subtle);padding:28px 0 0}.engine-practice-links{grid-template-columns:1fr}.engine-practice-link{border-right:0;border-bottom:1px solid var(--border-subtle)}.engine-practice-link:last-child{border-bottom:0}.engine-home-shell{margin-top:24px;padding-inline:18px}}
@media (max-width:620px){.engine-home-topline,.engine-practice-strip-head{align-items:flex-start;flex-direction:column;gap:14px}.engine-home-identity{align-items:flex-start;flex-direction:column;gap:8px}.engine-timeline-row{grid-template-columns:34px minmax(0,1fr)}.engine-timeline-action{grid-column:2;padding-top:0}.engine-timeline-marker{width:32px;height:32px}.engine-timeline-list::before{left:16px}.engine-timeline-title{white-space:normal}.engine-practice-strip{margin-top:32px}}

@media (max-width:720px){.interview-page[data-engine-state='setup'] :deep(.composer-footer){position:static;display:grid;width:100%;padding-top:16px;border-top:1px solid var(--border-subtle)}.interview-page[data-engine-state='setup'] :deep(.composer-footer-copy){display:grid}}
.interview-page[data-engine-state='setup'] :deep(.composer-start:disabled){background:#111;color:rgba(255,255,255,.58);opacity:1;cursor:not-allowed}

/* 配置页使用整页桌面工作区：页面本身不滚动，资料/简历列在窄窗口内独立收敛。 */
.interview-page[data-engine-state='setup'] {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 18px 8px;
}
.interview-page[data-engine-state='setup'] .interview-command-bar {
  flex: 0 0 auto;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  height: auto;
  max-width: none;
  margin: 12px 0 0;
  padding: 0 34px;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.interview-composer) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-progress) {
  flex: 0 0 auto;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-setup),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.role-setup),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-setup) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-workspace),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.role-layout),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-workspace) {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-source-center),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-inspector),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.role-column),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-source-rail),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-source-center),
.interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-inspector) {
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
}
@media (max-height: 850px) and (min-width: 901px) {
  .interview-page[data-engine-state='setup'] {
    padding-top: 8px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell {
    margin-top: 8px;
    padding-inline: 22px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-progress-node) {
    width: 20px;
    height: 20px;
    font-size: 10px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-progress-step) {
    gap: 3px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-progress-step strong) {
    font-size: 11px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-progress-step small) {
    font-size: 9px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.composer-body) {
    box-sizing: border-box;
    padding-bottom: 78px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-source-center),
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.role-column),
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-source-center) {
    padding-top: 14px;
    padding-bottom: 12px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.knowledge-inspector),
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.experience-inspector) {
    padding-top: 14px;
    padding-bottom: 12px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.document-row),
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.question-set-row) {
    padding-top: 7px;
    padding-bottom: 7px;
  }
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.persona-add),
  .interview-page[data-engine-state='setup'] .engine-setup-shell :deep(.persona-card) {
    padding-top: 7px;
    padding-bottom: 7px;
  }
}

/* 面试主页是固定的桌面工作区：只让内容在可用高度内收敛，不让整个应用出现页面滚动。 */
.interview-page[data-engine-state='home'] {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 14px 18px 8px;
}
@media (min-height: 900px) {
  .interview-page[data-engine-state='home'] {
    height: calc(100vh - 32px);
  }
}
@media (max-height: 899px) {
  .interview-page[data-engine-state='home'] {
    height: calc(100vh - 24px);
  }
}
.interview-page[data-engine-state='home'] .engine-home-shell {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 14px;
  padding-bottom: 0;
  overflow: hidden;
}
.interview-page[data-engine-state='home'] .engine-home-topline,
.interview-page[data-engine-state='home'] .engine-practice-strip {
  flex-shrink: 0;
}
.interview-page[data-engine-state='home'] .engine-home-layout {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}
.interview-page[data-engine-state='home'] .engine-timeline-panel,
.interview-page[data-engine-state='home'] .engine-insight-rail,
.interview-page[data-engine-state='home'] .engine-timeline-list {
  min-height: 0;
  overflow: hidden;
}

/* 低高度窗口仍保持“一页完成”：减少留白，不改变信息顺序和桌面布局。 */
@media (max-height: 850px) and (min-width: 901px) {
  .interview-page[data-engine-state='home'] { padding-top: 10px; padding-bottom: 4px; }
  .interview-page[data-engine-state='home'] .engine-home-shell { margin-top: 8px; }
  .interview-page[data-engine-state='home'] .engine-home-topline { padding-bottom: 10px; }
  .interview-page[data-engine-state='home'] .engine-home-layout { margin-top: 12px; gap: 32px; }
  .interview-page[data-engine-state='home'] .engine-timeline-head { padding-bottom: 7px; }
  .interview-page[data-engine-state='home'] .engine-timeline-head h2 { font-size: 20px; }
  .interview-page[data-engine-state='home'] .engine-timeline-row { padding-top: 13px; padding-bottom: 13px; row-gap: 4px; }
  .interview-page[data-engine-state='home'] .engine-timeline-marker { width: 46px; height: 46px; }
  .interview-page[data-engine-state='home'] .engine-timeline-node { margin-top: 10px; }
  .interview-page[data-engine-state='home'] .engine-timeline-issue { font-size: 11px; }
  .interview-page[data-engine-state='home'] .engine-insight-rail { gap: 24px; padding-top: 8px; padding-left: 28px; }
  .interview-page[data-engine-state='home'] .engine-trend-tabs { margin-top: 14px; margin-bottom: 11px; }
  .interview-page[data-engine-state='home'] .engine-trend-chart,
  .interview-page[data-engine-state='home'] .engine-trend-chart svg { height: 134px; }
  .interview-page[data-engine-state='home'] .engine-trend-empty-note { top: 54px; }
  .interview-page[data-engine-state='home'] .engine-focus-list { margin-top: 12px; gap: 8px; }
  .interview-page[data-engine-state='home'] .engine-focus-action { margin-top: 10px; }
  .interview-page[data-engine-state='home'] .engine-practice-strip { margin-top: 12px; padding-top: 12px; }
  .interview-page[data-engine-state='home'] .engine-practice-links { margin-top: 8px; }
  .interview-page[data-engine-state='home'] .engine-practice-link { padding: 8px 12px; }
  .interview-page[data-engine-state='home'] .engine-practice-icon { width: 28px; height: 28px; font-size: 23px; }
  .interview-page[data-engine-state='home'] .engine-practice-link small { margin-top: 2px; font-size: 11px; }
}

@media (max-width: 900px) {
  .interview-page[data-engine-state='home'] {
    height: auto;
    min-height: 100%;
    overflow: visible;
  }
  .interview-page[data-engine-state='home'] .engine-home-shell {
    overflow: visible;
  }
  .interview-page[data-engine-state='home'] .engine-home-layout,
  .interview-page[data-engine-state='home'] .engine-timeline-list,
  .interview-page[data-engine-state='home'] .engine-timeline-panel,
  .interview-page[data-engine-state='home'] .engine-insight-rail {
    overflow: visible;
  }
}

/* 近期记录与练习入口：复刻目标图的横向信息结构，开始练习属于左侧主工作区。 */
.interview-page[data-engine-state='home'] .engine-home-shell {
  margin-top: 20px;
}
.interview-page[data-engine-state='home'] .engine-home-identity {
  align-items: flex-start;
  flex-direction: column;
  gap: 9px;
}
.interview-page[data-engine-state='home'] .engine-home-focus {
  font-size: 14px;
}
.interview-page[data-engine-state='home'] .engine-home-layout {
  grid-template-columns: minmax(0, 1fr) 354px;
  gap: 34px;
  margin-top: 26px;
}
.interview-page[data-engine-state='home'] .engine-timeline-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
}
.interview-page[data-engine-state='home'] .engine-timeline-head {
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}
.interview-page[data-engine-state='home'] .engine-timeline-head h2 {
  font-size: 20px;
}
.engine-view-all {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: var(--copy);
  font-size: 11px;
  cursor: pointer;
}
.engine-view-all:hover {
  color: var(--brand);
}
.interview-page[data-engine-state='home'] .engine-timeline-list {
  flex: 0 1 auto;
  margin-top: 0;
}
.interview-page[data-engine-state='home'] .engine-timeline-list::before,
.interview-page[data-engine-state='home'] .engine-timeline-node {
  display: none;
}
.interview-page[data-engine-state='home'] .engine-timeline-row {
  grid-template-columns: 48px 78px minmax(120px, 1.35fr) 78px 88px minmax(120px, 1fr) 18px;
  grid-template-rows: 1fr;
  align-items: center;
  gap: 8px;
  padding: 15px 0;
  border-bottom: 1px solid var(--border-subtle);
}
.interview-page[data-engine-state='home'] .engine-timeline-marker {
  grid-column: 1;
  grid-row: 1;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: var(--surface-solid, #fff);
  font-size: 20px;
}
.interview-page[data-engine-state='home'] .engine-timeline-marker.mode-ROLE_BASED {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--border-subtle));
  background: color-mix(in srgb, var(--brand) 7%, var(--canvas));
  color: var(--brand);
}
.interview-page[data-engine-state='home'] .engine-timeline-marker.mode-KNOWLEDGE_TRAINING {
  border-color: rgba(124, 58, 237, .22);
  background: rgba(124, 58, 237, .06);
  color: #7c3aed;
}
.interview-page[data-engine-state='home'] .engine-timeline-marker.mode-EXPERIENCE_SIMULATION {
  border-color: rgba(49, 88, 212, .22);
  background: rgba(49, 88, 212, .06);
  color: #3158d4;
}
.interview-page[data-engine-state='home'] .engine-timeline-kind,
.interview-page[data-engine-state='home'] .engine-timeline-facts,
.interview-page[data-engine-state='home'] .engine-timeline-review,
.interview-page[data-engine-state='home'] .engine-timeline-issue {
  display: grid;
  min-width: 0;
  gap: 4px;
}
.interview-page[data-engine-state='home'] .engine-timeline-kind {
  grid-column: 2;
  grid-row: 1;
}
.engine-timeline-kind strong {
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.engine-timeline-kind strong.mode-ROLE_BASED { color: var(--brand); }
.engine-timeline-kind strong.mode-KNOWLEDGE_TRAINING { color: #7c3aed; }
.engine-timeline-kind strong.mode-EXPERIENCE_SIMULATION { color: #3158d4; }
.engine-timeline-kind small,
.engine-timeline-facts small,
.engine-timeline-review small,
.engine-timeline-issue small {
  color: var(--muted);
  font-size: 10px;
  white-space: nowrap;
}
.interview-page[data-engine-state='home'] .engine-timeline-copy {
  grid-column: 3;
  grid-row: 1;
  gap: 4px;
  min-width: 0;
}
.interview-page[data-engine-state='home'] .engine-timeline-title {
  font-size: 13px;
}
.interview-page[data-engine-state='home'] .engine-timeline-subtitle {
  font-size: 10px;
}
.engine-record-binding {
  width: fit-content;
  max-width: 100%;
  overflow: hidden;
  padding: 3px 6px;
  border-radius: 4px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.interview-page[data-engine-state='home'] .engine-timeline-facts {
  grid-column: 4;
  grid-row: 1;
  padding-top: 0;
}
.interview-page[data-engine-state='home'] .engine-timeline-facts strong,
.interview-page[data-engine-state='home'] .engine-timeline-review strong,
.interview-page[data-engine-state='home'] .engine-timeline-issue strong {
  overflow: hidden;
  color: var(--ink);
  font-size: 11px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.interview-page[data-engine-state='home'] .engine-timeline-review {
  grid-column: 5;
  grid-row: 1;
  padding-top: 0;
}
.interview-page[data-engine-state='home'] .engine-timeline-review strong {
  color: var(--brand);
}
.interview-page[data-engine-state='home'] .engine-timeline-issue {
  grid-column: 6;
  grid-row: 1;
  padding-top: 0;
}
.interview-page[data-engine-state='home'] .engine-timeline-action {
  grid-column: 7;
  grid-row: 1;
  padding-top: 0;
  justify-self: end;
  color: var(--ink);
}

.interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
  flex-shrink: 0;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}
.interview-page[data-engine-state='home'] .engine-practice-strip-head {
  align-items: center;
}
.interview-page[data-engine-state='home'] .engine-practice-strip-head h3 {
  margin-top: 0;
  font-size: 17px;
}
.interview-page[data-engine-state='home'] .engine-practice-links {
  margin-top: 12px;
}
.interview-page[data-engine-state='home'] .engine-practice-link {
  grid-template-columns: 30px minmax(0, 1fr) 14px;
  gap: 9px;
  padding: 10px 8px;
}
.interview-page[data-engine-state='home'] .engine-practice-icon {
  width: 28px;
  height: 28px;
  font-size: 23px;
}
.interview-page[data-engine-state='home'] .engine-practice-link strong {
  font-size: 12px;
}
.interview-page[data-engine-state='home'] .engine-practice-link small {
  margin-top: 2px;
  font-size: 10px;
}

/* 右侧能力变化 + 便签：提供切换，但不制造假趋势。 */
.interview-page[data-engine-state='home'] .engine-insight-rail {
  gap: 24px;
  padding-top: 0;
  padding-left: 28px;
}
.engine-growth-head {
  align-items: center;
}
.engine-growth-head > div:first-child {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.engine-growth-head > div:first-child > small {
  color: var(--muted);
  font-size: 10px;
}
.engine-growth-switch {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
}
.engine-growth-switch button {
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--muted);
  padding: 3px 6px;
  font-size: 10px;
  cursor: pointer;
}
.engine-growth-switch button.active {
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 700;
}
.interview-page[data-engine-state='home'] .engine-trend-widget {
  margin-top: 14px;
}
.interview-page[data-engine-state='home'] .engine-trend-tabs {
  margin-top: 0;
}
.engine-radar-chart {
  position: relative;
  height: 208px;
}
.engine-radar-chart svg {
  width: 100%;
  height: 208px;
  overflow: visible;
}
.engine-radar-grid {
  fill: none;
  stroke: var(--border-subtle);
  stroke-width: 1;
}
.engine-radar-axis {
  stroke: var(--border-subtle);
  stroke-width: 1;
}
.engine-radar-fill {
  fill: color-mix(in srgb, var(--brand) 15%, transparent);
  stroke: var(--brand);
  stroke-width: 2;
}
.engine-radar-point {
  fill: var(--brand);
  stroke: var(--canvas);
  stroke-width: 2;
}
.engine-radar-label {
  fill: var(--copy);
  font-size: 10px;
}
.engine-radar-empty-note {
  position: absolute;
  z-index: 1;
  top: 86px;
  right: 28px;
  left: 28px;
  margin: 0;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.5;
  text-align: center;
}
.engine-note-section {
  display: grid;
  gap: 10px;
}
.engine-note {
  padding: 10px 12px;
  border-left: 3px solid color-mix(in srgb, var(--brand) 55%, var(--border-subtle));
  background: color-mix(in srgb, var(--brand) 4%, var(--canvas));
}
.engine-note:nth-child(2) {
  border-left-color: #3158d4;
  background: color-mix(in srgb, #3158d4 4%, var(--canvas));
}
.engine-note:nth-child(3) {
  border-left-color: #c58b28;
  background: color-mix(in srgb, #c58b28 5%, var(--canvas));
}
.engine-note-kicker {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.engine-note h3 {
  margin: 4px 0 0;
  color: var(--ink);
  font-size: 12px;
  font-weight: 700;
}
.engine-note p {
  display: -webkit-box;
  overflow: hidden;
  margin: 5px 0 0;
  color: var(--copy);
  font-size: 10px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.engine-note button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--brand);
  font-size: 10px;
  cursor: pointer;
}

@media (max-height: 850px) and (min-width: 901px) {
  .interview-page[data-engine-state='home'] .engine-home-layout {
    margin-top: 24px;
    gap: 28px;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-row {
    padding-top: 10px;
    padding-bottom: 10px;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-marker {
    width: 36px;
    height: 36px;
    font-size: 17px;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
    margin-top: 12px;
    padding-top: 12px;
  }
  .interview-page[data-engine-state='home'] .engine-radar-chart,
  .interview-page[data-engine-state='home'] .engine-radar-chart svg {
    height: 170px;
  }
}

@media (min-height: 900px) and (min-width: 901px) {
  .interview-page[data-engine-state='home'] .engine-home-layout {
    margin-top: 52px;
  }
}

@media (max-height: 650px) and (min-width: 901px) {
  .interview-page[data-engine-state='home'] .engine-home-layout {
    margin-top: 12px;
  }
  .interview-page[data-engine-state='home'] .engine-insight-rail {
    gap: 12px;
  }
  .interview-page[data-engine-state='home'] .engine-trend-tabs {
    margin-bottom: 8px;
  }
  .interview-page[data-engine-state='home'] .engine-trend-chart,
  .interview-page[data-engine-state='home'] .engine-trend-chart svg {
    height: 126px;
  }
  .interview-page[data-engine-state='home'] .engine-trend-empty-note {
    top: 48px;
  }
  .interview-page[data-engine-state='home'] .engine-trend-scale {
    margin-top: 0;
  }
  .interview-page[data-engine-state='home'] .engine-note-section {
    gap: 6px;
  }
  .interview-page[data-engine-state='home'] .engine-note {
    padding: 7px 10px;
  }
  .interview-page[data-engine-state='home'] .engine-note h3 {
    margin-top: 2px;
  }
  .interview-page[data-engine-state='home'] .engine-note p {
    margin-top: 3px;
    -webkit-line-clamp: 1;
  }
  .interview-page[data-engine-state='home'] .engine-note button {
    margin-top: 3px;
  }
  .interview-page[data-engine-state='home'] .engine-radar-chart,
  .interview-page[data-engine-state='home'] .engine-radar-chart svg {
    height: 132px;
  }
}

@media (max-width: 900px) {
  .interview-page[data-engine-state='home'] .engine-home-layout {
    grid-template-columns: 1fr;
    overflow: visible;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-panel {
    overflow: visible;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-row {
    grid-template-columns: 44px 76px minmax(0, 1fr) 74px 16px;
    grid-template-rows: auto auto;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-facts {
    grid-column: 4;
    grid-row: 1;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-review,
  .interview-page[data-engine-state='home'] .engine-timeline-issue {
    display: none;
  }
  .interview-page[data-engine-state='home'] .engine-timeline-action {
    grid-column: 5;
    grid-row: 1;
  }
  .interview-page[data-engine-state='home'] .engine-insight-rail {
    border-top: 1px solid var(--border-subtle);
    border-left: 0;
    padding: 24px 0 0;
  }
}

/* 当前专注与导出：让目标上下文可识别，回答导出不暴露原始 JSON。 */
.interview-page[data-engine-state='home'] .engine-focus-target {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-left: 3px;
}
.interview-page[data-engine-state='home'] .engine-focus-target-mark {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  background: var(--bg-subtle, #f4f5f3);
  font-size: 10px;
  font-weight: 750;
}
.interview-page[data-engine-state='home'] .engine-focus-target-mark img {
  width: 16px;
  height: 16px;
  object-fit: contain;
}
.interview-page[data-engine-state='home'] .engine-focus-target-mark > span {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
}
.interview-page[data-engine-state='home'] .engine-focus-target small {
  color: var(--muted);
  font-size: 10px;
}
.interview-page[data-engine-state='home'] .engine-export-control {
  position: relative;
}
.interview-page[data-engine-state='home'] .engine-export-history-control {
  position: relative;
}
.interview-page[data-engine-state='home'] .engine-export-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
  display: grid;
  gap: 6px;
  width: 220px;
  padding: 10px;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  background: var(--surface-solid, #fff);
  box-shadow: 0 12px 30px rgba(0, 0, 0, .12);
}
.interview-page[data-engine-state='home'] .engine-export-menu > span {
  color: var(--muted);
  font-size: 10px;
}
.interview-page[data-engine-state='home'] .engine-export-menu > button {
  display: grid;
  gap: 3px;
  padding: 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.interview-page[data-engine-state='home'] .engine-export-menu > button:hover {
  background: var(--bg-hover);
}
.interview-page[data-engine-state='home'] .engine-export-menu strong {
  color: var(--ink);
  font-size: 11px;
}
.interview-page[data-engine-state='home'] .engine-export-menu small {
  color: var(--muted);
  font-size: 9px;
}
.interview-page[data-engine-state='home'] .engine-home-utilities .engine-export-history-control > button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--copy);
  font-size: 12px;
  cursor: pointer;
}
.interview-page[data-engine-state='home'] .engine-export-history-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
  display: grid;
  gap: 8px;
  width: 252px;
  padding: 10px;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  background: var(--surface-solid, #fff);
  box-shadow: 0 12px 30px rgba(0, 0, 0, .12);
}
.interview-page[data-engine-state='home'] .engine-export-history-menu > span {
  color: var(--muted);
  font-size: 10px;
}
.interview-page[data-engine-state='home'] .engine-export-history-menu > p {
  margin: 0;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}
.interview-page[data-engine-state='home'] .engine-export-history-list {
  display: grid;
  gap: 7px;
  max-height: 190px;
  overflow-y: auto;
}
.interview-page[data-engine-state='home'] .engine-export-history-item {
  display: grid;
  gap: 3px;
  padding: 7px 8px;
  border-radius: 7px;
  background: var(--bg-subtle, #f4f5f3);
}
.interview-page[data-engine-state='home'] .engine-export-history-item strong {
  overflow: hidden;
  color: var(--ink);
  font-size: 11px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.interview-page[data-engine-state='home'] .engine-export-history-item small {
  color: var(--muted);
  font-size: 9px;
}
.interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
  margin-top: auto;
  margin-bottom: 36px;
  padding-bottom: 8px;
}
.interview-page[data-engine-state='home'] .engine-home-motto {
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1.4;
}
.interview-page[data-engine-state='home'] .engine-home-date-line {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.interview-page[data-engine-state='home'] .engine-timeline-list[data-expanded='true'] {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-width: thin;
}
.interview-page[data-engine-state='home'] .engine-radar-partial-note {
  margin: -4px 0 4px;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.5;
}
.interview-page[data-engine-state='home'] .engine-home-utilities .engine-export-control > button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: var(--copy);
  font-size: 12px;
  cursor: pointer;
}
.interview-page[data-engine-state='home'] .engine-home-utilities .engine-export-control > button:disabled {
  color: var(--muted);
  cursor: not-allowed;
}
@media (max-height: 850px) and (min-width: 901px) {
  .interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
    margin-top: auto;
    margin-bottom: 36px;
    padding-top: 12px;
    padding-bottom: 4px;
  }
}
@media (max-width: 900px) {
  .interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
    margin-top: 20px;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}
/* 开始练习：保持底部留白的同时，让三个模式入口更易点击、视觉更有分量。 */
@media (min-width: 901px) {
  .interview-page[data-engine-state='home'] .engine-timeline-panel > .engine-practice-strip {
    margin-bottom: 44px;
  }
  .interview-page[data-engine-state='home'] .engine-practice-link {
    grid-template-columns: 32px minmax(0, 1fr) 14px;
    gap: 10px;
    padding: 12px 10px;
  }
  .interview-page[data-engine-state='home'] .engine-practice-icon {
    width: 30px;
    height: 30px;
    font-size: 24px;
  }
  .interview-page[data-engine-state='home'] .engine-practice-link strong {
    font-size: 13px;
  }
  .interview-page[data-engine-state='home'] .engine-practice-link small {
    font-size: 11px;
  }
}
/* 复盘是独立的白色桌面工作区，不沿用旧版两列外壳。 */
.interview-page[data-engine-state='review'] .engine-review-shell {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  max-width: none;
  margin: 0;
  padding: 0 34px 24px;
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='review'] {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 18px 8px;
}
.interview-page[data-engine-state='review'] .interview-command-bar {
  display: none;
}
.interview-page[data-engine-state='review'] .engine-review-shell > [data-test='interview-review'] {
  flex: 1 1 auto;
  min-height: 0;
}

/* 面试房间：安静的白色桌面工作区，保留必要上下文，不使用聊天产品式的重装饰。 */
.interview-page[data-engine-state='room'] .interview-chat-layout {
  min-height: 0;
  height: calc(100vh - 128px);
  border: 1px solid var(--border-subtle, #e4e7e5);
  border-radius: 12px;
  background: var(--surface-solid, #fff);
  box-shadow: none;
}
.interview-page[data-engine-state='room'] .chat-sidebar {
  width: 172px;
  padding: 14px 12px;
  border-right: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .chat-main { background: var(--surface-solid, #fff); }
.interview-page[data-engine-state='room'] .chat-plan-header {
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 20px;
  padding: 18px 26px 14px;
  border-bottom: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .chat-plan-kicker {
  margin-bottom: 7px;
  color: var(--brand, #168866);
  font-size: 10px;
  letter-spacing: .08em;
}
.interview-page[data-engine-state='room'] .chat-plan-header strong { font-size: 17px; font-weight: 700; }
.interview-page[data-engine-state='room'] .chat-plan-header p { margin-top: 6px; color: var(--muted); font-size: 12px; }
.interview-page[data-engine-state='room'] .chat-plan-status { min-width: 170px; padding-top: 4px; }
.interview-page[data-engine-state='room'] .chat-plan-status strong { font-size: 14px; font-variant-numeric: tabular-nums; }
.interview-page[data-engine-state='room'] .chat-plan-status span { font-size: 11px; font-weight: 500; }
.interview-page[data-engine-state='room'] .chat-plan-steps { grid-column: 1 / -1; gap: 6px; margin-top: 2px; }
.interview-page[data-engine-state='room'] .chat-plan-steps span { padding: 3px 8px 3px 4px; border: 0; border-radius: 6px; background: var(--bg-surface, #f6f7f6); font-size: 11px; }
.interview-page[data-engine-state='room'] .chat-plan-steps span.current { background: var(--ink, #111); color: #fff; }
.interview-page[data-engine-state='room'] .chat-plan-steps span.done { background: var(--accent-soft, #eef7f1); color: var(--brand); }
.interview-page[data-engine-state='room'] .chat-plan-steps i { width: 17px; height: 17px; background: transparent; font-size: 10px; }
.interview-page[data-engine-state='room'] .chat-plan-steps span.current i { color: #fff; }
.interview-page[data-engine-state='room'] :deep(.chat-messages) { padding: 24px 32px; background: var(--surface-solid, #fff); }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble) { border-color: var(--border-subtle, #e4e7e5); border-radius: 10px; background: var(--bg-surface, #f7f8f7); }
.interview-page[data-engine-state='room'] :deep(.user-bubble) { border-radius: 10px; background: var(--ink, #111); }
.interview-page[data-engine-state='room'] :deep(.evaluation-inline),
.interview-page[data-engine-state='room'] :deep(.summary-inline) { border-color: var(--border-subtle, #e4e7e5); border-radius: 10px; background: var(--surface-solid, #fff); }
.interview-page[data-engine-state='room'] .chat-input-bar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
  padding: 12px 24px 18px;
  border-top: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .voice-row { display: flex; align-items: center; gap: 7px; min-height: 42px; }
.interview-page[data-engine-state='room'] .voice-button,
.interview-page[data-engine-state='room'] .question-read-button {
  display: inline-flex; align-items: center; justify-content: center; min-width: 36px; height: 36px;
  border: 1px solid var(--border-default, #d6dbd8); border-radius: 8px; background: var(--surface-solid, #fff); color: var(--ink); cursor: pointer;
}
.interview-page[data-engine-state='room'] .voice-button.listening { border-color: var(--brand); color: var(--brand); background: var(--accent-soft, #eef7f1); }
.interview-page[data-engine-state='room'] .voice-button:disabled,
.interview-page[data-engine-state='room'] .question-read-button:disabled { cursor: not-allowed; opacity: .45; }
.interview-page[data-engine-state='room'] .question-read-button { padding: 0 9px; font-size: 11px; white-space: nowrap; }
.interview-page[data-engine-state='room'] .voice-hint { color: var(--brand); font-size: 11px; white-space: nowrap; }
.interview-page[data-engine-state='room'] .chat-textarea :deep(.el-textarea__inner) { min-height: 42px !important; padding: 10px 12px; border: 1px solid var(--border-default, #d6dbd8); border-radius: 8px; box-shadow: none; background: var(--surface-solid, #fff); color: var(--ink); resize: none; }
.interview-page[data-engine-state='room'] .chat-textarea :deep(.el-textarea__inner):focus { border-color: var(--ink, #111); }
.interview-page[data-engine-state='room'] .chat-send-btn { min-width: 84px; height: 42px; border: 1px solid var(--ink, #111); border-radius: 8px; background: var(--ink, #111); color: #fff; font-size: 12px; font-weight: 650; cursor: pointer; }
.interview-page[data-engine-state='room'] .chat-send-btn:disabled { cursor: not-allowed; opacity: .4; }
.interview-page[data-engine-state='room'] .review-mode-bar { margin: 0 24px 16px; padding: 10px 12px; border-top: 1px solid var(--border-subtle); color: var(--muted); font-size: 11px; }
.interview-page[data-engine-state='room'] .chat-room-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 24px 14px; padding: 9px 12px; border-left: 2px solid var(--ink, #111); background: var(--bg-surface, #f7f8f7); color: var(--copy); font-size: 12px; }
.interview-page[data-engine-state='room'] .chat-room-error button { border: 0; background: transparent; color: var(--ink); font-size: 12px; font-weight: 700; cursor: pointer; }
@media (max-width: 760px) {
  .interview-page[data-engine-state='room'] .interview-chat-layout { height: calc(100vh - 92px); border-radius: 0; }
  .interview-page[data-engine-state='room'] .chat-sidebar { display: none; }
  .interview-page[data-engine-state='room'] .chat-plan-header { grid-template-columns: 1fr; padding-inline: 18px; }
  .interview-page[data-engine-state='room'] .chat-plan-status { align-items: flex-start; min-width: 0; }
  .interview-page[data-engine-state='room'] :deep(.chat-messages) { padding: 18px; }
  .interview-page[data-engine-state='room'] .chat-input-bar { grid-template-columns: 1fr auto; padding-inline: 14px; }
  .interview-page[data-engine-state='room'] .voice-row { grid-column: 1 / -1; }
}

/* Focus Studio room: one calm desktop surface with progress, answer, and a narrow context inspector. */
.interview-page[data-engine-state='room'] {
  padding: 8px 12px 12px;
  background: var(--canvas, #fff);
}
.interview-page[data-engine-state='room'] .interview-command-bar {
  min-height: 42px;
  margin: 0 0 10px;
  padding: 0 2px;
  border-bottom: 0;
}
.interview-page[data-engine-state='room'] .interview-command-bar .bar-identity { display: none; }
.interview-page[data-engine-state='room'] .engine-back-button {
  margin-left: 0;
  padding: 6px 2px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--ink, #171717);
  font-size: 13px;
  font-weight: 650;
}
.interview-page[data-engine-state='room'] .engine-back-button:hover { color: var(--brand, #168b68); }
.interview-page[data-engine-state='room'] .interview-chat-layout {
  display: grid;
  grid-template-columns: 172px minmax(0, 1fr) 224px;
  height: calc(100vh - 72px);
  min-height: 560px;
  border: 0;
  border-top: 1px solid var(--border-subtle, #e4e7e5);
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
}
.interview-page[data-engine-state='room'] .chat-sidebar {
  width: auto;
  padding: 18px 14px 16px;
  border-right: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .chat-main {
  min-width: 0;
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .chat-plan-header {
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 18px;
  padding: 18px 28px 16px;
  border-bottom: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .chat-plan-context {
  min-width: 0;
}
.interview-page[data-engine-state='room'] .chat-plan-kicker {
  margin-bottom: 6px;
  color: var(--muted, #989893);
  font-size: 9px;
  letter-spacing: .11em;
}
.interview-page[data-engine-state='room'] .chat-plan-header strong {
  color: var(--ink, #171717);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -.025em;
}
.interview-page[data-engine-state='room'] .chat-plan-header p {
  margin-top: 6px;
  color: var(--muted, #989893);
  font-size: 11px;
}
.interview-page[data-engine-state='room'] .chat-plan-status {
  min-width: 90px;
  padding-top: 18px;
}
.interview-page[data-engine-state='room'] .chat-plan-status strong {
  color: var(--ink, #171717);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.interview-page[data-engine-state='room'] .chat-plan-steps {
  grid-column: 1 / -1;
  gap: 7px;
  margin-top: 0;
}
.interview-page[data-engine-state='room'] .chat-plan-steps span {
  padding: 3px 7px 3px 4px;
  border: 1px solid var(--border-subtle, #e4e7e5);
  border-radius: 999px;
  background: var(--surface-solid, #fff);
  font-size: 10px;
}
.interview-page[data-engine-state='room'] .chat-plan-steps span.current { border-color: var(--ink, #171717); background: var(--ink, #171717); }
.interview-page[data-engine-state='room'] .chat-plan-steps span.done { border-color: var(--brand, #168b68); background: var(--brand-soft, #eef7f1); color: var(--brand, #168b68); }
.interview-page[data-engine-state='room'] .chat-plan-steps i { width: 16px; height: 16px; font-size: 9px; }
.interview-page[data-engine-state='room'] :deep(.chat-messages) {
  gap: 18px;
  padding: 30px 34px 22px;
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] :deep(.chat-message.interviewer) { max-width: 760px; }
.interview-page[data-engine-state='room'] :deep(.msg-avatar) {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: var(--bg-surface, #f5f6f4);
  color: var(--ink, #171717);
  font-size: 12px;
}
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble) {
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
}
.interview-page[data-engine-state='room'] :deep(.bubble-header) { margin-bottom: 10px; }
.interview-page[data-engine-state='room'] :deep(.bubble-name) { font-size: 11px; font-weight: 700; }
.interview-page[data-engine-state='room'] :deep(.bubble-question-num) { color: var(--muted, #989893); font-size: 10px; }
.interview-page[data-engine-state='room'] :deep(.bubble-source-label) { margin: 0 0 8px; border-radius: 4px; background: var(--bg-surface, #f5f6f4); color: var(--muted, #6e6e6a); }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble .bubble-text) {
  max-width: 700px;
  color: var(--ink, #171717);
  font-size: clamp(20px, 2vw, 29px);
  font-weight: 650;
  letter-spacing: -.035em;
  line-height: 1.32;
}
.interview-page[data-engine-state='room'] :deep(.user-bubble) { border-radius: 9px; background: var(--ink, #171717); }
.interview-page[data-engine-state='room'] :deep(.evaluation-inline),
.interview-page[data-engine-state='room'] :deep(.summary-inline) {
  border-color: var(--border-subtle, #e4e7e5);
  border-radius: 10px;
  background: var(--surface-solid, #fff);
  box-shadow: 0 4px 14px rgba(20, 24, 22, .035);
}
.interview-page[data-engine-state='room'] :deep(.evaluation-inline) { max-width: 760px; margin-left: 38px; }
.interview-page[data-engine-state='room'] :deep(.evaluation-inline .ref-answer) { display: none; }
.interview-page[data-engine-state='room'] :deep(.eval-score-row) { grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
.interview-page[data-engine-state='room'] :deep(.eval-score-item) { display: grid; grid-template-columns: 1fr auto; gap: 6px 8px; align-items: center; }
.interview-page[data-engine-state='room'] :deep(.eval-score-item .el-progress) { grid-column: 1 / -1; }
.interview-page[data-engine-state='room'] :deep(.eval-score-item strong) { width: auto; font-size: 12px; font-variant-numeric: tabular-nums; }
.interview-page[data-engine-state='room'] .chat-input-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
  padding: 12px 28px 18px;
  border-top: 1px solid var(--border-subtle, #e4e7e5);
  background: var(--surface-solid, #fff);
}
.interview-page[data-engine-state='room'] .voice-row { display: none; }
.interview-page[data-engine-state='room'] .chat-textarea :deep(.el-textarea__inner) {
  min-height: 52px !important;
  padding: 13px 14px;
  border: 1px solid var(--border-subtle, #d6dbd8);
  border-radius: 9px;
  background: var(--surface-solid, #fff);
  color: var(--ink, #171717);
  font-size: 13px;
  resize: none;
}
.interview-page[data-engine-state='room'] .chat-send-btn { min-width: 92px; height: 52px; border-radius: 9px; background: var(--ink, #171717); }
.interview-page[data-engine-state='room'] .chat-room-error { margin: 0 28px 12px; }
@media (max-width: 980px) {
  .interview-page[data-engine-state='room'] .interview-chat-layout { grid-template-columns: 150px minmax(0, 1fr); }
  .interview-page[data-engine-state='room'] .room-context-panel { display: none; }
}
@media (max-width: 720px) {
  .interview-page[data-engine-state='room'] { padding: 0; }
  .interview-page[data-engine-state='room'] .interview-command-bar { padding: 0 14px; }
  .interview-page[data-engine-state='room'] .interview-chat-layout { display: flex; height: calc(100vh - 42px); min-height: 0; }
  .interview-page[data-engine-state='room'] .chat-sidebar { display: none; }
  .interview-page[data-engine-state='room'] .chat-plan-header { padding-inline: 18px; }
  .interview-page[data-engine-state='room'] :deep(.chat-messages) { padding: 24px 18px 18px; }
  .interview-page[data-engine-state='room'] .chat-input-bar { padding-inline: 14px; }
  .interview-page[data-engine-state='room'] :deep(.interviewer-bubble .bubble-text) { font-size: 21px; }
  .interview-page[data-engine-state='room'] :deep(.evaluation-inline) { margin-left: 0; }
}

/* Target room composition: a full white desktop canvas with one context strip and three aligned columns. */
.interview-page[data-engine-state='room'] {
  min-height: 100vh;
  padding: 6px 0 10px;
  background: #fff;
  color: #171717;
}
.interview-page[data-engine-state='room'] .interview-command-bar {
  min-height: 36px;
  margin: 0 0 6px;
  padding: 0 42px;
  border-bottom: 0;
}
.interview-page[data-engine-state='room'] .engine-back-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  padding: 4px 0;
  border: 0;
  background: transparent;
  color: #171717;
  font-size: 14px;
  font-weight: 650;
}
.interview-page[data-engine-state='room'] .engine-back-button .el-icon { font-size: 18px; }
.interview-page[data-engine-state='room'] .room-command-actions { display: flex; align-items: center; gap: 22px; margin-left: auto; }
.interview-page[data-engine-state='room'] .room-command-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border: 0;
  background: transparent;
  color: #272927;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.interview-page[data-engine-state='room'] .room-command-button .el-icon { font-size: 15px; }
.interview-page[data-engine-state='room'] .room-command-end { color: #171717; }
.interview-page[data-engine-state='room'] .interview-chat-layout {
  display: grid;
  grid-template-columns: 172px minmax(0, 1fr) 248px;
  grid-template-rows: 48px minmax(0, 1fr);
  height: calc(100vh - 58px);
  min-height: 0;
  overflow: hidden;
  position: relative;
  border: 0;
  border-top: 1px solid #e4e7e5;
  border-radius: 0;
  background: #fff;
}
.interview-page[data-engine-state='room'] .interview-chat-layout::after {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 248px;
  bottom: 0;
  width: 1px;
  background: #e4e7e5;
  content: '';
  pointer-events: none;
}
.interview-page[data-engine-state='room'] .room-plan-header {
  grid-column: 1 / -1;
  grid-row: 1;
  align-self: start;
  display: flex;
  align-items: stretch;
  width: fit-content;
  max-width: min(780px, calc(100% - 220px));
  margin-left: 47px;
  min-height: 38px;
  overflow: hidden;
  border: 1px solid #e4e7e5;
  border-radius: 10px;
  background: #fff;
}
.interview-page[data-engine-state='room'] { margin-left: -12px; margin-right: 8px; }
.interview-page[data-engine-state='room'] .room-plan-chip { display: flex; flex: 1 1 auto; align-items: center; gap: 9px; min-width: 0; padding: 5px 14px; border-right: 1px solid #e4e7e5; }
.interview-page[data-engine-state='room'] .room-plan-chip:last-of-type { border-right: 0; }
.interview-page[data-engine-state='room'] .room-plan-chip-icon { display: grid; place-items: center; color: #272927; font-size: 16px; }
.interview-page[data-engine-state='room'] .room-plan-chip > span:last-child { display: flex; align-items: baseline; gap: 4px; min-width: 0; white-space: nowrap; }
.interview-page[data-engine-state='room'] .room-plan-chip small { color: #777a76; font-size: 11px; line-height: 1.2; }
.interview-page[data-engine-state='room'] .room-plan-chip small::after { content: '：'; }
.interview-page[data-engine-state='room'] .room-plan-chip strong { max-width: 190px; overflow: hidden; color: #272927; font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.interview-page[data-engine-state='room'] .room-plan-mode { padding-left: 14px; }
.interview-page[data-engine-state='room'] .room-plan-mode .room-plan-chip-icon { color: #171717; }
.interview-page[data-engine-state='room'] .room-plan-progress { display: none; }
.interview-page[data-engine-state='room'] .chat-sidebar {
  grid-column: 1;
  grid-row: 2;
  width: auto;
  min-height: 0;
  padding: 24px 22px 20px;
  border-right: 1px solid #e4e7e5;
  background: #fff;
}
.interview-page[data-engine-state='room'] .chat-main {
  grid-column: 2;
  grid-row: 2;
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow-y: auto;
  background: #fff;
}
.interview-page[data-engine-state='room'] .room-context-panel {
  grid-column: 3;
  grid-row: 2;
  min-height: 0;
  border-left: 0;
  background: #fff;
}
.interview-page[data-engine-state='room'] .interview-chat-layout { overflow: visible; }
.interview-page[data-engine-state='room'] .room-context-lock { display: none; }
.interview-page[data-engine-state='room'] :deep(.chat-messages) { flex: 0 0 auto; min-height: 0; overflow: visible; padding: 18px 42px 8px; gap: 16px; background: #fff; }
.interview-page[data-engine-state='room'] :deep(.chat-messages) { display: contents; }
.interview-page[data-engine-state='room'] :deep(.chat-message.interviewer) { order: 1; margin: 18px 42px 4px; }
.interview-page[data-engine-state='room'] :deep(.chat-message.interviewer) { max-width: 760px; gap: 13px; }
.interview-page[data-engine-state='room'] :deep(.chat-message.interviewer > .msg-avatar) { display: none; }
.interview-page[data-engine-state='room'] :deep(.bubble-header) { display: none; }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble) { display: flex; flex-direction: column; }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble .bubble-text) { order: 1; }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble .bubble-source-label) { order: 2; width: fit-content; margin-top: 10px; }
.interview-page[data-engine-state='room'] :deep(.bubble-name) { font-size: 11px; }
.interview-page[data-engine-state='room'] :deep(.bubble-question-num) { font-size: 10px; }
.interview-page[data-engine-state='room'] :deep(.bubble-source-label) { display: inline-flex; align-items: center; gap: 6px; height: 20px; margin-bottom: 7px; padding: 0; border-radius: 0; background: transparent; color: #777a76; font-size: 11px; line-height: 20px; }
.interview-page[data-engine-state='room'] :deep(.bubble-source-label .el-icon) { font-size: 15px; }
.interview-page[data-engine-state='room'] :deep(.interviewer-bubble .bubble-text) { max-width: 640px; font-size: clamp(18px, 1.45vw, 23px); line-height: 1.42; }
.interview-page[data-engine-state='room'] :deep(.user-bubble) { border-radius: 8px; background: #171717; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user),
.interview-page[data-engine-state='room'] :deep(.chat-message.sending),
.interview-page[data-engine-state='room'] :deep(.chat-message.evaluation),
.interview-page[data-engine-state='room'] :deep(.chat-message.summary) { order: 3; max-width: none; margin-inline: 54px; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user) { margin-top: 20px; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-avatar),
.interview-page[data-engine-state='room'] :deep(.chat-message.sending .user-avatar) { display: none; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-bubble) { max-width: none; padding: 0; border-radius: 0; background: transparent; color: #171717; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-bubble)::before { content: none; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-bubble) { border-top: 1px solid #e4e7e5; padding-top: 16px; }
.interview-page[data-engine-state='room'] :deep(.submitted-answer-heading) { display: block; margin-bottom: 10px; color: #171717; font-size: 14px; font-weight: 700; line-height: 1.35; }
.interview-page[data-engine-state='room'] :deep(.submitted-answer-heading span) { color: #777a76; font-size: 11px; font-weight: 500; }
.interview-page[data-engine-state='room'] :deep(.chat-message.user .bubble-text) { color: #595c58; font-size: 13px; line-height: 1.65; }
.interview-page[data-engine-state='room'] :deep(.chat-message.evaluation) { margin-top: 18px; }
.interview-page[data-engine-state='room'] :deep(.evaluation-inline) { max-width: 760px; margin-left: 0; border: 1px solid #e4e7e5; border-radius: 10px; box-shadow: none; }
.interview-page[data-engine-state='room'] .chat-input-bar { grid-template-columns: minmax(0, 1fr) auto auto auto; row-gap: 14px; column-gap: 12px; padding: 8px 42px 18px; border-top: 1px solid #e4e7e5; background: #fff; }
.interview-page[data-engine-state='room'] .chat-input-bar { order: 2; }
.interview-page[data-engine-state='room'] .chat-textarea { grid-column: 1 / -1; }
.interview-page[data-engine-state='room'] .chat-textarea :deep(.el-textarea__inner) { min-height: 220px !important; padding: 14px 16px; border-color: #d8ddd9; border-radius: 8px; font-size: 13px; line-height: 1.65; }
.interview-page[data-engine-state='room'] .chat-send-btn { grid-column: 2; min-width: 118px; height: 40px; border-radius: 8px; background: #171717; }
.interview-page[data-engine-state='room'] .chat-room-inline-control { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-width: 102px; height: 40px; padding: 0 14px; border: 1px solid #d8ddd9; border-radius: 8px; background: #fff; color: #272927; font-size: 12px; font-weight: 650; cursor: pointer; }
.interview-page[data-engine-state='room'] .chat-room-inline-end { grid-column: 4; min-width: 112px; }
.interview-page[data-engine-state='room'] .chat-room-error { margin-inline: 42px; }
.interview-page[data-engine-state='room'] :deep(.room-context-lock) { display: none; }
.interview-page[data-engine-state='room'] :deep(.room-context-panel) { border-left: 0; }
.interview-page[data-engine-state='room'] :deep(.room-context-panel::before) { display: none; }
.interview-page[data-engine-state='room'] :deep(.chat-sidebar) { padding-top: 28px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-head) { margin-bottom: 14px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-current) { display: inline; color: #171717; font-size: 10px; font-weight: 650; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-item) { min-height: 43px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-name) { display: inline; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-item) { justify-content: flex-start; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-track) { padding-left: 4px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-progress-track)::before { left: 9px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-dot) { width: 10px; height: 10px; border: 1px solid #c7ccc8; background: #eef0ee; color: transparent; transition: background .2s ease, border-color .2s ease, box-shadow .2s ease; }
.interview-page[data-engine-state='room'] :deep(.sidebar-dot.completed) { border-color: #aeb6b1; background: #aeb6b1; color: transparent; }
.interview-page[data-engine-state='room'] :deep(.sidebar-dot.active) { width: 10px; height: 10px; border: 2px solid #168b68; background: #168b68; color: transparent; animation: room-dot-pulse 1.8s ease-in-out infinite; }
.interview-page[data-engine-state='room'] :deep(.sidebar-dot.viewing) { outline: 2px solid #168b68; outline-offset: 2px; }
@keyframes room-dot-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(116, 123, 119, 0); }
  50% { box-shadow: 0 0 0 5px rgba(22, 139, 104, .16); }
}
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-card) {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  grid-template-rows: auto auto auto;
  grid-template-areas: 'label label' 'avatar name' 'avatar meta';
  justify-items: start;
  align-items: center;
  column-gap: 10px;
  margin-top: auto;
  padding-top: 30px;
  padding-bottom: 66px;
  text-align: left;
}
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-card > span) { grid-area: label; margin: 0 0 10px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-avatar) { grid-area: avatar; width: 40px; height: 40px; border-radius: 50%; font-size: 15px; }
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-card strong) { grid-area: name; margin: 0; font-size: 12px; line-height: 1.35; }
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-card small) { grid-area: meta; font-size: 10px; line-height: 1.35; }
.interview-page[data-engine-state='room'] :deep(.sidebar-persona-card p) { display: none; }
.interview-page[data-engine-state='room'] :deep(.sidebar-round-card) { order: 2; margin-top: auto; padding-top: 28px; padding-bottom: 36px; }
.interview-page[data-engine-state='room'] :deep(.round-switch-button) { border-color: #e4e7e5; border-radius: 7px; background: #fff; }
.interview-page[data-engine-state='room'] :deep(.round-switch-button.active) { border-color: #e4e7e5; border-left: 2px solid #168b68; background: #fff; }
.interview-page[data-engine-state='room'] :deep(.round-switch-button i) { background: #f0f2ef; color: #6e6e6a; }
.interview-page[data-engine-state='room'] :deep(.round-switch-button.active i) { background: #168b68; color: #fff; }
.interview-page[data-engine-state='room'] .chat-room-inline-control:not(.chat-room-inline-end) { min-width: 129px; }
.interview-page[data-engine-state='room'] .review-mode-bar,
.interview-page[data-engine-state='room'] .chat-room-error { order: 4; }
@media (max-width: 1100px) {
  .interview-page[data-engine-state='room'] .interview-command-bar { padding-inline: 28px; }
  .interview-page[data-engine-state='room'] .interview-chat-layout { grid-template-columns: 172px minmax(0, 1fr) 240px; }
  .interview-page[data-engine-state='room'] .interview-chat-layout::after { right: 240px; }
  .interview-page[data-engine-state='room'] .room-plan-header { max-width: calc(100% - 160px); margin-left: 28px; }
  .interview-page[data-engine-state='room'] :deep(.chat-messages),
  .interview-page[data-engine-state='room'] .chat-input-bar { padding-inline: 34px; }
  .interview-page[data-engine-state='room'] :deep(.eval-score-row) { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 820px) {
  .interview-page[data-engine-state='room'] { padding: 0; }
  .interview-page[data-engine-state='room'] .interview-chat-layout { grid-template-columns: 1fr; grid-template-rows: 68px minmax(0, 1fr); height: calc(100vh - 60px); min-height: 0; }
  .interview-page[data-engine-state='room'] .chat-sidebar,
  .interview-page[data-engine-state='room'] .room-context-panel { display: none; }
  .interview-page[data-engine-state='room'] .chat-main { grid-column: 1; }
  .interview-page[data-engine-state='room'] .interview-command-bar { padding-inline: 14px; }
  .interview-page[data-engine-state='room'] .room-plan-header { max-width: calc(100% - 24px); margin-inline: 12px; }
  .interview-page[data-engine-state='room'] .interview-chat-layout::after { display: none; }
}

/* 首页记录是可操作的工作日志：删除动作只在聚焦/悬浮时出现，避免与打开记录竞争视觉层级。 */
.interview-page[data-engine-state='home'] .engine-timeline-row { cursor: pointer; }
.interview-page[data-engine-state='home'] .engine-timeline-row:focus-visible { outline: 2px solid var(--brand); outline-offset: 3px; }
.interview-page[data-engine-state='home'] .engine-timeline-delete {
  grid-column: 6;
  grid-row: 2;
  justify-self: end;
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  /* 保持低存在感但始终可发现，避免用户以为历史记录无法删除。 */
  opacity: .56;
  transition: opacity .16s ease, color .16s ease, background .16s ease;
}
.interview-page[data-engine-state='home'] .engine-timeline-row:hover .engine-timeline-delete,
.interview-page[data-engine-state='home'] .engine-timeline-row:focus-within .engine-timeline-delete { opacity: 1; }
.interview-page[data-engine-state='home'] .engine-timeline-delete:hover { color: #b42318; background: rgba(180, 35, 24, .08); }
.engine-review-load-state { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 0 14px; padding: 10px 14px; border-bottom: 1px solid #e4e7e5; color: #777a76; font-size: 12px; }
.engine-review-load-state.is-error { color: #8c2d22; background: #fff8f6; border: 1px solid #f1d7d1; border-radius: 8px; }
.engine-review-load-state button { padding: 0; border: 0; background: transparent; color: inherit; font-size: 12px; font-weight: 650; cursor: pointer; }
.chat-session-loading,
.chat-session-load-error { margin: 12px 42px 0; padding: 9px 12px; border: 1px solid #e4e7e5; border-radius: 8px; font-size: 12px; }
.chat-session-loading { color: #777a76; background: #fafbfa; }
.chat-session-load-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #8c2d22; background: #fff8f6; }
.chat-session-load-error button { padding: 0; border: 0; background: transparent; color: #8c2d22; font-size: 12px; font-weight: 650; cursor: pointer; }
.interview-page[data-engine-state='room'] .chat-send-btn,
.interview-page[data-engine-state='room'] .chat-room-inline-control { height: 40px; min-height: 40px; }

/* 已提交回答是记录内容，不再伪装成聊天气泡；提交中的状态也保持同一版式。 */
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-bubble),
.interview-page[data-engine-state='room'] :deep(.chat-message.sending .user-bubble) {
  max-width: none;
  padding: 16px 0 0;
  border: 0;
  border-top: 1px solid #e4e7e5;
  border-radius: 0;
  background: transparent !important;
  box-shadow: none;
  color: #171717;
}
.interview-page[data-engine-state='room'] :deep(.chat-message.user .user-bubble .bubble-text),
.interview-page[data-engine-state='room'] :deep(.chat-message.sending .user-bubble .bubble-text) {
  color: #595c58;
}
.interview-page[data-engine-state='room'] :deep(.submitted-answer-question),
.interview-page[data-engine-state='room'] :deep(.evaluation-question-label) {
  color: #777a76;
  font-size: 11px;
  font-weight: 500;
}
</style>
