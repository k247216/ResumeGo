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
          :data-nav="item.routeName"
          :data-icon="item.iconName"
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
        <button type="button" class="desktop-user" aria-label="本地用户状态" title="本地用户资料" @click="openProfile">
          <span class="desktop-user-avatar">
            <img v-if="isUploadedAvatar(profile.avatar) && !avatarLoadFailed" :src="profile.avatar" alt="" @error="avatarLoadFailed = true" />
            <span v-else>{{ avatarFallback(profile.avatar) }}</span>
          </span>
          <span class="local-dot" aria-hidden="true"></span>
        </button>
      </div>
    </aside>

    <main class="desktop-content">
      <slot />
    </main>

    <div v-if="profileOpen" class="profile-backdrop" role="presentation" @click.self="closeProfile">
      <section class="profile-dialog" role="dialog" aria-modal="true" aria-labelledby="profile-title" data-test="local-profile-dialog">
        <header class="profile-dialog-head">
          <div>
            <span>本地用户</span>
            <h2 id="profile-title">你的职业空间</h2>
          </div>
          <button type="button" class="profile-close" aria-label="关闭用户资料" @click="closeProfile">×</button>
        </header>
        <div class="profile-identity">
          <div class="profile-avatar-large">
            <img v-if="isUploadedAvatar(draftAvatar) && !draftAvatarLoadFailed" :src="draftAvatar" alt="头像预览" @error="draftAvatarLoadFailed = true" />
            <span v-else>{{ avatarFallback(draftAvatar) }}</span>
          </div>
          <div><strong>{{ draftName || '本地用户' }}</strong><small>资料、简历和使用记录只保存在本机</small></div>
        </div>
        <label class="profile-field"><span>显示名称</span><input v-model="draftName" maxlength="24" placeholder="输入你的名字" data-test="profile-name" /></label>
        <div class="profile-field"><span>选择头像</span><div class="avatar-options" role="radiogroup" aria-label="选择头像">
          <button v-for="avatar in avatarOptions" :key="avatar" type="button" class="avatar-option" :class="{ selected: draftAvatar === avatar }" :aria-checked="draftAvatar === avatar" role="radio" @click="draftAvatar = avatar">{{ avatar }}</button>
          <label class="avatar-upload avatar-option" aria-label="上传头像"><input type="file" accept="image/*" data-test="avatar-upload" @change="handleAvatarUpload" /><span aria-hidden="true">＋</span></label>
        </div></div>
        <div class="profile-stats">
          <div><small>开始使用</small><strong>{{ formatProfileTime(profile.createdAt, true) }}</strong></div>
          <div><small>最近使用</small><strong>{{ formatProfileTime(profile.lastUsedAt, false) }}</strong></div>
          <div><small>连续使用</small><strong>{{ streakDays }} 天</strong></div>
        </div>
        <section class="profile-heatmap" aria-labelledby="usage-heatmap-title">
          <div class="heatmap-head">
            <div><strong id="usage-heatmap-title">使用热力图</strong><small>按月查看</small></div>
            <div class="month-controls" aria-label="切换使用记录月份">
              <button type="button" data-test="usage-month-prev" aria-label="上个月" @click="shiftMonth(-1)">‹</button>
              <strong data-test="usage-month-label">{{ viewedMonthLabel }}</strong>
              <button type="button" data-test="usage-month-next" aria-label="下个月" @click="shiftMonth(1)">›</button>
            </div>
          </div>
          <div class="heatmap-weekdays" aria-hidden="true"><span v-for="weekday in weekdays" :key="weekday">{{ weekday }}</span></div>
          <div class="heatmap-grid-wrap">
            <div class="heatmap-grid" role="grid" :aria-label="`${viewedMonthLabel}使用热力图`">
              <template v-for="cell in heatmapCells" :key="cell.key">
                <button
                  v-if="cell.isCurrentMonth"
                  type="button"
                  class="heatmap-cell"
                  :class="[`level-${cell.level}`, { selected: selectedUsageKey === cell.key }]"
                  :data-test="`usage-day-${cell.key}`"
                  :aria-label="`${cell.key}，${cell.minutes ? formatUsageDuration(cell.minutes) : '0 分钟'}${cell.isDemo ? '，临时演示' : ''}`"
                  :title="`${cell.key} · ${cell.minutes ? formatUsageDuration(cell.minutes) : '0 分钟'}${cell.isDemo ? ' · 临时演示' : ''}`"
                  @mouseenter="hoverUsage(cell)"
                  @mouseleave="clearUsageHover"
                  @focus="hoverUsage(cell)"
                  @blur="clearUsageHover"
                  @click="selectUsageDay(cell.key)"
                ><i aria-hidden="true"></i></button>
                <span v-else class="heatmap-cell is-empty" aria-hidden="true"></span>
              </template>
            </div>
            <div v-if="hoveredUsage" class="heatmap-tooltip" role="tooltip" data-test="heatmap-tooltip">
              <strong>{{ hoveredUsage.key }}</strong>
              <span>{{ hoveredUsage.minutes ? formatUsageDuration(hoveredUsage.minutes) : '0 分钟' }}<em v-if="hoveredUsage.isDemo"> · 临时演示</em></span>
            </div>
          </div>
        </section>
        <footer class="profile-dialog-foot"><button type="button" class="profile-secondary" @click="closeProfile">取消</button><button type="button" class="profile-primary" data-test="profile-save" @click="saveProfile">保存资料</button></footer>
      </section>
    </div>
    <div v-if="isDesktopRuntime" class="desktop-usage-status" data-test="desktop-usage-status" aria-label="桌面端本地使用统计">
      <span>{{ profile.launchCount }} 次登录</span><i aria-hidden="true"></i><span>累计 {{ formatUsageDuration(totalUsageMinutes) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Aim, Calendar, ChatDotRound, Document, House, Moon, Setting, Sunny } from '@element-plus/icons-vue'
import BookIcon from '../components/BookIcon.vue'
import {
  buildMonthUsageCells,
  formatDateKey,
  formatUsageDuration,
  recordLocalUsage,
  readLocalProfile,
  touchLocalProfile,
  updateLocalProfile,
} from '../utils/localProfile'
import type { MonthUsageCell } from '../utils/localProfile'

const route = useRoute()
const themeStorageKey = 'resumego:theme'
const darkMode = ref(localStorage.getItem(themeStorageKey) === 'dark')
const desktopRuntime = Boolean(window.resumeGoDesktop)
const isDesktopRuntime = computed(() => desktopRuntime)
// 先同步读取已保存的资料，避免首帧用默认头像闪烁；真实使用记录在挂载后再写入。
const profile = ref(desktopRuntime ? touchLocalProfile() : readLocalProfile())
const profileOpen = ref(false)
const draftName = ref(profile.value.name)
const draftAvatar = ref(profile.value.avatar)
const avatarLoadFailed = ref(false)
const draftAvatarLoadFailed = ref(false)
const avatarOptions = ['林', 'K', '✦', '◐', '⌘', '职']
const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const now = new Date()
const viewedMonth = ref(new Date(now.getFullYear(), now.getMonth(), 1))
const selectedUsageKey = ref(formatDateKey(now))
const hoveredUsage = ref<MonthUsageCell | null>(null)
const streakDays = computed(() => {
  const dates = new Set(profile.value.usageDates)
  const cursor = new Date()
  let count = 0
  while (dates.has(formatDateKey(cursor))) {
    count += 1
    cursor.setDate(cursor.getDate() - 1)
  }
  return count
})
const viewedMonthLabel = computed(() => viewedMonth.value.toLocaleString('zh-CN', { year: 'numeric', month: 'long' }))
const heatmapCells = computed(() => buildMonthUsageCells(viewedMonth.value.getFullYear(), viewedMonth.value.getMonth(), profile.value))
const totalUsageMinutes = computed(() => Object.values(profile.value.usageMinutesByDate).reduce((total, minutes) => total + minutes, 0))

let usageTimer: number | undefined
onMounted(() => {
  // 桌面端已在 setup 中记录启动，避免首帧统计为 0 后再跳变；网页端沿用原有挂载记录。
  if (!desktopRuntime) profile.value = touchLocalProfile()
  usageTimer = window.setInterval(() => {
    profile.value = recordLocalUsage(1)
  }, 60_000)
})
onBeforeUnmount(() => {
  if (usageTimer) window.clearInterval(usageTimer)
  profile.value = recordLocalUsage(1)
})
watch(
  darkMode,
  (dark) => {
    localStorage.setItem(themeStorageKey, dark ? 'dark' : 'light')
    document.body.dataset.theme = dark ? 'dark' : 'light'
  },
  { immediate: true },
)
const navItems = [
  { label: '工作台', icon: House, iconName: 'House', routeName: 'workbench' },
  { label: '求职目标', icon: Aim, iconName: 'Aim', routeName: 'targets' },
  { label: '简历', icon: Document, iconName: 'Document', routeName: 'resumes' },
  { label: '知识库', icon: BookIcon, iconName: 'Book', routeName: 'knowledge' },
  { label: '模拟面试', icon: ChatDotRound, iconName: 'ChatDotRound', routeName: 'interview' },
  { label: '日程', icon: Calendar, iconName: 'Calendar', routeName: 'schedule' },
]

function openProfile() {
  profile.value = readLocalProfile()
  draftName.value = profile.value.name
  draftAvatar.value = profile.value.avatar
  avatarLoadFailed.value = false
  draftAvatarLoadFailed.value = false
  profileOpen.value = true
}
function closeProfile() { profileOpen.value = false }
function saveProfile() {
  profile.value = updateLocalProfile({ name: draftName.value, avatar: draftAvatar.value })
  avatarLoadFailed.value = false
  profileOpen.value = false
}
function shiftMonth(delta: number) {
  viewedMonth.value = new Date(viewedMonth.value.getFullYear(), viewedMonth.value.getMonth() + delta, 1)
  selectedUsageKey.value = formatDateKey(viewedMonth.value)
  hoveredUsage.value = null
}
function selectUsageDay(key: string) { selectedUsageKey.value = key }
function hoverUsage(cell: MonthUsageCell) { hoveredUsage.value = cell }
function clearUsageHover() { hoveredUsage.value = null }
function isUploadedAvatar(value: string) { return value.startsWith('data:image/') }
function avatarFallback(value: string) {
  if (isUploadedAvatar(value)) return '林'
  return value.trim().slice(0, 2) || '林'
}
function handleAvatarUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/') || file.size > 2 * 1024 * 1024) return
  const reader = new FileReader()
  reader.addEventListener('load', () => {
    if (typeof reader.result === 'string') draftAvatar.value = reader.result
  }, { once: true })
  reader.readAsDataURL(file)
}
function formatProfileTime(value: string, dateOnly: boolean) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', dateOnly
    ? { year: 'numeric', month: 'short', day: 'numeric' }
    : { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.desktop-shell{--canvas:#F5F5F2;--surface:rgba(255,255,255,.78);--surface-solid:#fff;--line:rgba(28,31,35,.11);--ink:#171717;--copy:#6E6E6A;--muted:#989893;--action-bg:#111212;--action-fg:#fff;--brand:#168B68;--brand-soft:rgba(22,139,104,.09);--danger:#b53c32;--danger-soft:rgba(180,62,53,.09);display:grid;grid-template-columns:56px minmax(0,1fr);gap:34px;min-height:100vh;padding:16px 24px 16px 18px;background:var(--canvas);color:var(--ink);color-scheme:light;transition:background .25s ease,color .25s ease}.desktop-shell.is-dark{--canvas:#111212;--surface:#1a1b1d;--surface-solid:#1a1b1d;--line:rgba(255,255,255,.12);--ink:#F2F2EF;--copy:#A3A39E;--muted:#73736F;--action-bg:#F1F1EE;--action-fg:#171717;--brand:#47B58E;--brand-soft:rgba(71,181,142,.11);--danger:#e06c60;--danger-soft:rgba(224,108,96,.14);color-scheme:dark}.desktop-rail{position:sticky;top:16px;z-index:50;display:flex;align-self:start;flex-direction:column;box-sizing:border-box;width:56px;height:calc(100vh - 32px);min-height:520px;padding:40px 8px 12px;border:1px solid rgba(255,255,255,.08);border-radius:22px;background:#0A0A0B;color:#fff;box-shadow:0 8px 24px rgba(0,0,0,.10)}.desktop-nav,.desktop-rail-bottom{display:grid;justify-items:center;gap:8px}.desktop-rail-bottom{margin-top:auto;gap:6px;padding-top:10px;border-top:1px solid rgba(255,255,255,.10)}.desktop-nav-item{position:relative;display:grid;width:40px;height:40px;place-items:center;border:0;border-radius:11px;background:transparent;color:#f3f4f4;text-decoration:none;cursor:pointer;transition:background .18s ease,color .18s ease}.desktop-nav-item:hover{background:#222324}.desktop-nav-item.router-link-active::before{content:'';position:absolute;inset:2px;border-radius:11px;background:#fff}.desktop-nav-item.router-link-active .el-icon{position:relative;z-index:1;color:#050606}.nav-tooltip{position:absolute;z-index:300;left:44px;display:none;padding:7px 10px;border-radius:8px;background:#080909;color:#fff;font-size:12px;font-weight:650;white-space:nowrap;box-shadow:0 8px 22px rgba(0,0,0,.2);pointer-events:none}.desktop-nav-item:hover .nav-tooltip,.desktop-nav-item:focus-visible .nav-tooltip{display:block}.desktop-user{position:relative;display:grid;width:36px;height:36px;place-items:center;border:1px solid rgba(255,255,255,.22);border-radius:50%;background:#f2eee7;color:#171819;cursor:pointer}.desktop-user:hover{box-shadow:0 0 0 3px rgba(255,255,255,.14)}.desktop-user-avatar{font-size:13px;font-weight:800;line-height:1}.local-dot{position:absolute;right:0;bottom:2px;width:9px;height:9px;border:2px solid #0A0A0B;border-radius:50%;background:#25b96c}.desktop-content{justify-self:center;width:100%;max-width:1224px;min-width:0;min-height:calc(100vh - 32px)}
/* 首页工作台：全高、禁文档滚动、left-anchored；Dock→应用 32px gap，应用 max-width 1270 不居中 */
.desktop-shell.shell-workspace{height:100vh;overflow:hidden;gap:32px}
.desktop-shell.shell-workspace{background:#fff}
.desktop-shell.is-dark.shell-workspace{background:#111212}
.desktop-shell.shell-workspace .desktop-content{justify-self:stretch;max-width:1270px;box-sizing:border-box;height:100%;min-height:0;padding:0 8px 0 0}
/* 全高页面（meta.fill：目标/简历/日程/设置）：full-bleed、内部面板滚动，不再居中窄容器 */
.desktop-shell.shell-fill{height:100vh;overflow:hidden;gap:16px}
.desktop-shell.shell-fill .desktop-content{justify-self:stretch;max-width:none;box-sizing:border-box;height:100%;min-height:0;padding:0 8px 0 0}
@media(max-width:900px){.desktop-shell{padding-left:14px}.nav-tooltip{display:none!important}}
@media(max-height:899px){.desktop-shell{padding-top:12px;padding-bottom:12px}.desktop-rail{top:12px;height:calc(100vh - 24px);min-height:480px;padding-top:36px}.desktop-content{min-height:calc(100vh - 24px)}}
.profile-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;background:rgba(15,18,18,.28);backdrop-filter:blur(5px);padding:24px}.profile-dialog{width:min(460px,100%);max-height:calc(100vh - 48px);overflow:auto;border:1px solid var(--line);border-radius:18px;background:var(--surface-solid);box-shadow:0 24px 70px rgba(0,0,0,.22);padding:22px;color:var(--ink)}.profile-dialog-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.profile-dialog-head span{color:var(--brand);font-size:10px;font-weight:750;letter-spacing:.12em}.profile-dialog-head h2{margin:4px 0 0;font-size:21px;letter-spacing:-.02em}.profile-close{border:0;background:none;color:var(--muted);font-size:22px;line-height:1;cursor:pointer}.profile-identity{display:flex;align-items:center;gap:12px;margin:22px 0 18px;padding:12px;border-radius:12px;background:var(--bg-subtle)}.profile-avatar-large{display:grid;place-items:center;width:48px;height:48px;border-radius:15px;background:var(--brand);color:#fff;font-size:19px;font-weight:800;overflow:hidden}.profile-avatar-large img{width:100%;height:100%;object-fit:cover}.profile-identity strong{display:block;font-size:14px}.profile-identity small{display:block;margin-top:4px;color:var(--muted);font-size:11px}.profile-field{display:grid;gap:7px;margin-top:15px}.profile-field>span{font-size:11px;font-weight:700;color:var(--copy)}.profile-field input{height:36px;border:1px solid var(--line);border-radius:9px;background:var(--surface-solid);color:var(--ink);padding:0 11px;outline:none}.profile-field input:focus{border-color:var(--brand);box-shadow:0 0 0 3px var(--brand-soft)}.avatar-options{display:flex;gap:8px;flex-wrap:wrap}.avatar-option{display:grid;place-items:center;width:34px;height:34px;border:1px solid var(--line);border-radius:10px;background:var(--surface-solid);color:var(--copy);font-weight:800;cursor:pointer}.avatar-option.selected{border-color:var(--brand);background:var(--brand-soft);color:var(--brand)}.avatar-upload{position:relative;grid-template-rows:16px 10px;gap:0;font-size:17px}.avatar-upload small{font-size:8px;font-weight:650}.avatar-upload input{position:absolute;inset:0;width:100%;height:100%;opacity:0;cursor:pointer}.profile-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:20px}.profile-stats div{display:grid;gap:5px;padding:10px;border-top:1px solid var(--line)}.profile-stats small{color:var(--muted);font-size:10px}.profile-stats strong{font-size:12px;font-variant-numeric:tabular-nums}.profile-heatmap{display:grid;gap:8px;margin-top:18px;padding-top:14px;border-top:1px solid var(--line)}.heatmap-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.heatmap-head>div:first-child{display:grid;gap:3px}.heatmap-head strong{font-size:12px}.heatmap-head small,.heatmap-foot{color:var(--muted);font-size:10px}.month-controls{display:flex;align-items:center;gap:6px}.month-controls strong{min-width:82px;text-align:center;font-size:11px;font-weight:750}.month-controls button{display:grid;width:22px;height:22px;place-items:center;border:1px solid var(--line);border-radius:7px;background:transparent;color:var(--copy);font-size:17px;line-height:1;cursor:pointer}.month-controls button:hover{border-color:var(--brand);color:var(--brand)}.heatmap-weekdays{display:grid;grid-template-columns:repeat(7,1fr);gap:4px;color:var(--muted);font-size:9px;text-align:center}.heatmap-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:4px}.heatmap-cell{display:grid;position:relative;min-width:0;aspect-ratio:1;border:1px solid transparent;border-radius:8px;background:var(--line);color:var(--copy);place-items:center;cursor:pointer;font:inherit}.heatmap-cell span{position:relative;z-index:1;font-size:10px;line-height:1}.heatmap-cell i{position:absolute;inset:4px;border-radius:5px;background:transparent}.heatmap-cell.level-1 i{background:rgba(22,139,104,.24)}.heatmap-cell.level-2 i{background:rgba(22,139,104,.44)}.heatmap-cell.level-3 i{background:rgba(22,139,104,.68)}.heatmap-cell.level-4 i{background:var(--brand)}.heatmap-cell.level-4{color:#fff}.heatmap-cell.selected{border-color:var(--ink);box-shadow:0 0 0 2px var(--surface-solid),0 0 0 3px var(--ink)}.heatmap-cell.is-empty{background:transparent;border-color:transparent;cursor:default}.daily-usage-detail{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:10px 11px;border:1px solid var(--line);border-radius:10px;background:var(--bg-subtle)}.daily-usage-detail div{display:grid;gap:3px}.daily-usage-detail strong{font-size:12px;font-variant-numeric:tabular-nums}.daily-usage-detail small{color:var(--muted);font-size:10px}.heatmap-foot{display:flex;align-items:center;gap:6px}.heatmap-legend{display:flex;gap:3px}.heatmap-legend i{display:block;width:10px;height:10px;border-radius:3px;background:var(--line)}.heatmap-legend .level-1{background:rgba(22,139,104,.24)}.heatmap-legend .level-2{background:rgba(22,139,104,.44)}.heatmap-legend .level-3{background:rgba(22,139,104,.68)}.heatmap-legend .level-4{background:var(--brand)}.heatmap-foot small{margin-left:auto}.profile-dialog-foot{display:flex;justify-content:flex-end;gap:8px;margin-top:22px}.profile-secondary,.profile-primary{border:1px solid var(--line);border-radius:9px;padding:8px 14px;font-size:12px;cursor:pointer}.profile-secondary{background:transparent;color:var(--copy)}.profile-primary{border-color:var(--brand);background:var(--brand);color:#fff;font-weight:700}
.desktop-user-avatar{display:grid;width:100%;height:100%;place-items:center;overflow:hidden;border-radius:50%}.desktop-user-avatar img{width:100%;height:100%;object-fit:cover}
.avatar-upload{grid-template-rows:1fr;gap:0;font-size:18px}.avatar-upload small{display:none}
.heatmap-weekdays{grid-template-columns:repeat(7,12px);gap:5px;justify-content:start}.heatmap-grid-wrap{position:relative;min-height:48px}.heatmap-grid{grid-template-columns:repeat(7,12px);gap:5px;justify-content:start;overflow:visible}.heatmap-cell{width:12px;height:12px;aspect-ratio:1;border:0;border-radius:3px;background:var(--line);padding:0}.heatmap-cell span{display:none}.heatmap-cell i{inset:0;border-radius:3px}.heatmap-cell.selected{border:1px solid var(--ink);box-shadow:0 0 0 2px var(--surface-solid),0 0 0 3px var(--ink)}.heatmap-cell.is-empty{background:transparent}.heatmap-tooltip{position:absolute;top:calc(100% + 6px);left:0;z-index:4;display:grid;gap:3px;min-width:116px;padding:7px 9px;border:1px solid var(--line);border-radius:8px;background:var(--surface-solid);box-shadow:0 8px 22px rgba(0,0,0,.14);pointer-events:none}.heatmap-tooltip strong{font-size:10px;font-variant-numeric:tabular-nums}.heatmap-tooltip span{color:var(--copy);font-size:10px}.heatmap-tooltip em{color:var(--muted);font-style:normal}.profile-heatmap{gap:10px}
.desktop-usage-status{position:fixed;right:28px;bottom:18px;z-index:20;display:inline-flex;align-items:center;gap:9px;color:var(--muted);font-size:11px;font-variant-numeric:tabular-nums;pointer-events:none}.desktop-usage-status i{width:3px;height:3px;border-radius:50%;background:var(--muted)}
</style>
