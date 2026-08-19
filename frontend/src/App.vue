<template>
  <router-view v-if="$route.meta.immersive" />
  <DesktopShell v-else>
    <router-view />
  </DesktopShell>
  <AiSetupGuide />
</template>

<script setup lang="ts">
import DesktopShell from './layouts/DesktopShell.vue'
import AiSetupGuide from './components/ai/AiSetupGuide.vue'
</script>
