<template>
  <aside class="editor-sidebar" :class="{ collapsed }">
    <div class="sidebar-scroll">
      <div class="sidebar-topline">
        <div class="sidebar-title">简历模块</div>
        <button
          class="sidebar-collapse-button"
          type="button"
          :title="collapsed ? '展开模块栏' : '收起模块栏'"
          @click="$emit('toggle-collapsed')"
        >
          {{ collapsed ? '›' : '‹' }}
        </button>
      </div>

      <nav class="module-list" aria-label="简历模块">
        <button
          v-for="section in sections"
          :key="section.id"
          class="sidebar-section"
          :class="{ active: selectedSectionId === section.id, hidden: !section.visible }"
          type="button"
          :title="section.title"
          @click="$emit('select-section', section.id)"
        >
          <el-icon class="section-icon">
            <component :is="iconOf(section.type)" />
          </el-icon>
          <span class="section-name">{{ section.title }}</span>
          <span class="section-actions" @click.stop>
            <button
              type="button"
              title="上移模块"
              @click="$emit('move-module', section.id, 'up')"
            >
              ↑
            </button>
            <button
              type="button"
              title="下移模块"
              @click="$emit('move-module', section.id, 'down')"
            >
              ↓
            </button>
            <button
              v-if="section.id !== 'personal-info'"
              class="danger"
              type="button"
              title="移除模块"
              @click="$emit('remove-module', section.id)"
            >
              ×
            </button>
          </span>
        </button>
      </nav>

      <template v-if="availableModules.length">
        <div class="sidebar-separator"></div>
        <div class="sidebar-title compact">添加模块</div>
        <nav class="module-list" aria-label="添加模块">
          <button
            v-for="module in availableModules"
            :key="module.id"
            class="sidebar-section addable"
            type="button"
            :title="`添加${module.title}`"
            @click="$emit('add-module', module.id)"
          >
            <el-icon class="section-icon">
              <component :is="iconOf(module.type)" />
            </el-icon>
            <span class="section-name">{{ module.title }}</span>
            <el-icon class="add-icon"><Plus /></el-icon>
          </button>
        </nav>
      </template>
    </div>

    <div class="sidebar-footer">
      <label
        class="sidebar-version-card"
        :class="{ locked: dirty }"
        :title="dirty ? '当前有未保存草稿，保存或放弃后才能切换版本' : '切换简历版本'"
      >
        <span class="version-card-label">当前版本</span>
        <strong>{{ currentVersion ? `v${currentVersion.versionNo}` : 'v-' }}</strong>
        <small>{{ formatDate(currentVersion?.createdAt) || '暂无时间' }}</small>
        <span class="version-card-arrow">⌄</span>
        <select
          :value="selectedVersionId ?? ''"
          :disabled="dirty || versions.length === 0"
          @change="$emit('switch-version', Number(($event.target as HTMLSelectElement).value))"
        >
          <option
            v-for="version in versions"
            :key="version.id"
            :value="version.id"
          >
            v{{ version.versionNo }} · {{ formatDate(version.createdAt) || '未知时间' }}
          </option>
        </select>
      </label>
      <p v-if="dirty" class="sidebar-version-hint">保存或放弃后可切换</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Avatar,
  Briefcase,
  ChatLineRound,
  Collection,
  Document,
  FolderOpened,
  Grid,
  Link,
  Medal,
  MostlyCloudy,
  Plus,
  School,
  Tools,
} from '@element-plus/icons-vue'
import type { EditorModuleOption, EditorSection } from '../../types/editor'
import type { ResumeVersion } from '../../types/resume'

const props = defineProps<{
  sections: EditorSection[]
  availableModules: EditorModuleOption[]
  selectedSectionId: string
  versions: ResumeVersion[]
  selectedVersionId: number | null
  dirty: boolean
  collapsed: boolean
}>()

defineEmits<{
  (event: 'select-section', sectionId: string): void
  (event: 'add-module', sectionId: string): void
  (event: 'remove-module', sectionId: string): void
  (event: 'move-module', sectionId: string, direction: 'up' | 'down'): void
  (event: 'switch-version', versionId: number): void
  (event: 'toggle-collapsed'): void
}>()

const currentVersion = computed(() => (
  props.versions.find((version) => version.id === props.selectedVersionId) ?? null
))

function iconOf(type: EditorSection['type']) {
  if (type === 'personal_info') return Avatar
  if (type === 'summary') return Document
  if (type === 'work_experience') return Briefcase
  if (type === 'education') return School
  if (type === 'skills') return Tools
  if (type === 'projects') return FolderOpened
  if (type === 'certifications') return Medal
  if (type === 'languages') return ChatLineRound
  if (type === 'github') return Link
  if (type === 'qr_codes') return Grid
  if (type === 'custom') return Collection
  return MostlyCloudy
}

function formatDate(value?: string | null) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

</script>

<style scoped>
.editor-sidebar {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--line, #e7eaf0);
  background: var(--surface-solid, #ffffff);
  padding: 18px 0 12px;
}

.editor-sidebar *,
.editor-sidebar *::before,
.editor-sidebar *::after {
  box-sizing: border-box;
}

.sidebar-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 14px;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 0;
}

.sidebar-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 10px 12px 18px;
}

.sidebar-title {
  color: var(--muted, #9aa3b2);
  font-size: 13px;
  font-weight: 800;
  padding: 0;
}

.sidebar-title.compact {
  padding: 14px 18px 10px;
  padding-bottom: 10px;
}

.sidebar-collapse-button {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: var(--surface, #f4f6f9);
  color: var(--muted, #707989);
  cursor: pointer;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  transition: background 0.16s ease, color 0.16s ease;
}

.sidebar-collapse-button:hover {
  background: var(--surface, #e9eef5);
  color: var(--ink, #101a33);
}

.module-list {
  display: grid;
  gap: 4px;
}

.sidebar-section {
  width: 100%;
  height: 44px;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--copy, #5f6572);
  cursor: pointer;
  font-size: 16px;
  padding: 0 18px;
  text-align: left;
  transition: background 0.16s ease, color 0.16s ease;
}

.sidebar-section:hover,
.sidebar-section.active {
  background: var(--surface, #f7f8fb);
  color: var(--ink, #161d2f);
}

.sidebar-section.active {
  box-shadow: inset 3px 0 0 var(--brand, #10a878);
}

.sidebar-section.hidden {
  opacity: 0.48;
}

.section-icon,
.section-plus {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 8px;
  color: var(--muted, #697181);
  background: var(--surface, #f3f5f8);
  font-size: 13px;
  font-weight: 900;
}

.sidebar-section.addable {
  color: var(--copy, #5f6572);
  font-size: 16px;
}

.add-icon {
  color: var(--muted, #9ca3af);
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.sidebar-section.addable:hover .add-icon {
  opacity: 1;
}

.section-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.sidebar-section:hover .section-actions,
.sidebar-section.active .section-actions {
  opacity: 1;
}

.section-actions button {
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted, #9ca3af);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}

.section-actions button:hover {
  background: var(--surface, #eef2f7);
  color: var(--copy, #475569);
}

.section-actions button.danger:hover {
  background: var(--danger-soft, #fff1f2);
  color: var(--danger, #dc2626);
}

.sidebar-separator {
  height: 1px;
  margin: 14px 12px 0;
  background: var(--line, #edf0f5);
}

.section-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-footer {
  flex: 0 0 auto;
  margin-top: auto;
  background: var(--surface-solid, #ffffff);
  padding: 10px 12px 0;
}

.sidebar-version-card {
  position: relative;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 60px;
  display: grid;
  grid-template-areas:
    "label arrow"
    "version arrow"
    "time arrow";
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: 11px 21px 14px;
  align-items: start;
  gap: 1px 8px;
  overflow: hidden;
  border: 1px solid var(--line, #e5eaf2);
  border-radius: 16px;
  background:
    radial-gradient(circle at 12% 18%, rgba(16, 185, 129, 0.12), transparent 34%),
    var(--surface-solid, #fff);
  color: var(--copy, #334155);
  cursor: pointer;
  padding: 7px 10px;
}

.version-card-label {
  grid-area: label;
  color: var(--muted, #94a3b8);
  font-size: 10px;
  font-weight: 900;
  line-height: 11px;
  letter-spacing: 0.04em;
}

.sidebar-version-card strong {
  grid-area: version;
  color: var(--ink, #0f172a);
  font-size: 19px;
  line-height: 21px;
  letter-spacing: -0.04em;
}

.sidebar-version-card small {
  grid-area: time;
  min-width: 0;
  overflow: hidden;
  color: var(--muted, #9aa3b2);
  font-size: 11px;
  font-weight: 700;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-card-arrow {
  grid-area: arrow;
  align-self: center;
  color: var(--muted, #94a3b8);
  font-size: 14px;
}

.sidebar-version-card select {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  opacity: 0;
  cursor: pointer;
}

.sidebar-version-card.locked {
  background: var(--surface, #f8fafc);
  cursor: not-allowed;
}

.sidebar-version-card.locked strong,
.sidebar-version-card.locked small,
.sidebar-version-card.locked .version-card-label,
.sidebar-version-card.locked .version-card-arrow {
  color: var(--muted, #a1a9b8);
}

.sidebar-version-card.locked select {
  cursor: not-allowed;
}

.sidebar-footer p {
  margin: 7px 9px 0;
  color: var(--muted, #a1a9b8);
  font-size: 11px;
  line-height: 1.4;
}

.editor-sidebar.collapsed {
  padding-top: 14px;
  padding-bottom: 10px;
}

.editor-sidebar.collapsed .sidebar-topline {
  justify-content: center;
  padding: 0 0 10px;
}

.editor-sidebar.collapsed .sidebar-title,
.editor-sidebar.collapsed .sidebar-title.compact,
.editor-sidebar.collapsed .section-name,
.editor-sidebar.collapsed .section-actions,
.editor-sidebar.collapsed .add-icon,
.editor-sidebar.collapsed .sidebar-separator,
.editor-sidebar.collapsed .sidebar-footer {
  display: none;
}

.editor-sidebar.collapsed .sidebar-collapse-button {
  width: 36px;
  height: 34px;
  border-radius: 12px;
  background: var(--surface, #f7f8fb);
}

.editor-sidebar.collapsed .module-list {
  justify-items: center;
  gap: 6px;
}

.editor-sidebar.collapsed .sidebar-section {
  width: 44px;
  height: 42px;
  grid-template-columns: 1fr;
  justify-items: center;
  gap: 0;
  border-radius: 14px;
  padding: 0;
}

.editor-sidebar.collapsed .sidebar-section.active {
  box-shadow: inset 3px 0 0 var(--brand, #10a878);
}

.editor-sidebar.collapsed .section-icon {
  width: 30px;
  height: 30px;
  border-radius: 12px;
  font-size: 14px;
}

@media (max-width: 1280px) {
  .editor-sidebar {
    padding-top: 16px;
  }

  .sidebar-topline {
    padding-right: 8px;
    padding-left: 14px;
  }

  .sidebar-title.compact {
    padding-right: 14px;
    padding-left: 14px;
  }

  .sidebar-section {
    gap: 7px;
    font-size: 15px;
    padding-right: 14px;
    padding-left: 14px;
  }

  .sidebar-footer {
    padding-right: 8px;
    padding-left: 8px;
  }

  .sidebar-version-card {
    border-radius: 14px;
    padding-right: 9px;
    padding-left: 9px;
  }
}

</style>
