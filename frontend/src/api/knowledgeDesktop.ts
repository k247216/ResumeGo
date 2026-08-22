import type { KnowledgeDesktopResult } from './http'

/**
 * 受管原文薄适配器：只把正整数 documentId 交给 preload；
 * 非 Electron 浏览器开发模式返回 DESKTOP_REQUIRED，不伪装已打开。
 */
export async function openKnowledgeSource(documentId: number): Promise<KnowledgeDesktopResult> {
  const desktop = typeof window !== 'undefined' ? window.resumeGoDesktop : undefined
  if (!desktop?.openKnowledgeSource) {
    return { ok: false, code: 'DESKTOP_REQUIRED', message: '该功能仅桌面端可用' }
  }
  return desktop.openKnowledgeSource(documentId)
}

export async function revealKnowledgeSource(documentId: number): Promise<KnowledgeDesktopResult> {
  const desktop = typeof window !== 'undefined' ? window.resumeGoDesktop : undefined
  if (!desktop?.revealKnowledgeSource) {
    return { ok: false, code: 'DESKTOP_REQUIRED', message: '该功能仅桌面端可用' }
  }
  return desktop.revealKnowledgeSource(documentId)
}
