<template>
  <section data-test="resume-library" class="resume-library-view">
    <PageHeader eyebrow="本地简历" title="简历库" subtitle="像书架一样浏览你的简历，选中后查看版本与详情。">
      <template #actions>
        <button type="button" class="header-btn" data-test="import-md-header" @click="pickImportFile"><el-icon :size="14"><Upload /></el-icon>导入 Markdown</button>
        <button type="button" class="header-btn" :disabled="library.loading.value" @click="library.load">刷新</button>
        <router-link class="header-btn btn-link" :to="buildResumeEditorLocation({ mode: 'blank' })">创建空白简历</router-link>
      </template>
    </PageHeader>

    <div class="library-body">
      <main class="shelf-pane">
        <div v-if="library.loading.value && !library.resumes.value.length" class="state-card">正在读取本地简历…</div>
        <div v-else-if="library.errorMessage.value && !library.resumes.value.length" data-test="resume-library-error" class="state-card error"><strong>无法读取本地简历</strong><span>{{ library.errorMessage.value }}</span><button type="button" class="state-btn" @click="library.load">重新加载</button></div>
        <div v-else-if="!library.resumes.value.length" data-test="resume-library-empty" class="state-card empty"><strong>创建第一份本地简历</strong><span>可以先整理通用简历，也可以直接导入 Markdown 文件。</span><router-link class="state-primary" :to="buildResumeEditorLocation({ mode: 'blank' })">从空白开始</router-link><button type="button" class="state-btn" data-test="import-md-empty" @click="pickImportFile">导入 Markdown</button></div>

        <template v-else>
          <p v-if="library.errorMessage.value" class="inline-error">{{ library.errorMessage.value }} <button type="button" @click="library.load">重试</button></p>
          <div class="resume-shelf">
            <article v-for="resume in library.resumes.value" :key="resume.id" class="paper-card" :class="{ selected: resume.id === library.selectedResumeId.value }" :data-test="`paper-card-${resume.id}`">
              <button type="button" class="paper-click" :data-test="`paper-open-${resume.id}`" @click="library.selectResume(resume.id)">
                <span class="paper">
                  <span class="paper-band"></span>
                  <strong class="paper-name">{{ paperName(resume) }}</strong>
                  <span class="paper-role">{{ paperRole(resume) }}</span>
                  <span class="paper-lines"><i></i><i></i><i></i></span>
                  <em v-if="resume.currentVersion" class="paper-version">V{{ resume.currentVersion.versionNo }}</em>
                </span>
              </button>
              <div class="paper-meta">
                <div><strong>{{ resume.title }}</strong><small>{{ updatedLabel(resume) }}</small></div>
              </div>
            </article>

            <article class="paper-card import-card">
              <button type="button" class="paper-click" data-test="import-md" @click="pickImportFile">
                <span class="paper paper-import"><el-icon :size="30"><Upload /></el-icon></span>
              </button>
              <div class="paper-meta"><div><strong>导入 Markdown</strong><small>从 .md 文件创建简历</small></div></div>
            </article>
          </div>
        </template>
      </main>

      <aside class="resume-inspector">
        <template v-if="library.selectedResume.value">
          <header class="inspector-identity">
            <p class="inspector-kicker">当前选择</p>
            <h2>{{ library.selectedResume.value.title }}</h2>
            <p class="inspector-identity-line">{{ identityLine }}</p>
            <p class="inspector-updated">最近更新 {{ updatedLabel(library.selectedResume.value) }}</p>
          </header>

          <router-link class="btn-primary continue-link" data-test="continue-editing" :to="buildResumeEditorLocation({ resumeId: library.selectedResume.value.id, versionId: library.selectedVersion.value?.id })">继续编辑</router-link>

          <section class="inspector-section" data-test="inspector-used-by">
            <div class="section-head">
              <h3 class="section-title">用于</h3>
              <span class="section-count">{{ usedByTargets.length }} 个求职目标</span>
            </div>
            <p v-if="usedByLoading" class="inspector-note">正在读取关联目标…</p>
            <p v-else-if="!usedByTargets.length" class="inspector-note">尚未关联求职目标</p>
            <div v-else class="used-by-list">
              <button v-for="row in usedByTargets" :key="row.targetId" type="button" class="used-by-row" data-test="used-by-target" @click="openTarget(row.targetId)">
                <span class="used-by-copy">{{ row.label }}</span>
                <el-icon :size="12"><ArrowRight /></el-icon>
              </button>
            </div>
          </section>

          <section class="inspector-section" data-test="inspector-content">
            <h3 class="section-title">内容</h3>
            <div class="stat-row"><span>项目经历</span><strong>{{ library.selectedVersion.value?.content.projects?.length ?? 0 }} 条</strong></div>
            <div class="stat-row"><span>技能项</span><strong>{{ skillCount }} 项</strong></div>
          </section>

          <section class="inspector-section version-section" data-test="inspector-versions">
            <div class="version-head">
              <h3 class="section-title">最近版本</h3>
              <span class="version-count">{{ library.versions.value.length }} 个版本</span>
            </div>
            <p v-if="library.versionLoading.value" class="inspector-note">正在读取版本…</p>
            <p v-else-if="library.versionError.value" class="inspector-error">{{ library.versionError.value }}</p>
            <template v-else>
              <div class="version-list">
                <button
                  v-for="version in visibleVersions"
                  :key="version.id"
                  type="button"
                  class="version-row"
                  :class="{ active: version.id === library.selectedVersionId.value }"
                  :data-test="`version-row-${version.id}`"
                  @click="library.selectVersion(version.id)"
                >
                  <strong>v{{ version.versionNo }}</strong>
                  <span class="version-meta">
                    <small>{{ createdByLabel(version.createdByType) }}</small>
                    <time>{{ formatTime(version.createdAt) }}</time>
                  </span>
                  <em>{{ version.changeSummary || '手工保存的简历版本' }}</em>
                </button>
              </div>
              <button v-if="library.versions.value.length > 3" type="button" class="versions-expand" data-test="versions-expand" @click="showAllVersions = !showAllVersions">
                {{ showAllVersions ? '收起版本' : '查看全部版本' }}<el-icon :size="12"><component :is="showAllVersions ? ArrowUp : ArrowDown" /></el-icon>
              </button>
            </template>
          </section>

          <div class="inspector-danger">
            <button
              type="button"
              class="danger-link"
              data-test="delete-resume"
              :disabled="deleting"
              @click="confirmDeleteResume"
            >
              {{ deleting ? '删除中…' : '删除这份简历' }}
            </button>
            <p>删除后简历及其版本不再显示，关联的求职目标不受影响。</p>
          </div>
        </template>
        <div v-else class="inspector-empty">选择一份简历查看详情</div>
      </aside>
    </div>

    <input ref="fileInput" type="file" accept=".md,.markdown,.txt,text/markdown" data-test="import-file" class="file-input" @change="handleFileChange" />

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
import { ArrowDown, ArrowRight, ArrowUp, Check, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { createResume } from '../../api/resume'
import { getJobDescription } from '../../api/job'
import { useResumeLibrary } from '../../composables/useResumeLibrary'
import PageHeader from '../../components/PageHeader.vue'
import { useTargetsStore } from '../../stores/targets'
import type { Resume } from '../../types/resume'
import type { ParsedMarkdownResume } from '../../utils/parseMarkdownResume'
import { parseMarkdownResume } from '../../utils/parseMarkdownResume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'

const library = useResumeLibrary()
const targetsStore = useTargetsStore()
const router = useRouter()

const identityLine = computed(() => {
  const basic = library.selectedVersion.value?.content.basicInfo
  return [basic?.name || '姓名待补充', basic?.targetRole || '目标方向待补充'].join(' · ')
})
const skillCount = computed(() => {
  const content = library.selectedVersion.value?.content
  return content?.skillCategories?.reduce((count, category) => count + (category.skills?.length ?? 0), 0) || content?.skills?.length || 0
})

// ── 用于：反查 targets 中引用本简历任意版本的目标，经 JD 显示 公司 · 岗位 ──
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
        // JD 标签只有在带公司信息时才替换目标名，避免丢掉目标名里的公司前缀
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

// ── 最近版本：按更新时间倒序，默认只展示 3 个，可展开全部 ──
const showAllVersions = ref(false)
const orderedVersions = computed(() => [...library.versions.value].sort((left, right) => String(right.createdAt).localeCompare(String(left.createdAt))))
const visibleVersions = computed(() => showAllVersions.value ? orderedVersions.value : orderedVersions.value.slice(0, 3))
watch(() => library.selectedResumeId.value, () => { showAllVersions.value = false })

onMounted(() => { void library.load() })
function createdByLabel(type: string) { if (type === 'user') return '手工维护'; if (type === 'system') return '系统创建'; if (type === 'ai_suggestion') return '建议生成'; return '版本记录' }
function formatTime(value: string) { if (!value) return '时间未知'; return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }

const deleting = ref(false)

async function confirmDeleteResume() {
  const resume = library.selectedResume.value
  if (!resume) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${resume.title}」吗？删除后简历及其全部版本不再显示，此操作不可恢复。`,
      '删除简历',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  deleting.value = true
  try {
    await library.remove(resume.id)
    ElMessage.success('简历已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除简历失败')
  } finally {
    deleting.value = false
  }
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
  if (!value) return '未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '未知' : `${date.getMonth() + 1}月${date.getDate()}日`
}

// Markdown 导入
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
</script>

<style scoped>
.resume-library-view{display:flex;flex-direction:column;height:100%;min-height:0}
.library-body{flex:1;min-height:0;display:grid;grid-template-columns:minmax(0,1fr) 360px;border-top:1px solid var(--border-subtle)}

/* ── 左列：书架 ── */
.shelf-pane{min-height:0;overflow-y:auto;padding:26px 30px 48px}
.resume-shelf{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:24px;max-width:690px}
.paper-card{display:grid;gap:10px;min-width:0}
.paper-click{border:0;background:transparent;padding:0;cursor:pointer}
.paper{position:relative;display:flex;flex-direction:column;gap:6px;aspect-ratio:3/4;padding:16px 14px;border:1px solid var(--border-default);border-radius:11px;background:var(--bg-elevated);box-shadow:0 8px 22px rgba(24,40,64,.06);overflow:hidden;transition:transform .18s ease,box-shadow .18s ease,border-color .18s ease}
.paper-click:hover .paper{transform:translateY(-3px);box-shadow:0 12px 28px rgba(24,40,64,.10)}
.paper-card.selected .paper{border-color:var(--brand);box-shadow:0 0 0 1px var(--brand)}
.paper-band{position:absolute;top:0;left:0;width:100%;height:1px;background:var(--line-subtle)}
.paper-name{position:relative;margin-top:16px;color:var(--ink);font-size:15px;font-weight:800;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.paper-role{position:relative;color:var(--muted);font-size:11px;line-height:1.4;display:-webkit-box;-webkit-line-clamp:1;-webkit-box-orient:vertical;overflow:hidden}
.paper-lines{position:relative;display:grid;gap:7px;margin-top:8px}
.paper-lines i{display:block;height:5px;border-radius:3px;background:var(--border-default)}
.paper-lines i:nth-child(2){width:86%}.paper-lines i:nth-child(3){width:64%}
.paper-version{position:absolute;right:10px;bottom:10px;padding:2px 8px;border-radius:999px;background:var(--bg-subtle);color:var(--copy);font-size:10px;font-weight:800;font-style:normal}
.paper-meta{display:grid;gap:3px;min-width:0;padding:0 2px}
.paper-meta strong{overflow:hidden;font-size:13px;font-weight:600;color:var(--ink);text-overflow:ellipsis;white-space:nowrap}
.paper-meta small{color:var(--muted);font-size:11px}
.import-card .paper{border-style:dashed;background:transparent;box-shadow:none;display:grid;place-items:center;color:var(--muted)}
.import-card .paper-band{display:none}
.import-card .paper-click:hover .paper{border-color:var(--border-strong);color:var(--muted);transform:none;box-shadow:none}

.state-card{min-height:240px;display:grid;place-content:center;justify-items:center;gap:12px;border:1px dashed var(--border-default);border-radius:var(--radius-panel);color:var(--muted);font-size:13px;text-align:center;padding:24px}
.state-card strong{color:var(--ink);font-size:19px;font-weight:650}
.state-card.error{color:var(--danger);border-style:solid}
.state-btn{border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);padding:8px 13px;font-size:13px;cursor:pointer}
.state-primary{border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:9px 15px;font-size:13px;font-weight:600;text-decoration:none}
.inline-error{border:1px solid var(--danger-soft);border-radius:var(--radius-control);background:var(--danger-soft);color:var(--danger);padding:9px 12px;font-size:13px}
.inline-error button{border:0;background:none;color:var(--danger);text-decoration:underline;cursor:pointer}

/* ── 右列：Inspector ── */
.resume-inspector{min-height:0;overflow-y:auto;border-left:1px solid var(--border-subtle);padding:26px 24px 48px}
.inspector-identity{padding-bottom:18px;border-bottom:1px solid var(--border-subtle)}
.inspector-kicker{margin:0 0 6px;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:.08em}
.inspector-identity h2{margin:0;font-size:20px;font-weight:650;letter-spacing:-.02em;color:var(--ink);overflow-wrap:anywhere}
.inspector-identity-line{margin:7px 0 0;color:var(--muted);font-size:13px;line-height:1.5}
.inspector-updated{margin:5px 0 0;color:var(--muted);font-size:12px}
.continue-link{display:block;margin:18px 0 6px;text-align:center}
.inspector-section{padding:18px 0;border-top:1px solid var(--border-subtle)}
.section-title{margin:0 0 4px;font-size:12px;font-weight:600;letter-spacing:.06em;color:var(--muted)}
.section-head{display:flex;align-items:baseline;justify-content:space-between;gap:10px}
.section-count{color:var(--muted);font-size:12px}
.used-by-list{display:grid;margin-top:6px}
.used-by-row{display:flex;align-items:center;gap:8px;width:100%;border:0;background:transparent;padding:10px;border-radius:var(--radius-control);color:var(--copy);font-size:13px;font-weight:500;cursor:pointer;text-align:left}
.used-by-row:hover{background:var(--bg-hover);color:var(--ink)}
.used-by-row + .used-by-row{border-top:1px solid var(--border-subtle)}
.used-by-copy{overflow:hidden;flex:1;min-width:0;text-overflow:ellipsis;white-space:nowrap}
.used-by-row .el-icon{color:var(--muted)}
.versions-expand{display:inline-flex;align-items:center;gap:4px;margin-top:12px;padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500;cursor:pointer}
.versions-expand:hover{color:var(--ink)}
.stat-row{display:flex;align-items:baseline;justify-content:space-between;gap:12px;padding:9px 2px}
.stat-row + .stat-row{border-top:1px solid var(--border-subtle)}
.stat-row span{color:var(--muted);font-size:13px}
.stat-row strong{color:var(--ink);font-size:13px;font-weight:600}
.version-head{display:flex;align-items:center;justify-content:space-between;gap:10px}
.version-count{color:var(--muted);font-size:12px}
.inspector-note{padding:10px 2px;color:var(--muted);font-size:13px}
.inspector-error{padding:10px 2px;color:var(--danger);font-size:13px}
.inspector-danger{padding:16px 0;border-top:1px solid var(--border-subtle)}
.danger-link{border:0;background:transparent;color:var(--danger);font-size:13px;font-weight:600;cursor:pointer;padding:4px 0}
.danger-link:hover:not(:disabled){text-decoration:underline}
.danger-link:disabled{opacity:.5;cursor:not-allowed}
.inspector-danger p{margin:6px 0 0;color:var(--muted);font-size:12px;line-height:1.6}
.version-list{display:grid;margin-top:6px}
.version-row{display:grid;gap:4px;border:0;background:transparent;padding:11px 10px;text-align:left;cursor:pointer;color:var(--ink);border-radius:var(--radius-control)}
.version-row:hover{background:var(--bg-hover)}
.version-row.active{background:var(--bg-selected)}
.version-row + .version-row{border-top:1px solid var(--border-subtle)}
.version-row.active + .version-row{border-top:1px solid transparent}
.version-row strong{font-size:14px;font-weight:650}
.version-meta{display:flex;align-items:center;justify-content:space-between;gap:10px;color:var(--muted);font-size:11px}
.version-row em{color:var(--muted);font-size:12px;font-style:normal;line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.inspector-empty{display:grid;place-items:center;min-height:100%;color:var(--muted);font-size:14px}

/* ── 顶部操作 / 通用 ── */
.header-btn{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);padding:9px 13px;font-size:13px;font-weight:500;cursor:pointer}
.header-btn:hover{background:var(--bg-hover)}
.header-btn:disabled{opacity:.5;cursor:default}
.btn-link{text-decoration:none}
.btn-primary{display:inline-flex;align-items:center;gap:6px;border:1px solid var(--brand);border-radius:var(--radius-control);background:var(--brand);color:#fff;padding:9px 15px;font-size:13px;font-weight:600;text-decoration:none;cursor:pointer}
.btn-primary:hover{background:var(--accent-hover)}
.file-input{display:none}

/* ── 导入弹窗 ── */
.import-backdrop{position:fixed;z-index:60;inset:0;display:grid;place-items:center;background:rgba(10,14,20,.36);backdrop-filter:blur(6px)}
.import-dialog{position:relative;display:grid;gap:8px;width:min(460px,calc(100vw - 40px));padding:28px;border:1px solid var(--border-default);border-radius:var(--radius-panel);background:var(--bg-elevated);color:var(--ink);box-shadow:0 26px 80px rgba(10,20,32,.2)}
.dialog-close{position:absolute;top:12px;right:14px;border:0;background:transparent;color:var(--muted);font-size:22px;cursor:pointer}
.import-dialog p{margin:0}
.import-dialog>p:first-of-type{color:var(--muted);font-weight:600;font-size:12px;letter-spacing:.06em}
.import-dialog h2{margin:2px 0 0;font-size:20px;font-weight:650}
.import-file-name{color:var(--muted);font-size:13px}
.import-warning{border-radius:var(--radius-control);background:var(--warning-soft, #fff4e0);color:var(--warning, #9a6410);padding:8px 10px;font-size:13px;line-height:1.5}
.import-summary{display:grid;gap:7px;margin:10px 0 0;padding:0;list-style:none}
.import-summary li{display:flex;align-items:center;gap:7px;color:var(--copy);font-size:13px}
.import-summary li .el-icon{color:var(--muted)}
.import-error{color:var(--danger);font-size:13px}
.import-dialog footer{display:flex;justify-content:flex-end;gap:10px;margin-top:14px}
.import-dialog footer button{border-radius:var(--radius-control);padding:9px 15px;font-size:13px;cursor:pointer}
.import-dialog footer .ghost{border:1px solid var(--border-default);background:transparent;color:var(--copy)}
.import-dialog footer .primary{border:0;background:var(--brand);color:#fff}
.import-dialog footer .primary:disabled{opacity:.55;cursor:not-allowed}

/* 共享断点阶梯：窄窗口下书架与 Inspector 上下堆叠 */
@media (max-width: 959px) {
  .library-body{grid-template-columns:minmax(0,1fr);grid-template-rows:minmax(0,1fr) auto}
  .resume-inspector{border-left:0;border-top:1px solid var(--border-subtle);max-height:46vh;padding-top:20px}
}
</style>
