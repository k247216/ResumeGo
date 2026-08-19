<template>
  <div class="page evidence-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Sprint 1｜能力证据</p>
        <h1>能力证据库</h1>
        <p class="page-desc">
          先把真实经历录入证据库。后续简历优化建议必须引用证据，AI 不允许编造经历。
        </p>
      </div>
      <el-button type="primary" @click="loadEvidences" :loading="loading">
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      @close="errorMessage = ''"
    />

    <div class="evidence-grid">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>新增能力证据</span>
            <el-tag type="success" effect="plain">本地资料库</el-tag>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
        >
          <el-form-item label="经历类型" prop="evidenceType">
            <el-select v-model="form.evidenceType" placeholder="选择类型" style="width: 100%">
              <el-option label="项目经历" value="project" />
              <el-option label="实习经历" value="internship" />
              <el-option label="竞赛经历" value="competition" />
              <el-option label="技能证据" value="skill" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>

          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="如：校园二手交易小程序" maxlength="200" />
          </el-form-item>

          <el-form-item label="背景/任务" prop="situation">
            <el-input
              v-model="form.situation"
              type="textarea"
              :rows="3"
              placeholder="这段经历发生在什么场景？要解决什么问题？"
            />
          </el-form-item>

          <el-form-item label="实际行动" prop="actionText">
            <el-input
              v-model="form.actionText"
              type="textarea"
              :rows="4"
              placeholder="你实际做了什么？这是后续简历建议的事实依据。"
            />
          </el-form-item>

          <el-form-item label="结果/量化指标" prop="resultText">
            <el-input
              v-model="form.resultText"
              type="textarea"
              :rows="3"
              placeholder="如：完成 8 个页面、支撑 200 条测试数据。可以先为空。"
            />
          </el-form-item>

          <el-form-item label="技能标签" prop="skillTagsText">
            <el-input
              v-model="form.skillTagsText"
              placeholder="用中文或英文逗号分隔，如：Java, Spring Boot, MySQL"
            />
          </el-form-item>

          <el-form-item label="来源说明" prop="sourceNote">
            <el-input
              v-model="form.sourceNote"
              placeholder="如：课程项目、GitLab 仓库、证书编号等"
              maxlength="500"
            />
          </el-form-item>

          <el-button type="primary" :loading="saving" @click="handleCreate">
            保存证据
          </el-button>
          <el-button @click="resetForm">清空</el-button>
        </el-form>
      </el-card>

      <div class="list-panel">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>已有证据</span>
              <el-tag>{{ evidences.length }} 条</el-tag>
            </div>
          </template>

          <div v-if="loading" class="loading-box">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
            <p>加载中...</p>
          </div>

          <el-empty v-else-if="evidences.length === 0" description="暂无能力证据" />

          <div v-else class="evidence-list">
            <el-card
              v-for="item in evidences"
              :key="item.id"
              class="evidence-item"
              shadow="hover"
            >
              <div class="evidence-title-row">
                <h3>{{ item.title }}</h3>
                <el-tag :type="typeTag(item.evidenceType)">
                  {{ typeLabel(item.evidenceType) }}
                </el-tag>
              </div>

              <p class="muted">行动：{{ item.actionText }}</p>
              <p v-if="item.resultText" class="muted">结果：{{ item.resultText }}</p>

              <div class="tag-list">
                <el-tag
                  v-for="skill in item.skillTags"
                  :key="`${item.id}-${skill}`"
                  effect="plain"
                >
                  {{ skill }}
                </el-tag>
              </div>
            </el-card>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { createEvidence, listEvidences } from '../api/evidence'
import type { CapabilityEvidence, EvidenceType } from '../types/evidence'

interface EvidenceForm {
  evidenceType: EvidenceType
  title: string
  situation: string
  actionText: string
  resultText: string
  skillTagsText: string
  sourceNote: string
}

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const evidences = ref<CapabilityEvidence[]>([])

const form = reactive<EvidenceForm>({
  evidenceType: 'project',
  title: '',
  situation: '',
  actionText: '',
  resultText: '',
  skillTagsText: '',
  sourceNote: '',
})

const rules: FormRules = {
  evidenceType: [{ required: true, message: '请选择经历类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  actionText: [{ required: true, message: '请输入实际行动', trigger: 'blur' }],
  skillTagsText: [{ required: true, message: '请至少填写一个技能标签', trigger: 'blur' }],
}

onMounted(() => {
  loadEvidences()
})

async function loadEvidences() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await listEvidences()
    evidences.value = res.data
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载能力证据失败'
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const skillTags = parseSkillTags(form.skillTagsText)
  if (skillTags.length === 0) {
    errorMessage.value = '请至少填写一个技能标签'
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    const res = await createEvidence({
      evidenceType: form.evidenceType,
      title: form.title.trim(),
      situation: form.situation.trim() || undefined,
      actionText: form.actionText.trim(),
      resultText: form.resultText.trim() || undefined,
      skillTags,
      sourceNote: form.sourceNote.trim() || undefined,
    })
    evidences.value = [...evidences.value, res.data]
    ElMessage.success('能力证据已保存')
    resetForm()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存能力证据失败'
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.evidenceType = 'project'
  form.title = ''
  form.situation = ''
  form.actionText = ''
  form.resultText = ''
  form.skillTagsText = ''
  form.sourceNote = ''
  formRef.value?.clearValidate()
}

function parseSkillTags(text: string) {
  return text
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

function typeLabel(type: EvidenceType) {
  const labels: Record<EvidenceType, string> = {
    project: '项目',
    internship: '实习',
    competition: '竞赛',
    skill: '技能',
    other: '其他',
  }
  return labels[type]
}

function typeTag(type: EvidenceType) {
  const tags: Record<EvidenceType, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    project: 'primary',
    internship: 'success',
    competition: 'warning',
    skill: 'info',
    other: 'info',
  }
  return tags[type]
}
</script>
