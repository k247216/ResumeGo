<template>
  <div class="desktop-shell" :class="{ 'is-dark': darkMode, 'shell-workspace': route.name === 'workbench', 'shell-fill': Boolean(route.meta.fill) }" :data-theme="darkMode ? 'dark' : 'light'">
    <aside class="desktop-rail" aria-label="桌面工具栏">
      <nav class="desktop-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.routeName"
          :to="{ name: item.routeName }"
          class="desktop-nav-item"
          :aria-label="item.label"
          :title="item.label"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span class="nav-tooltip">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="desktop-rail-bottom">
        <RouterLink :to="{ name: 'settings' }" class="desktop-nav-item" aria-label="设置" title="设置">
          <el-icon :size="20"><Setting /></el-icon>
          <span class="nav-tooltip">设置</span>
        </RouterLink>
        <button
          type="button"
          class="desktop-nav-item"
          :aria-label="darkMode ? '切换日间模式' : '切换夜间模式'"
          :title="darkMode ? '切换日间模式' : '切换夜间模式'"
          @click="darkMode = !darkMode"
        >
          <el-icon :size="20"><component :is="darkMode ? Sunny : Moon" /></el-icon>
        </button>
        <button type="button" class="desktop-user" aria-label="本地用户状态" title="数据仅保存在此设备">
          <el-icon :size="18"><UserFilled /></el-icon>
          <span class="local-dot" aria-hidden="true"></span>
        </button>
      </div>
    </aside>

    <main class="desktop-content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Aim, Calendar, ChatDotRound, Collection, Document, House, Moon, Setting, Sunny, UserFilled } from '@element-plus/icons-vue'

const route = useRoute()
const themeStorageKey = 'resumego:theme'
const darkMode = ref(localStorage.getItem(themeStorageKey) === 'dark')
watch(
  darkMode,
  (dark) => {
    localStorage.setItem(themeStorageKey, dark ? 'dark' : 'light')
    document.body.dataset.theme = dark ? 'dark' : 'light'
  },
  { immediate: true },
)
const navItems = [
  { label: '工作台', icon: House, routeName: 'workbench' },
  { label: '求职目标', icon: Aim, routeName: 'targets' },
  { label: '简历', icon: Document, routeName: 'resumes' },
  { label: '知识库', icon: Collection, routeName: 'knowledge' },
  { label: '模拟面试', icon: ChatDotRound, routeName: 'interview' },
  { label: '日程', icon: Calendar, routeName: 'schedule' },
]
</script>

<style scoped>
.desktop-shell{--canvas:#F5F5F2;--surface:rgba(255,255,255,.78);--surface-solid:#fff;--line:rgba(28,31,35,.11);--ink:#171717;--copy:#6E6E6A;--muted:#989893;--action-bg:#111212;--action-fg:#fff;--brand:#168B68;--brand-soft:rgba(22,139,104,.09);--danger:#b53c32;--danger-soft:rgba(180,62,53,.09);display:grid;grid-template-columns:56px minmax(0,1fr);gap:34px;min-height:100vh;padding:16px 24px 16px 18px;background:var(--canvas);color:var(--ink);color-scheme:light;transition:background .25s ease,color .25s ease}.desktop-shell.is-dark{--canvas:#111212;--surface:#1a1b1d;--surface-solid:#1a1b1d;--line:rgba(255,255,255,.12);--ink:#F2F2EF;--copy:#A3A39E;--muted:#73736F;--action-bg:#F1F1EE;--action-fg:#171717;--brand:#47B58E;--brand-soft:rgba(71,181,142,.11);--danger:#e06c60;--danger-soft:rgba(224,108,96,.14);color-scheme:dark}.desktop-rail{position:sticky;top:16px;display:flex;align-self:start;flex-direction:column;box-sizing:border-box;width:56px;height:calc(100vh - 32px);min-height:520px;padding:40px 8px 12px;border:1px solid rgba(255,255,255,.08);border-radius:22px;background:#0A0A0B;color:#fff;box-shadow:0 8px 24px rgba(0,0,0,.10)}.desktop-nav,.desktop-rail-bottom{display:grid;justify-items:center;gap:8px}.desktop-rail-bottom{margin-top:auto;gap:6px;padding-top:10px;border-top:1px solid rgba(255,255,255,.10)}.desktop-nav-item{position:relative;display:grid;width:40px;height:40px;place-items:center;border:0;border-radius:11px;background:transparent;color:#f3f4f4;text-decoration:none;cursor:pointer;transition:background .18s ease,color .18s ease}.desktop-nav-item:hover{background:#222324}.desktop-nav-item.router-link-active::before{content:'';position:absolute;inset:2px;border-radius:11px;background:#fff}.desktop-nav-item.router-link-active .el-icon{position:relative;z-index:1;color:#050606}.nav-tooltip{position:absolute;z-index:10;left:44px;display:none;padding:7px 10px;border-radius:8px;background:#080909;color:#fff;font-size:12px;font-weight:650;white-space:nowrap;box-shadow:0 8px 22px rgba(0,0,0,.2)}.desktop-nav-item:hover .nav-tooltip,.desktop-nav-item:focus-visible .nav-tooltip{display:block}.desktop-user{position:relative;display:grid;width:36px;height:36px;place-items:center;border:1px solid rgba(255,255,255,.22);border-radius:50%;background:#f2eee7;color:#171819;cursor:default}.local-dot{position:absolute;right:0;bottom:2px;width:9px;height:9px;border:2px solid #0A0A0B;border-radius:50%;background:#25b96c}.desktop-content{justify-self:center;width:100%;max-width:1224px;min-width:0;min-height:calc(100vh - 32px)}
/* 首页工作台：全高、禁文档滚动、left-anchored；Dock→应用 32px gap，应用 max-width 1270 不居中 */
.desktop-shell.shell-workspace{height:100vh;overflow:hidden;gap:32px}
.desktop-shell.shell-workspace .desktop-content{justify-self:stretch;max-width:1270px;box-sizing:border-box;height:100%;min-height:0;padding:0 8px 0 0}
/* 全高页面（meta.fill：目标/简历/日程/设置）：full-bleed、内部面板滚动，不再居中窄容器 */
.desktop-shell.shell-fill{height:100vh;overflow:hidden;gap:32px}
.desktop-shell.shell-fill .desktop-content{justify-self:stretch;max-width:none;box-sizing:border-box;height:100%;min-height:0;padding:0 8px 0 0}
@media(max-width:900px){.desktop-shell{padding-left:14px}.nav-tooltip{display:none!important}}
@media(max-height:899px){.desktop-shell{padding-top:12px;padding-bottom:12px}.desktop-rail{top:12px;height:calc(100vh - 24px);min-height:480px;padding-top:36px}.desktop-content{min-height:calc(100vh - 24px)}}
</style>
