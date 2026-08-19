<template>
  <div class="job-create">
    <div v-if="fromEditor" class="workspace-return-bar">
      <button type="button" @click="returnToEditor()">← 返回当前简历工作台</button>
      <span>新增 JD 后可继续用于当前简历匹配</span>
    </div>

    <div class="create-header-bar">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回岗位库
      </el-button>
    </div>

    <div class="create-hero">
      <h1>新增 JD</h1>
      <p class="create-desc">填入岗位信息后保存，将自动加入岗位库并可用于匹配和面试。</p>
    </div>

    <div class="create-layout">
      <div class="form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="create-form">
      <el-form-item label="岗位名称" prop="jobTitle">
        <el-input v-model="form.jobTitle" placeholder="如：Java后端开发工程师" maxlength="200" />
      </el-form-item>

      <el-form-item label="公司名称" prop="companyName">
        <el-input v-model="form.companyName" placeholder="如：字节跳动（选填）" maxlength="200" />
      </el-form-item>

      <el-form-item label="城市" prop="city">
        <el-input v-model="form.city" placeholder="如：北京（选填）" maxlength="50" />
      </el-form-item>

      <el-form-item label="岗位来源" prop="platform">
        <el-input v-model="form.platform" placeholder="如：招聘平台（选填）" maxlength="50" />
      </el-form-item>

      <el-form-item label="岗位类型" prop="jobType">
        <el-select v-model="form.jobType" placeholder="选择岗位类型（选填）" clearable style="width: 100%">
          <el-option label="前端开发" value="frontend" />
          <el-option label="后端开发" value="backend" />
          <el-option label="算法/AI" value="algorithm" />
          <el-option label="测试" value="testing" />
          <el-option label="产品" value="product" />
          <el-option label="数据" value="data" />
          <el-option label="客户端" value="client" />
          <el-option label="运维/SRE" value="ops" />
          <el-option label="全栈" value="fullstack" />
          <el-option label="安全" value="security" />
        </el-select>
      </el-form-item>

      <el-form-item label="薪资范围" prop="salary">
        <el-input v-model="form.salary" placeholder="如：7000-10000元/月 或面议（选填）" maxlength="50" />
      </el-form-item>

      <el-form-item label="必备技能" prop="tags">
        <el-input
          v-model="form.tags"
          placeholder="多个标签用英文逗号分隔，如：Java, Spring Boot, MySQL"
          maxlength="500"
        />
      </el-form-item>

      <el-form-item label="岗位职责" prop="rawText">
        <el-input
          v-model="form.rawText"
          type="textarea"
          :rows="8"
          placeholder="描述岗位职责，如：负责后端业务接口开发，参与数据库表设计..."
          maxlength="50000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="岗位要求" prop="requirements">
        <el-input
          v-model="form.requirements"
          type="textarea"
          :rows="4"
          placeholder="学历要求、经验要求等，如：本科及以上学历，3-5年相关经验"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="行业">
        <el-input v-model="form.industry" placeholder="如：互联网（选填）" maxlength="50" />
      </el-form-item>

      <el-form-item label="公司规模">
        <el-input v-model="form.companySize" placeholder="如：1000-9999人（选填）" maxlength="50" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存 JD
        </el-button>
      </el-form-item>
        </el-form>
      </div>

      <!-- 右侧：保存结果面板 -->
      <div v-if="savedJob" class="result-panel">
        <h3 class="result-title">保存成功</h3>
        <div class="result-info">
          <div class="result-item">
            <span class="result-label">岗位名称</span>
            <span class="result-val">{{ savedJob.jobTitle }}</span>
          </div>
          <div class="result-item">
            <span class="result-label">公司名称</span>
            <span class="result-val">{{ savedJob.companyName || '-' }}</span>
          </div>
          <div class="result-item">
            <span class="result-label">解析状态</span>
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </div>
          <div class="result-item">
            <span class="result-label">保存时间</span>
            <span class="result-val">{{ savedJob.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
          </div>
        </div>

        <div class="result-actions">
          <el-button
            size="small"
            type="success"
            @click="selectSavedJobAsTarget"
            style="width: 100%"
          >
            {{ fromEditor ? '设为目标并返回工作台' : '设为目标岗位' }}
          </el-button>
          <el-button
            v-if="savedJob.parseStatus === 'pending' || savedJob.parseStatus === 'failed'"
            type="primary"
            size="small"
            @click="handleParse"
            :loading="parsing"
            style="width: 100%"
          >
            {{ savedJob.parseStatus === 'failed' ? '重新解析' : '解析岗位要求' }}
          </el-button>
          <el-button
            v-if="savedJob.parseStatus === 'succeeded'"
            size="small"
            @click="goToDetail"
            style="width: 100%"
          >
            查看详情
          </el-button>
        </div>

        <el-alert
          v-if="savedJob.parseStatus === 'failed'"
          title="解析失败，请重试"
          type="error"
          :closable="false"
          style="margin-top: 12px"
        />

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          closable
          style="margin-top: 12px"
          @close="errorMessage = ''"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createJobDescription, parseJobDescription } from '../api/job'
import type { JobDescription } from '../types/job'
import {
  cancelPendingWorkspaceAction,
  markPendingWorkspaceSelectedJobId,
  markReturnToEditor,
  setWorkspaceSelectedJobId,
} from '../utils/workspaceContext'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

const form = ref({
  jobTitle: '',
  companyName: '',
  city: '',
  rawText: '',
  jobType: '' as string,
  salary: '',
  platform: '',
  requirements: '',
  industry: '',
  companySize: '',
  tags: '',
})

const rules: FormRules = {
  jobTitle: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { min: 1, max: 200, message: '岗位名称长度 1-200', trigger: 'blur' },
  ],
  rawText: [
    { required: true, message: '请粘贴岗位职责', trigger: 'blur' },
    { min: 20, max: 50000, message: '岗位职责长度 20-50000 字符', trigger: 'blur' },
  ],
  requirements: [
    { required: true, message: '请填写岗位要求', trigger: 'blur' },
  ],
  tags: [
    { required: true, message: '请填写必备技能', trigger: 'blur' },
  ],
}

const saving = ref(false)
const parsing = ref(false)
const savedJob = ref<JobDescription | null>(null)
const errorMessage = ref('')
const fromEditor = computed(() => route.query.from === 'editor')

function readRouteString(key: string): string | undefined {
  const value = route.query[key]
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue ? rawValue : undefined
}

function editorContextQuery() {
  if (!fromEditor.value) return undefined
  const query: Record<string, string> = { from: 'editor' }
  const resumeId = readRouteString('resumeId')
  const versionId = readRouteString('versionId')
  const mode = readRouteString('mode')
  if (resumeId) query.resumeId = resumeId
  if (versionId) query.versionId = versionId
  if (mode) query.mode = mode
  return query
}

function editorReturnQuery() {
  const query: Record<string, string> = { editor: '1' }
  const resumeId = readRouteString('resumeId')
  const versionId = readRouteString('versionId')
  const mode = readRouteString('mode')
  if (resumeId) query.resumeId = resumeId
  if (versionId) query.versionId = versionId
  if (mode) query.mode = mode
  return query
}

const statusTagType = computed(() => {
  switch (savedJob.value?.parseStatus) {
    case 'succeeded': return 'success'
    case 'failed': return 'danger'
    default: return 'info'
  }
})

const statusLabel = computed(() => {
  switch (savedJob.value?.parseStatus) {
    case 'succeeded': return '已解析'
    case 'failed': return '解析失败'
    default: return '待解析'
  }
})

function buildRawText(): string {
  const parts: string[] = []
  if (form.value.rawText.trim()) {
    parts.push(`岗位职责：${form.value.rawText.trim()}`)
  }
  if (form.value.requirements.trim()) {
    parts.push(`岗位要求：${form.value.requirements.trim()}`)
  }
  return parts.join('\n')
}

function buildSourceMetaJson(): string | undefined {
  const meta: Record<string, unknown> = {}
  if (form.value.city) meta.base = form.value.city
  if (form.value.salary) meta.salary = form.value.salary
  if (form.value.platform) meta.platform = form.value.platform
  if (form.value.industry) meta.industry = form.value.industry
  if (form.value.companySize) meta.companySize = form.value.companySize
  if (form.value.tags.trim()) {
    meta.tags = form.value.tags.split(',').map(t => t.trim()).filter(Boolean)
  }
  return Object.keys(meta).length > 0 ? JSON.stringify(meta) : undefined
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  errorMessage.value = ''
  try {
    const res = await createJobDescription({
      jobTitle: form.value.jobTitle,
      companyName: form.value.companyName || undefined,
      rawText: buildRawText(),
      jobType: form.value.jobType || undefined,
      sourceMetaJson: buildSourceMetaJson(),
    })
    savedJob.value = res.data
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleParse() {
  if (!savedJob.value) return

  parsing.value = true
  errorMessage.value = ''
  try {
    const res = await parseJobDescription(savedJob.value.id)
    savedJob.value = res.data
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '解析失败'
  } finally {
    parsing.value = false
  }
}

function goToDetail() {
  if (savedJob.value) {
    router.push({
      name: 'job-detail',
      params: { id: savedJob.value.id },
      query: editorContextQuery(),
    })
  }
}

function goBack() {
  router.push({
    name: 'jobs',
    query: editorContextQuery(),
  })
}

function returnToEditor(cancelPendingAction = true) {
  if (cancelPendingAction) {
    cancelPendingWorkspaceAction()
  }
  markReturnToEditor()
  router.push({ name: 'home', query: editorReturnQuery() })
}

function selectSavedJobAsTarget() {
  if (!savedJob.value) return
  setWorkspaceSelectedJobId(savedJob.value.id)
  if (fromEditor.value) {
    markPendingWorkspaceSelectedJobId(savedJob.value.id)
  }
  ElMessage.success(`已将「${savedJob.value.jobTitle}」设为目标岗位`)
  if (fromEditor.value) {
    returnToEditor(false)
  }
}
</script>

<style scoped>
.job-create {
  max-width: 1060px;
  margin: 0 auto;
  padding: 24px;
}

.create-header-bar {
  margin-bottom: 16px;
}

.create-hero {
  margin-bottom: 24px;
}

.create-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.create-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

/* 双栏布局 */
.create-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.form-card {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

/* 右侧结果面板 */
.result-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 24px;
}

.result-panel .el-alert {
  border-radius: 10px;
}

.result-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.result-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}

.result-val {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}

.result-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-actions .el-button {
  border-radius: 10px;
  font-weight: 600;
}

.create-form :deep(.el-button--primary) {
  background: #0f172a;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  padding: 10px 24px;
}

.create-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  background: #f8fafc;
  box-shadow: none;
}

.create-form :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}

.create-form :deep(.el-textarea__inner) {
  border-radius: 10px;
  background: #f8fafc;
}

.workspace-return-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0 0 18px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  padding: 10px 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.workspace-return-bar button {
  border: 0;
  border-radius: 10px;
  background: #0f172a;
  color: #fff;
  cursor: pointer;
  font-weight: 800;
  padding: 8px 14px;
  font-size: 13px;
}

.workspace-return-bar span {
  color: #64748b;
  font-size: 13px;
}
</style>
