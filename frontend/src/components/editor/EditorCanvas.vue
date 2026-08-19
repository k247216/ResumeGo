<template>
  <main class="editor-canvas">
    <header class="editor-canvas__toolbar">
      <div class="editor-canvas__title">
        <span>正在编辑</span>
        <h1>{{ activeSection?.title || resumeTitle }}</h1>
      </div>
      <div class="toolbar-actions">
        <div class="toolbar-meta">
          <strong>{{ versionLabel }}</strong>
          <small>{{ dirty ? '有未保存草稿' : updatedAt ? `更新 ${updatedAt}` : '等待版本数据' }}</small>
        </div>
        <button type="button" :disabled="!dirty || saving" @click="$emit('reset-draft')">
          放弃
        </button>
        <button class="save-button" type="button" :disabled="!dirty || saving" @click="$emit('save-draft')">
          {{ saving ? '保存中...' : '保存为新版本' }}
        </button>
      </div>
    </header>

    <section class="editor-focus-area">
      <SectionWrapper
        v-if="activeSection"
        :key="activeSection.id"
        :section="activeSection"
        selected
        @select="$emit('select-section', $event)"
        @update-field="(...args) => $emit('update-field', ...args)"
        @update-paragraph="(...args) => $emit('update-paragraph', ...args)"
        @update-chips="(...args) => $emit('update-chips', ...args)"
        @update-list-item="(...args) => $emit('update-list-item', ...args)"
        @add-list-item="(...args) => $emit('add-list-item', ...args)"
        @remove-list-item="(...args) => $emit('remove-list-item', ...args)"
        @move-list-item="(...args) => $emit('move-list-item', ...args)"
        @add-item="$emit('add-item', $event)"
        @remove-item="(...args) => $emit('remove-item', ...args)"
        @move-item="(...args) => $emit('move-item', ...args)"
        @toggle-visibility="$emit('toggle-visibility', $event)"
        @remove-section="$emit('remove-section', $event)"
      />
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SectionWrapper from './SectionWrapper.vue'
import type { EditorSection } from '../../types/editor'

const props = defineProps<{
  sections: EditorSection[]
  selectedSectionId: string
  resumeTitle: string
  versionLabel: string
  updatedAt?: string | null
  dirty: boolean
  saving: boolean
}>()

defineEmits<{
  (event: 'select-section', sectionId: string): void
  (event: 'update-field', sectionId: string, fieldKey: string, value: string): void
  (event: 'update-paragraph', sectionId: string, index: number, value: string): void
  (event: 'update-chips', sectionId: string, value: string): void
  (event: 'update-list-item', sectionId: string, fieldKey: string, index: number, value: string): void
  (event: 'add-list-item', sectionId: string, fieldKey: string): void
  (event: 'remove-list-item', sectionId: string, fieldKey: string, index: number): void
  (event: 'move-list-item', sectionId: string, fieldKey: string, index: number, direction: 'up' | 'down'): void
  (event: 'add-item', sectionId: string): void
  (event: 'remove-item', sectionId: string, index: number): void
  (event: 'move-item', sectionId: string, index: number, direction: 'up' | 'down'): void
  (event: 'toggle-visibility', sectionId: string): void
  (event: 'remove-section', sectionId: string): void
  (event: 'save-draft'): void
  (event: 'reset-draft'): void
}>()

const activeSection = computed(() => {
  return props.sections.find((section) => section.id === props.selectedSectionId)
    ?? props.sections[0]
    ?? null
})
</script>

<style scoped>
.editor-canvas {
  min-width: 0;
  height: 100%;
  overflow: hidden;
  background: #f6f8fb;
  display: flex;
  flex-direction: column;
}

.editor-canvas__toolbar {
  box-sizing: border-box;
  height: var(--editor-subbar-height, 54px);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #e5eaf2;
  background: rgba(255, 255, 255, 0.9);
  padding: 9px 16px;
}

.editor-canvas__toolbar span,
.toolbar-meta small {
  color: #7c8aa2;
  font-size: 11px;
}

.editor-canvas__title {
  min-width: 0;
}

.editor-canvas__toolbar h1 {
  margin: 1px 0 0;
  overflow: hidden;
  color: #101a33;
  font-size: 16px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-meta {
  display: grid;
  justify-items: end;
  gap: 1px;
}

.toolbar-meta strong {
  border-radius: 999px;
  background: #eaf8f3;
  color: #07875f;
  padding: 4px 8px;
  font-size: 12px;
}

.toolbar-actions button {
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #fff;
  color: #465671;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 6px 10px;
}

.toolbar-actions button:disabled {
  color: #a7b2c2;
  cursor: not-allowed;
}

.toolbar-actions .save-button {
  border-color: #10a878;
  background: #10a878;
  color: #fff;
}

.toolbar-actions .save-button:disabled {
  border-color: #cbd5e1;
  background: #cbd5e1;
}

.editor-focus-area {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 28px 28px 40px;
}
</style>
