<template>
  <div class="page job-explore-page">
    <div v-if="fromEditor" class="workspace-return-bar">
      <button type="button" @click="returnToEditor()">← 返回当前简历工作台</button>
      <span>选择一个岗位后将作为当前简历的目标 JD，返回后自动用于评分、建议和面试。</span>
    </div>

    <section class="explore-hero">
      <div>
        <p class="eyebrow">岗位库</p>
        <h1>选择一个真实 JD，开始定向优化</h1>
        <p class="page-desc">
          搜索岗位、公司或技能要求，按城市、薪资、类型筛选合适的 JD。
        </p>
      </div>
      <div class="explore-actions">
        <el-button @click="loadJobs" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openCreateJob">
          新增 JD
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

    <section class="job-toolbar">
      <div class="search-row">
        <el-input
          v-model="searchInput"
          class="job-search-input"
          clearable
          placeholder="搜索岗位、公司、技能关键词"
          @keyup.enter="doSearch"
          @clear="doSearch"
        >
          <template #prefix>
            <el-icon style="cursor: pointer" @click.stop="doSearch"><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="doSearch">搜索</el-button>
      </div>

      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">岗位类型</span>
          <el-select v-model="typeFilter" placeholder="全部类型" clearable size="small" style="width: 120px">
            <el-option
              v-for="t in recruitTypeOptions"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">岗位方向</span>
          <el-select v-model="categoryFilter" placeholder="全部方向" clearable size="small" style="width: 130px">
            <el-option
              v-for="c in categoryOptions"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">城市</span>
          <el-select v-model="cityFilter" placeholder="全部城市" clearable size="small" style="width: 140px">
            <el-option
              v-for="city in cityOptions"
              :key="city"
              :label="city"
              :value="city"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">薪资</span>
          <el-select v-model="salaryFilter" placeholder="全部薪资" clearable size="small" style="width: 140px">
            <el-option
              v-for="s in salaryOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </div>
      </div>

      <span class="job-count">共 {{ filteredJobs.length }} 个岗位</span>
    </section>

    <section class="job-board">
      <div v-if="loading" class="loading-box">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <el-empty v-else-if="jobs.length === 0" description="暂无岗位 JD">
        <el-button type="primary" @click="openCreateJob">
          现在粘贴一份 JD
        </el-button>
      </el-empty>

      <el-empty v-else-if="filteredJobs.length === 0" description="没有匹配的岗位">
        <el-button @click="clearFilters">清空筛选</el-button>
      </el-empty>

      <div v-else class="compact-job-grid">
        <article
          v-for="job in filteredJobs"
          :key="job.id"
          class="compact-job-card"
          :class="{ selected: job.id === selectedWorkspaceJobId }"
          @click="openJob(job.id)"
        >
          <button class="card-delete-btn" title="删除" @click.stop="handleDelete(job)">×</button>
          <div class="job-card-top">
            <CompanyAvatar :job="job" size="md" />
            <div class="job-title-block">
              <h3>{{ job.jobTitle }}</h3>
              <p>{{ job.companyName || '未填写公司' }}</p>
            </div>
          </div>

          <div class="job-meta-row">
            <span>{{ job.sourceMeta?.industry || '行业未填' }}</span>
            <span v-if="job.sourceMeta?.base">{{ job.sourceMeta.base }}</span>
            <span v-if="job.sourceMeta?.salary" class="meta-salary">{{ job.sourceMeta.salary }}</span>
          </div>

          <div class="job-skill-row">
            <span
              v-for="skill in previewSkills(job)"
              :key="skill"
            >
              {{ skill }}
            </span>
          </div>

          <div v-if="job.parsed" class="job-parsed-preview">
            <p v-if="job.parsed.responsibilities?.length">
              <span class="preview-label">职责</span>
              {{ job.parsed.responsibilities.slice(0, 2).join('；') }}
            </p>
            <p v-if="job.parsed.experienceRequirements?.length">
              <span class="preview-label">经验</span>
              {{ job.parsed.experienceRequirements.slice(0, 2).join('；') }}
            </p>
            <p>
              <span class="preview-label">学历</span>
              {{ job.parsed.educationRequirements?.length ? job.parsed.educationRequirements.slice(0, 2).join('；') : '不限' }}
            </p>
          </div>

          <div class="job-card-actions">
            <span v-if="job.id === selectedWorkspaceJobId" class="current-target-badge">当前目标</span>
            <button type="button" @click.stop="selectTargetJob(job)">
              {{ job.id === selectedWorkspaceJobId ? '已选中' : fromEditor ? '设为目标并返回' : '设为目标岗位' }}
            </button>
          </div>

        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listJobDescriptions, deleteJobDescription } from '../api/job'
import CompanyAvatar from '../components/CompanyAvatar.vue'
import type { JobDescription } from '../types/job'
import {
  cancelPendingWorkspaceAction,
  clearWorkspaceSelectedJobId,
  getWorkspaceSelectedJobId,
  markPendingWorkspaceSelectedJobId,
  markReturnToEditor,
  setWorkspaceSelectedJobId,
} from '../utils/workspaceContext'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const jobs = ref<JobDescription[]>([])
const searchInput = ref('')
const searchKeyword = ref('')
const typeFilter = ref('')
const categoryFilter = ref('')
const cityFilter = ref('')
const salaryFilter = ref('')
const selectedWorkspaceJobId = ref<number | null>(getWorkspaceSelectedJobId())

const recruitTypeOptions = [
  { label: '实习', value: 'internship' },
  { label: '校招', value: 'campus' },
  { label: '社招', value: 'social' },
]

const categoryOptions = computed(() => {
  const industries = new Set<string>()
  jobs.value.forEach((job) => {
    if (job.sourceMeta?.industry) {
      industries.add(job.sourceMeta.industry)
    }
  })
  return Array.from(industries).sort().map((v) => ({ label: v, value: v }))
})

const salaryOptions = [
  { label: '面议', value: 'negotiable' },
  { label: '5K 以下', value: '0-5' },
  { label: '5K-10K', value: '5-10' },
  { label: '10K-20K', value: '10-20' },
  { label: '20K-30K', value: '20-30' },
  { label: '30K 以上', value: '30+' },
]

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

const cityOptions = computed(() => {
  const cities = new Set<string>()
  jobs.value.forEach((job) => {
    if (job.sourceMeta?.base) {
      cities.add(job.sourceMeta.base)
    }
  })
  return Array.from(cities).sort()
})

function parseSalaryRange(salary: string | undefined): string | null {
  if (!salary) return null
  // 含有"面议"等字样
  if (/面议|薪资open|open/i.test(salary)) return 'negotiable'

  // 匹配中文格式，如 "9,482-15,409元/月"、"6,615-10,750元/月"
  const cnMatch = salary.match(/([\d,]+)\s*[-–—至到]\s*([\d,]+)\s*元?\/?月?/)
  if (cnMatch) {
    const low = Number(cnMatch[1].replace(/,/g, ''))
    const high = Number(cnMatch[2].replace(/,/g, ''))
    const avg = (low + high) / 2 / 1000 // 转为 k
    if (avg < 5) return '0-5'
    if (avg < 10) return '5-10'
    if (avg < 20) return '10-20'
    if (avg < 30) return '20-30'
    return '30+'
  }

  // 匹配英文格式，如 "15k-25k"、"20K-30K"
  const enMatch = salary.match(/(\d+)\s*[kK]\s*[-–—至到]\s*(\d+)\s*[kK]/)
  if (enMatch) {
    const avg = (Number(enMatch[1]) + Number(enMatch[2])) / 2
    if (avg < 5) return '0-5'
    if (avg < 10) return '5-10'
    if (avg < 20) return '10-20'
    if (avg < 30) return '20-30'
    return '30+'
  }

  // 匹配单个 k 值，如 "15k"、"20K"
  const single = salary.match(/(\d+)\s*[kK]/)
  if (single) {
    const val = Number(single[1])
    if (val < 5) return '0-5'
    if (val < 10) return '5-10'
    if (val < 20) return '10-20'
    if (val < 30) return '20-30'
    return '30+'
  }

  return null
}

function doSearch() {
  searchKeyword.value = searchInput.value.trim()
}

const filteredJobs = computed(() => {
  const q = searchKeyword.value.toLowerCase()
  return jobs.value.filter((job) => {
    const typeMatched = !typeFilter.value || job.jobType === typeFilter.value

    const categoryMatched = !categoryFilter.value || job.sourceMeta?.industry === categoryFilter.value

    const cityMatched = !cityFilter.value || job.sourceMeta?.base === cityFilter.value

    let salaryMatched = true
    if (salaryFilter.value) {
      const range = parseSalaryRange(job.sourceMeta?.salary)
      if (salaryFilter.value === 'negotiable') {
        salaryMatched = range === 'negotiable' || range === null
      } else if (!range) {
        salaryMatched = false
      } else {
        salaryMatched = range === salaryFilter.value
      }
    }

    const searchText = [
      job.jobTitle,
      job.companyName,
      job.rawText,
      ...(job.parsed?.requiredSkills ?? []),
      ...(job.parsed?.preferredSkills ?? []),
      ...(job.parsed?.responsibilities ?? []),
    ].join(' ').toLowerCase()
    const keywordMatched = !q || searchText.includes(q)
    return typeMatched && categoryMatched && cityMatched && salaryMatched && keywordMatched
  })
})

onMounted(() => {
  loadJobs()
})

async function loadJobs() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await listJobDescriptions()
    jobs.value = res.data
    if (selectedWorkspaceJobId.value && !res.data.some((job) => job.id === selectedWorkspaceJobId.value)) {
      selectedWorkspaceJobId.value = null
      clearWorkspaceSelectedJobId()
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载岗位 JD 失败'
  } finally {
    loading.value = false
  }
}

function openJob(id: number) {
  router.push({
    name: 'job-detail',
    params: { id },
    query: editorContextQuery(),
  })
}

function openCreateJob() {
  router.push({
    name: 'job-create',
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

function selectTargetJob(job: JobDescription) {
  selectedWorkspaceJobId.value = job.id
  setWorkspaceSelectedJobId(job.id)
  if (fromEditor.value) {
    markPendingWorkspaceSelectedJobId(job.id)
  }
  ElMessage.success(`已将「${job.jobTitle}」设为目标岗位`)
  if (fromEditor.value) {
    returnToEditor(false)
  }
}

async function handleDelete(job: JobDescription) {
  try {
    await ElMessageBox.confirm(
      `确定删除"${job.jobTitle}"吗？此操作不可恢复。`,
      '删除岗位',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
    await deleteJobDescription(job.id)
    jobs.value = jobs.value.filter(j => j.id !== job.id)
  } catch {
    // 用户取消
  }
}

function clearFilters() {
  searchInput.value = ''
  searchKeyword.value = ''
  typeFilter.value = ''
  categoryFilter.value = ''
  cityFilter.value = ''
  salaryFilter.value = ''
}

function previewSkills(job: JobDescription) {
  return job.parsed?.requiredSkills?.slice(0, 5) ?? []
}

</script>

<style scoped>
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

.compact-job-card.selected {
  position: relative;
  border-color: rgba(16, 168, 120, 0.58);
  box-shadow: 0 18px 42px rgba(16, 168, 120, 0.13);
}

.compact-job-card.selected::after {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  content: '';
  background: linear-gradient(90deg, #10a878, #34d399);
}

.job-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f7;
}

.current-target-badge {
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 11px;
  font-weight: 900;
  padding: 4px 8px;
}

.job-card-actions button {
  margin-left: auto;
  border: 1px solid rgba(16, 168, 120, 0.28);
  border-radius: 999px;
  background: #ffffff;
  color: #047857;
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 7px 11px;
}

.job-card-actions button:hover {
  border-color: rgba(16, 168, 120, 0.56);
  background: #ecfdf5;
}

</style>
