import { Capacitor } from '@capacitor/core'
import { objectUrl, getFile } from './fileStore'
import type { ResumeFileVer } from './store'

export interface ResumePreview {
  kind: 'pdf' | 'text' | 'none'
  url?: string
  text?: string
}

/** 解析版本文件用于预览：PDF 走对象 URL，MD/TXT 走文本，交给不同渲染。 */
export async function previewResume(ver: ResumeFileVer): Promise<ResumePreview> {
  const isText = ver.mime.startsWith('text/') || /\.(md|markdown|txt)$/i.test(ver.fileName)
  if (isText) {
    const blob = await getFile(ver.fileKey)
    return { kind: blob ? 'text' : 'none', text: blob ? await blob.text() : undefined }
  }
  const url = await objectUrl(ver.fileKey)
  return url ? { kind: 'pdf', url } : { kind: 'none' }
}

function toBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
}

/** 把简历文件发到系统分享面板（转发给 HR）；Web 端降级为下载。 */
export async function shareResumeFile(ver: ResumeFileVer): Promise<void> {
  const blob = await getFile(ver.fileKey)
  if (!blob) throw new Error('文件不存在')
  if (Capacitor.isNativePlatform()) {
    const { Filesystem, Directory } = await import('@capacitor/filesystem')
    const { Share } = await import('@capacitor/share')
    const written = await Filesystem.writeFile({ path: ver.fileName, data: await toBase64(blob), directory: Directory.Documents })
    await Share.share({ title: ver.fileName, url: written.uri, dialogTitle: '发送简历' })
  } else {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = ver.fileName
    a.click()
    URL.revokeObjectURL(url)
  }
}

export function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
