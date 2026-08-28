<template>
  <div class="interview-review-page" data-test="interview-review-page">
    <header class="review-topbar">
      <div class="review-context-line">
        <span class="review-mode-mark" :data-mode="mode">{{ modeLabel }}</span>
        <span v-if="planTitle" class="review-context-separator" aria-hidden="true">{{ ' · ' }}</span>
        <span v-if="planTitle" class="review-context-title">{{ planTitle }}</span>
        <span class="review-context-status">已完成 · 可复盘</span>
      </div>
      <button class="review-back-link" type="button" @click="emit('back-home')">返回面试主页 <span aria-hidden="true">→</span></button>
    </header>

    <div class="review-progress" aria-label="复盘步骤">
      <div class="review-progress-step is-complete">
        <span>01</span><strong>回答记录</strong><small>完整保留本次上下文</small>
      </div>
      <div class="review-progress-line" aria-hidden="true"></div>
      <div class="review-progress-step is-current">
        <span>02</span><strong>核心反馈</strong><small>定位最需要改进的地方</small>
      </div>
      <div class="review-progress-line" aria-hidden="true"></div>
      <div class="review-progress-step">
        <span>03</span><strong>下一步</strong><small>带着行动回到练习</small>
      </div>
    </div>

    <div class="review-layout">
      <main class="review-main">

      <section class="review-focus" data-test="review-focus">
        <div class="review-focus-heading">
          <span class="review-section-label">核心问题</span>
          <span class="review-date">{{ completedAtLabel }}</span>
        </div>
        <strong data-test="review-core-issue">{{ coreIssue }}</strong>
        <p>{{ evidence }}</p>
      </section>

      <section class="review-next" data-test="review-next-action">
        <div>
          <span class="review-section-label">下一步行动</span>
          <strong>{{ nextAction }}</strong>
        </div>
        <button type="button" class="review-primary-action" @click="emit('re-practice')">再次练习 <span aria-hidden="true">→</span></button>
      </section>

      <section class="review-question-section" data-test="review-question-timeline">
        <div class="review-section-head">
          <div>
            <span class="review-section-label">Answer timeline</span>
            <h2>逐题复盘</h2>
          </div>
          <span>{{ history.length }} 题</span>
        </div>
        <div v-if="history.length" class="review-question-list">
          <details v-for="item in history" :key="`${item.questionIndex}-${item.questionText}`" class="review-question-row" data-test="review-question-row">
            <summary>
              <!-- 后端题号从 1 开始；这里直接展示持久化题号，避免复盘把第 1 题显示为 02。 -->
              <span class="review-question-index">{{ String(item.questionIndex).padStart(2, '0') }}</span>
              <span class="review-question-summary">
                <strong>{{ item.questionText }}</strong>
                <small>{{ item.provenanceLabel || sourceTypeLabel(item.source) }}</small>
              </span>
              <span class="review-question-score">{{ scoreLabel(item) }}</span>
              <span class="review-question-chevron" aria-hidden="true">⌄</span>
            </summary>
            <div class="review-question-detail">
              <div>
                <span class="review-detail-label">我的回答</span>
                <p>{{ item.answerText || '本题未记录回答。' }}</p>
              </div>
              <div v-if="item.evaluation">
                <span class="review-detail-label">本题反馈</span>
                <p>{{ evaluationLine(item) }}</p>
              </div>
            </div>
          </details>
        </div>
        <p v-else class="review-empty">本次练习还没有可复盘的回答。</p>
      </section>
      </main>

      <aside class="review-aside">
        <section class="review-context-panel">
          <span class="review-section-label">Context snapshot</span>
          <h2>本次上下文</h2>
          <p class="review-source-line">{{ sourceLine }}</p>
          <dl>
            <template v-for="item in contextItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </template>
          </dl>
        </section>

        <section class="review-score-panel" data-test="review-score-card">
          <div class="review-section-head">
            <div>
              <span class="review-section-label">真实评分</span>
              <h2>能力维度</h2>
            </div>
          </div>
          <div v-if="scoreItems.length" class="review-score-list">
            <div v-for="item in scoreItems" :key="item.label" class="review-score-item">
              <span>{{ item.label }}</span>
              <div class="review-score-track"><i v-if="item.value != null" :style="{ width: `${item.value}%` }"></i></div>
              <strong>{{ item.value == null ? '—' : item.value }}</strong>
            </div>
          </div>
          <p v-else class="review-empty-score" data-test="review-empty-score">暂无真实评分，完成带评价的练习后这里会显示能力维度。</p>
        </section>

        <button type="button" class="review-aside-action" @click="emit('re-practice')">用相同上下文再次练习 <span aria-hidden="true">→</span></button>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  InterviewMode,
  InterviewPlanResponse,
  InterviewStatusResponse,
  PerQuestionScore,
  SessionHistoryItem,
} from '../../types/interview'

const props = defineProps<{
  session: InterviewStatusResponse
  plan: InterviewPlanResponse | null
  history: SessionHistoryItem[]
  scores: PerQuestionScore[]
}>()

const emit = defineEmits<{
  'back-home': []
  're-practice': []
}>()

const MODE_LABELS: Record<InterviewMode, string> = {
  ROLE_BASED: '岗位模拟',
  KNOWLEDGE_TRAINING: '知识训练',
  EXPERIENCE_SIMULATION: '真题演练',
}

const mode = computed<InterviewMode>(() => props.plan?.mode ?? 'ROLE_BASED')
const modeLabel = computed(() => MODE_LABELS[mode.value])
const planTitle = computed(() => props.plan?.title || props.session.personaName || '')
const snapshot = computed(() => props.plan?.startContextSnapshot ?? {})
const summary = computed<Record<string, unknown>>(() => {
  if (!props.session.summaryJson) return {}
  try {
    return JSON.parse(props.session.summaryJson) as Record<string, unknown>
  } catch {
    return {}
  }
})

const sourceLine = computed(() => {
  const values: string[] = []
  const snap = snapshot.value
  if (typeof snap.jobProjectName === 'string' && snap.jobProjectName) values.push(snap.jobProjectName)
  if (typeof snap.resumeTitle === 'string' && snap.resumeTitle) {
    values.push(snap.resumeVersionNo != null ? `${snap.resumeTitle} · V${snap.resumeVersionNo}` : snap.resumeTitle)
  }
  if (Array.isArray(snap.knowledgeDocumentTitles) && snap.knowledgeDocumentTitles.length) values.push(`资料：${snap.knowledgeDocumentTitles.join('、')}`)
  if (typeof snap.questionSetTitle === 'string' && snap.questionSetTitle) values.push(`题集：${snap.questionSetTitle}`)
  if (typeof snap.reviewMode === 'string' && snap.reviewMode) values.push(`回顾：${reviewModeLabel(snap.reviewMode)}`)
  return values.length ? values.join(' · ') : '开始上下文快照未提供名称信息'
})

const completedAtLabel = computed(() => {
  const value = props.plan?.updatedAt || props.plan?.createdAt
  if (!value) return '时间未记录'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '时间未记录' : date.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
})

function textList(value: unknown): string[] {
  if (Array.isArray(value)) return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
  return typeof value === 'string' && value.trim() ? [value.trim()] : []
}

const coreIssue = computed(() => props.plan?.summary?.crossWeaknesses?.[0]
  || textList(summary.value.weaknesses)[0]
  || props.history.flatMap((item) => textList(item.evaluation?.weaknesses))[0]
  || '本次练习还没有生成可引用的核心问题。')

const evidence = computed(() => props.plan?.summary?.overallSummary
  || textList(summary.value.suggestions)[0]
  || props.history.map((item) => item.evaluation?.referenceAnswer).find((value): value is string => Boolean(value))
  || '完成一次带评价的练习后，这里会显示与回答对应的证据。')

const nextAction = computed(() => props.plan?.summary?.suggestions?.[0]
  || textList(summary.value.suggestions)[0]
  || props.history.flatMap((item) => textList(item.evaluation?.suggestions))[0]
  || '回到练习准备，选择一个明确的训练来源继续。')

const contextItems = computed(() => {
  const values: Array<{ label: string; value: string }> = [{ label: '练习模式', value: modeLabel.value }]
  const snap = snapshot.value
  if (typeof snap.jobProjectName === 'string' && snap.jobProjectName) values.push({ label: '求职计划', value: snap.jobProjectName })
  if (typeof snap.resumeTitle === 'string' && snap.resumeTitle) values.push({ label: '简历版本', value: snap.resumeVersionNo != null ? `${snap.resumeTitle} · V${snap.resumeVersionNo}` : snap.resumeTitle })
  if (Array.isArray(snap.knowledgeDocumentTitles) && snap.knowledgeDocumentTitles.length) values.push({ label: '知识资料', value: snap.knowledgeDocumentTitles.join('、') })
  if (typeof snap.questionSetTitle === 'string' && snap.questionSetTitle) values.push({ label: '真实题集', value: snap.questionSetTitle })
  if (typeof snap.reviewMode === 'string' && snap.reviewMode) values.push({ label: '答题回顾', value: reviewModeLabel(snap.reviewMode) })
  if (props.session.personaName) values.push({ label: '面试官', value: props.session.personaName })
  return values
})

const scoreItems = computed<Array<{ label: string; value: number | null }>>(() => {
  if (!props.scores.length) return []
  const dimensions = [
    ['表达清晰度', 'clarity'],
    ['岗位相关性', 'relevance'],
    ['回答深度', 'depth'],
    ['回答结构', 'structure'],
    ['证据具体性', 'evidence'],
  ] as const
  return dimensions.map(([label, key]) => {
    const values = props.scores.map((score) => {
      const raw = key === 'evidence' ? (score.evidence ?? score.accuracy) : score[key]
      return typeof raw === 'number' && Number.isFinite(raw) ? raw : null
    }).filter((value): value is number => value != null)
    const value = values.length
      ? Math.round((values.reduce((sum, item) => sum + item, 0) / values.length) * 10)
      : null
    return { label, value }
  })
})

function sourceTypeLabel(source?: string | null) {
  if (source === 'AI_FOLLOW_UP') return 'AI 追问'
  if (source === 'IMPORTED_EXPERIENCE') return '导入面经原题'
  if (source === 'USER_MANUAL') return '手动录入原题'
  if (source === 'GENERATED_PRACTICE') return '练习题'
  return '本次练习题目'
}

function reviewModeLabel(value: string) {
  if (value === 'PER_QUESTION') return '逐题回顾'
  if (value === 'SOURCE_ONLY') return '只看原题'
  return '结束后复盘'
}

function scoreLabel(item: SessionHistoryItem) {
  const score = item.evaluation?.score
  if (!score) return '未评分'
  const values = [score.clarity, score.relevance, score.depth, score.structure || 0, score.evidence || score.accuracy].filter((value) => value > 0)
  return values.length ? `${Math.round(values.reduce((sum, value) => sum + value, 0) / values.length * 10)}` : '未评分'
}

function evaluationLine(item: SessionHistoryItem) {
  const evaluation = item.evaluation
  if (!evaluation) return '暂无逐题反馈。'
  return textList(evaluation.weaknesses)[0]
    || textList(evaluation.suggestions)[0]
    || textList(evaluation.strengths)[0]
    || '暂无文字反馈。'
}
</script>

<style scoped>
.interview-review-page{display:grid;grid-template-columns:minmax(0,1fr) 246px;gap:54px;min-height:0;padding:18px 0 28px;color:var(--ink);background:var(--surface-solid,#fff)}.review-main{min-width:0}.review-header{display:flex;align-items:flex-start;justify-content:space-between;gap:28px;padding-bottom:24px;border-bottom:1px solid var(--border-subtle)}.review-kicker,.review-section-label{display:block;color:var(--muted);font-size:10px;font-weight:700;letter-spacing:.12em;text-transform:uppercase}.review-header h1{margin:9px 0 7px;font-size:27px;font-weight:700;letter-spacing:-.035em;line-height:1.25}.review-header p{max-width:680px;margin:0;color:var(--muted);font-size:12px;line-height:1.6}.review-back-link,.review-aside-action{padding:5px 0;border:0;background:transparent;color:var(--copy);font-size:12px;cursor:pointer}.review-back-link:hover,.review-aside-action:hover{color:var(--brand)}.review-focus{display:grid;gap:10px;padding:30px 0 28px;border-bottom:1px solid var(--border-subtle)}.review-focus-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.review-date{color:var(--muted);font-size:11px}.review-focus>strong{max-width:760px;font-size:20px;font-weight:700;line-height:1.45}.review-focus>p{max-width:780px;margin:0;color:var(--copy);font-size:13px;line-height:1.85}.review-next{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;padding:24px 0;border-bottom:1px solid var(--border-subtle)}.review-next>div{display:grid;gap:8px}.review-next strong{max-width:650px;color:var(--copy);font-size:13px;font-weight:600;line-height:1.7}.review-primary-action{display:inline-flex;align-items:center;gap:9px;padding:9px 14px;border:1px solid var(--ink);border-radius:7px;background:var(--ink);color:#fff;font-size:12px;cursor:pointer}.review-primary-action:hover{background:var(--brand);border-color:var(--brand)}.review-question-section{padding-top:26px}.review-section-head{display:flex;align-items:flex-end;justify-content:space-between;gap:16px}.review-section-head h2,.review-context-panel h2,.review-score-panel h2{margin:7px 0 0;font-size:17px;font-weight:700;letter-spacing:-.02em}.review-section-head>span{color:var(--muted);font-size:11px}.review-question-list{margin-top:16px;border-top:1px solid var(--border-subtle)}.review-question-row{border-bottom:1px solid var(--border-subtle)}.review-question-row summary{display:grid;grid-template-columns:34px minmax(0,1fr) 52px 18px;gap:14px;align-items:center;padding:15px 0;list-style:none;cursor:pointer}.review-question-row summary::-webkit-details-marker{display:none}.review-question-index{color:var(--muted);font-size:11px;font-variant-numeric:tabular-nums}.review-question-summary{display:grid;gap:4px;min-width:0}.review-question-summary strong{overflow:hidden;color:var(--ink);font-size:13px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.review-question-summary small{overflow:hidden;color:var(--muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.review-question-score{color:var(--brand);font-size:12px;font-weight:700;text-align:right}.review-question-chevron{color:var(--muted);font-size:14px;text-align:right;transition:transform .16s ease}.review-question-row[open] .review-question-chevron{transform:rotate(180deg)}.review-question-detail{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:26px;padding:0 0 18px 48px}.review-detail-label{display:block;margin-bottom:6px;color:var(--muted);font-size:10px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}.review-question-detail p{margin:0;color:var(--copy);font-size:12px;line-height:1.75;white-space:pre-wrap}.review-empty,.review-empty-score{margin:16px 0 0;color:var(--muted);font-size:12px;line-height:1.7}.review-aside{display:grid;align-content:start;gap:28px;padding-top:9px}.review-context-panel,.review-score-panel{padding-bottom:22px;border-bottom:1px solid var(--border-subtle)}.review-context-panel dl{display:grid;gap:12px;margin:22px 0 0}.review-context-panel dt{color:var(--muted);font-size:10px}.review-context-panel dd{margin:3px 0 0;color:var(--copy);font-size:12px;line-height:1.5;word-break:break-word}.review-score-list{display:grid;gap:14px;margin-top:22px}.review-score-item{display:grid;grid-template-columns:78px minmax(0,1fr) 28px;gap:8px;align-items:center;color:var(--copy);font-size:10px}.review-score-item strong{color:var(--ink);font-size:11px;font-variant-numeric:tabular-nums;text-align:right}.review-score-track{height:4px;background:#eef0ed;overflow:hidden}.review-score-track i{display:block;height:100%;background:var(--brand)}.review-aside-action{justify-self:start}@media (max-width:900px){.interview-review-page{grid-template-columns:1fr;gap:28px}.review-aside{grid-template-columns:repeat(2,minmax(0,1fr));gap:20px;padding-top:0}.review-aside-action{grid-column:1/-1}.review-question-detail{grid-template-columns:1fr}}@media (max-width:620px){.review-header,.review-next{align-items:flex-start;flex-direction:column}.review-header h1{font-size:23px}.review-primary-action{align-self:flex-start}.review-aside{grid-template-columns:1fr}.review-question-row summary{grid-template-columns:28px minmax(0,1fr) 48px 14px;gap:9px}.review-question-detail{padding-left:37px}}
/* 复盘页沿用配置页的白色工作区：先交代上下文，再把结论与证据放在同一条阅读路径上。 */
.interview-review-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 0 0 28px;
  background: var(--surface-solid, #fff);
}
.review-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 42px;
  padding: 0 0 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.review-context-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}
.review-mode-mark {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  color: var(--copy);
  font-size: 11px;
  font-weight: 700;
}
.review-mode-mark[data-mode='KNOWLEDGE_TRAINING'] { color: #3158d4; }
.review-mode-mark[data-mode='EXPERIENCE_SIMULATION'] { color: #7c3aed; }
.review-context-title {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-context-separator {
  color: var(--muted);
  font-size: 12px;
}
.review-context-status {
  color: var(--muted);
  font-size: 11px;
}
.review-back-link {
  flex: 0 0 auto;
  padding: 7px 0;
  color: var(--copy);
  font-size: 12px;
}
.review-back-link:hover { color: var(--brand); }
.review-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  padding: 20px 0 18px;
  border-bottom: 1px solid var(--border-subtle);
}
.review-progress-step {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  column-gap: 9px;
  align-items: center;
  min-width: 0;
}
.review-progress-step > span {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border: 1px solid var(--border-subtle);
  border-radius: 50%;
  color: var(--muted);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}
.review-progress-step strong {
  overflow: hidden;
  color: var(--copy);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-progress-step small {
  grid-column: 2;
  margin-top: 3px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-progress-step.is-complete > span {
  border-color: var(--ink);
  background: var(--ink);
  color: #fff;
}
.review-progress-step.is-current > span {
  border-color: var(--brand);
  background: var(--brand);
  color: #fff;
}
.review-progress-step.is-current strong { color: var(--ink); }
.review-progress-line {
  width: clamp(28px, 8vw, 112px);
  height: 1px;
  margin: 11px 16px 0;
  background: var(--border-subtle);
}
.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 246px;
  gap: 46px;
  min-height: 0;
  padding-top: 26px;
}
.review-main { min-width: 0; }
.review-focus {
  padding: 0 0 24px;
}
.review-focus > strong { font-size: 19px; }
.review-next { padding: 21px 0; }
.review-question-section { padding-top: 23px; }
.review-aside {
  display: grid;
  align-content: start;
  gap: 26px;
  padding: 0 0 0 26px;
  border-left: 1px solid var(--border-subtle);
}
.review-context-panel,
.review-score-panel { padding-bottom: 21px; }
.review-source-line {
  margin: 11px 0 0;
  color: var(--copy);
  font-size: 11px;
  line-height: 1.65;
}
.review-context-panel dl { margin-top: 18px; }
.review-score-list { margin-top: 18px; }
.review-primary-action {
  min-height: 34px;
  padding: 8px 13px;
  border-radius: 6px;
  background: #171717;
  color: #fff;
}
.review-primary-action:hover { background: var(--brand); border-color: var(--brand); }
.review-aside-action { padding-top: 0; }

@media (max-width: 900px) {
  .review-progress { grid-template-columns: 1fr; gap: 10px; }
  .review-progress-line { display: none; }
  .review-layout { grid-template-columns: 1fr; gap: 28px; }
  .review-aside { grid-template-columns: repeat(2, minmax(0, 1fr)); border-top: 1px solid var(--border-subtle); border-left: 0; padding: 24px 0 0; }
  .review-aside-action { grid-column: 1 / -1; }
}
@media (max-width: 620px) {
  .review-topbar { align-items: flex-start; flex-direction: column; gap: 8px; }
  .review-progress { padding-top: 16px; }
  .review-layout { padding-top: 20px; }
  .review-aside { grid-template-columns: 1fr; }
  .review-aside-action { grid-column: auto; }
}
</style>
