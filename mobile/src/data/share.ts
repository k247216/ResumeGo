import { Capacitor } from '@capacitor/core'

export interface SharePayload {
  /** 收件人在微信/QQ 里看到的文件名，会原样落盘，所以只做非法字符清理不做 ASCII 化。 */
  fileName: string
  blob: Blob
  subject?: string
  dialogTitle?: string
}

const EXT_BY_MIME: Record<string, string> = {
  'application/pdf': 'pdf',
  'application/json': 'json',
  'text/plain': 'txt',
  'text/markdown': 'md',
  'image/png': 'png',
  'image/jpeg': 'jpg',
  'image/gif': 'gif',
  'image/webp': 'webp',
}

// 分享面板能否出现，取决于文件名最后那段扩展名能否被 Android 的 MimeTypeMap 认出来：
// 认不出来插件会退化成通配类型，微信/QQ 因为没声明接收通配类型而整块从候选里消失。
// 所以这里保证「一定有扩展名」，扩展名缺失时按 mime 补一个。
export function shareFileName(fileName: string, mime: string): string {
  const cleaned = (fileName || 'file').replace(/[\\/:*?"<>|]+/g, '_').replace(/^\.+/, '').trim() || 'file'
  if (/\.[A-Za-z0-9]{1,6}$/.test(cleaned)) return cleaned
  const ext = EXT_BY_MIME[mime.split(';')[0].trim().toLowerCase()]
  return ext ? `${cleaned}.${ext}` : cleaned
}

function toBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
}

function downloadAsFile(p: SharePayload): void {
  const url = URL.createObjectURL(p.blob)
  const a = document.createElement('a')
  a.href = url
  a.download = p.fileName
  a.click()
  URL.revokeObjectURL(url)
}

/** 用户在系统面板里主动返回不是故障，不能报成「分享失败」。 */
export function isShareCancelled(err: unknown): boolean {
  const message = err instanceof Error ? err.message : typeof err === 'string' ? err : ''
  return /cancel/i.test(message)
}

/** 只认这几家高频收件端，其余一律回落到通用说法，避免把包名直接甩给用户。 */
const SHARE_TARGET_NAMES: Record<string, string> = {
  'com.tencent.mm': '微信',
  'com.tencent.mobileqq': 'QQ',
  'com.tencent.tim': 'TIM',
  'com.alibaba.android.rimet': '钉钉',
  'com.lark.messenger': '飞书',
  'com.android.email': '邮件',
  'com.google.android.gm': '邮件',
}

export function shareTargetName(activityType: string | null | undefined): string | null {
  if (!activityType) return null
  return SHARE_TARGET_NAMES[activityType] ?? null
}

export function shareErrorMessage(err: unknown): string {
  const message = err instanceof Error ? err.message : typeof err === 'string' ? err : ''
  if (/FILE_NOTCREATED|ENOENT|space|磁盘|存储/i.test(message)) return '本机空间不足或写入失败，未能生成分享文件'
  return message ? `分享未完成：${message}` : '分享未完成'
}

/**
 * 返回用户实际选中的收件端包名；拿不到（Web 降级或系统未回报）时为 null，调用方据此决定要不要说「已发送」。
 *
 * 落盘目录必须是 CACHE：Filesystem 插件把 DOCUMENTS/EXTERNAL_STORAGE 视为「公共目录」，
 * 写入前会先申请 READ/WRITE_EXTERNAL_STORAGE——Android 13 起该权限已不存在，申请必被拒，
 * 于是 writeFile 直接 reject，分享面板根本没机会弹出。CACHE 走 getCacheDir()，
 * 免权限且已被 android/app/src/main/res/xml/file_paths.xml 的 cache-path 覆盖，FileProvider 能签发出 URI。
 */
export async function shareFile(p: SharePayload): Promise<string | null> {
  if (!Capacitor.isNativePlatform()) { downloadAsFile(p); return null }
  const { Filesystem, Directory } = await import('@capacitor/filesystem')
  const { Share } = await import('@capacitor/share')
  const fileName = shareFileName(p.fileName, p.blob.type)
  const written = await Filesystem.writeFile({
    path: fileName,
    data: await toBase64(p.blob),
    directory: Directory.Cache,
  })
  try {
    const res = await Share.share({ title: p.subject ?? fileName, url: written.uri, dialogTitle: p.dialogTitle ?? '分享' })
    return res.activityType || null
  } catch (err) {
    if (isShareCancelled(err)) return null
    throw err
  }
}
