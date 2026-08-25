<template>
  <section class="resume-library" data-test="resume-library-view">
    <!-- ── 头部：标题 + 主行动 ── -->
    <header class="lib-head">
      <div class="lib-head-copy">
        <h1>简历库</h1>
        <p class="lib-sub">
          {{ library.items.value.length }} 份简历资产<template v-if="library.items.value.length"> · 版本即历史，副本即独立表达</template>
        </p>
      </div>
      <div class="lib-actions">
        <button type="button" class="btn-ghost" data-test="import-md-header" @click="pickImportFile">导入 Markdown</button>
        <button type="button" class="btn-solid" data-test="create-blank-header" @click="goCreateBlank">创建空白简历</button>
      </div>
    </header>

    <!-- ── 工具栏：种类过滤 + 搜索 ── -->
    <div class="lib-toolbar">
      <div class="filter-pills" role="tablist" aria-label="简历种类过滤">
        <button
          v-for="option in KIND_OPTIONS"
          :key="option.value"
          type="button"
          class="filter-pill"
          :class="{ on: library.filter.value.kind === option.value }"
          :data-test="`filter-${option.value}`"
          @click="setKind(option.value)"
        >{{ option.label }}</button>
      </div>
      <label class="toolbar-search">
        <el-icon :size="13"><Search /></el-icon>
        <input
          :value="library.filter.value.keyword"
          type="search"
          placeholder="搜索简历名称"
          data-test="library-search"
          @input="library.filter.value.keyword = ($event.target as HTMLInputElement).value"
        />
      </label>
    </div>

    <!-- ── 主体：纸张卡片网格 + 右侧版本面板 ── -->
    <div class="lib-body">
      <div class="shelf" data-test="asset-shelf">
        <div v-if="library.loading.value && !library.items.value.length" class="shelf-state">正在读取本地简历…</div>

        <div v-else-if="library.errorMessage.value && !library.items.value.length" class="shelf-state error" data-test="resume-library-error">
          <strong>无法读取本地简历</strong>
          <span>{{ library.errorMessage.value }}</span>
          <button type="button" class="state-btn" data-test="retry-load" @click="library.load()">重新加载</button>
        </div>

        <div v-else-if="!library.visibleItems.value.length" class="shelf-state empty" data-test="resume-library-empty">
          <strong>创建第一份本地简历</strong>
          <span>可以先整理一份通用简历，也可以直接导入 Markdown 文件。</span>
          <button type="button" class="state-solid" data-test="create-blank" @click="goCreateBlank">从空白开始</button>
          <button type="button" class="state-btn" data-test="import-md-empty" @click="pickImportFile">导入 Markdown</button>
        </div>

        <div v-else class="paper-grid">
          <article
            v-for="(resume, index) in library.visibleItems.value"
            :key="resume.id"
            class="paper-card"
            :class="{ selected: resume.id === library.selectedResumeId.value, expression: isExpression(resume) }"
            :style="{ '--i': Math.min(index, 8) }"
            :data-test="`asset-row-${resume.id}`"
            @click="library.selectResume(resume.id)"
          >
            <div class="paper-visual">
              <span class="paper-kind" :data-test="`asset-kind-${resume.id}`">{{ kindLabel(resume) }}</span>
              <div class="paper-face">
                <strong class="paper-name">{{ paperName(resume) }}</strong>
                <span class="paper-role">{{ paperRole(resume) }}</span>
                <span class="paper-lines"><i></i><i></i><i></i></span>
              </div>
              <em v-if="resume.currentVersion" class="paper-version">V{{ resume.currentVersion.versionNo }}</em>
            </div>
            <div class="paper-meta">
              <div class="paper-meta-copy">
                <strong>{{ resume.title }}</strong>
                <small>{{ updatedLabel(resume) }}<template v-if="resume.archivedAt"> · 已归档</template></small>
              </div>
              <div class="paper-meta-actions" @click.stop>
                <router-link
                  v-if="resume.currentVersion"
                  data-test="continue-editing"
                  :to="buildResumeEditorLocation({ resumeId: resume.id, versionId: resume.currentVersion.id })"
                >编辑</router-link>
                <button type="button" data-test="open-fork" @click="openForkFor(resume)">副本</button>
                <button v-if="!resume.archivedAt" type="button" data-test="archive-resume" @click="openArchiveFor(resume)">归档</button>
              </div>
            </div>
          </article>

          <button type="button" class="paper-card import-card" data-test="import-md" @click="pickImportFile">
            <span class="import-plus"><el-icon :size="26"><Plus /></el-icon></span>
            <span class="import-label">导入 Markdown</span>
          </button>
        </div>
      </div>

      <aside class="lib-inspector">
        <ResumeVersionInspector
          v-if="inspectorOpen && library.selectedResume.value"
          :resume="library.selectedResume.value"
          :versions="library.versions.value"
          :selected-version-id="library.selectedVersionId.value"
          :version-loading="library.versionLoading.value"
          :version-error="library.versionError.value"
          :used-by-targets="usedByTargets"
          :used-by-loading="usedByLoading"
          @select-version="library.selectVersion($event)"
          @open-target="openTarget"
          @archive="openArchive"
          @close="inspectorOpen = false"
        >
          <template #identity-actions>
            <router-link
              v-if="library.selectedResume.value.currentVersion"
              class="inspector-primary"
              data-test="continue-editing"
              :to="buildResumeEditorLocation({ resumeId: library.selectedResume.value.id, versionId: library.selectedResume.value.currentVersion.id })"
            >继续编辑</router-link>
            <div class="inspector-secondary-row">
              <router-link
                v-if="library.selectedResume.value.currentVersion"
                class="inspector-secondary"
                data-test="view-current-version"
                :to="buildResumeEditorLocation({ resumeId: library.selectedResume.value.id, versionId: library.selectedResume.value.currentVersion.id })"
              >查看当前版本</router-link>
              <button type="button" class="inspector-secondary" data-test="open-fork" @click="openFork">创建岗位表达副本</button>
            </div>
          </template>
        </ResumeVersionInspector>
        <div v-else-if="library.selectedResume.value" class="inspector-collapsed">
          <button type="button" data-test="inspector-reopen" @click="inspectorOpen = true">版本与引用</button>
        </div>
        <div v-else class="inspector-placeholder">选中一份简历查看版本与引用</div>
      </aside>
    </div>

    <input ref="fileInput" type="file" accept=".md,.markdown,.txt,text/markdown" data-test="import-file" class="file-input" @change="handleFileChange" />

    <ResumeForkDialog
      :open="forkOpen"
      :resume="library.selectedResume.value"
      :version="library.selectedVersion.value"
      :submitting="forkSubmitting"
      :error="forkError"
      @confirm="confirmFork"
      @cancel="closeFork"
    />

    <ResumeArchiveDialog
      :open="archiveOpen"
      :resume="library.selectedResume.value"
      :submitting="archiveSubmitting"
      :error="archiveError"
      @confirm="confirmArchive"
      @cancel="closeArchive"
    />

    <div v-if="importOpen" class="import-backdrop" role="presentation" @click.self="closeImport">
      <section class="import-dialog" role="dialog" aria-modal="true" aria-labelledby="import-title" data-test="import-dialog">
        <button type="button" class="dialog-close" aria-label="关闭" @click="closeImport">×</button>
        <p>从 Markdown 导入</p>
        <h2 id="import-title">{{ importParsed?.title || '正在解析…' }}</h2>
        <span class="import-file-name">{{ importFileName }}</span>
        <p v-if="importParsed?.warnings.length" class="import-warning">{{ importParsed.warnings.join('；') }}</p>
        <ul v-if="importSummary.length" class="import-summary">
          <li v-for="row in importSummary" :key="row"><el-icon :size="13"><Check /></el-icon>{{ row }}</li>
        </ul>
        <p v-if="importError" class="import-error">{{ importError }}</p>
        <footer>
          <button type="button" class="ghost" @click="closeImport">取消</button>
          <button type="button" class="primary" data-test="import-confirm" :disabled="importing || !!importParsed?.warnings.length" @click="confirmImport">{{ importing ? '导入中…' : '创建简历' }}</button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Check, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createResume } from '../../api/resume'
import { getJobDescription } from '../../api/job'
import { useResumeLibrary } from '../../composables/useResumeLibrary'
import type { ResumeLibraryKindFilter } from '../../composables/useResumeLibrary'
import type { Resume } from '../../types/resume'
import ResumeVersionInspector from '../../components/resume-library/ResumeVersionInspector.vue'
import ResumeForkDialog from '../../components/resume-library/ResumeForkDialog.vue'
import ResumeArchiveDialog from '../../components/resume-library/ResumeArchiveDialog.vue'
import { useTargetsStore } from '../../stores/targets'
import type { ParsedMarkdownResume } from '../../utils/parseMarkdownResume'
import { parseMarkdownResume } from '../../utils/parseMarkdownResume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const KIND_OPTIONS: Array<{ value: ResumeLibraryKindFilter; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'general', label: '通用简历' },
  { value: 'expression', label: '岗位表达' },
  { value: 'archived', label: '归档' },
]

const library = useResumeLibrary()
const targetsStore = useTargetsStore()
const router = useRouter()

const inspectorOpen = ref(true)
watch(() => library.selectedResumeId.value, () => { inspectorOpen.value = true })

function setKind(kind: ResumeLibraryKindFilter) {
  library.filter.value.kind = kind
  void library.load()
}

function goCreateBlank() {
  void router.push(buildResumeEditorLocation({ mode: 'blank' }))
}

function isExpression(resume: Resume) {
  return resume.kind === 'JOB_EXPRESSION'
}
function kindLabel(resume: Resume) {
  return isExpression(resume) ? '岗位表达' : '通用'
}
function paperName(resume: Resume) {
  const basic = resume.currentVersion?.content.basicInfo
  return basic?.name?.trim() || resume.title
}
function paperRole(resume: Resume) {
  const basic = resume.currentVersion?.content.basicInfo
  return basic?.targetRole?.trim() || '求职意向待补充'
}
function updatedLabel(resume: Resume) {
  const value = resume.updatedAt ?? resume.currentVersion?.createdAt
  if (!value) return '时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '时间未知' : `${date.getMonth() + 1}月${date.getDate()}日更新`
}

// 卡片上的 fork/归档：先选中再打开，保证弹窗上下文正确
function openForkFor(resume: Resume) {
  library.selectResume(resume.id)
  openFork()
}
function openArchiveFor(resume: Resume) {
  library.selectResume(resume.id)
  openArchive()
}

// ── fork 流程 ──
const forkOpen = ref(false)
const forkSubmitting = ref(false)
const forkError = ref('')

function openFork() {
  if (!library.selectedResume.value) return
  forkError.value = ''
  forkOpen.value = true
}
function closeFork() {
  if (forkSubmitting.value) return
  forkOpen.value = false
  forkError.value = ''
}
async function confirmFork(title: string) {
  const version = library.selectedVersion.value
  if (!version || forkSubmitting.value) return
  forkSubmitting.value = true
  forkError.value = ''
  try {
    const forked = await library.fork(version.id, title)
    forkOpen.value = false
    ElMessage.success(`已创建副本「${forked.title}」`)
  } catch (error) {
    forkError.value = error instanceof Error ? error.message : '创建副本失败'
  } finally {
    forkSubmitting.value = false
  }
}

// ── 归档流程 ──
const archiveOpen = ref(false)
const archiveSubmitting = ref(false)
const archiveError = ref('')

function openArchive() {
  if (!library.selectedResume.value) return
  archiveError.value = ''
  archiveOpen.value = true
}
function closeArchive() {
  if (archiveSubmitting.value) return
  archiveOpen.value = false
  archiveError.value = ''
}
async function confirmArchive() {
  const resume = library.selectedResume.value
  if (!resume || archiveSubmitting.value) return
  archiveSubmitting.value = true
  archiveError.value = ''
  try {
    await library.archive(resume.id)
    archiveOpen.value = false
    ElMessage.success('简历已归档')
  } catch (error) {
    archiveError.value = error instanceof Error ? error.message : '归档失败'
  } finally {
    archiveSubmitting.value = false
  }
}

// ── 用于：反查引用本简历任意版本的目标，经 JD 显示 公司 · 岗位 ──
interface UsedByTarget { targetId: number; jobDescriptionId: number | null; label: string }
const usedByTargets = ref<UsedByTarget[]>([])
const usedByLoading = ref(false)
const jobLabelCache = ref(new Map<number, string>())
const usedByKey = computed(() => `${library.selectedResumeId.value}:${library.versions.value.map((v) => v.id).join(',')}`)

watch(usedByKey, async () => {
  usedByTargets.value = []
  if (!library.selectedResume.value) return
  if (!targetsStore.targets.length && !targetsStore.loading) {
    try { await targetsStore.load() } catch { /* 读不到目标时按未关联呈现 */ }
  }
  const key = usedByKey.value
  const versionIds = new Set(library.versions.value.map((v) => v.id))
  if (library.selectedResume.value.currentVersion?.id) versionIds.add(library.selectedResume.value.currentVersion.id)
  const matched = targetsStore.targets.filter((target) => target.resumeVersionId != null && versionIds.has(target.resumeVersionId))
  usedByLoading.value = true
  try {
    const rows = (await Promise.all(matched.map(async (target) => {
      let label = target.name || '求职目标'
      if (target.jobDescriptionId != null) {
        const jobLabel = await jobLabelFor(target.jobDescriptionId, label)
        if (jobLabel.includes(' · ')) label = jobLabel
      }
      return { targetId: target.id, jobDescriptionId: target.jobDescriptionId, label }
    }))).sort((left, right) => left.targetId - right.targetId)
    if (key === usedByKey.value) usedByTargets.value = rows
  } finally {
    if (key === usedByKey.value) usedByLoading.value = false
  }
}, { immediate: true })

async function jobLabelFor(jobDescriptionId: number, fallback: string): Promise<string> {
  const cached = jobLabelCache.value.get(jobDescriptionId)
  if (cached !== undefined) return cached
  try {
    const response = await getJobDescription(jobDescriptionId)
    const job = response.data
    const label = [job.companyName, job.jobTitle].filter(Boolean).join(' · ') || fallback
    jobLabelCache.value.set(jobDescriptionId, label)
    return label
  } catch {
    return fallback
  }
}

function openTarget(targetId: number) {
  void router.push({ name: 'targets', query: { targetId: String(targetId) } })
}

// ── Markdown 导入 ──
const fileInput = ref<HTMLInputElement | null>(null)
const importOpen = ref(false)
const importFileName = ref('')
const importParsed = ref<ParsedMarkdownResume | null>(null)
const importError = ref('')
const importing = ref(false)
const importSummary = computed(() => {
  const content = importParsed.value?.content
  if (!content) return []
  const rows: string[] = []
  if (content.basicInfo && Object.keys(content.basicInfo).length) rows.push(`基本信息 ${Object.keys(content.basicInfo).length} 项`)
  if (content.summary) rows.push('个人简介')
  if (content.workExperience?.length) rows.push(`工作经历 ${content.workExperience.length} 条`)
  if (content.projects?.length) rows.push(`项目经历 ${content.projects.length} 条`)
  if (content.education?.length) rows.push(`教育经历 ${content.education.length} 条`)
  if (content.skills?.length || content.skillCategories?.length) rows.push(`技能 ${(content.skills?.length ?? 0) + (content.skillCategories?.reduce((n, category) => n + (category.skills?.length ?? 0), 0) ?? 0)} 项`)
  if (content.certifications?.length) rows.push(`证书 ${content.certifications.length} 项`)
  if (content.customSections?.length) rows.push(`其他章节 ${content.customSections.length} 节`)
  return rows
})

function pickImportFile() {
  importError.value = ''
  fileInput.value?.click()
}
async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const text = await file.text()
  const fallback = file.name.replace(/\.(md|markdown|txt)$/i, '') || '导入的简历'
  importFileName.value = file.name
  importParsed.value = parseMarkdownResume(text, fallback)
  importError.value = ''
  importOpen.value = true
}
function closeImport() {
  if (importing.value) return
  importOpen.value = false
  importParsed.value = null
  importFileName.value = ''
  importError.value = ''
}
async function confirmImport() {
  const parsed = importParsed.value
  if (!parsed || importing.value || parsed.warnings.length) return
  importing.value = true
  importError.value = ''
  try {
    const response = await createResume({ title: parsed.title, content: parsed.content, changeSummary: '从 Markdown 导入' })
    await library.load()
    if (response.data.id) await library.selectResume(response.data.id)
    importOpen.value = false
    importParsed.value = null
    importFileName.value = ''
  } catch (error) {
    importError.value = error instanceof Error ? error.message : '导入失败'
  } finally {
    importing.value = false
  }
}

onMounted(() => { void library.load() })
</script>

<style scoped>
/* ═══════ 画布：与求职计划同款纯白固定布局 ═══════ */
.resume-library{position:fixed;top:0;right:0;bottom:0;left:92px;display:flex;flex-direction:column;padding:30px 64px 40px;background:#fff;overflow:hidden;z-index:5}

/* ── 头部 ── */
.lib-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}
.lib-head h1{margin:0;font-size:26px;font-weight:700;letter-spacing:-.02em;color:#17181a}
.lib-sub{margin:6px 0 0;color:#989893;font-size:13px}
.lib-actions{display:flex;gap:10px}
.btn-ghost{border:1px solid rgba(28,31,35,.18);border-radius:10px;background:#fff;color:#3c443f;padding:9px 15px;font-size:13px;font-weight:550;cursor:pointer}
.btn-ghost:hover{background:#f4f5f4;color:#17181a}
.btn-solid{border:1px solid #17181a;border-radius:10px;background:#17181a;color:#fff;padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer;transition:background .15s ease-out}
.btn-solid:hover{background:#000}

/* ── 工具栏 ── */
.lib-toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;padding:18px 0 4px;border-bottom:1px solid rgba(28,31,35,.07)}
.filter-pills{display:inline-flex;gap:2px}
.filter-pill{border:1px solid transparent;border-radius:999px;background:none;padding:5px 12px;font-size:12px;font-weight:550;color:#5c625d;cursor:pointer;transition:all .14s ease-out}
.filter-pill:hover{background:#f3f4f3;color:#17181a}
.filter-pill.on{border-color:rgba(22,139,104,.4);background:rgba(22,139,104,.1);color:var(--brand,#168b68);font-weight:650}
.toolbar-search{display:flex;align-items:center;gap:7px;margin-left:auto;border:1px solid rgba(28,31,35,.12);border-radius:9px;background:#fff;padding:6px 11px;color:#a2a29d}
.toolbar-search:focus-within{border-color:rgba(22,139,104,.5)}
.toolbar-search input{border:0;outline:none;background:none;width:220px;font:inherit;font-size:12.5px;color:#23292e}

/* ── 主体 ── */
.lib-body{flex:1;min-height:0;display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:28px;padding-top:16px}
.shelf{min-height:0;overflow-y:auto;padding:4px 4px 24px 0}

/* ── 纸张卡片网格 ── */
.paper-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(228px,1fr));gap:20px}
.paper-card{display:grid;gap:10px;border:0;background:none;padding:0;cursor:pointer;animation:paper-in .34s cubic-bezier(.2,.7,.3,1) both;animation-delay:calc(var(--i,0)*45ms)}
@keyframes paper-in{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:none}}
@media (prefers-reduced-motion: reduce){.paper-card{animation:none}}
.paper-visual{position:relative;aspect-ratio:3/4;display:flex;flex-direction:column;gap:6px;padding:18px 16px;border:1px solid rgba(28,31,35,.1);border-radius:14px;background:linear-gradient(180deg,#fdfdfc,#f7f8f6);overflow:hidden;transition:transform .18s cubic-bezier(.2,.7,.3,1),box-shadow .18s ease-out,border-color .18s ease-out}
.paper-card:hover .paper-visual{transform:translateY(-3px);border-color:rgba(22,139,104,.45);box-shadow:0 14px 34px rgba(16,24,40,.1)}
.paper-card.selected .paper-visual{border-color:var(--brand,#168b68);box-shadow:0 0 0 1px var(--brand,#168b68)}
.paper-kind{position:absolute;top:12px;right:12px;padding:2px 9px;border-radius:999px;font-size:10px;font-weight:700;background:rgba(28,31,35,.06);color:#5c625d}
.paper-card.expression .paper-kind{background:rgba(22,139,104,.12);color:var(--brand,#168b68)}
.paper-face{display:grid;gap:5px;margin-top:14px;min-width:0}
.paper-name{font-size:15px;font-weight:750;color:#17181a;letter-spacing:-.01em;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.paper-role{font-size:11px;color:#a2a29d;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.paper-lines{display:grid;gap:7px;margin-top:10px}
.paper-lines i{display:block;height:5px;border-radius:3px;background:rgba(28,31,35,.07)}
.paper-lines i:nth-child(2){width:84%}
.paper-lines i:nth-child(3){width:62%}
.paper-version{position:absolute;right:12px;bottom:12px;padding:2px 9px;border-radius:999px;background:#fff;border:1px solid rgba(28,31,35,.1);color:#3c443f;font-size:10.5px;font-weight:800;font-style:normal;font-variant-numeric:tabular-nums}
.paper-meta{display:flex;align-items:flex-start;justify-content:space-between;gap:8px;padding:0 2px}
.paper-meta-copy{min-width:0;display:grid;gap:2px}
.paper-meta-copy strong{font-size:13px;font-weight:600;color:#23292e;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.paper-meta-copy small{font-size:11px;color:#a2a29d}
.paper-meta-actions{display:none;gap:2px;flex:0 0 auto}
.paper-card:hover .paper-meta-actions,.paper-card.selected .paper-meta-actions{display:inline-flex}
.paper-meta-actions a,.paper-meta-actions button{border:0;background:none;padding:4px 7px;border-radius:7px;color:#3c443f;font-size:11.5px;font-weight:600;cursor:pointer;text-decoration:none}
.paper-meta-actions a:hover,.paper-meta-actions button:hover{background:#f1f2f1;color:var(--brand,#168b68)}

/* 导入卡 */
.import-card{border:1px dashed rgba(28,31,35,.2);border-radius:14px;min-height:0;place-content:center;justify-items:center;gap:10px;transition:border-color .15s ease-out,color .15s ease-out}
.import-card:hover{border-color:rgba(22,139,104,.5);transform:none;box-shadow:none}
.import-card:hover .import-plus{color:var(--brand,#168b68)}
.import-plus{display:grid;width:52px;height:52px;place-items:center;border-radius:50%;background:#f4f5f4;color:#8a9089}
.import-label{font-size:12.5px;font-weight:600;color:#5c625d}

/* 空态 / 错误态 */
.shelf-state{max-width:420px;margin:60px auto;display:grid;justify-items:center;gap:12px;border:1px dashed rgba(28,31,35,.15);border-radius:16px;padding:44px 32px;color:#989893;text-align:center}
.shelf-state strong{font-size:15px;color:#17181a}
.shelf-state span{font-size:12.5px;line-height:1.7}
.shelf-state.error strong{color:var(--danger)}
.state-solid{border:1px solid #17181a;border-radius:10px;background:#17181a;color:#fff;padding:9px 16px;font-size:13px;font-weight:600;cursor:pointer}
.state-btn{border:1px solid rgba(28,31,35,.18);border-radius:10px;background:#fff;color:#3c443f;padding:8px 14px;font-size:12.5px;cursor:pointer}
.state-btn:hover{background:#f4f5f4}

/* ── 右侧版本面板 ── */
.lib-inspector{min-height:0;overflow-y:auto;border-left:1px solid rgba(28,31,35,.07);padding:6px 0 24px 24px}
.inspector-primary{display:grid;place-items:center;border:1px solid #17181a;border-radius:10px;background:#17181a;color:#fff;padding:10px 15px;font-size:13px;font-weight:600;cursor:pointer;text-decoration:none}
.inspector-primary:hover{background:#000}
.inspector-secondary{border:1px solid rgba(28,31,35,.18);border-radius:10px;background:#fff;color:#3c443f;padding:9px 14px;font-size:12.5px;font-weight:550;cursor:pointer;text-decoration:none}
.inspector-secondary:hover{background:#f4f5f4;color:var(--brand,#168b68)}
.inspector-secondary-row{display:grid;grid-template-columns:1fr 1fr;gap:8px}
.inspector-secondary-row .inspector-secondary{display:grid;place-items:center;text-align:center}
.inspector-collapsed button{border:1px solid rgba(28,31,35,.18);border-radius:10px;background:#fff;color:#3c443f;padding:8px 14px;font-size:12px;cursor:pointer}
.inspector-placeholder{color:#b0b0ab;font-size:12.5px;padding:20px 0}

.file-input{display:none}

/* ── 导入弹窗 ── */
.import-backdrop{position:fixed;inset:0;z-index:50;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.import-dialog{position:relative;width:min(420px,100%);border-radius:14px;background:#fff;padding:20px;box-shadow:0 22px 60px rgba(0,0,0,.35)}
.dialog-close{position:absolute;top:10px;right:12px;border:0;background:none;color:#989893;font-size:18px;cursor:pointer}
.import-dialog>p{margin:0;color:#989893;font-size:12px}
.import-dialog h2{margin:6px 0 4px;font-size:17px;color:#17181a}
.import-file-name{color:#a2a29d;font-size:11.5px}
.import-warning{margin:8px 0 0;color:#ad6800;font-size:12px}
.import-summary{margin:10px 0 0;padding:0;list-style:none;display:grid;gap:4px;color:#3c443f;font-size:12px}
.import-summary li{display:flex;align-items:center;gap:6px}
.import-error{margin:8px 0 0;color:var(--danger);font-size:12.5px}
.import-dialog footer{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
.import-dialog footer button{border:1px solid rgba(28,31,35,.18);border-radius:9px;background:#fff;padding:8px 13px;font:inherit;cursor:pointer}
.import-dialog footer .primary{border-color:#17181a;background:#17181a;color:#fff;font-weight:600}
.import-dialog footer button:disabled{opacity:.55;cursor:default}
</style>
