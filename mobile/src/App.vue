<script setup lang="ts">
import { computed, onScopeDispose } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import TabBar from './components/TabBar.vue'
import Toast from './components/Toast.vue'
import ConfirmHost from './components/ConfirmHost.vue'
import { closeTopOverlay } from './data/overlays'
import { toast } from './data/toast'

const route = useRoute()
const router = useRouter()

const currentTab = computed(() => (route.meta.tab as string) ?? '')
const showTabBar = computed(() => route.name !== 'resume-detail')

function go(tab: string) {
  router.push({ name: tab })
}

let lastExitAt = 0
let removeBackListener: (() => void) | null = null
if (Capacitor.isNativePlatform()) {
  void import('@capacitor/app').then(({ App }) => {
    return App.addListener('backButton', () => {
      if (closeTopOverlay()) return
      if (typeof history.state?.back === 'string') { router.back(); return }
      // 已经在根页面：单按退出太容易误触，要求两下。
      if (Date.now() - lastExitAt < 2500) { void App.exitApp(); return }
      lastExitAt = Date.now()
      toast('再按一次退出职达')
    })
  }).then((handle) => { removeBackListener = () => handle.remove() })
}
onScopeDispose(() => removeBackListener?.())
</script>

<template>
  <div class="app-shell">
    <main class="app-main">
      <router-view />
    </main>
    <TabBar v-if="showTabBar" :current="currentTab" @go="go" />
    <Toast />
    <ConfirmHost />
  </div>
</template>
