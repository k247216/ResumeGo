<script setup lang="ts">
import AppIcon from './AppIcon.vue'
withDefaults(defineProps<{
  icon?: string
  title: string
  hint?: string
}>(), { icon: 'target' })
</script>

<template>
  <div class="empty-state">
    <span class="es-art"><AppIcon :name="icon" :size="26" /></span>
    <h3 class="es-title">{{ title }}</h3>
    <p v-if="hint" class="es-hint">{{ hint }}</p>
    <div v-if="$slots.action" class="es-action"><slot name="action" /></div>
  </div>
</template>
