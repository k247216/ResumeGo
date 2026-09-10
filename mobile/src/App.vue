<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TabBar from './components/TabBar.vue'
import Toast from './components/Toast.vue'
import ConfirmHost from './components/ConfirmHost.vue'
import { listSchedules } from './data/store'

const route = useRoute()
const router = useRouter()

const currentTab = computed(() => (route.meta.tab as string) ?? '')
const showTabBar = computed(() => route.name !== 'resume-detail')
const todayCount = computed(() => {
  const today = new Date().toDateString()
  return listSchedules().filter((e) => new Date(e.startTime).toDateString() === today).length
})

function go(tab: string) {
  router.push({ name: tab })
}
</script>

<template>
  <div class="app-shell">
    <main class="app-main">
      <router-view />
    </main>
    <TabBar v-if="showTabBar" :current="currentTab" :today-count="todayCount" @go="go" />
    <Toast />
    <ConfirmHost />
  </div>
</template>
