<template>
  <div v-if="!editorActive" class="launch-page">
    <section class="launch-hero">
      <div class="launch-copy">
        <div class="launch-badge">ResumeGo Studio · AI 求职成长闭环</div>
        <h1>让每一次评分、匹配和面试，都回到一份更强的简历。</h1>
        <p>
          职达以简历为中心，把岗位 JD、评分诊断、AI 建议和模拟面试收进同一个工作流。
          你不是在使用一堆功能页，而是在持续打磨一份可以投递的作品。
        </p>

        <div class="launch-actions">
          <button class="launch-primary" type="button" @click="continueResumeEditing">
            继续改进当前简历
          </button>
          <button class="launch-secondary" type="button" @click="startBlankResume">
            新建一份简历
          </button>
        </div>

        <section class="launch-resume-library">
          <div class="launch-version-head">
            <span>Resume Library</span>
            <strong>我的简历</strong>
          </div>
          <div v-if="resumes.length" class="launch-resume-grid">
            <button
              v-for="resume in sortedResumes"
              :key="resume.id"
              type="button"
              class="launch-resume-item"
              :class="{ active: resume.id === activeResume?.id }"
              @click="openResumeFromLaunch(resume.id)"
            >
              <div class="resume-card-thumb">
                <strong>{{ resumePreviewName(resume) }}</strong>
                <span></span>
                <span></span>
                <em></em>
                <span></span>
              </div>
              <div class="resume-card-main">
                <strong>{{ resume.title || '未命名简历' }}</strong>
                <span>{{ resumeJobLabel(resume) }}</span>
                <small>{{ resumeVersionSummary(resume) }}</small>
                <div class="resume-card-versions">
                  <i
                    v-for="version in resumeRecentVersions(resume).slice(0, 3)"
                    :key="version.id"
                  >
                    v{{ version.versionNo }}
                  </i>
                </div>
              </div>
            </button>
          </div>
          <p v-else class="launch-version-empty">暂无简历，先新建一份。</p>
        </section>
      </div>

      <div class="launch-resume-stage">
        <div class="launch-glow one"></div>
        <div class="launch-glow two"></div>
        <article class="launch-resume-card">
          <div class="launch-resume-top">
            <span>当前简历</span>
            <strong>{{ activeResume?.title || '我的简历' }}</strong>
            <small>{{ activeVersion ? `版本 v${activeVersion.versionNo}` : '等待版本数据' }}</small>
          </div>
          <EditorPreviewPanel
            class="launch-preview"
            :template-style="selectedTemplate"
            :sections="editorSections"
            :selected-section-id="selectedSectionId"
            :version-label="activeVersion ? `v${activeVersion.versionNo}` : '未选择版本'"
            @select-section="selectSection"
          />
        </article>
        <div class="launch-metrics">
          <div class="launch-metric-item">
            <strong>{{ activeVersion ? `v${activeVersion.versionNo}` : 'v-' }}</strong>
            <span>当前版本</span>
          </div>
          <div class="launch-metric-item">
            <strong>{{ jobs.length || 0 }}</strong>
            <span>岗位数据</span>
          </div>
          <div class="launch-metric-item">
            <strong>{{ resumes.length || 0 }}</strong>
            <span>简历数据</span>
          </div>
        </div>
      </div>
    </section>

  </div>

  <div v-else class="editor-stage">
    <header class="resume-editor-toolbar">
      <button class="toolbar-back" type="button" title="返回启动页" @click="exitEditor">←</button>
      <div class="toolbar-title">
        <strong>{{ editorResumeTitle }}</strong>
        <span>{{ editorStatusLabel }}</span>
      </div>

      <div class="toolbar-target-card" :class="{ empty: !selectedJob }">
        <CompanyAvatar :job="selectedJob" size="toolbar" />
        <div class="toolbar-target-card__content">
          <div class="toolbar-target-card__company">
            <strong>{{ selectedJob?.companyName || '未选择目标岗位' }}</strong>
            <em v-if="selectedJob" :class="selectedJobStatusClass">{{ selectedJobStatusLabel }}</em>
          </div>
          <small>{{ selectedJob ? targetJobMetaLine : '从岗位库选择目标岗位后，评分、建议和面试会围绕它展开' }}</small>
        </div>
        <div class="toolbar-target-card__actions">
          <button
            v-if="selectedJob"
            type="button"
            class="ghost"
            @click="viewSelectedJobDetail"
          >
            详情
          </button>
          <button
            type="button"
            class="primary"
            @click="openJobLibrary"
          >
            {{ selectedJob ? '更换' : '选择岗位' }}
          </button>
        </div>
      </div>

      <div class="toolbar-history" aria-label="草稿历史">
        <button type="button" :disabled="!canUndo" title="撤销 ⌘/Ctrl + Z" @click="undoDraft">↶</button>
        <button type="button" :disabled="!canRedo" title="重做 ⌘/Ctrl + Shift + Z / Ctrl + Y" @click="redoDraft">↷</button>
      </div>

      <nav class="toolbar-tools">
        <button type="button" :class="{ active: templateLibraryOpen }" title="打开简历模板库" @click="toggleTemplateLibrary">模板</button>
        <button type="button" title="打开简历评分" @click="openAssessment">评分</button>
        <button type="button" title="查看匹配度前五的岗位" @click="openJobRecommendations">岗位推荐</button>
        <button type="button" :class="{ active: rightPanelMode === 'ai' }" title="切换到 AI 建议 ⌘/Ctrl + 2" @click="showAiPanel">AI 建议</button>
        <button type="button" title="进入模拟面试" @click="startInterviewPreparation">模拟面试</button>
        <button type="button" title="打开岗位库" @click="openJobLibrary">岗位库</button>
        <button type="button" :disabled="exportingPdf" title="导出当前预览为 PDF" @click="exportResumePdf">
          {{ exportingPdf ? '导出中' : '导出' }}
        </button>
      </nav>

      <div class="template-current" role="button" tabindex="0" @click="openTemplateLibrary" @keydown.enter.prevent="openTemplateLibrary">
        <span>当前模板</span>
        <strong>{{ currentTemplate?.label || '模板' }}</strong>
      </div>
    </header>

    <transition name="template-backdrop">
      <button
        v-if="templateLibraryOpen"
        class="template-library-backdrop"
        type="button"
        aria-label="关闭模板库"
        @click="closeTemplateLibrary"
      ></button>
    </transition>

    <transition name="template-library">
      <section v-if="templateLibraryOpen" class="template-library-panel" @click.stop>
        <header class="template-library-head">
          <div>
            <span>Template Library</span>
            <strong>简历模板库</strong>
          </div>
          <button type="button" title="关闭模板库" @click="closeTemplateLibrary">×</button>
        </header>

        <div class="template-library-grid">
          <button
            v-for="template in templateOptions"
            :key="template.key"
            class="template-card"
            :class="{ active: selectedTemplate === template.key }"
            type="button"
            @click="selectTemplate(template.key)"
          >
            <span class="template-card-status">{{ selectedTemplate === template.key ? '当前使用' : template.badge }}</span>
            <div class="template-miniature" :class="`mini-${template.key}`">
              <i></i>
              <strong></strong>
              <span></span>
              <span></span>
              <em></em>
              <span></span>
            </div>
            <div class="template-card-copy">
              <strong>{{ template.label }}</strong>
              <span>{{ template.description }}</span>
            </div>
          </button>
        </div>
      </section>
    </transition>

    <el-dialog
      v-model="assessmentDialogOpen"
      title="简历诊断报告"
      width="860px"
      class="assessment-dialog"
      append-to-body
    >
      <div class="assessment-modal">
        <div class="assessment-modal__head">
          <div>
            <span class="assessment-modal__eyebrow">Resume Assessment</span>
            <strong>{{ assessmentTargetLabel }}</strong>
            <small>基于当前版本内容生成评分报告</small>
          </div>
          <button
            type="button"
            :disabled="!activeVersion || assessmentLoading"
            @click="runAssessmentInDialog"
          >
            {{ assessmentLoading ? '评分中...' : assessmentResult ? '重新评分' : '生成评分' }}
          </button>
        </div>

        <el-alert
          v-if="assessmentError"
          :title="assessmentError"
          type="error"
          show-icon
          closable
          @close="assessmentError = ''"
        />

        <div v-if="assessmentLoading" class="assessment-modal__loading">
          <i></i>
          <span>正在根据当前简历版本生成评分...</span>
        </div>

        <div
          v-else-if="!assessmentResult"
          class="assessment-modal__empty-state"
        >
          <strong>等待生成诊断报告</strong>
          <span>点击右上角按钮后，在这里查看总分、维度表现和主要改进点。</span>
        </div>

        <template v-else>
          <div class="assessment-modal__hero">
            <div class="assessment-modal__score">
              <div class="assessment-modal__score-ring">
                <i class="assessment-modal__score-aura"></i>
                <i class="assessment-modal__score-scan"></i>
                <el-progress
                  type="dashboard"
                  :percentage="Number(assessmentResult.totalScore)"
                  :color="assessmentProgressColor(Number(assessmentResult.totalScore), 100)"
                  :width="132"
                />
              </div>
              <div class="assessment-modal__score-copy">
                <span>Resume Score</span>
                <strong>
                  {{ assessmentResult.totalScore }}
                  <em>/100</em>
                </strong>
                <small>生成于 {{ assessmentResult.createdAt.replace('T', ' ').slice(0, 16) }}</small>
              </div>
            </div>
            <div class="assessment-modal__summary">
              <span>当前诊断</span>
              <strong>{{ assessmentSummaryText }}</strong>
              <p>评分结果用于定位简历表达问题，后续优化仍以你已录入的真实经历和证据为依据。</p>
            </div>
          </div>

          <div class="assessment-modal__dimensions">
            <article
              v-for="item in assessmentDimensionItems"
              :key="item.key"
              class="assessment-modal__dimension"
            >
              <div>
                <span>{{ item.label }}</span>
                <strong>{{ item.score }}/{{ item.max }}</strong>
              </div>
              <el-progress
                :percentage="item.percentage"
                :color="item.color"
                :show-text="false"
                :stroke-width="7"
              />
            </article>
          </div>

          <div class="assessment-modal__issues">
            <div class="assessment-modal__issues-head">
              <h3>主要改进点</h3>
              <span>{{ assessmentResult.deductions.length }} 项</span>
            </div>
            <p v-if="assessmentResult.deductions.length === 0" class="assessment-modal__empty">
              暂无扣分项，当前版本表现稳定。
            </p>
            <article
              v-for="(deduction, index) in assessmentResult.deductions.slice(0, 5)"
              :key="`${deduction.code}-${index}`"
            >
              <span>扣 {{ deduction.points }} 分</span>
              <div>
                <strong>{{ deduction.reason }}</strong>
                <p v-if="deduction.suggestion">{{ deduction.suggestion }}</p>
              </div>
            </article>
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog
      v-model="recommendDialogOpen"
      title="岗位推荐报告"
      width="900px"
      class="assessment-dialog recommend-dialog"
      append-to-body
    >
      <div class="assessment-modal">
        <div class="assessment-modal__head">
          <div>
            <span class="assessment-modal__eyebrow">Job Match Radar</span>
            <strong>{{ activeVersion ? `基于 v${activeVersion.versionNo} 推荐岗位` : '未选择版本' }}</strong>
            <small>对已解析岗位进行批量匹配，优先展示最适合当前简历的 Top 5</small>
          </div>
          <button
            type="button"
            :disabled="!activeVersion || recommendLoading"
            @click="runRecommendInDialog"
          >
            {{ recommendLoading ? '匹配中...' : recommendResults.length ? '重新匹配' : '开始匹配' }}
          </button>
        </div>

        <el-alert
          v-if="recommendError"
          :title="recommendError"
          type="error"
          show-icon
          closable
          @close="recommendError = ''"
        />

        <div v-if="recommendLoading" class="assessment-modal__loading">
          <i></i>
          <span>正在匹配所有岗位，排名前 5 的推荐结果...</span>
        </div>

        <div
          v-else-if="!recommendResults.length"
          class="assessment-modal__empty-state"
        >
          <strong>等待生成岗位推荐</strong>
          <span>点击开始匹配后，根据当前简历内容与所有岗位的匹配度，展示前 5 个最合适岗位。</span>
        </div>

        <template v-else>
          <div class="recommend-summary">
            <div>
              <span>推荐结果</span>
              <strong>{{ recommendResults.length }} 个岗位</strong>
            </div>
            <p>推荐分数来自岗位匹配规则，用于辅助选择目标岗位；最终投递仍建议结合城市、薪资和个人意愿判断。</p>
          </div>

          <div class="recommend-list">
            <article
              v-for="item in recommendResults"
              :key="item.jobDescriptionId"
              class="recommend-item"
              :class="{ selected: item.jobDescriptionId === selectedJobId }"
            >
              <div class="recommend-item__rank">
                <strong :class="rankClass(item.matchScore)">{{ item.matchScore }}</strong>
                <span>{{ item.matchLevel }}</span>
              </div>
              <CompanyAvatar :job="resolveRecommendJob(item.jobDescriptionId)" size="md" />
              <div class="recommend-item__info">
                <strong>{{ resolveJobTitle(item.jobDescriptionId) }}</strong>
                <small>{{ recommendMetaLine(item.jobDescriptionId) }}</small>
                <div class="recommend-item__chips">
                  <span
                    v-for="skill in recommendPreviewSkills(item.jobDescriptionId)"
                    :key="`${item.jobDescriptionId}-${skill}`"
                  >
                    {{ skill }}
                  </span>
                  <em v-if="!recommendPreviewSkills(item.jobDescriptionId).length">暂无结构化技能标签</em>
                </div>
              </div>
              <div class="recommend-item__actions">
                <button
                  type="button"
                  class="recommend-item__target"
                  :disabled="item.jobDescriptionId === selectedJobId"
                  @click="selectRecommendedJob(item.jobDescriptionId)"
                >
                  {{ item.jobDescriptionId === selectedJobId ? '当前目标' : '设为目标' }}
                </button>
                <button
                  type="button"
                  class="recommend-item__link"
                  @click="viewRecommendJobDetail(item.jobDescriptionId)"
                >
                  查看详情
                </button>
              </div>
            </article>
          </div>
        </template>
      </div>
    </el-dialog>

    <div class="editor-workspace-page">
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      @close="errorMessage = ''"
    />

    <section v-loading="loading" class="editor-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <EditorSidebar
        :sections="editorSections"
        :available-modules="availableModules"
        :selected-section-id="selectedSectionId"
        :versions="versions"
        :selected-version-id="selectedVersionId"
        :dirty="isDirty"
        :collapsed="sidebarCollapsed"
        @select-section="selectSection"
        @add-module="addResumeModule"
        @remove-module="removeResumeModule"
        @move-module="moveResumeModule"
        @switch-version="switchVersion"
        @toggle-collapsed="sidebarCollapsed = !sidebarCollapsed"
      />

      <EditorCanvas
        :sections="editorSections"
        :selected-section-id="selectedSectionId"
        :resume-title="editorResumeTitle"
        :version-label="editorVersionLabel"
        :updated-at="editorUpdatedAt"
        :dirty="isDirty"
        :saving="savingDraft"
        @select-section="selectSection"
        @update-field="updateDraftField"
        @update-paragraph="updateDraftParagraph"
        @update-chips="updateDraftChips"
        @update-list-item="updateDraftListItem"
        @add-list-item="addDraftListItem"
        @remove-list-item="removeDraftListItem"
        @move-list-item="moveDraftListItem"
        @add-item="addDraftItem"
        @remove-item="removeDraftItem"
        @move-item="moveDraftItem"
        @toggle-visibility="toggleSectionVisibility"
        @remove-section="removeResumeModule"
        @save-draft="saveDraftAsVersion"
        @reset-draft="resetDraft"
      />

      <aside class="editor-right-dock">
        <div class="right-dock-tabs">
          <button
            type="button"
            :class="{ active: rightPanelMode === 'preview' }"
            @click="rightPanelMode = 'preview'"
          >
            简历预览
          </button>
          <button
            type="button"
            :class="{ active: rightPanelMode === 'ai' }"
            @click="rightPanelMode = 'ai'"
          >
            AI 建议
            <span v-if="suggestions.length">{{ suggestions.length }}</span>
          </button>
        </div>

        <EditorPreviewPanel
          v-if="rightPanelMode === 'preview'"
          :template-style="selectedTemplate"
          :sections="editorSections"
          :selected-section-id="selectedSectionId"
          :version-label="editorVersionLabel"
          @select-section="selectSection"
        />

        <AiCoachPanel
          v-else
          :version="blankResumeMode ? null : activeVersion"
          :selected-job="selectedJob"
          :sections="editorSections"
          :selected-section="selectedSection"
          :suggestions="suggestions"
          :job-match="jobMatch"
          :suggestion-loading="suggestionLoading"
          :company-profile="selectedCompanyProfile"
          @open-jobs="openJobLibrary"
          @view-job="viewSelectedJobDetail"
          @select-section="selectSection"
          @generate-suggestions="generateEditorSuggestions"
        />
      </aside>
    </section>

    <transition name="layout-assistant-panel">
      <aside
        v-if="layoutAssistantOpen"
        class="layout-assistant-panel"
        :style="layoutAssistantPanelStyle"
      >
        <header>
          <div>
            <span>AI Layout</span>
            <strong>排版助手</strong>
          </div>
          <button type="button" title="关闭" @click="layoutAssistantOpen = false">×</button>
        </header>

        <button
          v-if="!layoutProposal"
          type="button"
          class="layout-assistant-generate"
          :disabled="layoutProposalLoading"
          @click="generateLayoutProposal"
        >
          {{ layoutProposalLoading ? '正在生成提案…' : '重新生成排版提案' }}
        </button>

        <template v-if="layoutProposal">
          <div v-if="layoutProposal.warnings.length" class="layout-proposal-warnings">
            <span
              v-for="warning in layoutProposal.warnings"
              :key="warning"
            >
              {{ warning }}
            </span>
          </div>

          <div
            v-if="layoutProposal.templateKey || layoutProposal.hiddenSectionIds.length"
            class="layout-proposal-meta"
          >
            <span v-if="layoutProposal.templateKey">
              模板调整：{{ templateLabel(layoutProposal.templateKey) }}
            </span>
            <span v-if="layoutProposal.hiddenSectionIds.length">
              隐藏空模块：{{ layoutProposal.hiddenSectionIds.map(sectionLabel).join('、') }}
            </span>
          </div>

          <div class="layout-diff-list">
            <article
              v-for="change in layoutProposal.changes"
              :key="change.id"
              class="layout-diff-card"
            >
              <header>
                <span>{{ change.label }}</span>
                <strong>{{ change.reason }}</strong>
              </header>
              <div class="layout-diff-grid">
                <div>
                  <small>修改前</small>
                  <p>{{ change.before }}</p>
                </div>
                <div>
                  <small>修改后</small>
                  <p>{{ change.after }}</p>
                </div>
              </div>
            </article>
          </div>

          <div class="layout-proposal-actions">
            <button type="button" class="primary" @click="applyLayoutProposal">接受并应用到草稿</button>
            <button type="button" @click="discardLayoutProposal">放弃</button>
          </div>
        </template>

        <div v-else class="layout-assistant-actions">
          <button
            v-for="action in layoutAssistantActions"
            :key="action.id"
            type="button"
            :class="action.level"
            @click="runLayoutAssistantAction(action.id)"
          >
            <span>{{ action.label }}</span>
            <strong>{{ action.title }}</strong>
            <small>{{ action.description }}</small>
          </button>
        </div>
      </aside>
    </transition>

    <button
      type="button"
      class="layout-assistant-fab"
      :class="{ active: layoutAssistantOpen, warning: layoutAssistantIssueCount > 0 }"
      :style="layoutAssistantFabStyle"
      title="打开 AI 排版助手"
      @pointerdown="startLayoutAssistantDrag"
      @click="toggleLayoutAssistantOpen"
    >
      <span aria-hidden="true"></span>
    </button>
  </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AiCoachPanel from '../components/editor/AiCoachPanel.vue'
import CompanyAvatar from '../components/CompanyAvatar.vue'
import EditorCanvas from '../components/editor/EditorCanvas.vue'
import EditorPreviewPanel from '../components/editor/EditorPreviewPanel.vue'
import EditorSidebar from '../components/editor/EditorSidebar.vue'
import { listJobDescriptions, resolveCompanyProfile } from '../api/job'
import { createLayoutProposal } from '../api/layout'
import { batchMatch, createJobMatch } from '../api/match'
import { generateSuggestionsWithAssessment } from '../api/optimization'
import {
  assessResumeVersion,
  createResume,
  createResumeVersion,
  getResumeVersions,
  listResumes,
  updateResumeTargetJob,
} from '../api/resume'
import {
  defaultResumeTemplateKey,
  isValidResumeTemplateKey,
  resumeTemplateOptions,
} from '../constants/resumeTemplates'
import type { EditorModuleOption, EditorSection, EditorSectionStatus } from '../types/editor'
import type { CompanyProfile, JobDescription } from '../types/job'
import type { LayoutProposalResponse } from '../types/layout'
import type { BatchMatchResult, JobMatch } from '../types/match'
import type { OptimizationSuggestion } from '../types/optimization'
import { exportResumeElementToPdf } from '../utils/exportResumePdf'
import {
  clearWorkspaceSelectedJobId,
  consumePendingWorkspaceSelectedJobId,
  consumeReturnToEditor,
  markReturnToEditor as markWorkspaceReturnToEditor,
  setWorkspaceSelectedJobId,
  setWorkspaceSelectedResumeId,
} from '../utils/workspaceContext'
import type {
  CertificationItem,
  CustomSectionItem,
  EducationItem,
  GithubItem,
  LanguageItem,
  ProjectItem,
  QrCodeItem,
  Resume,
  ResumeAssessment,
  ResumeContent,
  ResumeVersion,
  SkillCategory,
  WorkExperienceItem,
} from '../types/resume'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const resumes = ref<Resume[]>([])
const versions = ref<ResumeVersion[]>([])
const resumeVersionsById = ref<Record<number, ResumeVersion[]>>({})
const jobs = ref<JobDescription[]>([])
const selectedCompanyProfile = ref<CompanyProfile | null>(null)
const selectedResumeId = ref<number | null>(null)
const selectedVersionId = ref<number | null>(null)
const selectedJobId = ref<number | null>(null)
const selectedSectionId = ref('personal-info')
const draftContent = ref<ResumeContent>({})
const baseContentSnapshot = ref('')
const savingDraft = ref(false)
const jobMatch = ref<JobMatch | null>(null)
const suggestions = ref<OptimizationSuggestion[]>([])
const suggestionLoading = ref(false)
const rightPanelMode = ref<'preview' | 'ai'>('preview')
const editorActive = ref(false)
const blankResumeMode = ref(false)
const selectedTemplate = ref(defaultResumeTemplateKey)
const templateLibraryOpen = ref(false)
const exportingPdf = ref(false)
const layoutAssistantOpen = ref(false)
const layoutProposal = ref<LayoutProposalResponse | null>(null)
const layoutProposalLoading = ref(false)
const layoutAssistantPosition = ref(defaultLayoutAssistantPosition())
const layoutAssistantDragOffset = ref({ x: 0, y: 0 })
const layoutAssistantWasDragged = ref(false)
const sidebarCollapsed = ref(false)
const assessmentDialogOpen = ref(false)
const assessmentLoading = ref(false)
const assessmentResult = ref<ResumeAssessment | null>(null)
const assessmentError = ref('')
const recommendDialogOpen = ref(false)
const recommendLoading = ref(false)
const recommendResults = ref<BatchMatchResult[]>([])
const recommendSourceVersionId = ref<number | null>(null)
const recommendError = ref('')
const undoStack = ref<string[]>([])
const redoStack = ref<string[]>([])
const suppressDraftHistory = ref(false)
const maxDraftHistory = 50

const templateOptions = resumeTemplateOptions
const templateStorageKey = 'resumego:selectedResumeTemplate'
const pendingWorkspaceActionKey = 'resumego:pendingWorkspaceAction'
const legacyResumeJobMapStorageKey = 'resumego:resumeJobMap'
const lastResumeIdStorageKey = 'resumego:lastResumeId'

const genderOptions = ['男', '女', '其他', '不展示']
const politicalStatusOptions = ['群众', '共青团员', '中共党员', '中共预备党员', '民主党派', '不展示']
const maritalStatusOptions = ['未婚', '已婚', '不展示']
const educationLevelOptions = ['高中', '大专', '本科', '硕士', '博士', '其他']

type LayoutAssistantAction = {
  id: 'focus-longest' | 'hide-empty-sections'
  label: string
  title: string
  description: string
  level: 'warning' | 'info'
}

const degreeOptions = ['高中', '大专', '本科', '硕士', '博士', '其他']
const languageLevelOptions = ['入门', '日常交流', '熟练', '流利', '母语', 'CET-4', 'CET-6', '雅思', '托福']

const moduleCatalog: EditorModuleOption[] = [
  { id: 'personal-info', type: 'personal_info', title: '个人信息' },
  { id: 'summary', type: 'summary', title: '个人简介' },
  { id: 'work-experience', type: 'work_experience', title: '工作经历' },
  { id: 'education', type: 'education', title: '教育背景' },
  { id: 'skills', type: 'skills', title: '技能特长' },
  { id: 'projects', type: 'projects', title: '项目经历' },
  { id: 'certifications', type: 'certifications', title: '资格证书' },
  { id: 'languages', type: 'languages', title: '语言能力' },
  { id: 'github', type: 'github', title: 'GitHub 项目' },
  { id: 'qr-codes', type: 'qr_codes', title: '二维码' },
  { id: 'custom', type: 'custom', title: '自定义模块' },
]

const defaultActiveSectionIds = ['personal-info', 'summary', 'education', 'skills', 'projects']

const sortedResumes = computed(() => [...resumes.value].sort((left, right) => (
  resumeLatestTimestamp(right) - resumeLatestTimestamp(left)
)))

const activeResume = computed(() => (
  resumes.value.find((resume) => resume.id === selectedResumeId.value) ?? sortedResumes.value[0] ?? null
))

const activeVersion = computed(() => {
  if (blankResumeMode.value) return null
  const selected = versions.value.find((version) => version.id === selectedVersionId.value)
  return selected ?? activeResume.value?.currentVersion ?? versions.value[0] ?? null
})

const resumeContent = computed<ResumeContent>(() => draftContent.value)

const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value) ?? null)
const selectedJobLabel = computed(() => {
  if (!selectedJob.value) return '未选择目标岗位'
  return selectedJob.value.companyName
    ? `${selectedJob.value.jobTitle}｜${selectedJob.value.companyName}`
    : selectedJob.value.jobTitle
})

function layoutTargetJobPayload() {
  const job = selectedJob.value
  if (!job) return null
  return {
    jobTitle: job.jobTitle,
    companyName: job.companyName,
    requiredSkills: job.parsed?.requiredSkills ?? [],
    preferredSkills: job.parsed?.preferredSkills ?? [],
    responsibilities: job.parsed?.responsibilities ?? [],
    experienceRequirements: job.parsed?.experienceRequirements ?? [],
    educationRequirements: job.parsed?.educationRequirements ?? [],
  }
}

function templateLabel(templateKey: string) {
  return templateOptions.find((template) => template.key === templateKey)?.label ?? templateKey
}

function sectionLabel(sectionId: string) {
  return moduleCatalog.find((module) => module.id === sectionId)?.title ?? sectionId
}

const targetJobMetaLine = computed(() => {
  if (!selectedJob.value) return ''
  const meta = selectedJob.value.sourceMeta
  return [
    selectedJob.value.jobTitle,
    meta?.base,
    meta?.industry,
    meta?.salary,
  ].filter(Boolean).join(' · ')
})

const selectedJobStatusLabel = computed(() => {
  if (!selectedJob.value) return '待选择'
  if (selectedJob.value.parseStatus === 'succeeded') return '已解析'
  if (selectedJob.value.parseStatus === 'failed') return '解析失败'
  return '待解析'
})

const selectedJobStatusClass = computed(() => {
  if (!selectedJob.value) return 'empty'
  return selectedJob.value.parseStatus
})

const selectedSection = computed(() => {
  return editorSections.value.find((section) => section.id === selectedSectionId.value)
    ?? editorSections.value[0]
    ?? null
})

const isDirty = computed(() => snapshotResumeContent(draftContent.value) !== baseContentSnapshot.value)
const editorStatusLabel = computed(() => {
  if (blankResumeMode.value) return isDirty.value ? '新简历草稿未保存' : '空白新简历'
  return isDirty.value ? '有未保存草稿' : '当前版本未修改'
})
const editorResumeTitle = computed(() => blankResumeMode.value ? '新简历草稿' : activeResume.value?.title || '未命名简历')
const editorVersionLabel = computed(() => {
  if (blankResumeMode.value) return '新简历草稿'
  return activeVersion.value ? `v${activeVersion.value.versionNo}` : '未选择版本'
})
const editorUpdatedAt = computed(() => blankResumeMode.value ? '尚未保存' : formatDate(activeVersion.value?.createdAt))
const assessmentTargetLabel = computed(() => {
  if (!activeVersion.value) return '未选择版本'
  const summary = activeVersion.value.changeSummary ? ` · ${activeVersion.value.changeSummary}` : ''
  return `v${activeVersion.value.versionNo}${summary} · ${selectedJobLabel.value}`
})
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const currentTemplate = computed(() => (
  templateOptions.find((template) => template.key === selectedTemplate.value) ?? templateOptions[0]
))
const layoutAssistantIssues = computed(() => buildLayoutAssistantIssues(editorSections.value))
const layoutAssistantIssueCount = computed(() => layoutAssistantIssues.value.length)
const layoutAssistantFabStyle = computed(() => ({
  left: `${layoutAssistantPosition.value.x}px`,
  top: `${layoutAssistantPosition.value.y}px`,
}))
const layoutAssistantPanelStyle = computed(() => {
  const viewportWidth = typeof window === 'undefined' ? 1280 : window.innerWidth
  const viewportHeight = typeof window === 'undefined' ? 800 : window.innerHeight
  const panelWidth = Math.min(360, viewportWidth - 32)
  const panelHeight = Math.min(560, viewportHeight - 96)
  const left = clampNumber(layoutAssistantPosition.value.x - panelWidth + 42, 16, viewportWidth - panelWidth - 16)
  const top = clampNumber(layoutAssistantPosition.value.y - panelHeight - 10, 16, viewportHeight - panelHeight - 16)
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${panelWidth}px`,
    maxHeight: `${panelHeight}px`,
  }
})
const emptyVisibleSections = computed(() => editorSections.value.filter((section) => section.visible && section.status === 'empty' && section.id !== 'personal-info'))
const layoutAssistantActions = computed(() => {
  const actions: LayoutAssistantAction[] = []
  const longest = longestLayoutSection(editorSections.value)
  if (longest && sectionTextLength(longest) > 420) {
    actions.push({
      id: 'focus-longest',
      label: longest.title,
      title: '定位最长模块',
      description: '跳到内容密度最高的模块，手动压缩表达或应用内容建议。',
      level: 'warning',
    })
  }
  if (emptyVisibleSections.value.length > 0) {
    actions.push({
      id: 'hide-empty-sections',
      label: `${emptyVisibleSections.value.length} 个空模块`,
      title: '隐藏空模块',
      description: '将当前没有内容的可见模块临时隐藏，降低版面噪音。',
      level: 'info',
    })
  }
  return actions
})

const assessmentDimensionItems = computed(() => {
  const scores = assessmentResult.value?.dimensionScores ?? {}
  const config = [
    { key: 'completeness', label: '完整度', max: 15 },
    { key: 'evidenceSupport', label: '证据支撑', max: 20 },
    { key: 'experienceQuality', label: '经历质量', max: 30 },
    { key: 'quantitativeExpression', label: '量化表达', max: 20 },
    { key: 'readability', label: '可读性', max: 15 },
  ]
  return config.map((item) => {
    const score = Number(scores[item.key] ?? 0)
    return {
      ...item,
      score,
      percentage: item.max === 0 ? 0 : Math.round((score / item.max) * 100),
      color: assessmentProgressColor(score, item.max),
    }
  })
})

function assessmentProgressColor(score: number, max: number) {
  const percentage = max <= 0 ? 0 : (score / max) * 100
  if (percentage < 25) return '#ef4444'
  if (percentage < 50) return '#f97316'
  if (percentage < 75) return '#f59e0b'
  return '#10b981'
}

const assessmentSummaryText = computed(() => {
  if (!assessmentResult.value) return '等待评分'
  const totalScore = Number(assessmentResult.value.totalScore)
  const issueCount = assessmentResult.value.deductions.length
  if (totalScore >= 85 && issueCount === 0) return '整体表现稳定，可进入定向优化'
  if (totalScore >= 75) return `发现 ${issueCount} 个主要改进点，适合继续打磨`
  return `当前有 ${issueCount} 个明显短板，建议先补齐关键表达`
})

const editorSections = computed<EditorSection[]>(() => {
  const content = resumeContent.value
  const basicInfo = content.basicInfo ?? {}
  const summary = typeof content.summary === 'string' ? content.summary : ''
  const workExperience = asArray<WorkExperienceItem>(content.workExperience)
  const education = asArray<EducationItem>(content.education)
  const projects = asArray<ProjectItem>(content.projects)
  const skills = asArray<string>(content.skills)
  const skillCategories = normalizedSkillCategories(content.skillCategories, skills)
  const certifications = asArray<CertificationItem>(content.certifications)
  const languages = asArray<LanguageItem>(content.languages)
  const githubProjects = asArray<GithubItem>(content.githubProjects)
  const qrCodes = asArray<QrCodeItem>(content.qrCodes)
  const customSections = asArray<CustomSectionItem>(content.customSections)
  const hiddenSections = asStringArray(content.hiddenSections)
  const activeSectionIds = resolveActiveSectionIds(content)
  const isVisible = (sectionId: string) => !hiddenSections.includes(sectionId)

  const sections: EditorSection[] = [
    {
      id: 'personal-info',
      type: 'personal_info',
      title: '个人信息',
      subtitle: '姓名、岗位、联系方式和基础求职信息',
      visible: isVisible('personal-info'),
      status: statusByFilledCount([
        basicInfo.name,
        basicInfo.targetRole,
        basicInfo.email,
        basicInfo.phone,
      ]),
      fields: [
        { key: 'basicInfo.name', label: '姓名', value: safeText(basicInfo.name) },
        { key: 'basicInfo.targetRole', label: '职位', value: safeText(basicInfo.targetRole) },
        { key: 'basicInfo.age', label: '年龄', value: safeText(basicInfo.age) },
        { key: 'basicInfo.gender', label: '性别', value: safeText(basicInfo.gender), control: 'select', options: genderOptions },
        { key: 'basicInfo.politicalStatus', label: '政治面貌', value: safeText(basicInfo.politicalStatus), control: 'select', options: politicalStatusOptions },
        { key: 'basicInfo.ethnicity', label: '民族', value: safeText(basicInfo.ethnicity) },
        { key: 'basicInfo.hometown', label: '籍贯', value: safeText(basicInfo.hometown) },
        { key: 'basicInfo.maritalStatus', label: '婚姻状况', value: safeText(basicInfo.maritalStatus), control: 'select', options: maritalStatusOptions },
        { key: 'basicInfo.yearsOfExperience', label: '工作年限', value: safeText(basicInfo.yearsOfExperience) },
        { key: 'basicInfo.educationLevel', label: '最高学历', value: safeText(basicInfo.educationLevel), control: 'select', options: educationLevelOptions },
        { key: 'basicInfo.email', label: '邮箱', value: safeText(basicInfo.email) },
        { key: 'basicInfo.phone', label: '电话', value: safeText(basicInfo.phone) },
        { key: 'basicInfo.wechat', label: '微信', value: safeText(basicInfo.wechat) },
        { key: 'basicInfo.location', label: '所在地', value: safeText(basicInfo.location) },
        { key: 'basicInfo.website', label: '个人主页', value: safeText(basicInfo.website) },
      ],
      chips: [],
      paragraphs: [],
      meta: 'personal_info',
    },
    {
      id: 'summary',
      type: 'summary',
      title: '个人简介',
      subtitle: '一段集中表达求职定位和核心优势的简介',
      visible: isVisible('summary'),
      status: summary.trim() ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [summary],
      paragraphLabels: ['个人简介'],
      meta: summary.trim() ? '已填写' : '待补充',
    },
    {
      id: 'work-experience',
      type: 'work_experience',
      title: '工作经历',
      subtitle: '公司、岗位、时间、职责和成果',
      visible: isVisible('work-experience'),
      status: workExperience.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: workExperience.map((item, index) => ({
        id: `work-${index}`,
        title: item.position || item.company || `工作经历 ${index + 1}`,
        description: safeText(item.description),
        descriptionKey: `workExperience.${index}.description`,
        descriptionLabel: '职责与主要成就',
        fields: [
          { key: `workExperience.${index}.company`, label: '公司', value: safeText(item.company) },
          { key: `workExperience.${index}.position`, label: '职位', value: safeText(item.position) },
          { key: `workExperience.${index}.location`, label: '地点', value: safeText(item.location) },
          { key: `workExperience.${index}.startDate`, label: '开始时间', value: safeText(item.startDate) },
          { key: `workExperience.${index}.endDate`, label: '结束时间', value: safeText(item.endDate) },
        ],
        listFields: [
          { key: `workExperience.${index}.technologies`, label: '技术栈', value: asEditableStringArray(item.technologies) },
          { key: `workExperience.${index}.highlights`, label: '亮点成果', value: asEditableStringArray(item.highlights) },
        ],
      })),
      addLabel: '添加工作经历',
      meta: `${workExperience.length} 条工作经历`,
    },
    {
      id: 'education',
      type: 'education',
      title: '教育背景',
      subtitle: '学历、学校、专业与时间线',
      visible: isVisible('education'),
      status: education.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: education.map((item, index) => ({
        id: `edu-${index}`,
        title: item.school || item.institution || `教育背景 ${index + 1}`,
        description: '',
        fields: [
          { key: `education.${index}.school`, label: '学校', value: safeText(item.school || item.institution) },
          { key: `education.${index}.major`, label: '专业', value: safeText(item.major || item.field) },
          { key: `education.${index}.degree`, label: '学历', value: safeText(item.degree), control: 'select', options: degreeOptions },
          { key: `education.${index}.gpa`, label: 'GPA', value: safeText(item.gpa) },
          { key: `education.${index}.period`, label: '时间', value: safeText(item.period || [item.startDate, item.endDate].filter(Boolean).join(' - ')) },
        ],
        listFields: [
          { key: `education.${index}.highlights`, label: '在校亮点', value: asEditableStringArray(item.highlights) },
        ],
      })),
      addLabel: '添加教育背景',
      meta: `${education.length} 条教育经历`,
    },
    {
      id: 'skills',
      type: 'skills',
      title: '技能特长',
      subtitle: '按类别维护技术栈和能力关键词',
      visible: isVisible('skills'),
      status: skills.length > 0 || skillCategories.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: skillCategories.map((category, index) => ({
        id: `skill-${index}`,
        title: category.name || `技能类别 ${index + 1}`,
        description: '',
        fields: [
          { key: `skillCategories.${index}.name`, label: '技能类别', value: safeText(category.name) },
        ],
        listFields: [
          { key: `skillCategories.${index}.skills`, label: '技能项', value: asEditableStringArray(category.skills) },
        ],
      })),
      addLabel: '添加技能类别',
      meta: `${skills.length || skillCategories.length} 个技能`,
    },
    {
      id: 'projects',
      type: 'projects',
      title: '项目经历',
      subtitle: '后续 AI 建议和岗位匹配最核心的事实来源',
      visible: isVisible('projects'),
      status: projectStatus(projects),
      fields: [],
      chips: [],
      paragraphs: projects.length
        ? projects.map((project) => project.description || '')
        : [],
      paragraphLabels: projects.map((project, index) => project.title || `项目 ${index + 1}`),
      items: projects.map((project, index) => ({
        id: String(project.evidenceId ?? `${project.title || 'project'}-${index}`),
        title: project.title || project.name || `项目 ${index + 1}`,
        description: safeText(project.description),
        descriptionKey: `projects.${index}.description`,
        descriptionLabel: '项目描述',
        fields: [
          { key: `projects.${index}.title`, label: '项目名称', value: safeText(project.title || project.name) },
        ],
        listFields: [
          { key: `projects.${index}.technologies`, label: '技术栈', value: asEditableStringArray(project.technologies) },
          { key: `projects.${index}.highlights`, label: '项目亮点', value: asEditableStringArray(project.highlights) },
        ],
      })),
      addLabel: '添加项目经历',
      meta: `${projects.length} 个项目`,
    },
    {
      id: 'certifications',
      type: 'certifications',
      title: '资格证书',
      subtitle: '证书、奖项和认证信息',
      visible: isVisible('certifications'),
      status: certifications.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: certifications.map((item, index) => ({
        id: `cert-${index}`,
        title: item.name || `证书 ${index + 1}`,
        description: safeText(item.description),
        descriptionKey: `certifications.${index}.description`,
        descriptionLabel: '说明',
        fields: [
          { key: `certifications.${index}.name`, label: '证书名称', value: safeText(item.name) },
          { key: `certifications.${index}.issuer`, label: '颁发机构', value: safeText(item.issuer) },
          { key: `certifications.${index}.date`, label: '获得时间', value: safeText(item.date) },
        ],
      })),
      addLabel: '添加证书',
      meta: `${certifications.length} 个证书`,
    },
    {
      id: 'languages',
      type: 'languages',
      title: '语言能力',
      subtitle: '语言、熟练度和证明',
      visible: isVisible('languages'),
      status: languages.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: languages.map((item, index) => ({
        id: `language-${index}`,
        title: item.name || `语言 ${index + 1}`,
        description: safeText(item.description),
        descriptionKey: `languages.${index}.description`,
        descriptionLabel: '说明',
        fields: [
          { key: `languages.${index}.name`, label: '语言', value: safeText(item.name) },
          { key: `languages.${index}.level`, label: '熟练度', value: safeText(item.level), control: 'select', options: languageLevelOptions },
        ],
      })),
      addLabel: '添加语言能力',
      meta: `${languages.length} 项语言能力`,
    },
    {
      id: 'github',
      type: 'github',
      title: 'GitHub 项目',
      subtitle: '开源项目、仓库链接和技术说明',
      visible: isVisible('github'),
      status: githubProjects.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: githubProjects.map((item, index) => ({
        id: `github-${index}`,
        title: item.name || `GitHub 项目 ${index + 1}`,
        description: safeText(item.description),
        descriptionKey: `githubProjects.${index}.description`,
        descriptionLabel: '项目说明',
        fields: [
          { key: `githubProjects.${index}.name`, label: '项目名称', value: safeText(item.name) },
          { key: `githubProjects.${index}.url`, label: '仓库链接', value: safeText(item.url) },
        ],
        listFields: [
          { key: `githubProjects.${index}.technologies`, label: '技术栈', value: asEditableStringArray(item.technologies) },
        ],
      })),
      addLabel: '添加 GitHub 项目',
      meta: `${githubProjects.length} 个 GitHub 项目`,
    },
    {
      id: 'qr-codes',
      type: 'qr_codes',
      title: '二维码',
      subtitle: '作品集、博客、GitHub 等二维码入口',
      visible: isVisible('qr-codes'),
      status: qrCodes.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: qrCodes.map((item, index) => ({
        id: `qr-${index}`,
        title: item.label || `二维码 ${index + 1}`,
        description: '',
        fields: [
          { key: `qrCodes.${index}.label`, label: '名称', value: safeText(item.label) },
          { key: `qrCodes.${index}.url`, label: '链接', value: safeText(item.url) },
        ],
      })),
      addLabel: '添加二维码',
      meta: `${qrCodes.length} 个二维码`,
    },
    {
      id: 'custom',
      type: 'custom',
      title: '自定义模块',
      subtitle: '补充其他能增强求职表达的内容',
      visible: isVisible('custom'),
      status: customSections.length > 0 ? 'ready' : 'empty',
      fields: [],
      chips: [],
      paragraphs: [],
      items: customSections.map((item, index) => ({
        id: `custom-${index}`,
        title: item.title || `自定义内容 ${index + 1}`,
        description: safeText(item.description),
        descriptionKey: `customSections.${index}.description`,
        descriptionLabel: '内容',
        fields: [
          { key: `customSections.${index}.title`, label: '标题', value: safeText(item.title) },
        ],
      })),
      addLabel: '添加自定义内容',
      meta: `${customSections.length} 个自定义内容`,
    },
  ]

  const sectionById = new Map(sections.map((section) => [section.id, section]))
  return activeSectionIds
    .map((sectionId) => sectionById.get(sectionId))
    .filter((section): section is EditorSection => Boolean(section))
})

const availableModules = computed(() => {
  const activeIds = new Set(editorSections.value.map((section) => section.id))
  return moduleCatalog.filter((module) => !activeIds.has(module.id))
})

onMounted(() => {
  restoreSelectedTemplate()
  if (route.query.editor === '1' || consumeReturnToEditor()) {
    editorActive.value = true
  }
  window.addEventListener('keydown', handleEditorShortcut)
  loadEditorWorkbench()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEditorShortcut)
  window.removeEventListener('pointermove', handleLayoutAssistantDrag)
  window.removeEventListener('pointerup', stopLayoutAssistantDrag)
})

function continueResumeEditing() {
  if (!ensureTargetJobSelected('continue')) return
  blankResumeMode.value = false
  syncDraftFromVersion()
  enterEditor()
}

function toggleLayoutAssistantOpen() {
  if (layoutAssistantWasDragged.value) {
    layoutAssistantWasDragged.value = false
    return
  }
  const nextOpen = !layoutAssistantOpen.value
  layoutAssistantOpen.value = nextOpen
  if (nextOpen) {
    layoutProposal.value = null
    void generateLayoutProposal()
  }
}

function startLayoutAssistantDrag(event: PointerEvent) {
  if (event.button !== 0) return
  layoutAssistantDragOffset.value = {
    x: event.clientX - layoutAssistantPosition.value.x,
    y: event.clientY - layoutAssistantPosition.value.y,
  }
  layoutAssistantWasDragged.value = false
  window.addEventListener('pointermove', handleLayoutAssistantDrag)
  window.addEventListener('pointerup', stopLayoutAssistantDrag, { once: true })
}

function handleLayoutAssistantDrag(event: PointerEvent) {
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const nextX = clampNumber(event.clientX - layoutAssistantDragOffset.value.x, 12, viewportWidth - 38)
  const nextY = clampNumber(event.clientY - layoutAssistantDragOffset.value.y, 64, viewportHeight - 38)
  if (Math.abs(nextX - layoutAssistantPosition.value.x) > 2 || Math.abs(nextY - layoutAssistantPosition.value.y) > 2) {
    layoutAssistantWasDragged.value = true
  }
  layoutAssistantPosition.value = { x: nextX, y: nextY }
}

function stopLayoutAssistantDrag() {
  window.removeEventListener('pointermove', handleLayoutAssistantDrag)
}

function startBlankResume() {
  selectedResumeId.value = null
  selectedVersionId.value = null
  setSelectedTargetJobId(null)
  window.sessionStorage.setItem(pendingWorkspaceActionKey, 'blank')
  ElMessage.info('请先为新简历选择一个目标岗位')
  openJobLibrary()
}

function openResumeFromLaunch(resumeId: number) {
  if (!resumeId) return
  selectResumeContext(resumeId)
  if (!selectedJob.value) {
    ensureTargetJobSelected('continue')
    return
  }
  enterEditor()
}

function selectResumeContext(resumeId: number, versionId?: number) {
  const resume = resumes.value.find((item) => item.id === resumeId)
  if (!resume) return
  blankResumeMode.value = false
  selectedResumeId.value = resumeId
  saveLastResumeId(resumeId)
  setWorkspaceSelectedResumeId(resumeId)
  versions.value = resumeVersionsById.value[resumeId] ?? []
  selectedVersionId.value = versionId
    ?? resume.currentVersion?.id
    ?? versions.value[0]?.id
    ?? null
  setSelectedTargetJobId(resume.targetJobDescriptionId ?? null, { preserveJobArtifacts: true })
  assessmentResult.value = null
  assessmentError.value = ''
  jobMatch.value = null
  recommendResults.value = []
  recommendSourceVersionId.value = null
  recommendError.value = ''
  suggestions.value = []
  syncDraftFromVersion()
}

function ensureTargetJobSelected(pendingAction: 'continue' | 'blank' = 'continue') {
  if (selectedJob.value) return true
  window.sessionStorage.setItem(pendingWorkspaceActionKey, pendingAction)
  ElMessage.info('请先选择目标岗位，再开始书写简历')
  openJobLibrary()
  return false
}

function enterEditor() {
  editorActive.value = true
  rightPanelMode.value = 'preview'
  templateLibraryOpen.value = false
  window.sessionStorage.setItem('resumego:lastWorkspace', 'editor')
}

function buildEditorRouteQuery() {
  const query: Record<string, string> = { from: 'editor' }
  if (blankResumeMode.value) {
    query.mode = 'blank'
    return query
  }
  if (selectedResumeId.value) {
    query.resumeId = String(selectedResumeId.value)
  }
  if (selectedVersionId.value) {
    query.versionId = String(selectedVersionId.value)
  }
  return query
}

function readPositiveRouteNumber(key: string): number | null {
  const rawValue = route.query[key]
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null
}

function isRouteBlankWorkspaceReturn() {
  const modeValue = route.query.mode
  const mode = Array.isArray(modeValue) ? modeValue[0] : modeValue
  return mode === 'blank'
}

function applyPendingWorkspaceAction() {
  const pendingAction = window.sessionStorage.getItem(pendingWorkspaceActionKey)
  if (pendingAction) {
    window.sessionStorage.removeItem(pendingWorkspaceActionKey)
  }
  if (pendingAction === 'blank') {
    blankResumeMode.value = true
    selectedResumeId.value = null
    selectedVersionId.value = null
    syncBlankDraft()
    enterEditor()
    return
  }
  if (pendingAction === 'continue') {
    blankResumeMode.value = false
    syncDraftFromVersion()
    enterEditor()
  }
}

function openTemplateLibrary() {
  rightPanelMode.value = 'preview'
  templateLibraryOpen.value = true
}

function toggleTemplateLibrary() {
  rightPanelMode.value = 'preview'
  templateLibraryOpen.value = !templateLibraryOpen.value
}

function closeTemplateLibrary() {
  templateLibraryOpen.value = false
}

function showAiPanel() {
  templateLibraryOpen.value = false
  rightPanelMode.value = 'ai'
}

function selectTemplate(templateKey: string) {
  if (!isValidResumeTemplateKey(templateKey)) return
  selectedTemplate.value = templateKey
  window.localStorage.setItem(templateStorageKey, templateKey)
  rightPanelMode.value = 'preview'
  templateLibraryOpen.value = false
}

function restoreSelectedTemplate() {
  const storedTemplate = window.localStorage.getItem(templateStorageKey)
  selectedTemplate.value = storedTemplate && isValidResumeTemplateKey(storedTemplate)
    ? storedTemplate
    : defaultResumeTemplateKey
}

function exitEditor() {
  if (isDirty.value) {
    ElMessage.warning('当前有未保存草稿，请先保存为新版本或放弃修改')
    return
  }
  templateLibraryOpen.value = false
  blankResumeMode.value = false
  syncDraftFromVersion()
  editorActive.value = false
}

function switchVersion(versionId: number) {
  if (!versionId || versionId === selectedVersionId.value) return
  if (isDirty.value) {
    ElMessage.warning('当前有未保存草稿，请先保存为新版本或放弃修改')
    return
  }
  templateLibraryOpen.value = false
  blankResumeMode.value = false
  selectedVersionId.value = versionId
  selectedSectionId.value = 'personal-info'
  rightPanelMode.value = 'preview'
  jobMatch.value = null
  suggestions.value = []
  recommendResults.value = []
  recommendSourceVersionId.value = null
  recommendError.value = ''
  assessmentResult.value = null
  assessmentError.value = ''
}

function handleEditorShortcut(event: KeyboardEvent) {
  if (!editorActive.value) return
  if (event.key === 'Escape' && templateLibraryOpen.value) {
    event.preventDefault()
    templateLibraryOpen.value = false
    return
  }
  const withModifier = event.metaKey || event.ctrlKey
  if (!withModifier) return

  const key = event.key.toLowerCase()
  if (key === 'z') {
    event.preventDefault()
    if (event.shiftKey) {
      redoDraft()
    } else {
      undoDraft()
    }
    return
  }
  if (key === 'y') {
    event.preventDefault()
    redoDraft()
    return
  }
  if (key === 's') {
    event.preventDefault()
    if (!savingDraft.value) {
      void saveDraftAsVersion()
    }
    return
  }
  if (key === '1') {
    event.preventDefault()
    templateLibraryOpen.value = false
    rightPanelMode.value = 'preview'
    return
  }
  if (key === '2') {
    event.preventDefault()
    showAiPanel()
  }
}

watch(
  () => activeVersion.value?.id,
  () => {
    if (blankResumeMode.value) return
    syncDraftFromVersion()
  },
)

watch(
  () => selectedJob.value?.companyName,
  (companyName) => {
    void loadSelectedCompanyProfile(companyName)
  },
  { immediate: true },
)

watch(
  () => JSON.stringify(draftContent.value),
  (nextSnapshot, previousSnapshot) => {
    if (suppressDraftHistory.value) {
      suppressDraftHistory.value = false
      return
    }
    if (!previousSnapshot || previousSnapshot === nextSnapshot) return
    undoStack.value = [...undoStack.value, previousSnapshot].slice(-maxDraftHistory)
    redoStack.value = []
  },
)

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

async function loadEditorWorkbench() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [resumeRes, jobRes] = await Promise.all([
      listResumes(),
      listJobDescriptions(),
    ])

    resumes.value = resumeRes.data
    jobs.value = jobRes.data

    const versionEntries = await Promise.all(
      resumeRes.data.map(async (resume) => {
        const versionRes = await getResumeVersions(resume.id)
        return [resume.id, versionRes.data] as const
      }),
    )
    resumeVersionsById.value = Object.fromEntries(versionEntries)
    await migrateLegacyResumeJobBindings(jobRes.data)

    const routeResumeId = readPositiveRouteNumber('resumeId')
    const routeVersionId = readPositiveRouteNumber('versionId')
    const preferredResumeId = selectedResumeId.value ?? routeResumeId ?? loadLastResumeId()
    const firstResume = resumes.value.find((resume) => resume.id === preferredResumeId)
      ?? resumes.value[resumes.value.length - 1]
      ?? resumes.value[0]
    if (firstResume?.id) {
      const versionBelongsToResume = routeVersionId
        ? (resumeVersionsById.value[firstResume.id] ?? []).some((version) => version.id === routeVersionId)
        : false
      selectResumeContext(firstResume.id, versionBelongsToResume ? routeVersionId! : undefined)
    }

    const pendingAction = window.sessionStorage.getItem(pendingWorkspaceActionKey)
    const pendingJobId = consumePendingWorkspaceSelectedJobId()
    const pendingJob = jobRes.data.find((job) => job.id === pendingJobId)
    if (pendingJob?.id) {
      setSelectedTargetJobId(pendingJob.id)
      if (selectedResumeId.value && pendingAction !== 'blank') {
        await bindSelectedJobToResume(selectedResumeId.value)
      }
      if (pendingAction || isRouteBlankWorkspaceReturn()) {
        applyPendingWorkspaceAction()
      }
    } else if (pendingAction) {
      ElMessage.info('请先选择目标岗位，再开始书写简历')
      openJobLibrary()
    } else if (selectedJobId.value) {
      setWorkspaceSelectedJobId(selectedJobId.value)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载编辑器工作台失败'
  } finally {
    loading.value = false
  }
}

function selectSection(sectionId: string) {
  selectedSectionId.value = sectionId
  requestAnimationFrame(() => {
    document
      .querySelector(`[data-section-id="${sectionId}"]`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    document
      .querySelector(`[data-preview-section-id="${sectionId}"]`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

function updateDraftField(_sectionId: string, fieldKey: string, value: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (fieldKey.startsWith('skillCategories.')) {
    ensureSkillCategories(next)
  }
  setByPath(next, fieldKey, normalizeDraftFieldValue(fieldKey, value))
  draftContent.value = next
}

function updateDraftParagraph(sectionId: string, index: number, value: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (sectionId === 'summary') {
    next.summary = value
    draftContent.value = next
    return
  }
  if (sectionId === 'projects') {
    const projects = [...(next.projects ?? [])]
    const current = { ...(projects[index] ?? {}) }
    current.description = value
    projects[index] = current
    next.projects = projects
  }
  draftContent.value = next
}

function updateDraftListItem(_sectionId: string, fieldKey: string, index: number, value: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (fieldKey.startsWith('skillCategories.')) {
    ensureSkillCategories(next)
  }
  const list = listByPath(next, fieldKey)
  list[index] = value
  setByPath(next, fieldKey, list)
  if (fieldKey.startsWith('skillCategories.')) {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
}

function addDraftListItem(_sectionId: string, fieldKey: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (fieldKey.startsWith('skillCategories.')) {
    ensureSkillCategories(next)
  }
  const list = listByPath(next, fieldKey)
  list.push('')
  setByPath(next, fieldKey, list)
  draftContent.value = next
  focusListInput(fieldKey, list.length - 1)
}

function removeDraftListItem(_sectionId: string, fieldKey: string, index: number) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (fieldKey.startsWith('skillCategories.')) {
    ensureSkillCategories(next)
  }
  const list = listByPath(next, fieldKey)
  list.splice(index, 1)
  setByPath(next, fieldKey, list)
  if (fieldKey.startsWith('skillCategories.')) {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
}

function moveDraftListItem(_sectionId: string, fieldKey: string, index: number, direction: 'up' | 'down') {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  if (fieldKey.startsWith('skillCategories.')) {
    ensureSkillCategories(next)
  }
  const list = listByPath(next, fieldKey)
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= list.length) return
  const [current] = list.splice(index, 1)
  list.splice(targetIndex, 0, current)
  setByPath(next, fieldKey, list)
  if (fieldKey.startsWith('skillCategories.')) {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
}

function updateDraftChips(sectionId: string, value: string) {
  if (sectionId !== 'skills') return
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  next.skills = parseEditableList(value)
  draftContent.value = next
}

function addDraftItem(sectionId: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const collectionKey = collectionKeyBySection(sectionId)
  if (!collectionKey) return
  const current = asArray<Record<string, unknown>>(next[collectionKey])
  next[collectionKey] = [...current, createEmptyItem(sectionId)]
  if (sectionId === 'skills') {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
  focusFirstFieldInItem(collectionKey, current.length)
}

function removeDraftItem(sectionId: string, index: number) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const collectionKey = collectionKeyBySection(sectionId)
  if (!collectionKey) return
  const collection = asArray<Record<string, unknown>>(next[collectionKey])
  collection.splice(index, 1)
  next[collectionKey] = collection
  if (sectionId === 'skills') {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
}

function moveDraftItem(sectionId: string, index: number, direction: 'up' | 'down') {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const collectionKey = collectionKeyBySection(sectionId)
  if (!collectionKey) return
  const collection = asArray<Record<string, unknown>>(next[collectionKey])
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= collection.length) return
  const [current] = collection.splice(index, 1)
  collection.splice(targetIndex, 0, current)
  next[collectionKey] = collection
  if (sectionId === 'skills') {
    next.skills = flattenSkillCategories(next.skillCategories)
  }
  draftContent.value = next
}

function toggleSectionVisibility(sectionId: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const hidden = new Set(asStringArray(next.hiddenSections))
  if (hidden.has(sectionId)) {
    hidden.delete(sectionId)
  } else {
    hidden.add(sectionId)
  }
  next.hiddenSections = Array.from(hidden)
  draftContent.value = next
}

function runLayoutAssistantAction(actionId: LayoutAssistantAction['id']) {
  if (actionId === 'focus-longest') {
    const target = longestLayoutSection(editorSections.value)
    if (target) {
      layoutAssistantOpen.value = false
      selectSection(target.id)
      ElMessage.info(`已定位到「${target.title}」`)
    }
    return
  }

  if (actionId === 'hide-empty-sections') {
    const sections = emptyVisibleSections.value
    if (!sections.length) return
    const next = cloneContent(draftContent.value)
    const hidden = new Set(asStringArray(next.hiddenSections))
    sections.forEach((section) => hidden.add(section.id))
    next.hiddenSections = Array.from(hidden)
    draftContent.value = next
    rightPanelMode.value = 'preview'
    layoutAssistantOpen.value = false
    ElMessage.success(`已隐藏 ${sections.length} 个空模块，保存后进入新版本`)
  }
}

async function generateLayoutProposal() {
  if (layoutProposalLoading.value) return

  layoutProposalLoading.value = true
  try {
    const response = await createLayoutProposal({
      resumeVersionId: activeVersion.value?.id ?? null,
      draftContent: cloneContent(draftContent.value),
      targetJobDescriptionId: selectedJobId.value,
      targetJob: layoutTargetJobPayload(),
      templateKey: selectedTemplate.value,
      goal: 'compress_wording_and_improve_layout_density',
    })
    layoutProposal.value = {
      ...response.data,
      changes: response.data.changes ?? [],
      hiddenSectionIds: response.data.hiddenSectionIds ?? [],
      warnings: response.data.warnings ?? [],
    }

    const hasProposal =
      layoutProposal.value.changes.length > 0 ||
      Boolean(layoutProposal.value.templateKey) ||
      layoutProposal.value.hiddenSectionIds.length > 0
    if (!hasProposal) {
      ElMessage.info(layoutProposal.value.warnings[0] || '当前没有明显需要自动调整的排版项')
      layoutProposal.value = null
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '生成 AI 排版提案失败'
    ElMessage.error(message)
  } finally {
    layoutProposalLoading.value = false
  }
}

function applyLayoutProposal() {
  if (!layoutProposal.value) return
  const proposal = layoutProposal.value
  const next = cloneContent(draftContent.value)
  proposal.changes.forEach((change) => {
    setByPath(next, change.fieldKey, normalizeLayoutAfterValue(next, change.fieldKey, change.after))
  })
  if (proposal.hiddenSectionIds.length) {
    const hidden = new Set(asStringArray(next.hiddenSections))
    proposal.hiddenSectionIds.forEach((sectionId) => hidden.add(sectionId))
    next.hiddenSections = Array.from(hidden)
  }
  draftContent.value = next
  rightPanelMode.value = 'preview'
  if (proposal.templateKey) {
    selectTemplate(proposal.templateKey)
  }
  layoutProposal.value = null
  layoutAssistantOpen.value = false
  ElMessage.success('已将排版提案应用到当前草稿，确认后可保存为新版本')
}

function discardLayoutProposal() {
  layoutProposal.value = null
  ElMessage.info('已放弃本次排版提案')
}

function normalizeLayoutAfterValue(content: ResumeContent, fieldKey: string, after: string) {
  const currentValue = getByPath(content, fieldKey)
  if (Array.isArray(currentValue)) {
    return after
      .split(/[、，,;\n；]+/)
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return after
}


function addResumeModule(sectionId: string) {
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const active = resolveActiveSectionIds(next)
  next.activeSections = active.includes(sectionId) ? active : [...active, sectionId]
  const hidden = new Set(asStringArray(next.hiddenSections))
  hidden.delete(sectionId)
  next.hiddenSections = Array.from(hidden)
  draftContent.value = next
  selectedSectionId.value = sectionId
}

function removeResumeModule(sectionId: string) {
  if (sectionId === 'personal-info') return
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  next.activeSections = resolveActiveSectionIds(next).filter((id) => id !== sectionId)
  draftContent.value = next
  if (selectedSectionId.value === sectionId) {
    selectedSectionId.value = next.activeSections[0] || 'personal-info'
  }
}

function moveResumeModule(sectionId: string, direction: 'up' | 'down') {
  if (sectionId === 'personal-info') return
  rightPanelMode.value = 'preview'
  const next = cloneContent(draftContent.value)
  const active = resolveActiveSectionIds(next)
  const index = active.indexOf(sectionId)
  if (index < 0) return
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex <= 0 || targetIndex >= active.length) return
  const reordered = [...active]
  const [current] = reordered.splice(index, 1)
  reordered.splice(targetIndex, 0, current)
  next.activeSections = reordered
  draftContent.value = next
}

function resetDraft() {
  if (blankResumeMode.value) {
    syncBlankDraft()
    ElMessage.info('已恢复为空白新简历')
    return
  }
  syncDraftFromVersion()
  ElMessage.info('已恢复到当前版本内容')
}

function undoDraft() {
  const previousSnapshot = undoStack.value[undoStack.value.length - 1]
  if (!previousSnapshot) return
  undoStack.value = undoStack.value.slice(0, -1)
  redoStack.value = [...redoStack.value, JSON.stringify(draftContent.value)].slice(-maxDraftHistory)
  suppressDraftHistory.value = true
  draftContent.value = JSON.parse(previousSnapshot) as ResumeContent
}

function redoDraft() {
  const nextSnapshot = redoStack.value[redoStack.value.length - 1]
  if (!nextSnapshot) return
  redoStack.value = redoStack.value.slice(0, -1)
  undoStack.value = [...undoStack.value, JSON.stringify(draftContent.value)].slice(-maxDraftHistory)
  suppressDraftHistory.value = true
  draftContent.value = JSON.parse(nextSnapshot) as ResumeContent
}

function openJobLibrary() {
  templateLibraryOpen.value = false
  markReturnToEditor()
  router.push({ name: 'jobs', query: buildEditorRouteQuery() })
}

function markReturnToEditor() {
  markWorkspaceReturnToEditor()
}

function viewSelectedJobDetail() {
  if (!selectedJob.value) return
  templateLibraryOpen.value = false
  markReturnToEditor()
  router.push({
    name: 'job-detail',
    params: { id: selectedJob.value.id },
    query: buildEditorRouteQuery(),
  })
}

async function saveDraftAsVersion() {
  if (blankResumeMode.value) {
    await saveBlankDraftAsNewResume()
    return
  }
  if (!activeResume.value?.id || !activeVersion.value) {
    ElMessage.info('请先选择简历版本')
    return
  }
  if (!isDirty.value) return

  savingDraft.value = true
  errorMessage.value = ''
  try {
    const res = await createResumeVersion(activeResume.value.id, {
      content: draftContent.value,
      changeSummary: blankResumeMode.value
        ? `新建空白简历：针对「${selectedJobLabel.value}」创建`
        : `人工编辑：基于 v${activeVersion.value.versionNo} 保存编辑器草稿`,
    })
    const versionRes = await getResumeVersions(activeResume.value.id)
    versions.value = versionRes.data
    resumeVersionsById.value = {
      ...resumeVersionsById.value,
      [activeResume.value.id]: versionRes.data,
    }
    selectedVersionId.value = res.data.id
    blankResumeMode.value = false
    await bindSelectedJobToResume(activeResume.value.id)
    suppressDraftHistory.value = true
    draftContent.value = cloneContent(res.data.content)
    baseContentSnapshot.value = snapshotResumeContent(draftContent.value)
    undoStack.value = []
    redoStack.value = []
    ElMessage.success(`已保存为新版本 v${res.data.versionNo}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存简历新版本失败'
  } finally {
    savingDraft.value = false
  }
}

async function saveBlankDraftAsNewResume() {
  if (!selectedJob.value) {
    ElMessage.info('请先选择目标岗位')
    openJobLibrary()
    return
  }
  if (!hasMeaningfulResumeContent(draftContent.value)) {
    ElMessage.info('请先填写姓名、目标岗位、简介、教育、项目或技能等至少一项内容')
    return
  }

  savingDraft.value = true
  errorMessage.value = ''
  try {
    const res = await createResume({
      title: deriveResumeTitle(draftContent.value),
      content: draftContent.value,
      changeSummary: `新建简历：针对「${selectedJobLabel.value}」创建`,
      targetJobDescriptionId: selectedJob.value.id,
    })
    const resumeRes = await listResumes()
    resumes.value = resumeRes.data
    const versionRes = await getResumeVersions(res.data.id)
    resumeVersionsById.value = {
      ...resumeVersionsById.value,
      [res.data.id]: versionRes.data,
    }
    selectedResumeId.value = res.data.id
    saveLastResumeId(res.data.id)
    versions.value = versionRes.data
    selectedVersionId.value = res.data.currentVersion?.id ?? versionRes.data[0]?.id ?? null
    blankResumeMode.value = false
    rememberResumeJobBinding(res.data.id, selectedJob.value.id)
    replaceResumeInList(res.data)
    suppressDraftHistory.value = true
    draftContent.value = cloneContent(activeVersion.value?.content ?? res.data.currentVersion?.content ?? draftContent.value)
    baseContentSnapshot.value = snapshotResumeContent(draftContent.value)
    undoStack.value = []
    redoStack.value = []
    ElMessage.success(`已创建新简历 ${res.data.title}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建新简历失败'
  } finally {
    savingDraft.value = false
  }
}

async function generateEditorSuggestions() {
  templateLibraryOpen.value = false
  if (!ensureSavedResumeForDownstreamAction('生成 JD 匹配建议')) return
  if (!activeVersion.value || !selectedJob.value) {
    ElMessage.info('请先选择简历版本和目标岗位')
    return
  }
  if (isDirty.value) {
    ElMessage.warning('当前有未保存草稿，请先保存为新版本后再生成建议')
    return
  }
  if (selectedJob.value.parseStatus !== 'succeeded') {
    ElMessage.warning('当前 JD 尚未结构化解析，请先到岗位详情页解析')
    markReturnToEditor()
    router.push({
      name: 'job-detail',
      params: { id: selectedJob.value.id },
      query: buildEditorRouteQuery(),
    })
    return
  }

  suggestionLoading.value = true
  errorMessage.value = ''
  try {
    const match = await createJobMatch({
      resumeVersionId: activeVersion.value.id,
      jobDescriptionId: selectedJob.value.id,
    })
    jobMatch.value = match

    if (!match.id) {
      throw new Error('岗位匹配结果缺少 matchId，无法生成 AI 建议')
    }

    const res = await generateSuggestionsWithAssessment(match.id)
    suggestions.value = res.data.suggestions
    rightPanelMode.value = 'ai'
    ElMessage.success('项目技术表达建议已生成')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '生成 AI 建议失败'
  } finally {
    suggestionLoading.value = false
  }
}

function openAssessment() {
  templateLibraryOpen.value = false
  if (!ensureSavedResumeForDownstreamAction('进行简历评分')) return
  if (!activeVersion.value) {
    ElMessage.info('请先准备一个简历版本')
    return
  }
  assessmentDialogOpen.value = true
  if (!assessmentResult.value) {
    void runAssessmentInDialog()
  }
}

async function runAssessmentInDialog() {
  if (!activeVersion.value) return
  assessmentLoading.value = true
  assessmentError.value = ''
  try {
    const res = await assessResumeVersion(activeVersion.value.id)
    assessmentResult.value = res.data
  } catch (error) {
    assessmentError.value = error instanceof Error ? error.message : '生成简历评分失败'
  } finally {
    assessmentLoading.value = false
  }
}

function openJobRecommendations() {
  templateLibraryOpen.value = false
  if (!ensureSavedResumeForDownstreamAction('查看岗位推荐')) return
  if (!activeVersion.value) {
    ElMessage.info('请先准备一个简历版本')
    return
  }
  recommendDialogOpen.value = true
  if (!recommendResults.value.length || recommendSourceVersionId.value !== activeVersion.value.id) {
    void runRecommendInDialog()
  }
}

async function runRecommendInDialog() {
  if (!activeVersion.value) return
  const versionId = activeVersion.value.id
  recommendLoading.value = true
  recommendError.value = ''
  try {
    const res = await batchMatch(versionId, 5)
    recommendResults.value = res.matches
    recommendSourceVersionId.value = versionId
  } catch (error) {
    recommendError.value = error instanceof Error ? error.message : '岗位匹配失败'
  } finally {
    recommendLoading.value = false
  }
}

function resolveRecommendJob(id: number) {
  return jobs.value.find((job) => job.id === id) ?? null
}

function resolveJobTitle(id: number) {
  return jobs.value.find(j => j.id === id)?.jobTitle ?? '未知岗位'
}

function resolveJobCompany(id: number) {
  return jobs.value.find(j => j.id === id)?.companyName ?? '未知公司'
}

function recommendMetaLine(id: number) {
  const job = resolveRecommendJob(id)
  if (!job) return '未知公司'
  return [
    resolveJobCompany(id),
    job.sourceMeta?.base,
    job.sourceMeta?.industry,
    job.sourceMeta?.salary,
  ].filter(Boolean).join(' · ')
}

function recommendPreviewSkills(id: number) {
  const job = resolveRecommendJob(id)
  return [
    ...(job?.parsed?.requiredSkills ?? []),
  ].filter(Boolean).slice(0, 5)
}

async function selectRecommendedJob(jobId: number) {
  const job = resolveRecommendJob(jobId)
  if (!job) return
  setSelectedTargetJobId(jobId)
  if (!activeResume.value?.id) {
    ElMessage.success(`已将「${job.jobTitle}」设为当前目标岗位`)
    return
  }
  try {
    await bindSelectedJobToResume(activeResume.value.id)
    ElMessage.success(`已将「${job.jobTitle}」设为当前目标岗位`)
  } catch (error) {
    recommendError.value = error instanceof Error ? error.message : '更新目标岗位失败'
  }
}

function viewRecommendJobDetail(jobId: number) {
  recommendDialogOpen.value = false
  markReturnToEditor()
  router.push({
    name: 'job-detail',
    params: { id: jobId },
    query: buildEditorRouteQuery(),
  })
}

function rankClass(score: number) {
  if (score >= 80) return 'rank-high'
  if (score >= 60) return 'rank-mid'
  if (score >= 40) return 'rank-low'
  return 'rank-none'
}

function startInterviewPreparation() {
  templateLibraryOpen.value = false
  if (!ensureSavedResumeForDownstreamAction('进入模拟面试')) return
  if (!activeVersion.value || !selectedJob.value) {
    ElMessage.info('请先选择简历版本和目标岗位')
    return
  }
  markReturnToEditor()
  router.push({
    name: 'interview',
    query: {
      versionId: activeVersion.value.id,
      jobId: selectedJob.value.id,
      from: 'editor',
    },
  })
}

function ensureSavedResumeForDownstreamAction(actionLabel: string) {
  if (!selectedJob.value) {
    ElMessage.info('请先选择目标岗位')
    openJobLibrary()
    return false
  }
  if (blankResumeMode.value || isDirty.value) {
    ElMessage.warning(`请先保存当前简历草稿，再${actionLabel}`)
    return false
  }
  return true
}

function resumeRecentVersions(resume: Resume) {
  return resumeVersionsById.value[resume.id] ?? []
}

function resumeLatestTimestamp(resume: Resume) {
  const versionTimes = resumeRecentVersions(resume)
    .map((version) => Date.parse(version.createdAt))
    .filter(Number.isFinite)
  const currentVersionTime = Date.parse(resume.currentVersion?.createdAt ?? '')
  if (Number.isFinite(currentVersionTime)) {
    versionTimes.push(currentVersionTime)
  }
  const resumeUpdatedAt = Date.parse(resume.updatedAt ?? '')
  if (Number.isFinite(resumeUpdatedAt)) {
    versionTimes.push(resumeUpdatedAt)
  }
  return Math.max(...versionTimes, resume.id || 0)
}

function resumeCurrentVersion(resume: Resume) {
  const cached = resumeRecentVersions(resume)
  return cached.find((version) => version.id === resume.currentVersion?.id) ?? resume.currentVersion ?? cached[0] ?? null
}

function resumePreviewName(resume: Resume) {
  const version = resumeCurrentVersion(resume)
  const basicInfo = version?.content.basicInfo
  return basicInfo?.name?.trim() || resume.title || '未命名'
}

function resumeVersionSummary(resume: Resume) {
  const current = resumeCurrentVersion(resume)
  const total = resumeRecentVersions(resume).length
  if (!current) return '暂无版本'
  return `最新 v${current.versionNo} · 共 ${total || 1} 个版本`
}

function resumeJobLabel(resume: Resume) {
  const jobId = resume.targetJobDescriptionId
  const job = jobs.value.find((item) => item.id === jobId)
  if (!job) return '未绑定目标岗位'
  return job.companyName ? `${job.jobTitle}｜${job.companyName}` : job.jobTitle
}

function rememberResumeJobBinding(resumeId: number, jobId: number) {
  if (!resumeId || !jobId) return
  resumes.value = resumes.value.map((resume) => (
    resume.id === resumeId
      ? { ...resume, targetJobDescriptionId: jobId }
      : resume
  ))
}

async function bindSelectedJobToResume(resumeId: number) {
  if (!resumeId || !selectedJobId.value) return
  const targetJobId = selectedJobId.value
  const res = await updateResumeTargetJob(resumeId, {
    targetJobDescriptionId: targetJobId,
  })
  rememberResumeJobBinding(resumeId, targetJobId)
  replaceResumeInList(res.data)
}

function setSelectedTargetJobId(
  jobId: number | null,
  options: { preserveJobArtifacts?: boolean } = {},
) {
  const normalizedJobId = Number.isFinite(jobId) && Number(jobId) > 0 ? Number(jobId) : null
  const changed = selectedJobId.value !== normalizedJobId
  selectedJobId.value = normalizedJobId
  if (normalizedJobId) {
    setWorkspaceSelectedJobId(normalizedJobId)
  } else {
    clearWorkspaceSelectedJobId()
  }
  if (changed && !options.preserveJobArtifacts) {
    jobMatch.value = null
    suggestions.value = []
    assessmentResult.value = null
    assessmentError.value = ''
  }
}

function replaceResumeInList(nextResume: Resume) {
  resumes.value = resumes.value.map((resume) => (
    resume.id === nextResume.id ? nextResume : resume
  ))
}

function loadLastResumeId() {
  const resumeId = Number(window.localStorage.getItem(lastResumeIdStorageKey))
  return Number.isFinite(resumeId) && resumeId > 0 ? resumeId : null
}

function saveLastResumeId(resumeId: number) {
  if (!Number.isFinite(resumeId) || resumeId <= 0) return
  window.localStorage.setItem(lastResumeIdStorageKey, String(resumeId))
}

function loadLegacyResumeJobMap(): Record<number, number> {
  try {
    const raw = window.localStorage.getItem(legacyResumeJobMapStorageKey)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, unknown>
    return Object.fromEntries(
      Object.entries(parsed)
        .map(([resumeId, jobId]) => [Number(resumeId), Number(jobId)] as const)
        .filter(([resumeId, jobId]) => (
          Number.isFinite(resumeId)
          && Number.isFinite(jobId)
          && resumeId > 0
          && jobId > 0
        )),
    )
  } catch {
    return {}
  }
}

function persistLegacyResumeJobMap(value: Record<number, number>) {
  if (Object.keys(value).length === 0) {
    window.localStorage.removeItem(legacyResumeJobMapStorageKey)
    return
  }
  window.localStorage.setItem(legacyResumeJobMapStorageKey, JSON.stringify(value))
}

async function migrateLegacyResumeJobBindings(availableJobs: JobDescription[]) {
  const legacyMap = loadLegacyResumeJobMap()
  if (Object.keys(legacyMap).length === 0) return

  const validJobIds = new Set(availableJobs.map((job) => job.id))
  const remainingLegacyMap = { ...legacyMap }
  let changed = false

  for (const resume of resumes.value) {
    const legacyJobId = legacyMap[resume.id]
    if (!legacyJobId) continue

    if (resume.targetJobDescriptionId || !validJobIds.has(legacyJobId)) {
      delete remainingLegacyMap[resume.id]
      changed = true
      continue
    }

    try {
      const res = await updateResumeTargetJob(resume.id, {
        targetJobDescriptionId: legacyJobId,
      })
      replaceResumeInList(res.data)
      delete remainingLegacyMap[resume.id]
      changed = true
    } catch {
      // 旧数据迁移失败时保留本地映射，避免直接吞掉用户历史上下文。
    }
  }

  if (changed) {
    persistLegacyResumeJobMap(remainingLegacyMap)
  }
}

function deriveResumeTitle(content: ResumeContent) {
  const name = content.basicInfo?.name?.trim()
  const role = content.basicInfo?.title?.trim() || content.basicInfo?.targetRole?.trim()
  if (name && role) return `${name}的${role}简历`
  if (role) return `${role}简历`
  if (selectedJob.value?.jobTitle) return `${selectedJob.value.jobTitle}简历`
  return '未命名简历'
}

function hasMeaningfulResumeContent(content: ResumeContent) {
  const ignoredKeys = new Set(['activeSections', 'hiddenSections'])
  const hasMeaningfulValue = (value: unknown): boolean => {
    if (typeof value === 'string') return value.trim().length > 0
    if (typeof value === 'number' || typeof value === 'boolean') return true
    if (Array.isArray(value)) return value.some(hasMeaningfulValue)
    if (value && typeof value === 'object') {
      return Object.entries(value as Record<string, unknown>)
        .some(([key, childValue]) => !ignoredKeys.has(key) && hasMeaningfulValue(childValue))
    }
    return false
  }
  return hasMeaningfulValue(content)
}

function snapshotResumeContent(content: ResumeContent) {
  const normalized = cloneContent(content)
  normalized.activeSections = resolveActiveSectionIds(normalized)
  normalized.hiddenSections = asStringArray(normalized.hiddenSections)
  return JSON.stringify(normalized)
}

async function exportResumePdf() {
  if (exportingPdf.value) return
  templateLibraryOpen.value = false
  rightPanelMode.value = 'preview'
  if (isDirty.value) {
    ElMessage.warning('当前有未保存草稿，将导出右侧预览中的当前内容')
  }
  exportingPdf.value = true
  try {
    await nextTick()
    const paper = document.querySelector<HTMLElement>('.editor-right-dock .a4-paper')
    if (!paper) {
      throw new Error('未找到可导出的简历预览')
    }
    const versionLabel = activeVersion.value ? `v${activeVersion.value.versionNo}` : 'draft'
    await exportResumeElementToPdf({
      sourceElement: paper,
      fileName: `${editorResumeTitle.value || '职达简历'}-${versionLabel}`,
    })
    ElMessage.success('PDF 已生成')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导出 PDF 失败'
  } finally {
    exportingPdf.value = false
  }
}

function safeText(value?: string | null) {
  return value?.trim() || ''
}

function statusByFilledCount(values: Array<string | undefined | null>): EditorSectionStatus {
  const filled = values.filter((value) => Boolean(value?.trim())).length
  if (filled === 0) return 'empty'
  if (filled < values.length) return 'warning'
  return 'ready'
}

function projectStatus(projects: ProjectItem[]): EditorSectionStatus {
  if (projects.length === 0) return 'empty'
  return projects.some((project) => !project.evidenceId) ? 'warning' : 'ready'
}

function asArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? value as T[] : []
}

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.map(String).map((item) => item.trim()).filter(Boolean)
}

function asEditableStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.map(String)
}

function parseEditableList(value: string) {
  return value
    .split(/[\n,，、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizedSkillCategories(value: unknown, fallbackSkills: string[]): SkillCategory[] {
  const categories = asArray<SkillCategory>(value)
  if (categories.length) return categories
  return fallbackSkills.length ? [{ name: '技术栈', skills: fallbackSkills }] : []
}

function ensureSkillCategories(content: ResumeContent) {
  const categories = asArray<SkillCategory>(content.skillCategories)
  if (categories.length) return
  const fallbackSkills = asStringArray(content.skills)
  content.skillCategories = fallbackSkills.length
    ? [{ name: '技术栈', skills: fallbackSkills }]
    : [{ name: '技能类别', skills: [] }]
}

function flattenSkillCategories(value: unknown): string[] {
  return asArray<SkillCategory>(value).flatMap((category) => asStringArray(category.skills))
}

function collectionKeyBySection(sectionId: string) {
  const mapping: Record<string, keyof ResumeContent> = {
    'work-experience': 'workExperience',
    education: 'education',
    skills: 'skillCategories',
    projects: 'projects',
    certifications: 'certifications',
    languages: 'languages',
    github: 'githubProjects',
    'qr-codes': 'qrCodes',
    custom: 'customSections',
  }
  return mapping[sectionId]
}

function resolveActiveSectionIds(content: ResumeContent) {
  const stored = asStringArray(content.activeSections)
  if (stored.length > 0) {
    const validIds = new Set(moduleCatalog.map((module) => module.id))
    const seen = new Set<string>()
    return ['personal-info', ...stored].filter((id) => {
      if (!validIds.has(id) || seen.has(id)) return false
      seen.add(id)
      return true
    })
  }

  const ids = new Set(defaultActiveSectionIds)
  if (asArray<WorkExperienceItem>(content.workExperience).length) ids.add('work-experience')
  if (asArray<CertificationItem>(content.certifications).length) ids.add('certifications')
  if (asArray<LanguageItem>(content.languages).length) ids.add('languages')
  if (asArray<GithubItem>(content.githubProjects).length) ids.add('github')
  if (asArray<QrCodeItem>(content.qrCodes).length) ids.add('qr-codes')
  if (asArray<CustomSectionItem>(content.customSections).length) ids.add('custom')
  return orderedSectionIds(Array.from(ids))
}

function orderedSectionIds(ids: string[]) {
  const idSet = new Set(ids)
  return moduleCatalog
    .map((module) => module.id)
    .filter((id) => idSet.has(id))
}

function createEmptyItem(sectionId: string): Record<string, unknown> {
  if (sectionId === 'work-experience') {
    return {
      company: '',
      position: '',
      location: '',
      startDate: '',
      endDate: '',
      description: '',
      technologies: [],
      highlights: [],
    }
  }
  if (sectionId === 'education') {
    return {
      school: '',
      major: '',
      degree: '',
      gpa: '',
      period: '',
      highlights: [],
    }
  }
  if (sectionId === 'skills') return { name: '', skills: [] }
  if (sectionId === 'projects') {
    return {
      title: '',
      description: '',
      technologies: [],
      highlights: [],
    }
  }
  if (sectionId === 'certifications') return { name: '', issuer: '', date: '', description: '' }
  if (sectionId === 'languages') return { name: '', level: '', description: '' }
  if (sectionId === 'github') return { name: '', url: '', description: '', technologies: [] }
  if (sectionId === 'qr-codes') return { label: '', url: '' }
  return { title: '', description: '' }
}

function formatDate(value?: string | null) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function normalizeDraftFieldValue(fieldKey: string, value: string) {
  if (!fieldKey.endsWith('.evidenceId')) return value
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

function defaultLayoutAssistantPosition() {
  if (typeof window === 'undefined') return { x: 24, y: 640 }
  return {
    x: Math.max(12, window.innerWidth - 58),
    y: Math.max(64, window.innerHeight - 58),
  }
}

function clampNumber(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function buildLayoutAssistantIssues(sections: EditorSection[]) {
  const issues: Array<{ id: string; sectionId: string }> = []
  const visibleSections = sections.filter((section) => section.visible)
  const textLength = visibleSections.reduce((sum, section) => sum + sectionTextLength(section), 0)

  if (visibleSections.length >= 8) {
    issues.push({ id: 'too-many-sections', sectionId: visibleSections[0]?.id ?? 'personal-info' })
  }
  if (textLength > 2400) {
    issues.push({ id: 'content-overflow', sectionId: longestLayoutSection(visibleSections)?.id ?? 'summary' })
  }
  visibleSections.forEach((section) => {
    const length = sectionTextLength(section)
    if ((section.id === 'summary' || section.id === 'projects' || section.id === 'work-experience') && length > 520) {
      issues.push({ id: `${section.id}-dense`, sectionId: section.id })
    }
    if (section.id === 'skills' && section.chips.length > 18) {
      issues.push({ id: 'skills-too-many', sectionId: section.id })
    }
    if (section.id === 'projects' && (section.items?.length ?? 0) > 4) {
      issues.push({ id: 'projects-too-many', sectionId: section.id })
    }
  })

  return issues.slice(0, 5)
}

function sectionTextLength(section: EditorSection) {
  return [
    ...section.fields.map((field) => field.value),
    ...section.chips,
    ...section.paragraphs,
    ...(section.items ?? []).flatMap((item) => [
      item.title,
      item.description,
      ...(item.fields ?? []).map((field) => field.value),
      ...(item.listFields ?? []).flatMap((field) => field.value),
    ]),
  ].join('').length
}

function longestLayoutSection(sections: EditorSection[]) {
  return sections
    .filter((section) => section.visible)
    .reduce<EditorSection | null>((current, section) => {
      if (!current) return section
      return sectionTextLength(section) > sectionTextLength(current) ? section : current
    }, null)
}

function syncDraftFromVersion() {
  const content = cloneContent(activeVersion.value?.content ?? {})
  suppressDraftHistory.value = true
  draftContent.value = content
  baseContentSnapshot.value = snapshotResumeContent(content)
  undoStack.value = []
  redoStack.value = []
}

function syncBlankDraft() {
  const content = createBlankResumeContent()
  suppressDraftHistory.value = true
  draftContent.value = content
  baseContentSnapshot.value = snapshotResumeContent(content)
  undoStack.value = []
  redoStack.value = []
  jobMatch.value = null
  suggestions.value = []
  assessmentResult.value = null
  assessmentError.value = ''
  selectedSectionId.value = 'personal-info'
  rightPanelMode.value = 'preview'
}

function createBlankResumeContent(): ResumeContent {
  return {
    activeSections: [...defaultActiveSectionIds],
    basicInfo: {
      name: '',
      title: '',
      age: '',
      gender: '',
      politicalStatus: '',
      ethnicity: '',
      maritalStatus: '',
      yearsOfExperience: '',
      highestEducation: '',
      email: '',
      phone: '',
      wechat: '',
      location: '',
    },
    summary: '',
    education: [],
    skills: [],
    skillCategories: [],
    projects: [],
  }
}

function cloneContent(content: ResumeContent): ResumeContent {
  return JSON.parse(JSON.stringify(content ?? {})) as ResumeContent
}

function setByPath(target: ResumeContent, path: string, value: unknown) {
  const parts = path.split('.')
  let current: Record<string, unknown> = target
  for (let index = 0; index < parts.length - 1; index += 1) {
    const key = parts[index]
    const nextKey = parts[index + 1]
    const existing = current[key]
    if (existing && typeof existing === 'object') {
      current = existing as Record<string, unknown>
      continue
    }
    current[key] = /^\d+$/.test(nextKey) ? [] : {}
    current = current[key] as Record<string, unknown>
  }
  current[parts[parts.length - 1]] = value
}

function getByPath(target: ResumeContent, path: string) {
  const parts = path.split('.')
  let current: unknown = target
  for (const part of parts) {
    if (current && typeof current === 'object') {
      current = (current as Record<string, unknown>)[part]
    } else {
      return undefined
    }
  }
  return current
}

function listByPath(target: ResumeContent, path: string) {
  const parts = path.split('.')
  let current: unknown = target
  for (const part of parts) {
    if (current && typeof current === 'object') {
      current = (current as Record<string, unknown>)[part]
    } else {
      return []
    }
  }
  return Array.isArray(current) ? [...current].map(String) : []
}

function focusListInput(fieldKey: string, index: number) {
  void nextTick(() => {
    const target = document.querySelector<HTMLInputElement>(
      `[data-list-field="${fieldKey}"][data-list-index="${index}"]`,
    )
    target?.focus()
  })
}

function focusFirstFieldInItem(collectionKey: keyof ResumeContent, index: number) {
  void nextTick(() => {
    const target = document.querySelector<HTMLElement>(
      `[data-field-key^="${String(collectionKey)}.${index}."]`,
    )
    target?.focus()
  })
}
</script>

<style scoped>
.launch-page {
  height: 100vh;
  color: #f8fafc;
  background:
    radial-gradient(circle at 12% 8%, rgba(16, 185, 129, 0.32), transparent 30%),
    radial-gradient(circle at 85% 12%, rgba(59, 130, 246, 0.28), transparent 26%),
    linear-gradient(135deg, #08111f 0%, #111827 46%, #0f2f2b 100%);
  overflow: hidden;
  padding: clamp(24px, 3vw, 42px);
}

.launch-hero {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(340px, 0.78fr) minmax(480px, 1.22fr);
  align-items: center;
  gap: clamp(20px, 3vw, 42px);
}

.launch-copy {
  min-height: 0;
  max-height: 100%;
  display: flex;
  flex-direction: column;
}

.launch-badge {
  display: inline-flex;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #a7f3d0;
  font-size: 13px;
  font-weight: 900;
  padding: 9px 13px;
}

.launch-copy h1 {
  margin: 16px 0 14px;
  max-width: 760px;
  color: #fff;
  font-size: clamp(34px, 4.1vw, 62px);
  line-height: 1;
  letter-spacing: -0.07em;
}

.launch-copy p {
  max-width: 660px;
  color: rgba(226, 232, 240, 0.78);
  font-size: 16px;
  line-height: 1.72;
}

.launch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.launch-actions button {
  border: 0;
  border-radius: 18px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 900;
  padding: 13px 18px;
}

.launch-primary {
  background: linear-gradient(135deg, #10b981, #22c55e);
  color: #052e20;
  box-shadow: 0 18px 50px rgba(16, 185, 129, 0.35);
}

.launch-secondary {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.launch-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  position: relative;
  z-index: 1;
  gap: 8px;
  width: 100%;
}

.launch-metric-item {
  min-width: 0;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  padding: 9px 12px;
  backdrop-filter: blur(18px);
}

.launch-metric-item strong,
.launch-metric-item span {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launch-metric-item strong {
  color: #fff;
  font-size: 22px;
  font-weight: 950;
  line-height: 1;
}

.launch-metric-item span {
  color: rgba(226, 232, 240, 0.58);
  margin-top: 2px;
  font-size: 12px;
  font-weight: 800;
}

.launch-resume-library {
  max-width: 680px;
  min-height: 0;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  margin-top: 18px;
}

.launch-version-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.launch-version-head span {
  color: #6ee7b7;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.launch-version-head strong {
  color: #fff;
  font-size: 15px;
  font-weight: 900;
}

.launch-resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  min-height: 0;
  max-height: min(43vh, 420px);
  overflow-y: auto;
  gap: 10px;
  padding-right: 4px;
}

.launch-resume-item {
  min-width: 0;
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 10px;
  min-height: 110px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.16s ease;
}

.launch-resume-item:hover {
  border-color: rgba(110, 231, 183, 0.48);
  background: rgba(16, 185, 129, 0.14);
  transform: translateY(-1px);
}

.launch-resume-item.active {
  border-color: rgba(16, 185, 129, 0.75);
  background: rgba(16, 185, 129, 0.18);
}

.resume-card-thumb {
  display: grid;
  align-content: start;
  gap: 5px;
  height: 90px;
  overflow: hidden;
  border-radius: 16px;
  background: #f8fafc;
  padding: 10px 8px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.22);
}

.resume-card-thumb strong {
  color: #101a33;
  font-size: 11px;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-card-thumb span,
.resume-card-thumb em {
  display: block;
  height: 4px;
  border-radius: 999px;
  background: #dbe3ef;
}

.resume-card-thumb span:nth-child(2) { width: 86%; background: #10b981; }
.resume-card-thumb span:nth-child(3) { width: 68%; }
.resume-card-thumb em { width: 42%; background: #bfdbfe; }
.resume-card-thumb span:nth-child(5) { width: 78%; }

.resume-card-main {
  min-width: 0;
}

.resume-card-main > strong,
.resume-card-main > span,
.resume-card-main > small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-card-main > strong {
  font-size: 16px;
  font-weight: 950;
}

.resume-card-main > span {
  margin-top: 8px;
  color: #6ee7b7;
  font-size: 12px;
  font-weight: 900;
}

.resume-card-main > small {
  margin-top: 4px;
  color: rgba(226, 232, 240, 0.62);
  font-size: 11px;
}

.resume-card-versions {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 10px;
}

.resume-card-versions i {
  padding: 3px 7px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  color: rgba(226, 232, 240, 0.7);
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
}

.launch-version-empty {
  margin: 0;
  color: rgba(226, 232, 240, 0.64);
  font-size: 13px;
}

.launch-resume-stage {
  position: relative;
  height: min(90vh, 830px);
  min-height: 580px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.launch-glow {
  position: absolute;
  filter: blur(38px);
  opacity: 0.7;
  border-radius: 999px;
}

.launch-glow.one {
  width: 260px;
  height: 260px;
  right: 20px;
  top: 20px;
  background: #10b981;
}

.launch-glow.two {
  width: 220px;
  height: 220px;
  left: 40px;
  bottom: 70px;
  background: #3b82f6;
}

.launch-resume-card {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(24px);
}

.launch-resume-top {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  padding: 12px 16px;
}

.launch-resume-top span,
.launch-resume-top small {
  color: rgba(226, 232, 240, 0.68);
  font-size: 13px;
}

.launch-resume-top strong {
  color: #fff;
  margin-right: auto;
}

.launch-preview {
  height: calc(100% - 49px);
}

.editor-stage {
  position: relative;
  height: 100vh;
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: #f8fafc;
}

.layout-assistant-fab {
  position: fixed;
  z-index: 70;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  opacity: 0.7;
  touch-action: none;
  backdrop-filter: blur(14px);
  transition: opacity 0.18s ease, transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.layout-assistant-fab:hover,
.layout-assistant-fab.active {
  border-color: rgba(16, 185, 129, 0.45);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.16);
  opacity: 1;
  transform: translateY(-2px);
}

.layout-assistant-fab span {
  display: block;
  width: 18px;
  height: 18px;
  border: 1.5px solid #94a3b8;
  border-radius: 999px;
  background:
    radial-gradient(circle, #94a3b8 0 2px, transparent 2.5px),
    radial-gradient(circle, transparent 0 7px, rgba(148, 163, 184, 0.22) 7px 8px, transparent 8.5px);
}

.layout-assistant-fab.warning span {
  border-color: #10b981;
  background:
    radial-gradient(circle, #10b981 0 2px, transparent 2.5px),
    radial-gradient(circle, transparent 0 7px, rgba(16, 185, 129, 0.24) 7px 8px, transparent 8.5px);
}

.layout-assistant-panel {
  position: fixed;
  z-index: 69;
  overflow: auto;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 28px 90px rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(18px);
  padding: 14px;
}

.layout-assistant-generate {
  width: 100%;
  margin-top: 12px;
  border: 0;
  border-radius: 16px;
  background: #101a33;
  color: #ffffff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 950;
  padding: 11px 12px;
}

.layout-assistant-generate:disabled {
  cursor: wait;
  opacity: 0.68;
}

.layout-proposal-warnings {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.layout-proposal-warnings span {
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.1);
  color: #92400e;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.55;
  padding: 8px 10px;
}

.layout-proposal-meta {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.layout-proposal-meta span {
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: 12px;
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.45;
  padding: 8px 10px;
}

.layout-diff-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.layout-diff-card {
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #ffffff;
  padding: 11px;
}

.layout-diff-card header {
  margin-bottom: 8px;
}

.layout-diff-card header span {
  color: #059669;
  font-size: 11px;
  font-weight: 950;
}

.layout-diff-card header strong {
  display: block;
  margin-top: 3px;
  color: #334155;
  font-size: 12px;
  line-height: 1.45;
}

.layout-diff-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 7px;
}

.layout-diff-grid div {
  border-radius: 12px;
  background: #f8fafc;
  padding: 8px;
}

.layout-diff-grid div:last-child {
  background: #f1fff9;
}

.layout-diff-grid small {
  display: block;
  color: #94a3b8;
  font-size: 10px;
  font-weight: 950;
  margin-bottom: 4px;
}

.layout-diff-grid p {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  color: #334155;
  font-size: 12px;
  line-height: 1.55;
}

.layout-proposal-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  margin-top: 12px;
}

.layout-proposal-actions button {
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 9px 10px;
}

.layout-proposal-actions button.primary {
  border-color: #10a878;
  background: #10a878;
  color: #ffffff;
}

.layout-assistant-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.layout-assistant-panel header span,
.layout-assistant-panel header strong {
  display: block;
}

.layout-assistant-panel header span {
  color: #059669;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.layout-assistant-panel header strong {
  margin-top: 3px;
  color: #101a33;
  font-size: 18px;
  font-weight: 950;
}

.layout-assistant-panel header button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
}

.layout-assistant-summary {
  display: grid;
  gap: 5px;
  border: 1px solid #d8efe6;
  border-radius: 18px;
  background:
    radial-gradient(circle at 0 0, rgba(16, 185, 129, 0.12), transparent 42%),
    #f8fffc;
  padding: 12px;
}

.layout-assistant-summary strong {
  color: #101a33;
  font-size: 14px;
  font-weight: 950;
}

.layout-assistant-summary span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.layout-assistant-actions {
  display: grid;
  gap: 9px;
  margin-top: 12px;
}

.layout-assistant-actions button {
  display: grid;
  gap: 4px;
  border: 1px solid #e5eaf2;
  border-left: 3px solid #2563eb;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  padding: 11px 12px;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.layout-assistant-actions button.warning {
  border-left-color: #f59e0b;
}

.layout-assistant-actions button:hover {
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.1);
  transform: translateY(-1px);
}

.layout-assistant-actions span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 950;
}

.layout-assistant-actions strong {
  color: #101a33;
  font-size: 13px;
  font-weight: 950;
}

.layout-assistant-actions small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.layout-assistant-panel-enter-active,
.layout-assistant-panel-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.layout-assistant-panel-enter-from,
.layout-assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

.resume-editor-toolbar {
  height: 56px;
  display: grid;
  grid-template-columns: auto minmax(150px, 0.22fr) minmax(300px, 0.5fr) auto minmax(360px, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e5eaf2;
  background: rgba(255, 255, 255, 0.96);
  padding: 0 14px;
}

.toolbar-back {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font-size: 22px;
}

.toolbar-back:hover {
  background: #f1f5f9;
}

.toolbar-title strong,
.toolbar-title span {
  display: block;
}

.toolbar-title strong {
  color: #111827;
  font-size: 17px;
}

.toolbar-title span {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 2px;
}

.toolbar-target-card {
  min-width: 0;
  height: 44px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 16px;
  background:
    radial-gradient(circle at 8% 0%, rgba(16, 185, 129, 0.12), transparent 38%),
    rgba(248, 250, 252, 0.94);
  padding: 5px 8px 5px 6px;
}

.toolbar-target-card.empty {
  opacity: 0.72;
}

.toolbar-target-card__content {
  min-width: 0;
}

.toolbar-target-card__company {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.toolbar-target-card__company em {
  position: relative;
  flex: 0 0 auto;
  color: #047857;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  line-height: 1;
  padding-left: 8px;
}

.toolbar-target-card__company em::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: currentColor;
  content: "";
  transform: translateY(-50%);
}

.toolbar-target-card__company em.succeeded {
  color: #047857;
}

.toolbar-target-card__company em.pending {
  color: #2563eb;
}

.toolbar-target-card__company em.failed {
  color: #dc2626;
}

.toolbar-target-card strong,
.toolbar-target-card small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-target-card strong {
  color: #101a33;
  font-size: 12px;
  line-height: 1.1;
}

.toolbar-target-card small {
  margin-top: 2px;
  color: #8a94a6;
  font-size: 10px;
  line-height: 1.1;
}

.toolbar-target-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 1px;
}

.toolbar-target-card__actions button {
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  padding: 4px 5px;
  transition: background 0.16s ease, color 0.16s ease;
}

.toolbar-target-card__actions button.ghost {
  color: #94a3b8;
}

.toolbar-target-card__actions button.primary {
  color: #7c8aa2;
}

.toolbar-target-card__actions button:hover {
  background: #ecfdf5;
  color: #047857;
}

.toolbar-history {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.toolbar-history button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 15px;
  font-weight: 900;
}

.toolbar-history button:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.toolbar-history button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.toolbar-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.toolbar-tools button {
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 8px 10px;
}

.toolbar-tools button:hover,
.toolbar-tools button.active {
  background: #f1f5f9;
  color: #047857;
}

.toolbar-tools button.active {
  box-shadow: inset 0 -2px 0 rgba(16, 185, 129, 0.7);
}

.template-current {
  min-width: 88px;
  display: grid;
  gap: 1px;
  border-left: 1px solid #e5eaf2;
  color: #64748b;
  cursor: pointer;
  padding-left: 10px;
}

.template-current span {
  font-size: 10px;
  font-weight: 900;
}

.template-current strong {
  color: #0f172a;
  font-size: 13px;
}

.template-current:hover strong {
  color: #047857;
}

:global(.assessment-dialog .el-dialog__body) {
  padding: 0;
}

:global(.assessment-dialog .el-dialog) {
  overflow: hidden;
  border-radius: 28px;
  background: #f8fafc;
  box-shadow: 0 32px 90px rgba(15, 23, 42, 0.22);
}

:global(.assessment-dialog .el-dialog__header) {
  margin: 0;
  padding: 18px 24px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
}

:global(.assessment-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 950;
  letter-spacing: -0.03em;
}

:global(.assessment-dialog .el-dialog__headerbtn) {
  top: 16px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #f1f5f9;
}

:global(.assessment-dialog .el-dialog__headerbtn:hover) {
  background: #e2e8f0;
}

:global(.assessment-dialog .el-alert) {
  border: 0;
  border-radius: 16px;
  overflow: hidden;
}

.assessment-modal {
  display: grid;
  gap: 16px;
  max-height: min(78vh, 760px);
  overflow-y: auto;
  padding: 20px 24px 24px;
  background:
    radial-gradient(circle at 8% 0%, rgba(16, 185, 129, 0.11), transparent 30%),
    radial-gradient(circle at 90% 12%, rgba(59, 130, 246, 0.08), transparent 28%),
    #f8fafc;
}

.assessment-modal__head {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  overflow: hidden;
  border: 1px solid rgba(16, 185, 129, 0.14);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(236, 253, 245, 0.78)),
    #ffffff;
  padding: 18px 20px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.06);
}

.assessment-modal__head span,
.assessment-modal__head strong,
.assessment-modal__head small {
  display: block;
}

.assessment-modal__eyebrow {
  width: fit-content;
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
  font-size: 10px;
  font-weight: 950;
  letter-spacing: 0.14em;
  padding: 5px 9px;
  text-transform: uppercase;
}

.assessment-modal__head strong {
  margin-top: 8px;
  color: #101a33;
  font-size: 20px;
  line-height: 1.25;
}

.assessment-modal__head small {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.assessment-modal__head button {
  border: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 900;
  padding: 12px 18px;
  box-shadow: 0 16px 34px rgba(16, 185, 129, 0.28);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.assessment-modal__head button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 42px rgba(16, 185, 129, 0.34);
}

.assessment-modal__head button:disabled {
  background: #cbd5e1;
  box-shadow: none;
  cursor: not-allowed;
}

.assessment-modal__loading,
.assessment-modal__empty-state {
  min-height: 220px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  color: #64748b;
  text-align: center;
}

.assessment-modal__loading i {
  width: 36px;
  height: 36px;
  border: 3px solid #d1fae5;
  border-top-color: #10b981;
  border-radius: 14px;
  animation: assessment-spin 0.9s linear infinite;
}

.assessment-modal__loading span {
  font-size: 13px;
  font-weight: 900;
}

.assessment-modal__empty-state strong {
  color: #0f172a;
  font-size: 18px;
  font-weight: 950;
}

.assessment-modal__empty-state span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

@keyframes assessment-spin {
  to {
    transform: rotate(360deg);
  }
}

.assessment-modal__hero {
  display: grid;
  grid-template-columns: minmax(270px, 0.95fr) minmax(0, 1.05fr);
  gap: 14px;
}

.assessment-modal__score,
.assessment-modal__summary {
  display: grid;
  align-items: center;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 24px;
  background: #fff;
  padding: 18px;
  box-shadow: 0 18px 52px rgba(15, 23, 42, 0.055);
}

.assessment-modal__score {
  position: relative;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 253, 244, 0.72)),
    radial-gradient(circle at 20% 20%, rgba(16, 185, 129, 0.12), transparent 36%),
    #ffffff;
}

.assessment-modal__score::after {
  content: '';
  position: absolute;
  inset: -40% auto auto -20%;
  width: 58%;
  height: 180%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  opacity: 0.45;
  transform: rotate(18deg);
  animation: assessment-score-shine 4.8s ease-in-out infinite;
  pointer-events: none;
}

.assessment-modal__score-ring {
  position: relative;
  display: grid;
  place-items: center;
  width: 148px;
  height: 148px;
  border-radius: 28px;
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.94), rgba(236, 253, 245, 0.74)),
    linear-gradient(135deg, #f0fdf4, #eff6ff);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 18px 42px rgba(16, 185, 129, 0.18);
  isolation: isolate;
  animation: assessment-score-float 4.2s ease-in-out infinite;
}

.assessment-modal__score-ring :deep(.el-progress) {
  position: relative;
  z-index: 2;
  filter: drop-shadow(0 10px 20px rgba(16, 185, 129, 0.16));
}

.assessment-modal__score-aura,
.assessment-modal__score-scan {
  position: absolute;
  pointer-events: none;
}

.assessment-modal__score-aura {
  inset: 10px;
  z-index: 0;
  border-radius: 24px;
  background:
    conic-gradient(from 90deg, rgba(16, 185, 129, 0.12), rgba(59, 130, 246, 0.16), rgba(16, 185, 129, 0.12));
  filter: blur(8px);
  opacity: 0.82;
  animation: assessment-score-pulse 2.8s ease-in-out infinite;
}

.assessment-modal__score-scan {
  z-index: 1;
  inset: 22px;
  border-radius: 999px;
  border: 1px solid rgba(16, 185, 129, 0.12);
  background:
    linear-gradient(120deg, transparent 18%, rgba(255, 255, 255, 0.72) 48%, transparent 72%);
  mix-blend-mode: screen;
  animation: assessment-score-scan 3.6s ease-in-out infinite;
}

.assessment-modal__score-copy {
  position: relative;
  z-index: 1;
  display: grid;
  align-content: center;
  gap: 9px;
  min-width: 0;
}

.assessment-modal__score span,
.assessment-modal__score small,
.assessment-modal__summary span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.assessment-modal__score-copy > span {
  color: #059669;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.assessment-modal__score-copy > small {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.4;
}

.assessment-modal__score strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 2px 0 4px;
  color: #101a33;
  font-size: 54px;
  font-weight: 950;
  line-height: 0.95;
  letter-spacing: -0.06em;
  animation: assessment-score-pop 0.42s cubic-bezier(0.2, 0.9, 0.2, 1) both;
}

.assessment-modal__score strong em {
  color: #94a3b8;
  font-size: 18px;
  font-style: normal;
  font-weight: 900;
  letter-spacing: -0.03em;
}

@keyframes assessment-score-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes assessment-score-pulse {
  0%, 100% {
    opacity: 0.56;
    transform: scale(0.96);
  }
  50% {
    opacity: 0.95;
    transform: scale(1.04);
  }
}

@keyframes assessment-score-scan {
  0%, 100% {
    opacity: 0.16;
    transform: rotate(0deg) translateX(-6px);
  }
  50% {
    opacity: 0.62;
    transform: rotate(10deg) translateX(8px);
  }
}

@keyframes assessment-score-shine {
  0%, 62%, 100% {
    transform: translateX(-120%) rotate(18deg);
  }
  82% {
    transform: translateX(260%) rotate(18deg);
  }
}

@keyframes assessment-score-pop {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.assessment-modal__summary {
  align-content: center;
  gap: 10px;
}

.assessment-modal__summary strong {
  color: #101a33;
  font-size: 20px;
  line-height: 1.35;
  letter-spacing: -0.03em;
}

.assessment-modal__summary p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.assessment-modal__dimensions {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.assessment-modal__dimension {
  display: grid;
  align-content: space-between;
  gap: 9px;
  min-height: 96px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 18px;
  background: #fff;
  padding: 13px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.035);
}

.assessment-modal__dimension > div {
  display: grid;
  gap: 4px;
}

.assessment-modal__dimension span {
  color: #64748b;
}

.assessment-modal__dimension strong {
  color: #101a33;
  font-size: 16px;
}

.assessment-modal__issues {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  padding: 16px;
}

.assessment-modal__issues-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assessment-modal__issues h3 {
  margin: 0;
  color: #101a33;
  font-size: 16px;
  font-weight: 950;
}

.assessment-modal__issues-head > span {
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  padding: 5px 9px;
}

.assessment-modal__issues article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  border: 1px solid #f2e4d2;
  border-radius: 18px;
  background: linear-gradient(135deg, #fffaf2, #ffffff);
  padding: 12px;
}

.assessment-modal__issues article > span {
  align-self: start;
  border-radius: 999px;
  background: #ffedd5;
  color: #c2410c;
  font-size: 11px;
  font-weight: 900;
  padding: 4px 8px;
}

.assessment-modal__issues strong {
  color: #334155;
  font-size: 14px;
  line-height: 1.45;
}

.assessment-modal__issues p,
.assessment-modal__empty {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

/* 岗位推荐列表 */
.recommend-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
  padding: 14px 16px;
}

.recommend-summary span,
.recommend-summary strong {
  display: block;
}

.recommend-summary span {
  color: #059669;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.recommend-summary strong {
  margin-top: 4px;
  color: #101a33;
  font-size: 22px;
  font-weight: 950;
}

.recommend-summary p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.recommend-list {
  display: grid;
  gap: 12px;
}

.recommend-item {
  display: grid;
  grid-template-columns: 72px 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.07);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.recommend-item:hover {
  border-color: rgba(16, 185, 129, 0.32);
  background: #ffffff;
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.11);
  transform: translateY(-1px);
}

.recommend-item.selected {
  border-color: rgba(16, 185, 129, 0.58);
  background:
    radial-gradient(circle at 0 0, rgba(16, 185, 129, 0.12), transparent 36%),
    rgba(255, 255, 255, 0.95);
}

.recommend-item__rank {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 58px;
  border-radius: 16px;
  background: #f8fafc;
  padding: 8px;
}

.recommend-item__rank strong {
  font-size: 26px;
  font-weight: 950;
  line-height: 1.2;
}

.recommend-item__rank .rank-high { color: #16a34a; }
.recommend-item__rank .rank-mid  { color: #d97706; }
.recommend-item__rank .rank-low  { color: #dc2626; }
.recommend-item__rank .rank-none { color: #94a3b8; }

.recommend-item__rank span {
  margin-top: 2px;
  font-size: 11px;
  font-weight: 900;
  color: #64748b;
}

.recommend-item__info {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.recommend-item__info strong {
  overflow: hidden;
  color: #101a33;
  font-size: 15px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-item__info small {
  overflow: hidden;
  font-size: 13px;
  color: #94a3b8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-item__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.recommend-item__chips span {
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  font-weight: 900;
  padding: 4px 8px;
}

.recommend-item__chips em {
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.recommend-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.recommend-item__target,
.recommend-item__link {
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  padding: 8px 12px;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.recommend-item__target {
  border: 0;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 10px 22px rgba(16, 185, 129, 0.22);
  color: #ffffff;
}

.recommend-item__target:not(:disabled):hover,
.recommend-item__link:hover {
  transform: translateY(-1px);
}

.recommend-item__target:disabled {
  background: #d1fae5;
  box-shadow: none;
  color: #047857;
  cursor: default;
}

.recommend-item__link {
  border: 1px solid #dbe3ee;
  background: #ffffff;
  color: #475569;
}

.recommend-item__link:hover {
  background: #f8fafc;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
}

@media (max-width: 820px) {
  .recommend-summary {
    grid-template-columns: 1fr;
  }

  .recommend-item {
    grid-template-columns: 68px 42px minmax(0, 1fr);
  }

  .recommend-item__actions {
    grid-column: 1 / -1;
  }
}

.template-library-backdrop {
  position: absolute;
  inset: 56px 0 0;
  z-index: 25;
  border: 0;
  background: rgba(15, 23, 42, 0.035);
  cursor: default;
}

.template-library-panel {
  position: absolute;
  top: 66px;
  right: 18px;
  z-index: 30;
  width: min(820px, calc(100vw - 36px));
  max-height: calc(100vh - 88px);
  overflow: auto;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(18px);
  padding: 14px;
}

.template-library-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.template-library-head span,
.template-library-head strong {
  display: block;
}

.template-library-head span {
  color: #10a878;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.template-library-head strong {
  color: #0f172a;
  font-size: 18px;
  margin-top: 2px;
}

.template-library-head button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
}

.template-library-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.template-card {
  position: relative;
  display: grid;
  gap: 9px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #fff;
  cursor: pointer;
  padding: 10px;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.template-card:hover,
.template-card.active {
  border-color: rgba(16, 168, 120, 0.46);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.09);
  transform: translateY(-1px);
}

.template-card-status {
  position: absolute;
  top: 8px;
  right: 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 10px;
  font-weight: 900;
  padding: 3px 6px;
}

.template-card.active .template-card-status {
  background: #e9fff5;
  color: #07875f;
}

.template-miniature {
  height: 112px;
  display: grid;
  align-content: start;
  gap: 6px;
  overflow: hidden;
  border-radius: 12px;
  background: #f8fafc;
  padding: 14px 12px;
}

.template-miniature i,
.template-miniature strong,
.template-miniature span,
.template-miniature em {
  display: block;
  border-radius: 999px;
  background: #cbd5e1;
}

.template-miniature i {
  width: 38px;
  height: 8px;
}

.template-miniature strong {
  width: 70%;
  height: 12px;
  background: #0f172a;
}

.template-miniature span {
  height: 7px;
}

.template-miniature span:nth-of-type(1) {
  width: 92%;
}

.template-miniature span:nth-of-type(2) {
  width: 76%;
}

.template-miniature em {
  width: 100%;
  height: 2px;
  margin: 4px 0 2px;
}

.template-miniature span:nth-of-type(3) {
  width: 84%;
}

.mini-blue i,
.mini-blue em {
  background: #2563eb;
}

.mini-classic {
  background: #fbfaf7;
}

.mini-classic i,
.mini-classic em {
  background: #111827;
}

.mini-classic strong {
  background: #374151;
}

.mini-minimal {
  background: #ffffff;
}

.mini-minimal i,
.mini-minimal em {
  background: #e5e7eb;
}

.mini-emerald {
  background: linear-gradient(180deg, #f2fbf7 0%, #ffffff 100%);
}

.mini-emerald i,
.mini-emerald em {
  background: #10a878;
}

.mini-emerald strong {
  background: #064e3b;
}

.mini-graphite {
  background: #f4f4f5;
}

.mini-graphite i,
.mini-graphite em {
  background: #52525b;
}

.mini-graphite strong {
  background: #18181b;
}

.mini-sidebar {
  grid-template-columns: 28px 1fr;
  background: linear-gradient(90deg, #101a33 0 28%, #ffffff 28% 100%);
}

.mini-sidebar i,
.mini-sidebar em {
  background: #10a878;
}

.mini-sidebar strong,
.mini-sidebar span {
  grid-column: 2;
}

.mini-sidebar strong {
  background: #101a33;
}

.mini-compact {
  gap: 4px;
  background: #ffffff;
}

.mini-compact i,
.mini-compact em {
  background: #64748b;
}

.mini-compact span {
  height: 5px;
}

.mini-elegant {
  background: #fffdf7;
}

.mini-elegant i,
.mini-elegant em {
  background: #8b5cf6;
}

.mini-elegant strong {
  background: #312e81;
}

.mini-warm {
  background: #fff7ed;
}

.mini-warm i,
.mini-warm em {
  background: #c2410c;
}

.mini-warm strong {
  background: #7c2d12;
}

.mini-terminal {
  background: #101a33;
}

.mini-terminal i,
.mini-terminal em {
  background: #34d399;
}

.mini-terminal strong {
  background: #e2e8f0;
}

.mini-terminal span {
  background: #475569;
}

.template-card-copy {
  display: grid;
  gap: 4px;
}

.template-card-copy strong {
  color: #0f172a;
  font-size: 14px;
}

.template-card-copy span {
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

.template-library-enter-active,
.template-library-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.template-backdrop-enter-active,
.template-backdrop-leave-active {
  transition: opacity 0.16s ease;
}

.template-library-enter-from,
.template-library-leave-to,
.template-backdrop-enter-from,
.template-backdrop-leave-to {
  opacity: 0;
}

.template-library-enter-from,
.template-library-leave-to {
  transform: translateY(-6px) scale(0.98);
}

.editor-workspace-page {
  --editor-subbar-height: 54px;
  height: calc(100% - 56px);
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(16, 185, 129, 0.11), transparent 32%),
    linear-gradient(135deg, #f8fafc 0%, #eef3f8 100%);
}

.editor-shell {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 214px minmax(380px, 0.8fr) minmax(540px, 1.2fr);
  transition: grid-template-columns 0.18s ease;
}

.editor-shell.sidebar-collapsed {
  grid-template-columns: 64px minmax(440px, 0.88fr) minmax(540px, 1.12fr);
}

.editor-right-dock {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-left: 1px solid #e5eaf2;
  background: #f1f5f9;
}

.right-dock-tabs {
  box-sizing: border-box;
  height: var(--editor-subbar-height);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  align-items: center;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e5eaf2;
  padding: 11px 14px;
}

.right-dock-tabs button {
  height: 28px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #778398;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 0 10px;
  transition: background 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.right-dock-tabs button.active {
  background: rgba(16, 26, 51, 0.92);
  color: #fff;
  box-shadow: 0 8px 18px rgba(16, 26, 51, 0.12);
}

.right-dock-tabs button:not(.active):hover {
  background: rgba(241, 245, 249, 0.9);
  color: #334155;
}

.right-dock-tabs span {
  display: inline-grid;
  min-width: 16px;
  height: 16px;
  place-items: center;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.92);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  margin-left: 6px;
}

@media (max-width: 1280px) {
  .launch-page {
    padding: 22px;
  }

  .launch-hero {
    grid-template-columns: minmax(320px, 0.78fr) minmax(440px, 1.22fr);
    gap: 22px;
  }

  .launch-resume-stage {
    height: min(90vh, 790px);
    min-height: 540px;
  }

  .launch-resume-grid {
    max-height: min(44vh, 390px);
  }

  .resume-editor-toolbar {
    grid-template-columns: auto minmax(130px, 0.24fr) minmax(240px, 0.5fr) auto minmax(300px, 1fr);
    gap: 8px;
  }

  .template-current {
    display: none;
  }

  .template-library-panel {
    right: 12px;
    width: min(620px, calc(100vw - 24px));
  }

  .template-library-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .template-card {
    grid-template-columns: 116px minmax(0, 1fr);
    align-items: center;
  }

  .template-miniature {
    height: 104px;
  }

  .toolbar-tools {
    justify-content: flex-end;
    overflow-x: auto;
  }

  .editor-shell {
    grid-template-columns: 190px minmax(340px, 0.82fr) minmax(460px, 1.18fr);
  }

  .editor-shell.sidebar-collapsed {
    grid-template-columns: 60px minmax(380px, 0.88fr) minmax(460px, 1.12fr);
  }
}

@media (max-height: 760px) {
  .launch-page {
    padding-block: 16px;
  }

  .launch-badge {
    padding: 7px 11px;
  }

  .launch-copy h1 {
    margin: 12px 0 10px;
    font-size: clamp(30px, 3.7vw, 52px);
  }

  .launch-copy p {
    font-size: 15px;
    line-height: 1.55;
  }

  .launch-actions {
    margin-top: 14px;
  }

  .launch-resume-library {
    margin-top: 14px;
  }

  .launch-resume-grid {
    max-height: min(39vh, 330px);
  }

  .launch-resume-stage {
    height: min(91vh, 720px);
    min-height: 490px;
  }

  .launch-metric-item {
    padding: 8px 10px;
  }

  .launch-metric-item strong {
    font-size: 18px;
  }
}

@media (max-width: 1080px) {
  .toolbar-target-card__company em,
  .toolbar-target-card small,
  .toolbar-target-card__actions .ghost {
    display: none;
  }

  .toolbar-target-card {
    grid-template-columns: 34px minmax(0, 1fr) auto;
  }
}

@media (max-width: 960px) {
  .launch-page {
    height: auto;
    min-height: 100vh;
    overflow-y: auto;
  }

  .launch-hero {
    height: auto;
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .launch-copy {
    max-height: none;
  }

  .launch-resume-library {
    max-width: none;
  }

  .launch-resume-grid {
    max-height: none;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    overflow: visible;
  }

  .launch-resume-stage {
    height: min(88vh, 840px);
    min-height: 620px;
  }

}

@media (max-width: 820px) {
  .assessment-modal__hero {
    grid-template-columns: 1fr;
  }

  .assessment-modal__dimensions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .template-library-grid {
    grid-template-columns: 1fr;
  }
}
</style>
