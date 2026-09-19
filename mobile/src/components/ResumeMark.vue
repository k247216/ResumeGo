<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeMarkName } from '../data/resumeMark'

const props = withDefaults(defineProps<{ variant?: ResumeMarkName; size?: number }>(), {
  variant: 'general', size: 44,
})
const px = computed(() => `${props.size}px`)
</script>

<template>
  <!-- 简历库图标：所有变体都是「纸张 + 简历版式」（姓名条/头像/正文行），
       变体只决定强调元素——确保每一个看起来都像一份简历，而不是无关图形。 -->
  <span class="resume-mark" :class="`resume-mark-${props.variant}`" :style="{ '--resume-mark-size': px }" aria-hidden="true">
    <svg viewBox="0 0 44 44" fill="none">
      <rect class="resume-mark-paper" x="5.5" y="4.5" width="33" height="35" rx="9" />
      <path class="resume-mark-fold" d="M28 5v8.5a2 2 0 0 0 2 2h8.5" />
      <template v-if="props.variant === 'profile'">
        <circle class="resume-mark-accent" cx="17" cy="21" r="4" />
        <path class="resume-mark-accent" d="M10.5 31c.8-3.2 3-4.8 6.5-4.8s5.7 1.6 6.5 4.8M27 21h6M27 26h5M13 13.5h8" />
      </template>
      <template v-else-if="props.variant === 'engineering'">
        <path class="resume-mark-accent" d="M13 13.5h8m-8 14h18m-18 5h10m-11-14 11-9m2 18 3-2.5m3-2.5 3-2.5m-6 5-3 2.5" />
      </template>
      <template v-else-if="props.variant === 'academic'">
        <path class="resume-mark-accent" d="M11 20.5 22 16l11 4.5-11 4.5-11-4.5Zm5 2.5v4.8c3.7 2.4 7.3 2.4 11 0V23M33 20.5v6M13 13.5h7" />
        <circle class="resume-mark-accent-fill" cx="33" cy="28.5" r="1.7" />
      </template>
      <template v-else-if="props.variant === 'product'">
        <rect class="resume-mark-accent-fill" x="12" y="17" width="4.5" height="4.5" rx="1.2" />
        <rect class="resume-mark-accent-fill" x="12" y="24" width="4.5" height="4.5" rx="1.2" />
        <rect class="resume-mark-accent-fill" x="12" y="31" width="4.5" height="4.5" rx="1.2" />
        <path class="resume-mark-accent" d="M21 19.2h11M21 26.2h11M21 33.2h8M13 13.5h8" />
      </template>
      <template v-else-if="props.variant === 'research'">
        <circle class="resume-mark-accent-fill" cx="14.2" cy="19.2" r="1.8" />
        <circle class="resume-mark-accent-fill" cx="14.2" cy="26.2" r="1.8" />
        <circle class="resume-mark-accent-fill" cx="14.2" cy="33.2" r="1.8" />
        <path class="resume-mark-accent" d="M20 19.2h11M20 26.2h11M20 33.2h8M13 13.5h8" />
      </template>
      <template v-else-if="props.variant === 'creative'">
        <path class="resume-mark-accent" d="M13 13.5h18M13 19h18M13 24.5h12" />
        <path class="resume-mark-accent" d="M13 33.5c1.5-3.5 3.5-3.5 5-.5s3.5 3 5-.5 3.5-3.5 5 0" />
      </template>
      <template v-else-if="props.variant === 'timeline'">
        <path class="resume-mark-accent" d="M14 14v20M14 18h17M14 26h12M14 34h8" />
        <circle class="resume-mark-accent-fill" cx="14" cy="14" r="2.3" />
        <circle class="resume-mark-accent-fill" cx="14" cy="26" r="2.3" />
        <circle class="resume-mark-accent-fill" cx="14" cy="34" r="2.3" />
      </template>
      <template v-else>
        <path class="resume-mark-accent" d="M13 13.5h18M13 19.5h18M13 25.5h18M13 31.5h11" />
      </template>
    </svg>
  </span>
</template>
