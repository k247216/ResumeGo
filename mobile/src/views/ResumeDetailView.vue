<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import EmptyState from '../components/EmptyState.vue'
import ResumeMark from '../components/ResumeMark.vue'
import Sheet from '../components/Sheet.vue'
import {
  addResumeVersion, currentVersionOf, deleteResume, getResume, renameResume,
  setCurrentVersion, setVersionNote, versionsOf,
} from '../data/store'
import { humanSize, isSupportedResume, previewResume, RESUME_FILE_ACCEPT, RESUME_UNSUPPORTED_HINT, shareResumeFile, type ResumePreview } from '../data/resumeFile'
import { shareErrorMessage, shareTargetName } from '../data/share'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { resumeMarkOf } from '../data/resumeMark'

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

const noteOpen = ref(false)
const noteValue = ref('')
function openNote() {
  if (!current.value) return
  noteValue.value = current.value.note ?? ''
  noteOpen.value = true
}
function saveNote() {
  const ver = current.value
  if (!ver) return
  setVersionNote(resumeId, ver.id, noteValue.value)
  noteOpen.value = false
  toast(noteValue.value.trim() ? '备注已保存' : '备注已清空')
}

function openVersionPicker() { fileRef.value?.click() }
async function onVersionFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !resume.value) return
  if (!isSupportedResume(file)) { toast(RESUME_UNSUPPORTED_HINT); return }
  await addResumeVersion(resumeId, file)
  toast('已新增最新版本')
  // 刚传完的这一刻最清楚自己改了什么，过了就懒得补了。
  openNote()
}

async function onShare() {
  const ver = current.value
  if (!ver) return
  sharing.value = true
  try {
    const target = await shareResumeFile(ver)
    const label = shareTargetName(target)
    if (target) toast(label ? `已交给${label}` : '已交给所选应用')
  } catch (err) {
    toast(shareErrorMessage(err))
  } finally {
    sharing.value = false
  }
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
  await router.replace({ name: 'resumes' })
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
      <ResumeMark v-if="resume" :variant="resume.mark ?? resumeMarkOf(resume.id)" :size="38" />
      <button class="icon-btn" aria-label="删除简历" @click="onDelete"><AppIcon name="trash" :size="17" /></button>
    </header>

    <p class="section-kicker">版本 · 当前 {{ current ? `V${current.versionNo}` : '无' }}</p>
    <div class="version-rail">
      <button
        v-for="v in versions" :key="v.id"
        class="version-dot" :class="{ on: current?.id === v.id }"
        @click="setCurrentVersion(resumeId, v.id)"
      >V{{ v.versionNo }}<small>{{ short(v.createdAt) }}</small><i v-if="v.note" class="dot-flag" aria-label="该版本已有备注" /></button>
      <button class="version-dot add" @click="openVersionPicker"><AppIcon name="plus" :size="15" /> 新版</button>
    </div>
    <input ref="fileRef" type="file" :accept="RESUME_FILE_ACCEPT" hidden @change="onVersionFile">

    <div v-if="current" class="ver-meta">
      <span class="chip">{{ current.fileName }}</span>
      <span class="chip-meta">{{ humanSize(current.size) }} · {{ new Date(current.createdAt).toLocaleDateString('zh-CN') }}</span>
    </div>

    <button v-if="current" class="note-row" @click="openNote">
      <span class="note-row-label"><AppIcon name="edit" :size="14" /> V{{ current.versionNo }} 备注</span>
      <span v-if="current.note" class="note-text">{{ current.note }}</span>
      <span v-else class="note-empty">这一版投了什么岗位、改了什么，写一句 ›</span>
    </button>

    <!-- 预览：PDF 内嵌 / MD 渲染 / 缺失兜底 -->
    <div class="doc-stage">
      <div v-if="loading" class="doc-loading"><span class="spinner" aria-hidden="true" /> 正在读取文件…</div>
      <iframe v-else-if="preview.kind === 'pdf' && preview.url" class="doc-pdf" :src="preview.url" title="简历预览" />
      <img v-else-if="preview.kind === 'image' && preview.url" class="doc-image" :src="preview.url" alt="简历预览">
      <article v-else-if="preview.kind === 'text'" class="doc-md" v-html="mdHtml" />
      <EmptyState v-else-if="preview.kind === 'unsupported'" icon="file" title="该格式无法在本机预览" hint="职达支持 PDF、Markdown、TXT 和图片简历。文件仍完整保存在本机，可以照常转发给 HR；换一份受支持的格式就能预览。">
        <template #action>
          <button class="btn-primary" @click="openVersionPicker"><AppIcon name="upload" :size="16" /> 重新上传</button>
        </template>
      </EmptyState>
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

    <Sheet v-if="noteOpen && current" :title="`V${current.versionNo} 备注`" @close="noteOpen = false">
      <p class="theme-intro">记下这一版投的岗位、改动的侧重点，回看版本历史时就不用靠记忆猜。</p>
      <div class="field">
        <label for="version-note-input">备注</label>
        <textarea id="version-note-input" v-model="noteValue" rows="4" maxlength="200" placeholder="例：投字节后端 3 面版，加了 Go 并发项目细节"></textarea>
        <small class="field-help">{{ noteValue.trim().length }}/200</small>
      </div>
      <div class="sheet-actions">
        <button class="btn-ghost" @click="noteOpen = false">取消</button>
        <button class="btn-primary" @click="saveNote">保存</button>
      </div>
    </Sheet>
  </div>
  <EmptyState v-else icon="file" title="简历不存在" hint="它可能已被删除，返回简历列表查看其它版本。">
    <template #action>
      <button class="btn-ghost" @click="router.back()">返回</button>
    </template>
  </EmptyState>
</template>
