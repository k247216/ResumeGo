<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeMarkName } from '../data/resumeMark'

const props = withDefaults(defineProps<{ variant?: ResumeMarkName; size?: number }>(), {
  variant: 'general', size: 44,
})
const px = computed(() => `${props.size}px`)
</script>

<template>
  <span class="resume-mark" :class="`resume-mark-${props.variant}`" :style="{ '--resume-mark-size': px }" aria-hidden="true">
    <svg viewBox="0 0 44 44" fill="none">
      <rect class="resume-mark-paper" x="5.5" y="4.5" width="33" height="35" rx="9" />
      <path class="resume-mark-fold" d="M28 5v8.5a2 2 0 0 0 2 2h8.5" />
      <template v-if="props.variant === 'profile'">
        <circle class="resume-mark-accent" cx="17" cy="22" r="4" />
        <path class="resume-mark-accent" d="M10.5 32c.8-3.2 3-4.8 6.5-4.8s5.7 1.6 6.5 4.8M26 22h7M26 27h5" />
      </template>
      <template v-else-if="props.variant === 'engineering'">
        <path class="resume-mark-accent" d="m17 20-4 3 4 3m10-6 4 3-4 3m-3-10-4 14" />
      </template>
      <template v-else-if="props.variant === 'academic'">
        <path class="resume-mark-accent" d="m11 21 11-5 11 5-11 5-11-5Zm5 2.2v5.1c3.7 2.5 7.3 2.5 11 0v-5.1M33 21v6" />
        <circle class="resume-mark-accent-fill" cx="33" cy="29" r="1.7" />
      </template>
      <template v-else-if="props.variant === 'product'">
        <rect class="resume-mark-accent-fill" x="11" y="19" width="7" height="10" rx="2" />
        <rect class="resume-mark-accent-fill" x="20" y="15" width="7" height="14" rx="2" />
        <rect class="resume-mark-accent-fill" x="29" y="11" width="4" height="18" rx="2" />
      </template>
      <template v-else-if="props.variant === 'research'">
        <path class="resume-mark-accent" d="M11 30h22M14 27v-6m7 6V15m7 12v-9" />
        <circle class="resume-mark-accent-fill" cx="14" cy="18" r="2" />
        <circle class="resume-mark-accent-fill" cx="21" cy="12" r="2" />
        <circle class="resume-mark-accent-fill" cx="28" cy="20" r="2" />
        <path class="resume-mark-accent" d="m14 18 7-6 7 8" />
      </template>
      <template v-else-if="props.variant === 'creative'">
        <path class="resume-mark-accent" d="m22 12 2.6 6.2 6.4.5-4.9 4.2 1.5 6.3-5.6-3.4-5.6 3.4 1.5-6.3-4.9-4.2 6.4-.5L22 12Z" />
      </template>
      <template v-else-if="props.variant === 'timeline'">
        <path class="resume-mark-accent" d="M14 13v19M14 17h18M14 25h13M14 33h8" />
        <circle class="resume-mark-accent-fill" cx="14" cy="13" r="2.3" />
        <circle class="resume-mark-accent-fill" cx="14" cy="25" r="2.3" />
        <circle class="resume-mark-accent-fill" cx="14" cy="33" r="2.3" />
      </template>
      <template v-else>
        <path class="resume-mark-accent" d="M22 11v22M13 20h18M16 14l12 16M28 14 16 30" />
        <circle class="resume-mark-accent-fill" cx="22" cy="22" r="3.2" />
      </template>
    </svg>
  </span>
</template>
