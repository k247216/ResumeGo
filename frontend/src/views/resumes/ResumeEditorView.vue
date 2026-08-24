<template>
  <div class="focused-editor">
    <header class="topbar">
      <div class="topbar__identity">
        <button type="button" @click="returnToWorkbench">← 返回工作台</button>
        <span></span>
        <div><small>简历编辑</small><strong>{{ editor.resumeTitle.value }}</strong></div>
      </div>
      <div class="topbar__actions">
        <button type="button" :disabled="!editor.canUndo.value" @click="editor.undo">撤销</button>
        <button type="button" :disabled="!editor.canRedo.value" @click="editor.redo">重做</button>
        <label>模板
          <select v-model="selectedTemplate">
            <option v-for="template in resumeTemplateOptions" :key="template.key" :value="template.key">{{ template.label }}</option>
          </select>
        </label>
        <button type="button" :disabled="exporting" @click="exportPdf">{{ exporting ? '导出中…' : '导出 PDF' }}</button>
      </div>
    </header>

    <div v-if="editor.loading.value" class="editor-state">正在读取本地简历…</div>
    <div v-else-if="editor.errorMessage.value && !editor.sections.value.length" class="editor-state error">
      <strong>无法打开简历</strong><span>{{ editor.errorMessage.value }}</span>
    </div>
    <template v-else>
      <div v-if="editor.errorMessage.value || targetLinkError" class="inline-error">
        <span>{{ targetLinkError || editor.errorMessage.value }}</span>
        <button v-if="targetLinkError" type="button" @click="retryTargetLink">重新关联</button>
      </div>
      <div class="editor-grid" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
        <EditorSidebar
          :sections="editor.sections.value"
          :available-modules="editor.availableModules.value"
          :selected-section-id="selectedSectionId"
          :versions="editor.versions.value"
          :selected-version-id="editor.selectedVersionId.value"
          :dirty="editor.dirty.value"
          :collapsed="sidebarCollapsed"
          @select-section="selectSection"
          @add-module="editor.addModule"
          @remove-module="removeModule"
          @move-module="editor.moveModule"
          @switch-version="editor.switchVersion"
          @toggle-collapsed="sidebarCollapsed = !sidebarCollapsed"
        />
        <EditorCanvas
          :sections="editor.sections.value"
          :selected-section-id="selectedSectionId"
          :resume-title="editor.resumeTitle.value"
          :version-label="editor.versionLabel.value"
          :updated-at="editor.updatedAt.value"
          :dirty="editor.dirty.value"
          :saving="editor.saving.value"
          :blank="editor.blank.value"
          @select-section="selectSection"
          @update-field="editor.updateField"
          @update-paragraph="editor.updateParagraph"
          @update-chips="editor.updateChips"
          @update-list-item="editor.updateListItem"
          @add-list-item="editor.addListItem"
          @remove-list-item="editor.removeListItem"
          @move-list-item="editor.moveListItem"
          @add-item="editor.addItem"
          @remove-item="editor.removeItem"
          @move-item="editor.moveItem"
          @toggle-visibility="editor.toggleVisibility"
          @remove-section="removeModule"
          @save-draft="save"
          @reset-draft="editor.reset"
        />
        <EditorPreviewPanel
          :sections="editor.sections.value"
          :selected-section-id="selectedSectionId"
          :version-label="editor.versionLabel.value"
          :template-style="selectedTemplate"
          @select-section="selectSection"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import EditorCanvas from '../../components/editor/EditorCanvas.vue'
import EditorPreviewPanel from '../../components/editor/EditorPreviewPanel.vue'
import EditorSidebar from '../../components/editor/EditorSidebar.vue'
import { useResumeEditor } from '../../composables/useResumeEditor'
import { defaultResumeTemplateKey, resumeTemplateOptions } from '../../constants/resumeTemplates'
import { exportResumeElementToPdf } from '../../utils/exportResumePdf'
import { linkResumeVersionToTarget } from '../../utils/targetMaterials'

const route = useRoute()
const router = useRouter()
const editor = useResumeEditor()
const selectedSectionId = ref('personal-info')
const selectedTemplate = ref(localStorage.getItem('resumego:selectedResumeTemplate') || defaultResumeTemplateKey)
const sidebarCollapsed = ref(false)
const exporting = ref(false)
const targetLinkError = ref('')

onMounted(() => {
  void editor.load({
    resumeId: positiveQueryId(route.query.resumeId),
    versionId: positiveQueryId(route.query.versionId),
    mode: route.query.mode === 'blank' ? 'blank' : undefined,
  })
})

onBeforeRouteLeave(() => confirmDiscard())

function positiveQueryId(value: unknown) {
  const number = Number(Array.isArray(value) ? value[0] : value)
  return Number.isSafeInteger(number) && number > 0 ? number : undefined
}

function confirmDiscard() {
  return !editor.dirty.value || window.confirm('当前有未保存的简历修改，确定离开吗？')
}

function returnToWorkbench() {
  if (!confirmDiscard()) return
  const targetId = positiveQueryId(route.query.targetId)
  void router.push({ name: 'workbench', query: targetId ? { targetId: String(targetId) } : {} })
}

function selectSection(sectionId: string) { selectedSectionId.value = sectionId }

function removeModule(sectionId: string) {
  editor.removeModule(sectionId)
  if (!editor.sections.value.some((section) => section.id === selectedSectionId.value)) {
    selectedSectionId.value = editor.sections.value[0]?.id ?? 'personal-info'
  }
}

async function save() {
  const wasBlank = editor.blank.value
  try {
    await editor.save()
    if (wasBlank) await linkSavedResumeToTarget()
  } catch { /* error is rendered by the composable */ }
}

async function linkSavedResumeToTarget() {
  const targetId = positiveQueryId(route.query.targetId)
  const versionId = editor.selectedVersionId.value
  if (!targetId || !versionId) return
  targetLinkError.value = ''
  try { await linkResumeVersionToTarget(targetId, versionId) }
  catch (error) { targetLinkError.value = `简历已保存，但关联求职目标失败：${error instanceof Error ? error.message : '请重试'}` }
}

function retryTargetLink() { void linkSavedResumeToTarget() }

async function exportPdf() {
  exporting.value = true
  try {
    localStorage.setItem('resumego:selectedResumeTemplate', selectedTemplate.value)
    await nextTick()
    const paper = document.querySelector<HTMLElement>('.focused-editor .a4-paper')
    if (!paper) throw new Error('未找到可导出的简历预览')
    await exportResumeElementToPdf({ sourceElement: paper, fileName: `${editor.resumeTitle.value}-${editor.versionLabel.value}` })
  } catch (error) {
    editor.errorMessage.value = error instanceof Error ? error.message : '导出 PDF 失败'
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.focused-editor{--editor-subbar-height:58px;--canvas:#eef2f4;--surface:#f8fafc;--surface-solid:#fff;--line:#dfe5e8;--ink:#1c2a32;--copy:#40506b;--muted:#7b8992;--brand:#10a878;--brand-soft:#eaf8f3;--danger:#a23d35;--danger-soft:#fff2f0;height:100vh;overflow:hidden;background:var(--canvas,#eef2f4);color:var(--ink,#1c2a32)}.topbar{box-sizing:border-box;height:64px;display:flex;align-items:center;justify-content:space-between;gap:16px;border-bottom:1px solid var(--line,#dfe5e8);background:var(--surface-solid,#fff);padding:9px 16px}.topbar__identity,.topbar__actions{display:flex;align-items:center;gap:9px}.topbar__identity>span{width:1px;height:28px;background:var(--line,#dfe5e8)}.topbar__identity div{display:grid}.topbar small{color:var(--muted,#7b8992)}.topbar button,.topbar select{border:1px solid var(--line,#d5dfe3);border-radius:8px;background:var(--surface-solid,#fff);padding:7px 10px}.topbar button:disabled{color:var(--muted,#aab4ba)}.topbar label{display:flex;align-items:center;gap:6px;color:var(--muted,#66757e);font-size:13px}.editor-grid{height:calc(100vh - 64px);display:grid;grid-template-columns:220px minmax(420px,.9fr) minmax(420px,1.1fr);min-width:1060px}.editor-grid.sidebar-collapsed{grid-template-columns:58px minmax(420px,.9fr) minmax(420px,1.1fr)}body[data-theme='dark'] .focused-editor{--canvas:#101112;--surface:#1a1b1d;--surface-solid:#1a1b1d;--line:rgba(255,255,255,.12);--ink:#f4f4f2;--copy:#c4c6c8;--muted:#9da0a4;--brand:#2ea884;--brand-soft:rgba(46,168,132,.16);--danger:#e06c60;--danger-soft:rgba(224,108,96,.14);color-scheme:dark}.editor-state{height:calc(100vh - 64px);display:grid;place-content:center;gap:8px;color:var(--muted,#6c7982)}.editor-state.error{color:var(--danger,#a23d35)}.inline-error{position:fixed;z-index:8;top:72px;left:50%;display:flex;align-items:center;gap:10px;transform:translateX(-50%);margin:0;border:1px solid var(--danger-soft,#efc7c2);border-radius:8px;background:var(--danger-soft,#fff2f0);color:var(--danger,#a23d35);padding:8px 12px}.inline-error button{border:0;border-radius:7px;background:var(--danger,#a23d35);color:#fff;padding:6px 9px}
</style>
