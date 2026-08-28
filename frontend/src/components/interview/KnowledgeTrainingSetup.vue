<template>
  <div class="knowledge-setup" data-test="knowledge-training-setup">
    <div class="knowledge-workspace">
      <section class="knowledge-source-center">
        <header class="source-center-head">
          <div>
            <span class="setup-kicker">Step 1 · Source</span>
            <h3>选择知识资料</h3>
            <p>{{ draft.knowledgeDocumentIds.length }} 份已选 <span>·</span> 从你的笔记中建立本次训练上下文</p>
          </div>
        </header>

        <div class="source-path-row">
          <div class="folder-picker">
            <button type="button" class="source-breadcrumb-button" data-test="knowledge-folder-picker" @click="folderMenuOpen = !folderMenuOpen"><el-icon><Folder /></el-icon><span v-for="(part, index) in selectedCategoryPath" :key="`${part}-${index}`" class="breadcrumb-part" :class="{ 'path-current': index === selectedCategoryPath.length - 1 }">{{ part }}</span><el-icon class="breadcrumb-chevron"><ArrowDown /></el-icon></button>
            <div v-if="folderMenuOpen" class="folder-menu" data-test="knowledge-folder-menu">
              <button type="button" class="folder-option" :class="{ selected: props.selectedCategoryId == null }" data-test="knowledge-folder-option-all" @click="selectCategory(null)"><el-icon><Folder /></el-icon><span>全部资料</span></button>
              <button v-for="category in categoryOptions" :key="category.id" type="button" class="folder-option" :class="{ selected: props.selectedCategoryId === category.id }" :data-test="`knowledge-folder-option-${category.id}`" :style="{ paddingLeft: `${10 + category.depth * 14}px` }" @click="selectCategory(category.id)"><el-icon><Folder /></el-icon><span>{{ category.name }}</span><small>{{ category.descendantDocumentCount }}</small></button>
              <p v-if="!categoryOptions.length" class="folder-menu-empty">知识库还没有文件夹，先在知识库中创建分类。</p>
            </div>
          </div>
          <label class="source-search"><el-icon><Search /></el-icon><input v-model="searchQuery" data-test="knowledge-search" type="search" placeholder="搜索文件或文件夹" /></label>
        </div>

        <div class="source-toolbar">
          <span>全部资料 <strong>{{ filteredOptions.length }}</strong></span>
          <div class="source-toolbar-actions"><span>按最近修改</span><button type="button" aria-label="列表视图"><el-icon><List /></el-icon></button><button type="button" aria-label="网格视图"><el-icon><Grid /></el-icon></button></div>
        </div>

        <div class="document-list" data-test="knowledge-document-grid">
          <div class="document-list-head"><span>名称</span><span>类型</span><span>标签</span><span>修改时间</span><span>大小</span></div>
          <button v-for="item in filteredOptions" :key="item.value" type="button" class="document-row" :class="{ selected: isSelected(item.value), disabled: item.disabled }" :disabled="item.disabled" @click="toggleDocument(item)">
            <span class="document-name"><span class="document-check" :class="{ checked: isSelected(item.value) }"><el-icon v-if="isSelected(item.value)"><Check /></el-icon></span><span class="file-mark" :data-file-type="item.fileType || 'FILE'">{{ item.fileType || 'FILE' }}</span><strong>{{ item.label }}</strong></span>
            <span class="document-type">{{ item.fileType || '文件' }}</span>
            <span class="document-tags">{{ item.description || '本地资料' }}</span>
            <span class="document-updated">{{ formatDate(item.updatedAt) }}</span>
            <span class="document-size">{{ item.fileSize || '—' }}</span>
          </button>
          <div v-if="!filteredOptions.length" class="document-empty"><el-icon><Search /></el-icon><strong>没有匹配的资料</strong><span>换个关键词，或先在知识库导入一份笔记。</span></div>
        </div>

        <section class="training-settings">
          <div class="inspector-section-head"><span class="setup-kicker">Step 2 · Focus</span><h3>提问策略</h3></div>
          <div class="setting-block difficulty-block">
            <div class="setting-label"><span>提问深度</span><strong>{{ difficultyLabels[difficultyIndex] }}</strong></div>
            <input class="range-control" type="range" min="0" max="4" step="1" :value="difficultyIndex" aria-label="提问深度" @input="onDifficultyInput" />
            <div class="range-labels"><span v-for="label in difficultyLabels" :key="label">{{ label }}</span></div><small class="setting-description">{{ difficultyDescription }}</small>
          </div>
          <div class="setting-block style-block">
            <div class="setting-label"><span>提问风格</span><small>选择一种节奏</small></div>
            <div class="style-options" data-test="knowledge-question-style">
              <button v-for="style in stylePresets" :key="style.label" type="button" :data-test="`knowledge-style-${style.label}`" :class="{ active: props.draft.questionStyle === style.label }" @click="selectQuestionStyle(style.label)">
                <strong>{{ style.label }}</strong><small>{{ style.description }}</small>
              </button>
            </div>
          </div>
          <div class="setting-block">
            <div class="setting-label"><span>题目数量</span><small>本次预计</small></div>
            <div class="count-stepper" data-test="knowledge-question-count"><button type="button" aria-label="减少题目" @click="adjustQuestionCount(-1)"><el-icon><Minus /></el-icon></button><strong>{{ draft.questionCount }}</strong><span>题</span><button type="button" aria-label="增加题目" @click="adjustQuestionCount(1)"><el-icon><Plus /></el-icon></button></div>
          </div>
          <div class="setting-block focus-block"><div class="setting-label"><span>训练重点</span><small>可选</small></div><div class="focus-chips"><button v-for="tag in focusPresets" :key="tag" type="button" :class="{ active: draft.focusTags.includes(tag) }" @click="toggleFocus(tag)">{{ tag }}</button></div><input :value="draft.focusTags.join('、')" data-test="knowledge-focus" type="text" placeholder="输入自定义重点，用顿号分隔" @input="onFocusInput" /></div>
          <label class="setting-block supplement-block"><span class="setting-label"><span>补充说明</span><small>可选</small></span><textarea :value="draft.supplement" data-test="knowledge-supplement" rows="2" maxlength="240" placeholder="例如：结合项目场景解释缓存一致性" @input="onSupplementInput"></textarea></label>
          <p class="source-boundary"><el-icon><Lock /></el-icon>回答只引用已选资料；没有依据时会明确标注。</p>
        </section>
      </section>

      <aside class="knowledge-inspector" data-test="knowledge-preview">
        <section class="source-preview-panel">
          <div class="inspector-head"><div><span class="setup-kicker">Preview</span><h3>{{ preview?.title || '资料预览' }}</h3></div><div v-if="preview" class="preview-head-actions"><span class="preview-type">{{ preview.typeLabel }}</span><button type="button" class="open-knowledge-button" data-test="knowledge-open-document" @click="openKnowledgeDocument">在知识库中打开</button></div></div>
          <div v-if="previewLoading" class="preview-state">正在读取资料正文…</div>
          <template v-else-if="preview">
            <div class="preview-tabs" role="tablist"><button type="button" :class="{ active: previewTab === 'preview' }" @click="previewTab = 'preview'">预览</button><button type="button" :class="{ active: previewTab === 'outline' }" @click="previewTab = 'outline'">提取大纲</button></div>
            <div class="preview-content"><div class="preview-meta">{{ preview.meta }}</div><pre v-if="previewTab === 'preview'">{{ preview.content }}</pre><div v-else class="preview-outline"><span v-for="line in previewOutline" :key="line">{{ line }}</span><small v-if="!previewOutline.length">这份资料暂时没有可提取的标题。</small></div></div>
          </template>
          <div v-else class="preview-state"><el-icon><Document /></el-icon><strong>选择资料查看正文</strong><span>预览只用于确认来源，不会改变原文件。</span></div>
        </section>

        <div class="selected-strip">
          <div class="selected-strip-head"><span>已选资料</span><small>{{ selectedOptions.length }} 份</small></div>
          <span v-if="!selectedOptions.length" class="selected-strip-empty">选择一份或多份资料后，会在这里形成上下文。</span>
          <div v-for="item in selectedOptions" :key="item.value" class="selected-chip">
            <button type="button" class="selected-chip-main" @click="previewDocument(item.value)">{{ item.fileType || 'FILE' }} · {{ item.label }}</button>
            <button type="button" class="selected-chip-remove" :aria-label="`取消选择 ${item.label}`" @click="removeDocument(item.value)"><el-icon><Close /></el-icon></button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown, Check, Close, Document, Folder, Grid, List, Lock, Minus, Plus, Search } from '@element-plus/icons-vue'
import type { KnowledgeTrainingDraft } from '../../composables/useInterviewComposer'
import { clampQuestionCount } from '../../composables/useInterviewComposer'
import type { KnowledgeCategoryNode } from '../../types/knowledge'
import type { SelectOption } from './RoleBasedSetup.vue'

type DocumentPreview = { id: number; title: string; typeLabel: string; meta: string; content: string }

const props = defineProps<{
  draft: KnowledgeTrainingDraft
  documentOptions: SelectOption[]
  categoryOptions?: KnowledgeCategoryNode[]
  selectedCategoryId?: number | null
  preview?: DocumentPreview | null
  previewLoading?: boolean
}>()
const emit = defineEmits<{
  'update:draft': [draft: KnowledgeTrainingDraft]
  'preview-document': [id: number]
  'select-category': [id: number | null]
  'open-knowledge-document': [id: number]
}>()

const searchQuery = ref('')
const previewTab = ref<'preview' | 'outline'>('preview')
const folderMenuOpen = ref(false)
const difficultyLabels = ['入门', '基础', '进阶', '深入', '专家']
const difficultyDescriptions = ['识别概念和术语', '解释原理与常见用法', '结合场景进行取舍', '分析源码、边界和故障', '综合设计、反例与权衡']
const stylePresets = [
  { label: '结构化', description: '逐层拆解，适合系统复习' },
  { label: '追问型', description: '围绕回答连续深挖' },
  { label: '案例型', description: '以真实场景检验应用' },
]
const focusPresets = ['基础概念', '原理理解', '场景应用', '源码与设计']

function patch(value: Partial<KnowledgeTrainingDraft>) { emit('update:draft', { ...props.draft, ...value }) }
function isSelected(id: number) { return props.draft.knowledgeDocumentIds.includes(id) }
function toggleDocument(item: SelectOption) {
  if (item.disabled) return
  const next = isSelected(item.value) ? props.draft.knowledgeDocumentIds.filter((id) => id !== item.value) : [...props.draft.knowledgeDocumentIds, item.value]
  patch({ knowledgeDocumentIds: next })
  emit('preview-document', item.value)
}
function removeDocument(id: number) {
  patch({ knowledgeDocumentIds: props.draft.knowledgeDocumentIds.filter((item) => item !== id) })
}
function previewDocument(id: number) { emit('preview-document', id) }
function splitTags(value: string) { return value.split(/[，,、]/).map((item) => item.trim()).filter(Boolean).slice(0, 6) }
function onFocusInput(event: Event) { patch({ focusTags: splitTags((event.target as HTMLInputElement).value) }) }
function onSupplementInput(event: Event) { patch({ supplement: (event.target as HTMLTextAreaElement).value }) }
function onDifficultyInput(event: Event) { patch({ difficulty: difficultyLabels[Number((event.target as HTMLInputElement).value)] || difficultyLabels[1] }) }
function selectQuestionStyle(style: string) { patch({ questionStyle: style }) }
function adjustQuestionCount(delta: number) { patch({ questionCount: clampQuestionCount('KNOWLEDGE_TRAINING', props.draft.questionCount + delta) }) }
function toggleFocus(tag: string) { patch({ focusTags: (props.draft.focusTags.includes(tag) ? props.draft.focusTags.filter((item) => item !== tag) : [...props.draft.focusTags, tag]).slice(0, 6) }) }
function selectCategory(id: number | null) { folderMenuOpen.value = false; emit('select-category', id) }
function openKnowledgeDocument() { if (props.preview) emit('open-knowledge-document', props.preview.id) }
function formatDate(value?: string) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value.slice(0, 10) : date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) }
const selectedOptions = computed(() => props.documentOptions.filter((item) => isSelected(item.value)))
const previewOutline = computed(() => props.preview?.content.split('\n').map((line) => line.trim()).filter((line) => /^#{1,3}\s|^\d+[.、)]\s/.test(line)).slice(0, 8) ?? [])
const difficultyIndex = computed(() => Math.max(0, difficultyLabels.indexOf(props.draft.difficulty)))
const difficultyDescription = computed(() => difficultyDescriptions[difficultyIndex.value] || difficultyDescriptions[0])
const categoryOptions = computed(() => props.categoryOptions ?? [])
const selectedCategory = computed(() => categoryOptions.value.find((item) => item.id === props.selectedCategoryId) ?? null)
const selectedCategoryPath = computed(() => {
  if (!selectedCategory.value) return ['本地知识库', '全部资料']
  const byId = new Map(categoryOptions.value.map((item) => [item.id, item]))
  const path = [selectedCategory.value.name]
  let current = selectedCategory.value
  const seen = new Set<number>([current.id])
  while (current.parentId != null && !seen.has(current.parentId)) {
    const parent = byId.get(current.parentId)
    if (!parent) break
    path.unshift(parent.name)
    seen.add(parent.id)
    current = parent
  }
  return ['本地知识库', ...path]
})
const filteredOptions = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return props.documentOptions.filter((item) => {
    const matchesQuery = !query || `${item.label} ${item.description || ''} ${item.meta || ''}`.toLowerCase().includes(query)
    return matchesQuery
  })
})
</script>

<style scoped>
.knowledge-setup{width:100%;color:var(--ink)}
.knowledge-workspace{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(360px,.9fr);min-height:550px;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}
.source-center-head h3,.inspector-head h3,.inspector-section-head h3{margin:3px 0 0;color:var(--ink);font-size:18px;letter-spacing:-.03em}.setup-kicker{color:var(--muted);font-size:9.5px;font-weight:750;letter-spacing:.12em;text-transform:uppercase}
.knowledge-source-center{min-width:0;padding:22px 22px 18px}.source-center-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.source-center-head p{margin:6px 0 0;color:var(--muted);font-size:10.5px}.source-center-head p span{padding:0 3px}.source-path-row{display:flex;align-items:center;gap:16px;margin-top:20px}.source-breadcrumb-button{display:inline-flex;align-items:center;gap:8px;min-width:0;padding:8px 10px;border:1px solid var(--border-subtle);border-radius:7px;background:var(--surface-solid,#fff);color:var(--copy);font-size:10.5px;white-space:nowrap;cursor:pointer}.source-breadcrumb-button:hover{border-color:var(--ink);color:var(--ink)}.source-breadcrumb-button .el-icon:first-child{color:var(--brand)}.source-breadcrumb-button b{color:var(--muted);font-weight:400}.source-breadcrumb-button strong{color:var(--ink);font-weight:650}.source-breadcrumb-button .breadcrumb-chevron{margin-left:3px;color:var(--muted)}.source-search{display:flex;align-items:center;gap:7px;min-width:180px;flex:1;border:1px solid var(--border-subtle);border-radius:7px;padding:7px 9px;color:var(--muted)}.source-search:focus-within{border-color:var(--ink)}.source-search input{width:100%;border:0;outline:0;background:transparent;color:var(--ink);font:inherit;font-size:11px}.source-toolbar{display:flex;align-items:center;justify-content:space-between;margin:24px 0 8px;color:var(--muted);font-size:10.5px}.source-toolbar strong{color:var(--ink);font-weight:650}.source-toolbar-actions{display:flex;align-items:center;gap:7px}.source-toolbar-actions button{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-subtle);border-radius:5px;background:transparent;color:var(--copy);cursor:pointer}.source-toolbar-actions button:hover{border-color:var(--ink)}
.document-list{border-top:1px solid var(--border-subtle)}.document-list-head,.document-row{display:grid;grid-template-columns:minmax(190px,1.5fr) 68px minmax(100px,1fr) 74px 68px;align-items:center;column-gap:10px}.document-list-head{padding:8px 7px;color:var(--muted);font-size:9.5px}.document-row{width:100%;padding:10px 7px;border:0;border-bottom:1px solid color-mix(in srgb,var(--border-subtle) 72%,transparent);background:transparent;color:var(--copy);font:inherit;text-align:left;cursor:pointer}.document-row:hover:not(:disabled){background:var(--surface-muted,#f8f8f5)}.document-row.selected{background:var(--brand-soft,#eff8f0)}.document-row.disabled{cursor:default;opacity:.55}.document-name{display:flex;align-items:center;gap:8px;min-width:0}.document-check{display:grid;place-items:center;width:14px;height:14px;border:1px solid var(--border-subtle);border-radius:3px;color:#fff;flex:none}.document-check.checked{border-color:var(--brand);background:var(--brand)}.document-check .el-icon{font-size:11px}.file-mark{display:grid;place-items:center;width:25px;height:25px;border:1px solid var(--border-subtle);border-radius:5px;background:var(--surface-solid,#fff);color:var(--ink);font-size:8px;font-weight:750;flex:none}.file-mark[data-file-type="PDF"]{color:#b94535}.file-mark[data-file-type="MD"]{color:#2f6e43}.document-name strong{overflow:hidden;color:var(--ink);font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.document-type,.document-tags,.document-updated,.document-size{overflow:hidden;color:var(--muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.document-empty{display:grid;justify-items:center;gap:7px;padding:60px 10px;color:var(--muted);font-size:11px}.document-empty .el-icon{font-size:20px}.document-empty strong{color:var(--ink);font-size:12px}.demo-note{margin:12px 7px 0;color:var(--muted);font-size:10px;line-height:1.5}.demo-note span{margin-right:6px;color:var(--ink);font-weight:650}.selected-strip{display:flex;align-items:center;flex-wrap:wrap;gap:7px;margin-top:18px;padding-top:14px;border-top:1px solid var(--border-subtle)}.selected-strip-label{color:var(--ink);font-size:10.5px;font-weight:650}.selected-strip-empty{color:var(--muted);font-size:10px}.selected-chip{display:flex;align-items:center;gap:4px;max-width:220px;border:1px solid var(--border-subtle);border-radius:5px;background:var(--surface-muted,#f7f7f4);padding:5px 7px;color:var(--copy);font-size:9.5px;cursor:pointer}.selected-chip:hover{border-color:var(--ink)}.selected-chip .el-icon{color:var(--muted);font-size:11px}
.knowledge-inspector{display:grid;align-content:start;gap:0;padding:22px 0 20px 20px;border-left:1px solid var(--border-subtle)}.source-preview-panel{display:grid;gap:12px;min-height:226px;padding-bottom:18px;border-bottom:1px solid var(--border-subtle)}.inspector-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.inspector-head h3{overflow:hidden;max-width:300px;font-size:14px;text-overflow:ellipsis;white-space:nowrap}.preview-type{color:var(--muted);font-size:9px;font-weight:750}.preview-tabs{display:flex;gap:20px;border-bottom:1px solid var(--border-subtle)}.preview-tabs button{position:relative;padding:0 0 9px;border:0;background:transparent;color:var(--muted);font-size:10.5px;cursor:pointer}.preview-tabs button.active{color:var(--ink);font-weight:650}.preview-tabs button.active::after{position:absolute;right:0;bottom:-1px;left:0;height:2px;background:var(--brand);content:''}.preview-meta{color:var(--muted);font-size:10px}.preview-content{display:grid;gap:8px;min-height:140px}.preview-content pre{max-height:176px;margin:0;overflow:auto;white-space:pre-wrap;color:var(--copy);font:inherit;font-size:10.5px;line-height:1.65}.preview-outline{display:grid;gap:7px;max-height:176px;overflow:auto;color:var(--copy);font-size:10.5px;line-height:1.5}.preview-outline span{display:block}.preview-outline span::first-letter{color:var(--brand)}.preview-outline small{color:var(--muted)}.preview-state{display:grid;align-content:center;justify-items:start;gap:7px;min-height:150px;color:var(--muted);font-size:10.5px;line-height:1.5}.preview-state strong{color:var(--ink);font-size:11.5px}.preview-state .el-icon{font-size:23px;color:var(--ink)}.training-settings{display:grid;gap:17px;padding-top:18px}.inspector-section-head{display:grid;gap:2px}.inspector-section-head h3{font-size:15px}.setting-block{display:grid;gap:9px}.setting-label{display:flex;align-items:baseline;justify-content:space-between;gap:10px;color:var(--ink);font-size:11px;font-weight:650}.setting-label strong{color:var(--brand);font-size:11px}.setting-label small{color:var(--muted);font-size:9.5px;font-weight:400}.range-control{width:100%;height:3px;accent-color:var(--brand);cursor:pointer}.range-labels{display:flex;justify-content:space-between;color:var(--muted);font-size:9px}.count-stepper{display:flex;align-items:center;gap:8px}.count-stepper button{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-subtle);border-radius:5px;background:transparent;color:var(--ink);cursor:pointer}.count-stepper button:hover{border-color:var(--ink)}.count-stepper strong{min-width:20px;text-align:center;font-size:14px}.count-stepper span{color:var(--muted);font-size:10px}.focus-chips{display:flex;flex-wrap:wrap;gap:5px}.focus-chips button{padding:5px 7px;border:1px solid var(--border-subtle);border-radius:4px;background:transparent;color:var(--copy);font-size:9.5px;cursor:pointer}.focus-chips button.active{border-color:var(--brand);background:var(--brand-soft,#eff8f0);color:var(--brand)}.focus-block input,.supplement-block textarea{width:100%;box-sizing:border-box;border:0;border-bottom:1px solid var(--border-subtle);outline:0;background:transparent;padding:5px 0;color:var(--ink);font:inherit;font-size:10.5px}.focus-block input:focus,.supplement-block textarea:focus{border-color:var(--ink)}.supplement-block textarea{resize:vertical;line-height:1.5}.source-boundary{display:flex;align-items:flex-start;gap:6px;margin:0;color:var(--muted);font-size:9.5px;line-height:1.5}.source-boundary .el-icon{color:var(--brand);flex:none}.selected-strip{display:grid;gap:8px;padding-top:14px;border-top:1px solid var(--border-subtle)}.selected-strip-head{display:flex;align-items:center;justify-content:space-between;color:var(--ink);font-size:10.5px;font-weight:650}.selected-strip-head small{color:var(--muted);font-size:9px;font-weight:400}.selected-strip-empty{color:var(--muted);font-size:10px;line-height:1.5}.selected-chip{display:flex;align-items:center;gap:4px;max-width:100%;border:1px solid var(--border-subtle);border-radius:5px;background:var(--surface-muted,#f7f7f4);padding:5px 7px;color:var(--copy);font-size:9.5px;cursor:pointer;text-align:left}.selected-chip:hover{border-color:var(--ink)}.selected-chip .el-icon{margin-left:auto;color:var(--muted);font-size:11px}
.folder-picker{position:relative;min-width:0}.breadcrumb-part{display:inline-flex;align-items:center}.breadcrumb-part:not(:last-of-type)::after{margin:0 4px;color:var(--muted);content:'/';font-weight:400}.breadcrumb-part.path-current{color:var(--ink);font-weight:650}.folder-menu{position:absolute;z-index:5;top:calc(100% + 6px);left:0;display:grid;min-width:220px;max-height:260px;overflow:auto;padding:5px;border:1px solid var(--border-subtle);border-radius:8px;background:var(--surface-solid,#fff);box-shadow:0 12px 28px rgba(15,23,42,.12)}.folder-option{display:grid;grid-template-columns:16px 1fr auto;align-items:center;gap:7px;width:100%;border:0;border-radius:5px;background:transparent;padding:8px 7px;color:var(--copy);font-size:10.5px;text-align:left;cursor:pointer}.folder-option:hover,.folder-option.selected{background:var(--surface-muted,#f5f5f2);color:var(--ink)}.folder-option .el-icon{color:var(--brand)}.folder-option small{color:var(--muted);font-size:9px}.folder-menu-empty{margin:8px;color:var(--muted);font-size:10px;line-height:1.45}.preview-head-actions{display:flex;align-items:center;gap:8px}.open-knowledge-button{border:0;background:transparent;color:var(--brand);font-size:9.5px;cursor:pointer;white-space:nowrap}.open-knowledge-button:hover{text-decoration:underline}.setting-description{color:var(--muted);font-size:9.5px;line-height:1.4}
.style-options{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px}.style-options button{display:grid;gap:3px;min-height:48px;padding:8px;border:1px solid var(--border-subtle);border-radius:6px;background:transparent;color:var(--copy);text-align:left;cursor:pointer}.style-options button:hover{border-color:var(--ink)}.style-options button.active{border-color:var(--brand);background:var(--brand-soft,#eff8f0);color:var(--ink)}.style-options strong{font-size:10px}.style-options small{color:var(--muted);font-size:8.5px;line-height:1.35}
@media (max-width:1120px){.knowledge-workspace{grid-template-columns:minmax(0,1.2fr) minmax(320px,.8fr)}.knowledge-inspector{padding-left:16px}.source-preview-panel{min-height:210px}}
@media (max-width:700px){.knowledge-workspace{display:block}.knowledge-source-center{padding:16px 0}.source-center-head{display:grid}.source-path-row{align-items:stretch;flex-wrap:wrap}.source-search{min-width:160px}.document-list-head,.document-row{grid-template-columns:minmax(170px,1fr) 56px 70px}.document-list-head span:nth-child(3),.document-row>.document-tags{display:none}.document-list-head span:nth-child(5),.document-row>.document-size{display:none}.knowledge-inspector{display:block;padding:16px 0 0;border-left:0;border-top:1px solid var(--border-subtle)}.source-preview-panel{border-right:0;border-bottom:1px solid var(--border-subtle);padding:0 0 16px}.training-settings{padding-top:16px}}
.selected-chip{display:flex;align-items:center;gap:4px;max-width:220px;padding:0;overflow:hidden}
.selected-chip-main{min-width:0;flex:1;overflow:hidden;border:0;background:transparent;padding:5px 0 5px 7px;color:var(--copy);font:inherit;font-size:9.5px;text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer}
.selected-chip-remove{display:grid;place-items:center;width:24px;height:24px;border:0;background:transparent;color:var(--muted);cursor:pointer}
.selected-chip-remove:hover{color:var(--ink)}
</style>
