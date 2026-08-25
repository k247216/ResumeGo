<template>
  <section class="studio" data-test="resume-library-view">
    <!-- ── 顶栏：标题 + 搜索 + 主入口 ── -->
    <header class="studio-topbar" data-test="resume-command-bar">
      <h1 class="topbar-title">简历库</h1>
      <label class="topbar-search">
        <el-icon :size="13"><Search /></el-icon>
        <input
          :value="library.filter.value.keyword"
          type="search"
          placeholder="搜索简历标题"
          data-test="library-search"
          @input="library.filter.value.keyword = ($event.target as HTMLInputElement).value"
        />
      </label>
      <div class="topbar-actions">
        <button type="button" class="topbar-btn" data-test="import-md-header" @click="pickImportFile">导入简历</button>
        <button type="button" class="topbar-btn primary" data-test="create-blank-header" @click="goCreateBlank">新建简历</button>
      </div>
    </header>

    <!-- ── 三栏：资产导航 | 版本工作区 | 检查器 ── -->
    <div class="studio-body" :class="{ 'inspector-open': inspectorOpen }">
      <aside class="nav-pane">
        <ResumeAssetNavigator
          :items="library.visibleItems.value"
          :selected-id="library.selectedResumeId.value"
          :loading="library.loading.value"
          :error="library.errorMessage.value"
          :filter="library.filter.value.kind"
          :archived-count="0"
          @select="onSelectAsset"
          @retry="library.load()"
          @update:filter="setKind"
          @open-archived="setKind('archived')"
        />
      </aside>

      <main class="work-pane">
        <template v-if="library.selectedResume.value">
          <ResumeAssetHeader
            :resume="library.selectedResume.value"
            @rename="onRename"
          />
          <ResumeVersionRail
            :versions="library.versions.value"
            :selected-version-id="library.selectedVersionId.value"
            :current-version-id="library.selectedResume.value.currentVersion?.id ?? null"
            @select-version="onSelectVersion"
          />
          <ResumeCompareToolbar
            :selected-version-no="library.selectedVersion.value?.versionNo ?? null"
            :viewing-current="viewingCurrent"
            :comparing="comparing"
            @update:comparing="comparing = $event"
          />
          <div class="work-canvas">
            <ResumeChangeSummary
              v-if="comparing"
              :changes="chapterChanges"
              :parent-version-no="parentVersionNo"
            />
            <div class="doc-canvas">
              <ResumeDocumentPreview
                :content="viewingContent"
                :changes="chapterChanges"
                :compare-mode="comparing"
                @edit="goEditCurrent"
              />
              <div v-if="comparing" class="compare-legend" data-test="compare-legend">
                <span class="legend-item"><i class="legend-dot modified" aria-hidden="true"></i>内容已更新</span>
                <span class="legend-item"><i class="legend-dot added" aria-hidden="true"></i>内容已新增</span>
                <span class="legend-item"><i class="legend-dot removed" aria-hidden="true"></i>内容已删除</span>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="work-empty" data-test="resume-library-empty">
          <strong>还没有简历</strong>
          <span>新建空白简历或导入 Markdown，开始维护你的第一份简历资产。</span>
          <button type="button" class="btn-solid" data-test="create-blank" @click="goCreateBlank">新建简历</button>
          <button type="button" class="btn-ghost" data-test="import-md-empty" @click="pickImportFile">导入 Markdown</button>
        </div>
      </main>

      <aside class="inspector-pane">
        <ResumeVersionInspector
          v-if="inspectorOpen && library.selectedResume.value"
          :resume="library.selectedResume.value"
          :selected-version="library.selectedVersion.value"
          :current-version-id="library.selectedResume.value.currentVersion?.id ?? null"
          :used-by-targets="usedByTargets"
          :used-by-loading="usedByLoading"
          @open-target="openTarget"
          @fork="openFork"
          @archive="openArchive"
          @close="inspectorOpen = false"
        />
        <button
          v-else
          type="button"
          class="inspector-reopen"
          data-test="inspector-reopen"
          @click="inspectorOpen = true"
        >版本检查器</button>
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
import { Check, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createResume } from '../../api/resume'
import { getJobDescription } from '../../api/job'
import { useResumeLibrary } from '../../composables/useResumeLibrary'
import type { ResumeLibraryKindFilter } from '../../composables/useResumeLibrary'
import ResumeAssetNavigator from '../../components/resume-library/ResumeAssetNavigator.vue'
import ResumeAssetHeader from '../../components/resume-library/ResumeAssetHeader.vue'
import ResumeVersionRail from '../../components/resume-library/ResumeVersionRail.vue'
import ResumeCompareToolbar from '../../components/resume-library/ResumeCompareToolbar.vue'
import ResumeChangeSummary from '../../components/resume-library/ResumeChangeSummary.vue'
import ResumeDocumentPreview from '../../components/resume-library/ResumeDocumentPreview.vue'
import ResumeVersionInspector from '../../components/resume-library/ResumeVersionInspector.vue'
import ResumeForkDialog from '../../components/resume-library/ResumeForkDialog.vue'
import ResumeArchiveDialog from '../../components/resume-library/ResumeArchiveDialog.vue'
import { diffResumeContent, type ResumeChapterChange } from '../../utils/resumeVersionDiff'
import { useTargetsStore } from '../../stores/targets'
import type { ParsedMarkdownResume } from '../../utils/parseMarkdownResume'
import { parseMarkdownResume } from '../../utils/parseMarkdownResume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const library = useResumeLibrary()
const targetsStore = useTargetsStore()
const router = useRouter()

const inspectorOpen = ref(true)
const comparing = ref(false)
watch(() => library.selectedVersionId.value, () => { comparing.value = false })

function setKind(kind: ResumeLibraryKindFilter) {
  library.filter.value.kind = kind
  void library.load()
}

function onSelectAsset(id: number) {
  comparing.value = false
  library.selectResume(id)
}

function onSelectVersion(id: number) {
  library.selectVersion(id)
}

const viewingCurrent = computed(() => {
  const resume = library.selectedResume.value
  const version = library.selectedVersion.value
  return resume != null && version != null && version.id === resume.currentVersion?.id
})

/** 画布渲染选中版本的正文（历史版本只读预览同一来源） */
const viewingContent = computed(() => library.selectedVersion.value?.content ?? null)

/** 选中版本相对父版本的确定性章节变化 */
const chapterChanges = computed<ResumeChapterChange[]>(() => {
  const version = library.selectedVersion.value
  if (!version?.parentVersionId) return []
  const parent = library.versions.value.find((item) => item.id === version.parentVersionId)
  if (!parent) return []
  return diffResumeContent(parent.content, version.content)
})

const parentVersionNo = computed(() => (library.selectedVersion.value?.versionNo ?? 1) - 1)

function goEditCurrent() {
  const resume = library.selectedResume.value
  if (!resume) return
  void router.push(buildResumeEditorLocation({ resumeId: resume.id, versionId: resume.currentVersion?.id }))
}

function goCreateBlank() {
  void router.push(buildResumeEditorLocation({ mode: 'blank' }))
}

function onRename(title: string) {
  const resume = library.selectedResume.value
  if (!resume) return
  library.rename(resume.id, title).then(() => ElMessage.success('已改名')).catch((error) => {
    ElMessage.error(error instanceof Error ? error.message : '改名失败')
  })
}

function openTarget(targetId: number) {
  void router.push({ name: 'targets', query: { targetId: String(targetId) } })
}

// ── fork 流程 ──
const forkOpen = ref(false)
const forkSubmitting = ref(false)
const forkError = ref('')

function openFork() {
  if (!library.selectedResume.value || !library.selectedVersion.value) return
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

// ── 用于：反查引用选中版本的求职目标 ──
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
/* ═══════ Version Studio：资产导航 | 版本工作区 | 检查器 ═══════ */
.studio{position:fixed;top:0;right:0;bottom:0;left:92px;display:flex;flex-direction:column;background:var(--canvas,#fafaf8);overflow:hidden;z-index:5}

.studio-topbar{display:flex;align-items:center;gap:16px;min-height:56px;padding:8px 20px;border-bottom:1px solid var(--border-subtle);background:var(--surface-solid,#fff);flex:0 0 auto}
.topbar-title{margin:0;font-size:15px;font-weight:700;color:var(--ink);letter-spacing:-.01em;white-space:nowrap}
.topbar-search{display:flex;align-items:center;gap:7px;border:1px solid transparent;border-radius:9px;background:var(--bg-subtle);padding:7px 12px;color:var(--muted)}
.topbar-search:focus-within{border-color:var(--brand);box-shadow:0 0 0 2px var(--brand-soft);background:var(--surface-solid,#fff)}
.topbar-search input{border:0;outline:none;background:none;width:240px;font:inherit;font-size:12.5px;color:var(--ink)}
.topbar-actions{display:flex;gap:10px;margin-left:auto}
.topbar-btn{border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);padding:8px 13px;font-size:12.5px;cursor:pointer}
.topbar-btn:hover{background:var(--bg-hover)}
.topbar-btn.primary{border:0;background:var(--brand);color:#fff;font-weight:600}
.topbar-btn.primary:hover{opacity:.92}

.studio-body{flex:1;min-height:0;display:grid;grid-template-columns:264px minmax(0,1fr) 292px}
.studio-body.inspector-open{grid-template-columns:264px minmax(0,1fr) 292px}

.nav-pane{min-height:0;border-right:1px solid var(--border-subtle);background:var(--surface-solid,#fff);display:flex;flex-direction:column}
.nav-empty-actions{display:grid;gap:8px;padding:0 12px 16px}
.btn-solid{border:1px solid var(--brand);border-radius:9px;background:var(--brand);color:#fff;padding:8px 14px;font-size:12.5px;font-weight:600;cursor:pointer}
.btn-ghost{border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);padding:8px 13px;font-size:12px;cursor:pointer}

.work-pane{min-height:0;overflow-y:auto;padding:18px 24px 48px;display:flex;flex-direction:column;gap:14px}
.work-canvas{flex:1;min-height:0;display:flex;gap:16px;align-items:flex-start}
.doc-canvas{flex:1;min-width:0;display:grid;gap:12px}
.compare-legend{display:flex;gap:18px;padding:2px 4px}
.legend-item{display:inline-flex;align-items:center;gap:6px;font-size:11px;color:var(--muted)}
.legend-dot{width:8px;height:8px;border-radius:3px}
.legend-dot.modified{background:rgba(217,119,6,.55)}
.legend-dot.added{background:var(--brand)}
.legend-dot.removed{background:#c25656}
.work-empty{margin:auto;display:grid;justify-items:center;gap:12px;border:1px dashed var(--border-default);border-radius:16px;padding:52px 40px;text-align:center;color:var(--muted)}
.work-empty strong{font-size:15px;color:var(--ink)}
.work-empty span{font-size:12.5px;line-height:1.7;max-width:340px}

.inspector-pane{min-height:0;overflow-y:auto;border-left:1px solid var(--border-subtle);background:var(--surface-solid,#fff);padding:14px 16px 24px}
.inspector-reopen{border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);padding:7px 12px;font-size:12px;cursor:pointer}

.file-input{display:none}

/* ── 响应式：1280 / 1024 两档，不压扁正文 ── */
@media (max-width: 1279px) {
  .studio-body,.studio-body.inspector-open{grid-template-columns:240px minmax(0,1fr)}
  .inspector-pane{display:none}
  .inspector-reopen{display:block}
  .studio-body .inspector-pane{display:none}
  .studio-body.inspector-open .inspector-pane{display:flex;position:absolute;right:0;top:56px;bottom:0;width:300px;z-index:20;box-shadow:-12px 0 32px rgba(16,24,40,.1)}
}
@media (max-width: 1023px) {
  .studio-body,.studio-body.inspector-open{grid-template-columns:minmax(0,1fr)}
  .nav-pane{display:none}
  .studio-body.inspector-open .inspector-pane{width:min(340px,90vw)}
}
</style>
