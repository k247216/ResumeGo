<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成与编辑台一致的纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <!-- 编辑台真实模版渲染：与编辑台所见一致，只读；纸张按工作区比例展示 -->
    <div v-else ref="containerRef" class="studio-preview scrollable" data-test="studio-preview">
      <div class="paper-holder" :style="{ width: `${PAPER_WIDTH * effectiveScale}px`, height: `${PAPER_HEIGHT * effectiveScale}px` }">
        <div class="paper-scale" :style="{ transform: `scale(${effectiveScale})` }">
          <EditorPreviewPanel
            :sections="previewSections"
            selected-section-id=""
            version-label=""
            :template-style="templateStyle"
            :highlight-section-ids="modifiedSectionIds"
            :added-section-ids="addedSectionIds"
            :highlight-values="highlightValues"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import EditorPreviewPanel from '../editor/EditorPreviewPanel.vue'
import { buildSections } from '../../composables/useResumeEditor'
import type { ResumeContent } from '../../types/resume'
import type { ResumeChapterChange } from '../../utils/resumeVersionDiff'

const props = defineProps<{
  content: ResumeContent | null
  /** 比较模式下选中版本相对父版本的章节级变化 */
  changes?: ResumeChapterChange[]
  compareMode?: boolean
  /** 编辑台选择的模版（与编辑台共用同一持久化键） */
  templateStyle?: string
  /** 工作区预览倍率（1 = 100%），不在界面中暴露数值控件 */
  scale?: number
}>()

const emit = defineEmits<{ edit: [] }>()

const PAPER_WIDTH = 794
const PAPER_HEIGHT = 1123
const containerRef = ref<HTMLDivElement | null>(null)
const containerWidth = ref(0)
let resizeObserver: ResizeObserver | null = null

/** 宽屏尽量利用正文分栏，窄屏保留可读下限并只滚动正文预览。 */
const effectiveScale = computed(() => {
  const floor = props.scale ?? 0.7
  if (!containerWidth.value) return floor
  const fit = (containerWidth.value / PAPER_WIDTH) * 0.96
  return Math.min(1, Math.max(floor, fit))
})

onMounted(() => {
  if (!containerRef.value || !('ResizeObserver' in globalThis)) return
  resizeObserver = new ResizeObserver(([entry]) => {
    containerWidth.value = entry.contentRect.width
  })
  resizeObserver.observe(containerRef.value)
})

onBeforeUnmount(() => resizeObserver?.disconnect())

/** 章节全部为空视为无正文 */
const empty = computed(() => {
  const content = props.content
  if (!content) return true
  const chapters = [
    content.basicInfo && Object.keys(content.basicInfo).length ? 'x' : '',
    content.summary,
    content.workExperience?.length ? 'x' : '',
    content.projects?.length ? 'x' : '',
    content.education?.length ? 'x' : '',
    content.skills?.length ? 'x' : '',
    content.skillCategories?.length ? 'x' : '',
    content.certifications?.length ? 'x' : '',
    content.languages?.length ? 'x' : '',
    content.githubProjects?.length ? 'x' : '',
    content.customSections?.length ? 'x' : '',
  ]
  return chapters.every((chapter) => !chapter)
})

/** 预览保留完整字段；列表标题与纸张正文是两层不同的信息密度。 */
const previewSections = computed(() => buildSections(props.content ?? {}))

/** diff 章节键 → 编辑台 section id */
const SECTION_ID_MAP: Record<string, string> = {
  basicInfo: 'personal-info',
  summary: 'summary',
  workExperience: 'work-experience',
  education: 'education',
  skills: 'skills',
  skillCategories: 'skills',
  projects: 'projects',
  certifications: 'certifications',
  languages: 'languages',
  githubProjects: 'github',
  customSections: 'custom',
}

const modifiedSectionIds = computed(() => {
  if (!props.compareMode) return []
  return (props.changes ?? [])
    .filter((change) => change.changeType === 'modified')
    .map((change) => SECTION_ID_MAP[change.chapterKey])
    .filter(Boolean)
})
const addedSectionIds = computed(() => {
  if (!props.compareMode) return []
  return (props.changes ?? [])
    .filter((change) => change.changeType === 'added')
    .map((change) => SECTION_ID_MAP[change.chapterKey])
    .filter(Boolean)
})

const highlightValues = computed(() => {
  if (!props.compareMode) return {}
  const result: Record<string, NonNullable<ResumeChapterChange['details']>> = {}
  for (const change of props.changes ?? []) {
    const sectionId = SECTION_ID_MAP[change.chapterKey]
    if (!sectionId || !change.details?.length) continue
    result[sectionId] = change.details
  }
  return result
})
</script>

<style scoped>
.doc-preview{width:100%;height:100%;min-width:0;min-height:0}
.studio-preview{display:block;width:100%;height:100%;min-width:0;min-height:0;overflow:auto;scrollbar-gutter:stable}
.paper-holder{position:relative;overflow:visible;margin:0 auto}
.paper-scale{position:absolute;top:0;left:0;width:794px;height:1123px;transform-origin:top left}
/* 面板融入页面：无内滚动、无自带缩放头、纸张静态全宽 */
.studio-preview :deep(.preview-header){display:none}
.studio-preview :deep(.preview-scroll){overflow:visible;height:auto;max-height:none;padding:0}
.studio-preview :deep(.paper-viewport){position:static;width:794px !important;min-height:1123px !important;height:1123px !important}
.studio-preview :deep(.a4-paper){--paper-scale:1 !important;position:static;transform:none;left:0;width:794px;min-height:1123px;box-shadow:0 2px 10px rgba(16,24,40,.07),0 1px 3px rgba(16,24,40,.04)}
.studio-preview :deep(.resume-head h1){display:block}
/* 只读 */
.studio-preview :deep(.resume-preview-section){cursor:default;outline:none}
/* 变化使用同一套“章节边线 + 文字荧光线”，不再把一整行铺成色块；颜色和左侧图例一致。 */
.studio-preview :deep(.diff-highlight-modified),
.studio-preview :deep(.diff-highlight-added),
.studio-preview :deep(.diff-highlight-removed),
.studio-preview :deep(.resume-preview-section.diff-modified h2),
.studio-preview :deep(.resume-preview-section.diff-added h2){
  border-radius:0;
  padding:0 2px;
  box-decoration-break:clone;
  -webkit-box-decoration-break:clone;
}
.studio-preview :deep(.resume-preview-section.diff-modified h2),
.studio-preview :deep(.resume-preview-section.diff-added h2){background:none !important;box-shadow:none !important;border-left:3px solid transparent;padding-left:8px}
.studio-preview :deep(.resume-preview-section.diff-modified h2){border-left-color:rgba(217,119,6,.9)}
.studio-preview :deep(.resume-preview-section.diff-added h2){border-left-color:rgba(22,139,104,.9)}
.studio-preview :deep(.diff-highlight-modified){background:linear-gradient(transparent 64%,rgba(245,180,0,.62) 64%,rgba(245,180,0,.62) 91%,transparent 91%) !important;box-shadow:none !important}
.studio-preview :deep(.diff-highlight-added){background:linear-gradient(transparent 64%,rgba(22,139,104,.48) 64%,rgba(22,139,104,.48) 91%,transparent 91%) !important;box-shadow:none !important}
.studio-preview :deep(.diff-highlight-removed){background:linear-gradient(transparent 64%,rgba(194,86,86,.48) 64%,rgba(194,86,86,.48) 91%,transparent 91%) !important;color:#963d3d;box-shadow:none !important}
.doc-empty{display:grid;justify-items:center;gap:10px;border:1px dashed var(--border-default);border-radius:12px;padding:44px 24px;text-align:center;color:var(--muted)}
.doc-empty strong{color:var(--ink);font-size:14px}
.doc-empty span{font-size:12px;line-height:1.7;max-width:320px}
.doc-empty-btn{border:1px solid #17181a;border-radius:9px;background:#17181a;color:#fff;padding:8px 15px;font-size:12.5px;font-weight:600;cursor:pointer}
</style>
