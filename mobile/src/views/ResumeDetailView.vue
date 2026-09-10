<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import EmptyState from '../components/EmptyState.vue'
import {
  addResumeVersion, currentVersionOf, deleteResume, getResume, renameResume,
  setCurrentVersion, versionsOf,
} from '../data/store'
import { humanSize, previewResume, shareResumeFile, type ResumePreview } from '../data/resumeFile'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'

const route = useRoute()
const router = useRouter()
const resumeId = Number(route.params.id)

const resume = computed(() => getResume(resumeId))
const versions = computed(() => versionsOf(resumeId))
const current = computed(() => currentVersionOf(resumeId))

const preview = ref<ResumePreview>({ kind: 'none' })
const loading = ref(false)
const sharing = ref(false)
const renaming = ref(false)
const renameValue = ref('')
const fileRef = ref<HTMLInputElement | null>(null)
let lastUrl: string | null = null

async function loadPreview() {
  loading.value = true
  if (lastUrl) { URL.revokeObjectURL(lastUrl); lastUrl = null }
  const ver = current.value
  if (!ver) { preview.value = { kind: 'none' }; loading.value = false; return }
  const p = await previewResume(ver)
  if (p.url) lastUrl = p.url
  preview.value = p
  loading.value = false
}
watch(current, () => { void loadPreview() }, { immediate: true })
onBeforeUnmount(() => { if (lastUrl) URL.revokeObjectURL(lastUrl) })

// 极简 Markdown 渲染：转义后按行/列表/分隔线构建，不引入原始 HTML。
const mdHtml = computed(() => {
  if (preview.value.kind !== 'text' || !preview.value.text) return ''
  const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const inline = (s: string) => esc(s).replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  const out: string[] = []
  let inList = false
  for (const raw of preview.value.text.split(/\r?\n/)) {
    const line = raw.trimEnd()
    const h = /^(#{1,4})\s+(.*)$/.exec(line)
    if (h) {
      if (inList) { out.push('</ul>'); inList = false }
      const lv = h[1].length
      out.push(`<h${lv}>${inline(h[2])}</h${lv}>`)
    } else if (/^[-*]\s+/.test(line)) {
      if (!inList) { out.push('<ul>'); inList = true }
      out.push(`<li>${inline(line.replace(/^[-*]\s+/, ''))}</li>`)
    } else if (/^---+$/.test(line)) {
      if (inList) { out.push('</ul>'); inList = false }
      out.push('<hr>')
    } else if (line === '') {
      if (inList) { out.push('</ul>'); inList = false }
    } else {
      if (inList) { out.push('</ul>'); inList = false }
      out.push(`<p>${inline(line)}</p>`)
    }
  }
  if (inList) out.push('</ul>')
  return out.join('')
})

function openVersionPicker() { fileRef.value?.click() }
async function onVersionFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !resume.value) return
  await addResumeVersion(resumeId, file)
  toast('已新增最新版本')
}

async function onShare() {
  const ver = current.value
  if (!ver) return
  sharing.value = true
  try { await shareResumeFile(ver); toast('已发送简历文件') } catch { toast('分享失败') } finally { sharing.value = false }
}

function startRename() { renameValue.value = resume.value?.title ?? ''; renaming.value = true }
function saveRename() {
  const v = renameValue.value.trim()
  if (v && resume.value) renameResume(resumeId, v)
  renaming.value = false
}
const confirmingDelete = ref(false)
async function onDelete() {
  if (confirmingDelete.value) return
  confirmingDelete.value = true
  const ok = await confirmAction({
    title: `删除「${resume.value?.title}」？`,
    message: '该简历及其所有版本文件都会被移除，且不可恢复。',
    confirmLabel: '删除',
    danger: true,
  })
  confirmingDelete.value = false
  if (!ok) return
  await deleteResume(resumeId)
  toast('已删除')
  router.back()
}
function short(v: string): string {
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<template>
  <div v-if="resume">
    <header class="page-head">
      <button class="icon-btn" aria-label="返回" @click="router.back()"><AppIcon name="back" :size="18" /></button>
      <div class="grow">
        <template v-if="renaming">
          <input class="title-edit" v-model="renameValue" @keyup.enter="saveRename" @blur="saveRename" autofocus>
        </template>
        <h1 v-else class="page-title clickable" style="font-size: 18px" @click="startRename">
          {{ resume.title }}<AppIcon name="edit" :size="13" class="title-edit-ic" />
        </h1>
        <p class="page-sub">{{ versions.length }} 个版本 · 点标题可改名</p>
      </div>
      <button class="icon-btn" aria-label="删除简历" @click="onDelete"><AppIcon name="trash" :size="17" /></button>
    </header>

    <p class="section-kicker">版本 · 当前 {{ current ? `V${current.versionNo}` : '无' }}</p>
    <div class="version-rail">
      <button
        v-for="v in versions" :key="v.id"
        class="version-dot" :class="{ on: current?.id === v.id }"
        @click="setCurrentVersion(resumeId, v.id)"
      >V{{ v.versionNo }}<small>{{ short(v.createdAt) }}</small></button>
      <button class="version-dot add" @click="openVersionPicker"><AppIcon name="plus" :size="15" /> 新版</button>
    </div>
    <input ref="fileRef" type="file" hidden @change="onVersionFile">

    <div v-if="current" class="ver-meta">
      <span class="chip">{{ current.fileName }}</span>
      <span class="chip-meta">{{ humanSize(current.size) }} · {{ new Date(current.createdAt).toLocaleDateString('zh-CN') }}</span>
    </div>

    <!-- 预览：PDF 内嵌 / MD 渲染 / 缺失兜底 -->
    <div class="doc-stage">
      <div v-if="loading" class="doc-loading"><span class="spinner" aria-hidden="true" /> 正在读取文件…</div>
      <iframe v-else-if="preview.kind === 'pdf' && preview.url" class="doc-pdf" :src="preview.url" title="简历预览" />
      <img v-else-if="preview.kind === 'image' && preview.url" class="doc-image" :src="preview.url" alt="简历预览">
      <article v-else-if="preview.kind === 'text'" class="doc-md" v-html="mdHtml" />
      <EmptyState v-else icon="file" title="文件在本机已缺失" hint="这份简历的版本文件不在当前设备上，点「新增一版」重新上传一份。">
        <template #action>
          <button class="btn-primary" @click="openVersionPicker"><AppIcon name="upload" :size="16" /> 重新上传</button>
        </template>
      </EmptyState>
    </div>

    <div class="doc-actions">
      <button class="btn-ghost" @click="openVersionPicker"><AppIcon name="upload" :size="16" /> 新增一版</button>
      <button class="btn-primary" :disabled="!current || sharing" @click="onShare"><AppIcon name="share" :size="16" /> {{ sharing ? '发送中…' : '发送给 HR' }}</button>
    </div>
  </div>
  <EmptyState v-else icon="file" title="简历不存在" hint="它可能已被删除，返回简历列表查看其它版本。">
    <template #action>
      <button class="btn-ghost" @click="router.back()">返回</button>
    </template>
  </EmptyState>
</template>
