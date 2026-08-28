<template>
  <section class="editor-preview-panel">
    <header class="preview-header">
      <div class="preview-zoom">
        <button type="button" :disabled="zoom <= 50" @click="zoom -= 10">−</button>
        <span>{{ zoom }}%</span>
        <button type="button" :disabled="zoom >= 110" @click="zoom += 10">+</button>
      </div>
    </header>

    <div class="preview-scroll" title="双击恢复 70% 缩放" @dblclick="zoom = 70">
      <div class="paper-viewport" :style="paperViewportStyle">
        <article
          class="a4-paper"
          :class="`template-${templateStyle}`"
          :style="paperStyle"
        >
          <header class="resume-head">
            <h1 :class="valueHighlightClass('personal-info', basicName)">{{ basicName }}</h1>
            <div class="resume-contact">
              <span :class="valueHighlightClass('personal-info', targetRole)">{{ targetRole }}</span>
              <span v-if="contactLine" :class="valueHighlightClass('personal-info', contactLine)">{{ contactLine }}</span>
            </div>
            <div v-if="profileLine" class="resume-profile-line">
              <span :class="valueHighlightClass('personal-info', profileLine)">{{ profileLine }}</span>
            </div>
          </header>

          <section
            v-for="section in printableSections"
            :key="section.id"
            class="resume-preview-section"
            :class="{ active: section.id === selectedSectionId, 'diff-modified': highlightSectionIds?.includes(section.id), 'diff-added': addedSectionIds?.includes(section.id) }"
            :data-preview-section-id="section.id"
            role="button"
            tabindex="0"
            @click="$emit('select-section', section.id)"
            @keydown.enter="$emit('select-section', section.id)"
          >
            <h2>{{ section.title }}</h2>

            <div v-if="section.fields.length" class="resume-preview-fields">
              <p v-for="field in section.fields" :key="field.key">
                <strong>{{ field.label }}</strong>
                <span :class="valueHighlightClass(section.id, field.value)">{{ field.value || '待补充' }}</span>
              </p>
            </div>

            <div v-if="section.chips.length" class="resume-preview-chips">
              <span v-for="chip in section.chips" :key="chip" :class="valueHighlightClass(section.id, chip)">{{ chip }}</span>
            </div>

            <div v-if="section.items?.length" class="resume-preview-paragraphs project-preview-list">
              <div
                v-for="item in section.items"
                :key="item.id"
                class="resume-preview-item"
              >
                <div class="resume-preview-item__head">
                  <strong :class="valueHighlightClass(section.id, previewItemTitle(section, item))">{{ previewItemTitle(section, item) }}</strong>
                  <span v-if="previewItemTime(section, item)" :class="valueHighlightClass(section.id, previewItemTime(section, item))">{{ previewItemTime(section, item) }}</span>
                </div>

                <div v-if="previewItemMeta(section, item)" class="resume-preview-item-meta">
                  <span :class="valueHighlightClass(section.id, previewItemMeta(section, item))">{{ previewItemMeta(section, item) }}</span>
                </div>

                <div v-if="visibleFieldsForItem(section, item).length" class="resume-preview-item-fields">
                  <span
                    v-for="field in visibleFieldsForItem(section, item)"
                    :key="field.key"
                  >
                    <strong>{{ field.label }}：</strong><span :class="valueHighlightClass(section.id, field.value)">{{ field.value }}</span>
                  </span>
                </div>

                <p v-if="item.description" :class="valueHighlightClass(section.id, item.description)">
                  {{ item.description }}
                </p>

                <div
                  v-for="field in visibleListFields(item.listFields)"
                  :key="field.key"
                  class="resume-preview-list-field"
                >
                  <strong>{{ field.label }}：</strong>
                  <span v-for="value in visibleListValues(field.value)" :key="value" :class="valueHighlightClass(section.id, value)">{{ value }}</span>
                </div>
              </div>
            </div>

            <div v-else-if="section.paragraphs.length" class="resume-preview-paragraphs">
              <p
                v-for="(paragraph, index) in section.paragraphs"
                :key="`${section.id}-${index}`"
                :class="valueHighlightClass(section.id, paragraph)"
              >
                <strong v-if="section.paragraphLabels?.[index]">{{ section.paragraphLabels[index] }}：</strong>
                {{ paragraph || '待补充' }}
              </p>
            </div>
          </section>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { EditorSection } from '../../types/editor'
import type { ResumeValueChange } from '../../utils/resumeVersionDiff'

const props = defineProps<{
  sections: EditorSection[]
  selectedSectionId: string
  versionLabel: string
  templateStyle?: string
  /** 荧光笔高亮：内容有变化的章节（琥珀扫过） */
  highlightSectionIds?: string[]
  /** 新增章节（绿色扫过） */
  addedSectionIds?: string[]
  highlightValues?: Record<string, ResumeValueChange[]>
}>()

defineEmits<{
  (event: 'select-section', sectionId: string): void
}>()

const zoom = ref(70)
const baseWidth = 794
const baseHeight = 1123
const basePaddingX = 58
const basePaddingY = 54
const templateStyle = computed(() => props.templateStyle || 'classic')
const zoomRatio = computed(() => zoom.value / 100)

const basicSection = computed(() => props.sections.find((section) => section.type === 'personal_info'))
const printableSections = computed(() => (
  props.sections.filter((section) => section.type !== 'personal_info' && section.visible)
))

const basicName = computed(() => {
  const name = valueOfBasic('basicInfo.name')
  return name || '姓名待补充'
})

const targetRole = computed(() => {
  const role = valueOfBasic('basicInfo.targetRole')
  return role || '目标岗位待明确'
})

const contactLine = computed(() => {
  const phone = valueOfBasic('basicInfo.phone')
  const email = valueOfBasic('basicInfo.email')
  const location = valueOfBasic('basicInfo.location')
  return [location, phone, email].filter(Boolean).join(' · ') || '联系方式待补充'
})

const profileLine = computed(() => {
  const age = valueOfBasic('basicInfo.age')
  const gender = normalizedVisibleValue(valueOfBasic('basicInfo.gender'))
  const education = valueOfBasic('basicInfo.educationLevel')
  const years = valueOfBasic('basicInfo.yearsOfExperience')
  const political = normalizedVisibleValue(valueOfBasic('basicInfo.politicalStatus'))
  return [age && `${age}岁`, gender, education, years && `${years}经验`, political]
    .filter(Boolean)
    .join(' · ')
})

function valueOfBasic(key: string) {
  return basicSection.value?.fields.find((field) => field.key === key)?.value?.trim() || ''
}

function normalizedVisibleValue(value: string) {
  return value === '不展示' ? '' : value
}

function valueHighlightClass(sectionId: string, value: string | undefined) {
  const text = value?.trim()
  if (!text) return {}
  const matches = props.highlightValues?.[sectionId] ?? []
  const match = matches.find((item) => item.value && (text === item.value || text.includes(item.value) || item.value.includes(text)))
  if (!match) return {}
  return {
    'diff-highlight-modified': match.changeType === 'modified',
    'diff-highlight-added': match.changeType === 'added',
  }
}

function visibleFields(fields: EditorSection['fields']) {
  return fields.filter((field) => field.value?.trim())
}

function visibleFieldsForItem(section: EditorSection, item: NonNullable<EditorSection['items']>[number]) {
  const hiddenLabelsByType: Record<string, string[]> = {
    work_experience: ['公司', '职位', '地点', '开始时间', '结束时间'],
    education: ['学校', '专业', '学历', '时间'],
    projects: ['项目名称'],
    skills: ['技能类别'],
    certifications: ['证书名称', '颁发机构', '获得时间'],
    languages: ['语言', '熟练度'],
    github: ['项目名称', '仓库链接'],
    qr_codes: ['名称', '链接'],
    custom: ['标题'],
  }
  const hidden = hiddenLabelsByType[section.type] ?? []
  return visibleFields(item.fields ?? []).filter((field) => !hidden.includes(field.label))
}

function visibleListFields(fields: NonNullable<EditorSection['items']>[number]['listFields']) {
  return (fields ?? []).filter((field) => visibleListValues(field.value).length > 0)
}

function visibleListValues(values: string[]) {
  return values.map((value) => value.trim()).filter(Boolean)
}

function fieldValue(item: NonNullable<EditorSection['items']>[number], label: string) {
  return item.fields?.find((field) => field.label === label)?.value?.trim() || ''
}

function previewItemTitle(section: EditorSection, item: NonNullable<EditorSection['items']>[number]) {
  if (section.type === 'work_experience') return fieldValue(item, '职位') || item.title || '职位待补充'
  if (section.type === 'education') return fieldValue(item, '学校') || item.title || '学校待补充'
  if (section.type === 'projects') return fieldValue(item, '项目名称') || item.title || '项目名称待补充'
  if (section.type === 'skills') return fieldValue(item, '技能类别') || item.title || '技能类别'
  if (section.type === 'certifications') return fieldValue(item, '证书名称') || item.title || '证书待补充'
  if (section.type === 'languages') return fieldValue(item, '语言') || item.title || '语言待补充'
  if (section.type === 'github') return fieldValue(item, '项目名称') || item.title || 'GitHub 项目'
  if (section.type === 'qr_codes') return fieldValue(item, '名称') || item.title || '二维码'
  if (section.type === 'custom') return fieldValue(item, '标题') || item.title || '自定义内容'
  return item.title || '条目名称待补充'
}

function previewItemTime(section: EditorSection, item: NonNullable<EditorSection['items']>[number]) {
  if (section.type === 'work_experience') {
    return [fieldValue(item, '开始时间'), fieldValue(item, '结束时间')].filter(Boolean).join(' - ')
  }
  if (section.type === 'education') return fieldValue(item, '时间')
  if (section.type === 'certifications') return fieldValue(item, '获得时间')
  return ''
}

function previewItemMeta(section: EditorSection, item: NonNullable<EditorSection['items']>[number]) {
  if (section.type === 'work_experience') {
    return [fieldValue(item, '公司'), fieldValue(item, '地点')].filter(Boolean).join(' · ')
  }
  if (section.type === 'education') {
    return [fieldValue(item, '学历'), fieldValue(item, '专业'), fieldValue(item, 'GPA') && `GPA ${fieldValue(item, 'GPA')}`]
      .filter(Boolean)
      .join(' · ')
  }
  if (section.type === 'certifications') return fieldValue(item, '颁发机构')
  if (section.type === 'languages') return fieldValue(item, '熟练度')
  if (section.type === 'github') return fieldValue(item, '仓库链接')
  if (section.type === 'qr_codes') return fieldValue(item, '链接')
  return ''
}

const paperStyle = computed(() => {
  return {
    width: `${baseWidth}px`,
    minHeight: `${baseHeight}px`,
    padding: `${basePaddingY}px ${basePaddingX}px`,
    fontSize: '13px',
    '--paper-scale': String(zoomRatio.value),
  }
})

const paperViewportStyle = computed(() => ({
  width: `${baseWidth * zoomRatio.value}px`,
  minHeight: `${baseHeight * zoomRatio.value}px`,
}))
</script>

<style scoped>
.editor-preview-panel {
  position: relative;
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--surface, #f1f5f9);
}

.preview-header {
  position: absolute;
  top: 10px;
  right: 14px;
  z-index: 3;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border: 1px solid var(--line, rgba(226, 232, 240, 0.58));
  border-radius: 999px;
  background: var(--surface, rgba(255, 255, 255, 0.42));
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  opacity: 0.62;
  padding: 3px;
  transition: opacity 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.preview-header:hover {
  border-color: var(--line, rgba(203, 213, 225, 0.86));
  background: var(--surface, rgba(255, 255, 255, 0.74));
  opacity: 1;
}

.preview-zoom {
  display: flex;
  align-items: center;
  gap: 2px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  padding: 2px;
}

.preview-zoom button {
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--copy, #334155);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  line-height: 18px;
  padding: 0;
}

.preview-zoom button:hover {
  background: var(--surface, rgba(241, 245, 249, 0.82));
}

.preview-zoom button:disabled {
  color: var(--muted, #cbd5e1);
  cursor: not-allowed;
}

.preview-zoom span {
  width: 31px;
  color: var(--muted, #64748b);
  font-size: 10px;
  font-weight: 800;
  line-height: 18px;
  text-align: center;
  letter-spacing: 0;
}

.preview-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 34px 22px 90px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.paper-viewport {
  position: relative;
  flex: 0 0 auto;
}

.a4-paper {
  box-sizing: border-box;
  position: absolute;
  top: 0;
  left: 50%;
  flex: 0 0 auto;
  background: #fff;
  color: #111827;
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.18);
  transform: translateX(-50%) scale(var(--paper-scale));
  transform-origin: top center;
  --resume-accent: #111827;
  --resume-rule: #111827;
  --resume-muted: #6b7280;
}

.a4-paper.template-blue {
  --resume-accent: #2563eb;
  --resume-rule: #2563eb;
}

.a4-paper.template-blue .resume-head,
.a4-paper.template-blue .resume-preview-section h2 {
  border-bottom-color: var(--resume-rule);
}

.a4-paper.template-classic {
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
}

.a4-paper.template-classic .resume-head {
  border-bottom-color: var(--resume-rule);
}

.a4-paper.template-classic .resume-head h1 {
  letter-spacing: 0.08em;
}

.a4-paper.template-classic .resume-preview-section h2 {
  border-bottom-color: var(--resume-rule);
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  font-size: 1em;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.a4-paper.template-minimal .resume-head {
  border-bottom-color: #e5e7eb;
  margin-bottom: 20px;
  padding-bottom: 18px;
  text-align: left;
}

.a4-paper.template-minimal {
  box-shadow: 0 18px 54px rgba(15, 23, 42, 0.11);
  --resume-accent: #111827;
  --resume-rule: #e5e7eb;
  --resume-muted: #6b7280;
}

.a4-paper.template-minimal .resume-contact {
  justify-content: flex-start;
}

.a4-paper.template-minimal .resume-preview-section h2 {
  border-bottom-color: var(--resume-rule);
  color: #374151;
  letter-spacing: 0.02em;
  text-transform: none;
}

.a4-paper.template-minimal .resume-preview-chips span {
  background: #f3f4f6;
}

.a4-paper.template-emerald {
  --resume-accent: #047857;
  --resume-rule: #10a878;
  --resume-muted: #64748b;
  background:
    linear-gradient(90deg, rgba(16, 168, 120, 0.08) 0 8px, transparent 8px),
    #fff;
}

.a4-paper.template-emerald .resume-head {
  border-bottom-color: var(--resume-rule);
  text-align: left;
}

.a4-paper.template-emerald .resume-head h1 {
  color: #064e3b;
}

.a4-paper.template-emerald .resume-preview-section h2 {
  border-bottom-color: rgba(16, 168, 120, 0.28);
}

.a4-paper.template-emerald .resume-preview-chips span,
.a4-paper.template-emerald .resume-preview-list-field > span {
  background: #ecfdf5;
  color: #047857;
}

.a4-paper.template-graphite {
  --resume-accent: #18181b;
  --resume-rule: #52525b;
  --resume-muted: #71717a;
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  background: #fdfdfd;
}

.a4-paper.template-graphite .resume-head {
  border-bottom-color: #27272a;
  text-align: left;
}

.a4-paper.template-graphite .resume-head h1 {
  color: #18181b;
  font-size: 1.9em;
  letter-spacing: -0.02em;
}

.a4-paper.template-graphite .resume-contact {
  justify-content: flex-start;
}

.a4-paper.template-graphite .resume-preview-section h2 {
  border-bottom-color: #e4e4e7;
  color: #27272a;
  font-size: 1em;
  letter-spacing: 0.08em;
}

.a4-paper.template-sidebar {
  --resume-accent: #101a33;
  --resume-rule: #10a878;
  --resume-muted: #64748b;
  padding-left: 220px !important;
  background:
    linear-gradient(90deg, #101a33 0 178px, #ffffff 178px 100%);
}

.a4-paper.template-sidebar .resume-head {
  position: absolute;
  top: 54px;
  left: 36px;
  width: 118px;
  border-bottom: 0;
  color: #e2e8f0;
  text-align: left;
}

.a4-paper.template-sidebar .resume-head h1 {
  color: #fff;
  font-size: 1.58em;
  line-height: 1.16;
}

.a4-paper.template-sidebar .resume-contact,
.a4-paper.template-sidebar .resume-profile-line {
  display: grid;
  justify-content: flex-start;
  gap: 8px;
  color: rgba(226, 232, 240, 0.78);
  font-size: 0.84em;
}

.a4-paper.template-sidebar .resume-preview-section h2 {
  border-bottom-color: rgba(16, 168, 120, 0.35);
}

.a4-paper.template-sidebar .resume-preview-chips span,
.a4-paper.template-sidebar .resume-preview-list-field > span {
  background: #f1f5f9;
}

.a4-paper.template-compact {
  --resume-accent: #0f172a;
  --resume-rule: #64748b;
  --resume-muted: #64748b;
  padding: 42px 48px !important;
  font-size: 12px !important;
}

.a4-paper.template-compact .resume-head {
  margin-bottom: 18px;
  padding-bottom: 14px;
}

.a4-paper.template-compact .resume-head h1 {
  font-size: 1.78em;
}

.a4-paper.template-compact .resume-contact {
  margin-top: 9px;
}

.a4-paper.template-compact .resume-preview-section {
  padding: 5px 0 7px;
}

/* 荧光标记统一落在文字底部，不把整行内容铺成色块。 */
.diff-highlight-modified,
.diff-highlight-added {
  background: none;
  border-radius: 0;
  padding: 0;
  text-decoration-line: underline;
  text-decoration-thickness: .28em;
  text-decoration-skip-ink: none;
  text-underline-offset: -.12em;
}

.diff-highlight-modified {
  text-decoration-color: rgba(245, 180, 0, .48);
}

.diff-highlight-added {
  text-decoration-color: rgba(22, 139, 104, .34);
}

.resume-preview-section.diff-added h2 {
  background: none;
  text-decoration-line: underline;
  text-decoration-thickness: .28em;
  text-decoration-skip-ink: none;
  text-decoration-color: rgba(22, 139, 104, .34);
  text-underline-offset: -.12em;
}

.a4-paper.template-compact .resume-preview-section h2 {
  margin-bottom: 6px;
  line-height: 1.7;
}

.a4-paper.template-compact .resume-preview-fields p,
.a4-paper.template-compact .resume-preview-paragraphs p,
.a4-paper.template-compact .resume-preview-item__head {
  line-height: 1.5;
}

.a4-paper.template-compact .resume-preview-item {
  margin-bottom: 8px;
}

.a4-paper.template-elegant {
  --resume-accent: #312e81;
  --resume-rule: #8b5cf6;
  --resume-muted: #6b7280;
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
  background:
    linear-gradient(180deg, rgba(139, 92, 246, 0.08), transparent 180px),
    #fff;
}

.a4-paper.template-elegant .resume-head {
  border-bottom-color: rgba(139, 92, 246, 0.42);
}

.a4-paper.template-elegant .resume-head h1 {
  color: #312e81;
  font-size: 2.18em;
  letter-spacing: 0.04em;
}

.a4-paper.template-elegant .resume-preview-section h2 {
  border-bottom-color: rgba(139, 92, 246, 0.25);
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
  font-size: 1.16em;
}

.a4-paper.template-elegant .resume-preview-chips span {
  background: #f5f3ff;
  color: #4338ca;
}

.a4-paper.template-warm {
  --resume-accent: #9a3412;
  --resume-rule: #c2410c;
  --resume-muted: #78716c;
  background:
    linear-gradient(180deg, #fff7ed 0 86px, #ffffff 86px 100%);
}

.a4-paper.template-warm .resume-head {
  border-bottom-color: rgba(194, 65, 12, 0.35);
}

.a4-paper.template-warm .resume-head h1 {
  color: #7c2d12;
}

.a4-paper.template-warm .resume-preview-section h2 {
  border-bottom-color: rgba(194, 65, 12, 0.22);
}

.a4-paper.template-warm .resume-preview-chips span,
.a4-paper.template-warm .resume-preview-list-field > span {
  background: #ffedd5;
  color: #9a3412;
}

.a4-paper.template-terminal {
  --resume-accent: #34d399;
  --resume-rule: #34d399;
  --resume-muted: #94a3b8;
  border: 10px solid #101a33;
  background: #f8fafc;
  font-family: 'SFMono-Regular', 'Cascadia Code', Menlo, Consolas, 'Noto Sans SC', monospace;
}

.a4-paper.template-terminal .resume-head {
  border-bottom-color: #34d399;
  background: #101a33;
  margin: -44px -48px 24px;
  padding: 34px 44px 24px;
  text-align: left;
}

.a4-paper.template-terminal .resume-head h1 {
  color: #fff;
  font-size: 1.88em;
}

.a4-paper.template-terminal .resume-contact,
.a4-paper.template-terminal .resume-profile-line {
  justify-content: flex-start;
  color: #cbd5e1;
}

.a4-paper.template-terminal .resume-preview-section h2 {
  border-bottom-style: dashed;
  border-bottom-color: rgba(52, 211, 153, 0.46);
  color: #047857;
}

.a4-paper.template-terminal .resume-preview-chips span,
.a4-paper.template-terminal .resume-preview-list-field > span {
  background: #d1fae5;
  color: #065f46;
}

.resume-head {
  border-bottom: 2px solid var(--resume-rule);
  padding-bottom: 24px;
  margin-bottom: 28px;
  text-align: center;
}

.resume-head h1,
.resume-head p {
  margin: 0;
}

.resume-head h1 {
  color: var(--resume-accent);
  font-size: 2.05em;
  letter-spacing: 0.02em;
}

.resume-contact {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px 18px;
  margin-top: 16px;
  color: var(--resume-muted);
  font-size: 0.98em;
}

.resume-profile-line {
  margin-top: 8px;
  color: var(--resume-muted);
  font-size: 0.92em;
}

.resume-preview-section {
  position: relative;
  padding: 9px 0 11px;
  border-radius: 0;
  cursor: pointer;
  outline: 1px solid transparent;
  outline-offset: 4px;
  transition: outline-color 0.16s ease;
}

.resume-preview-section:hover {
  outline-color: rgba(148, 163, 184, 0.22);
}

.resume-preview-section.active {
  outline-color: rgba(148, 163, 184, 0.34);
}

.resume-preview-section h2 {
  margin: 0 0 10px;
  border-bottom: 1.6px solid var(--resume-rule);
  color: var(--resume-accent);
  font-size: 1.12em;
  line-height: 2;
  letter-spacing: 0;
  text-transform: none;
}

.resume-preview-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.resume-preview-fields p,
.resume-preview-paragraphs p {
  margin: 0 0 8px;
  color: #374151;
  font-size: 1em;
  line-height: 1.72;
}

.resume-preview-fields strong,
.resume-preview-paragraphs strong {
  color: #111827;
}

.resume-preview-fields strong {
  display: inline-block;
  margin-right: 6px;
}

.resume-preview-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.resume-preview-chips span {
  border-radius: 999px;
  background: #eef2ff;
  color: #334155;
  font-size: 0.92em;
  padding: 5px 9px;
}

.resume-preview-item {
  margin-bottom: 12px;
}

.resume-preview-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #111827;
  font-size: 1em;
  line-height: 1.65;
}

.resume-preview-item__head span {
  flex: 0 0 auto;
  color: #9ca3af;
  font-size: 0.92em;
}

.resume-preview-item-meta {
  color: #6b7280;
  font-size: 0.94em;
  line-height: 1.55;
  margin: -2px 0 4px;
}

.resume-preview-item-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  margin: 2px 0 5px;
  color: #6b7280;
  font-size: 0.93em;
  line-height: 1.6;
}

.resume-preview-item-fields strong {
  color: #4b5563;
}

.resume-preview-list-field {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
  margin-top: 4px;
  color: #4b5563;
  font-size: 0.92em;
}

.resume-preview-list-field > span {
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  padding: 2px 7px;
}

.project-preview-list em {
  display: inline-block;
  color: #047857;
  font-style: normal;
  font-size: 0.9em;
  margin-left: 6px;
}

/* 基础五套可投递模板：结构稳定，只改变层级和识别色；其余模板在下方提供独立视觉变体。 */
.a4-paper.template-classic,
.a4-paper.template-blue,
.a4-paper.template-minimal,
.a4-paper.template-emerald,
.a4-paper.template-graphite {
  font-family: Inter, 'Noto Sans SC', ui-sans-serif, system-ui, sans-serif;
  background: #fff;
  border: 0;
}

.a4-paper.template-classic {
  --resume-accent: #111827;
  --resume-rule: #1f2937;
  --resume-muted: #6b7280;
}

.a4-paper.template-blue {
  --resume-accent: #1d4ed8;
  --resume-rule: #2563eb;
  --resume-muted: #64748b;
}

.a4-paper.template-blue .resume-head,
.a4-paper.template-blue .resume-preview-section h2 {
  border-bottom-color: rgba(37, 99, 235, 0.68);
}

.a4-paper.template-minimal {
  --resume-accent: #111827;
  --resume-rule: #d1d5db;
  --resume-muted: #6b7280;
}

.a4-paper.template-minimal .resume-head {
  border-bottom-color: #d1d5db;
  margin-bottom: 22px;
  padding-bottom: 18px;
}

.a4-paper.template-minimal .resume-preview-section h2 {
  border-bottom-color: transparent;
  color: #374151;
  letter-spacing: 0.01em;
}

.a4-paper.template-emerald {
  --resume-accent: #047857;
  --resume-rule: #168b68;
  --resume-muted: #64748b;
  border-top: 6px solid #168b68;
}

.a4-paper.template-emerald .resume-head {
  border-bottom-color: rgba(22, 139, 104, 0.44);
  text-align: left;
}

.a4-paper.template-emerald .resume-head h1 {
  color: #064e3b;
}

.a4-paper.template-emerald .resume-contact,
.a4-paper.template-emerald .resume-profile-line {
  justify-content: flex-start;
}

.a4-paper.template-emerald .resume-preview-section h2 {
  border-bottom-color: rgba(22, 139, 104, 0.28);
}

.a4-paper.template-graphite {
  --resume-accent: #18181b;
  --resume-rule: #52525b;
  --resume-muted: #71717a;
}

.a4-paper.template-graphite .resume-head {
  border-bottom-color: #27272a;
  text-align: left;
}

.a4-paper.template-graphite .resume-head h1 {
  color: #18181b;
}

.a4-paper.template-graphite .resume-contact,
.a4-paper.template-graphite .resume-profile-line {
  justify-content: flex-start;
}

/* 后十套模板使用独立的排版结构；颜色只是识别层，不再承担模板差异。 */
.a4-paper.template-royal {
  --resume-accent: #6d28d9;
  --resume-rule: #a78bfa;
  --resume-muted: #6b7280;
  border-top: 6px solid #7c3aed;
}
.a4-paper.template-royal .resume-head { display:grid; grid-template-columns:minmax(0,1fr) auto; align-items:end; gap:4px 20px; border-bottom-color: rgba(124, 58, 237, .32); text-align:left; }
.a4-paper.template-royal .resume-head h1 { grid-column:1 / -1; font-size:2.25em; letter-spacing:.03em; }
.a4-paper.template-royal .resume-contact { justify-content:flex-end; margin-top:0; }
.a4-paper.template-royal .resume-profile-line { grid-column:1 / -1; }
.a4-paper.template-royal .resume-preview-section { display:grid; grid-template-columns:150px minmax(0,1fr); gap:18px; padding:12px 0; }
.a4-paper.template-royal .resume-preview-section h2 { border-bottom:0; letter-spacing:.1em; text-transform:uppercase; }
.a4-paper.template-royal .resume-preview-chips span { background: #f5f3ff; color: #6d28d9; }

.a4-paper.template-steel {
  --resume-accent: #0f766e;
  --resume-rule: #5eead4;
  --resume-muted: #64748b;
  background: #fbfefd;
}
.a4-paper.template-steel .resume-head { display:grid; grid-template-columns:minmax(0,1.4fr) minmax(0,1fr); align-items:end; gap:18px; border-bottom-color: rgba(13, 148, 136, .35); text-align: left; }
.a4-paper.template-steel .resume-head h1 { font-size:1.9em; }
.a4-paper.template-steel .resume-contact,
.a4-paper.template-steel .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-steel .resume-contact { margin-top:0; padding:8px 0 0 16px; border-left:2px solid rgba(13,148,136,.35); }
.a4-paper.template-steel .resume-preview-section { display:grid; grid-template-columns:168px minmax(0,1fr); gap:16px; padding:12px 0; border-bottom:1px solid rgba(13,148,136,.12); }
.a4-paper.template-steel .resume-preview-section h2 { border-bottom:0; color:#0f766e; }
.a4-paper.template-steel .resume-preview-chips span { background: #f0fdfa; color: #0f766e; }

.a4-paper.template-wine {
  --resume-accent: #9f1239;
  --resume-rule: #fb7185;
  --resume-muted: #78716c;
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
}
.a4-paper.template-wine .resume-head { border-bottom-color: rgba(159, 18, 57, .32); padding-bottom:30px; }
.a4-paper.template-wine .resume-head h1 { font-size:2.35em; letter-spacing:.05em; }
.a4-paper.template-wine .resume-preview-section { border-left:2px solid rgba(159,18,57,.26); padding-left:18px; }
.a4-paper.template-wine .resume-preview-section h2 { border-bottom:0; letter-spacing:.08em; }
.a4-paper.template-wine .resume-preview-section h2::before { content:'§ '; opacity:.65; }
.a4-paper.template-wine .resume-preview-chips span { background: #fff1f2; color: #9f1239; }

.a4-paper.template-navy {
  --resume-accent: #1e3a8a;
  --resume-rule: #60a5fa;
  --resume-muted: #64748b;
  background: #fbfdff;
}
.a4-paper.template-navy .resume-head { display:grid; grid-template-columns:minmax(0,1fr) auto; align-items:end; gap:8px 22px; border-bottom-color: rgba(37, 99, 235, .35); text-align: left; }
.a4-paper.template-navy .resume-head h1 { grid-column:1 / -1; }
.a4-paper.template-navy .resume-contact,
.a4-paper.template-navy .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-navy .resume-contact { margin-top:0; padding:7px 10px; border:1px solid rgba(37,99,235,.28); border-radius:6px; }
.a4-paper.template-navy .resume-preview-section h2 { border-bottom-color: rgba(37, 99, 235, .22); letter-spacing: .08em; }
.a4-paper.template-navy .resume-preview-section h2 { display:inline-block; width:max-content; max-width:100%; padding:4px 10px; border-bottom:0; background:#eff6ff; border-radius:4px; }
.a4-paper.template-navy .resume-preview-chips span { background: #eff6ff; color: #1d4ed8; }

.a4-paper.template-forest {
  --resume-accent: #166534;
  --resume-rule: #86efac;
  --resume-muted: #64748b;
  border-left: 7px solid #16a34a;
}
.a4-paper.template-forest { padding-left:42px !important; }
.a4-paper.template-forest .resume-head { border-bottom-color: rgba(22, 101, 52, .3); text-align: left; }
.a4-paper.template-forest .resume-contact,
.a4-paper.template-forest .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-forest .resume-preview-section { padding-left:14px; border-left:2px solid rgba(22,101,52,.16); }
.a4-paper.template-forest .resume-preview-section h2 { border-bottom-color: rgba(22, 101, 52, .2); }
.a4-paper.template-forest .resume-preview-chips span { background: #f0fdf4; color: #166534; }

.a4-paper.template-slate {
  --resume-accent: #334155;
  --resume-rule: #94a3b8;
  --resume-muted: #64748b;
  font-size: 12.5px;
}
.a4-paper.template-slate .resume-head { border-bottom-color: #cbd5e1; margin-bottom: 21px; padding-bottom: 17px; text-align: left; }
.a4-paper.template-slate .resume-contact,
.a4-paper.template-slate .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-slate .resume-preview-section { display:grid; grid-template-columns:136px minmax(0,1fr); gap:14px; padding:7px 0 8px; }
.a4-paper.template-slate .resume-preview-section h2 { border-bottom:0; color:#334155; }

.a4-paper.template-rose {
  --resume-accent: #be185d;
  --resume-rule: #f9a8d4;
  --resume-muted: #78716c;
  border-top: 6px solid #ec4899;
}
.a4-paper.template-rose .resume-head { border-bottom-color: rgba(236, 72, 153, .28); }
.a4-paper.template-rose .resume-preview-section h2 { display:inline-block; width:max-content; max-width:100%; padding:4px 10px; border-bottom:0; border-radius:999px; background:#fdf2f8; }
.a4-paper.template-rose .resume-preview-chips span { background: #fdf2f8; color: #be185d; }

.a4-paper.template-ocean {
  --resume-accent: #0369a1;
  --resume-rule: #38bdf8;
  --resume-muted: #64748b;
}
.a4-paper.template-ocean .resume-head { border-bottom-color: rgba(14, 165, 233, .35); }
.a4-paper.template-ocean .resume-contact { justify-content:flex-start; margin-top:12px; padding:8px 10px; border:1px solid rgba(14,165,233,.24); border-radius:8px; }
.a4-paper.template-ocean .resume-preview-section h2 { border-bottom-color: rgba(14, 165, 233, .24); }
.a4-paper.template-ocean .resume-preview-chips span { background: #f0f9ff; color: #0369a1; }

.a4-paper.template-amber {
  --resume-accent: #92400e;
  --resume-rule: #fbbf24;
  --resume-muted: #78716c;
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
}
.a4-paper.template-amber .resume-head { border-bottom-color: rgba(217, 119, 6, .32); text-align: left; }
.a4-paper.template-amber .resume-head { padding-left:18px; border-left:4px solid #fbbf24; }
.a4-paper.template-amber .resume-contact,
.a4-paper.template-amber .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-amber .resume-preview-section { display:grid; grid-template-columns:150px minmax(0,1fr); gap:16px; }
.a4-paper.template-amber .resume-preview-section h2 { border-bottom:0; color:#92400e; }
.a4-paper.template-amber .resume-preview-chips span { background: #fffbeb; color: #92400e; }

.a4-paper.template-nord {
  --resume-accent: #334155;
  --resume-rule: #7dd3fc;
  --resume-muted: #64748b;
  background: #f8fafc;
}
.a4-paper.template-nord { font-family: 'IBM Plex Mono', 'SFMono-Regular', Consolas, monospace; }
.a4-paper.template-nord .resume-head { border-bottom:1px dashed rgba(14, 165, 233, .45); text-align: left; }
.a4-paper.template-nord .resume-contact,
.a4-paper.template-nord .resume-profile-line { justify-content: flex-start; }
.a4-paper.template-nord .resume-preview-section { padding:10px 12px; margin:7px 0; border:1px dashed rgba(14,165,233,.22); border-radius:5px; }
.a4-paper.template-nord .resume-preview-section h2 { border-bottom:0; letter-spacing:.04em; }
.a4-paper.template-nord .resume-preview-chips span { background: #e0f2fe; color: #334155; }

/* 新增五套结构型模板：每套改变阅读路径，不只替换强调色。 */
.a4-paper.template-editorial {
  --resume-accent: #1f2937;
  --resume-rule: #d6d3d1;
  --resume-muted: #78716c;
  font-family: 'Noto Serif SC', Georgia, 'Times New Roman', serif;
  background: #fffdf9;
}
.a4-paper.template-editorial .resume-head { display:grid; grid-template-columns:minmax(0,1fr) auto; align-items:end; gap:8px 24px; position:relative; border-bottom:0; padding-bottom:25px; text-align:left; }
.a4-paper.template-editorial .resume-head::after { content:''; position:absolute; right:0; bottom:0; left:0; height:2px; background:linear-gradient(90deg,#1f2937 0 28%,#d6d3d1 28% 100%); }
.a4-paper.template-editorial .resume-head h1 { grid-column:1 / -1; font-size:2.35em; letter-spacing:-.03em; }
.a4-paper.template-editorial .resume-contact { justify-content:flex-end; margin-top:0; font-family:Inter,'Noto Sans SC',sans-serif; font-size:.86em; }
.a4-paper.template-editorial .resume-profile-line { grid-column:1 / -1; }
.a4-paper.template-editorial .resume-preview-section { display:grid; grid-template-columns:132px minmax(0,1fr); gap:20px; padding:13px 0; border-bottom:1px solid rgba(214,211,209,.7); }
.a4-paper.template-editorial .resume-preview-section h2 { border-bottom:0; color:#57534e; font-size:.92em; letter-spacing:.08em; text-transform:uppercase; }
.a4-paper.template-editorial .resume-preview-chips span { background:#f5f5f4; color:#44403c; }

.a4-paper.template-timeline {
  --resume-accent: #166534;
  --resume-rule: #bbf7d0;
  --resume-muted: #64748b;
  background:#fff;
}
.a4-paper.template-timeline .resume-head { border-bottom-color:rgba(22,101,52,.26); text-align:left; }
.a4-paper.template-timeline .resume-contact,.a4-paper.template-timeline .resume-profile-line { justify-content:flex-start; }
.a4-paper.template-timeline .resume-preview-section { display:grid; grid-template-columns:128px minmax(0,1fr); gap:18px; position:relative; margin-left:5px; padding:13px 0 13px 20px; border-bottom:0; border-left:2px solid #dcfce7; }
.a4-paper.template-timeline .resume-preview-section::before { content:''; position:absolute; top:19px; left:-6px; width:10px; height:10px; border-radius:50%; background:#16a34a; box-shadow:0 0 0 3px #dcfce7; }
.a4-paper.template-timeline .resume-preview-section h2 { border-bottom:0; color:#166534; font-size:.95em; }
.a4-paper.template-timeline .resume-preview-chips span { background:#f0fdf4; color:#166534; }

.a4-paper.template-mono {
  --resume-accent: #111827;
  --resume-rule: #9ca3af;
  --resume-muted: #6b7280;
  font-family:'SFMono-Regular','Cascadia Code',Menlo,Consolas,'Noto Sans SC',monospace;
  background:#fafafa;
  border:1px solid #d1d5db;
}
.a4-paper.template-mono .resume-head { margin:-48px -58px 25px; padding:36px 48px 24px; background:#111827; border-bottom:4px solid #9ca3af; text-align:left; }
.a4-paper.template-mono .resume-head h1 { color:#fff; letter-spacing:-.04em; }
.a4-paper.template-mono .resume-contact,.a4-paper.template-mono .resume-profile-line { justify-content:flex-start; color:#d1d5db; }
.a4-paper.template-mono .resume-preview-section { padding:11px 0; border-bottom:1px solid #e5e7eb; }
.a4-paper.template-mono .resume-preview-section h2 { border-bottom:0; color:#111827; letter-spacing:.08em; }
.a4-paper.template-mono .resume-preview-chips span { border-radius:3px; background:#e5e7eb; color:#111827; }

.a4-paper.template-folio {
  --resume-accent: #0f172a;
  --resume-rule: #cbd5e1;
  --resume-muted: #64748b;
  counter-reset: folio-section;
  background:#fff;
}
.a4-paper.template-folio .resume-head { display:grid; grid-template-columns:minmax(0,1fr) 170px; align-items:end; gap:18px; padding-bottom:28px; border-bottom-color:#0f172a; text-align:left; }
.a4-paper.template-folio .resume-head h1 { font-size:2.55em; line-height:1; letter-spacing:-.06em; }
.a4-paper.template-folio .resume-contact { justify-content:flex-start; margin-top:0; padding-left:15px; border-left:3px solid #0f172a; }
.a4-paper.template-folio .resume-profile-line { grid-column:1 / -1; }
.a4-paper.template-folio .resume-preview-section { display:grid; grid-template-columns:152px minmax(0,1fr); gap:18px; counter-increment:folio-section; padding:14px 0; border-bottom:1px solid #e2e8f0; }
.a4-paper.template-folio .resume-preview-section h2 { border-bottom:0; color:#334155; }
.a4-paper.template-folio .resume-preview-section h2::before { content:counter(folio-section,decimal-leading-zero) '  '; color:#94a3b8; font-size:.78em; letter-spacing:.04em; }
.a4-paper.template-folio .resume-preview-chips span { background:#f1f5f9; color:#334155; }

.a4-paper.template-paper {
  --resume-accent: #78350f;
  --resume-rule: #d6d3d1;
  --resume-muted: #78716c;
  font-family:Georgia,'Noto Serif SC','Times New Roman',serif;
  background:#fffdf5;
  border-left:1px solid #e7e5e4;
  border-right:1px solid #e7e5e4;
  box-shadow:0 15px 40px rgba(120,53,15,.1);
}
.a4-paper.template-paper .resume-head { border-bottom:3px double #a8a29e; padding-bottom:22px; text-align:left; }
.a4-paper.template-paper .resume-head h1 { color:#78350f; font-size:2.2em; letter-spacing:.02em; }
.a4-paper.template-paper .resume-contact,.a4-paper.template-paper .resume-profile-line { justify-content:flex-start; }
.a4-paper.template-paper .resume-preview-section { display:grid; grid-template-columns:142px minmax(0,1fr); gap:18px; padding:12px 0; border-bottom:1px solid #e7e5e4; }
.a4-paper.template-paper .resume-preview-section h2 { border-bottom:0; color:#78350f; font-size:1em; }
.a4-paper.template-paper .resume-preview-chips span { background:#fef3c7; color:#92400e; }
</style>
