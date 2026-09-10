<script setup lang="ts">
import { toastMessage } from '../data/toast'
</script>

<template>
  <Transition name="toast">
    <div v-if="toastMessage" class="toast">{{ toastMessage }}</div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(var(--tabbar-h) + 76px + var(--safe-bottom));
  z-index: 60;
  max-width: 82%;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(23, 24, 26, .92);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, .25);
  pointer-events: none;
}
[data-theme='dark'] .toast { background: rgba(241, 241, 238, .95); color: #171717; }
.toast-enter-active, .toast-leave-active { transition: opacity .2s ease-out, transform .2s ease-out; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
</style>
