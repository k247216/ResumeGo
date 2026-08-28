<template>
  <div class="experience-setup" data-test="experience-simulation-setup">
    <div class="experience-workspace">
      <aside class="experience-source-rail" aria-label="真实面经资料说明">
        <div class="rail-heading"><span class="setup-kicker">Knowledge base</span><h3>真实面经</h3></div>
        <div class="source-breadcrumb"><el-icon><Folder /></el-icon><span>知识库</span><b>/</b><strong>真实面经</strong></div>
        <div class="source-summary" data-test="experience-source-summary"><span class="summary-dot"></span><span>知识库资料</span><small>{{ questionSetOptions.length }} 组</small></div>
        <div class="rail-divider"></div>
        <p class="rail-note">手动笔记和导入文件都放在“真实面经”文件夹中，原始题目会与 AI 追问分开保存。</p>
      </aside>

      <section class="experience-source-center">
        <header class="source-center-head"><div><span class="setup-kicker">Step 1 · Question set</span><h3>选择一组真实题集</h3><p>{{ filteredOptions.length }} 组可用 <span>·</span> 原题来源保持可追溯</p></div><label class="source-search"><el-icon><Search /></el-icon><input v-model="searchQuery" type="search" placeholder="搜索公司、岗位或题集" data-test="experience-search" /></label></header>
        <div class="source-toolbar"><span>题集列表 <strong>{{ filteredOptions.length }}</strong></span><span class="source-toolbar-actions"><button type="button" class="source-refresh" data-test="experience-refresh-sources" aria-label="刷新真实面经资料" title="刷新真实面经资料" @click="emit('refresh-sources')"><el-icon><Refresh /></el-icon></button><span class="source-hint"><el-icon><InfoFilled /></el-icon>不展示 AI 生成练习题</span></span></div>
        <div class="question-set-list" data-test="experience-question-set-list">
          <div class="question-list-head"><span>题集</span><span>来源</span><span>题数</span><span>更新</span></div>
          <button v-for="item in filteredOptions" :key="item.value" type="button" class="question-set-row" :class="{ selected: draft.questionSetId === item.value, disabled: item.disabled }" :disabled="item.disabled" @click="selectQuestionSet(item)"><span class="question-set-title"><span class="question-check" :class="{ checked: draft.questionSetId === item.value }"><el-icon v-if="draft.questionSetId === item.value"><Check /></el-icon></span><span class="question-set-icon" :data-source="item.sourceType || 'USER_MANUAL'"><img v-if="companyBrand(item).icon" :src="companyBrand(item).icon!" alt="" aria-hidden="true" /><span v-else>{{ companyBrand(item).letter }}</span></span><span class="question-set-copy"><span class="question-set-company">{{ item.companyName || (item.sourceDocumentId ? '知识库 · 真实面经' : '未填写公司') }}</span><strong class="question-set-role">{{ item.targetRole || item.label }}</strong><small v-if="item.targetRole && item.label !== item.targetRole">{{ item.label }}</small></span></span><span class="question-set-source source-tag">{{ sourceLabel(item.sourceType, item.knowledgeSourceType) }}</span><span class="question-set-count">{{ item.itemCount ?? (item.sourceDocumentId ? '待整理' : '—') }}</span><span class="question-set-updated">{{ formatDate(item.updatedAt) }}</span></button>
          <div v-if="!filteredOptions.length" class="question-empty"><el-icon><Document /></el-icon><strong>还没有真实面经题集</strong><span>请在知识库的“真实面经”文件夹手动录入或导入题目。</span></div>
        </div>
        <section class="selected-question-preview" data-test="experience-question-preview">
          <div class="selected-preview-head"><div><span class="setup-kicker">Selected set</span><h4>{{ preview?.title || '所选题集预览' }}</h4></div><span v-if="preview" class="preview-count">{{ previewQuestions.length }} 道原题</span></div>
          <div v-if="preview" class="question-preview-table" data-test="experience-question-preview-list">
            <div
              v-for="(question, index) in orderedPreviewQuestions"
              :key="`${question.sourceIndex}-${question.text}`"
              class="question-preview-row"
              :class="{ 'is-dragging': draggedQuestionSourceIndex === question.sourceIndex, 'is-drop-target': dragTargetSourceIndex === question.sourceIndex }"
              draggable="true"
              :aria-label="`第${index + 1}题：${question.text}，可拖动调整顺序`"
              @dragstart="beginQuestionDrag(question.sourceIndex, $event)"
              @dragenter.prevent="setQuestionDragTarget(question.sourceIndex)"
              @dragover.prevent="setQuestionDragTarget(question.sourceIndex)"
              @drop.prevent.stop="dropQuestion(question.sourceIndex)"
              @dragend="endQuestionDrag"
            >
              <span class="question-drag-handle" aria-hidden="true">⠿</span>
              <span class="question-order-index">{{ index + 1 }}</span>
              <strong>真实题目</strong>
              <p>{{ question.text }}</p>
              <small>原始来源</small>
            </div>
          </div>
          <div v-else class="question-preview-empty">选择题集后，会在这里按原始顺序预览题目。</div>
        </section>
      </section>

      <aside class="experience-inspector" data-test="experience-preview">
        <section class="experience-settings"><div class="inspector-section-head"><span class="setup-kicker">Step 2 · Practice focus</span><h3>演练重点</h3></div><div class="setting-block"><div class="setting-label"><span>追问强度</span><strong>{{ intensityLabels[intensityIndex] }}</strong></div><input class="range-control" type="range" min="0" max="2" step="1" :value="intensityIndex" aria-label="追问强度" data-test="experience-intensity" @input="onIntensityInput" /><div class="range-labels"><span v-for="label in intensityLabels" :key="label">{{ label }}</span></div></div><div class="setting-block"><div class="setting-label"><span>题目数量</span><small class="question-count-range">{{ questionCountBounds.min }}–{{ questionCountBounds.max }} 题</small></div><div class="count-stepper" data-test="experience-question-count"><button type="button" aria-label="减少题目" @click="adjustQuestionCount(-1)"><el-icon><Minus /></el-icon></button><strong>{{ draft.questionCount }}</strong><span>题</span><button type="button" aria-label="增加题目" @click="adjustQuestionCount(1)"><el-icon><Plus /></el-icon></button></div></div><div class="setting-block review-block"><div class="setting-label"><span>答题回顾</span><small>结束后生成</small></div><div class="review-options"><button type="button" data-test="experience-review-per" :class="{ active: draft.reviewMode === 'PER_QUESTION' }" @click="patch({ reviewMode: 'PER_QUESTION' })"><strong>逐题回顾</strong><small>每道原题后立即整理</small></button><button type="button" data-test="experience-review-end" :class="{ active: draft.reviewMode === 'END_OF_SESSION' }" @click="patch({ reviewMode: 'END_OF_SESSION' })"><strong>结束后复盘</strong><small>完成整组题目后统一看</small></button><button type="button" data-test="experience-review-source" :class="{ active: draft.reviewMode === 'SOURCE_ONLY' }" @click="patch({ reviewMode: 'SOURCE_ONLY' })"><strong>只看原题</strong><small>保留回答，不生成逐题建议</small></button></div></div><div class="setting-block focus-block"><div class="setting-label"><span>考察重点</span><small>可选</small></div><div class="focus-chips"><button v-for="tag in focusPresets" :key="tag" type="button" :class="{ active: draft.focusTags.includes(tag) }" @click="toggleFocus(tag)">{{ tag }}</button></div><div class="selected-focus-tags" v-if="draft.focusTags.length"><span v-for="tag in draft.focusTags" :key="tag" class="focus-tag"><span>{{ tag }}</span><button type="button" :aria-label="`移除重点 ${tag}`" :data-test="`experience-focus-remove-${tag}`" @click="removeFocusTag(tag)">×</button></span></div><div class="custom-focus-entry"><input v-model="newFocusTag" data-test="experience-focus" type="text" placeholder="输入重点后按 Enter 添加" @keydown.enter.prevent="addCustomFocusTag" /><button type="button" aria-label="添加考察重点" @click="addCustomFocusTag"><el-icon><Plus /></el-icon></button></div></div><label class="setting-block supplement-block"><span class="setting-label"><span>补充说明</span><small>可选</small></span><textarea :value="draft.supplement" data-test="experience-supplement" rows="2" maxlength="240" placeholder="例如：先还原原题，再追问你的方案取舍" @input="onSupplementInput"></textarea></label><p class="source-boundary"><el-icon><Lock /></el-icon>原始题目来自题集，AI 只负责主持和追问，不会伪造面经。</p></section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Check, Document, Folder, InfoFilled, Lock, Minus, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { clampQuestionCount, getQuestionCountBounds } from '../../composables/useInterviewComposer'
import type { ExperienceSimulationDraft } from '../../composables/useInterviewComposer'
import type { SelectOption } from './RoleBasedSetup.vue'
import { companyMark } from '../../constants/companyBrands'

type QuestionPreview = { id: number; title: string; typeLabel: string; meta: string; content: string }
const props = defineProps<{ draft: ExperienceSimulationDraft; questionSetOptions: SelectOption[]; preview?: QuestionPreview | null; previewLoading?: boolean }>()
const emit = defineEmits<{ 'update:draft': [draft: ExperienceSimulationDraft]; 'preview-question-set': [id: number | null]; 'materialize-source': [documentId: number]; 'refresh-sources': [] }>()
const searchQuery = ref('')
const newFocusTag = ref('')
const draggedQuestionSourceIndex = ref<number | null>(null)
const dragTargetSourceIndex = ref<number | null>(null)
const intensityLabels = ['克制', '适中', '高压']
const focusPresets = ['项目还原', '系统设计', '技术取舍', '场景追问']

function patch(value: Partial<ExperienceSimulationDraft>) { emit('update:draft', { ...props.draft, ...value }) }
function selectQuestionSet(option: SelectOption) {
  if (option.disabled) return
  if (option.sourceDocumentId != null) {
    emit('materialize-source', option.sourceDocumentId)
    return
  }
  if (props.draft.questionSetId === option.value) {
    patch({ questionSetId: null, questionOrder: [] })
    emit('preview-question-set', null)
    return
  }
  patch({ questionSetId: option.value, questionOrder: [], questionCount: clampQuestionCount('EXPERIENCE_SIMULATION', props.draft.questionCount, option.itemCount) })
  emit('preview-question-set', option.value)
}
function addCustomFocusTag() { const tag = newFocusTag.value.trim(); if (!tag || props.draft.focusTags.includes(tag)) return; patch({ focusTags: [...props.draft.focusTags, tag].slice(0, 6) }); newFocusTag.value = '' }
function onSupplementInput(event: Event) { patch({ supplement: (event.target as HTMLTextAreaElement).value }) }
function onIntensityInput(event: Event) { patch({ followUpIntensity: intensityLabels[Number((event.target as HTMLInputElement).value)] || intensityLabels[1] }) }
function adjustQuestionCount(delta: number) { patch({ questionCount: clampQuestionCount('EXPERIENCE_SIMULATION', props.draft.questionCount + delta, selectedQuestionSetCount.value) }) }
function toggleFocus(tag: string) { patch({ focusTags: (props.draft.focusTags.includes(tag) ? props.draft.focusTags.filter((item) => item !== tag) : [...props.draft.focusTags, tag]).slice(0, 6) }) }
function removeFocusTag(tag: string) { patch({ focusTags: props.draft.focusTags.filter((item) => item !== tag) }) }
function formatDate(value?: string) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value.slice(0, 10) : date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }) }
function sourceLabel(value?: string, knowledgeSourceType?: string) {
  if (value === 'IMPORTED_EXPERIENCE') return '外部导入'
  if (value === 'USER_MANUAL') return '手动笔记'
  if (value === 'KNOWLEDGE_DOCUMENT') return knowledgeSourceType === 'NOTE' ? '手动笔记' : '外部导入'
  return '知识库资料'
}
const iconKeyNames: Record<string, string> = { tencent: '腾讯', bytedance: '字节跳动', meituan: '美团', alibaba: '阿里', baidu: '百度', jd: '京东', pinduoduo: '拼多多', didi: '滴滴' }
function companyBrand(item: SelectOption) {
  // front matter 中的 icon 是显式元数据：有已收录的 icon key 时优先使用它，
  // 否则再按公司名匹配；这样修改知识库图标后刷新题集会同步到列表。
  return companyMark(iconKeyNames[item.companyIconKey || ''] || item.companyName || null)
}
const intensityIndex = computed(() => Math.max(0, intensityLabels.indexOf(props.draft.followUpIntensity)))
const selectedQuestionSetCount = computed(() => props.questionSetOptions.find((item) => item.value === props.draft.questionSetId)?.itemCount ?? null)
const questionCountBounds = computed(() => getQuestionCountBounds('EXPERIENCE_SIMULATION', selectedQuestionSetCount.value))
const previewQuestions = computed(() => props.preview?.content.split('\n').map((line) => line.replace(/^\d+[.、)]\s*/, '').trim()).filter(Boolean) ?? [])
const orderedPreviewQuestions = computed(() => {
  const order = props.draft.questionOrder ?? []
  const valid = order.length === previewQuestions.value.length
    && order.every((sourceIndex, index) => Number.isInteger(sourceIndex)
      && sourceIndex >= 0
      && sourceIndex < previewQuestions.value.length
      && order.indexOf(sourceIndex) === index)
  const effectiveOrder = valid ? order : previewQuestions.value.map((_, index) => index)
  return effectiveOrder.map((sourceIndex) => ({ sourceIndex, text: previewQuestions.value[sourceIndex] }))
})
function beginQuestionDrag(sourceIndex: number, event: DragEvent) {
  draggedQuestionSourceIndex.value = sourceIndex
  dragTargetSourceIndex.value = sourceIndex
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(sourceIndex))
  }
}
function setQuestionDragTarget(sourceIndex: number) {
  if (draggedQuestionSourceIndex.value !== null && sourceIndex !== draggedQuestionSourceIndex.value) {
    dragTargetSourceIndex.value = sourceIndex
  }
}
function dropQuestion(targetSourceIndex: number) {
  const sourceIndex = draggedQuestionSourceIndex.value
  const current = orderedPreviewQuestions.value
  if (sourceIndex === null || sourceIndex === targetSourceIndex) {
    endQuestionDrag()
    return
  }
  const fromIndex = current.findIndex((question) => question.sourceIndex === sourceIndex)
  const targetIndex = current.findIndex((question) => question.sourceIndex === targetSourceIndex)
  if (fromIndex < 0 || targetIndex < 0) {
    endQuestionDrag()
    return
  }
  const next = current.map((question) => question.sourceIndex)
  const [moved] = next.splice(fromIndex, 1)
  next.splice(targetIndex, 0, moved)
  patch({ questionOrder: next })
  endQuestionDrag()
}
function endQuestionDrag() {
  draggedQuestionSourceIndex.value = null
  dragTargetSourceIndex.value = null
}
const filteredOptions = computed(() => { const query = searchQuery.value.trim().toLowerCase(); return props.questionSetOptions.filter((item) => !query || `${item.label} ${item.description || ''} ${item.meta || ''}`.toLowerCase().includes(query)) })
</script>

<style scoped>
.experience-setup{width:100%;color:var(--ink)}.experience-workspace{display:grid;grid-template-columns:180px minmax(0,1fr) 286px;min-height:550px;border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}.experience-source-rail{display:grid;align-content:start;gap:8px;padding:22px 16px 20px 0;border-right:1px solid var(--border-subtle)}.rail-heading{display:grid;gap:3px;padding:0 10px 13px}.setup-kicker{color:var(--muted);font-size:9.5px;font-weight:750;letter-spacing:.12em;text-transform:uppercase}.rail-heading h3,.source-center-head h3,.inspector-head h3,.inspector-section-head h3{margin:3px 0 0;color:var(--ink);font-size:18px;letter-spacing:-.03em}.source-breadcrumb{display:flex;align-items:center;gap:5px;padding:0 10px 9px;color:var(--muted);font-size:10px}.source-breadcrumb .el-icon{color:var(--brand)}.source-breadcrumb b{font-weight:400}.source-breadcrumb strong{color:var(--ink);font-weight:650}.source-summary{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:8px;margin:0 10px;padding:9px 0;color:var(--ink);font-size:11px;font-weight:650}.source-summary small{color:var(--muted);font-size:10px;font-weight:400}.summary-dot{width:7px;height:7px;border-radius:50%;background:var(--brand)}.rail-divider{height:1px;margin:10px 10px;border-top:1px solid var(--border-subtle)}.rail-note{margin:14px 10px 0;color:var(--muted);font-size:10px;line-height:1.55}
.experience-source-center{min-width:0;padding:22px}.source-center-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px}.source-center-head p{margin:6px 0 0;color:var(--muted);font-size:10.5px}.source-center-head p span{padding:0 3px}.source-search{display:flex;align-items:center;gap:7px;width:220px;border:1px solid var(--border-subtle);border-radius:7px;padding:7px 9px;color:var(--muted)}.source-search:focus-within{border-color:var(--ink)}.source-search input{width:100%;border:0;outline:0;background:transparent;color:var(--ink);font:inherit;font-size:11px}.source-toolbar{display:flex;align-items:center;justify-content:space-between;margin:24px 0 8px;color:var(--muted);font-size:10.5px}.source-toolbar strong{color:var(--ink);font-weight:650}.source-hint{display:flex;align-items:center;gap:5px;font-size:9.5px}.source-hint .el-icon{color:var(--brand)}.question-set-list{border-top:1px solid var(--border-subtle)}.question-list-head,.question-set-row{display:grid;grid-template-columns:minmax(230px,1.6fr) 100px 48px 70px;align-items:center;column-gap:10px}.question-list-head{padding:8px 7px;color:var(--muted);font-size:9.5px}.question-set-row{width:100%;padding:11px 7px;border:0;border-bottom:1px solid color-mix(in srgb,var(--border-subtle) 72%,transparent);background:transparent;color:var(--copy);font:inherit;text-align:left;cursor:pointer}.question-set-row:hover:not(:disabled){background:var(--surface-muted,#f8f8f5)}.question-set-row.selected{background:var(--brand-soft,#eff8f0)}.question-set-row.disabled{cursor:default;opacity:.55}.question-set-title{display:flex;align-items:center;gap:8px;min-width:0}.question-check{display:grid;place-items:center;width:14px;height:14px;border:1px solid var(--border-subtle);border-radius:3px;color:#fff;flex:none}.question-check.checked{border-color:var(--brand);background:var(--brand)}.question-check .el-icon{font-size:11px}.question-set-icon{display:grid;place-items:center;width:25px;height:25px;border:1px solid var(--border-subtle);border-radius:5px;background:var(--surface-solid,#fff);color:var(--ink);flex:none}.question-set-icon[data-source="IMPORTED_EXPERIENCE"]{color:#7a5ab0}.question-set-title strong{overflow:hidden;color:var(--ink);font-size:11px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.question-set-source,.question-set-count,.question-set-updated{overflow:hidden;color:var(--muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.question-empty{display:grid;justify-items:center;gap:7px;padding:66px 10px;color:var(--muted);font-size:11px;text-align:center}.question-empty .el-icon{font-size:22px}.question-empty strong{color:var(--ink);font-size:12px}.demo-note{margin:12px 7px 0;color:var(--muted);font-size:10px;line-height:1.5}.demo-note span{margin-right:6px;color:var(--ink);font-weight:650}
.experience-inspector{display:grid;align-content:start;padding:22px 0 20px 20px;border-left:1px solid var(--border-subtle)}.question-preview-panel{display:grid;gap:12px;min-height:226px;padding-bottom:18px;border-bottom:1px solid var(--border-subtle)}.inspector-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.inspector-head h3{overflow:hidden;max-width:205px;font-size:14px;text-overflow:ellipsis;white-space:nowrap}.preview-type{color:var(--muted);font-size:9px;font-weight:750}.preview-content{display:grid;gap:8px;min-height:140px}.preview-meta{color:var(--muted);font-size:10px}.preview-content pre{max-height:156px;margin:0;overflow:auto;white-space:pre-wrap;color:var(--copy);font:inherit;font-size:10.5px;line-height:1.65}.source-badge{display:flex;align-items:center;gap:5px;color:var(--brand);font-size:9.5px}.preview-state{display:grid;align-content:center;justify-items:start;gap:7px;min-height:150px;color:var(--muted);font-size:10.5px;line-height:1.5}.preview-state strong{color:var(--ink);font-size:11.5px}.preview-state .el-icon{font-size:23px;color:var(--ink)}.experience-settings{display:grid;gap:17px;padding-top:18px}.inspector-section-head{display:grid;gap:2px}.inspector-section-head h3{font-size:15px}.setting-block{display:grid;gap:9px}.setting-label{display:flex;align-items:baseline;justify-content:space-between;gap:10px;color:var(--ink);font-size:11px;font-weight:650}.setting-label strong{color:var(--brand);font-size:11px}.setting-label small{color:var(--muted);font-size:9.5px;font-weight:400}.range-control{width:100%;height:3px;accent-color:var(--brand);cursor:pointer}.range-labels{display:flex;justify-content:space-between;color:var(--muted);font-size:9px}.count-stepper{display:flex;align-items:center;gap:8px}.count-stepper button{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--border-subtle);border-radius:5px;background:transparent;color:var(--ink);cursor:pointer}.count-stepper button:hover{border-color:var(--ink)}.count-stepper strong{min-width:20px;text-align:center;font-size:14px}.count-stepper span{color:var(--muted);font-size:10px}.focus-chips{display:flex;flex-wrap:wrap;gap:5px}.focus-chips button{padding:5px 7px;border:1px solid var(--border-subtle);border-radius:4px;background:transparent;color:var(--copy);font-size:9.5px;cursor:pointer}.focus-chips button.active{border-color:var(--brand);background:var(--brand-soft,#eff8f0);color:var(--brand)}.focus-block input,.supplement-block textarea{width:100%;box-sizing:border-box;border:0;border-bottom:1px solid var(--border-subtle);outline:0;background:transparent;padding:5px 0;color:var(--ink);font:inherit;font-size:10.5px}.focus-block input:focus,.supplement-block textarea:focus{border-color:var(--ink)}.supplement-block textarea{resize:vertical;line-height:1.5}.source-boundary{display:flex;align-items:flex-start;gap:6px;margin:0;color:var(--muted);font-size:9.5px;line-height:1.5}.source-boundary .el-icon{color:var(--brand);flex:none}
@media (max-width:1120px){.experience-workspace{grid-template-columns:160px minmax(0,1fr)}.experience-inspector{grid-column:1 / -1;grid-template-columns:minmax(0,1fr) minmax(260px,.8fr);column-gap:24px;border-left:0;border-top:1px solid var(--border-subtle);padding:18px 0 0}.question-preview-panel{border-bottom:0;border-right:1px solid var(--border-subtle);padding-right:20px}.experience-settings{padding-top:0}}
@media (max-width:700px){.experience-workspace{display:block}.experience-source-rail{border-right:0;border-bottom:1px solid var(--border-subtle);padding:14px 0}.rail-note{display:none}.experience-source-center{padding:16px 0}.source-center-head{display:grid}.source-search{width:auto}.question-list-head,.question-set-row{grid-template-columns:minmax(190px,1fr) 70px 48px}.question-list-head span:nth-child(4),.question-set-row>.question-set-updated{display:none}.experience-inspector{display:block;padding:16px 0 0}.question-preview-panel{border-right:0;border-bottom:1px solid var(--border-subtle);padding:0 0 16px}.experience-settings{padding-top:16px}}
.experience-workspace{grid-template-columns:230px minmax(0,1fr) 310px}.selected-question-preview{display:grid;gap:12px;margin-top:24px;padding-top:20px;border-top:1px solid var(--border-subtle)}.selected-preview-head{display:flex;align-items:flex-end;justify-content:space-between;gap:14px}.selected-preview-head h4{margin:4px 0 0;color:var(--ink);font-size:15px;letter-spacing:-.02em}.preview-count{color:var(--muted);font-size:10px}.question-preview-table{display:grid;border-top:1px solid var(--border-subtle)}.question-preview-row{display:grid;grid-template-columns:24px 62px minmax(0,1fr) 70px;align-items:center;gap:8px;padding:8px 4px;border-bottom:1px solid color-mix(in srgb,var(--border-subtle) 72%,transparent);font-size:10px}.question-preview-row>span{color:var(--muted);font-variant-numeric:tabular-nums}.question-preview-row>strong{justify-self:start;padding:3px 5px;border:1px solid var(--border-subtle);border-radius:4px;background:var(--surface-muted,#f7f7f4);color:var(--copy);font-size:9px;font-weight:500}.question-preview-row p{overflow:hidden;margin:0;color:var(--ink);text-overflow:ellipsis;white-space:nowrap}.question-preview-row small{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap}.question-preview-empty{padding:18px 0;color:var(--muted);font-size:10.5px}.experience-inspector{padding-left:20px}.experience-settings{padding-top:0}.question-preview-panel{display:none}
.question-set-copy{display:grid;gap:2px;min-width:0}.question-set-company{overflow:hidden;color:var(--muted);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.question-set-copy small{overflow:hidden;color:var(--muted);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.question-set-icon{font-size:9px;font-weight:750;letter-spacing:.02em}.review-options{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:5px}.review-options button{display:grid;gap:3px;padding:7px 6px;border:1px solid var(--border-subtle);border-radius:5px;background:transparent;color:var(--copy);text-align:left;cursor:pointer}.review-options button:hover{border-color:var(--ink)}.review-options button.active{border-color:var(--brand);background:var(--brand-soft,#eff8f0)}.review-options strong{color:var(--ink);font-size:9.5px}.review-options small{color:var(--muted);font-size:8.5px;line-height:1.35}.selected-focus-tags{display:flex;flex-wrap:wrap;gap:5px}.focus-tag{display:inline-flex;align-items:center;gap:4px;padding:3px 5px;border:1px solid color-mix(in srgb,var(--brand) 42%,var(--border-subtle));border-radius:4px;background:var(--brand-soft,#eff8f0);color:var(--brand);font-size:9px}.focus-tag button{padding:0;border:0;background:transparent;color:inherit;font-size:12px;line-height:1;cursor:pointer}.custom-focus-entry{display:flex;align-items:center;border-bottom:1px solid var(--border-subtle)}.custom-focus-entry:focus-within{border-color:var(--ink)}.custom-focus-entry input{width:100%;border:0;outline:0;background:transparent;padding:5px 0;color:var(--ink);font:inherit;font-size:10.5px}.custom-focus-entry button{display:grid;place-items:center;width:22px;height:22px;border:0;background:transparent;color:var(--muted);cursor:pointer}.custom-focus-entry button:hover{color:var(--ink)}
@media (max-width:1120px){.experience-workspace{grid-template-columns:230px minmax(0,1fr)}.experience-inspector{grid-column:1 / -1;grid-template-columns:minmax(0,1fr) minmax(260px,.8fr);column-gap:24px;border-left:0;border-top:1px solid var(--border-subtle);padding:18px 0 0}.experience-settings{padding-top:0}}
@media (max-width:700px){.experience-workspace{display:block}.experience-source-center{padding:16px 0}.experience-inspector{display:block;padding:16px 0 0}.question-preview-row{grid-template-columns:22px 58px minmax(0,1fr)}.question-preview-row small{display:none}}
.question-set-icon img{width:18px;height:18px;object-fit:contain}
.source-tag{justify-self:start;padding:3px 5px;border:1px solid var(--border-subtle);border-radius:4px;background:var(--surface-muted,#f7f7f4);font-size:9px}
.question-set-list{max-height:314px;overflow-y:auto;overscroll-behavior:contain;scrollbar-gutter:stable}
.question-list-head{position:sticky;top:0;z-index:1;background:var(--canvas,#fff)}
.question-preview-table{max-height:250px;overflow-y:auto;overscroll-behavior:contain;scrollbar-gutter:stable}
.question-preview-row{grid-template-columns:24px 62px minmax(0,1fr) 70px 42px}
.source-toolbar-actions{display:flex;align-items:center;gap:9px}.source-refresh{display:grid;place-items:center;width:22px;height:22px;padding:0;border:1px solid transparent;border-radius:5px;background:transparent;color:var(--muted);cursor:pointer}.source-refresh:hover{border-color:var(--border-subtle);color:var(--ink)}
@media (max-width:700px){.question-preview-row{grid-template-columns:22px 58px minmax(0,1fr) 42px}.question-preview-row small{display:none}}
.question-preview-table{gap:7px;padding-top:7px;border-top:0}
.question-preview-row{grid-template-columns:18px 24px 62px minmax(0,1fr) 62px;gap:8px;min-height:42px;padding:8px 9px;border:1px solid var(--border-subtle);border-radius:8px;background:var(--surface-solid,#fff);cursor:grab;transition:border-color .16s ease,background .16s ease,opacity .16s ease,box-shadow .16s ease}
.question-preview-row:hover{border-color:var(--border-default);background:var(--surface-muted,#f8f8f5)}
.question-preview-row:active{cursor:grabbing}
.question-preview-row.is-dragging{opacity:.42;border-style:dashed}
.question-preview-row.is-drop-target{border-color:var(--brand);background:var(--brand-soft,#eff8f0);box-shadow:0 0 0 2px color-mix(in srgb,var(--brand) 16%,transparent)}
.question-drag-handle{color:var(--muted);font-size:16px;line-height:1;letter-spacing:-4px;user-select:none}
.question-order-index{color:var(--muted);font-size:10px;font-variant-numeric:tabular-nums}
.question-preview-row p{white-space:normal;line-height:1.45}
.question-preview-row small{justify-self:end}
@media (max-width:700px){.question-preview-row{grid-template-columns:18px 22px 58px minmax(0,1fr);padding-inline:7px}.question-preview-row small{display:none}}
</style>
