<template>
  <section v-if="plan" class="result-workspace" data-test="interview-result-workspace">
    <header class="result-head">
      <p class="result-kicker">训练结果</p>
      <h2>{{ modeLabel }} · {{ plan.title }}</h2>
      <p class="result-context">{{ snapshotLine }}</p>
    </header>

    <section v-if="plan.summary" class="result-summary" data-test="result-summary">
      <h3>整次总结</h3>
      <p class="summary-text">{{ plan.summary.overallSummary }}</p>
      <div class="summary-cols">
        <div>
          <h4>做得好</h4>
          <ul><li v-for="item in plan.summary.crossStrengths" :key="item">{{ item }}</li></ul>
        </div>
        <div>
          <h4>待改善</h4>
          <ul><li v-for="item in plan.summary.crossWeaknesses" :key="item">{{ item }}</li></ul>
        </div>
        <div>
          <h4>下一步建议</h4>
          <ul><li v-for="item in plan.summary.suggestions" :key="item">{{ item }}</li></ul>
        </div>
      </div>
    </section>

    <section class="result-rounds" data-test="result-rounds">
      <h3>轮次</h3>
      <div v-for="round in plan.rounds" :key="round.sessionId" class="round-row" data-test="result-round">
        <strong>第 {{ round.roundOrder }} 轮 · {{ round.personaName }}</strong>
        <span>{{ round.status }}</span>
        <em>{{ round.currentQuestionIndex }}/{{ round.totalQuestions }}</em>
      </div>
      <p v-if="!plan.rounds.length" class="result-empty">本次训练暂无轮次记录。</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { InterviewPlanResponse } from '../../types/interview'

const props = defineProps<{ plan: InterviewPlanResponse | null }>()

const MODE_LABELS: Record<string, string> = {
  ROLE_BASED: '岗位模拟',
  KNOWLEDGE_TRAINING: '知识训练',
  EXPERIENCE_SIMULATION: '面经模拟',
}

const modeLabel = computed(() => {
  const mode = props.plan?.mode ?? 'ROLE_BASED'
  return MODE_LABELS[mode] ?? mode
})

/** 历史展示使用开始快照中的名称与版本号，不用当前数据覆盖 */
const snapshotLine = computed(() => {
  const snapshot = props.plan?.startContextSnapshot
  if (!snapshot || Object.keys(snapshot).length === 0) return ''
  const parts: string[] = []
  if (snapshot.jobProjectName) parts.push(String(snapshot.jobProjectName))
  if (snapshot.resumeTitle && snapshot.resumeVersionNo != null) {
    parts.push(`${snapshot.resumeTitle} · V${snapshot.resumeVersionNo}`)
  }
  if (snapshot.questionSetTitle) parts.push(`题集：${snapshot.questionSetTitle}`)
  if (snapshot.knowledgeDocumentTitles && Array.isArray(snapshot.knowledgeDocumentTitles) && snapshot.knowledgeDocumentTitles.length) {
    parts.push(`资料：${snapshot.knowledgeDocumentTitles.join('、')}`)
  }
  return parts.join(' · ')
})
</script>

<style scoped>
.result-workspace{display:grid;gap:16px}
.result-kicker{margin:0;color:var(--muted);font-size:11px;font-weight:650;letter-spacing:.06em}
.result-head h2{margin:6px 0 6px;font-size:18px;color:var(--ink)}
.result-context{margin:0;color:var(--muted);font-size:12.5px}
.result-summary{border-top:1px solid var(--border-subtle);padding-top:14px;display:grid;gap:10px}
.result-summary h3,.result-rounds h3{margin:0;font-size:13px;color:var(--ink)}
.summary-text{margin:0;color:var(--copy);font-size:13px;line-height:1.7}
.summary-cols{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
.summary-cols h4{margin:0 0 6px;font-size:11px;color:var(--muted)}
.summary-cols ul{margin:0;padding-left:16px;color:var(--copy);font-size:12px;line-height:1.7}
.result-rounds{border-top:1px solid var(--border-subtle);padding-top:14px;display:grid;gap:8px}
.round-row{display:flex;gap:12px;align-items:baseline;font-size:12.5px;color:var(--copy)}
.round-row em{font-style:normal;color:var(--muted);font-variant-numeric:tabular-nums}
.result-empty{margin:0;color:var(--muted);font-size:12px}
</style>
