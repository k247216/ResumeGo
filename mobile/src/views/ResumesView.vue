<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import CompanyMark from '../components/CompanyMark.vue'
import EmptyState from '../components/EmptyState.vue'
import PickerField from '../components/PickerField.vue'
import ResumeMark from '../components/ResumeMark.vue'
import Sheet from '../components/Sheet.vue'
import {
  createInterviewLog, currentVersionOf, deleteInterviewLog, deleteResume, importResume, listInterviewLogs, listResumes, listSchedules, listTargets, updateInterviewLog, versionsOf,
} from '../data/store'
import { headEllipsis, humanSize, isSupportedResume, RESUME_FILE_ACCEPT, RESUME_UNSUPPORTED_HINT } from '../data/resumeFile'
import { resumeMarkOf } from '../data/resumeMark'
import { parseInterviewMany } from '../data/interviewParse'
import { toast } from '../data/toast'
import { confirmAction } from '../data/confirm'
import { QUESTION_CATS, type QuestionCat } from '../types/project'
import type { InterviewLog } from '../types/project'

const router = useRouter()
const resumes = computed(() => listResumes())
const fileRef = ref<HTMLInputElement | null>(null)
const busy = ref(false)

/** 简历与面经同属「下场要带的资料」：简历是你递出去的，面经是你带进场的。 */
const tab = ref<'resumes' | 'logs'>('resumes')

function openPicker() { fileRef.value?.click() }
async function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!isSupportedResume(file)) { toast(RESUME_UNSUPPORTED_HINT); return }
  busy.value = true
  try {
    const resume = await importResume('', file)
    toast('简历已导入')
    router.push({ name: 'resume-detail', params: { id: String(resume.id) } })
  } catch { toast('导入失败，请重试') } finally { busy.value = false }
}

function verCount(id: number) { return versionsOf(id).length }
function mdDay(isoStr: string): string {
  const d = new Date(isoStr)
  return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}月${d.getDate()}日`
}
/** 卡片标题已经是文件名本身，副行再重复一遍只会在长文件名上被裁两次——换成版本号和上传时间。 */
function verLine(id: number): string {
  const v = currentVersionOf(id)
  if (!v) return ''
  const day = mdDay(v.createdAt)
  return `当前 V${v.versionNo} · ${humanSize(v.size)}${day ? ` · ${day}上传` : ''}`
}
async function removeResume(id: number, title: string) {
  const ok = await confirmAction({
    title: `删除「${headEllipsis(title, 12)}」？`,
    message: '这份简历和它的全部版本会从本机移除，已绑定的求职目标也会解除关联。',
    confirmLabel: '删除', danger: true,
  })
  if (!ok) return
  await deleteResume(id)
  toast('简历已删除')
}

// ── 面经库：别人家的真实面试记录，粘贴进来变成可检索、可复习的纸面文章 ──
const interviewLogs = computed(() => listInterviewLogs())
const ivSheetOpen = ref(false)
const ivEditing = ref<InterviewLog | null>(null)
const ivReader = ref<InterviewLog | null>(null)
const ivForm = ref({ title: '', link: '', raw: '', source: 'imported' as 'self' | 'imported' })
/** 一次粘贴也可能包含多篇面经（再次出现「一面/笔试」就算新的一篇），保存时逐篇建档。 */
const ivParsedList = computed(() => (ivForm.value.raw.trim() ? parseInterviewMany(ivForm.value.raw) : []))

/** 关联首选日程：日程自带公司与「第几面」，面经才能逐条对上；只选计划则是整线共享。 */
const ivLinkOptions = computed(() => {
  const day = (iso: string) => {
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}/${d.getDate()}`
  }
  const scheds = listSchedules()
    .filter((s) => s.eventType === 'interview' || s.eventType === 'exam')
    .sort((a, b) => b.startTime.localeCompare(a.startTime))
    .map((s) => ({ value: `s${s.id}`, label: `${s.title} · ${day(s.startTime)}` }))
  const targets = listTargets().map((t) => ({ value: `t${t.id}`, label: `${t.name}（整条投递线）` }))
  return [...scheds, ...targets]
})
/** 解析关联选择：s 前缀 = 日程（targetId 跟着日程走），t 前缀 = 直接挂计划。 */
function resolveIvLink(link: string): { targetId: number | null; scheduleId: number | null } {
  if (link.startsWith('s')) {
    const scheduleId = Number(link.slice(1))
    const targetId = listSchedules().find((s) => s.id === scheduleId)?.jobProjectId ?? null
    return { targetId, scheduleId }
  }
  if (link.startsWith('t')) return { targetId: Number(link.slice(1)), scheduleId: null }
  return { targetId: null, scheduleId: null }
}
function linkOf(log: InterviewLog): string {
  if (log.scheduleId != null) return `s${log.scheduleId}`
  if (log.targetId != null) return `t${log.targetId}`
  return ''
}
function ivTargetName(log: InterviewLog): string {
  if (log.scheduleId != null) {
    const s = listSchedules().find((x) => x.id === log.scheduleId)
    if (s) return s.title
  }
  return listTargets().find((t) => t.id === log.targetId)?.name ?? ''
}
function ivSnippet(log: InterviewLog): string {
  const plain = log.contentHtml.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return headEllipsis(plain, 64)
}
function ivDate(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1} 月 ${d.getDate()} 日`
}
function openIvCreate() {
  ivEditing.value = null
  ivForm.value = { title: '', link: '', raw: '', source: 'imported' }
  ivSheetOpen.value = true
}
function openIvEdit(log: InterviewLog) {
  ivEditing.value = log
  // 编辑只保留原文重构：正文永远由解析器生成，不存在两份真相。
  ivForm.value = { title: log.title, link: linkOf(log), raw: log.contentHtml.replace(/<[^>]+>/g, '\n').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&').trim(), source: log.source ?? 'imported' }
  ivReader.value = null
  ivSheetOpen.value = true
}
function saveIv() {
  const title = ivForm.value.title.trim()
  const raw = ivForm.value.raw.trim()
  if (!title || !raw) return
  const valid = ivParsedList.value.filter((p) => p.html)
  if (!valid.length) {
    toast('粘贴的内容没有可用的正文')
    return
  }
  const { targetId, scheduleId } = resolveIvLink(ivForm.value.link)
  if (ivEditing.value) {
    const p = valid[0]
    // 问题清单变了，旧的「问过」勾选按新题数截断，防止越界勾选
    const asked = (ivEditing.value.asked ?? []).filter((i) => i < p.questions.length)
    updateInterviewLog(ivEditing.value.id, { title, targetId, scheduleId, source: ivForm.value.source, contentHtml: p.html, rounds: p.rounds, questions: p.questions, questionCats: p.cats, questionCount: p.questions.length, asked })
    toast('面经已更新')
  } else {
    valid.forEach((p, i) => {
      const name = valid.length > 1 ? `${title} · 第${i + 1}篇` : title
      createInterviewLog(name, targetId, p.html, p.rounds, p.questions, { source: ivForm.value.source, scheduleId, questionCats: p.cats })
    })
    const total = valid.reduce((n, p) => n + p.questions.length, 0)
    toast(valid.length > 1 ? `识别出 ${valid.length} 篇面经，已分别建档 · 共 ${total} 题` : `已收录 · ${valid[0].rounds} 轮 · ${valid[0].questions.length} 个问题`)
  }
  ivSheetOpen.value = false
}
async function removeIv(log: InterviewLog) {
  const ok = await confirmAction({
    title: '删除这篇面经？',
    message: `「${log.title}」会被永久删除，无法撤销。`,
    confirmLabel: '删除',
    danger: true,
  })
  if (!ok) return
  deleteInterviewLog(log.id)
  ivReader.value = null
  toast('面经已删除')
}
function sourceLabel(log: InterviewLog): string {
  return (log.source ?? 'imported') === 'self' ? '自记' : '搬运'
}

// ── 题目清单：按原文顺序逐题展示，每题带题型角标（八股/场景/手撕/项目），不做任何勾选 ──
const READER_CAT_ORDER: QuestionCat[] = ['rote', 'scene', 'coding', 'project']
const readerQuestions = computed(() => {
  const log = ivReader.value
  if (!log) return []
  const cats = log.questionCats ?? log.questions.map(() => 'rote' as QuestionCat)
  return log.questions.map((text, i) => ({ no: i + 1, text, cat: cats[i] ?? ('rote' as QuestionCat) }))
})
const readerCatSummary = computed(() => {
  const log = ivReader.value
  if (!log) return []
  const cats = log.questionCats ?? log.questions.map(() => 'rote' as QuestionCat)
  const counts = new Map<QuestionCat, number>()
  for (const c of cats) counts.set(c, (counts.get(c) ?? 0) + 1)
  return READER_CAT_ORDER.filter((c) => counts.has(c)).map((c) => ({ cat: c, count: counts.get(c)! }))
})
</script>

<template>
  <div>
    <header class="page-head">
      <div class="grow">
        <h1 class="page-title">我的资料</h1>
        <p class="page-sub">简历与面经都只存本机 · <b>{{ resumes.length }}</b> 份简历 · <b>{{ interviewLogs.length }}</b> 篇面经</p>
      </div>
      <button v-if="tab === 'resumes'" class="icon-btn" aria-label="导入简历" :disabled="busy" @click="openPicker">
        <AppIcon name="upload" :size="18" />
      </button>
    </header>

    <div class="rv-tabs" role="tablist" aria-label="资料类型切换">
      <button class="rv-tab" :class="{ on: tab === 'resumes' }" role="tab" :aria-selected="tab === 'resumes'" @click="tab = 'resumes'">简历库</button>
      <button class="rv-tab" :class="{ on: tab === 'logs' }" role="tab" :aria-selected="tab === 'logs'" @click="tab = 'logs'">面经库<em v-if="interviewLogs.length">{{ interviewLogs.length }}</em></button>
    </div>

    <template v-if="tab === 'resumes'">
      <input ref="fileRef" type="file" :accept="RESUME_FILE_ACCEPT" hidden @change="onFile">

      <!-- 首次使用：导入引导 -->
      <div v-if="!resumes.length" class="import-hero card">
        <div class="ih-art"><AppIcon name="file" :size="30" /></div>
        <h3>还没有简历</h3>
        <p>把你的简历文件传进来就能管理。之后每次更新，<br>再传一次即可，旧版本会完整保留。</p>
        <button class="btn-primary block" :disabled="busy" @click="openPicker">
          <AppIcon name="upload" :size="17" /> {{ busy ? '导入中…' : '上传简历' }}
        </button>
        <p class="ih-note">🔒 文件只存本机，不上传任何服务器</p>
      </div>

      <div v-else class="list">
        <article
          v-for="(r, i) in resumes" :key="r.id"
          class="card workspace-card resume-card" :class="`resume-tone-${i % 4}`" :style="{ '--i': i }"
          @click="router.push({ name: 'resume-detail', params: { id: String(r.id) } })"
        >
          <ResumeMark :variant="r.mark ?? resumeMarkOf(r.id)" :size="42" />
          <div class="head-copy">
            <h3>{{ r.title }}</h3>
            <small>{{ verLine(r.id) || '暂无版本' }}</small>
          </div>
          <div class="rc-side">
            <span class="stage-pill" :style="{ background: 'var(--brand-soft)', color: 'var(--brand)' }">{{ verCount(r.id) }} 版</span>
            <button class="resume-more" :aria-label="`删除 ${r.title}`" @click.stop="removeResume(r.id, r.title)"><AppIcon name="trash" :size="15" /></button>
            <AppIcon name="chevronRight" :size="18" class="rc-chev" />
          </div>
        </article>
      </div>

      <button v-if="resumes.length" class="fab" aria-label="导入简历" @click="openPicker"><AppIcon name="plus" :size="24" /></button>
    </template>

    <template v-else>
      <div class="section-head">
        <div>
          <h2>面经库</h2>
          <p>别人踩过的坑，就是你下场的地图</p>
        </div>
        <button class="btn-primary" @click="openIvCreate"><AppIcon name="plus" :size="16" /> 添加面经</button>
      </div>
      <div v-if="interviewLogs.length" class="iv-list">
        <button v-for="log in interviewLogs" :key="log.id" class="iv-card workspace-card" @click="ivReader = log">
          <span class="iv-card-head">
            <CompanyMark v-if="log.targetId || log.scheduleId != null" :name="ivTargetName(log)" :size="30" />
            <AppIcon v-else name="book" :size="22" />
            <span class="iv-copy">
              <strong>{{ log.title }}<em class="iv-src" :class="(log.source ?? 'imported') === 'self' ? 'self' : 'imp'">{{ sourceLabel(log) }}</em></strong>
              <small>{{ log.rounds }} 轮 · {{ log.questionCount }} 题<template v-if="ivTargetName(log)"> · {{ ivTargetName(log) }}</template></small>
            </span>
            <time>{{ ivDate(log.createdAt) }}</time>
          </span>
          <p class="iv-snippet">{{ ivSnippet(log) }}</p>
        </button>
      </div>
      <EmptyState
        v-else
        icon="book"
        title="还没有收藏面经"
        hint="在牛客、脉脉、贴吧看到真实面经，复制过来粘一下，它就成了你下场的地图。"
      />
    </template>

    <!-- 面经编辑 -->
    <Sheet v-if="ivSheetOpen" :title="ivEditing ? '编辑面经' : '添加面经'" @close="ivSheetOpen = false">
      <div class="field"><label>标题</label><input v-model="ivForm.title" placeholder="如：字节跳动 后端一面面经"></div>
      <div class="field">
        <label>来源</label>
        <div class="iv-src-picker" role="radiogroup" aria-label="面经来源">
          <button type="button" role="radio" :aria-checked="ivForm.source === 'imported'" :class="{ on: ivForm.source === 'imported' }" @click="ivForm.source = 'imported'">
            <strong>搬运的</strong><small>别人发的公开面经</small>
          </button>
          <button type="button" role="radio" :aria-checked="ivForm.source === 'self'" :class="{ on: ivForm.source === 'self' }" @click="ivForm.source = 'self'">
            <strong>自记的</strong><small>面完自己复述的真题</small>
          </button>
        </div>
      </div>
      <div class="field">
        <PickerField
          :model-value="ivForm.link"
          :options="ivLinkOptions"
          label="关联场次（选到具体面试，才能对上第几面）"
          title="这场面经对应哪场面试？"
          placeholder="不关联"
          clearable
          clear-label="不关联"
          searchable
          icon="target"
          @update:model-value="(v) => ivForm.link = (v as string)"
        />
      </div>
      <div class="field">
        <label>粘贴面经原文</label>
        <textarea v-model="ivForm.raw" class="iv-paste" rows="10" placeholder="从牛客 / 脉脉 / 贴吧复制面经原文，粘到这里——轮次、问题清单会自动识别排版。"></textarea>
        <p v-if="ivParsedList.length" class="iv-hint">{{ ivParsedList.length > 1 ? `识别出 ${ivParsedList.length} 篇面经，保存时会分别建档` : `已识别 ${ivParsedList[0].rounds} 轮 · ${ivParsedList[0].questions.length} 个问题，保存后自动排版` }}</p>
      </div>
      <div class="sheet-actions">
        <button class="btn-primary" :disabled="!ivForm.title.trim() || !ivForm.raw.trim()" @click="saveIv">保存</button>
      </div>
    </Sheet>

    <!-- 面经阅读器：纸面排版，问题清单、轮次标题都来自解析器 -->
    <div v-if="ivReader" class="iv-reader" role="dialog" aria-modal="true" :aria-label="ivReader.title">
      <div class="ivr-bar">
        <button class="icon-btn" aria-label="关闭" @click="ivReader = null"><AppIcon name="chevronLeft" :size="20" /></button>
        <strong class="ivr-title">{{ ivReader.title }}</strong>
        <button class="icon-btn" aria-label="编辑面经" @click="openIvEdit(ivReader)"><AppIcon name="edit" :size="16" /></button>
      </div>
      <article class="ivr-paper" v-html="ivReader.contentHtml"></article>
      <!-- 题目清单：按原文顺序逐题展示，每题带题型角标 -->
      <div v-if="ivReader.questions.length" class="ivr-qlist">
        <div class="ivrq-head">
          <strong>题目清单</strong>
          <span>按原文顺序 · 共 {{ ivReader.questions.length }} 题</span>
        </div>
        <div class="ivrq-cats">
          <span v-for="c in readerCatSummary" :key="c.cat" class="ivrq-cat-chip" :style="{ color: QUESTION_CATS[c.cat].color }">{{ QUESTION_CATS[c.cat].label }} {{ c.count }}</span>
        </div>
        <div v-for="q in readerQuestions" :key="q.no" class="ivq-card">
          <span class="ivq-no" :style="{ color: QUESTION_CATS[q.cat].color, borderColor: QUESTION_CATS[q.cat].color }">{{ q.no }}</span>
          <span class="ivq-text">{{ q.text }}</span>
          <span class="ivq-tag" :style="{ color: QUESTION_CATS[q.cat].color }">{{ QUESTION_CATS[q.cat].label }}</span>
        </div>
      </div>
      <div class="ivr-foot">
        <span class="ivr-meta">{{ sourceLabel(ivReader) }} · {{ ivReader.rounds }} 轮 · {{ ivReader.questionCount }} 个问题</span>
        <button class="btn-danger" @click="removeIv(ivReader)"><AppIcon name="trash" :size="14" /> 删除</button>
      </div>
    </div>
  </div>
</template>
