<template>
  <div class="desktop-shell">
    <aside class="desktop-rail">
      <RouterLink class="desktop-brand" :to="{ name: 'workbench' }" aria-label="ResumeGo 工作台">
        <span>R</span>
        <strong>ResumeGo</strong>
      </RouterLink>

      <nav class="desktop-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.routeName"
          :to="{ name: item.routeName }"
          class="desktop-nav-item"
        >
          <span class="desktop-nav-mark">{{ item.mark }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="desktop-local-status">
        <span class="status-dot"></span>
        <span>数据保存在此设备</span>
      </div>
    </aside>

    <section class="desktop-surface">
      <header class="desktop-titlebar">
        <div>
          <strong>{{ currentTitle }}</strong>
          <small>本地求职工作台</small>
        </div>
        <span class="desktop-mode">本地模式</span>
      </header>
      <main class="desktop-content">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const navItems = [
  { label: '工作台', mark: '⌂', routeName: 'workbench' },
  { label: '求职目标', mark: '◎', routeName: 'targets' },
  { label: '简历', mark: '▤', routeName: 'resumes' },
  { label: '能力证据', mark: '◇', routeName: 'evidences' },
  { label: '设置', mark: '⚙', routeName: 'settings' },
]

const currentTitle = computed(() => (
  navItems.find((item) => item.routeName === route.name)?.label ?? 'ResumeGo'
))
</script>

<style scoped>
.desktop-shell{display:grid;grid-template-columns:196px minmax(0,1fr);min-height:100vh;background:#f3f5f6;color:#182335}.desktop-rail{display:flex;flex-direction:column;padding:20px 14px;background:#172238;color:#dce5ed}.desktop-brand{display:flex;align-items:center;gap:10px;padding:4px 8px 22px;color:#fff;text-decoration:none}.desktop-brand>span{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:#2ab183;color:#0c2c23;font-weight:900}.desktop-brand strong{font-size:15px}.desktop-nav{display:grid;gap:6px}.desktop-nav-item{display:flex;align-items:center;gap:11px;padding:11px 12px;border-radius:10px;color:#bdc8d3;text-decoration:none;font-size:13px}.desktop-nav-item:hover{background:#223049;color:#fff}.desktop-nav-item.router-link-active{background:#168866;color:#fff}.desktop-nav-mark{display:grid;place-items:center;width:22px;font-size:15px}.desktop-local-status{display:flex;align-items:center;gap:8px;margin-top:auto;padding:14px 8px 4px;color:#aebbc8;font-size:11px}.status-dot{width:7px;height:7px;border-radius:50%;background:#51c697;box-shadow:0 0 0 3px rgba(81,198,151,.14)}.desktop-surface{min-width:0}.desktop-titlebar{height:68px;display:flex;align-items:center;justify-content:space-between;padding:0 28px;border-bottom:1px solid #dfe4e8;background:rgba(255,255,255,.9)}.desktop-titlebar div{display:grid;gap:2px}.desktop-titlebar strong{font-size:15px}.desktop-titlebar small{color:#84909d}.desktop-mode{padding:5px 9px;border-radius:999px;background:#e5f7f0;color:#17795d;font-size:11px}.desktop-content{min-height:calc(100vh - 68px)}
@media(max-width:760px){.desktop-shell{grid-template-columns:72px minmax(0,1fr)}.desktop-brand{justify-content:center}.desktop-brand strong,.desktop-nav-item>span:last-child,.desktop-local-status>span:last-child{display:none}.desktop-nav-item{justify-content:center}.desktop-titlebar{padding:0 16px}}
</style>
