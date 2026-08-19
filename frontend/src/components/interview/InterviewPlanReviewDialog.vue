<template>
  <el-dialog
    :model-value="modelValue"
    title="整次面试复盘"
    width="780px"
    class="plan-review-dialog"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="summary" class="plan-review-dialog-content">
      <div class="plan-review-dialog-hero">
        <div>
          <span class="review-kicker">Interview Review</span>
          <h3>{{ summary.plan.jobLabel }}</h3>
          <p>{{ summary.plan.resumeLabel }} · {{ summary.completedRounds }}/{{ summary.totalRounds }} 轮已完成</p>
        </div>
        <div v-if="summary.overall" class="plan-review-dialog-score">
          <strong>{{ summary.overall.displayAverage }}</strong>
          <span>/10</span>
        </div>
      </div>

      <div v-if="summary.overall" class="plan-review-dialog-metrics">
        <div v-for="dimension in summary.overall.dimensions" :key="dimension.key" class="plan-review-dialog-metric">
          <span>{{ dimension.label }}</span>
          <strong>{{ dimension.value.toFixed(1) }}</strong>
          <div><i :style="{ width: `${dimension.value * 10}%`, background: dimension.color }" /></div>
        </div>
      </div>

      <div class="round-review-grid">
        <article
          v-for="round in summary.rounds"
          :key="round.sessionId"
          class="round-review-card"
          :class="{ completed: round.completed }"
        >
          <span>第 {{ round.order }} 轮</span>
          <strong>{{ round.personaName }}</strong>
          <small>{{ round.personaTitle }}</small>
          <div v-if="round.summary" class="round-review-score">
            <b>{{ round.summary.displayAverage }}</b><em>/10</em>
            <p>薄弱点：{{ round.summary.weakest.label }}</p>
          </div>
          <p v-else class="round-review-pending">{{ round.completed ? '评分加载中' : '待完成' }}</p>
        </article>
      </div>

      <div v-if="summary.cachedSummary" class="plan-review-ai-summary">
        <h4>整次总结</h4>
        <p>{{ summary.cachedSummary.overallSummary }}</p>
        <div v-if="summary.cachedSummary.crossStrengths.length" class="plan-review-list">
          <span>稳定优势</span>
          <ul><li v-for="item in summary.cachedSummary.crossStrengths" :key="item">{{ item }}</li></ul>
        </div>
        <div v-if="summary.cachedSummary.crossWeaknesses.length" class="plan-review-list">
          <span>共性薄弱点</span>
          <ul><li v-for="item in summary.cachedSummary.crossWeaknesses" :key="item">{{ item }}</li></ul>
        </div>
        <div v-if="summary.cachedSummary.suggestions.length" class="plan-review-list">
          <span>训练方向</span>
          <ul><li v-for="item in summary.cachedSummary.suggestions" :key="item">{{ item }}</li></ul>
        </div>
      </div>
      <div v-else class="plan-review-empty-summary">
        <el-icon><Trophy /></el-icon>
        <strong>整次总结正在准备中</strong>
        <span>完成全部面试官后，可以生成跨轮次复盘。</span>
      </div>
    </div>
    <template #footer>
      <el-button data-test="close-plan-review" @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Trophy } from '@element-plus/icons-vue'
import type { InterviewPlanReviewSummary } from '../../utils/interviewReview'

defineProps<{
  modelValue: boolean
  summary: InterviewPlanReviewSummary | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>

<style scoped>
.plan-review-dialog-content{display:grid;gap:18px}.plan-review-dialog-hero{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;padding:20px;border-radius:22px;background:linear-gradient(135deg,#101a33,#1e3a5f);color:#fff}.review-kicker{color:#6ee7b7;font-size:10px;font-weight:900;letter-spacing:.14em;text-transform:uppercase}.plan-review-dialog-hero h3{margin:7px 0 4px;font-size:22px}.plan-review-dialog-hero p{margin:0;color:#cbd5e1;font-size:13px}.plan-review-dialog-score strong{font-size:34px}.plan-review-dialog-score span{color:#cbd5e1}.plan-review-dialog-metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.plan-review-dialog-metric{display:grid;gap:6px;padding:12px;border:1px solid #e5eaf2;border-radius:14px}.plan-review-dialog-metric span{color:#64748b;font-size:11px}.plan-review-dialog-metric strong{color:#0f172a}.plan-review-dialog-metric>div{height:5px;border-radius:999px;background:#e2e8f0;overflow:hidden}.plan-review-dialog-metric i{display:block;height:100%;border-radius:inherit}.round-review-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}.round-review-card{display:grid;gap:4px;padding:14px;border:1px solid #e5eaf2;border-radius:16px;background:#f8fafc}.round-review-card.completed{border-color:#bbf7d0;background:#f0fdf4}.round-review-card>span,.round-review-card small{color:#64748b;font-size:11px}.round-review-score b{font-size:20px}.round-review-score em{font-size:11px;font-style:normal}.round-review-score p,.round-review-pending{margin:4px 0 0;color:#64748b;font-size:11px}.plan-review-ai-summary,.plan-review-empty-summary{padding:18px;border-radius:18px;background:#f8fafc}.plan-review-ai-summary h4{margin:0 0 8px}.plan-review-ai-summary>p{color:#334155;line-height:1.7}.plan-review-list{margin-top:12px}.plan-review-list span{color:#047857;font-size:12px;font-weight:900}.plan-review-list ul{margin:6px 0 0;padding-left:18px;color:#475569}.plan-review-empty-summary{display:grid;place-items:center;gap:7px;color:#64748b;text-align:center}.plan-review-empty-summary strong{color:#0f172a}@media(max-width:700px){.plan-review-dialog-metrics{grid-template-columns:repeat(2,1fr)}}
</style>
