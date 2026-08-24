<template>
  <section class="targets-page">
    <PageHeader eyebrow="求职计划" title="求职计划" subtitle="以公司为单位管理求职进度，投递信息集中沉淀在投递专区。">
      <template #actions>
        <div class="view-switch" role="tablist" aria-label="视图切换">
          <button role="tab" type="button" :aria-selected="viewMode === 'board'" :class="{ on: viewMode === 'board' }" data-test="view-board" @click="viewMode = 'board'">计划看板</button>
          <button role="tab" type="button" :aria-selected="viewMode === 'applications'" :class="{ on: viewMode === 'applications' }" data-test="view-applications" @click="viewMode = 'applications'">投递专区</button>
        </div>
        <button class="btn-primary" type="button" data-test="create-target" @click="openCreate">新建求职目标</button>
      </template>
    </PageHeader>

    <!-- ── 计划看板 ── -->
    <div v-if="viewMode === 'board'" class="plan-scroll">
      <p v-if="store.errorMessage && !store.targets.length" class="plan-error" role="alert">{{ store.errorMessage }}</p>
      <div v-if="store.loading && !store.targets.length" class="plan-empty">正在读取本地计划…</div>

      <template v-else>
        <!-- 工具栏：固定两行，阶段行放不下收进「更多」 -->
        <div class="plan-toolbar" data-test="plan-toolbar">
          <div class="toolbar-row">
            <span class="toolbar-kicker">阶段</span>
            <button
              v-for="option in primaryStageOptions"
              :key="option.key"
              type="button"
              class="filter-pill"
              :class="{ on: statusFilter === option.key }"
              :data-test="`filter-stage-${option.key}`"
              @click="statusFilter = option.key"
            >{{ option.label }}<em v-if="option.count != null">{{ option.count }}</em></button>

            <label class="toolbar-search">
              <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path fill="currentColor" d="M11.7 10.3A6 6 0 1 0 10.3 11.7l3 3a1 1 0 0 0 1.4-1.4l-3-3ZM7 12A5 5 0 1 1 7 2a5 5 0 0 1 0 10Z"/></svg>
              <input v-model="searchKeyword" data-test="plan-search" autocomplete="off" placeholder="搜索公司或岗位…">
            </label>
          </div>
          <div class="toolbar-row" v-if="roleFilterOptions.length > 1">
            <span class="toolbar-kicker">岗位</span>
            <button
              v-for="option in visibleRoleOptions"
              :key="option.key"
              type="button"
              class="filter-pill"
              :class="{ on: roleFilter === option.key }"
              :data-test="`filter-role-${option.key}`"
              @click="roleFilter = option.key"
            >{{ option.label }}<em v-if="option.count != null">{{ option.count }}</em></button>
            <button
              v-if="extraRoleOptions.length"
              type="button"
              class="filter-pill more-toggle"
              :class="{ on: roleMoreOpen || extraRoleOptions.some((option) => roleFilter === option.key) }"
              @click="roleMoreOpen = !roleMoreOpen"
            >{{ roleMoreOpen ? '收起 ∧' : `其他 ${extraRoleOptions.length} ∨` }}</button>
            <template v-if="roleMoreOpen">
              <button
                v-for="option in extraRoleOptions"
                :key="option.key"
                type="button"
                class="filter-pill"
                :class="{ on: roleFilter === option.key }"
                :data-test="`filter-role-${option.key}`"
                @click="roleFilter = option.key"
              >{{ option.label }}<em v-if="option.count != null">{{ option.count }}</em></button>
            </template>
          </div>
        </div>

        <div class="plan-grid">
          <article
            v-for="(target, index) in visibleTargets"
            :key="target.id"
            class="plan-card"
            :class="{ archived: target.status === 'archived' }"
            :style="{ '--i': Math.min(index, 8) }"
            :data-test="`plan-card-${target.id}`"
          >
            <!-- 头部：logo + 身份 -->
            <header class="card-head">
              <img v-if="markFor(target).icon" class="logo-img" :src="markFor(target).icon" alt="" aria-hidden="true">
              <span
                v-else
                class="logo-mark"
                :style="{ background: markFor(target).color, color: markFor(target).lightText ? '#fff' : '#1b1b1b' }"
                aria-hidden="true"
              >{{ markFor(target).letter }}</span>
              <div class="head-copy">
                <h3>{{ cardTitle(target) }}</h3>
                <small v-if="jobFor(target)">{{ jdStateLabel(target) }}<template v-if="target.id === store.activeTargetId"> · 当前目标</template></small>
                <button v-else-if="target.status === 'active'" type="button" class="jd-add" :data-test="`jd-add-${target.id}`" @click.stop="openJd(target)">＋ 录入岗位 JD</button>
                <small v-else>尚未录入岗位 JD</small>
              </div>
              <div class="head-side">
                <button type="button" class="stage-pill link" :class="stageOf(target)" :data-test="`stage-pill-${target.id}`" :title="'查看阶段时间轴'" @click.stop="openTimeline(target)" :style="pillStyle(target)">{{ stageLabel(target) }}</button>
                <button class="menu-trigger" type="button" :aria-label="`更多操作：${target.name}`" @click.stop="toggleMenu(target.id, $event)">⋯</button>
              </div>
            </header>

          <!-- 阶段管线 -->
          <ol class="pipeline" :data-test="`pipeline-${target.id}`" aria-label="求职阶段">
            <li v-for="(step, stepIndex) in stageStepsOf(target)" :key="step.key" class="pipeline-item">
              <button
                type="button"
                class="node"
                :class="{ current: step.current, done: step.reached && !step.current }"
                :data-test="`stage-${target.id}-${step.key}`"
                :disabled="nodeDisabled(target, step.key)"
                @click="changeStage(target, step.key)"
              >
                <span class="dot" aria-hidden="true" />
                <span class="node-label">{{ step.label }}</span>
                <span v-if="stageTimeOf(target, step.key)" class="node-time">{{ stageTimeOf(target, step.key) }}</span>
              </button>
              <span v-if="stepIndex < stageStepsOf(target).length - 1" class="connector" :class="{ filled: stepIndex < stageCurrentIndexOf(target) }" aria-hidden="true" />
            </li>
          </ol>

          <!-- 结果标记：下拉菜单式，单向推进，进入结果态后锁定 -->
          <div v-if="target.status === 'active'" class="outcome-row" data-test="outcomes">
            <template v-if="!isTerminalStage(stageOf(target))">
              <button
                type="button"
                class="outcome-trigger"
                :data-test="`outcome-trigger-${target.id}`"
                :disabled="busyId === target.id"
                @click.stop="toggleOutcomeMenu(target.id, $event)"
              >
                标记结果
                <span class="chevron" aria-hidden="true">▾</span>
              </button>
              <span v-if="operationError" class="locked-note error-text">{{ operationError }}</span>
            </template>
            <p v-else class="locked-note">已标记「{{ stageLabel(target) }}」，状态锁定 · 点击右上角阶段标签查看时间轴</p>
          </div>

          <!-- 简历修改版 -->
          <div class="resume-row">
            <template v-if="resumeFor(target)">
              <button type="button" class="resume-chip linked" data-test="resume-chip" @click="openResume(resumeFor(target)!)">
                <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path fill="currentColor" d="M4 1h6l4 4v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1Zm6 1v3h3L10 2Z"/></svg>
                {{ companyNameOf(target) || '简历' }}修改版 · V{{ resumeFor(target)!.versionNo }}
              </button>
              <span class="resume-meta">基于此版本修改</span>
            </template>
            <template v-else>
              <button type="button" class="resume-chip unlinked" data-test="resume-chip" @click="() => void router.push({ name: 'resumes' })">
                ＋ 绑定简历
              </button>
              <span class="resume-meta">绑定后针对该公司修改即为修改版</span>
            </template>
          </div>

          <!-- 近期面试记录（无模拟面试入口）-->
          <div class="interviews" :data-test="`interviews-${target.id}`">
            <h4>近期面试记录<span v-if="recentPlansOf(target).length" class="interviews-count">{{ recentPlansOf(target).length }} 条</span></h4>
            <ul v-if="recentPlansOf(target).length">
              <li v-for="plan in recentPlansOf(target)" :key="plan.planId">
                <button type="button" class="plan-row" @click="viewPlan(plan)">
                  <span class="plan-main">
                    <strong>{{ plan.title }}</strong>
                    <small>{{ plan.rounds.length }} 轮 · {{ plan.questionCount }} 题</small>
                  </span>
                  <span class="plan-meta">
                    {{ shortDate(plan.updatedAt ?? plan.createdAt) }}
                    <em v-if="plan.summary" class="score">{{ plan.summary.overallScore }}</em>
                  </span>
                </button>
              </li>
            </ul>
            <p v-else class="interviews-empty">暂无面试记录，完成一次模拟后自动汇总到这里。</p>
          </div>

          <!-- 时间元信息 -->
          <footer class="card-foot">
            <span>创建于 {{ shortDate(target.createdAt) }}</span>
            <template v-if="target.location"><span class="foot-dot" aria-hidden="true">·</span><span>{{ target.location }}</span></template>
            <template v-else-if="target.industry"><span class="foot-dot" aria-hidden="true">·</span><span>{{ target.industry }}</span></template>
            <span class="foot-dot" aria-hidden="true">·</span>
            <span>最近 {{ recentLabel(target) }}</span>
          </footer>
        </article>
      </div>

      <div v-if="!store.loading && visibleTargets.length === 0 && store.targets.length" class="plan-empty big">
        没有匹配当前筛选的计划。
      </div>
      </template>
    </div>

    <!-- ── 投递专区 ── -->
    <div v-else class="plan-scroll" data-test="applications-zone">
      <!-- 我的求职目标 -->
      <h4 class="zone-kicker">我的求职目标<span class="sites-count">{{ sortedTargets.length }} 个</span></h4>
      <table v-if="store.targets.length" class="apply-table" data-test="apply-table">
        <thead>
          <tr>
            <th>求职目标</th>
            <th>阶段</th>
            <th>行业</th>
            <th>期望岗位</th>
            <th>地点</th>
            <th>备注</th>
            <th>投递官网</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="target in sortedTargets" :key="target.id" :class="{ archived: target.status === 'archived' }" :data-test="`apply-row-${target.id}`">
            <td class="cell-target">
              <img v-if="markFor(target).icon" class="row-logo" :src="markFor(target).icon" alt="" aria-hidden="true">
              <span v-else class="row-letter" :style="{ background: markFor(target).color }">{{ markFor(target).letter }}</span>
              <span class="cell-target-copy">
                <strong>{{ cardTitle(target) }}</strong>
                <small>{{ shortDate(target.createdAt) }} 创建</small>
              </span>
            </td>
            <td>
              <button type="button" class="stage-pill mini link" :class="stageOf(target)" :data-test="`timeline-${target.id}`" title="查看阶段时间轴" @click="openTimeline(target)">
                {{ stageLabel(target) }}
              </button>
            </td>
            <td class="cell-muted">{{ target.industry || '—' }}</td>
            <td class="cell-muted">{{ target.targetRole || '—' }}</td>
            <td class="cell-muted">{{ target.location || '—' }}</td>
            <td class="cell-notes">{{ target.notes || '—' }}</td>
            <td>
              <a v-if="siteFor(target)" class="site-link" :href="siteFor(target)!.url" target="_blank" rel="noopener noreferrer">
                {{ siteFor(target)!.name }}
                <svg viewBox="0 0 16 16" width="11" height="11" aria-hidden="true"><path fill="currentColor" d="M4 2h10v10h-2V5.4L4.7 12.7 3.3 11.3 10.6 4H4V2Z"/></svg>
              </a>
              <span v-else class="cell-muted">—</span>
            </td>
            <td class="cell-actions">
              <button type="button" class="text-btn" :data-test="`edit-apply-${target.id}`" @click="openApply(target)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
      <!-- 直达官网目录（可收起，收起时仅占一行） -->
      <section class="sites-panel" :class="{ collapsed: !directoryOpen }" data-test="sites-panel">
        <button type="button" class="sites-toggle" :aria-expanded="directoryOpen" @click="directoryOpen = !directoryOpen">
          <span class="tl-chevron" :class="{ open: directoryOpen }" aria-hidden="true">▾</span>
          <h4>直达官网<span class="sites-count">收录 100+ 家企业招聘官网</span></h4>
        </button>
        <div v-show="directoryOpen" class="sites-body">
          <label class="toolbar-search sites-search">
            <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path fill="currentColor" d="M11.7 10.3A6 6 0 1 0 10.3 11.7l3 3a1 1 0 0 0 1.4-1.4l-3-3ZM7 12A5 5 0 1 1 7 2a5 5 0 0 1 0 10Z"/></svg>
            <input v-model="directoryKeyword" data-test="directory-search" autocomplete="off" placeholder="搜索公司…">
          </label>
          <div v-show="filteredCareerSites.length" class="sites-grid">
            <a
              v-for="site in pagedCareerSites"
              :key="site.company"
              class="site-card"
              :href="site.url"
              target="_blank"
              rel="noopener noreferrer"
              :data-test="`site-${site.company}`"
            >
              <img v-if="companyMark(site.company).icon" class="site-logo" :src="companyMark(site.company).icon" alt="" aria-hidden="true">
              <span v-else class="site-letter" :style="{ background: companyMark(site.company).color }">{{ companyMark(site.company).letter }}</span>
              <span class="site-copy">
                <strong>{{ site.company }}</strong>
                <small>{{ site.url.replace('https://', '') }}</small>
              </span>
              <svg viewBox="0 0 16 16" width="11" height="11" aria-hidden="true"><path fill="currentColor" d="M4 2h10v10h-2V5.4L4.7 12.7 3.3 11.3 10.6 4H4V2Z"/></svg>
            </a>
          </div>
          <!-- 段状分页 -->
          <nav v-if="sitePageCount > 1 && filteredCareerSites.length" class="sites-pager" aria-label="官网目录分页">
            <button type="button" class="pager-arrow" :disabled="directoryPage === 0" aria-label="上一页" @click="directoryPage--">‹</button>
            <button
              v-for="page in sitePageCount"
              :key="page"
              type="button"
              class="pager-dash"
              :class="{ on: directoryPage === page - 1 }"
              :aria-label="`第 ${page} 页`"
              @click="directoryPage = page - 1"
            />
            <button type="button" class="pager-arrow" :disabled="directoryPage >= sitePageCount - 1" aria-label="下一页" @click="directoryPage++">›</button>
          </nav>
          <p v-if="!filteredCareerSites.length" class="plan-empty">没有匹配的公司。</p>
        </div>
      </section>

      <div v-if="store.targets.length === 0" class="plan-empty big">还没有求职计划，先在计划看板新建目标。</div>
    </div>

    <!-- ⋯ 菜单浮层 -->
    <Teleport to="body">
      <div v-if="menuTargetId != null || outcomeTargetId != null" class="menu-backdrop" @click="closeMenu" />
      <div v-if="menuTarget" class="menu-popover" :style="menuStyle">
        <button type="button" @click="beginRename(menuTarget)">重命名</button>
        <button v-if="menuTarget.status === 'active'" type="button" @click="archiveFromMenu(menuTarget)">归档计划</button>
        <button v-else type="button" @click="restoreFromMenu(menuTarget)">恢复计划</button>
        <button type="button" class="danger" @click="openDelete(menuTarget)">删除</button>
      </div>
      <!-- 标记结果菜单 -->
      <div v-if="outcomeTarget" class="menu-popover outcome-menu" :style="outcomeMenuStyle" data-test="outcome-menu">
        <p class="outcome-menu-title">标记为最终结果</p>
        <button
          v-for="outcome in OUTCOME_OPTIONS"
          :key="outcome.key"
          type="button"
          class="outcome-option"
          :data-test="`outcome-${outcomeTarget.id}-${outcome.key}`"
          @click="pickOutcome(outcome.key)"
        >
          <span class="outcome-dot" :style="{ background: TARGET_STAGE_COLORS[outcome.key] }" aria-hidden="true" />
          <span class="outcome-copy">
            <strong>{{ outcome.label }}</strong>
            <small>{{ outcome.desc }}</small>
          </span>
        </button>
      </div>
    </Teleport>

    <TargetCreateDialog
      :open="createOpen"
      :resumes="resumes"
      :submitting="creating"
      :error-message="createError"
      @close="closeCreate"
      @create="createTarget"
    />

    <!-- 重命名小弹窗 -->
    <div v-if="renameTarget" class="dialog-backdrop" role="presentation" @click.self="cancelRename">
      <section class="mini-dialog" role="dialog" aria-modal="true" aria-label="重命名求职目标">
        <h2>重命名求职目标</h2>
        <form @submit.prevent="saveRename">
          <input v-model="renamingName" data-test="rename-input" aria-label="新名称">
          <p v-if="operationError" class="error" role="alert">{{ operationError }}</p>
          <footer>
            <button type="button" @click="cancelRename">取消</button>
            <button class="primary" type="submit">保存</button>
          </footer>
        </form>
      </section>
    </div>

    <TargetDeleteDialog :open="Boolean(deleteTarget)" :target="deleteTarget" :submitting="deleting" :error-message="deleteError" @close="closeDelete" @confirm="confirmDelete" />

    <TargetInterviewsDialog :open="viewingPlan != null" :plan="viewingPlan" @close="viewingPlan = null" />

    <TargetApplyDialog
      :open="applyTarget != null"
      :target="applyTarget"
      :saving="applySaving"
      :error-message="applyError"
      @close="closeApply"
      @save="saveApply"
    />

    <TargetJdDialog
      :open="jdTarget != null"
      :target="jdTarget"
      :submitting="jdSaving"
      :error-message="jdError"
      @close="jdTarget = null"
      @save="saveJd"
    />

    <TargetStageTimeline
      :open="timelineTarget != null"
      :target="timelineTarget"
      :events="timelineEvents"
      :loading="timelineLoading"
      @close="timelineTarget = null"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTargetsStore } from '../../stores/targets'
import PageHeader from '../../components/PageHeader.vue'
import TargetCreateDialog, { type TargetDraftPayload } from '../../components/targets/TargetCreateDialog.vue'
import TargetDeleteDialog from '../../components/targets/TargetDeleteDialog.vue'
import TargetInterviewsDialog from '../../components/targets/TargetInterviewsDialog.vue'
import TargetApplyDialog from '../../components/targets/TargetApplyDialog.vue'
import TargetJdDialog from '../../components/targets/TargetJdDialog.vue'
import TargetStageTimeline from '../../components/targets/TargetStageTimeline.vue'
import { CAREER_SITES } from '../../constants/careerSites'
import type { JobProject, StageEvent, TargetStage } from '../../types/project'
import {
  TARGET_STAGE_LABELS,
  TARGET_STAGE_COLORS,
  normalizeTargetStage,
  stageFlowRank,
  isTerminalStage,
} from '../../types/project'
import type { Resume } from '../../types/resume'
import type { JobDescription } from '../../types/job'
import type { ScheduleEvent } from '../../types/schedule'
import type { InterviewPlanResponse } from '../../types/interview'
import { getResumeVersion, listResumes } from '../../api/resume'
import { createJobDescription, listJobDescriptions } from '../../api/job'
import { listScheduleEvents } from '../../api/schedule'
import { listMyInterviewPlans } from '../../api/interview'
import { listStageEvents } from '../../api/project'
import { companyMark } from '../../constants/companyBrands'

interface LinkedResume {
  resumeId: number
  versionId: number
  title: string
  versionNo: number
}

const PIPELINE_STAGES: readonly TargetStage[] = ['applied', 'exam', 'interview', 'hr', 'offer']

const store = useTargetsStore()
const route = useRoute()
const router = useRouter()

const resumes = ref<Resume[]>([])
const jobsByTarget = ref(new Map<number, JobDescription | null>())
const resumeByVersion = ref(new Map<number, LinkedResume>())
const scheduleEvents = ref<ScheduleEvent[]>([])
const interviewPlans = ref<InterviewPlanResponse[]>([])

const busyId = ref<number | null>(null)
const operationError = ref('')
const createOpen = ref(false)
const creating = ref(false)
const createError = ref('')
const deleteTarget = ref<JobProject | null>(null)
const deleting = ref(false)
const deleteError = ref('')
const renameTarget = ref<JobProject | null>(null)
const renamingName = ref('')
const menuTargetId = ref<number | null>(null)
const menuAnchor = ref<{ x: number; y: number }>({ x: 0, y: 0 })
const viewingPlan = ref<InterviewPlanResponse | null>(null)
const applyTarget = ref<JobProject | null>(null)
const applySaving = ref(false)
const applyError = ref('')
const statusFilter = ref<'all' | TargetStage | 'outcome' | 'archived'>('all')
const roleFilter = ref<string>('all')
const viewMode = ref<'board' | 'applications'>('board')
const searchKeyword = ref('')
const directoryKeyword = ref('')

const filteredCareerSites = computed(() => {
  const keyword = directoryKeyword.value.trim().toLowerCase()
  if (!keyword) return CAREER_SITES
  return CAREER_SITES.filter((site) => site.company.toLowerCase().includes(keyword) || site.name.toLowerCase().includes(keyword))
})

// 全量分页展示（段状指示器）
const SITE_PAGE_SIZE = 16
const directoryPage = ref(0)
const sitePageCount = computed(() => Math.max(1, Math.ceil(filteredCareerSites.value.length / SITE_PAGE_SIZE)))
const pagedCareerSites = computed(() => {
  const page = Math.min(directoryPage.value, sitePageCount.value - 1)
  return filteredCareerSites.value.slice(page * SITE_PAGE_SIZE, (page + 1) * SITE_PAGE_SIZE)
})
watch(directoryKeyword, () => { directoryPage.value = 0 })
const jdTarget = ref<JobProject | null>(null)
const jdSaving = ref(false)
const jdError = ref('')
const timelineTarget = ref<JobProject | null>(null)
const timelineEvents = ref<StageEvent[]>([])
const timelineLoading = ref(false)
const directoryOpen = ref(true)
const outcomeTargetId = ref<number | null>(null)
const outcomeAnchor = ref<{ x: number; y: number }>({ x: 0, y: 0 })

const OUTCOME_OPTIONS: Array<{ key: TargetStage; label: string; desc: string }> = [
  { key: 'offer', label: '已拿 Offer', desc: '顺利通过，恭喜' },
  { key: 'pool', label: '泡池子', desc: '投递后无回音，进入人才池' },
  { key: 'screened_out', label: '被筛下', desc: '简历未过筛' },
  { key: 'rejected', label: '被拒', desc: '面试或流程未通过' },
  { key: 'closed', label: '已放弃', desc: '主动结束该计划' },
]

const outcomeTarget = computed(() => store.targets.find((target) => target.id === outcomeTargetId.value) ?? null)
const outcomeMenuStyle = computed(() => ({
  left: `${Math.min(outcomeAnchor.value.x, (typeof window !== 'undefined' ? window.innerWidth : 1200) - 240)}px`,
  top: `${Math.min(outcomeAnchor.value.y + 6, (typeof window !== 'undefined' ? window.innerHeight : 800) - 230)}px`,
}))
function toggleOutcomeMenu(id: number, event?: MouseEvent) {
  if (event) outcomeAnchor.value = { x: event.clientX, y: event.clientY }
  menuTargetId.value = null
  outcomeTargetId.value = outcomeTargetId.value === id ? null : id
}
async function pickOutcome(outcome: TargetStage) {
  const target = outcomeTarget.value
  if (!target) return
  outcomeTargetId.value = null
  await changeStage(target, outcome)
}

// 每个目标的阶段流转历史（用于管线节点下方时间与时间轴弹窗）
const stageEventsByTarget = ref(new Map<number, StageEvent[]>())

async function loadAllStageEvents() {
  const results = await Promise.all(store.targets.map(async (target) => {
    try {
      const res = await listStageEvents(target.id)
      return [target.id, res.data] as const
    } catch { return [target.id, [] as StageEvent[]] as const }
  }))
  stageEventsByTarget.value = new Map(results)
}

function eventsOf(target: JobProject): StageEvent[] {
  return stageEventsByTarget.value.get(target.id) ?? []
}
// 该阶段最近一次进入时间；无留痕返回空
function stageTimeOf(target: JobProject, stage: TargetStage): string {
  const event = [...eventsOf(target)].reverse().find((item) => item.stage === stage)
  if (!event) return ''
  const date = new Date(event.occurredAt)
  if (Number.isNaN(date.getTime())) return ''
  const sameYear = date.getFullYear() === new Date().getFullYear()
  return sameYear
    ? `${date.getMonth() + 1}月${date.getDate()}日`
    : `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

async function openTimeline(target: JobProject) {
  timelineTarget.value = target
  timelineEvents.value = []
  timelineLoading.value = true
  try {
    const res = await listStageEvents(target.id)
    timelineEvents.value = res.data
    stageEventsByTarget.value.set(target.id, res.data)
  } catch { timelineEvents.value = [] } finally {
    timelineLoading.value = false
  }
}

// ── 工具栏筛选 ──
const ROLE_CATEGORIES: Array<[string, string[]]> = [
  ['后端', ['后端', '服务端', 'Java', 'Golang', 'C++']],
  ['前端', ['前端', 'Web', 'FE']],
  ['客户端', ['客户端', 'Android', '安卓', 'iOS', '移动']],
  ['测试', ['测试', 'QA', '质量']],
  ['算法', ['算法', 'AI', '机器学习', '大模型', 'NLP']],
  ['数据', ['数据', '数仓', 'BI']],
  ['产品', ['产品']],
]

function roleCategoryOf(target: JobProject): string {
  const job = jobFor(target)
  const text = `${job?.companyName ?? ''} ${job?.jobTitle ?? ''} ${target.name} ${target.targetRole ?? ''}`
  for (const [label, keywords] of ROLE_CATEGORIES) {
    if (keywords.some((keyword) => text.toLowerCase().includes(keyword.toLowerCase()))) return label
  }
  return '其他'
}

const stageFilterOptions = computed<Array<{ key: 'all' | TargetStage | 'outcome' | 'archived'; label: string; count: number }>>(() => {
  const hasOutcome = (target: JobProject) =>
    target.status === 'active' && ['pool', 'screened_out', 'rejected', 'closed'].includes(stageOf(target))
  return [
    { key: 'all', label: '全部', count: store.targets.length },
    { key: 'applied', label: TARGET_STAGE_LABELS.applied, count: countStage('applied') },
    { key: 'exam', label: TARGET_STAGE_LABELS.exam, count: countStage('exam') },
    { key: 'interview', label: TARGET_STAGE_LABELS.interview, count: countStage('interview') },
    { key: 'hr', label: TARGET_STAGE_LABELS.hr, count: countStage('hr') },
    { key: 'offer', label: 'Offer', count: countStage('offer') },
    { key: 'outcome', label: '已有结果', count: store.targets.filter(hasOutcome).length },
    { key: 'archived', label: '已归档', count: store.targets.filter((target) => target.status === 'archived').length },
  ]
})

function countStage(stage: TargetStage): number {
  return store.targets.filter((target) => target.status === 'active' && stageOf(target) === stage).length
}

// 工具栏折叠：岗位行展示前 7 个，其余收进「其他」；阶段全部直接展示
const roleMoreOpen = ref(false)
const primaryStageOptions = computed(() => stageFilterOptions.value)
const ROLE_PRIMARY_COUNT = 7
const visibleRoleOptions = computed(() => roleFilterOptions.value.slice(0, ROLE_PRIMARY_COUNT))
const extraRoleOptions = computed(() => roleFilterOptions.value.slice(ROLE_PRIMARY_COUNT))

const roleFilterOptions = computed<Array<{ key: string; label: string; count?: number }>>(() => {
  const categories = new Map<string, number>()
  for (const target of store.targets) {
    if (target.status !== 'active') continue
    const category = roleCategoryOf(target)
    categories.set(category, (categories.get(category) ?? 0) + 1)
  }
  return [
    { key: 'all', label: '全部' },
    ...[...categories.entries()].sort((left, right) => right[1] - left[1]).map(([key, count]) => ({ key, label: key, count })),
  ]
})

const visibleTargets = computed(() => sortedTargets.value.filter((target) => {
  if (statusFilter.value === 'archived') {
    if (target.status !== 'archived') return false
  } else if (statusFilter.value === 'outcome') {
    if (target.status !== 'active' || !['pool', 'screened_out', 'rejected', 'closed'].includes(stageOf(target))) return false
  } else if (statusFilter.value !== 'all') {
    if (target.status !== 'active' || stageOf(target) !== statusFilter.value) return false
  }
  if (roleFilter.value !== 'all' && roleCategoryOf(target) !== roleFilter.value) return false
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (keyword) {
    const job = jobFor(target)
    const haystack = `${target.name} ${job?.companyName ?? ''} ${job?.jobTitle ?? ''} ${target.targetRole ?? ''}`.toLowerCase()
    if (!haystack.includes(keyword)) return false
  }
  return true
}))

// ── 投递专区 ──
// 公司名匹配链：JD 的公司名 → 目标名称首段（如「腾讯 Java 后端开发」→ 腾讯）
function siteFor(target: JobProject) {
  const company = companyNameOf(target) || companyFromName(target.name)
  if (!company) return null
  return CAREER_SITES.find((site) => company.includes(site.company)) ?? null
}
function companyFromName(name: string): string {
  const head = name.split(/[·•\-—|]/)[0]?.trim() ?? ''
  return head.split(/\s+/)[0] ?? ''
}

// ── JD 后续录入 ──
function openJd(target: JobProject) {
  jdError.value = ''
  jdTarget.value = target
}
async function saveJd(payload: { companyName: string; jobTitle: string; jdText: string }) {
  if (!jdTarget.value) return
  jdSaving.value = true
  jdError.value = ''
  try {
    const jobResponse = await createJobDescription({
      jobTitle: payload.jobTitle,
      companyName: payload.companyName,
      rawText: payload.jdText,
    })
    await store.updateLinks(jdTarget.value.id, {
      jobDescriptionId: jobResponse.data.id,
      resumeVersionId: jdTarget.value.resumeVersionId,
    })
    jdTarget.value = null
    await loadMaterials()
  } catch (error) {
    jdError.value = error instanceof Error ? error.message : '保存 JD 失败'
  } finally {
    jdSaving.value = false
  }
}

onMounted(async () => {
  if (!store.targets.length) void store.load()
  await Promise.all([loadMaterials(), loadSchedule(), loadInterviewPlans()])
  void loadAllStageEvents()
})

async function loadMaterials() {
  try {
    const [resumeRes, jobRes] = await Promise.all([listResumes(), listJobDescriptions()])
    resumes.value = resumeRes.data
    const jobMap = new Map<number, JobDescription>()
    for (const job of jobRes.data) jobMap.set(job.id, job)
    for (const target of store.targets) {
      jobsByTarget.value.set(target.id, target.jobDescriptionId != null ? (jobMap.get(target.jobDescriptionId) ?? null) : null)
    }
    const versionMap = new Map<number, LinkedResume>()
    for (const resume of resumeRes.data) {
      if (resume.currentVersion?.id != null) {
        versionMap.set(resume.currentVersion.id, { resumeId: resume.id, versionId: resume.currentVersion.id, title: resume.title, versionNo: resume.currentVersion.versionNo })
      }
    }
    // 目标可能关联到非当前版本：按版本 id 回查补齐，找不到就如实不渲染简历行。
    const missingVersionIds = store.targets
      .map((target) => target.resumeVersionId)
      .filter((id): id is number => id != null && !versionMap.has(id))
    await Promise.all(missingVersionIds.map(async (versionId) => {
      try {
        const versionRes = await getResumeVersion(versionId)
        const resume = resumeRes.data.find((item) => item.id === versionRes.data.resumeId)
        if (resume) {
          versionMap.set(versionId, { resumeId: resume.id, versionId, title: resume.title, versionNo: versionRes.data.versionNo })
        }
      } catch { /* 版本已删除则保持未关联 */ }
    }))
    resumeByVersion.value = versionMap
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '读取关联材料失败'
  }
}

async function loadSchedule() {
  try {
    const res = await listScheduleEvents()
    scheduleEvents.value = res.data
  } catch { /* 日程读取失败时只缺时间元信息，不阻塞整页 */ }
}

async function loadInterviewPlans() {
  try {
    const res = await listMyInterviewPlans()
    interviewPlans.value = res.data
  } catch { /* 面试记录读取失败时只缺该区块，不阻塞整页 */ }
}

// ── 卡片派生数据 ──
const sortedTargets = computed(() => {
  const rank = (target: JobProject) => target.status === 'active' ? 0 : 1
  return [...store.targets].sort((left, right) => rank(left) - rank(right))
})

function jobFor(target: JobProject): JobDescription | null {
  return jobsByTarget.value.get(target.id) ?? null
}
function companyNameOf(target: JobProject): string {
  return jobFor(target)?.companyName?.trim() ?? ''
}
function cardTitle(target: JobProject): string {
  const job = jobFor(target)
  if (job) return job.companyName ? `${job.companyName} · ${job.jobTitle}` : job.jobTitle
  return target.name
}
function jdStateLabel(target: JobProject): string {
  const job = jobFor(target)
  if (!job) return '尚未录入岗位 JD'
  if (job.parseStatus === 'succeeded') return 'JD 已录入并解析'
  if (job.parseStatus === 'failed') return 'JD 解析失败'
  return 'JD 已录入'
}
function markFor(target: JobProject) {
  return companyMark(companyNameOf(target) || target.name)
}
function stageOf(target: JobProject): TargetStage {
  return normalizeTargetStage(target.stage)
}
function stageLabel(target: JobProject): string {
  return TARGET_STAGE_LABELS[stageOf(target)]
}
function stageCurrentIndexOf(target: JobProject): number {
  const stage = stageOf(target)
  if (isTerminalStage(stage)) return -1
  return PIPELINE_STAGES.indexOf(stage)
}
function stageLocked(target: JobProject): boolean {
  return target.status === 'archived' || isTerminalStage(stageOf(target))
}
function nodeDisabled(target: JobProject, key: TargetStage): boolean {
  if (stageLocked(target) || busyId.value === target.id) return true
  const curRank = stageFlowRank(stageOf(target))
  const keyRank = stageFlowRank(key)
  return curRank > 0 && keyRank > 0 && keyRank < curRank
}
function pillStyle(target: JobProject) {
  const color = TARGET_STAGE_COLORS[stageOf(target)]
  return { background: `color-mix(in srgb, ${color} 12%, transparent)`, color }
}
function stageStepsOf(target: JobProject) {
  const currentIndex = stageCurrentIndexOf(target)
  return PIPELINE_STAGES.map((key) => ({
    key,
    label: TARGET_STAGE_LABELS[key],
    current: key === stageOf(target),
    reached: currentIndex >= 0 && PIPELINE_STAGES.indexOf(key) <= currentIndex,
  }))
}
function resumeFor(target: JobProject): LinkedResume | null {
  if (!target.resumeVersionId) return null
  return resumeByVersion.value.get(target.resumeVersionId) ?? null
}
function recentPlansOf(target: JobProject): InterviewPlanResponse[] {
  const jdId = target.jobDescriptionId
  if (jdId == null) return []
  return interviewPlans.value
    .filter((plan) => plan.jobDescriptionId === jdId && (target.resumeVersionId == null || plan.resumeVersionId === target.resumeVersionId))
    .sort((left, right) => planTimestamp(right) - planTimestamp(left))
    .slice(0, 5)
}
function planTimestamp(plan: InterviewPlanResponse): number {
  const at = plan.summaryGeneratedAt ?? plan.updatedAt ?? plan.createdAt ?? ''
  const time = new Date(at).getTime()
  return Number.isNaN(time) ? 0 : time
}
function targetEvents(target: JobProject): ScheduleEvent[] {
  const jdId = target.jobDescriptionId
  if (jdId == null) return []
  return scheduleEvents.value
    .filter((event) => event.jobDescriptionId === jdId)
    .sort((left, right) => new Date(left.startTime).getTime() - new Date(right.startTime).getTime())
}
function recentLabel(target: JobProject): string {
  if (target.stageUpdatedAt) {
    const days = daysAgo(target.stageUpdatedAt)
    return days === 0 ? '今天更新状态' : days === 1 ? '昨天更新状态' : `${shortDate(target.stageUpdatedAt)} 更新状态`
  }
  const events = targetEvents(target)
  if (events.length) {
    const last = [...events].reverse().find((event) => new Date(event.startTime).getTime() <= Date.now())
    if (last) return `${shortDate(last.startTime)} ${last.title}`
  }
  return `${shortDate(target.createdAt)} 创建`
}
function viewPlan(plan: InterviewPlanResponse) {
  viewingPlan.value = plan
}

// ── 投递工具 ──
function openApply(target: JobProject) {
  applyError.value = ''
  applyTarget.value = target
}
function closeApply() {
  if (!applySaving.value) applyTarget.value = null
}
async function saveApply(payload: { industry: string; role: string; location: string; notes: string }) {
  if (!applyTarget.value) return
  applySaving.value = true
  applyError.value = ''
  try {
    await store.saveApplication(applyTarget.value.id, payload)
    applyTarget.value = null
  } catch (error) {
    applyError.value = error instanceof Error ? error.message : '保存投递信息失败'
  } finally {
    applySaving.value = false
  }
}

// ── 阶段切换 ──
async function changeStage(target: JobProject, stage: TargetStage) {
  if (stage === stageOf(target)) return
  if (isTerminalStage(stageOf(target))) {
    operationError.value = '该计划已有最终结果，状态已锁定'
    return
  }
  const curRank = stageFlowRank(stageOf(target))
  const nextRank = stageFlowRank(stage)
  if (curRank > 0 && nextRank > 0 && nextRank < curRank) {
    operationError.value = '阶段只能向前推进，不能回退'
    return
  }
  busyId.value = target.id
  operationError.value = ''
  try {
    await store.setStage(target.id, stage)
    void loadAllStageEvents()
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '更新求职阶段失败'
  } finally {
    busyId.value = null
  }
}

// ── 简历跳转 ──
function openResume(linked: LinkedResume) {
  void router.push({ name: 'resume-editor', query: { resumeId: String(linked.resumeId), versionId: String(linked.versionId) } })
}

// ── ⋯ 菜单 ──
const menuTarget = computed(() => store.targets.find((target) => target.id === menuTargetId.value) ?? null)
const menuStyle = computed(() => ({
  left: `${Math.min(menuAnchor.value.x, (typeof window !== 'undefined' ? window.innerWidth : 1200) - 170)}px`,
  top: `${Math.min(menuAnchor.value.y + 6, (typeof window !== 'undefined' ? window.innerHeight : 800) - 150)}px`,
}))
function toggleMenu(id: number, event?: MouseEvent) {
  if (event) menuAnchor.value = { x: event.clientX, y: event.clientY }
  menuTargetId.value = menuTargetId.value === id ? null : id
}
function closeMenu() { menuTargetId.value = null; outcomeTargetId.value = null }

// ── 重命名 ──
function beginRename(target: JobProject, event?: MouseEvent) {
  if (event) toggleMenu(target.id, event)
  closeMenu()
  renameTarget.value = target
  renamingName.value = target.name
  operationError.value = ''
}
function cancelRename() { renameTarget.value = null; renamingName.value = '' }
async function saveRename() {
  if (!renameTarget.value) return
  const name = renamingName.value.trim()
  if (!name) { operationError.value = '名称不能为空'; return }
  busyId.value = renameTarget.value.id
  try {
    await store.rename(renameTarget.value.id, name)
    cancelRename()
  } catch (error) {
    operationError.value = error instanceof Error ? error.message : '重命名失败'
  } finally { busyId.value = null }
}

// ── 归档 / 恢复 / 删除 ──
async function archiveFromMenu(target: JobProject, event?: MouseEvent) {
  if (event) toggleMenu(target.id, event)
  closeMenu()
  busyId.value = target.id
  try { await store.archive(target.id) } finally { busyId.value = null }
}
async function restoreFromMenu(target: JobProject, event?: MouseEvent) {
  if (event) toggleMenu(target.id, event)
  closeMenu()
  busyId.value = target.id
  try { await store.restore(target.id) } finally { busyId.value = null }
}
function openDelete(target: JobProject) {
  closeMenu()
  deleteTarget.value = target
  deleteError.value = ''
}
function closeDelete() { if (!deleting.value) deleteTarget.value = null }
async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true; deleteError.value = ''
  try { await store.remove(deleteTarget.value.id); deleteTarget.value = null }
  catch (error) { deleteError.value = error instanceof Error ? error.message : '删除求职目标失败' }
  finally { deleting.value = false }
}

// ── 创建（含 JD 录入）──
function openCreate() { createError.value = ''; createOpen.value = true }
function closeCreate() { if (!creating.value) createOpen.value = false }
async function createTarget(payload: TargetDraftPayload) {
  creating.value = true; createError.value = ''
  try {
    let jobDescriptionId: number | undefined
    // 只有真正粘贴了 JD 原文才创建岗位记录；仅填公司/岗位时不虚构 JD。
    if (payload.jdText) {
      const jobResponse = await createJobDescription({
        jobTitle: payload.jobTitle || payload.name,
        ...(payload.companyName ? { companyName: payload.companyName } : {}),
        rawText: payload.jdText,
      })
      jobDescriptionId = jobResponse.data.id
    }
    const created = await store.create({
      name: payload.name,
      ...(jobDescriptionId != null ? { jobDescriptionId } : {}),
      ...(payload.resumeVersionId != null ? { resumeVersionId: payload.resumeVersionId } : {}),
    })
    jobsByTarget.value.set(created.id, null)
    createOpen.value = false
    await loadMaterials()
  } catch (error) {
    createError.value = error instanceof Error ? error.message : '创建求职目标失败'
  } finally {
    creating.value = false
  }
}

// ── 工具 ──
function shortDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
function daysAgo(value: string): number {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return -1
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  return Math.round((startOfDay(new Date()) - startOfDay(date)) / 86400000)
}

watch(() => route.query.targetId, () => { /* 兼容旧入口 query，不再做详情预选 */ })
</script>

<style scoped>
/* 页面级纯白画布：四周均匀露出窄幅底色，圆角浮层 */
.targets-page{position:fixed;top:0;right:0;bottom:0;left:92px;display:flex;flex-direction:column;padding:26px 64px 50px;background:#fff;z-index:5}
.plan-scroll{flex:1;min-height:0;overflow-y:auto;padding:10px 0 0}

/* 视图切换 */
.view-switch{display:inline-flex;gap:2px;border:1px solid var(--line-subtle,rgba(28,31,35,.1));border-radius:10px;background:#f4f4f2;padding:3px;margin-right:10px}
.view-switch button{border:0;border-radius:8px;background:none;padding:7px 14px;font:inherit;font-size:12.5px;font-weight:600;color:#5c625d;cursor:pointer;transition:all .15s ease-out}
.view-switch button:hover{color:#17181a}
.view-switch button.on{background:#fff;color:#17181a;box-shadow:0 1px 3px rgba(16,24,40,.1)}

/* 直达官网目录 */
.sites-panel{margin-top:26px;padding:10px 18px 14px;border:1px solid var(--line-subtle,rgba(28,31,35,.08));border-radius:16px;background:#fcfcfb;transition:padding .2s ease-out}
.sites-panel.collapsed{padding:6px 18px}
.sites-toggle{display:flex;align-items:center;gap:9px;width:100%;border:0;background:none;padding:6px 2px;cursor:pointer;text-align:left}
.tl-chevron{display:inline-block;color:var(--muted,#989893);font-size:11px;transition:transform .18s ease-out}
.tl-chevron.open{transform:rotate(180deg)}
.sites-toggle h4{margin:0;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--muted,#989893)}
.sites-toggle .toolbar-search{margin-left:auto}
.sites-body{padding-top:4px}
.sites-search{margin:2px 0 12px}
.sites-pager{display:flex;align-items:center;justify-content:center;gap:7px;margin-top:14px}
.pager-arrow{display:grid;place-items:center;width:22px;height:22px;border:0;border-radius:50%;background:none;color:#8a9089;font-size:15px;line-height:1;cursor:pointer}
.pager-arrow:hover:not(:disabled){background:#eef0ee;color:#17181a}
.pager-arrow:disabled{opacity:.35;cursor:default}
.pager-dash{width:7px;height:7px;padding:0;border:0;border-radius:999px;background:#d6d8d3;cursor:pointer;transition:all .18s cubic-bezier(.2,.7,.3,1)}
.pager-dash:hover{background:#b9bcb5}
.pager-dash.on{width:22px;background:var(--brand,#168b68)}
.zone-kicker{margin:4px 0 8px;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--muted,#989893)}
.sites-head{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:12px}
.sites-head h4{margin:0;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--muted,#989893)}
.sites-count{margin-left:8px;letter-spacing:0;font-weight:550;color:#b0b0ab}
.sites-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}
@media(max-width:1099px){.sites-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:820px){.sites-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
.site-card{display:flex;align-items:center;gap:9px;padding:9px 11px;border:1px solid var(--line-subtle,rgba(28,31,35,.08));border-radius:10px;background:#fff;text-decoration:none;color:inherit;transition:border-color .15s ease-out,transform .15s ease-out,box-shadow .15s ease-out}
.site-card:hover{border-color:var(--brand-soft,rgba(22,139,104,.4));transform:translateY(-1px);box-shadow:0 4px 14px rgba(16,24,40,.06)}
.site-logo{flex:0 0 auto;width:22px;height:22px;object-fit:contain}
.site-letter{flex:0 0 auto;display:grid;place-items:center;width:22px;height:22px;border-radius:6px;color:#fff;font-size:11px;font-weight:700}
.site-copy{min-width:0;flex:1;display:grid}
.site-copy strong{font-size:12.5px;font-weight:600;color:#23292e;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.site-copy small{font-size:10.5px;color:var(--muted,#a2a29d);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.site-card svg{flex:0 0 auto;color:#c3c6c1}
.site-card:hover svg{color:var(--brand,#168b68)}

/* 投递专区表格 */
.apply-table{width:100%;border-collapse:collapse;background:#fff}
.apply-table th{text-align:left;padding:11px 14px;font-size:11px;font-weight:650;letter-spacing:.06em;color:var(--muted,#989893);border-bottom:1px solid var(--line-subtle,rgba(28,31,35,.09));white-space:nowrap}
.apply-table td{padding:13px 14px;border-bottom:1px solid var(--line-subtle,rgba(28,31,35,.06));font-size:13px;color:#23292e;vertical-align:middle}
.apply-table tbody tr{transition:background .13s ease-out}
.apply-table tbody tr:hover{background:#fafbfa}
.apply-table tbody tr.archived{opacity:.55}
.cell-target{display:flex;align-items:center;gap:10px;min-width:0}
.row-logo{flex:0 0 auto;width:24px;height:24px;object-fit:contain}
.row-letter{flex:0 0 auto;display:grid;place-items:center;width:24px;height:24px;border-radius:7px;color:#fff;font-size:11.5px;font-weight:700}
.cell-target-copy{display:grid;gap:1px;min-width:0}
.cell-target-copy strong{font-size:13px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.cell-target-copy small{font-size:11px;color:var(--muted,#a2a29d)}
.cell-muted{color:#6d7570}
.cell-notes{max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#6d7570}
.stage-pill.mini{padding:3px 9px;font-size:11px}
.site-link{display:inline-flex;align-items:center;gap:5px;color:var(--brand,#168b68);text-decoration:none;font-size:12.5px;font-weight:600}
.site-link:hover{text-decoration:underline}
.cell-actions .text-btn{border:0;background:none;padding:5px 9px;border-radius:7px;color:#3c443f;font:inherit;font-size:12.5px;font-weight:600;cursor:pointer}
.cell-actions .text-btn:hover{background:#f1f2f1;color:var(--brand,#168b68)}

/* 卡片上的 JD 补录入口 */
.jd-add{justify-self:start;display:inline-flex;align-items:center;gap:4px;margin-top:1px;border:0;background:none;padding:0;color:var(--brand,#168b68);font-size:12px;font-weight:650;cursor:pointer}
.jd-add:hover{text-decoration:underline}
.plan-error{margin:0 0 12px;color:var(--danger)}
.plan-empty{padding:26px 4px;color:var(--muted);font-size:13px}
.plan-empty.big{font-size:14px;line-height:1.7}

/* 工具栏 */
.plan-toolbar{display:flex;flex-direction:column;align-items:stretch;gap:2px;padding:12px 0 6px;border-bottom:1px solid var(--line-subtle,rgba(28,31,35,.07))}
.toolbar-row{display:flex;align-items:center;gap:6px;flex-wrap:wrap;min-height:34px}
.toolbar-row .toolbar-kicker{flex:0 0 auto}
.more-toggle{border-style:dashed;border-color:var(--line,rgba(28,31,35,.18))}
.more-toggle.on{border-style:solid}
.toolbar-kicker{color:var(--muted,#989893);font-size:11px;font-weight:650;letter-spacing:.06em;margin-right:2px}
.filter-pill{display:inline-flex;align-items:center;gap:5px;border:1px solid transparent;border-radius:999px;background:none;padding:5px 11px;font-size:12px;font-weight:550;color:#5c625d;cursor:pointer;transition:all .14s ease-out}
.filter-pill:hover{background:#f3f4f3;color:#17181a}
.filter-pill.on{border-color:var(--brand-soft,rgba(22,139,104,.4));background:var(--brand-soft,rgba(22,139,104,.1));color:var(--brand,#168b68);font-weight:650}
.filter-pill em{font-style:normal;font-size:10.5px;font-weight:650;background:rgba(28,31,35,.07);border-radius:999px;padding:0 5px;line-height:15px}
.filter-pill.on em{background:rgba(22,139,104,.16)}
.toolbar-search{display:flex;align-items:center;gap:7px;margin-left:auto;border:1px solid var(--line-subtle,rgba(28,31,35,.12));border-radius:9px;background:#fff;padding:6px 11px;color:#a2a29d}
.toolbar-search:focus-within{border-color:var(--brand-soft,rgba(22,139,104,.5))}
.toolbar-search input{border:0;outline:none;background:none;width:260px;font:inherit;font-size:12.5px;color:#23292e}

/* 卡片网格 */
.plan-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(400px,1fr));gap:18px;padding-top:14px}

/* 卡片 */
.plan-card{position:relative;display:flex;flex-direction:column;gap:14px;border:1px solid var(--line-subtle,rgba(28,31,35,.09));border-radius:16px;background:#fff;padding:18px 18px 14px;transition:transform .18s cubic-bezier(.2,.7,.3,1),box-shadow .18s ease-out,border-color .18s ease-out;animation:card-in .34s cubic-bezier(.2,.7,.3,1) both;animation-delay:calc(var(--i,0)*45ms)}
.plan-card:hover{transform:translateY(-2px);border-color:var(--brand-soft,rgba(22,139,104,.35));box-shadow:0 12px 32px rgba(16,24,40,.08)}
.plan-card.archived{opacity:.62;filter:saturate(.55)}
@keyframes card-in{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.plan-card{animation:none;transition:none}.plan-card:hover{transform:none;box-shadow:none}}

/* 头部 */
.card-head{display:flex;align-items:center;gap:12px}
.logo-mark{flex:0 0 auto;display:grid;place-items:center;width:38px;height:38px;border-radius:11px;font-size:17px;font-weight:700;user-select:none}
.logo-img{flex:0 0 auto;width:34px;height:34px;object-fit:contain}
.head-copy{min-width:0;flex:1}
.head-copy h3{margin:0;font-size:15px;font-weight:650;letter-spacing:-.01em;color:#17181a;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.head-copy small{display:block;margin-top:2px;color:var(--muted,#989893);font-size:12px}
.head-side{display:flex;align-items:center;gap:6px}
.stage-pill{flex:0 0 auto;padding:4px 10px;border-radius:999px;font-size:11.5px;font-weight:650;letter-spacing:.01em;background:var(--brand-soft,rgba(22,139,104,.1));color:var(--brand,#168b68)}
.stage-pill.offer{background:#fff7e6;color:#ad6800}
.stage-pill.link{cursor:pointer;border:0;font-family:inherit}
.stage-pill.link:hover{filter:brightness(.96)}
.stage-pill em,.stage-pill .pill-time{font-style:normal;margin-left:6px;font-weight:550;font-size:10.5px;opacity:.85}
.stage-pill.closed{background:#f1f1ee;color:#989893}
.menu-trigger{border:0;background:none;border-radius:8px;width:26px;height:26px;color:var(--muted,#989893);font-size:15px;cursor:pointer}
.menu-trigger:hover{background:#f1f1ee;color:#17181a}

/* 阶段管线 */
.pipeline{display:flex;align-items:flex-start;margin:0;padding:2px 0 0;list-style:none}
.pipeline-item{position:relative;flex:1;display:flex;align-items:center;justify-content:flex-start}
.node{display:grid;justify-items:center;gap:5px;border:0;background:none;padding:0;color:#a2a29d;cursor:pointer;min-width:0}
.node:hover:not(:disabled) .node-label{color:#17181a}
.node:disabled{cursor:default;opacity:.75}
.dot{width:13px;height:13px;border-radius:50%;background:#fff;border:2px solid #cfd2cd;transition:all .16s ease-out}
.node.done .dot{border-color:var(--brand,#168b68);background:var(--brand-soft,rgba(22,139,104,.25))}
.node.current .dot{border-color:var(--brand,#168b68);background:var(--brand,#168b68);animation:pulse-ring 2.4s ease-out infinite}
@keyframes pulse-ring{0%{box-shadow:0 0 0 0 rgba(22,139,104,.28)}70%{box-shadow:0 0 0 7px rgba(22,139,104,0)}100%{box-shadow:0 0 0 0 rgba(22,139,104,0)}}
@media (prefers-reduced-motion: reduce){.node.current .dot{animation:none}}
.node-label{max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px;font-weight:550}
.node.current .node-label{color:var(--brand,#168b68);font-weight:700}
.node-time{margin-top:-1px;font-size:10px;color:var(--muted,#a2a29d);font-variant-numeric:tabular-nums}
.node.done .node-label{color:#5f6f66}
.connector{flex:1;height:2px;margin:0 4px 16px;border-radius:1px;background:#eceeea;align-self:flex-start;transform:translateY(6px)}
.connector.filled{background:var(--brand-soft,rgba(22,139,104,.4))}

/* 简历行 */
.resume-row{display:flex;align-items:center;gap:10px}
.resume-chip{display:inline-flex;align-items:center;gap:6px;border-radius:999px;padding:6px 12px;font-size:12px;font-weight:600;cursor:pointer;transition:all .15s ease-out}
.resume-chip.linked{border:1px solid var(--line-subtle,rgba(28,31,35,.12));background:#fafaf8;color:#2c332e}
.resume-chip.linked:hover{border-color:var(--brand-soft,rgba(22,139,104,.45));color:var(--brand,#168b68)}
.resume-chip.unlinked{border:1px dashed var(--line,rgba(28,31,35,.2));background:#fff;color:var(--muted,#74828c)}
.resume-chip.unlinked:hover{color:var(--brand,#168b68);border-color:var(--brand-soft,rgba(22,139,104,.45))}
.resume-meta{color:var(--muted,#a2a29d);font-size:11.5px}

/* 近期面试 */
.interviews{min-height:96px}
.interviews h4{margin:0 0 8px;font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--muted,#989893)}
.interviews-count{margin-left:7px;letter-spacing:0;font-weight:550;color:#b0b0ab}
.interviews ul{margin:0;padding:0;list-style:none;display:grid;gap:2px}
.plan-row{display:flex;align-items:center;justify-content:space-between;gap:10px;width:100%;text-align:left;border:1px solid transparent;background:none;border-radius:9px;padding:9px 10px;cursor:pointer;transition:border-color .14s ease-out,background .14s ease-out}
.plan-row:hover{background:#f6f7f6;border-color:var(--line-subtle,rgba(28,31,35,.06))}
.plan-main{min-width:0;display:grid;gap:1px}
.plan-row strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13.5px;font-weight:600;color:#23292e}
.plan-row:hover strong{color:var(--brand,#168b68)}
.plan-main small{font-size:11px;color:var(--muted,#a2a29d)}
.plan-meta{flex:0 0 auto;display:inline-flex;align-items:center;gap:8px;color:var(--muted,#989893);font-size:12px;font-variant-numeric:tabular-nums}
.score{display:inline-grid;place-items:center;min-width:26px;padding:1px 6px;border-radius:999px;background:var(--brand-soft,rgba(22,139,104,.1));color:var(--brand,#168b68);font-style:normal;font-weight:700;font-size:11px}
.interviews-empty{margin:0;color:#b0b0ab;font-size:12px;padding:2px 0 4px}

/* 结果标记 */
.outcome-row{display:flex;align-items:center;gap:10px;min-height:26px}
.outcome-trigger{display:inline-flex;align-items:center;gap:6px;border:1px dashed var(--line,rgba(28,31,35,.22));border-radius:999px;background:#fff;color:#5c625d;padding:5px 13px;font-size:12px;font-weight:600;cursor:pointer;transition:all .14s ease-out}
.outcome-trigger:hover:not(:disabled){color:var(--brand,#168b68);border-color:var(--brand-soft,rgba(22,139,104,.5))}
.outcome-trigger:disabled{opacity:.55;cursor:default}
.chevron{font-size:10px}
.locked-note{margin:0;display:flex;align-items:center;gap:7px;color:#8a9089;font-size:12px}
.error-text{color:var(--danger)}
.menu-popover.outcome-menu{min-width:236px;padding:6px}
.outcome-menu-title{margin:4px 10px 6px;font-size:11px;font-weight:650;letter-spacing:.06em;color:var(--muted,#989893)}
.outcome-option{display:flex;align-items:center;gap:10px;width:100%;text-align:left;border:0;background:none;border-radius:9px;padding:8px 10px;cursor:pointer}
.outcome-option:hover{background:#f4f5f4}
.outcome-dot{flex:0 0 auto;width:9px;height:9px;border-radius:50%}
.outcome-copy{display:grid;gap:1px;min-width:0}
.outcome-copy strong{font-size:12.5px;font-weight:600;color:#23292e}
.outcome-copy small{font-size:11px;color:var(--muted,#a2a29d)}
/* 底部元信息 */
.card-foot{display:flex;align-items:center;gap:6px;border-top:1px solid var(--line-subtle,rgba(28,31,35,.07));padding-top:10px;color:var(--muted,#a2a29d);font-size:11.5px;font-variant-numeric:tabular-nums}

/* ⋯ 菜单 */
.menu-backdrop{position:fixed;inset:0;z-index:30}
.menu-popover{position:fixed;z-index:31;display:grid;min-width:148px;padding:5px;border:1px solid var(--line-subtle,rgba(28,31,35,.1));border-radius:12px;background:#fff;box-shadow:0 14px 36px rgba(16,24,40,.14);animation:menu-in .14s ease-out}
@keyframes menu-in{from{opacity:0;transform:translateY(-4px)}to{opacity:1;transform:none}}
.menu-popover button{border:0;background:none;text-align:left;padding:8px 11px;border-radius:8px;font:inherit;font-size:13px;color:#23292e;cursor:pointer}
.menu-popover button:hover{background:#f4f5f4}
.menu-popover button.danger{color:var(--danger)}

/* 迷你弹窗 */
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.mini-dialog{width:min(380px,100%);border-radius:14px;background:#fff;padding:20px;box-shadow:0 22px 60px rgba(0,0,0,.35);animation:card-in .18s ease-out both}
.mini-dialog h2{margin:0 0 14px;font-size:17px}
.mini-dialog form{display:grid;gap:12px}
.mini-dialog input{border:1px solid var(--line,#d6dfe4);border-radius:9px;padding:10px 12px;font:inherit}
.error{margin:0;color:var(--danger);font-size:13px}
.mini-dialog footer{display:flex;justify-content:flex-end;gap:8px}
.mini-dialog footer button{border:1px solid var(--line,#d6dfe4);border-radius:9px;background:#fff;padding:8px 13px;font:inherit;cursor:pointer}
.mini-dialog footer .primary{border-color:var(--brand);background:var(--brand);color:#fff;font-weight:600}

.btn-primary{border:1px solid #17181a;border-radius:var(--radius-control,10px);background:#17181a;color:#fff;padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer;transition:background .15s ease-out}
.btn-primary:hover{background:#000}
</style>
