<template>
  <div class="role-setup" data-test="role-based-setup">
    <div class="role-layout">
      <section class="role-column target-column">
        <div class="role-column-head">
          <div><span class="setup-kicker">Target context</span><h3>目标岗位</h3></div>
          <span class="role-column-count">{{ projectOptions.length }} 个目标</span>
        </div>
        <div class="target-list" data-test="role-project-select">
          <button v-for="item in projectOptions" :key="item.value" type="button" class="target-option" :class="{ selected: draft.jobProjectId === item.value, disabled: item.disabled }" :disabled="item.disabled" :data-test="`role-project-option-${item.value}`" @click="selectProject(item)">
            <span class="target-mark" aria-hidden="true">{{ initials(item.label) }}</span>
            <span class="target-copy"><strong>{{ item.label }}</strong><small>{{ item.description || '求职管线 · 进行中' }}</small></span>
            <el-icon v-if="draft.jobProjectId === item.value"><Check /></el-icon>
          </button>
        </div>
        <div v-if="selectedProject" class="target-summary">
          <div class="target-summary-head"><span>当前目标</span><el-icon><Lock /></el-icon></div>
          <strong>{{ selectedProject.label }}</strong>
          <span>{{ selectedProject.description || '岗位阶段待补充' }}</span>
          <small>{{ selectedProject.meta || '未填写工作地点' }}</small>
        </div>
        <div class="context-lock" :class="{ empty: !selectedProject }"><el-icon><Lock /></el-icon><span>{{ selectedProject ? '开始后锁定岗位与阶段快照' : '先选择一个求职目标' }}</span></div>
      </section>

      <section class="role-column resume-column">
        <div class="role-column-head">
          <div><span class="setup-kicker">Resume snapshot</span><h3>简历版本</h3></div>
          <span class="role-column-count">开始后锁定</span>
        </div>
        <div class="resume-list" data-test="role-resume-select">
          <button v-for="item in resumeOptions" :key="item.value" type="button" class="resume-option" :class="{ selected: draft.resumeVersionId === item.value, disabled: item.disabled }" :disabled="item.disabled" :data-test="`role-resume-option-${item.value}`" @click="selectResume(item)">
            <span class="resume-option-mark" aria-hidden="true"><el-icon><Document /></el-icon></span>
            <span class="resume-option-copy"><strong>{{ item.label }}</strong><small>{{ item.description || '本地简历版本' }}</small></span>
            <el-icon v-if="draft.resumeVersionId === item.value"><Check /></el-icon>
          </button>
        </div>
        <div v-if="selectedResume" class="resume-preview" data-test="role-resume-preview">
          <div class="resume-preview-head"><span class="resume-preview-type">RESUME</span><strong>{{ selectedResume.label }}</strong><button type="button" @click="clearResume">更换</button></div>
          <div class="resume-preview-body">
            <strong>{{ selectedResume.preview?.headline || selectedResume.label.split(' · ')[0] }}</strong>
            <small v-if="selectedResume.preview?.summary">{{ selectedResume.preview.summary }}</small>
            <small v-else-if="selectedResume.preview?.skills?.length">已读取 {{ selectedResume.preview.skills.length }} 项技能</small>
            <small v-else class="resume-preview-muted">该版本暂无可展示的摘要</small>
            <div v-if="selectedResume.preview?.skills?.length" class="resume-preview-tags">
              <span v-for="skill in selectedResume.preview.skills" :key="skill">{{ skill }}</span>
            </div>
            <p>开始后将基于该版本的岗位表达和项目事实进行追问。</p>
          </div>
        </div>
        <div v-else class="resume-empty"><el-icon><Document /></el-icon><span>选择一份简历，查看本次使用的版本摘要</span></div>
      </section>

      <section class="role-column interviewer-column">
        <div class="role-column-head">
          <div><span class="setup-kicker">Interview panel</span><h3>面试官顺序</h3></div>
          <span class="role-column-count">{{ selectedPersonas.length }} 位已选</span>
        </div>
        <p class="role-helper"><el-icon><InfoFilled /></el-icon> 拖动排序或使用箭头调整顺序，开始后按此节奏进行。</p>
        <div class="persona-sequence" data-test="role-persona-select">
          <article v-for="(item, index) in selectedPersonas" :key="item.value" class="persona-card">
            <span class="persona-order">{{ index + 1 }}</span><span class="persona-icon" aria-hidden="true"><el-icon><User /></el-icon></span><span class="persona-copy"><strong>{{ item.label }}</strong><small>{{ item.description || '关注项目与岗位深度' }}</small></span>
            <div class="persona-actions"><button type="button" :disabled="index === 0" aria-label="上移面试官" @click="movePersona(index, -1)"><el-icon><ArrowUp /></el-icon></button><button type="button" :disabled="index === selectedPersonas.length - 1" aria-label="下移面试官" @click="movePersona(index, 1)"><el-icon><ArrowDown /></el-icon></button><button type="button" aria-label="移除面试官" @click="removePersona(item.value)"><el-icon><Close /></el-icon></button></div>
          </article>
          <div v-if="!selectedPersonas.length" class="persona-empty">从下方选择一位或多位面试官，组成本次面试顺序。</div>
        </div>
        <div class="persona-add-list" v-if="availablePersonas.length">
          <button v-for="item in availablePersonas" :key="item.value" type="button" class="persona-add" :data-test="`role-persona-option-${item.value}`" @click="addPersona(item.value)"><el-icon><Plus /></el-icon><span>{{ item.label }}</span><small>{{ item.description }}</small></button>
        </div>
        <div class="role-tuning">
          <div class="tuning-line"><span class="field-label">题目数量</span><div class="stepper"><button type="button" aria-label="减少题目" @click="adjustQuestionCount(-1)">−</button><strong>{{ draft.questionCount }}</strong><span>题</span><button type="button" aria-label="增加题目" @click="adjustQuestionCount(1)">＋</button></div></div>
          <div class="focus-line"><span class="field-label">本次重点 <small>可选</small></span><div class="focus-chips"><button v-for="tag in focusPresets" :key="tag" type="button" :class="{ selected: draft.focusTags.includes(tag) }" @click="toggleFocus(tag)">{{ tag }}</button></div></div>
          <input :value="draft.focusTags.join('、')" class="focus-input" data-test="role-focus" placeholder="也可以输入自定义重点，用顿号分隔" @input="patch({ focusTags: splitTags(($event.target as HTMLInputElement).value) })" />
        </div>
        <label class="supplement-line"><span class="field-label">给面试官的补充说明 <small>可选</small></span><textarea :value="draft.supplement" data-test="role-supplement" rows="2" maxlength="240" placeholder="例如：优先追问 Redis 一致性和项目取舍" @input="patch({ supplement: ($event.target as HTMLTextAreaElement).value })" /></label>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowUp, Check, Close, Document, InfoFilled, Lock, Plus, User } from '@element-plus/icons-vue'
import { clampQuestionCount } from '../../composables/useInterviewComposer'
import type { RoleBasedDraft } from '../../composables/useInterviewComposer'

export interface SelectOption {
  value: number
  label: string
  description?: string
  meta?: string
  disabled?: boolean
  fileType?: string
  updatedAt?: string
  sourceType?: string
  /** 未物化的知识库资料原始类型：NOTE 为手动笔记，其余为外部导入。 */
  knowledgeSourceType?: string
  /** 知识库中的原始面经资料；存在时尚未物化为题集。 */
  sourceDocumentId?: number | null
  itemCount?: number
  fileSize?: string
  companyName?: string | null
  targetRole?: string | null
  companyIconKey?: string | null
  preview?: {
    headline?: string
    summary?: string
    skills?: string[]
  }
}
const props = defineProps<{ draft: RoleBasedDraft; projectOptions: SelectOption[]; resumeOptions: SelectOption[]; personaOptions: SelectOption[] }>()
const emit = defineEmits<{ 'update:draft': [draft: RoleBasedDraft] }>()
const focusPresets = ['项目取舍', '系统设计', '并发与性能', '故障排查']
function patch(patchValue: Partial<RoleBasedDraft>) { emit('update:draft', { ...props.draft, ...patchValue }) }
const selectedProject = computed(() => props.projectOptions.find((item) => item.value === props.draft.jobProjectId))
const selectedResume = computed(() => props.resumeOptions.find((item) => item.value === props.draft.resumeVersionId))
const selectedPersonas = computed(() => props.draft.personaIds.map((id) => props.personaOptions.find((item) => item.value === id)).filter((item): item is SelectOption => Boolean(item && !item.disabled)))
const availablePersonas = computed(() => props.personaOptions.filter((item) => !item.disabled && !props.draft.personaIds.includes(item.value)))
function initials(label: string) { return label.replace(/\s*[·|｜].*$/, '').trim().slice(0, 2).toUpperCase() || '目' }
function selectProject(item: SelectOption) { if (!item.disabled) patch({ jobProjectId: item.value }) }
function selectResume(item: SelectOption) { if (!item.disabled) patch({ resumeVersionId: item.value }) }
function clearResume() { patch({ resumeVersionId: null }) }
function addPersona(id: number) { patch({ personaIds: [...props.draft.personaIds, id] }) }
function removePersona(id: number) { patch({ personaIds: props.draft.personaIds.filter((item) => item !== id) }) }
function movePersona(index: number, delta: number) { const next = [...props.draft.personaIds]; const target = index + delta; if (target < 0 || target >= next.length) return; [next[index], next[target]] = [next[target], next[index]]; patch({ personaIds: next }) }
function adjustQuestionCount(delta: number) { patch({ questionCount: clampQuestionCount('ROLE_BASED', props.draft.questionCount + delta) }) }
function toggleFocus(tag: string) { const next = props.draft.focusTags.includes(tag) ? props.draft.focusTags.filter((item) => item !== tag) : [...props.draft.focusTags, tag]; patch({ focusTags: next.slice(0, 6) }) }
function splitTags(value: string) { return value.split(/[，,、]/).map((item) => item.trim()).filter(Boolean).slice(0, 6) }
</script>

<style scoped>
.role-setup{display:grid;gap:18px;width:100%}.role-layout{display:grid;grid-template-columns:minmax(190px,.75fr) minmax(300px,1.05fr) minmax(390px,1.45fr);border-top:1px solid var(--border-subtle);border-bottom:1px solid var(--border-subtle)}.role-column{display:grid;align-content:start;gap:16px;min-width:0;padding:20px 22px}.role-column + .role-column{border-left:1px solid var(--border-subtle)}.role-column-head{display:flex;align-items:flex-end;justify-content:space-between;gap:10px}.setup-kicker{color:var(--muted);font-size:10px;font-weight:700;letter-spacing:.1em;text-transform:uppercase}.role-column-head h3{margin:4px 0 0;color:var(--ink);font-size:18px;letter-spacing:-.03em}.role-column-count{color:var(--muted);font-size:10.5px;white-space:nowrap}.target-list,.resume-list{display:grid;gap:4px}.target-option,.resume-option{display:flex;align-items:center;gap:10px;width:100%;padding:10px 8px;border:1px solid transparent;border-radius:9px;background:transparent;color:var(--ink);text-align:left;cursor:pointer;transition:background .16s,border-color .16s}.target-option:hover:not(:disabled),.resume-option:hover:not(:disabled){border-color:var(--border-subtle);background:var(--surface-muted,#f7f7f4)}.target-option.selected,.resume-option.selected{border-color:color-mix(in srgb,var(--brand) 45%,var(--border-subtle));background:var(--brand-soft,#eef8f1)}.target-option.disabled,.resume-option.disabled{cursor:default;opacity:.55}.target-mark{display:grid;place-items:center;width:30px;height:30px;border:1px solid var(--border-subtle);border-radius:50%;background:var(--surface-solid,#fff);color:var(--ink);font-size:10px;font-weight:750;flex:none}.target-copy,.resume-option-copy{display:grid;gap:3px;min-width:0;flex:1}.target-copy strong,.resume-option-copy strong{overflow:hidden;color:var(--ink);font-size:12px;font-weight:650;text-overflow:ellipsis;white-space:nowrap}.target-copy small,.resume-option-copy small{overflow:hidden;color:var(--muted);font-size:10.5px;text-overflow:ellipsis;white-space:nowrap}.target-option>.el-icon,.resume-option>.el-icon{color:var(--brand);font-size:14px}.resume-option-mark{display:grid;place-items:center;width:30px;height:34px;border:1px solid var(--border-subtle);border-radius:6px;background:var(--surface-solid,#fff);color:var(--ink);flex:none}.context-lock{display:flex;align-items:center;gap:7px;margin-top:3px;color:var(--muted);font-size:10.5px}.context-lock .el-icon{color:var(--brand)}.context-lock.empty .el-icon{color:var(--muted)}.resume-preview{display:grid;border:1px solid var(--border-subtle);border-radius:10px;background:var(--surface-muted,#f7f7f4);overflow:hidden}.resume-preview-head{display:flex;align-items:center;gap:8px;padding:9px 11px;border-bottom:1px solid var(--border-subtle)}.resume-preview-type{color:var(--muted);font-size:9px;font-weight:750;letter-spacing:.08em}.resume-preview-head strong{overflow:hidden;flex:1;color:var(--ink);font-size:11.5px;text-overflow:ellipsis;white-space:nowrap}.resume-preview-head button{border:0;background:transparent;color:var(--brand);font-size:10.5px;cursor:pointer}.resume-preview-body{display:grid;gap:7px;padding:16px}.resume-preview-body>strong{font-size:16px}.resume-preview-body>small{color:var(--muted);font-size:10.5px}.resume-preview-tags{display:flex;flex-wrap:wrap;gap:5px}.resume-preview-tags span{padding:3px 6px;border:1px solid var(--border-subtle);border-radius:4px;background:var(--surface-solid,#fff);color:var(--copy);font-size:9.5px}.resume-preview-body p{margin:3px 0 0;color:var(--muted);font-size:10.5px;line-height:1.55}.resume-empty{display:flex;align-items:center;gap:8px;min-height:104px;color:var(--muted);font-size:11px}.resume-empty .el-icon{font-size:20px}.role-helper{display:flex;align-items:flex-start;gap:6px;margin:-5px 0 0;color:var(--muted);font-size:10.5px;line-height:1.5}.persona-sequence{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px}.persona-card{display:grid;grid-template-columns:auto auto 1fr;align-items:start;gap:7px;min-width:0;padding:11px 9px;border:1px solid var(--border-subtle);border-radius:9px;background:var(--surface-muted,#f7f7f4)}.persona-order{display:grid;place-items:center;width:18px;height:18px;border:1px solid var(--border-subtle);border-radius:50%;background:var(--surface-solid,#fff);color:var(--ink);font-size:9px;font-weight:700}.persona-icon{color:var(--brand);font-size:17px}.persona-copy{display:grid;gap:3px;min-width:0}.persona-copy strong{overflow:hidden;color:var(--ink);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.persona-copy small{color:var(--muted);font-size:9.5px;line-height:1.4}.persona-actions{grid-column:1 / -1;display:flex;justify-content:flex-end;gap:2px}.persona-actions button{display:grid;place-items:center;width:22px;height:22px;border:0;background:transparent;color:var(--muted);cursor:pointer}.persona-actions button:hover:not(:disabled){color:var(--ink)}.persona-actions button:disabled{cursor:default;opacity:.3}.persona-empty{grid-column:1 / -1;padding:17px 0;border:1px dashed var(--border-subtle);color:var(--muted);font-size:11px;text-align:center}.persona-add-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px}.persona-add{display:grid;grid-template-columns:auto 1fr;align-items:center;column-gap:6px;padding:8px 9px;border:1px solid var(--border-subtle);border-radius:7px;background:transparent;color:var(--ink);text-align:left;cursor:pointer}.persona-add:hover{border-color:var(--ink)}.persona-add .el-icon{grid-row:span 2;color:var(--brand)}.persona-add span{overflow:hidden;font-size:10.5px;text-overflow:ellipsis;white-space:nowrap}.persona-add small{overflow:hidden;color:var(--muted);font-size:9px;text-overflow:ellipsis;white-space:nowrap}.persona-demo-note{color:var(--muted);font-size:10.5px}.role-tuning{display:grid;gap:13px;border-top:1px solid var(--border-subtle);padding-top:14px}.tuning-line{display:flex;align-items:center;justify-content:space-between;gap:12px}.field-label{display:flex;align-items:baseline;gap:6px;color:var(--ink);font-size:11.5px;font-weight:650}.field-label small{color:var(--muted);font-size:10px;font-weight:400}.stepper{display:flex;align-items:center;gap:8px}.stepper button{display:grid;place-items:center;width:25px;height:25px;border:1px solid var(--border-subtle);border-radius:5px;background:var(--surface-muted,#f7f7f4);color:var(--ink);font-size:15px;cursor:pointer}.stepper strong{min-width:19px;text-align:center;font-size:13px}.stepper span{color:var(--muted);font-size:10px}.focus-line{display:grid;gap:8px}.focus-chips{display:flex;flex-wrap:wrap;gap:5px}.focus-chips button{padding:5px 8px;border:1px solid var(--border-subtle);border-radius:5px;background:transparent;color:var(--copy);font-size:10px;cursor:pointer}.focus-chips button.selected{border-color:var(--brand);background:var(--brand-soft,#eef8f1);color:var(--brand)}.focus-input{width:100%;box-sizing:border-box;border:0;border-bottom:1px solid var(--border-subtle);outline:0;background:transparent;padding:6px 0;color:var(--ink);font-size:11px}.focus-input:focus{border-bottom-color:var(--ink)}.supplement-line{display:grid;gap:7px}.supplement-line textarea{box-sizing:border-box;width:100%;resize:vertical;border:0;border-bottom:1px solid var(--border-subtle);outline:0;background:transparent;padding:7px 0;color:var(--ink);font:inherit;font-size:11px;line-height:1.6}.supplement-line textarea:focus{border-bottom-color:var(--ink)}
@media (max-width:1080px){.role-layout{grid-template-columns:repeat(2,minmax(0,1fr))}.interviewer-column{grid-column:1 / -1;border-left:0!important;border-top:1px solid var(--border-subtle)}}
@media (max-width:700px){.role-layout{grid-template-columns:1fr}.role-column + .role-column{border-left:0;border-top:1px solid var(--border-subtle)}.interviewer-column{grid-column:auto}.persona-sequence{grid-template-columns:1fr}.persona-add-list{grid-template-columns:1fr}}
.target-summary{display:grid;gap:5px;margin-top:6px;padding:12px;border:1px solid color-mix(in srgb,var(--brand) 34%,var(--border-subtle));border-radius:8px;background:var(--brand-soft,#eff8f0)}.target-summary-head{display:flex;align-items:center;justify-content:space-between;color:var(--brand);font-size:10px;font-weight:650}.target-summary strong{overflow:hidden;color:var(--ink);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.target-summary>span,.target-summary>small{color:var(--copy);font-size:10px}.target-summary>small{color:var(--muted)}.resume-preview-muted{font-style:italic}
</style>
