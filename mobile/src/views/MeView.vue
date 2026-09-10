<script setup lang="ts">
import { ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import AppIcon from '../components/AppIcon.vue'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { exportBackup, importBackup, resetToSeed } from '../data/store'
import { getTheme, toggleTheme, type Theme } from '../data/theme'

const theme = ref<Theme>(getTheme())
const fileRef = ref<HTMLInputElement | null>(null)

function onToggleTheme() {
  theme.value = toggleTheme()
  toast(theme.value === 'dark' ? '已切换暗色' : '已切换浅色')
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

async function onReset() {
  const ok = await confirmAction({
    title: '清空并恢复示例？',
    message: '当前所有求职目标、日程与简历记录都会被清空，简历文件本体不会被恢复。',
    confirmLabel: '清空',
    danger: true,
  })
  if (!ok) return
  resetToSeed()
  toast('已恢复示例数据')
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

    <p class="section-kicker">外观</p>
    <div class="list">
      <button class="setting-row" @click="onToggleTheme">
        <span class="sr-ic"><AppIcon name="layers" :size="18" /></span>
        <span class="s-label">主题</span>
        <span class="s-value">{{ theme === 'dark' ? '暗色' : '浅色' }} ›</span>
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
      <button class="setting-row" style="color: var(--danger)" @click="onReset">
        <span class="sr-ic"><AppIcon name="trash" :size="18" /></span>
        <span class="s-label" style="color: var(--danger)">清空并恢复示例</span>
      </button>
    </div>

    <div class="about">
      <div class="setting-row" style="cursor: default">
        <span class="sr-ic"><AppIcon name="user" :size="18" /></span>
        <span class="s-label">职达 · 移动端</span>
        <span class="s-value">v0.2</span>
      </div>
      <p class="about-note">求职目标、日程、简历记录默认不上传云端；提醒在设备本地触发，无需推送服务器。<br>备份仅含文本记录，简历 PDF/MD 需在新设备重新导入。</p>
    </div>
  </div>
</template>
