<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成与编辑台一致的纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <!-- 编辑台真实模版渲染：与编辑台所见一致，只读；缩放控制由父级提供 -->
    <div v-else ref="containerRef" class="studio-preview" data-test="studio-preview">
      <div class="paper-holder" :style="{ width: `${PAPER_WIDTH * effectiveScale}px`, height: `${PAPER_HEIGHT * effectiveScale}px` }">
        <div class="paper-scale" :style="{ transform: `scale(${effectiveScale})` }">
          <EditorPreviewPanel
            :sections="previewSections"
            selected-section-id=""
            version-label=""
            :template-style="templateStyle"
            :highlight-section-ids="modifiedSectionIds"
            :added-section-ids="addedSectionIds"
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
  /** 用户缩放倍率（1 = 100%） */
  scale?: number
}>()

const emit = defineEmits<{ edit: [] }>()

const PAPER_WIDTH = 794
const PAPER_HEIGHT = 1123
const containerRef = ref<HTMLDivElement | null>(null)
const containerWidth = ref(794)
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (containerRef.value && 'ResizeObserver' in globalThis) {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) containerWidth.value = entry.contentRect.width
    })
    resizeObserver.observe(containerRef.value)
  }
})
onBeforeUnmount(() => { resizeObserver?.disconnect() })

/** 自适应容器宽度 × 用户缩放 */
const effectiveScale = computed(() => {
  const fit = Math.min(1, containerWidth.value / PAPER_WIDTH)
  const zoom = props.scale ?? 1
  return Math.max(0.3, fit * zoom)
})

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

/** 预览保留完整字段（姓名行由样式隐藏：本地单用户，姓名无信息量） */
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
</script>

<style scoped>
.studio-preview{min-height:0}
.paper-holder{position:relative;overflow:visible;margin:0 auto}
.paper-scale{position:absolute;top:0;left:0;width:794px;transform-origin:top left}
/* 面板融入页面：无内滚动、无自带缩放头、纸张静态全宽 */
.studio-preview :deep(.preview-header){display:none}
.studio-preview :deep(.preview-scroll){overflow:visible;height:auto;max-height:none}
.studio-preview :deep(.paper-viewport){position:static;height:auto}
.studio-preview :deep(.a4-paper){--paper-scale:1 !important;position:static;transform:none;left:auto;width:100%;min-height:1123px;box-shadow:0 2px 10px rgba(16,24,40,.07),0 1px 3px rgba(16,24,40,.04)}
/* 预览不显示姓名：隐藏纸张大标题与个人信息里的姓名行（本地单用户） */
.studio-preview :deep(.resume-head h1){display:none}
.studio-preview :deep([data-preview-section-id='personal-info'] .resume-preview-fields p:first-child){display:none}
/* 只读 */
.studio-preview :deep(.resume-preview-section){cursor:default;outline:none}
/* 荧光笔文字效果：变化章节的标题与正文文字加荧光扫过 */
.studio-preview :deep(.resume-preview-section.diff-modified h2),
.studio-preview :deep(.resume-preview-section.diff-added h2){
  background:linear-gradient(to top, rgba(217,119,6,.32) 38%, transparent 38%);
  border-radius:2px;
  width:fit-content;
  padding:0 4px;
}
.studio-preview :deep(.resume-preview-section.diff-added h2){
  background:linear-gradient(to top, rgba(22,139,104,.28) 38%, transparent 38%);
}
.studio-preview :deep(.resume-preview-section.diff-modified .resume-preview-fields p),
.studio-preview :deep(.resume-preview-section.diff-modified .resume-preview-item__head strong),
.studio-preview :deep(.resume-preview-section.diff-modified .resume-preview-paragraphs p){
  background:linear-gradient(to top, rgba(255,193,7,.30) 42%, transparent 42%);
  border-radius:2px;
}
.studio-preview :deep(.resume-preview-section.diff-added .resume-preview-fields p),
.studio-preview :deep(.resume-preview-section.diff-added .resume-preview-item__head strong),
.studio-preview :deep(.resume-preview-section.diff-added .resume-preview-paragraphs p),
.studio-preview :deep(.resume-preview-section.diff-added .resume-preview-chips span){
  background:linear-gradient(to top, rgba(22,139,104,.22) 42%, transparent 42%);
  border-radius:2px;
}
.doc-empty{display:grid;justify-items:center;gap:10px;border:1px dashed var(--border-default);border-radius:12px;padding:44px 24px;text-align:center;color:var(--muted)}
.doc-empty strong{color:var(--ink);font-size:14px}
.doc-empty span{font-size:12px;line-height:1.7;max-width:320px}
.doc-empty-btn{border:1px solid #17181a;border-radius:9px;background:#17181a;color:#fff;padding:8px 15px;font-size:12.5px;font-weight:600;cursor:pointer}
</style>
