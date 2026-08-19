<template>
  <section class="interview-lobby-hero">
    <div class="lobby-hero-copy">
      <p class="section-kicker">{{ fromTarget ? 'Target Practice' : 'Interview Practice' }}</p>
      <h1>{{ fromTarget ? '用当前目标验证这版简历' : '把这版简历放进一次完整多轮面试里验证' }}</h1>
      <p>
        {{ fromTarget
          ? '岗位和简历版本已经由求职目标锁定。你只需选择本轮面试官与考察重点，面试记录会留在这组真实材料下。'
          : '绑定简历版本与目标岗位，选择多位面试官依次追问；过程、评分与复盘都会沉淀为下一版简历的优化依据。' }}
      </p>
      <div class="lobby-hero-actions">
        <button data-test="create-interview" class="interview-start-button" type="button" @click="emit('create')">
          新建模拟面试
          <el-icon><ArrowRight /></el-icon>
        </button>
        <button
          data-test="view-growth"
          class="lobby-ghost-button"
          type="button"
          :disabled="growthLoading"
          @click="emit('view-growth')"
        >
          <el-icon v-if="growthLoading" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Trophy /></el-icon>
          查看成长趋势
        </button>
      </div>
    </div>
    <div class="lobby-hero-panel">
      <div class="lobby-orbit">
        <span>简历</span><span>岗位</span><span>面试</span>
        <div class="zhida-brand-mark" aria-label="职达">
          <img :src="zhidaInterviewBrand" alt="职达 AI 简历求职助手" />
        </div>
      </div>
      <div class="lobby-stat-grid">
        <div><span>面试档案</span><strong>{{ recordCount }}</strong></div>
        <div><span>完成复盘</span><strong>{{ completedCount }}</strong></div>
        <div><span>待继续</span><strong>{{ inProgressCount }}</strong></div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ArrowRight, Loading, Trophy } from '@element-plus/icons-vue'
import zhidaInterviewBrand from '../../assets/zhida-interview-brand.png'

defineProps<{
  fromTarget: boolean
  recordCount: number
  completedCount: number
  inProgressCount: number
  growthLoading: boolean
}>()

const emit = defineEmits<{
  create: []
  'view-growth': []
}>()
</script>

<style scoped>
.interview-lobby-hero{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:22px;margin-bottom:18px;padding:28px;border-radius:28px;background:linear-gradient(135deg,#101a33,#123d36);color:#fff}.section-kicker{margin:0;color:#6ee7b7;font-size:11px;font-weight:900;letter-spacing:.14em;text-transform:uppercase}.lobby-hero-copy h1{max-width:760px;margin:10px 0 12px;font-size:30px;line-height:1.2}.lobby-hero-copy>p:last-of-type{max-width:740px;margin:0;color:#dbe7e5;line-height:1.75}.lobby-hero-actions{display:flex;gap:10px;margin-top:20px}.lobby-hero-actions button{display:inline-flex;align-items:center;gap:7px;min-height:40px;padding:9px 16px;border-radius:999px;font-weight:900;cursor:pointer}.interview-start-button{border:0;background:#10b981;color:#fff}.lobby-ghost-button{border:1px solid rgba(255,255,255,.25);background:rgba(255,255,255,.08);color:#fff}.lobby-ghost-button:disabled{opacity:.6}.lobby-hero-panel{align-self:stretch;padding:14px;border-radius:22px;background:#fff;color:#0f172a}.lobby-orbit{position:relative;display:grid;place-items:center;height:118px;border-radius:18px;background:#101a33}.lobby-orbit>span{position:absolute;padding:4px 8px;border-radius:999px;background:rgba(255,255,255,.1);color:#dbeafe;font-size:10px}.lobby-orbit>span:nth-child(1){top:12px;left:14px}.lobby-orbit>span:nth-child(2){top:28px;right:14px}.lobby-orbit>span:nth-child(3){bottom:12px;left:18px}.zhida-brand-mark img{width:66px;height:66px;object-fit:contain}.lobby-stat-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:10px}.lobby-stat-grid div{padding:8px;border-radius:13px;background:#f8fafc}.lobby-stat-grid span,.lobby-stat-grid strong{display:block}.lobby-stat-grid span{color:#64748b;font-size:10px}.lobby-stat-grid strong{margin-top:3px;font-size:20px}@media(max-width:900px){.interview-lobby-hero{grid-template-columns:1fr}.lobby-hero-panel{display:none}}@media(max-width:620px){.interview-lobby-hero{padding:20px}.lobby-hero-actions{align-items:stretch;flex-direction:column}.lobby-hero-actions button{justify-content:center}}
</style>
