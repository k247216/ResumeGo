/**
 * 心得正文的 HTML 处理。
 * 心得从「纯文本 textarea」升级为可加粗/插图/插链接的富文本后，存储格式变成 HTML。
 * HTML 一旦进库就可能从备份导入进来（不可信来源），所以渲染前的净化是强制的：
 * 白名单标签 + 白名单属性，on* / style / class / script 一律不留。
 */

const ALLOWED_TAGS = new Set(['P', 'BR', 'B', 'STRONG', 'I', 'EM', 'U', 'S', 'A', 'IMG', 'UL', 'OL', 'LI', 'DIV', 'SPAN'])
/** 彻底丢弃而不是展开子节点：它们的内容本身就是攻击载荷。 */
const DROP_TAGS = new Set(['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'LINK', 'META'])

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function safeHref(url: string): string | null {
  return /^(https?:\/\/|mailto:)/i.test(url.trim()) ? url.trim() : null
}
function safeImgSrc(url: string): string | null {
  const v = url.trim()
  return /^(https?:\/\/|data:image\/)/i.test(v) ? v : null
}

function sanitizeNode(node: Node, out: Node): void {
  for (const child of [...node.childNodes]) {
    if (child.nodeType === Node.TEXT_NODE) {
      out.appendChild(document.createTextNode(child.nodeValue ?? ''))
      continue
    }
    if (child.nodeType !== Node.ELEMENT_NODE) continue
    const el = child as Element
    const tag = el.tagName
    if (DROP_TAGS.has(tag)) continue
    if (!ALLOWED_TAGS.has(tag)) {
      // 不认识的标签：剥掉壳，保留里面的文字（如 <font>、旧编辑器的包装层）
      sanitizeNode(el, out)
      continue
    }
    const next = document.createElement(tag === 'STRONG' ? 'strong' : tag.toLowerCase())
    if (tag === 'A') {
      const href = safeHref(el.getAttribute('href') ?? '')
      if (href) next.setAttribute('href', href)
      else { sanitizeNode(el, out); continue }
    } else if (tag === 'IMG') {
      const src = safeImgSrc(el.getAttribute('src') ?? '')
      if (!src) continue
      next.setAttribute('src', src)
      next.setAttribute('alt', '')
    }
    sanitizeNode(el, next)
    out.appendChild(next)
  }
}

/** 白名单净化：返回可直接 innerHTML 的安全片段。 */
export function sanitizeReviewHtml(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const out = document.createElement('div')
  sanitizeNode(doc.body, out)
  return out.innerHTML
}

/** 旧心得是纯文本（含换行），新格式是 HTML：进来时统一成安全 HTML。
 *  判定从严——只有以块级标签开头或含 <br 的才当 HTML（编辑器产出的必然长这样），
 *  否则用户旧文里的「<img>」这类字样会被误当标签剥掉。 */
export function ensureReviewHtml(raw: string | null | undefined): string {
  const text = (raw ?? '').trim()
  if (!text) return ''
  const looksLikeHtml = /^<(p|div|ul|ol)\b/i.test(text) || /<br\b/i.test(text)
  if (looksLikeHtml) return sanitizeReviewHtml(text)
  return `<p>${escapeHtml(text).replace(/\n/g, '<br>')}</p>`
}

/** 心得的纯文本视图：字数统计、心得墙摘要都用它，不含任何标签。 */
export function reviewPlainText(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return (doc.body.textContent ?? '').replace(/\u00a0/g, ' ')
}

/**
 * 图片压成内联 dataURL：心得是纯本机存储，插入的图必须跟着正文走。
 * 长边压到 1280px、JPEG 0.72，一般能从几 MB 压到一两百 KB，localStorage 才装得下几张。
 */
export async function compressImageToDataUrl(file: File, maxDim = 1280, quality = 0.72): Promise<string> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
  const scale = Math.min(1, maxDim / Math.max(bitmap.width, bitmap.height))
  const w = Math.max(1, Math.round(bitmap.width * scale))
  const h = Math.max(1, Math.round(bitmap.height * scale))
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) { bitmap.close(); throw new Error('canvas unavailable') }
  ctx.drawImage(bitmap, 0, 0, w, h)
  bitmap.close()
  return canvas.toDataURL('image/jpeg', quality)
}

/**
 * 同上，但产出 Blob：截图存 IndexedDB（fileStore）时用 Blob，不必背着 base64 的 33% 膨胀。
 */
export async function compressImageToBlob(file: File, maxDim = 1280, quality = 0.72): Promise<Blob> {
  const dataUrl = await compressImageToDataUrl(file, maxDim, quality)
  const res = await fetch(dataUrl)
  return await res.blob()
}
