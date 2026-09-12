<script setup lang="ts">
import { runToastAction, toastAction, toastMessage } from '../data/toast'
</script>

<template>
  <Transition name="toast">
    <div v-if="toastMessage" class="toast" :class="{ actionable: toastAction }">
      <span class="toast-text">{{ toastMessage }}</span>
      <button v-if="toastAction" type="button" class="toast-btn" @click="runToastAction">{{ toastAction.label }}</button>
    </div>
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
.toast.actionable {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 92%;
  padding: 10px 12px 10px 20px;
  text-align: left;
}
.toast-text { min-width: 0; }
/* 整条 toast 不接收事件，只有操作按钮可点，否则提示会出现的那一刻就挡住了底下的列表。 */
.toast-btn {
  flex: 0 0 auto;
  pointer-events: auto;
  padding: 6px 12px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, .16);
  color: inherit;
  font: inherit;
  font-weight: 700;
}
.toast-btn:active { background: rgba(255, 255, 255, .28); }
[data-theme='dark'] .toast { background: rgba(241, 241, 238, .95); color: #171717; }
[data-theme='dark'] .toast-btn { background: rgba(23, 24, 26, .1); }
[data-theme='dark'] .toast-btn:active { background: rgba(23, 24, 26, .2); }
.toast-enter-active, .toast-leave-active { transition: opacity .2s ease-out, transform .2s ease-out; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
</style>
