<script setup lang="ts">
import { computed, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import AppIcon from '../components/AppIcon.vue'
import Sheet from '../components/Sheet.vue'
import { toast } from '../data/toast'
import { exportBackup, importBackup, listResumes, listSchedules, listTargets } from '../data/store'
import { getTheme, setTheme, THEME_OPTIONS, type Theme } from '../data/theme'

const theme = ref<Theme>(getTheme())
const themeOpen = ref(false)
const fileRef = ref<HTMLInputElement | null>(null)
const activeTargetCount = computed(() => listTargets().filter((target) => target.status === 'active').length)
const resumeCount = computed(() => listResumes().length)
const scheduleCount = computed(() => listSchedules().length)
const nextSchedule = computed(() => listSchedules().find((event) => new Date(event.startTime).getTime() >= Date.now() - 3_600_000) ?? null)
function nextScheduleLabel() {
  if (!nextSchedule.value) return '暂时没有下一场安排'
  const date = new Date(nextSchedule.value.startTime)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')} · ${nextSchedule.value.title}`
}

function chooseTheme(value: Theme) {
  theme.value = value
  setTheme(value)
  themeOpen.value = false
  toast(`已切换${THEME_OPTIONS.find((option) => option.value === value)?.label ?? '主题'}`)
}

async function onExport() {
  const json = exportBackup()
  if (Capacitor.isNativePlatform()) {
    const { Filesystem, Directory } = await import('@capacitor/filesystem')
    const { Share } = await import('@capacitor/share')
    const written = await Filesystem.writeFile({ path: `zhida-backup-${Date.now()}.json`, data: json, directory: Directory.Documents })
    await Share.share({ title: '职达备份', url: written.uri })
  } else {
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `zhida-backup-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }
  toast('备份已导出（不含简历文件本体）')
}

function onImportClick() { fileRef.value?.click() }
async function onImportFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const text = await file.text()
  const res = importBackup(text)
  toast(res.ok ? '记录已恢复' : (res.message ?? '恢复失败'))
  ;(e.target as HTMLInputElement).value = ''
}

</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">我的</h1>
        <p class="page-sub">本地优先 · 数据仅存于此设备</p>
      </div>
    </header>

    <section class="me-profile card workspace-card" aria-label="本地工作区状态">
      <span class="me-avatar"><AppIcon name="user" :size="22" /></span>
      <div class="me-profile-copy">
        <strong>职达 · Career OS</strong>
        <small>你的本地职业资产空间</small>
      </div>
      <span class="me-version">移动端 · v0.2.5</span>
    </section>

    <p class="section-kicker">工作区概况</p>
    <section class="me-overview card workspace-card" aria-label="工作区概况">
      <div class="me-metrics">
        <div class="metric-card pastel-pink"><strong>{{ activeTargetCount }}</strong><small>进行中的目标</small></div>
        <div class="metric-card pastel-yellow"><strong>{{ scheduleCount }}</strong><small>本地日程</small></div>
        <div class="metric-card pastel-mint"><strong>{{ resumeCount }}</strong><small>简历资产</small></div>
      </div>
      <div class="me-next"><span class="me-next-dot" /><span><small>下一步安排</small><strong>{{ nextScheduleLabel() }}</strong></span></div>
    </section>

    <p class="section-kicker">外观</p>
    <div class="list">
      <button class="setting-row" @click="themeOpen = true">
        <span class="sr-ic"><AppIcon name="layers" :size="18" /></span>
        <span class="s-label">主题</span>
        <span class="s-value">{{ THEME_OPTIONS.find((option) => option.value === theme)?.label }} ›</span>
      </button>
    </div>

    <p class="section-kicker">数据</p>
    <div class="list">
      <button class="setting-row" @click="onExport">
        <span class="sr-ic"><AppIcon name="download" :size="18" /></span>
        <span class="s-label">导出备份</span>
        <span class="s-value">JSON ›</span>
      </button>
      <button class="setting-row" @click="onImportClick">
        <span class="sr-ic"><AppIcon name="upload" :size="18" /></span>
        <span class="s-label">恢复备份</span>
        <span class="s-value">选择文件 ›</span>
      </button>
      <input ref="fileRef" type="file" accept="application/json" hidden @change="onImportFile">
    </div>

    <div class="about">
      <div class="setting-row" style="cursor: default">
        <span class="sr-ic"><AppIcon name="user" :size="18" /></span>
        <span class="s-label">职达 · 移动端</span>
        <span class="s-value">v0.2.5</span>
      </div>
      <p class="about-note">求职目标、日程、简历记录默认不上传云端；提醒在设备本地触发，无需推送服务器。<br>备份仅含文本记录，简历 PDF/MD 需在新设备重新导入。</p>
    </div>

    <Sheet v-if="themeOpen" title="选择主题" @close="themeOpen = false">
      <p class="theme-intro">让工作区适合你今天的专注状态。</p>
      <div class="theme-options" role="radiogroup" aria-label="主题选项">
        <button
          v-for="option in THEME_OPTIONS"
          :key="option.value"
          class="theme-option"
          :class="{ on: theme === option.value }"
          role="radio"
          :aria-checked="theme === option.value"
          @click="chooseTheme(option.value)"
        >
          <span class="theme-swatch" :class="`theme-${option.value}`" aria-hidden="true"><i /></span>
          <span class="theme-option-copy"><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
          <AppIcon v-if="theme === option.value" name="check" :size="17" class="theme-check" />
        </button>
      </div>
    </Sheet>
  </div>
</template>
