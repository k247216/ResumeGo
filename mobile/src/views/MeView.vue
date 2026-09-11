<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import Sheet from '../components/Sheet.vue'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { shareErrorMessage, shareFile, shareTargetName } from '../data/share'
import {
  exportBackup, hasQuarantinedData, importBackup, listResumes, listSchedules, listTargets,
  resetWorkspace, storageFaultMessage,
} from '../data/store'
import { getTheme, setTheme, THEME_OPTIONS, type Theme } from '../data/theme'
import { armAllReminders, collectReminderDiagnostics, type ReminderDiagnostics, type ReminderReport } from '../data/reminders'
import { previewReminder, REMINDER_OUTCOME_MESSAGES, requestExactAlarmPermission, requestNotificationPermission } from '../data/notifications'
import type { ScheduleEvent } from '../types/schedule'

const appVersion = __APP_VERSION__
const theme = ref<Theme>(getTheme())
const themeOpen = ref(false)
const fileRef = ref<HTMLInputElement | null>(null)
const activeTargetCount = computed(() => listTargets().filter((target) => target.status === 'active').length)
const resumeCount = computed(() => listResumes().length)
const scheduleCount = computed(() => listSchedules().length)
const faultMessage = ref<string | null>(storageFaultMessage())
const quarantined = ref(hasQuarantinedData())

const diag = ref<ReminderDiagnostics | null>(null)
const diagBusy = ref(false)
const guideOpen = ref(false)
const exporting = ref(false)

const permissionLabel = computed(() => {
  const state = diag.value?.permission
  if (state === 'granted') return '已开启'
  if (state === 'denied') return '未开启'
  return '浏览器预览'
})
const exactAlarmLabel = computed(() => {
  const state = diag.value?.exactAlarm
  if (state === 'granted') return '允许准点'
  if (state === 'denied') return '可能被延后'
  return '无需设置'
})
/** 意图条数与系统实际挂起条数的差值，就是被 ROM 后台清理吃掉的提醒。 */
const droppedCount = computed(() => {
  const current = diag.value
  if (!current || current.systemArmed === null) return null
  return Math.max(0, current.intended - current.systemArmed)
})
const diagSummary = computed(() => {
  const current = diag.value
  if (!current) return '检测中…'
  if (current.intended === 0) return '还没有提醒'
  if (current.permission !== 'granted') return '通知权限未开启'
  if (droppedCount.value && droppedCount.value > 0) return `${droppedCount.value} 条未被系统受理`
  return `${current.systemArmed ?? current.report?.scheduled ?? 0} 条已在系统`
})

async function refreshDiagnostics() {
  diag.value = await collectReminderDiagnostics()
  faultMessage.value = storageFaultMessage()
  quarantined.value = hasQuarantinedData()
}
onMounted(refreshDiagnostics)

/** 一条都没进系统时必须说清原因，否则「0 条已排入」会被读成恢复失败或恢复成功。 */
function reminderReportLine(report: ReminderReport): string {
  if (!report.total) return ''
  if (report.scheduled === report.total) return `${report.scheduled} 条提醒已排入`
  if (!report.scheduled) {
    const reason = report.denied ? 'denied' : report.web ? 'web' : report.failed ? 'failed' : 'skipped-past'
    return `0/${report.total} 条提醒未生效 · ${REMINDER_OUTCOME_MESSAGES[reason]}`
  }
  return `${report.scheduled}/${report.total} 条提醒已排入，其余未生效`
}

async function rearmAll() {
  diagBusy.value = true
  try {
    const report = await armAllReminders()
    await refreshDiagnostics()
    toast(reminderReportLine(report) || '没有需要重排的提醒')
  } finally {
    diagBusy.value = false
  }
}

async function enablePermission() {
  const granted = await requestNotificationPermission()
  await refreshDiagnostics()
  toast(granted ? '通知权限已开启，记得重新排一次提醒' : '系统仍未授予通知权限，提醒无法送达')
}

async function enableExactAlarm() {
  const state = await requestExactAlarmPermission()
  await refreshDiagnostics()
  toast(state === 'granted' ? '已允许准点提醒' : state === 'unsupported' ? '当前系统不需要该设置' : '仍未允许，提醒可能被系统延后')
}

async function testReminder() {
  // 用一条本地探针日程，不写库：让用户在没有日程时也能验证通知链路是否真的通。
  const probe: ScheduleEvent = {
    id: 0, title: '职达提醒自检', eventType: 'other',
    startTime: new Date(Date.now() + 3000).toISOString(), endTime: null, notes: null,
    jobDescriptionId: null, jobProjectId: null, createdAt: '', updatedAt: '',
  }
  const outcome = await previewReminder(probe)
  toast(outcome === 'scheduled' ? '3 秒后会弹一条测试通知' : REMINDER_OUTCOME_MESSAGES[outcome])
}

function chooseTheme(value: Theme) {
  theme.value = value
  setTheme(value)
  themeOpen.value = false
  toast(`已切换${THEME_OPTIONS.find((option) => option.value === value)?.label ?? '主题'}`)
}

async function onExport() {
  const json = exportBackup()
  exporting.value = true
  try {
    const target = await shareFile({
      fileName: `zhida-backup-${new Date().toISOString().slice(0, 10)}.json`,
      blob: new Blob([json], { type: 'application/json' }),
      subject: '职达备份',
      dialogTitle: '导出备份',
    })
    const label = shareTargetName(target)
    if (target) toast(label ? `备份已交给${label} · 不含简历文件本体` : '备份已交给所选应用 · 不含简历文件本体')
  } catch (err) {
    toast(shareErrorMessage(err))
  } finally {
    exporting.value = false
  }
}

function onImportClick() { fileRef.value?.click() }
async function onImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const text = await file.text()
  input.value = ''
  const ok = await confirmAction({
    title: '用这份备份覆盖当前数据？',
    message: '本机现有的求职目标、日程和简历记录会被替换，此操作不可撤销。建议先导出一次备份。',
    confirmLabel: '覆盖恢复',
    danger: true,
  })
  if (!ok) return
  const res = importBackup(text)
  if (!res.ok) { toast(res.message ?? '恢复失败'); return }
  const summary = res.restored
  // 恢复出来的提醒意图不会自动进系统，必须立刻重排，否则换机后一路静默。
  const report = await armAllReminders()
  await refreshDiagnostics()
  toast(summary
    ? `已恢复 ${summary.targets} 个目标 · ${summary.schedules} 条日程 · ${reminderReportLine(report) || '无需重排提醒'}`
    : '记录已恢复')
}

async function onReset() {
  const ok = await confirmAction({
    title: '清空本机全部记录？',
    message: '求职目标、日程、提醒和已导入的简历文件都会被永久删除，且无法撤销。请先导出备份。',
    confirmLabel: '全部清空',
    danger: true,
  })
  if (!ok) return
  const res = await resetWorkspace()
  if (!res.ok) { toast(res.message ?? '清空失败'); return }
  await refreshDiagnostics()
  toast('工作区已清空')
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

    <p v-if="faultMessage" class="notice-bar danger">{{ faultMessage }}</p>
    <p v-if="quarantined" class="notice-bar">检测到上次保存的记录无法读取。原始数据已单独保留、未被覆盖，当前显示的是空工作区。</p>

    <section class="me-overview card workspace-card" aria-label="本地工作区">
      <div class="workspace-head">
        <span class="ws-brand">职达<i>CareerOS</i></span>
        <span class="me-version">v{{ appVersion }}</span>
      </div>
      <p class="ws-note">目标、日程与简历都只写进这台设备的本地存储，不上传、不联网也能用。</p>
      <div class="me-metrics">
        <div class="metric-card pastel-pink"><strong>{{ activeTargetCount }}</strong><small>进行中的目标</small></div>
        <div class="metric-card pastel-yellow"><strong>{{ scheduleCount }}</strong><small>本地日程</small></div>
        <div class="metric-card pastel-mint"><strong>{{ resumeCount }}</strong><small>简历资产</small></div>
      </div>
    </section>

    <p class="section-kicker">提醒可靠性</p>
    <div class="list">
      <div class="setting-row diag-row">
        <span class="sr-ic"><AppIcon name="bell" :size="18" /></span>
        <span class="s-label">通知权限</span>
        <span class="s-value">{{ permissionLabel }}<i v-if="diag && diag.permission !== 'granted'" class="diag-flag">无法送达</i></span>
        <button v-if="diag && diag.permission !== 'granted'" class="btn-ghost btn-sm" @click="enablePermission">去开启</button>
      </div>
      <div class="setting-row diag-row">
        <span class="sr-ic"><AppIcon name="clock" :size="18" /></span>
        <span class="s-label">准点触发</span>
        <span class="s-value">{{ exactAlarmLabel }}<i v-if="diag && diag.exactAlarm === 'denied'" class="diag-flag">可能被延后</i></span>
        <button v-if="diag && diag.exactAlarm === 'denied'" class="btn-ghost btn-sm" @click="enableExactAlarm">去允许</button>
      </div>
      <div class="setting-row diag-row">
        <span class="sr-ic"><AppIcon name="check" :size="18" /></span>
        <span class="s-label">系统受理</span>
        <span class="s-value">{{ diagSummary }}</span>
      </div>
      <button class="setting-row hint-row" :disabled="diagBusy" @click="rearmAll">
        <span class="sr-ic"><AppIcon name="send" :size="18" /></span>
        <span class="s-label">{{ diagBusy ? '正在排入系统…' : '重新排入系统' }}</span>
        <span class="s-value">修好后点一次 ›</span>
      </button>
      <button class="setting-row hint-row" @click="testReminder">
        <span class="sr-ic"><AppIcon name="bell" :size="18" /></span>
        <span class="s-label">发送测试提醒</span>
        <span class="s-value">3 秒后弹横幅 ›</span>
      </button>
      <button class="setting-row hint-row" @click="guideOpen = true">
        <span class="sr-ic"><AppIcon name="external" :size="18" /></span>
        <span class="s-label">厂商省电设置</span>
        <span class="s-value">收不到提醒时看这里 ›</span>
      </button>
    </div>

    <p class="section-kicker">外观</p>
    <div class="list">
      <button class="setting-row hint-row" @click="themeOpen = true">
        <span class="sr-ic"><AppIcon name="layers" :size="18" /></span>
        <span class="s-label">主题</span>
        <span class="s-value">{{ THEME_OPTIONS.find((option) => option.value === theme)?.label }} ›</span>
      </button>
    </div>

    <p class="section-kicker">数据</p>
    <div class="list">
      <button class="setting-row hint-row" :disabled="exporting" @click="onExport">
        <span class="sr-ic"><AppIcon name="download" :size="18" /></span>
        <span class="s-label">{{ exporting ? '正在生成备份…' : '导出备份' }}</span>
        <span class="s-value">{{ exporting ? '请稍候' : 'JSON ›' }}</span>
      </button>
      <button class="setting-row hint-row" @click="onImportClick">
        <span class="sr-ic"><AppIcon name="upload" :size="18" /></span>
        <span class="s-label">恢复备份</span>
        <span class="s-value">覆盖本机 ›</span>
      </button>
      <button class="setting-row hint-row" style="color: var(--danger)" @click="onReset">
        <span class="sr-ic"><AppIcon name="trash" :size="18" /></span>
        <span class="s-label" style="color: var(--danger)">清空本机记录</span>
        <span class="s-value" style="color: var(--danger)">不可恢复 ›</span>
      </button>
      <input ref="fileRef" type="file" accept="application/json" hidden @change="onImportFile">
    </div>

    <div class="about">
      <p class="about-note">备份只含文本记录，简历 PDF/MD 文件本体需在新设备重新导入。</p>
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

    <Sheet v-if="guideOpen" title="提醒送达设置指南" @close="guideOpen = false">
      <p class="guide-lead">职达不使用推送服务器，提醒由手机系统自己触发。国产系统为了省电会默认杀掉后台计划，所以需要你在系统里放行一次。</p>
      <ol class="guide-list">
        <li><strong>通知权限</strong>：设置 → 应用管理 → 职达 → 通知，允许全部通知。</li>
        <li><strong>提醒渠道</strong>：同一页里的「日程提醒」渠道，把重要性设为「允许横幅通知」，否则提醒只会安静地躺进下拉栏。</li>
        <li><strong>小米 / 红米</strong>：应用管理 → 职达 → 省电策略，选「无限制」，并开启「自启动」。</li>
        <li><strong>华为 / 荣耀</strong>：设置 → 电池 → 应用启动管理 → 职达，改「手动管理」并保留后台活动。</li>
        <li><strong>OPPO / 一加 / vivo</strong>：设置 → 电池 → 后台耗电管理，允许职达后台运行。</li>
        <li><strong>准点提醒</strong>：Android 12 以上若未允许「闹钟和提醒」，通知会晚几分钟才到，可在上方点「去允许」。</li>
        <li>放行后回到本页点一次<strong>「重新排入系统」</strong>，之前被清掉的提醒才会补回来。</li>
      </ol>
      <p class="chip-meta">不同系统版本的菜单名称略有差异，按上述路径在设置里搜索「职达」或「自启动」即可找到。</p>
      <div class="sheet-actions">
        <button class="btn-primary" @click="guideOpen = false">知道了</button>
      </div>
    </Sheet>
  </div>
</template>
