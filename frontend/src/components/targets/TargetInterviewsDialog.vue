<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="interview-dialog-title">
      <header>
        <div>
          <small>面试记录</small>
          <h2 id="interview-dialog-title">{{ plan?.title ?? '面试详情' }}</h2>
          <p v-if="plan" class="meta">
            {{ plan.rounds.length }} 轮 · 共 {{ plan.questionCount }} 题
            <template v-if="plan.summary"> · 综合评分 {{ plan.summary.overallScore }}</template>
          </p>
        </div>
        <button type="button" aria-label="关闭" @click="$emit('close')">×</button>
      </header>

      <div v-if="loading" class="state">正在读取题目…</div>
      <div v-else-if="error" class="state error" role="alert">{{ error }}</div>
      <div v-else-if="!plan?.rounds.length" class="state">这场面试还没有可查看的记录。</div>

      <section v-for="round in rounds" v-else :key="round.info.sessionId" class="round">
        <h3>
          第 {{ round.info.roundOrder }} 轮 · {{ round.info.personaName }}
          <span class="persona-title">{{ round.info.personaTitle }}</span>
        </h3>
        <details v-for="item in round.items" :key="item.questionIndex" class="question">
          <summary>
            <span class="q-index">{{ item.questionIndex + 1 }}</span>
            <span class="q-text">{{ item.questionText }}</span>
          </summary>
          <div class="answer-body">
            <p v-if="item.answerText" class="answer">{{ item.answerText }}</p>
            <p v-else class="answer muted">未作答</p>
            <div v-if="item.evaluation?.score" class="scores">
              <span>清晰 {{ item.evaluation.score.clarity }}</span>
              <span>相关 {{ item.evaluation.score.relevance }}</span>
              <span>深度 {{ item.evaluation.score.depth }}</span>
              <span>准确 {{ item.evaluation.score.accuracy }}</span>
            </div>
          </div>
        </details>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { InterviewPlanResponse, InterviewPlanRound } from '../../types/interview'
import { getSessionHistory } from '../../api/interview'

interface RoundWithItems {
  info: InterviewPlanRound
  items: Awaited<ReturnType<typeof getSessionHistory>>['data']['items']
}

const props = withDefaults(defineProps<{
  open: boolean
  plan: InterviewPlanResponse | null
}>(), { open: false })

defineEmits<{ (event: 'close'): void }>()

const rounds = ref<RoundWithItems[]>([])
const loading = ref(false)
const error = ref('')

watch(() => [props.open, props.plan?.planId] as const, async ([open]) => {
  if (!open || !props.plan) return
  loading.value = true
  error.value = ''
  rounds.value = []
  try {
    const loaded = await Promise.all(props.plan.rounds.map(async (info) => {
      const res = await getSessionHistory(info.sessionId)
      return { info, items: res.data.items }
    }))
    rounds.value = loaded.sort((left, right) => left.info.roundOrder - right.info.roundOrder)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取面试题目失败'
  } finally {
    loading.value = false
  }
}, { immediate: true })
</script>

<style scoped>
.dialog-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.dialog{width:min(640px,100%);max-height:min(78vh,720px);overflow-y:auto;border-radius:16px;background:#fff;color:#141516;box-shadow:0 22px 60px rgba(0,0,0,.35);padding:22px;animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.dialog{animation:none}}
.dialog header{display:flex;justify-content:space-between;gap:16px;margin-bottom:14px}
.dialog h2{margin:5px 0 4px;font-size:20px}
.meta{margin:0;color:var(--muted,#74828c);font-size:13px}
.dialog header button{align-self:start;border:0;background:none;color:var(--muted,#5b6570);font-size:24px;cursor:pointer}
.state{padding:26px 4px;color:var(--muted,#74828c);font-size:14px}
.state.error{color:var(--danger,#b53c32)}
.round{border-top:1px solid var(--line-subtle,rgba(28,31,35,.07));padding:14px 2px}
.round h3{margin:0 0 10px;font-size:13px;font-weight:650;color:#344552}
.persona-title{margin-left:8px;font-weight:500;color:var(--muted,#74828c)}
.question{border:1px solid var(--line-subtle,rgba(28,31,35,.08));border-radius:10px;padding:0;margin-bottom:8px;background:#fff;transition:border-color .15s ease-out}
.question:hover{border-color:var(--brand-soft,rgba(22,139,104,.35))}
.question summary{display:flex;align-items:center;gap:10px;padding:10px 12px;cursor:pointer;list-style:none;font-weight:550;font-size:13.5px;color:#1f2d3a}
.question summary::-webkit-details-marker{display:none}
.q-index{flex:0 0 auto;display:grid;place-items:center;width:22px;height:22px;border-radius:50%;background:var(--brand-soft,rgba(22,139,104,.12));color:var(--brand,#168866);font-size:11.5px;font-weight:700}
.q-text{min-width:0;line-height:1.5}
.answer-body{padding:0 12px 12px 44px}
.answer{margin:0;padding:9px 11px;border-radius:8px;background:var(--surface-subtle,#f7f8f8);color:#4a5560;font-size:13px;line-height:1.65;white-space:pre-wrap}
.answer.muted{background:none;color:var(--muted,#98a2ad);padding-left:0}
.scores{display:flex;gap:12px;margin-top:8px;color:var(--muted,#74828c);font-size:12px}
</style>
