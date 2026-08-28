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
        <button type="button" class="template-trigger" data-test="template-trigger" @click="templateDialogOpen = true">
          <span>模板</span><strong>{{ currentTemplateLabel }}</strong><span aria-hidden="true">⌄</span>
        </button>
        <button type="button" :disabled="exporting" @click="exportPdf">{{ exporting ? '导出中…' : '导出 PDF' }}</button>
      </div>
    </header>

    <div v-if="templateDialogOpen" class="template-backdrop" role="presentation" @click.self="templateDialogOpen = false">
      <section class="template-dialog" role="dialog" aria-modal="true" aria-labelledby="template-dialog-title" data-test="template-dialog">
        <header class="template-dialog-head">
          <div><span>简历外观</span><h2 id="template-dialog-title">选择一个适合你的模板</h2><p>模板只影响排版外观，不会改变简历内容或版本。</p></div>
          <button type="button" class="template-dialog-close" aria-label="关闭模板选择" @click="templateDialogOpen = false">×</button>
        </header>
        <div class="template-grid">
          <button
            v-for="template in resumeTemplateOptions"
            :key="template.key"
            type="button"
            class="template-option"
            :class="{ selected: selectedTemplate === template.key }"
            :data-test="`template-option-${template.key}`"
            @click="chooseTemplate(template.key)"
          >
            <span class="template-preview" aria-hidden="true">
              <EditorPreviewPanel :sections="editor.sections.value" selected-section-id="" :version-label="editor.versionLabel.value" :template-style="template.key" />
            </span>
            <span class="template-option-copy"><strong>{{ template.label }}</strong><small>{{ template.badge }}</small><em>{{ template.description }}</em></span>
            <span v-if="selectedTemplate === template.key" class="template-selected-mark">✓</span>
          </button>
        </div>
      </section>
    </div>

    <div v-if="saveSummaryOpen" class="save-summary-backdrop" role="presentation" @click.self="closeSaveSummary">
      <section class="save-summary-dialog" role="dialog" aria-modal="true" aria-labelledby="save-summary-title" data-test="save-summary-dialog">
        <header class="save-summary-head">
          <div><span>版本记录</span><h2 id="save-summary-title">为这次修改留下说明</h2><p>可选填写，之后会显示在简历库的版本时间轴中。</p></div>
          <button type="button" aria-label="关闭保存说明" @click="closeSaveSummary">×</button>
        </header>
        <label class="save-summary-field"><span>本次修改说明</span><textarea v-model="saveSummary" rows="4" maxlength="240" placeholder="例如：补充 Redis 项目的性能数据"></textarea></label>
        <footer class="save-summary-actions"><button type="button" @click="closeSaveSummary">稍后填写</button><button type="button" class="save-summary-confirm" data-test="save-summary-confirm" :disabled="saveSubmitting" @click="confirmSave">{{ saveSubmitting ? '保存中…' : '保存新版本' }}</button></footer>
      </section>
    </div>

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
          @save-draft="requestSave"
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import EditorCanvas from '../../components/editor/EditorCanvas.vue'
import EditorPreviewPanel from '../../components/editor/EditorPreviewPanel.vue'
import EditorSidebar from '../../components/editor/EditorSidebar.vue'
import { useResumeEditor } from '../../composables/useResumeEditor'
import { defaultResumeTemplateKey, resumeTemplateOptions } from '../../constants/resumeTemplates'
import { exportResumeElementToPdf } from '../../utils/exportResumePdf'
import { linkResumeVersionToTarget } from '../../utils/targetMaterials'
import { normalizeResumeTemplateKey, readResumeTemplate, writeResumeTemplate } from '../../utils/resumeTemplate'
import type { ResumeTemplateKey } from '../../utils/resumeTemplate'

const route = useRoute()
const router = useRouter()
const editor = useResumeEditor()
const selectedSectionId = ref('personal-info')
const initialResumeId = positiveQueryId(route.query.resumeId) ?? positiveQueryId(route.query.versionId)
const selectedTemplate = ref<ResumeTemplateKey>(readResumeTemplate(initialResumeId) || defaultResumeTemplateKey)
const templateDialogOpen = ref(false)
const currentTemplateLabel = computed(() => resumeTemplateOptions.find((template) => template.key === selectedTemplate.value)?.label ?? '选择模板')
// 模板是简历资产级偏好：同一资产的所有版本共用，切换资产时不污染其他简历。
watch(selectedTemplate, (value) => {
  writeResumeTemplate(editor.resumeId.value ?? initialResumeId, value)
})
watch(() => editor.resumeId.value, (resumeId) => {
  if (resumeId) selectedTemplate.value = readResumeTemplate(resumeId)
})

function chooseTemplate(key: string) {
  selectedTemplate.value = normalizeResumeTemplateKey(key)
  templateDialogOpen.value = false
}
const sidebarCollapsed = ref(false)
const exporting = ref(false)
const targetLinkError = ref('')
const saveSummaryOpen = ref(false)
const saveSummary = ref('')
const saveSubmitting = ref(false)

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
  const resumeId = editor.resumeId.value ?? positiveQueryId(route.query.resumeId)
  void router.push({ name: 'resumes', query: resumeId ? { resumeId: String(resumeId) } : {} })
}

function selectSection(sectionId: string) { selectedSectionId.value = sectionId }

function removeModule(sectionId: string) {
  editor.removeModule(sectionId)
  if (!editor.sections.value.some((section) => section.id === selectedSectionId.value)) {
    selectedSectionId.value = editor.sections.value[0]?.id ?? 'personal-info'
  }
}

function requestSave() {
  if ((!editor.dirty.value && !editor.blank.value) || editor.saving.value) return
  saveSummary.value = ''
  saveSummaryOpen.value = true
}

function closeSaveSummary() {
  if (saveSubmitting.value) return
  saveSummaryOpen.value = false
  saveSummary.value = ''
}

async function confirmSave() {
  if (saveSubmitting.value) return
  const wasBlank = editor.blank.value
  saveSubmitting.value = true
  let succeeded = false
  try {
    await editor.save(saveSummary.value.trim() || undefined)
    if (wasBlank) await linkSavedResumeToTarget()
    succeeded = true
  } catch { /* error is rendered by the composable */ }
  finally {
    saveSubmitting.value = false
    if (succeeded) closeSaveSummary()
  }
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
    writeResumeTemplate(editor.resumeId.value ?? initialResumeId, selectedTemplate.value)
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
.template-trigger{display:flex!important;align-items:center;gap:7px}.template-trigger span:first-child{color:var(--muted,#7b8992);font-size:12px;font-weight:500}.template-trigger strong{font-size:12px;color:var(--ink,#1c2a32)}.template-trigger span:last-child{color:var(--muted,#7b8992);font-size:12px}
.save-summary-backdrop{position:fixed;inset:0;z-index:45;display:grid;place-items:center;background:rgba(15,23,42,.28);backdrop-filter:blur(6px);padding:24px}.save-summary-dialog{width:min(430px,calc(100vw - 40px));border:1px solid var(--line,#dfe5e8);border-radius:16px;background:var(--surface-solid,#fff);box-shadow:0 24px 70px rgba(15,23,42,.2);padding:20px}.save-summary-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.save-summary-head span{color:var(--brand,#10a878);font-size:10px;font-weight:800;letter-spacing:.12em}.save-summary-head h2{margin:5px 0 4px;color:var(--ink,#1c2a32);font-size:19px;letter-spacing:-.02em}.save-summary-head p{margin:0;color:var(--muted,#7b8992);font-size:11.5px;line-height:1.5}.save-summary-head>button{border:0;background:none;color:var(--muted,#7b8992);font-size:23px;line-height:1;cursor:pointer}.save-summary-field{display:grid;gap:7px;margin-top:20px;color:var(--copy,#40506b);font-size:12px;font-weight:650}.save-summary-field textarea{box-sizing:border-box;width:100%;resize:vertical;border:1px solid var(--line,#dfe5e8);border-radius:9px;background:var(--surface-solid,#fff);color:var(--ink,#1c2a32);padding:9px 10px;font:inherit;font-size:12px;line-height:1.55;outline:none}.save-summary-field textarea:focus{border-color:var(--brand,#10a878);box-shadow:0 0 0 2px var(--brand-soft,#eaf8f3)}.save-summary-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}.save-summary-actions button{border:1px solid var(--line,#dfe5e8);border-radius:8px;background:var(--surface-solid,#fff);color:var(--copy,#40506b);padding:7px 12px;font:inherit;font-size:11.5px;cursor:pointer}.save-summary-actions .save-summary-confirm{border-color:#17181a;background:#17181a;color:#fff;font-weight:650}.save-summary-actions .save-summary-confirm:disabled{opacity:.55;cursor:wait}
.template-backdrop{position:fixed;inset:0;z-index:40;display:grid;place-items:center;background:rgba(15,23,42,.28);backdrop-filter:blur(6px);padding:28px}.template-dialog{width:min(1040px,100%);max-height:min(760px,calc(100vh - 56px));overflow:hidden;border:1px solid var(--line,#dfe5e8);border-radius:18px;background:var(--surface-solid,#fff);box-shadow:0 28px 90px rgba(15,23,42,.24);padding:22px}.template-dialog-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.template-dialog-head span{color:var(--brand,#10a878);font-size:10px;font-weight:800;letter-spacing:.13em}.template-dialog-head h2{margin:5px 0 4px;color:var(--ink,#1c2a32);font-size:22px;letter-spacing:-.02em}.template-dialog-head p{margin:0;color:var(--muted,#7b8992);font-size:11.5px}.template-dialog-close{border:0;background:none;color:var(--muted,#7b8992);font-size:23px;line-height:1;cursor:pointer}.template-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-top:22px;max-height:calc(100vh - 190px);overflow:auto;padding:2px 3px 8px}.template-option{position:relative;display:grid;gap:9px;min-width:0;border:1px solid var(--line,#dfe5e8);border-radius:12px;background:var(--surface-solid,#fff);padding:9px;text-align:left;cursor:pointer;transition:border-color .15s ease,box-shadow .15s ease,transform .15s ease}.template-option:hover{border-color:var(--brand,#10a878);box-shadow:0 10px 24px rgba(16,168,120,.12);transform:translateY(-1px)}.template-option.selected{border-color:var(--brand,#10a878);box-shadow:0 0 0 2px var(--brand-soft,#eaf8f3)}.template-preview{display:block;height:210px;overflow:hidden;border-radius:8px;background:#eef2f4;pointer-events:none}.template-preview :deep(.editor-preview-panel){height:430px!important;background:#eef2f4}.template-preview :deep(.preview-scroll){padding:14px 0 0!important;overflow:hidden!important;justify-content:center!important}.template-preview :deep(.paper-viewport){transform:scale(.31);transform-origin:top center}.template-preview :deep(.preview-header){display:none}.template-option-copy{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:3px 7px;align-items:baseline}.template-option-copy strong{overflow:hidden;color:var(--ink,#1c2a32);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.template-option-copy small{color:var(--brand,#10a878);font-size:9.5px;font-weight:750}.template-option-copy em{grid-column:1/-1;display:-webkit-box;overflow:hidden;color:var(--muted,#7b8992);font-size:10px;font-style:normal;line-height:1.4;-webkit-box-orient:vertical;-webkit-line-clamp:2}.template-selected-mark{position:absolute;top:15px;right:15px;display:grid;place-items:center;width:21px;height:21px;border-radius:50%;background:var(--brand,#10a878);color:#fff;font-size:12px;font-weight:800}@media(max-width:1100px){.template-grid{grid-template-columns:repeat(4,minmax(0,1fr))}}@media(max-width:820px){.template-dialog{padding:17px}.template-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:580px){.template-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
