<template>
  <router-view v-if="$route.meta.immersive" />
  <DesktopShell v-else>
    <router-view />
  </DesktopShell>
</template>

<script setup lang="ts">
import DesktopShell from './layouts/DesktopShell.vue'
</script>
