<template>
  <div class="doc-preview" data-test="resume-document-preview">
    <div v-if="empty" class="doc-empty" data-test="doc-preview-empty">
      <strong>该版本还没有正文内容</strong>
      <span>进入编辑台补充基本信息、经历与技能后，这里会生成与编辑台一致的纸张预览。</span>
      <button type="button" class="doc-empty-btn" @click="emit('edit')">进入编辑台补充内容</button>
    </div>

    <!-- 编辑台真实模版渲染：与编辑台所见一致，只读 -->
    <div v-else class="studio-preview" data-test="studio-preview">
      <EditorPreviewPanel
        :sections="sections"
        selected-section-id=""
        version-label=""
        :template-style="templateStyle"
        :highlight-section-ids="modifiedSectionIds"
        :added-section-ids="addedSectionIds"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
}>()

const emit = defineEmits<{ edit: [] }>()

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

const sections = computed(() => buildSections(props.content ?? {}))

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
.studio-preview{
  --paper-scale: 0.92;
}
/* 面板自带缩放头保留（−/100%/+）；画布区撑满 */
.studio-preview :deep(.preview-scroll) {
  max-height: none;
}
/* 只读：去掉指针与聚焦态 */
.studio-preview :deep(.resume-preview-section) {
  cursor: default;
  outline: none;
}
</style>
