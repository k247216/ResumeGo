import { ref } from 'vue'
import { createEvidence, listEvidences } from '../api/evidence'
import type { CapabilityEvidence, CreateCapabilityEvidenceRequest } from '../types/evidence'

export function useEvidenceLibrary() {
  const evidences = ref<CapabilityEvidence[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')

  async function load() {
    loading.value = true
    errorMessage.value = ''
    try { evidences.value = (await listEvidences()).data }
    catch (error) { errorMessage.value = error instanceof Error ? error.message : '读取能力证据失败' }
    finally { loading.value = false }
  }

  async function create(payload: CreateCapabilityEvidenceRequest) {
    saving.value = true
    errorMessage.value = ''
    try {
      const response = await createEvidence(payload)
      evidences.value = [response.data, ...evidences.value.filter((item) => item.id !== response.data.id)]
      return response.data
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '保存能力证据失败'
      throw error
    } finally { saving.value = false }
  }

  return { evidences, loading, saving, errorMessage, load, create }
}
