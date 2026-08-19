import { computed, ref } from 'vue'
import { getResumeVersions, listResumes } from '../api/resume'
import type { Resume, ResumeVersion } from '../types/resume'

export function useResumeLibrary() {
  const resumes = ref<Resume[]>([])
  const selectedResumeId = ref<number | null>(null)
  const versions = ref<ResumeVersion[]>([])
  const selectedVersionId = ref<number | null>(null)
  const loading = ref(false)
  const versionLoading = ref(false)
  const errorMessage = ref('')
  const versionError = ref('')

  const selectedResume = computed(() => resumes.value.find((resume) => resume.id === selectedResumeId.value) ?? null)
  const selectedVersion = computed(() => versions.value.find((version) => version.id === selectedVersionId.value) ?? selectedResume.value?.currentVersion ?? null)

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try {
      const response = await listResumes()
      resumes.value = response.data
      const current = resumes.value.find((resume) => resume.id === selectedResumeId.value) ?? resumes.value[0] ?? null
      if (current) await selectResume(current.id)
      else {
        selectedResumeId.value = null
        selectedVersionId.value = null
        versions.value = []
      }
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '读取本地简历失败'
    } finally {
      loading.value = false
    }
  }

  async function selectResume(id: number) {
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
    } catch (error) {
      if (selectedResumeId.value === id) {
        versions.value = resume.currentVersion ? [resume.currentVersion] : []
        versionError.value = error instanceof Error ? error.message : '读取版本历史失败'
      }
    } finally {
      if (selectedResumeId.value === id) versionLoading.value = false
    }
  }

  function selectVersion(id: number) {
    if (versions.value.some((version) => version.id === id)) selectedVersionId.value = id
  }

  return { resumes, selectedResumeId, selectedResume, versions, selectedVersionId, selectedVersion, loading, versionLoading, errorMessage, versionError, load, selectResume, selectVersion }
}
