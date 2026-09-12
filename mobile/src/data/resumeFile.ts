import { objectUrl, getFile } from './fileStore'
import { shareFile } from './share'
import type { ResumeFileVer } from './store'

export interface ResumePreview {
  kind: 'pdf' | 'image' | 'text' | 'unsupported' | 'none'
  url?: string
  text?: string
}

const PDF_EXT = /\.pdf$/i
const TEXT_EXT = /\.(md|markdown|txt)$/i
const IMAGE_EXT = /\.(png|jpe?g|gif|webp|bmp|svg)$/i

/** 预览能真正画出来的三类格式；其余在导入前就拒掉，别存一份只会空白打开的文件。 */
export const RESUME_FILE_ACCEPT =
  '.pdf,.md,.markdown,.txt,.png,.jpg,.jpeg,.gif,.webp,.bmp,.svg,application/pdf,text/markdown,text/plain,image/*'
export const RESUME_UNSUPPORTED_HINT = '只支持 PDF、Markdown、TXT 或图片简历'

export function isSupportedResume(file: { name: string; type: string }): boolean {
  return PDF_EXT.test(file.name) || TEXT_EXT.test(file.name) || IMAGE_EXT.test(file.name)
    || file.type === 'application/pdf' || file.type.startsWith('text/') || file.type.startsWith('image/')
}

/** 解析版本文件用于预览：PDF 走对象 URL，MD/TXT 走文本，交给不同渲染。 */
export async function previewResume(ver: ResumeFileVer): Promise<ResumePreview> {
  const isText = ver.mime.startsWith('text/') || TEXT_EXT.test(ver.fileName)
  if (isText) {
    const blob = await getFile(ver.fileKey)
    return { kind: blob ? 'text' : 'none', text: blob ? await blob.text() : undefined }
  }
  const isImage = ver.mime.startsWith('image/') || IMAGE_EXT.test(ver.fileName)
  const isPdf = ver.mime === 'application/pdf' || PDF_EXT.test(ver.fileName)
  // 浏览器给出的 mime 可能为空或 application/octet-stream，扩展名是最后的依据；两者都对不上就不是可预览格式。
  if (!isImage && !isPdf) return { kind: 'unsupported' }
  const url = await objectUrl(ver.fileKey)
  if (!url) return { kind: 'none' }
  return { kind: isImage ? 'image' : 'pdf', url }
}

/** 把简历文件发到系统分享面板（微信 / QQ / 邮件…由用户当场选）；Web 端降级为下载。 */
export async function shareResumeFile(ver: ResumeFileVer): Promise<string | null> {
  const blob = await getFile(ver.fileKey)
  if (!blob) throw new Error('本机未找到该版本的文件')
  // IndexedDB 读回来的 Blob 常是 octet-stream，会以泛型身份进分享面板，按入库时记的 mime 纠正。
  const typed = ver.mime && blob.type !== ver.mime ? blob.slice(0, blob.size, ver.mime) : blob
  const target = await shareFile({
    fileName: ver.fileName,
    blob: typed,
    subject: ver.fileName,
    dialogTitle: '分享简历',
  })
  return target
}

export function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/**
 * 系统导出的简历名常常很长，而文件名的两头才是有效信息：前缀认人、后缀认格式。
 * 尾部省略会把 `.pdf` 省掉，所以从中间截；按码点切，中文文件名不会被劈成半个字。
 */
export function middleEllipsis(name: string, max = 28): string {
  const chars = [...name.trim()]
  if (chars.length <= max) return chars.join('')
  const extMatch = /\.([\p{L}\p{N}]{1,5})$/u.exec(name)
  const ext = extMatch ? [...extMatch[0]] : []
  const stem = chars.slice(0, chars.length - ext.length)
  const keep = Math.max(4, max - 1 - ext.length)
  const head = Math.ceil(keep / 2)
  const tail = Math.floor(keep / 2)
  return `${stem.slice(0, head).join('')}…${tail ? stem.slice(stem.length - tail).join('') : ''}${ext.join('')}`
}

/**
 * 只留开头。middleEllipsis 是为「保住扩展名」而生的，但简历标题、选择器标签这类文本没有扩展名，
 * 身份全在前缀里，从中间裁只会剩下「…0260910」这种没意义的日期尾巴。
 */
export function headEllipsis(name: string, max = 14): string {
  const chars = [...name.trim()]
  if (chars.length <= max) return chars.join('')
  return `${chars.slice(0, Math.max(1, max - 1)).join('')}…`
}
