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
            <h1>{{ basicName }}</h1>
            <div class="resume-contact">
              <span>{{ targetRole }}</span>
              <span v-if="contactLine">{{ contactLine }}</span>
            </div>
            <div v-if="profileLine" class="resume-profile-line">
              {{ profileLine }}
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
                <span>{{ field.value || '待补充' }}</span>
              </p>
            </div>

            <div v-if="section.chips.length" class="resume-preview-chips">
              <span v-for="chip in section.chips" :key="chip">{{ chip }}</span>
            </div>

            <div v-if="section.items?.length" class="resume-preview-paragraphs project-preview-list">
              <div
                v-for="item in section.items"
                :key="item.id"
                class="resume-preview-item"
              >
                <div class="resume-preview-item__head">
                  <strong>{{ previewItemTitle(section, item) }}</strong>
                  <span v-if="previewItemTime(section, item)">{{ previewItemTime(section, item) }}</span>
                </div>

                <div v-if="previewItemMeta(section, item)" class="resume-preview-item-meta">
                  {{ previewItemMeta(section, item) }}
                </div>

                <div v-if="visibleFieldsForItem(section, item).length" class="resume-preview-item-fields">
                  <span
                    v-for="field in visibleFieldsForItem(section, item)"
                    :key="field.key"
                  >
                    <strong>{{ field.label }}：</strong>{{ field.value }}
                  </span>
                </div>

                <p v-if="item.description">
                  {{ item.description }}
                </p>

                <div
                  v-for="field in visibleListFields(item.listFields)"
                  :key="field.key"
                  class="resume-preview-list-field"
                >
                  <strong>{{ field.label }}：</strong>
                  <span v-for="value in visibleListValues(field.value)" :key="value">{{ value }}</span>
                </div>
              </div>
            </div>

            <div v-else-if="section.paragraphs.length" class="resume-preview-paragraphs">
              <p
                v-for="(paragraph, index) in section.paragraphs"
                :key="`${section.id}-${index}`"
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

const props = defineProps<{
  sections: EditorSection[]
  selectedSectionId: string
  versionLabel: string
  templateStyle?: string
  /** 荧光笔高亮：内容有变化的章节（琥珀扫过） */
  highlightSectionIds?: string[]
  /** 新增章节（绿色扫过） */
  addedSectionIds?: string[]
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

/* 荧光笔高亮：修改=琥珀扫过；新增=绿色扫过（章节级，不伪造词级 diff） */
.resume-preview-section.diff-modified {
  background: linear-gradient(120deg, rgba(217, 119, 6, 0.14), rgba(217, 119, 6, 0.05));
  box-shadow: inset 3px 0 0 rgba(217, 119, 6, 0.45);
  border-radius: 4px;
}

.resume-preview-section.diff-added {
  background: linear-gradient(120deg, rgba(22, 139, 104, 0.13), rgba(22, 139, 104, 0.04));
  box-shadow: inset 3px 0 0 rgba(22, 139, 104, 0.45);
  border-radius: 4px;
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

.a4-paper.template-royal { --resume-accent: #6d28d9; --resume-rule: #7c3aed; --resume-muted: #8b7ab8; }
.a4-paper.template-steel { --resume-accent: #0e7490; --resume-rule: #0891b2; --resume-muted: #6b8a94; }
.a4-paper.template-wine { --resume-accent: #9f1239; --resume-rule: #be123c; --resume-muted: #b07a8a; }
.a4-paper.template-navy { --resume-accent: #1e3a8a; --resume-rule: #1d4ed8; --resume-muted: #7a86ad; }
.a4-paper.template-forest { --resume-accent: #166534; --resume-rule: #15803d; --resume-muted: #6b8f77; }
.a4-paper.template-slate { --resume-accent: #334155; --resume-rule: #475569; --resume-muted: #84909e; }
.a4-paper.template-rose { --resume-accent: #db2777; --resume-rule: #ec4899; --resume-muted: #c48ba6; }
.a4-paper.template-ocean { --resume-accent: #0891b2; --resume-rule: #06b6d4; --resume-muted: #74a8b5; }
.a4-paper.template-amber { --resume-accent: #b45309; --resume-rule: #d97706; --resume-muted: #b3926a; }
.a4-paper.template-nord { --resume-accent: #3d5a73; --resume-rule: #4b6584; --resume-muted: #8496a5; }

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
</style>
