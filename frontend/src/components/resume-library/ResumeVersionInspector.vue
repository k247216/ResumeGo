<template>
  <aside class="version-inspector" data-test="resume-version-inspector">
    <header class="inspector-head">
      <div>
        <span class="inspector-kicker">版本工具</span>
        <template v-if="selectedVersion">
          <div class="inspector-title-row">
            <h2>版本 V{{ selectedVersion.versionNo }}</h2>
            <span class="version-state" :class="{ current: viewingCurrent }">{{ viewingCurrent ? '当前版本' : '历史预览' }}</span>
          </div>
        </template>
        <h2 v-else-if="resume">{{ resume.title }}</h2>
      </div>
      <div class="inspector-head-actions" v-if="selectedVersion">
        <button type="button" class="inspector-icon-btn" data-test="copy-version" aria-label="复制简历名称" title="复制简历名称" @click="copyVersion">
          <el-icon :size="14"><CopyDocument /></el-icon>
        </button>
        <button type="button" class="inspector-icon-btn" data-test="inspector-more" aria-label="更多版本操作" title="更多版本操作" @click="ElMessage.info('更多版本操作将在后续版本开放')">
          <el-icon :size="15"><MoreFilled /></el-icon>
        </button>
      </div>
    </header>

    <template v-if="resume && selectedVersion">
      <!-- 版本信息 -->
      <section class="inspector-section" data-test="version-meta">
        <h3 class="section-title">版本信息</h3>
        <div class="meta-grid">
          <div class="meta-row"><span>创建来源</span><strong>{{ sourceLabel(selectedVersion.createdByType) }}</strong></div>
          <div class="meta-row"><span>创建时间</span><strong>{{ formatTime(selectedVersion.createdAt) }}</strong></div>
          <div v-if="selectedVersion.parentVersionId" class="meta-row"><span>父版本</span><strong>{{ versionLabel(selectedVersion.parentVersionId) }}</strong></div>
          <div v-if="resume.forkedFromVersionId" class="meta-row"><span>来源版本</span><strong>{{ versionLabel(resume.forkedFromVersionId) }}</strong></div>
        </div>
      </section>

      <!-- 版本说明 -->
      <section class="inspector-section" data-test="version-note">
        <h3 class="section-title">版本说明</h3>
        <template v-if="editingSummary">
          <textarea v-model="summaryDraft" class="change-summary-input" data-test="change-summary-input" rows="3" maxlength="240" placeholder="写下这次版本修改了什么"></textarea>
          <div class="summary-edit-actions">
            <button type="button" data-test="cancel-change-summary" @click="cancelSummaryEdit">取消</button>
            <button type="button" class="summary-save" data-test="save-change-summary" @click="saveSummary">保存说明</button>
          </div>
        </template>
        <template v-else>
          <p v-if="selectedVersion.changeSummary" class="meta-summary">{{ selectedVersion.changeSummary }}</p>
          <p v-else class="inspector-note">暂无版本说明</p>
          <button type="button" class="inline-action" data-test="edit-change-summary" @click="startSummaryEdit">编辑说明</button>
        </template>
      </section>

      <!-- 绑定状态 -->
      <section class="inspector-section" data-test="binding-status">
        <h3 class="section-title">绑定状态</h3>
        <p v-if="usedByLoading" class="inspector-note">正在读取绑定目标…</p>
        <div v-if="usedByTargets.length && !boundTargets.length" class="used-by-list">
          <button
            v-for="row in usedByTargets"
            :key="row.targetId"
            type="button"
            class="used-by-row"
            data-test="used-by-target"
            @click="emit('open-target', row.targetId)"
          >
            <span class="used-by-copy">{{ row.label }}</span>
          </button>
        </div>
        <div v-if="boundTargets.length" class="binding-summary-list" data-test="binding-summary">
          <article v-for="target in boundTargets" :key="target.targetId" class="binding-summary-card">
            <div class="binding-summary-copy">
              <span>当前目标</span>
              <strong>{{ target.label }}</strong>
              <small v-if="target.stageLabel">{{ target.stageLabel }} · 关联 V{{ selectedVersion.versionNo }}</small>
            </div>
            <div class="binding-summary-actions">
              <button type="button" data-test="used-by-target" @click="emit('open-target', target.targetId)">查看目标</button>
              <button type="button" data-test="unbind-target" @click="emit('unbind-target', target.targetId)">解除绑定</button>
            </div>
          </article>
          <button type="button" class="binding-change-link" data-test="change-binding" @click="bindingDialogOpen = true">更换绑定</button>
        </div>
        <div v-else-if="availableTargets.length" class="binding-empty-state" data-test="binding-empty">
          <p class="inspector-note">此版本尚未绑定到任何求职目标。</p>
          <button type="button" class="binding-open-button" data-test="open-binding-dialog" @click="bindingDialogOpen = true">绑定到求职目标</button>
        </div>
        <p v-else class="inspector-note" data-test="binding-empty">此版本尚未绑定，也还没有可绑定的求职目标。</p>
        <ResumeTargetBindingDialog
          :open="bindingDialogOpen"
          :targets="availableTargets"
          @confirm="submitBinding"
          @cancel="bindingDialogOpen = false"
        />
      </section>

      <!-- 操作 -->
      <section class="inspector-section" data-test="inspector-ops">
        <h3 class="section-title">操作</h3>
        <div class="inspector-actions">
          <button type="button" class="inspector-primary" data-test="open-fork" @click="emit('fork')">
            基于此版本创建岗位副本
          </button>
          <router-link
            v-if="viewingCurrent"
            class="inspector-secondary"
            data-test="continue-editing"
            :to="buildResumeEditorLocation({ resumeId: resume.id, versionId: selectedVersion.id })"
          >进入编辑台</router-link>
          <span v-else class="readonly-note" data-test="history-readonly-note">历史版本只读，不改变当前版本</span>
        </div>
      </section>

      <!-- 历史活动：竖向时间轴 -->
      <section class="inspector-section" data-test="activity-timeline">
        <div class="section-head">
          <h3 class="section-title">历史活动</h3>
          <button v-if="activityEvents.length > 4" type="button" class="activity-all" data-test="activity-toggle" @click="activityShowAll = !activityShowAll">{{ activityShowAll ? '收起' : '查看全部' }}</button>
        </div>
        <ul class="activity-list" :class="{ expanded: activityShowAll }" data-test="activity-list">
          <li v-for="(event, index) in activityShowAll ? activityEvents : activityEvents.slice(0, 4)" :key="event.key" class="activity-row" :class="{ latest: event.latest }" :style="{ '--activity-delay': `${index * 38}ms` }">
            <span class="activity-dot" :class="event.status" aria-hidden="true"></span>
            <span class="activity-copy">
              <span class="activity-topline"><strong>{{ event.label }}</strong></span>
              <small v-if="event.modules" class="activity-modules">{{ event.modules }}</small>
              <small>{{ event.time }}</small>
            </span>
          </li>
        </ul>
      </section>

      <section class="inspector-section inspector-archive" data-test="archive-section">
        <button type="button" class="archive-link" data-test="delete-resume" @click="emit('archive')">
          {{ resume.archivedAt ? '从回收站恢复' : '归档到回收站' }}
        </button>
        <p>归档只移入回收站，不删除版本与历史引用。</p>
      </section>
    </template>
    <div v-else class="inspector-placeholder">选择资产后查看版本信息</div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CopyDocument, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Resume, ResumeVersion } from '../../types/resume'
import { buildResumeEditorLocation } from '../../utils/editorRoute'
import { diffResumeContent } from '../../utils/resumeVersionDiff'
import ResumeTargetBindingDialog, { type ResumeTargetBindingOption } from './ResumeTargetBindingDialog.vue'

const props = withDefaults(defineProps<{
  resume: Resume | null
  versions: ResumeVersion[]
  selectedVersion: ResumeVersion | null
  currentVersionId: number | null
  usedByTargets: Array<{ targetId: number; label: string }>
  usedByLoading: boolean
  availableTargets?: ResumeTargetBindingOption[]
}>(), { availableTargets: () => [] })

const emit = defineEmits<{
  'open-target': [id: number]
  'bind-target': [id: number]
  'unbind-target': [id: number]
  'update-summary': [versionId: number, summary: string]
  fork: []
  archive: []
}>()

const viewingCurrent = computed(() =>
  props.selectedVersion != null && props.selectedVersion.id === props.currentVersionId)
const bindingDialogOpen = ref(false)
const editingSummary = ref(false)
const summaryDraft = ref('')
const boundTargets = computed(() => props.availableTargets
  .filter((target) => props.selectedVersion != null && target.resumeVersionId === props.selectedVersion.id)
  .map((target) => ({
    ...target,
    label: props.usedByTargets.find((row) => row.targetId === target.targetId)?.label ?? target.label,
  })))

function submitBinding(id: number) {
  if (!Number.isSafeInteger(id) || id <= 0) return
  emit('bind-target', id)
  bindingDialogOpen.value = false
}

function startSummaryEdit() {
  summaryDraft.value = props.selectedVersion?.changeSummary ?? ''
  editingSummary.value = true
}

function cancelSummaryEdit() {
  editingSummary.value = false
  summaryDraft.value = ''
}

function saveSummary() {
  if (!props.selectedVersion) return
  emit('update-summary', props.selectedVersion.id, summaryDraft.value.trim())
  editingSummary.value = false
}

watch(() => props.selectedVersion?.id, () => cancelSummaryEdit())

/** 历史活动：从真实版本派生（创建事件），不虚构改名/编辑记录 */
const activityShowAll = ref(false)
const activityEvents = computed(() => {
  const sorted = [...props.versions].sort((left, right) =>
    String(right.createdAt).localeCompare(String(left.createdAt)))
  return sorted.map((version, index) => {
    // 相对父版本的真实章节变化（无父版本 = 初始创建）
    const parent = sorted.find((item) => item.id === version.parentVersionId)
    let modules: string | null = null
    if (parent) {
      const labels = diffResumeContent(parent.content, version.content).map((change) => change.chapterLabel)
      modules = labels.length ? `更新了 ${labels.join('、')}` : null
    }
    const latest = version.id === props.currentVersionId
    const status = latest ? 'current' : version.createdByType === 'fork' ? 'fork' : parent ? 'updated' : 'created'
    return {
      key: version.id,
      label: `${profileName.value} ${parent ? '更新了内容' : '创建了版本'}${parent ? '' : ` V${version.versionNo}`}`,
      modules: index === sorted.length - 1 && !version.parentVersionId ? null : modules,
      time: formatTime(version.createdAt),
      latest,
      status,
    }
  })
})

const profileName = computed(() => {
  try {
    const raw = localStorage.getItem('resumego:local-profile')
    const parsed = raw ? JSON.parse(raw) as { name?: unknown } : null
    return typeof parsed?.name === 'string' && parsed.name.trim() ? parsed.name.trim() : '本地用户'
  } catch {
    return '本地用户'
  }
})

function sourceLabel(type: string) {
  if (type === 'user') return '手工保存'
  if (type === 'fork') return '岗位副本创建'
  if (type === 'import') return '导入'
  if (type === 'ai_suggestion') return 'AI 建议'
  return '版本记录'
}
function formatTime(value: string) {
  if (!value) return '时间未知'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间未知'
    : date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function versionLabel(id: number) {
  const version = props.versions.find((item) => item.id === id)
  return version ? `V${version.versionNo}` : '历史版本'
}

async function copyVersion() {
  const text = props.resume?.title?.trim()
  if (!text) return
  try {
    if (!navigator.clipboard?.writeText) throw new Error('clipboard-unavailable')
    await navigator.clipboard.writeText(text)
    ElMessage.success('简历名称已复制')
  } catch {
    ElMessage.info('当前环境不支持直接复制，请从编辑台查看内容')
  }
}
</script>

<style scoped>
.version-inspector{display:grid;gap:10px;align-content:start}
.inspector-head{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}
.inspector-head-actions{display:flex;align-items:center;gap:2px;margin-top:1px}
.inspector-icon-btn{display:grid;place-items:center;width:27px;height:27px;border:0;border-radius:7px;background:transparent;color:var(--muted);cursor:pointer}
.inspector-icon-btn:hover{background:var(--bg-hover);color:var(--ink)}
.inspector-kicker{display:block;margin:0 0 2px;color:var(--muted);font-size:9.5px;font-weight:750;letter-spacing:.12em}
.inspector-title-row{display:flex;align-items:center;gap:7px}
.inspector-head h2{margin:0;font-size:20px;font-weight:760;color:var(--ink);font-variant-numeric:tabular-nums;letter-spacing:-.02em}
.version-state{color:var(--muted);font-size:10.5px;font-weight:600}
.version-state.current{color:var(--copy)}
.inspector-actions{display:grid;gap:8px}
.inspector-primary{display:grid;place-items:center;border:1px solid #17181a;border-radius:8px;background:#17181a;color:#fff;padding:7px 9px;font-size:11.5px;font-weight:650;cursor:pointer;text-decoration:none}
.inspector-primary:hover{background:#2d2e30;border-color:#2d2e30}
.inspector-secondary{display:grid;place-items:center;border:1px solid var(--line,rgba(28,31,35,.18));border-radius:8px;background:var(--surface-solid,#fff);color:var(--copy);padding:7px 9px;font-size:11.5px;font-weight:550;cursor:pointer;text-decoration:none}
.inspector-secondary:hover{border-color:var(--brand);color:var(--brand)}
.readonly-note{padding:8px 0;color:var(--muted);font-size:11.5px}
.inspector-section{display:grid;gap:6px;border-top:1px solid var(--border-subtle);padding-top:9px}
.section-title{margin:0;font-size:10.5px;font-weight:650;letter-spacing:.07em;color:var(--muted)}
.section-head{display:flex;align-items:baseline;justify-content:space-between}
.section-count{font-size:10.5px;color:var(--muted)}
.meta-grid{display:grid;gap:0}
.meta-row{display:flex;align-items:baseline;justify-content:space-between;gap:10px;min-width:0;padding:7px 0;border-bottom:1px solid var(--border-subtle);font-size:10.5px;color:var(--muted)}
.meta-row span{font-size:10.5px;letter-spacing:.02em}
.meta-row strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11.5px;font-weight:600;color:var(--copy);font-variant-numeric:tabular-nums;text-align:right}
.meta-summary{margin:0;color:var(--copy);font-size:11.5px;line-height:1.55}
.inline-action{justify-self:start;border:0;background:none;padding:0;color:var(--copy);font-size:11px;font-weight:650;cursor:pointer}
.change-summary-input{width:100%;box-sizing:border-box;resize:vertical;border:1px solid var(--border-default);border-radius:8px;background:var(--surface-solid);color:var(--copy);padding:8px;font:inherit;font-size:11.5px;line-height:1.5;outline:none}.change-summary-input:focus{border-color:var(--brand);box-shadow:0 0 0 2px var(--brand-soft)}
.summary-edit-actions{display:flex;justify-content:flex-end;gap:8px}.summary-edit-actions button{border:0;background:none;color:var(--muted);padding:0;font-size:10.5px;cursor:pointer}.summary-edit-actions .summary-save{color:var(--copy);font-weight:650}.summary-edit-actions button:hover{color:var(--brand)}
.inspector-note{margin:0;color:var(--muted);font-size:11px;line-height:1.5}
.used-by-list{display:grid;gap:4px}
.used-by-row{display:flex;align-items:center;border:0;background:none;border-radius:8px;padding:7px 9px;font-size:12px;color:var(--copy);cursor:pointer;text-align:left}
.used-by-row:hover{background:var(--bg-hover);color:var(--brand)}
.used-by-copy{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.binding-summary-list{display:grid;gap:7px}
.binding-summary-card{display:grid;gap:8px;border:1px solid var(--border-subtle);border-radius:9px;background:var(--bg-subtle);padding:9px 10px}
.binding-summary-copy{display:grid;gap:2px;min-width:0}.binding-summary-copy span{color:var(--muted);font-size:9.5px}.binding-summary-copy strong{overflow:hidden;color:var(--copy);font-size:11.5px;text-overflow:ellipsis;white-space:nowrap}.binding-summary-copy small{color:var(--muted);font-size:10px}
.binding-summary-actions{display:flex;align-items:center;gap:10px}.binding-summary-actions button,.binding-change-link{border:0;background:none;padding:0;color:var(--copy);font-size:10.5px;font-weight:650;cursor:pointer}.binding-summary-actions button:hover,.binding-change-link:hover{color:var(--brand)}.binding-summary-actions button + button{color:var(--muted);font-weight:550}.binding-empty-state{display:grid;gap:7px}.binding-open-button{justify-self:start;border:1px solid var(--border-default);border-radius:7px;background:var(--surface-solid);color:var(--copy);padding:6px 9px;font-size:10.5px;font-weight:650;cursor:pointer}.binding-open-button:hover{border-color:var(--brand);color:var(--brand)}
.binding-control{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:6px;margin-top:2px}.binding-control select{min-width:0;height:30px;border:1px solid var(--line,rgba(28,31,35,.18));border-radius:7px;background:var(--surface-solid,#fff);color:var(--copy);padding:0 7px;font:inherit;font-size:10.5px;outline:none}.binding-control select:focus{border-color:var(--brand)}.binding-control button{height:30px;border:1px solid var(--line,rgba(28,31,35,.18));border-radius:7px;background:var(--bg-subtle);color:var(--copy);padding:0 8px;font-size:10.5px;font-weight:650;cursor:pointer}.binding-control button:not(:disabled):hover{border-color:var(--brand);color:var(--brand)}.binding-control button:disabled{cursor:not-allowed;opacity:.45}
.activity-all{border:0;background:none;padding:0;color:var(--brand);font-size:11px;font-weight:600;cursor:pointer}
.activity-list{margin:0;padding:0;list-style:none;display:grid;gap:9px;position:relative}
/* 竖向时间轴：连线贯穿节点 */
.activity-list::before{content:'';position:absolute;left:4px;top:8px;bottom:8px;width:1px;background:var(--border-subtle)}
.activity-row{display:flex;align-items:flex-start;gap:8px;position:relative;animation:activity-enter .2s ease both;animation-delay:var(--activity-delay,0ms)}
.activity-dot{display:block;width:8px;height:8px;border-radius:50%;background:#a7aaa7;flex:0 0 auto;margin:5px 2px 0 0;z-index:1}
.activity-dot.current{background:var(--brand)}
.activity-dot.fork,.activity-dot.updated,.activity-dot.created{background:#a7aaa7}
.activity-copy{display:grid;gap:2px;min-width:0}
.activity-topline{display:block;min-width:0}
.activity-copy strong{overflow:hidden;font-size:11.5px;font-weight:650;color:var(--copy);text-overflow:ellipsis;white-space:nowrap}
.activity-row.latest .activity-copy strong{color:var(--ink)}
.activity-copy small{font-size:10px;color:var(--muted);font-variant-numeric:tabular-nums}
@keyframes activity-enter{from{opacity:0;transform:translateY(4px)}to{opacity:1;transform:none}}
@media(prefers-reduced-motion:reduce){.activity-row{animation:none}}
.inspector-archive{margin-top:2px;padding-bottom:6px}
.archive-link{justify-self:start;border:0;background:none;padding:0;color:var(--muted);font-size:11.5px;font-weight:550;cursor:pointer}
.archive-link:hover{color:var(--danger)}
.inspector-archive p{margin:0;color:var(--muted);font-size:10px;line-height:1.5}
.inspector-placeholder{color:var(--muted);font-size:12px;padding:16px 0}
</style>
