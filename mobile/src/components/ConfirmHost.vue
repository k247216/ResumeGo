<script setup lang="ts">
import { onScopeDispose, watch } from 'vue'
import { confirmState, confirmResult } from '../data/confirm'
import { registerOverlay } from '../data/overlays'
import AppIcon from './AppIcon.vue'

let unregisterOverlay: (() => void) | null = null
watch(() => confirmState.value.open, (v) => {
  unregisterOverlay?.()
  unregisterOverlay = v ? registerOverlay(() => confirmResult(false)) : null
})
onScopeDispose(() => unregisterOverlay?.())
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm" :duration="{ enter: 260, leave: 240 }">
      <div v-if="confirmState.open" class="confirm-wrap">
        <div class="sheet-backdrop" @click="confirmResult(false)" />
        <section class="confirm-card" role="alertdialog" aria-modal="true" :aria-label="confirmState.title">
          <span class="confirm-icon" :class="{ danger: confirmState.danger }">
            <AppIcon :name="confirmState.danger ? 'trash' : 'check'" :size="20" />
          </span>
          <h2>{{ confirmState.title }}</h2>
          <p v-if="confirmState.message">{{ confirmState.message }}</p>
          <div class="confirm-actions">
            <button class="btn-ghost" @click="confirmResult(false)">取消</button>
            <button
              class="btn-primary"
              :class="{ 'btn-danger-solid': confirmState.danger }"
              @click="confirmResult(true)"
            >{{ confirmState.confirmLabel }}</button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
