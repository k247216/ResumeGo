<template>
  <article
    v-if="visible"
    class="company-profile-signal"
    :class="[`variant-${variant}`]"
  >
    <div class="signal-head">
      <span>{{ label }}</span>
      <em>{{ confidenceText }}</em>
    </div>
    <strong>{{ companyName }} 偏好：{{ primaryTagsText }}</strong>
    <p v-if="writingStyle && variant !== 'inline'">{{ writingStyle }}</p>
    <div v-if="secondaryTags.length && variant !== 'inline'" class="signal-tags">
      <i v-for="tag in secondaryTags" :key="tag">{{ tag }}</i>
    </div>
    <small v-if="variant === 'full'">{{ sourceText }} · 仅作表达建议参考，不参与评分、排序或录用判断</small>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CompanyProfile } from '../types/job'

const props = withDefaults(defineProps<{
  profile: CompanyProfile | null
  variant?: 'compact' | 'full' | 'inline'
  label?: string
}>(), {
  variant: 'compact',
  label: 'Company Signal',
})

const primaryTags = computed(() => [
  ...(props.profile?.preferenceTags ?? []),
  ...(props.profile?.interviewFocus ?? []),
].filter(Boolean).slice(0, 4))

const secondaryTags = computed(() => [
  ...(props.profile?.resumeAdviceRules ?? []),
].filter(Boolean).slice(0, 3))

const visible = computed(() => Boolean(props.profile?.companyName && primaryTags.value.length))
const companyName = computed(() => props.profile?.companyName ?? '目标公司')
const writingStyle = computed(() => props.profile?.writingStyle ?? '')
const primaryTagsText = computed(() => primaryTags.value.join(' · '))
const confidenceText = computed(() => {
  const confidence = props.profile?.confidenceLevel
  if (confidence === 'high') return '高置信'
  if (confidence === 'medium_high') return '较高置信'
  if (confidence === 'medium') return '中等置信'
  if (confidence === 'low') return '低置信'
  return '表达参考'
})
const sourceText = computed(() => props.profile?.sourceNote || '公司偏好资料')
</script>

<style scoped>
.company-profile-signal {
  min-width: 0;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 16px;
  background:
    radial-gradient(circle at 0 0, rgba(37, 99, 235, 0.08), transparent 40%),
    linear-gradient(180deg, var(--surface-solid, #ffffff), var(--surface, #f8fafc));
  padding: 11px 12px;
}

.signal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 5px;
}

.signal-head span,
.signal-head em {
  color: #2563eb;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
}

.signal-head em {
  color: var(--muted, #64748b);
  font-weight: 800;
}

.company-profile-signal strong {
  display: block;
  overflow: hidden;
  color: var(--ink, #102033);
  font-size: 12px;
  font-weight: 900;
  line-height: 1.5;
  text-overflow: ellipsis;
}

.company-profile-signal p {
  margin: 5px 0 0;
  color: var(--muted, #64748b);
  font-size: 12px;
  line-height: 1.55;
}

.signal-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 8px;
}

.signal-tags i {
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
  padding: 4px 7px;
}

.company-profile-signal small {
  display: block;
  margin-top: 8px;
  color: var(--muted, #94a3b8);
  font-size: 11px;
  line-height: 1.5;
}

.variant-inline {
  display: flex;
  align-items: center;
  gap: 9px;
  border-radius: 12px;
  padding: 7px 10px;
}

.variant-inline .signal-head {
  flex: 0 0 auto;
  margin: 0;
}

.variant-inline .signal-head em {
  display: none;
}

.variant-inline strong {
  white-space: nowrap;
}

.variant-full {
  border-radius: 20px;
  padding: 15px;
}

.variant-full strong {
  font-size: 14px;
}
</style>
