<script setup lang="ts">
const props = defineProps<{ current: string }>()
const emit = defineEmits<{ (e: 'go', tab: string): void }>()

const tabs = [
  { key: 'schedule', label: '日程', icon: 'M8 3v3M16 3v3M3.5 8.5h17M5 5h14a1.5 1.5 0 0 1 1.5 1.5v12A1.5 1.5 0 0 1 19 20H5a1.5 1.5 0 0 1-1.5-1.5v-12A1.5 1.5 0 0 1 5 5Z' },
  { key: 'targets', label: '目标', icon: 'M12 12m-2 0a2 2 0 1 0 4 0a2 2 0 1 0-4 0M12 12m-6 0a6 6 0 1 0 12 0a6 6 0 1 0-12 0M12 12m-9.5 0a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0' },
  { key: 'resumes', label: '资料', icon: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z' },
  { key: 'me', label: '我的', icon: 'M12 12a4 4 0 1 0 0-8a4 4 0 0 0 0 8ZM4.5 20a7.5 7.5 0 0 1 15 0' },
]
</script>

<template>
  <nav class="tabbar" aria-label="主导航">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="tab"
      :class="{ on: props.current === tab.key }"
      :aria-current="props.current === tab.key ? 'page' : undefined"
      @click="emit('go', tab.key)"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path :d="tab.icon" />
      </svg>
      <span>{{ tab.label }}</span>
    </button>
  </nav>
</template>
