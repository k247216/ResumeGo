<template>
  <el-dialog :model-value="modelValue" title="成长趋势" width="800px" :close-on-click-modal="false" @update:model-value="emitClose">
    <div v-if="report" class="growth-content">
      <!-- 顶部信息 -->
      <div class="growth-header">
        <div class="growth-header-info">
          <span class="growth-header-label">当前岗位</span>
          <strong>{{ report.jobTitle }}</strong>
          <small v-if="report.companyName">{{ report.companyName }}</small>
        </div>
        <div class="growth-header-info">
          <span class="growth-header-label">当前简历</span>
          <strong>{{ resumeLabel }}</strong>
        </div>
      </div>

      <!-- 版本时间线 -->
      <div class="growth-timeline" v-if="snapshots.length > 1">
        <div class="growth-timeline-line">
          <div
            v-for="(snap, idx) in snapshots"
            :key="snap.resumeVersionId"
            class="growth-timeline-dot"
            :class="{ active: idx === snapshots.length - 1 }"
          >
            <span class="growth-timeline-dot-inner"></span>
            <span class="growth-timeline-label">{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</span>
          </div>
        </div>
      </div>

      <!-- 折线图 -->
      <div class="growth-chart" v-if="snapshots.length > 1">
        <h4 class="growth-section-title">四维能力变化趋势</h4>
        <svg :viewBox="'0 0 600 280'" class="growth-line-chart" preserveAspectRatio="xMidYMid meet">
          <line v-for="y in 5" :key="'grid-' + y" :x1="50" :y1="40 + (y - 1) * 50" :x2="580" :y2="40 + (y - 1) * 50" stroke="#e5e7eb" stroke-width="1" />
          <text v-for="y in 5" :key="'ylabel-' + y" :x="42" :y="44 + (y - 1) * 50" text-anchor="end" font-size="10" fill="#94a3b8">{{ 10 - (y - 1) * 2.5 }}</text>
          <text
            v-for="(snap, idx) in snapshots"
            :key="'xlabel-' + idx"
            :x="50 + (idx / Math.max(snapshots.length - 1, 1)) * 530"
            :y="272"
            text-anchor="middle"
            font-size="10"
            fill="#64748b"
          >{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</text>

          <template v-for="(dim, dimIdx) in dims" :key="dim.key">
            <polyline
              :points="snapshots.map((snap, idx) => {
                const x = 50 + (idx / Math.max(snapshots.length - 1, 1)) * 530
                const y = 240 - ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 200
                return x + ',' + y
              }).join(' ')"
              :stroke="dim.color"
              :stroke-width="dimIdx === 0 ? 2.5 : 2"
              fill="none"
              stroke-linejoin="round"
              stroke-linecap="round"
            />
            <circle
              v-for="(snap, idx) in snapshots"
              :key="dim.key + '-' + idx"
              :cx="50 + (idx / Math.max(snapshots.length - 1, 1)) * 530"
              :cy="240 - ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 200"
              :r="dimIdx === 0 ? 4 : 3"
              :fill="dim.color"
              stroke="white"
              stroke-width="1.5"
            />
          </template>
        </svg>

        <div class="growth-legend">
          <span v-for="dim in dims" :key="dim.key" class="growth-legend-item">
            <span class="growth-legend-dot" :style="{ background: dim.color }"></span>
            {{ dim.label }}
          </span>
        </div>
      </div>

      <!-- 变化摘要 -->
      <div class="growth-changes" v-if="snapshots.length > 1">
        <h4 class="growth-section-title">变化摘要</h4>
        <div class="growth-changes-grid">
          <div v-for="dim in dims" :key="dim.key" class="growth-change-item">
            <span class="growth-change-label">{{ dim.label }}</span>
            <span
              class="growth-change-value"
              :class="{
                'growth-change-positive': (changes[dim.key as keyof GrowthDimensions] as number) > 0,
                'growth-change-negative': (changes[dim.key as keyof GrowthDimensions] as number) < 0,
              }"
            >
              {{ (changes[dim.key as keyof GrowthDimensions] as number) > 0 ? '+' : '' }}{{ (changes[dim.key as keyof GrowthDimensions] as number).toFixed(1) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 版本卡片 -->
      <div class="growth-snapshots">
        <h4 class="growth-section-title">版本详情</h4>
        <div v-for="snap in snapshots" :key="snap.resumeVersionId" class="growth-snapshot-card">
          <div class="growth-snapshot-header">
            <strong>{{ snap.versionLabel.split('·')[0]?.trim() || snap.versionLabel }}</strong>
            <span class="growth-snapshot-badge">代表面试：Plan #{{ snap.representativePlanId }}</span>
            <span class="growth-snapshot-badge">该版本共面试 {{ snap.interviewCount }} 次</span>
          </div>
          <div class="growth-snapshot-scores">
            <div v-for="dim in dims" :key="dim.key" class="growth-snapshot-score">
              <span class="growth-snapshot-score-label">{{ dim.label }}</span>
              <span class="growth-snapshot-score-bar-wrapper">
                <span
                  class="growth-snapshot-score-bar"
                  :style="{ width: ((snap.dimensions[dim.key as keyof GrowthDimensions] as number) / 10) * 100 + '%', background: dim.color }"
                ></span>
              </span>
              <span class="growth-snapshot-score-value">{{ (snap.dimensions[dim.key as keyof GrowthDimensions] as number).toFixed(1) }}</span>
            </div>
          </div>
          <div v-if="snap.summary" class="growth-snapshot-summary">
            <span>总结：</span>{{ snap.summary }}
          </div>
        </div>
      </div>

      <!-- 单次提示 -->
      <div v-if="snapshots.length <= 1" class="growth-single-hint">
        <el-icon><Trophy /></el-icon>
        <p>仅有 1 个版本数据</p>
        <small>完成更多面试后可见成长趋势对比</small>
      </div>
    </div>
    <div v-else-if="loading" class="growth-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载成长数据，请稍候...</span>
    </div>
    <template #footer>
      <el-button @click="emitClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Trophy, Loading } from '@element-plus/icons-vue'
import type { GrowthDimensions, GrowthReport } from '../../types/interview'

export interface GrowthDimensionDef {
  key: string
  label: string
  color: string
}

const props = defineProps<{
  modelValue: boolean
  report: GrowthReport | null
  loading: boolean
  snapshots: GrowthReport['snapshots']
  changes: GrowthDimensions
  resumeLabel: string
  dims: GrowthDimensionDef[]
}>()

const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

function emitClose() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.growth-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.growth-header {
  display: flex;
  gap: 32px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line, #e5e7eb);
}

.growth-header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.growth-header-info strong {
  font-size: 16px;
  color: var(--ink, #101a33);
}

.growth-header-info small {
  font-size: 12px;
  color: var(--muted, #64748b);
}

.growth-header-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted, #94a3b8);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.growth-timeline {
  padding: 8px 0;
}

.growth-timeline-line {
  display: flex;
  align-items: flex-start;
  position: relative;
  padding-top: 10px;
}

.growth-timeline-line::before {
  content: '';
  position: absolute;
  top: 16px;
  left: 8px;
  right: 8px;
  height: 2px;
  background: var(--line, #e5e7eb);
}

.growth-timeline-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  position: relative;
  z-index: 1;
}

.growth-timeline-dot-inner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--surface, #fff);
  border: 2px solid var(--line, #cbd5e1);
}

.growth-timeline-dot.active .growth-timeline-dot-inner {
  background: var(--brand, #10b981);
  border-color: var(--brand, #10b981);
}

.growth-timeline-label {
  font-size: 12px;
  color: var(--muted, #64748b);
  white-space: nowrap;
}

.growth-timeline-dot.active .growth-timeline-label {
  color: var(--ink, #101a33);
  font-weight: 600;
}

.growth-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink, #101a33);
  margin: 0 0 12px;
}

.growth-line-chart {
  width: 100%;
  height: auto;
}

.growth-legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.growth-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted, #64748b);
}

.growth-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.growth-changes-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.growth-change-item {
  padding: 10px 12px;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.growth-change-label {
  font-size: 12px;
  color: var(--muted, #64748b);
}

.growth-change-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--muted, #94a3b8);
}

.growth-change-positive {
  color: var(--brand, #10b981);
}

.growth-change-negative {
  color: var(--danger, #ef4444);
}

.growth-snapshots {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.growth-snapshot-card {
  padding: 12px 16px;
  background: var(--surface, #f8fafc);
  border: 1px solid var(--line, #e5e7eb);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.growth-snapshot-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.growth-snapshot-header strong {
  font-size: 14px;
  color: var(--ink, #101a33);
}

.growth-snapshot-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: var(--brand-soft, #ecfdf5);
  color: var(--brand, #059669);
  border-radius: 999px;
}

.growth-snapshot-scores {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.growth-snapshot-score {
  display: flex;
  align-items: center;
  gap: 10px;
}

.growth-snapshot-score-label {
  font-size: 12px;
  color: var(--muted, #64748b);
  width: 64px;
  flex-shrink: 0;
}

.growth-snapshot-score-bar-wrapper {
  flex: 1;
  height: 8px;
  background: var(--surface, #e5e7eb);
  border-radius: 4px;
  overflow: hidden;
}

.growth-snapshot-score-bar {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.growth-snapshot-score-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink, #101a33);
  width: 32px;
  text-align: right;
  flex-shrink: 0;
}

.growth-snapshot-summary {
  font-size: 12px;
  color: var(--muted, #64748b);
  line-height: 1.5;
  padding-top: 4px;
  border-top: 1px solid var(--line, #e5e7eb);
}

.growth-snapshot-summary span {
  font-weight: 600;
  color: var(--ink, #101a33);
}

.growth-single-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: var(--muted, #94a3b8);
}

.growth-single-hint .el-icon {
  font-size: 32px;
  color: var(--muted, #cbd5e1);
}

.growth-single-hint p {
  font-size: 14px;
  color: var(--muted, #64748b);
  margin: 0;
}

.growth-single-hint small {
  font-size: 12px;
}

.growth-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px;
  color: var(--muted, #94a3b8);
  font-size: 14px;
}

.growth-loading .el-icon {
  font-size: 28px;
}

@media (max-width: 640px) {
  .growth-changes-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .growth-header {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
