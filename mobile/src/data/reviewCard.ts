/**
 * 复盘分享长图：把一场心得渲染成一张 750 宽的卡片图（Canvas，纯本地零网络）。
 * 分享出去的是「可读的复盘卡片」，不是截图——版式、日期、标签都是排版出来的，
 * 底部带一行产品署名。发导师/朋友求指点，是产品天然的传播位。
 */

const W = 750
const PAD = 56
const BRAND = '#168B68'
const INK = '#2E2A24'
const MUTED = '#8A8071'
const PAPER = '#F7F1E1'
const FONT = '"PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif'

export interface ReviewCardData {
  company: string
  typeLabel: string
  dateLabel: string
  text: string
  tags: string[]
}

/** 按测量宽度折行；measure 注入以便单测（Canvas 在测试环境不可用）。 */
export function wrapByMeasure(text: string, measure: (s: string) => number, maxWidth: number): string[] {
  const lines: string[] = []
  for (const paragraph of text.split('\n')) {
    if (!paragraph.trim()) { lines.push(''); continue }
    let line = ''
    for (const ch of paragraph) {
      if (measure(line + ch) > maxWidth && line) {
        lines.push(line)
        line = ch
      } else {
        line += ch
      }
    }
    if (line) lines.push(line)
  }
  return lines
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

export async function renderReviewCard(data: ReviewCardData): Promise<Blob | null> {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) return null

  const bodyFont = `30px ${FONT}`
  const titleFont = `800 44px ${FONT}`
  const metaFont = `26px ${FONT}`
  ctx.font = bodyFont
  const measure = (s: string) => ctx.measureText(s).width
  const lines = wrapByMeasure(data.text.trim() || '（还没有正文）', measure, W - PAD * 2)
  const lineHeight = 48
  const tagH = data.tags.length ? 64 : 0
  const H = 232 + lines.length * lineHeight + tagH + 150

  canvas.width = W
  canvas.height = H

  // 纸面：米色底 + 顶部品牌条，一眼是「职达的复盘卡片」
  ctx.fillStyle = PAPER
  ctx.fillRect(0, 0, W, H)
  ctx.fillStyle = BRAND
  ctx.fillRect(0, 0, W, 10)

  // 抬头：公司 · 类型 + 日期
  ctx.textBaseline = 'top'
  ctx.fillStyle = BRAND
  ctx.font = titleFont
  ctx.fillText(`${data.company} · ${data.typeLabel}`, PAD, 58)
  ctx.fillStyle = MUTED
  ctx.font = metaFont
  ctx.fillText(data.dateLabel, PAD, 126)

  // 分隔线
  ctx.strokeStyle = 'rgba(96, 82, 46, .25)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(PAD, 188)
  ctx.lineTo(W - PAD, 188)
  ctx.stroke()

  // 正文
  ctx.fillStyle = INK
  ctx.font = bodyFont
  let y = 232
  for (const line of lines) {
    ctx.fillText(line, PAD, y)
    y += lineHeight
  }

  // 标签胶囊
  if (data.tags.length) {
    let tx = PAD
    const ty = y + 8
    ctx.font = `24px ${FONT}`
    for (const tag of data.tags.slice(0, 6)) {
      const tw = ctx.measureText(tag).width + 32
      ctx.fillStyle = 'rgba(22, 139, 104, .12)'
      roundRect(ctx, tx, ty, tw, 44, 22)
      ctx.fill()
      ctx.fillStyle = BRAND
      ctx.fillText(tag, tx + 16, ty + 9)
      tx += tw + 14
      if (tx > W - PAD - 60) break
    }
    y += tagH
  }

  // 落款
  ctx.fillStyle = MUTED
  ctx.font = metaFont
  ctx.fillText('职达 CareerOS · 本地求职陪伴', PAD, H - 78)

  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/png')
  })
}
