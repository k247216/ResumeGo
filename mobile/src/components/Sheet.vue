<script setup lang="ts">
import AppIcon from './AppIcon.vue'
defineProps<{ title: string }>()
const emit = defineEmits<{ (e: 'close'): void }>()
</script>

<template>
  <Teleport to="body">
    <div class="sheet-backdrop" @click="emit('close')" />
    <section class="sheet" role="dialog" aria-modal="true" :aria-label="title">
      <div class="sheet-grab" />
      <div class="sheet-hd">
        <h2>{{ title }}</h2>
        <button class="sheet-x" aria-label="关闭" @click="emit('close')"><AppIcon name="close" :size="18" /></button>
      </div>
      <slot />
    </section>
  </Teleport>
</template>
