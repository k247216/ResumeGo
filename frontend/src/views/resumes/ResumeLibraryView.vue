<template>
  <section class="resume-library" data-test="resume-library-view">
    <header class="command-bar" data-test="resume-command-bar">
      <div class="bar-identity">
        <h1 class="bar-title"><span class="bar-title-icon" aria-hidden="true"><el-icon :size="17"><Document /></el-icon></span><span>简历库</span></h1>
      </div>
      <div class="bar-search-wrap">
        <el-icon class="bar-search-icon" :size="14"><Search /></el-icon>
        <input
          :value="library.filter.value.keyword"
          type="search"
          class="bar-search"
          placeholder="搜索简历名称"
          data-test="library-search"
          @input="library.filter.value.keyword = ($event.target as HTMLInputElement).value"
        />
      </div>
      <div class="bar-actions">
        <button type="button" class="bar-btn" data-test="import-md-header" @click="pickImportFile"><el-icon><Upload /></el-icon><span>导入 Markdown</span></button>
        <button type="button" class="bar-btn primary" data-test="create-blank-header" @click="goCreateBlank">创建空白简历</button>
      </div>
    </header>

    <div class="library-body">
      <aside class="asset-pane">
        <div class="pane-head">
          <span class="pane-title">简历资产</span>
          <span class="pane-count">{{ library.items.value.length }}</span>
        </div>
        <div class="kind-tabs" role="tablist" aria-label="简历种类过滤">
          <button
            v-for="option in KIND_OPTIONS"
            :key="option.value"
            type="button"
            class="kind-tab"
            :class="{ on: library.filter.value.kind === option.value }"
            :data-test="`filter-${option.value}`"
            @click="setKind(option.value)"
          >{{ option.label }}</button>
        </div>
        <div class="pane-scroll">
          <ResumeAssetList
            :items="library.visibleItems.value"
            :selected-id="library.selectedResumeId.value"
            :loading="library.loading.value"
            :error="library.errorMessage.value"
            @select="library.selectResume($event)"
            @retry="library.load()"
            @create-blank="goCreateBlank"
            @import="pickImportFile"
          />
        </div>
      </aside>

      <main class="detail-pane">
        <div class="pane-head">
          <span class="pane-title">详情</span>
        </div>
        <div class="pane-scroll detail-scroll">
          <ResumeAssetWorkspace
            :resume="library.selectedResume.value"
            :version="library.selectedVersion.value"
            @fork="openFork"
          />
        </div>
      </main>

      <aside class="inspector-pane">
        <ResumeVersionInspector
          v-if="inspectorOpen"
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
        />
        <button
          v-else
          type="button"
          class="inspector-reopen"
          data-test="inspector-reopen"
          @click="inspectorOpen = true"
        >打开版本与引用</button>
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
import { Check, Document, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createResume } from '../../api/resume'
import { getJobDescription } from '../../api/job'
import { useResumeLibrary } from '../../composables/useResumeLibrary'
import type { ResumeLibraryKindFilter } from '../../composables/useResumeLibrary'
import ResumeAssetList from '../../components/resume-library/ResumeAssetList.vue'
import ResumeAssetWorkspace from '../../components/resume-library/ResumeAssetWorkspace.vue'
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
/* ═══════ 命令栏：与知识库同款 ═══════ */
.resume-library{display:flex;flex-direction:column;height:100vh;min-height:0;background:var(--canvas,#fafaf8)}
.command-bar{display:flex;align-items:center;gap:18px;min-height:58px;padding:8px 18px;border-bottom:1px solid var(--border-subtle);background:var(--surface-solid,#fff)}
.bar-identity{display:flex;align-items:center;gap:10px}
.bar-title{display:inline-flex;align-items:center;gap:10px;margin:0;font-size:22px;font-weight:650;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.bar-title-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px;background:var(--brand-soft);color:var(--brand)}
.bar-search{flex:1;min-width:0;max-width:300px;padding:7px 12px 7px 32px;border:1px solid transparent;border-radius:9px;background:var(--bg-subtle);color:var(--ink);font-size:12.5px;transition:border-color .15s ease,box-shadow .15s ease,background .15s ease;appearance:none;-webkit-appearance:none}
.bar-search:focus{outline:0;border-color:var(--brand);box-shadow:0 0 0 2px var(--brand-soft);background:var(--surface-solid,#fff)}
.bar-search-wrap{position:relative;display:flex;align-items:center;flex:none;margin-left:auto}
.bar-search-icon{position:absolute;left:10px;color:var(--muted);pointer-events:none}
.bar-actions{display:flex;align-items:center;gap:10px}
.bar-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 13px;border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);font-size:13px;cursor:pointer;white-space:nowrap}
.bar-btn:hover{background:var(--bg-hover)}
.bar-btn.primary{border:0;background:var(--brand);color:#fff;font-weight:600}
.bar-btn.primary:hover{opacity:.92}

/* ═══════ 三面板 ═══════ */
.library-body{flex:1;min-height:0;display:grid;grid-template-columns:320px minmax(0,1fr) 340px}
.asset-pane,.inspector-pane{min-height:0;display:flex;flex-direction:column;border-right:1px solid var(--border-subtle);background:var(--surface-solid,#fff)}
.inspector-pane{border-right:0;border-left:1px solid var(--border-subtle)}
.detail-pane{min-height:0;display:flex;flex-direction:column;background:var(--canvas,#fafaf8)}

.pane-head{display:flex;align-items:center;justify-content:space-between;padding:12px 16px 8px;flex:0 0 auto}
.pane-title{font-size:11px;font-weight:650;letter-spacing:.07em;color:var(--muted)}
.pane-count{font-size:11px;color:var(--muted);font-variant-numeric:tabular-nums}
.pane-scroll{flex:1;min-height:0;overflow-y:auto;padding:4px 12px 24px}
.detail-scroll{padding:20px 26px 48px}

/* 种类过滤：分段控制 */
.kind-tabs{display:inline-flex;gap:2px;margin:0 12px 10px;border:1px solid var(--border-subtle);border-radius:9px;background:var(--bg-subtle);padding:3px;flex:0 0 auto}
.kind-tab{border:0;border-radius:7px;background:none;padding:5px 11px;font:inherit;font-size:11.5px;font-weight:600;color:var(--muted);cursor:pointer;transition:all .14s ease-out}
.kind-tab:hover{color:var(--ink)}
.kind-tab.on{background:var(--surface-solid,#fff);color:var(--ink);box-shadow:0 1px 3px rgba(16,24,40,.1)}

.inspector-reopen{margin:16px;align-self:flex-start;border:1px solid var(--border-default);border-radius:var(--radius-control,10px);background:var(--bg-surface);color:var(--copy);padding:7px 13px;font-size:12px;cursor:pointer}
.inspector-reopen:hover{background:var(--bg-hover)}

.file-input{display:none}

/* ═══════ 导入弹窗 ═══════ */
.import-backdrop{position:fixed;inset:0;z-index:50;display:grid;place-items:center;background:rgba(7,8,8,.5);padding:24px}
.import-dialog{position:relative;width:min(420px,100%);border-radius:14px;background:var(--surface-solid,#fff);padding:20px;box-shadow:0 22px 60px rgba(0,0,0,.35)}
.dialog-close{position:absolute;top:10px;right:12px;border:0;background:none;color:var(--muted);font-size:18px;cursor:pointer}
.import-dialog>p{margin:0;color:var(--muted);font-size:12px}
.import-dialog h2{margin:6px 0 4px;font-size:17px;color:var(--ink)}
.import-file-name{color:var(--muted);font-size:11.5px}
.import-warning{margin:8px 0 0;color:var(--warning,#ad6800);font-size:12px}
.import-summary{margin:10px 0 0;padding:0;list-style:none;display:grid;gap:4px;color:var(--copy);font-size:12px}
.import-summary li{display:flex;align-items:center;gap:6px}
.import-error{margin:8px 0 0;color:var(--danger);font-size:12.5px}
.import-dialog footer{display:flex;justify-content:flex-end;gap:8px;margin-top:16px}
.import-dialog footer button{border:1px solid var(--border-default);border-radius:9px;background:var(--bg-surface);padding:8px 13px;font:inherit;cursor:pointer}
.import-dialog footer .primary{border-color:var(--brand);background:var(--brand);color:#fff;font-weight:600}
.import-dialog footer button:disabled{opacity:.55;cursor:default}

@media (max-width: 1099px) {
  .library-body{grid-template-columns:280px minmax(0,1fr)}
  .inspector-pane{display:none}
}
</style>
