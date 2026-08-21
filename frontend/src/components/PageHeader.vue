<template>
  <header class="page-header">
    <div class="page-header-copy">
      <p v-if="eyebrow" class="page-eyebrow">{{ eyebrow }}</p>
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding:26px 4px 20px}
.page-header-copy{min-width:0}
.page-eyebrow{margin:0 0 6px;color:var(--brand);font-size:12px;font-weight:600;letter-spacing:.08em;line-height:1.3}
.page-title{margin:0;font-size:28px;font-weight:650;letter-spacing:-.02em;line-height:1.2;color:var(--ink)}
.page-subtitle{margin:8px 0 0;color:var(--copy);font-size:14px;line-height:1.5}
.page-header-actions{display:flex;align-items:center;gap:10px;flex:0 0 auto}
</style>
