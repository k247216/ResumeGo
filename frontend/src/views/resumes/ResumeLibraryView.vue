<template>
  <section class="studio" data-test="resume-library-view">
    <!-- ── 顶栏：搜索 + 本地文件动作 ── -->
    <header class="studio-topbar" data-test="resume-command-bar">
      <label class="topbar-search">
        <el-icon :size="13"><Search /></el-icon>
        <input
          :value="library.filter.value.keyword"
          type="search"
          placeholder="搜索简历、版本或岗位..."
          data-test="library-search"
          @input="library.filter.value.keyword = ($event.target as HTMLInputElement).value"
        />
        <kbd>⌘ K</kbd>
      </label>
      <div class="topbar-actions">
        <button type="button" class="topbar-btn split" data-test="import-md-header" @click="pickImportFile">
          <el-icon :size="14"><Upload /></el-icon><span>导入简历</span><el-icon :size="11"><ArrowDown /></el-icon>
        </button>
        <button type="button" class="topbar-btn primary split" data-test="create-blank-header" @click="goCreateBlank">
          <el-icon :size="14"><Plus /></el-icon><span>新建简历</span><el-icon :size="11"><ArrowDown /></el-icon>
        </button>
        <span class="topbar-sep" aria-hidden="true"></span>
        <button type="button" class="topbar-icon-btn history-trigger" data-test="open-history" aria-label="历史活动" title="历史活动" @click="historyOpen = true"><el-icon :size="16"><Clock /></el-icon></button>
        <button type="button" class="topbar-icon-btn" data-test="open-help" aria-label="使用帮助" title="使用帮助" @click="helpOpen = true"><el-icon :size="16"><QuestionFilled /></el-icon></button>
        <RouterLink class="topbar-icon-btn" aria-label="设置" title="设置" :to="{ name: 'settings' }"><el-icon :size="16"><Setting /></el-icon></RouterLink>
      </div>
    </header>

    <!-- ── 三栏：资产导航 | 版本工作区 | 检查器 ── -->
    <div class="studio-body inspector-open">
      <aside class="nav-pane">
        <ResumeAssetNavigator
          :items="library.visibleItems.value"
          :selected-id="library.selectedResumeId.value"
          :loading="library.loading.value"
          :error="library.errorMessage.value"
          :filter="library.filter.value.kind"
          :archived-count="library.filter.value.kind === 'archived' ? library.visibleItems.value.length : 0"
          @select="onSelectAsset"
          @retry="library.load()"
          @update:filter="setKind"
          @open-archived="setKind('archived')"
          @clear-trash="trashClearOpen = true"
        />
      </aside>

      <main class="work-pane">
        <template v-if="library.selectedResume.value">
          <ResumeAssetHeader
            :resume="library.selectedResume.value"
            @rename="onRename"
            @fork="openFork"
            @archive="openArchive"
          />
          <ResumeVersionRail
            :versions="library.versions.value"
            :selected-version-id="library.selectedVersionId.value"
            :current-version-id="library.selectedResume.value.currentVersion?.id ?? null"
            @select-version="onSelectVersion"
          />
          <div class="work-toolbar">
            <ResumeCompareToolbar
              :selected-version-no="library.selectedVersion.value?.versionNo ?? null"
              :viewing-current="viewingCurrent"
              :comparing="comparing"
              @update:comparing="comparing = $event"
            />
          </div>
          <p v-if="library.versionError.value" class="data-warning" data-test="version-load-error">
            版本历史暂时无法完整读取，当前只显示可用版本。
            <button type="button" @click="library.selectResume(library.selectedResumeId.value)">重新加载</button>
          </p>
          <div class="work-canvas">
            <div class="change-column">
              <ResumeChangeSummary
                :changes="chapterChanges"
                :parent-version-no="parentVersionNo"
              />
              <div class="compare-legend" data-test="compare-legend">
                <span class="legend-item"><i class="legend-dot modified" aria-hidden="true"></i>内容已更新</span>
                <span class="legend-item"><i class="legend-dot added" aria-hidden="true"></i>内容已新增</span>
                <span class="legend-item"><i class="legend-dot removed" aria-hidden="true"></i>内容已删除</span>
              </div>
            </div>
            <div class="doc-canvas">
              <ResumeDocumentPreview
                :content="viewingContent"
                :changes="chapterChanges"
                :compare-mode="comparing"
                :template-style="templateStyle"
                :scale="0.7"
                @edit="goEditCurrent"
              />
            </div>
          </div>
        </template>
        <div v-else class="work-empty" :class="`empty-kind-${library.filter.value.kind}`" data-test="resume-library-empty">
          <span class="empty-mark" aria-hidden="true">{{ emptyState.mark }}</span>
          <strong>{{ emptyState.title }}</strong>
          <span>{{ emptyState.description }}</span>
          <div v-if="emptyState.showCreate" class="empty-actions">
            <button type="button" class="btn-solid" data-test="create-blank" @click="goCreateBlank">新建简历</button>
            <button type="button" class="btn-ghost" data-test="import-md-empty" @click="pickImportFile">导入 Markdown</button>
          </div>
          <button v-else type="button" class="btn-ghost" data-test="empty-back-to-all" @click="setKind('all')">查看全部简历</button>
        </div>
      </main>

      <aside class="inspector-pane">
        <ResumeVersionInspector
          v-if="library.selectedResume.value"
          :resume="library.selectedResume.value"
          :versions="library.versions.value"
          :selected-version="library.selectedVersion.value"
          :current-version-id="library.selectedResume.value.currentVersion?.id ?? null"
          :used-by-targets="usedByTargets"
          :used-by-loading="usedByLoading"
          :available-targets="availableTargets"
          @open-target="openTarget"
          @bind-target="bindTarget"
          @unbind-target="unbindTarget"
          @update-summary="updateVersionSummary"
          @fork="openFork"
          @archive="openArchive"
        />
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
      :archived="Boolean(library.selectedResume.value?.archivedAt)"
      :submitting="archiveSubmitting"
      :error="archiveError"
      @confirm="confirmArchive"
      @cancel="closeArchive"
    />

    <div v-if="trashClearOpen" class="studio-modal-backdrop" role="presentation" @click.self="closeTrashClear">
      <section class="studio-modal trash-clear-modal" role="dialog" aria-modal="true" aria-labelledby="trash-clear-title" data-test="trash-clear-dialog">
        <header class="studio-modal-head"><div><span>回收站</span><h2 id="trash-clear-title">清空回收站</h2></div><button type="button" aria-label="关闭清空回收站" @click="closeTrashClear">×</button></header>
        <p class="trash-clear-copy">清空后，这些归档简历将从回收站移除，之后不能从简历库恢复。此操作只影响当前回收站内容。</p>
        <footer class="trash-clear-actions"><button type="button" class="btn-ghost" @click="closeTrashClear">取消</button><button type="button" class="trash-clear-confirm" data-test="trash-clear-confirm" :disabled="trashClearSubmitting" @click="clearTrash">{{ trashClearSubmitting ? '清空中…' : '确认清空' }}</button></footer>
      </section>
    </div>

    <div v-if="historyOpen" class="studio-modal-backdrop" role="presentation" @click.self="historyOpen = false">
      <section class="studio-modal history-modal" role="dialog" aria-modal="true" aria-labelledby="history-title" data-test="history-dialog">
        <header class="studio-modal-head"><div><span>版本记录</span><h2 id="history-title">全部历史修改</h2><p class="history-subtitle">从初始简历到当前版本，每次变化都保留在本机。</p></div><button type="button" aria-label="关闭历史记录" @click="historyOpen = false">×</button></header>
        <div v-if="historyEvents.length" class="history-summary-card">
          <span class="history-summary-icon" :class="historyEvents[0].status" aria-hidden="true"></span>
          <div><small>最近一次活动</small><strong>{{ historyEvents[0].label }}</strong><span>{{ historyEvents[0].time }}</span></div>
        </div>
        <ol v-if="historyEvents.length" class="history-timeline">
          <li v-for="(event, index) in historyEvents" :key="event.key" class="history-event" :class="{ latest: event.latest }" :style="{ '--history-delay': `${index * 42}ms` }"><span class="history-node" :class="event.status" aria-hidden="true"></span><div class="history-event-copy"><strong>{{ event.label }}</strong><small>{{ event.time }}</small><p v-if="event.modules">{{ event.modules }}</p></div></li>
        </ol>
        <p v-else class="studio-modal-empty">当前简历还没有可展示的版本记录。</p>
      </section>
    </div>

    <div v-if="helpOpen" class="studio-modal-backdrop" role="presentation" @click.self="helpOpen = false">
      <section class="studio-modal help-modal" role="dialog" aria-modal="true" aria-labelledby="help-title" data-test="help-dialog">
        <header class="studio-modal-head"><div><span>简历库指南</span><h2 id="help-title">如何维护你的简历资产</h2></div><button type="button" aria-label="关闭使用帮助" @click="helpOpen = false">×</button></header>
        <div class="help-steps"><article><b>01</b><div><strong>选择资产</strong><p>基础简历保存你的长期经历；岗位版本用于一次具体求职表达。</p></div></article><article><b>02</b><div><strong>查看变化</strong><p>选择版本后，左侧更新摘要和正文中的彩色标记会说明新增、修改与移除。</p></div></article><article><b>03</b><div><strong>进入编辑台</strong><p>只编辑当前版本。历史版本保持只读，需要时可以创建副本再继续修改。</p></div></article><article><b>04</b><div><strong>绑定求职目标</strong><p>在右侧绑定状态中选择目标，让面试与投递流程使用明确的简历版本。</p></div></article></div>
      </section>
    </div>

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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowDown, Check, Clock, Plus, QuestionFilled, Search, Setting, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createResume, updateResumeVersionSummary } from '../../api/resume'
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
import { TARGET_STAGE_LABELS } from '../../types/project'
import type { ParsedMarkdownResume } from '../../utils/parseMarkdownResume'
import { parseMarkdownResume } from '../../utils/parseMarkdownResume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'
import { cloneResumeTemplate, readResumeTemplate } from '../../utils/resumeTemplate'

const library = useResumeLibrary()
const targetsStore = useTargetsStore()
const router = useRouter()
const templateStyle = computed(() => readResumeTemplate(library.selectedResume.value?.id))

const comparing = ref(true)
const historyOpen = ref(false)
const helpOpen = ref(false)
watch(() => library.selectedVersionId.value, () => { comparing.value = true })

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

const emptyState = computed(() => {
  const kind = library.filter.value.kind
  if (kind === 'archived') return {
    mark: '↺',
    title: '回收站为空',
    description: '归档后的简历会出现在这里，可恢复或清空。',
    showCreate: false,
  }
  if (kind === 'expression') return {
    mark: '◇',
    title: '还没有岗位版本',
    description: '先选择一份基础简历，再创建针对求职目标的岗位表达版本。',
    showCreate: false,
  }
  if (kind === 'favorites') return {
    mark: '☆',
    title: '还没有收藏的简历',
    description: '在简历标题旁点亮星标，常用版本会集中出现在这里。',
    showCreate: false,
  }
  if (kind === 'general') return {
    mark: '□',
    title: '还没有基础简历',
    description: '基础简历用于沉淀长期经历，之后可以从任意版本创建岗位表达。',
    showCreate: true,
  }
  return {
    mark: '□',
    title: '还没有简历',
    description: '新建空白简历或导入 Markdown，开始维护你的第一份简历资产。',
    showCreate: true,
  }
})

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

const availableTargets = computed(() => targetsStore.targets.map((target) => ({
  targetId: target.id,
  label: target.name || '求职目标',
  companyName: (target.name || '').split('·')[0]?.trim() || target.name,
  stageLabel: TARGET_STAGE_LABELS[target.stage],
  targetRole: target.targetRole,
  resumeVersionId: target.resumeVersionId,
})))

const historyEvents = computed(() => {
  const profile = (() => {
    try {
      const raw = localStorage.getItem('resumego:local-profile')
      const parsed = raw ? JSON.parse(raw) as { name?: unknown } : null
      return typeof parsed?.name === 'string' && parsed.name.trim() ? parsed.name.trim() : '本地用户'
    } catch { return '本地用户' }
  })()
  return [...library.versions.value].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt))).map((version) => {
    const parent = library.versions.value.find((item) => item.id === version.parentVersionId)
    const modules = parent ? diffResumeContent(parent.content, version.content).map((change) => change.chapterLabel).join('、') : ''
    const latest = version.id === library.selectedResume.value?.currentVersion?.id
    const status = latest ? 'current' : version.createdByType === 'fork' ? 'fork' : parent ? 'updated' : 'created'
    return {
      key: version.id,
      label: `${profile} ${parent ? `更新至 V${version.versionNo}` : `创建 V${version.versionNo}`}`,
      time: formatHistoryTime(version.createdAt),
      modules: modules ? `涉及：${modules}` : '',
      latest,
      status,
    }
  })
})

function formatHistoryTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '时间未知' : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function updateVersionSummary(versionId: number, summary: string) {
  try {
    const response = await updateResumeVersionSummary(versionId, summary)
    library.versions.value = library.versions.value.map((version) => (
      version.id === versionId ? response.data : version
    ))
    ElMessage.success(summary ? '版本说明已更新' : '版本说明已清空')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新版本说明失败')
  }
}

async function bindTarget(targetId: number) {
  const version = library.selectedVersion.value
  const target = targetsStore.targets.find((item) => item.id === targetId)
  if (!version || !target) return
  try {
    await targetsStore.updateLinks(targetId, { jobDescriptionId: target.jobDescriptionId, resumeVersionId: version.id })
    ElMessage.success(`已绑定到「${target.name}」`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '绑定求职目标失败')
  }
}

async function unbindTarget(targetId: number) {
  const target = targetsStore.targets.find((item) => item.id === targetId)
  if (!target) return
  try {
    await targetsStore.updateLinks(targetId, { jobDescriptionId: target.jobDescriptionId, resumeVersionId: null })
    ElMessage.success(`已解除「${target.name}」的简历绑定`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '解除简历绑定失败')
  }
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
  const sourceResumeId = library.selectedResume.value?.id
  if (!version || sourceResumeId == null || forkSubmitting.value) return
  forkSubmitting.value = true
  forkError.value = ''
  try {
    const sourceTemplate = readResumeTemplate(sourceResumeId)
    const forked = await library.fork(version.id, title)
    // 副本初始外观与来源一致，但写入自己的资产级键，之后切换模板互不影响。
    cloneResumeTemplate(sourceResumeId, forked.id, sourceTemplate)
    forkOpen.value = false
    ElMessage.success(`已创建副本「${forked.title}」`)
  } catch (error) {
    forkError.value = error instanceof Error ? error.message : '创建副本失败'
  } finally {
    forkSubmitting.value = false
  }
}

// ── 删除到回收站 / 恢复流程（后端仍使用可恢复的软删除字段） ──
const archiveOpen = ref(false)
const archiveSubmitting = ref(false)
const archiveError = ref('')
const trashClearOpen = ref(false)
const trashClearSubmitting = ref(false)

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
  const restoring = Boolean(resume.archivedAt)
  archiveSubmitting.value = true
  archiveError.value = ''
  try {
    if (restoring) await library.restore(resume.id)
    else await library.archive(resume.id)
    archiveOpen.value = false
    ElMessage.success(restoring ? '简历已恢复' : '简历已归档到回收站')
  } catch (error) {
    archiveError.value = error instanceof Error ? error.message : (restoring ? '恢复失败' : '删除失败')
  } finally {
    archiveSubmitting.value = false
  }
}

function closeTrashClear() {
  if (trashClearSubmitting.value) return
  trashClearOpen.value = false
}

async function clearTrash() {
  if (trashClearSubmitting.value || library.filter.value.kind !== 'archived') return
  const archivedItems = [...library.visibleItems.value]
  if (!archivedItems.length) {
    closeTrashClear()
    return
  }
  trashClearSubmitting.value = true
  try {
    for (const item of archivedItems) await library.remove(item.id)
    trashClearOpen.value = false
    ElMessage.success('回收站已清空')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清空回收站失败')
  } finally {
    trashClearSubmitting.value = false
  }
}

// ── 用于：反查引用选中版本的求职目标 ──
interface UsedByTarget { targetId: number; jobDescriptionId: number | null; label: string }
const usedByTargets = ref<UsedByTarget[]>([])
const usedByLoading = ref(false)
const jobLabelCache = ref(new Map<number, string>())
const usedByKey = computed(() => `${library.selectedResumeId.value}:${library.versions.value.map((v) => v.id).join(',')}:${targetsStore.targets.map((target) => `${target.id}-${target.resumeVersionId ?? ''}`).join(',')}`)

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

function onFavoriteChanged() {
  library.refreshFavorites()
  if (library.filter.value.kind !== 'favorites') return
  const favoriteItems = library.visibleItems.value
  if (!favoriteItems.some((resume) => resume.id === library.selectedResumeId.value)) {
    void library.selectResume(favoriteItems[0]?.id ?? null)
  }
}

onMounted(async () => {
  window.addEventListener('resumego:resume-favorite-changed', onFavoriteChanged)
  await library.load()
  const query = (router.currentRoute?.value?.query ?? {}) as Record<string, unknown>
  const resumeId = Number(Array.isArray(query.resumeId) ? query.resumeId[0] : query.resumeId)
  if (Number.isSafeInteger(resumeId) && resumeId > 0 && library.resumes.value.some((resume) => resume.id === resumeId)) {
    await library.selectResume(resumeId)
  }
})

onBeforeUnmount(() => window.removeEventListener('resumego:resume-favorite-changed', onFavoriteChanged))
</script>

<style scoped>
/* ═══════ Version Studio：资产导航 | 版本工作区 | 检查器 ═══════ */
.studio{position:fixed;top:0;right:0;bottom:0;left:92px;display:flex;flex-direction:column;background:var(--surface-solid,#fff);overflow:hidden;z-index:5}

.studio-topbar{display:flex;align-items:center;gap:12px;min-height:48px;padding:4px 14px;border-bottom:1px solid var(--border-subtle);background:var(--surface-solid,#fff);flex:0 0 auto}
.topbar-search{display:flex;align-items:center;gap:8px;flex:0 1 320px;border:1px solid var(--border-subtle);border-radius:8px;background:#fff;padding:6px 10px;color:var(--muted);box-shadow:0 1px 2px rgba(16,24,40,.04)}
.topbar-search:focus-within{border-color:var(--brand);box-shadow:0 0 0 2px var(--brand-soft);background:var(--surface-solid,#fff)}
.topbar-search input{border:0;outline:none;background:none;width:100%;min-width:0;font:inherit;font-size:12.5px;color:var(--ink)}
.topbar-search kbd{flex:0 0 auto;border:1px solid var(--border-subtle);border-radius:5px;background:var(--bg-subtle,#f6f7f7);padding:2px 6px;color:var(--muted);font-size:10px;line-height:1.2}
.topbar-actions{display:flex;gap:8px;margin-left:auto}
.topbar-btn{display:inline-flex;align-items:center;justify-content:center;gap:7px;border:1px solid var(--border-default);border-radius:8px;background:transparent;color:var(--copy);padding:6px 10px;font-size:12px;cursor:pointer;min-width:92px}
.topbar-btn:hover{background:var(--bg-hover)}
.topbar-btn.primary{border:1px solid #17181a;background:#17181a;color:#fff;font-weight:600;min-width:104px}
.topbar-btn.primary:hover{opacity:.92}
.topbar-btn.split{white-space:nowrap}
.topbar-sep{width:1px;height:20px;background:var(--border-subtle);margin:0 2px}
.topbar-icon-btn{display:grid;place-items:center;width:30px;height:30px;border:0;border-radius:7px;background:transparent;color:var(--copy);cursor:pointer;text-decoration:none}
.topbar-icon-btn:hover{background:var(--bg-hover);color:var(--ink)}
.history-trigger{position:relative}

.studio-body{flex:1;min-height:0;display:grid;grid-template-columns:196px minmax(0,1fr) clamp(270px,21vw,300px);overflow:hidden}
.studio-body.inspector-open{grid-template-columns:196px minmax(0,1fr) clamp(270px,21vw,300px)}

.nav-pane{min-height:0;border-right:1px solid var(--border-subtle);background:var(--surface-solid,#fff);display:flex;flex-direction:column}
.nav-empty-actions{display:grid;gap:8px;padding:0 12px 16px}
.btn-solid{border:1px solid #17181a;border-radius:9px;background:#17181a;color:#fff;padding:8px 14px;font-size:12.5px;font-weight:600;cursor:pointer}
.btn-ghost{border:1px solid var(--border-default);border-radius:9px;background:transparent;color:var(--copy);padding:8px 13px;font-size:12px;cursor:pointer}

.work-pane{min-height:0;overflow-y:auto;overflow-x:hidden;scrollbar-gutter:stable;padding:9px 22px 28px;display:flex;flex-direction:column;gap:6px;background:var(--surface-solid,#fff)}
.work-pane::-webkit-scrollbar{width:10px}
.work-pane::-webkit-scrollbar-track{background:transparent}
.work-pane::-webkit-scrollbar-thumb{border:3px solid transparent;border-radius:999px;background:rgba(28,31,35,.18);background-clip:padding-box}
.work-pane::-webkit-scrollbar-thumb:hover{background:rgba(28,31,35,.34);background-clip:padding-box}
.work-toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;flex:0 0 auto}
.work-canvas{flex:1;min-height:0;display:grid;grid-template-columns:minmax(132px,148px) minmax(0,1fr);column-gap:18px;align-items:stretch;margin-top:0}
.change-column{min-width:0;min-height:0;height:100%;display:flex;flex-direction:column;border-right:1px solid var(--border-subtle);padding-right:18px;box-sizing:border-box}
.doc-canvas{position:relative;width:100%;height:100%;min-width:0;min-height:0;overflow:hidden;display:block;margin-top:0}
/* 更新摘要与正文之间用一条轻分隔线建立阅读顺序；缩放控件留在上方工具栏，不参与正文网格。 */
.work-canvas .change-summary{position:sticky;top:0;align-self:start;border:0!important;box-shadow:none!important;background:transparent!important}
.doc-canvas :deep(.editor-preview-panel),
.doc-canvas :deep(.paper-viewport){background:transparent!important}
.doc-canvas :deep(.a4-paper){border:0!important;box-shadow:none!important}
.compare-legend{display:grid;gap:8px;margin-top:auto;padding:16px 0 2px}
.legend-item{display:inline-flex;align-items:center;gap:7px;font-size:10.5px;color:var(--copy);white-space:nowrap}
.legend-dot{width:8px;height:8px;border-radius:3px}
.legend-dot.modified{background:rgba(217,119,6,.55)}
.legend-dot.added{background:var(--brand)}
.legend-dot.removed{background:#c25656}
.work-empty{margin:auto;display:grid;justify-items:center;gap:12px;max-width:420px;border:1px solid var(--border-subtle);border-radius:16px;padding:52px 40px;text-align:center;color:var(--muted);background:linear-gradient(180deg,var(--surface-solid,#fff),var(--bg-subtle,#f7f8f8))}
.empty-mark{display:grid;place-items:center;width:42px;height:42px;border:1px solid var(--border-default);border-radius:13px;background:var(--surface-solid,#fff);color:var(--copy);font-size:21px;font-weight:650}
.work-empty strong{font-size:15px;color:var(--ink)}
.work-empty span{font-size:12.5px;line-height:1.7;max-width:340px}
.work-empty .empty-mark{font-size:21px;line-height:1;max-width:none}
.empty-actions{display:flex;align-items:center;gap:9px}
.data-warning{display:flex;align-items:center;gap:8px;margin:0;color:#9a5b00;font-size:11.5px;line-height:1.5}
.data-warning button{border:0;background:none;color:var(--brand);font:inherit;font-weight:650;cursor:pointer;padding:0}

.inspector-pane{min-height:0;overflow-y:auto;border-left:1px solid var(--border-subtle);background:var(--surface-solid,#fff);padding:12px 18px 20px}

.file-input{display:none}

/* ── 响应式：1280 / 1024 两档，不压扁正文 ── */
@media (max-width: 1279px) {
  .studio-body,.studio-body.inspector-open{grid-template-columns:188px minmax(0,1fr) 270px}
}
@media (max-width: 1023px) {
  .studio-body,.studio-body.inspector-open{grid-template-columns:188px minmax(0,1fr) 240px}
  .work-pane{padding-left:16px;padding-right:16px}
}
@media (max-width: 760px) {
  .studio{left:78px}
  .studio-topbar{gap:8px;padding-left:10px;padding-right:10px}
  .topbar-search{flex:1 1 auto;min-width:0}
  .topbar-search kbd,.topbar-btn span,.topbar-btn .el-icon:last-child,.topbar-sep{display:none}
  .topbar-btn{min-width:30px;width:30px;padding:0}
  .studio-body,.studio-body.inspector-open{grid-template-columns:156px minmax(0,1fr)}
  .inspector-pane{display:none}
  .work-pane{padding-left:12px;padding-right:12px}
  .work-canvas{grid-template-columns:96px minmax(0,1fr);column-gap:10px}
  .change-column{padding-right:10px}
  .change-summary{width:96px}
}

.studio-modal-backdrop{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:28px;background:rgba(15,18,18,.28);backdrop-filter:blur(5px)}
.studio-modal{width:min(560px,calc(100vw - 48px));max-height:min(720px,calc(100vh - 56px));overflow:auto;border:1px solid var(--border-subtle);border-radius:16px;background:var(--surface-solid,#fff);box-shadow:0 24px 70px rgba(0,0,0,.18);padding:22px;color:var(--ink)}
.studio-modal-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.studio-modal-head span{color:var(--muted);font-size:10px;font-weight:700;letter-spacing:.1em}.studio-modal-head h2{margin:5px 0 0;font-size:21px;letter-spacing:-.02em}.studio-modal-head button{border:0;background:none;color:var(--muted);font-size:24px;line-height:1;cursor:pointer}
.history-subtitle{margin:7px 0 0;color:var(--muted);font-size:11px;line-height:1.5}
.trash-clear-modal{width:min(430px,calc(100vw - 48px))}.trash-clear-copy{margin:22px 0 0;color:var(--copy);font-size:12px;line-height:1.7}.trash-clear-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:22px}.trash-clear-confirm{border:1px solid var(--danger,#b84b42);border-radius:8px;background:var(--danger,#b84b42);color:#fff;padding:8px 13px;font-size:11.5px;font-weight:650;cursor:pointer}.trash-clear-confirm:disabled{opacity:.55;cursor:default}
.history-summary-card{display:grid;grid-template-columns:26px minmax(0,1fr);align-items:center;gap:10px;margin-top:20px;border:1px solid var(--border-subtle);border-radius:12px;background:var(--bg-subtle);padding:10px 12px}.history-summary-icon{display:block;width:8px;height:8px;margin-left:9px;border-radius:50%;background:#a7aaa7}.history-summary-icon.current{background:var(--brand)}.history-summary-icon.fork,.history-summary-icon.updated{background:#a7aaa7}.history-summary-card div{display:grid;gap:2px;min-width:0}.history-summary-card small,.history-summary-card span{color:var(--muted);font-size:10px}.history-summary-card strong{overflow:hidden;color:var(--copy);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.history-timeline{position:relative;display:grid;gap:0;margin:22px 0 2px;padding:0;list-style:none}.history-timeline::before{content:'';position:absolute;left:4px;top:8px;bottom:12px;width:1px;border-radius:999px;background:var(--border-subtle)}.history-event{position:relative;display:flex;gap:13px;padding:0 0 20px}.history-event.latest{padding-bottom:22px}.history-node{position:relative;z-index:1;display:block;flex:0 0 auto;width:8px;height:8px;margin:5px 1px 0 0;border-radius:50%;background:#a7aaa7}.history-node.current{background:var(--brand)}.history-node.fork,.history-node.updated{background:#a7aaa7}.history-event-copy{display:grid;gap:3px;min-width:0}.history-event strong{overflow:hidden;font-size:12.5px;color:var(--copy);font-weight:650;text-overflow:ellipsis;white-space:nowrap}.history-event.latest strong{color:var(--ink)}.history-event small{display:block;color:var(--muted);font-size:10.5px;font-variant-numeric:tabular-nums}.history-event p{margin:3px 0 0;color:var(--muted);font-size:11px}.studio-modal-empty{margin:24px 0 6px;color:var(--muted);font-size:12px}.help-steps{display:grid;gap:0;margin-top:22px}.help-steps article{display:grid;grid-template-columns:32px minmax(0,1fr);gap:12px;padding:14px 0;border-top:1px solid var(--border-subtle)}.help-steps b{color:var(--muted);font-size:11px;font-variant-numeric:tabular-nums}.help-steps strong{font-size:12.5px;color:var(--copy)}.help-steps p{margin:5px 0 0;color:var(--muted);font-size:11.5px;line-height:1.6}
.history-event{animation:history-enter .24s ease both;animation-delay:var(--history-delay,0ms)}@keyframes history-enter{from{opacity:0;transform:translateY(5px)}to{opacity:1;transform:none}}@media(prefers-reduced-motion:reduce){.history-event{animation:none}}
</style>
