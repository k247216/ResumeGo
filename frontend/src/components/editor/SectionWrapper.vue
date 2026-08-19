<template>
  <article
    class="section-wrapper"
    :class="{ selected, muted: section.status === 'empty', 'hidden-in-preview': !section.visible }"
    :data-section-id="section.id"
    @click="$emit('select', section.id)"
  >
    <header class="section-wrapper__header">
      <div class="section-wrapper__title">
        <span class="section-wrapper__handle">⋮⋮</span>
        <div>
          <h3>{{ section.title }}</h3>
          <p>{{ section.subtitle }}</p>
        </div>
      </div>

      <div class="section-wrapper__actions">
        <button
          class="visibility-action"
          type="button"
          :title="section.visible ? '在预览中隐藏' : '在预览中显示'"
          @click.stop="$emit('toggle-visibility', section.id)"
        >
          <el-icon><component :is="section.visible ? View : Hide" /></el-icon>
        </button>
        <button
          v-if="section.id !== 'personal-info'"
          class="remove-section-action"
          type="button"
          title="移除模块"
          @click.stop="$emit('remove-section', section.id)"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </header>

    <div class="section-wrapper__body">
      <div v-if="section.fields.length" class="section-fields">
        <div v-for="field in section.fields" :key="field.key">
          <span>{{ field.label }}</span>
          <select
            v-if="field.control === 'select'"
            :value="field.value"
            :data-field-key="field.key"
            @change="$emit('update-field', section.id, field.key, ($event.target as HTMLSelectElement).value)"
          >
            <option value="">请选择</option>
            <option
              v-for="option in field.options || []"
              :key="optionValue(option)"
              :value="optionValue(option)"
            >
              {{ optionLabel(option) }}
            </option>
          </select>
          <input
            v-else
            :value="field.value"
            :data-field-key="field.key"
            type="text"
            @input="$emit('update-field', section.id, field.key, ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>

      <div v-if="section.chips.length && section.type !== 'skills'" class="section-chips">
        <span v-for="chip in section.chips" :key="chip">{{ chip }}</span>
      </div>

      <div v-if="section.items?.length" class="section-items">
        <div
          v-for="(item, index) in section.items"
          :key="item.id"
          class="section-item-card"
        >
          <div class="section-item-card__head">
            <span class="item-index">#{{ index + 1 }}</span>
            <strong>{{ item.title || `条目 ${index + 1}` }}</strong>
            <div class="section-item-actions">
              <span v-if="item.evidenceLabel">{{ item.evidenceLabel }}</span>
              <button
                type="button"
                title="上移条目"
                @click.stop="$emit('move-item', section.id, index, 'up')"
              >
                ↑
              </button>
              <button
                type="button"
                title="下移条目"
                @click.stop="$emit('move-item', section.id, index, 'down')"
              >
                ↓
              </button>
              <button
                class="danger"
                type="button"
                title="删除条目"
                @click.stop="$emit('remove-item', section.id, index)"
              >
                ×
              </button>
            </div>
          </div>

          <div v-if="item.fields?.length" class="section-fields item-fields">
            <div v-for="field in item.fields" :key="field.key">
              <span>{{ field.label }}</span>
              <select
                v-if="field.control === 'select'"
                :value="field.value"
                :data-field-key="field.key"
                @change="$emit('update-field', section.id, field.key, ($event.target as HTMLSelectElement).value)"
              >
                <option value="">请选择</option>
                <option
                  v-for="option in field.options || []"
                  :key="optionValue(option)"
                  :value="optionValue(option)"
                >
                  {{ optionLabel(option) }}
                </option>
              </select>
              <input
                v-else
                :value="field.value"
                :data-field-key="field.key"
                type="text"
                @input="$emit('update-field', section.id, field.key, ($event.target as HTMLInputElement).value)"
              />
            </div>
          </div>

          <label v-if="item.descriptionKey">
            <span>{{ item.descriptionLabel || '描述' }}</span>
            <textarea
              :value="item.description"
              :data-field-key="item.descriptionKey"
              placeholder="补充可验证的职责、行动和结果"
              @input="$emit('update-field', section.id, item.descriptionKey, ($event.target as HTMLTextAreaElement).value)"
            />
          </label>

          <label
            v-for="field in item.listFields || []"
            :key="field.key"
            class="section-list-editor"
          >
            <div class="section-list-editor__head">
              <span>{{ field.label }}</span>
              <button
                type="button"
                @click.stop="$emit('add-list-item', section.id, field.key)"
              >
                + 新增
              </button>
            </div>
            <div class="section-list-editor__items">
              <div
                v-for="(value, valueIndex) in field.value"
                :key="`${field.key}-${valueIndex}`"
                class="section-list-row"
              >
                <input
                  :value="value"
                  :placeholder="field.placeholder || '输入一项内容'"
                  :data-list-field="field.key"
                  :data-list-index="valueIndex"
                  type="text"
                  @input="$emit('update-list-item', section.id, field.key, valueIndex, ($event.target as HTMLInputElement).value)"
                />
                <div class="section-list-row-actions">
                  <button
                    type="button"
                    title="上移"
                    @click.stop="$emit('move-list-item', section.id, field.key, valueIndex, 'up')"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    title="下移"
                    @click.stop="$emit('move-list-item', section.id, field.key, valueIndex, 'down')"
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    title="删除"
                    @click.stop="$emit('remove-list-item', section.id, field.key, valueIndex)"
                  >
                    ×
                  </button>
                </div>
              </div>
              <button
                v-if="field.value.length === 0"
                class="empty-list-add"
                type="button"
                @click.stop="$emit('add-list-item', section.id, field.key)"
              >
                + 添加第一项
              </button>
            </div>
          </label>
        </div>
        <button
          class="add-item-button"
          type="button"
          @click.stop="$emit('add-item', section.id)"
        >
          + {{ section.addLabel || '添加条目' }}
        </button>
      </div>

      <div v-else-if="section.paragraphs.length" class="section-paragraphs">
        <label
          v-for="(paragraph, index) in section.paragraphs"
          :key="`${section.id}-${index}`"
        >
          <span>{{ section.paragraphLabels?.[index] || `内容 ${index + 1}` }}</span>
          <textarea
            :value="paragraph"
            @input="$emit('update-paragraph', section.id, index, ($event.target as HTMLTextAreaElement).value)"
          />
        </label>
      </div>

      <p v-if="!section.fields.length && !section.items?.length && !section.chips.length && !section.paragraphs.length" class="empty-section">
        当前版本还没有这一段内容，后续可以从证据或 AI 建议中补充。
      </p>
      <button
        v-if="section.addLabel && !section.items?.length"
        class="add-item-button"
        type="button"
        @click.stop="$emit('add-item', section.id)"
      >
        + {{ section.addLabel }}
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { Close, Hide, View } from '@element-plus/icons-vue'
import type { EditorFieldOption, EditorSection } from '../../types/editor'

defineProps<{
  section: EditorSection
  selected: boolean
}>()

defineEmits<{
  (event: 'select', sectionId: string): void
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
}>()

function optionValue(option: EditorFieldOption) {
  return typeof option === 'string' ? option : option.value
}

function optionLabel(option: EditorFieldOption) {
  return typeof option === 'string' ? option : option.label
}
</script>

<style scoped>
.section-wrapper {
  border: 1px solid #eceff5;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 26px rgba(15, 23, 42, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
  cursor: pointer;
}

.section-wrapper:hover {
  border-color: #d7dde8;
  box-shadow: 0 14px 38px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.section-wrapper.selected {
  border-color: #cfd6e2;
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.055);
}

.section-wrapper.muted {
  background: #fff;
}

.section-wrapper.hidden-in-preview {
  opacity: 0.62;
}

.section-wrapper__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.section-wrapper__title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.section-wrapper__handle {
  color: #c4ccda;
  font-size: 16px;
  letter-spacing: -4px;
  transform: rotate(90deg);
}

.section-wrapper h3,
.section-wrapper p {
  margin: 0;
}

.section-wrapper h3 {
  color: #101a33;
  font-size: 15px;
}

.section-wrapper__title p {
  margin-top: 3px;
  color: #8290a7;
  font-size: 12px;
}

.section-wrapper__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.section-wrapper:hover .section-wrapper__actions,
.section-wrapper.selected .section-wrapper__actions {
  opacity: 1;
}

.section-wrapper__actions button {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 0;
}

.visibility-action {
  color: #9aa3b2;
}

.visibility-action:hover {
  background: #f1f5f9;
  color: #334155;
}

.remove-section-action {
  color: #a1a9b8;
}

.remove-section-action:hover {
  background: #fff1f2;
  color: #dc2626;
}

.section-wrapper__body {
  padding: 14px;
}

.section-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.section-fields.item-fields {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.section-fields div {
  border-radius: 12px;
  background: #f8fafc;
  padding: 8px 10px;
}

.section-fields span {
  display: block;
  color: #8b97ac;
  font-size: 12px;
  margin-bottom: 4px;
}

.section-fields input,
.section-fields select {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #17213a;
  font-size: 14px;
  font-weight: 700;
}

.section-fields select {
  cursor: pointer;
  appearance: none;
}

.section-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.section-chips span {
  border: 1px solid #dbe7ff;
  border-radius: 999px;
  background: #f5f9ff;
  color: #30517f;
  font-size: 12px;
  padding: 6px 10px;
}

.section-paragraphs {
  display: grid;
  gap: 10px;
}

.section-items {
  display: grid;
  gap: 10px;
}

.section-item-card {
  display: grid;
  gap: 9px;
  border: 1px solid #ebeff5;
  border-radius: 14px;
  background: #fff;
  padding: 10px;
}

.add-item-button {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 9px 12px;
}

.add-item-button:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #17213a;
}

.section-item-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-item-card__head strong {
  color: #17213a;
  font-size: 13px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-index {
  color: #a1a9b8;
  font-size: 12px;
  font-weight: 900;
}

.section-item-actions {
  margin-left: auto;
}

.section-item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.section-item-card:hover .section-item-actions,
.section-item-card:focus-within .section-item-actions {
  opacity: 1;
}

.section-item-actions span {
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 8px;
}

.section-item-actions button {
  border: 1px solid #dbe3ef;
  border-radius: 9px;
  background: #fff;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  min-width: 26px;
  padding: 4px 7px;
}

.section-item-actions button:hover:not(:disabled) {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #334155;
}

.section-item-actions button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.section-item-actions button.danger {
  color: #dc2626;
}

.section-item-actions button.danger:hover {
  border-color: #fecaca;
  background: #fff1f2;
}

.section-paragraphs label,
.section-item-card label,
.section-list-editor {
  display: grid;
  gap: 6px;
}

.section-paragraphs label > span,
.section-item-card label > span,
.section-list-editor__head > span {
  color: #77849a;
  font-size: 12px;
  font-weight: 700;
}

.section-paragraphs textarea,
.section-item-card textarea,
.section-item-card input,
.section-list-row input {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  outline: 0;
  background: #fbfdff;
  padding: 8px 10px;
  color: #40506b;
  font-size: 13px;
  line-height: 1.7;
}

.section-paragraphs textarea,
.section-item-card textarea {
  min-height: 72px;
  resize: vertical;
}

.section-item-card input {
  min-height: 36px;
}

.section-list-editor__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.section-list-editor__head button,
.empty-list-add {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 5px 9px;
}

.section-list-editor__head button:hover,
.empty-list-add:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #17213a;
}

.section-list-editor__items {
  display: grid;
  gap: 7px;
}

.section-list-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 7px;
}

.section-list-row input {
  min-height: 36px;
  background: #fff;
}

.section-list-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.section-list-row:hover .section-list-row-actions,
.section-list-row:focus-within .section-list-row-actions {
  opacity: 1;
}

.section-list-row button {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  min-width: 28px;
  padding: 0 8px;
}

.section-list-row button:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #334155;
}

.section-list-row button:last-child:hover {
  border-color: #fecaca;
  background: #fff1f2;
  color: #dc2626;
}

.section-paragraphs textarea:focus,
.section-item-card textarea:focus,
.section-item-card input:focus,
.section-list-row input:focus,
.section-item-card select:focus,
.section-fields input:focus,
.section-fields select:focus {
  border-color: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.14);
}

.empty-section {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 980px) {
  .section-fields {
    grid-template-columns: 1fr;
  }
}
</style>
