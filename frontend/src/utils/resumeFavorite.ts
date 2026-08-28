const FAVORITE_KEY_PREFIX = 'resumego:resume-favorite:'
export const RESUME_FAVORITE_CHANGED = 'resumego:resume-favorite-changed'

export function resumeFavoriteStorageKey(resumeId: number): string {
  return `${FAVORITE_KEY_PREFIX}${resumeId}`
}

export function isResumeFavorite(resumeId: number): boolean {
  try {
    return localStorage.getItem(resumeFavoriteStorageKey(resumeId)) === 'true'
  } catch {
    return false
  }
}

export function setResumeFavorite(resumeId: number, favorite: boolean): void {
  try {
    localStorage.setItem(resumeFavoriteStorageKey(resumeId), String(favorite))
    window.dispatchEvent(new CustomEvent(RESUME_FAVORITE_CHANGED, {
      detail: { resumeId, favorite },
    }))
  } catch {
    // 本地存储不可用时，调用方仍保留当前内存状态。
  }
}
