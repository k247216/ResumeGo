<template>
  <span
    class="company-avatar-mark"
    :class="[`size-${size}`, `source-${brand.source}`]"
    :style="{ background: brand.gradient }"
    :title="brand.name"
  >
    <img
      v-if="brand.logoUrl && !imageFailed"
      :src="brand.logoUrl"
      :alt="`${brand.name} 标识`"
      @error="imageFailed = true"
    >
    <span v-else>{{ brand.initials }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { JobDescription } from '../types/job'
import { getCompanyBrand } from '../utils/companyLogo'

const props = withDefaults(defineProps<{
  job?: JobDescription | null
  size?: 'toolbar' | 'sm' | 'md' | 'lg'
}>(), {
  job: null,
  size: 'md',
})

const imageFailed = ref(false)
const brand = computed(() => getCompanyBrand(props.job))

watch(() => brand.value.logoUrl, () => {
  imageFailed.value = false
})
</script>

<style scoped>
.company-avatar-mark {
  position: relative;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  color: #fff;
  font-weight: 900;
  letter-spacing: -0.03em;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.26),
    0 10px 24px rgba(15, 23, 42, 0.08);
}

.company-avatar-mark.size-toolbar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  font-size: 11px;
}

.company-avatar-mark.size-sm {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  font-size: 12px;
}

.company-avatar-mark.size-md {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  font-size: 14px;
}

.company-avatar-mark.size-lg {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  font-size: 18px;
}

.company-avatar-mark img {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  background: #fff;
  object-fit: contain;
  padding: 5px;
}

.company-avatar-mark.source-local-brand img {
  background: transparent;
  object-fit: cover;
  padding: 0;
}

.company-avatar-mark span {
  position: relative;
  z-index: 0;
}
</style>
