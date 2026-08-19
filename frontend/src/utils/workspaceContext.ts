const selectedJobKey = 'resumego:selectedJobId'
const selectedResumeKey = 'resumego:selectedResumeId'
const pendingSelectedJobKey = 'resumego:pendingSelectedJobId'
const returnToEditorKey = 'resumego:returnToEditor'
const pendingWorkspaceActionKey = 'resumego:pendingWorkspaceAction'

export function getWorkspaceSelectedJobId(): number | null {
  const rawValue = window.sessionStorage.getItem(selectedJobKey)
  if (!rawValue) return null
  const jobId = Number(rawValue)
  return Number.isFinite(jobId) && jobId > 0 ? jobId : null
}

export function setWorkspaceSelectedJobId(jobId: number) {
  if (!Number.isFinite(jobId) || jobId <= 0) return
  window.sessionStorage.setItem(selectedJobKey, String(jobId))
}

export function getWorkspaceSelectedResumeId(): number | null {
  const rawValue = window.sessionStorage.getItem(selectedResumeKey)
  if (!rawValue) return null
  const resumeId = Number(rawValue)
  return Number.isFinite(resumeId) && resumeId > 0 ? resumeId : null
}

export function setWorkspaceSelectedResumeId(resumeId: number) {
  if (!Number.isFinite(resumeId) || resumeId <= 0) return
  window.sessionStorage.setItem(selectedResumeKey, String(resumeId))
}

export function markPendingWorkspaceSelectedJobId(jobId: number) {
  if (!Number.isFinite(jobId) || jobId <= 0) return
  window.sessionStorage.setItem(pendingSelectedJobKey, String(jobId))
}

export function consumePendingWorkspaceSelectedJobId(): number | null {
  const rawValue = window.sessionStorage.getItem(pendingSelectedJobKey)
  if (rawValue) {
    window.sessionStorage.removeItem(pendingSelectedJobKey)
  }
  const jobId = Number(rawValue)
  return Number.isFinite(jobId) && jobId > 0 ? jobId : null
}

export function clearWorkspaceSelectedJobId() {
  window.sessionStorage.removeItem(selectedJobKey)
}

export function markReturnToEditor() {
  window.sessionStorage.setItem(returnToEditorKey, '1')
}

export function cancelPendingWorkspaceAction() {
  window.sessionStorage.removeItem(pendingWorkspaceActionKey)
}

export function consumeReturnToEditor() {
  const shouldReturn = window.sessionStorage.getItem(returnToEditorKey) === '1'
  if (shouldReturn) {
    window.sessionStorage.removeItem(returnToEditorKey)
  }
  return shouldReturn
}
