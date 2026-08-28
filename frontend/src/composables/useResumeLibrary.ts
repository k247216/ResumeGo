import { computed, ref } from 'vue'
import {
  archiveResume,
  deleteResume,
  forkResumeVersion,
  getResumeVersions,
  listResumes,
  renameResume,
  restoreResume,
} from '../api/resume'
import type { Resume, ResumeVersion } from '../types/resume'
import { isResumeFavorite } from '../utils/resumeFavorite'

export type ResumeLibraryKindFilter = 'all' | 'general' | 'expression' | 'favorites' | 'archived'

export interface ResumeLibraryFilter {
  kind: ResumeLibraryKindFilter
  keyword: string
}

/**
 * 简历库资产状态：Library → Workspace → Inspector 共用的单一事实来源。
 * 不自动选择或修改任何 Pipeline 绑定；历史引用来自真实数据。
 */
export function useResumeLibrary() {
  const resumes = ref<Resume[]>([])
  const selectedResumeId = ref<number | null>(null)
  const versions = ref<ResumeVersion[]>([])
  const selectedVersionId = ref<number | null>(null)
  const loading = ref(false)
  const versionLoading = ref(false)
  const errorMessage = ref('')
  const versionError = ref('')
  const filter = ref<ResumeLibraryFilter>({ kind: 'all', keyword: '' })
  const favoriteRevision = ref(0)

  /** 别名：资产语义命名 */
  const items = resumes
  const error = errorMessage

  const selectedResume = computed(() => resumes.value.find((resume) => resume.id === selectedResumeId.value) ?? null)
  const selectedVersion = computed(() => versions.value.find((version) => version.id === selectedVersionId.value) ?? selectedResume.value?.currentVersion ?? null)

  /** 关键词标题过滤（客户端，不产生额外请求） */
  const visibleItems = computed(() => {
    void favoriteRevision.value
    const keyword = filter.value.keyword.trim().toLowerCase()
    const base = filter.value.kind === 'favorites'
      ? resumes.value.filter((resume) => isResumeFavorite(resume.id))
      : resumes.value
    if (!keyword) return base
    return base.filter((resume) => resume.title.toLowerCase().includes(keyword))
  })

  function refreshFavorites() {
    favoriteRevision.value += 1
  }

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
    const kind = filter.value.kind === 'general'
      ? 'GENERAL'
      : filter.value.kind === 'expression' ? 'JOB_EXPRESSION' : undefined
      const archived = filter.value.kind === 'archived' ? true : false
      const response = await listResumes(kind, archived)
      resumes.value = response.data
      const selectable = filter.value.kind === 'favorites' ? visibleItems.value : resumes.value
      const current = selectable.find((resume) => resume.id === selectedResumeId.value) ?? selectable[0] ?? null
      if (current) await selectResume(current.id)
      else {
        selectedResumeId.value = null
        selectedVersionId.value = null
        versions.value = []
      }
    } catch (err) {
      errorMessage.value = err instanceof Error ? err.message : '读取本地简历失败'
    } finally {
      loading.value = false
    }
  }

  async function selectResume(id: number | null) {
    if (id == null) {
      selectedResumeId.value = null
      selectedVersionId.value = null
      versions.value = []
      return
    }
    const resume = resumes.value.find((item) => item.id === id)
    if (!resume) return
    selectedResumeId.value = id
    selectedVersionId.value = resume.currentVersion?.id ?? null
    versionLoading.value = true
    versionError.value = ''
    try {
      const response = await getResumeVersions(id)
      if (selectedResumeId.value !== id) return
      versions.value = response.data
      if (!versions.value.some((version) => version.id === selectedVersionId.value)) {
        selectedVersionId.value = versions.value[0]?.id ?? null
      }
    } catch (err) {
      if (selectedResumeId.value === id) {
        versions.value = resume.currentVersion ? [resume.currentVersion] : []
        versionError.value = err instanceof Error ? err.message : '读取版本历史失败'
      }
    } finally {
      if (selectedResumeId.value === id) versionLoading.value = false
    }
  }

  const select = selectResume

  function selectVersion(id: number) {
    if (versions.value.some((version) => version.id === id)) selectedVersionId.value = id
  }

  /** fork 后刷新并选中新资产。 */
  async function fork(versionId: number, title: string): Promise<Resume> {
    const res = await forkResumeVersion(versionId, title)
    await load()
    if (resumes.value.some((item) => item.id === res.data.id)) {
      await selectResume(res.data.id)
    }
    return res.data
  }

  /** 归档后从默认列表移除，选中回退到剩余资产。 */
  async function archive(resumeId: number): Promise<void> {
    await archiveResume(resumeId)
    if (selectedResumeId.value === resumeId) {
      selectedResumeId.value = null
      versions.value = []
      selectedVersionId.value = null
    }
    await load()
  }

  async function restore(resumeId: number): Promise<void> {
    await restoreResume(resumeId)
    await load()
  }

  async function rename(resumeId: number, title: string): Promise<void> {
    await renameResume(resumeId, title)
    await load()
  }

  async function remove(id: number) {
    await deleteResume(id)
    if (selectedResumeId.value === id) {
      selectedResumeId.value = null
      versions.value = []
      selectedVersionId.value = null
    }
    await load()
  }

  return {
    resumes,
    items,
    visibleItems,
    selectedResumeId,
    selectedResume,
    versions,
    selectedVersionId,
    selectedVersion,
    loading,
    versionLoading,
    errorMessage,
    error,
    versionError,
    filter,
    refreshFavorites,
    load,
    selectResume,
    select,
    selectVersion,
    fork,
    archive,
    restore,
    rename,
    remove,
  }
}
