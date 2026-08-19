<template>
  <div class="page resume-studio-page">
    <section class="resume-hero">
      <div>
        <p class="eyebrow">简历中心</p>
        <h1>管理简历版本，追踪每一次优化</h1>
        <p class="page-desc">
          查看当前版本、切换历史版本，并进入评分与岗位匹配。页面默认面向演示和产品使用，隐藏底层数据格式。
        </p>
      </div>
      <div class="resume-hero-actions">
        <el-button @click="loadResumes" :loading="loading">刷新</el-button>
        <el-button
          type="primary"
          :disabled="!activeVersion"
          @click="openAssessment"
        >
          进入评分与匹配
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      @close="errorMessage = ''"
    />

    <section class="resume-studio-layout">
      <aside class="resume-library">
        <div class="library-head">
          <span>我的简历</span>
          <el-tag>{{ resumes.length }} 份</el-tag>
        </div>

        <div v-if="loading" class="loading-box">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <p>加载中...</p>
        </div>

        <el-empty v-else-if="resumes.length === 0" description="暂无简历" />

        <button
          v-for="item in resumes"
          v-else
          :key="item.id"
          class="library-card"
          :class="{ active: selectedResume?.id === item.id }"
          @click="selectResume(item)"
        >
          <span class="library-icon">
            <el-icon><Document /></el-icon>
          </span>
          <span>
            <strong>{{ item.title }}</strong>
            <small>当前版本 v{{ item.currentVersion?.versionNo ?? '-' }}</small>
          </span>
        </button>
      </aside>

      <main class="resume-workspace">
        <el-empty v-if="!selectedResume" description="请选择一份简历" />

        <template v-else>
          <section class="resume-overview-card">
            <div class="resume-cover">
              <div class="paper-lines">
                <i></i>
                <i></i>
                <i></i>
              </div>
              <span>v{{ activeVersion?.versionNo ?? '-' }}</span>
            </div>

            <div class="overview-main">
              <div class="overview-title-line">
                <h2>{{ selectedResume.title }}</h2>
                <el-tag type="success" effect="plain">
                  {{ viewingVersion ? '历史版本' : '当前版本' }}
                </el-tag>
              </div>
              <p>
                {{ content?.basicInfo?.name || '未填写姓名' }}
                <span>·</span>
                {{ content?.basicInfo?.targetRole || '目标岗位待补充' }}
              </p>
              <div class="overview-stats">
                <div>
                  <small>项目经历</small>
                  <strong>{{ projectCount }}</strong>
                </div>
                <div>
                  <small>技能标签</small>
                  <strong>{{ skillCount }}</strong>
                </div>
                <div>
                  <small>版本数量</small>
                  <strong>{{ versions.length || 1 }}</strong>
                </div>
              </div>
            </div>

            <div class="overview-actions">
              <button type="button" @click="openAssessment">
                开始诊断
                <el-icon><ArrowRight /></el-icon>
              </button>
              <small>{{ activeVersion?.changeSummary || '保留原版本，修改过程可追溯' }}</small>
            </div>
          </section>

          <section class="resume-version-panel">
            <div class="section-title-row">
              <div>
                <h3>版本历史</h3>
                <p>切换版本查看内容，采纳 AI 建议后会生成新的版本。</p>
              </div>
              <el-tag>{{ versions.length || 1 }} 个版本</el-tag>
            </div>

            <div class="version-timeline">
              <button
                v-for="ver in displayedVersions"
                :key="ver.id"
                class="timeline-item"
                :class="{ active: activeVersion?.id === ver.id }"
                @click="switchVersion(ver)"
              >
                <span class="timeline-dot"></span>
                <strong>v{{ ver.versionNo }}</strong>
                <small>{{ createdByLabel(ver.createdByType) }}</small>
                <em>{{ formatTime(ver.createdAt) }}</em>
              </button>
            </div>
          </section>

          <section class="resume-content-preview" v-if="content">
            <div class="section-title-row">
              <div>
                <h3>简历内容预览</h3>
                <p>用产品化视图查看结构化内容，避免演示时暴露底层数据格式。</p>
              </div>
              <el-button
                v-if="activeVersion"
                text
                type="primary"
                @click="reloadVersion(activeVersion.id)"
                :loading="versionLoading"
              >
                重新读取
              </el-button>
            </div>

            <div class="resume-preview-grid">
              <article class="preview-section">
                <div class="preview-section-head">
                  <el-icon><User /></el-icon>
                  <strong>基本信息</strong>
                </div>
                <div class="profile-row">
                  <span>姓名</span>
                  <strong>{{ content.basicInfo?.name || '-' }}</strong>
                </div>
                <div class="profile-row">
                  <span>目标岗位</span>
                  <strong>{{ content.basicInfo?.targetRole || '-' }}</strong>
                </div>
              </article>

              <article class="preview-section">
                <div class="preview-section-head">
                  <el-icon><School /></el-icon>
                  <strong>教育经历</strong>
                </div>
                <div
                  v-for="(edu, index) in content.education || []"
                  :key="index"
                  class="compact-line"
                >
                  <strong>{{ edu.school || '学校待补充' }}</strong>
                  <span>{{ edu.major || '-' }}｜{{ edu.degree || '-' }}｜{{ edu.period || '-' }}</span>
                </div>
                <p v-if="!content.education?.length" class="muted">暂无教育经历</p>
              </article>

              <article class="preview-section wide">
                <div class="preview-section-head">
                  <el-icon><Collection /></el-icon>
                  <strong>项目 / 经历</strong>
                </div>
                <div
                  v-for="(project, index) in content.projects || []"
                  :key="index"
                  class="experience-preview"
                >
                  <div>
                    <strong>{{ project.title || `项目 ${index + 1}` }}</strong>
                    <el-tag v-if="project.evidenceId" type="success" size="small" effect="plain">
                      已关联证据
                    </el-tag>
                    <el-tag v-else type="warning" size="small" effect="plain">
                      待补证据
                    </el-tag>
                  </div>
                  <p>{{ project.description || '-' }}</p>
                </div>
              </article>

              <article class="preview-section">
                <div class="preview-section-head">
                  <el-icon><PriceTag /></el-icon>
                  <strong>技能标签</strong>
                </div>
                <div class="resume-skill-cloud">
                  <span v-for="skill in content.skills || []" :key="skill">{{ skill }}</span>
                  <p v-if="!content.skills?.length" class="muted">暂无技能标签</p>
                </div>
              </article>
            </div>
          </section>
        </template>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Collection,
  Document,
  Loading,
  PriceTag,
  School,
  User,
} from '@element-plus/icons-vue'
import { getResumeVersion, getResumeVersions, listResumes } from '../api/resume'
import type { Resume, ResumeContent, ResumeVersion } from '../types/resume'

const router = useRouter()
const loading = ref(false)
const versionLoading = ref(false)
const errorMessage = ref('')
const resumes = ref<Resume[]>([])
const selectedResume = ref<Resume | null>(null)
const currentVersion = ref<ResumeVersion | null>(null)
const versions = ref<ResumeVersion[]>([])
const viewingVersion = ref<ResumeVersion | null>(null)

const activeVersion = computed(() => viewingVersion.value ?? currentVersion.value)
const content = computed<ResumeContent | null>(() => activeVersion.value?.content ?? null)
const projectCount = computed(() => content.value?.projects?.length ?? 0)
const skillCount = computed(() => content.value?.skills?.length ?? 0)
const displayedVersions = computed(() => {
  if (versions.value.length > 0) return versions.value
  return currentVersion.value ? [currentVersion.value] : []
})

onMounted(() => {
  loadResumes()
})

async function loadResumes() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await listResumes()
    resumes.value = res.data
    if (!selectedResume.value && resumes.value.length > 0) {
      selectResume(resumes.value[0])
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载简历失败'
  } finally {
    loading.value = false
  }
}

function selectResume(resume: Resume) {
  selectedResume.value = resume
  currentVersion.value = resume.currentVersion ?? null
  viewingVersion.value = null
  loadVersions(resume.id)
}

async function loadVersions(resumeId: number) {
  versions.value = []
  try {
    const res = await getResumeVersions(resumeId)
    versions.value = res.data
  } catch {
    // versions 保持空，不阻塞主流程
  }
}

function switchVersion(ver: ResumeVersion) {
  viewingVersion.value = ver.id === currentVersion.value?.id ? null : ver
}

async function reloadVersion(versionId: number) {
  versionLoading.value = true
  errorMessage.value = ''
  try {
    const res = await getResumeVersion(versionId)
    if (versionId === currentVersion.value?.id) {
      currentVersion.value = res.data
    }
    viewingVersion.value = res.data
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '读取简历版本失败'
  } finally {
    versionLoading.value = false
  }
}

function createdByLabel(type: string) {
  if (type === 'ai_suggestion') return 'AI 建议生成'
  if (type === 'user') return '手动维护'
  if (type === 'system') return '系统初始化'
  return type
}

function formatTime(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function openAssessment() {
  if (!activeVersion.value) return
  router.push({
    name: 'resume-assessment',
    params: { versionId: activeVersion.value.id },
  })
}
</script>
