/**
 * 复盘分享长图：把一场心得渲染成一张 750 宽的卡片图（Canvas，纯本地零网络）。
 * 版式复刻应用里的「全屏心得笔记本」：暖色桌面上贴一张带胶带的白便签纸，
 * 正文落在横线纸上，标签是品牌绿胶囊，底部落款——分享出去一眼就是职达，不是一张陌生截图。
 */

const W = 750
const SHEET_X = 48
const SHEET_W = W - 96
const SHEET_TOP = 100
const PAD = 44
const LINE_H = 48
const BRAND = '#168B68'
const BRAND_SOFT = 'rgba(22, 139, 104, .10)'
const INK = '#2E2A24'
const MUTED = '#8A8071'
const DESK = '#F4F1EA'
const PAPER_LINE = 'rgba(96, 82, 46, .13)'
const DIVIDER = 'rgba(96, 82, 46, .28)'
const TAPE = 'rgba(222, 205, 158, .6)'
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
  const tagFont = `600 24px ${FONT}`

  // 先量后画：正文折行、标题与标签宽度都在定画布高度之前量完
  ctx.font = bodyFont
  const measure = (s: string) => ctx.measureText(s).width
  const lines = wrapByMeasure(data.text.trim() || '（还没有正文）', measure, SHEET_W - PAD * 2)

  ctx.font = titleFont
  const maxTitle = SHEET_W - PAD * 2 - 88 - 26
  let company = data.company || '未命名'
  while (ctx.measureText(company).width > maxTitle && company.length > 1) company = company.slice(0, -1)
  if (company !== (data.company || '未命名')) company += '…'

  ctx.font = tagFont
  const tags = data.tags.slice(0, 6)
  const tagWidths = tags.map((t) => ctx.measureText(t).width + 36)

  // 布局：纸内 = 抬头(头像行) → 虚线 → 横线正文 → 标签 → 纸底；纸外 = 落款
  const dividerY = SHEET_TOP + PAD + 88 + 34
  const bodyTop = dividerY + 34
  let y = bodyTop + lines.length * LINE_H
  if (tags.length) y += 24 + 46
  const sheetH = y + PAD - SHEET_TOP
  const H = SHEET_TOP + sheetH + 110

  canvas.width = W
  canvas.height = H

  // 桌面：比纸暖一度的底，衬出中间那张便签
  ctx.fillStyle = DESK
  ctx.fillRect(0, 0, W, H)

  // 便签纸：白纸 + 圆角 + 柔和投影
  ctx.save()
  ctx.shadowColor = 'rgba(16, 24, 40, .18)'
  ctx.shadowBlur = 40
  ctx.shadowOffsetY = 16
  ctx.fillStyle = '#FFFFFF'
  roundRect(ctx, SHEET_X, SHEET_TOP, SHEET_W, sheetH, 24)
  ctx.fill()
  ctx.restore()

  // 胶带压在纸顶中央，微微歪着——应用里便签的识别符号
  ctx.save()
  ctx.translate(W / 2, SHEET_TOP)
  ctx.rotate(-2.4 * Math.PI / 180)
  ctx.fillStyle = TAPE
  ctx.fillRect(-85, -22, 170, 44)
  ctx.restore()

  // 抬头：品牌绿头像圈（取公司首字，对应应用里的 CompanyMark）+ 公司名 + 类型·日期
  const avX = SHEET_X + PAD
  const avY = SHEET_TOP + PAD
  ctx.beginPath()
  ctx.arc(avX + 44, avY + 44, 44, 0, Math.PI * 2)
  ctx.fillStyle = BRAND_SOFT
  ctx.fill()
  ctx.lineWidth = 2
  ctx.strokeStyle = 'rgba(22, 139, 104, .25)'
  ctx.stroke()
  ctx.fillStyle = BRAND
  ctx.font = `800 38px ${FONT}`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText((data.company || '未').slice(0, 1), avX + 44, avY + 47)
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'

  const textX = avX + 88 + 26
  ctx.fillStyle = INK
  ctx.font = titleFont
  ctx.fillText(company, textX, avY + 38)
  ctx.fillStyle = MUTED
  ctx.font = metaFont
  ctx.fillText(`${data.typeLabel} · ${data.dateLabel}`, textX, avY + 78)

  // 虚线分隔
  ctx.setLineDash([10, 8])
  ctx.strokeStyle = DIVIDER
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(SHEET_X + PAD, dividerY)
  ctx.lineTo(SHEET_X + SHEET_W - PAD, dividerY)
  ctx.stroke()
  ctx.setLineDash([])

  // 正文：横线纸，文字落在行上
  ctx.font = bodyFont
  for (let i = 0; i < lines.length; i++) {
    const ly = bodyTop + i * LINE_H
    ctx.strokeStyle = PAPER_LINE
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(SHEET_X + PAD, ly - 9)
    ctx.lineTo(SHEET_X + SHEET_W - PAD, ly - 9)
    ctx.stroke()
    if (lines[i]) {
      ctx.fillStyle = INK
      ctx.fillText(lines[i], SHEET_X + PAD, ly)
    }
  }

  // 标签胶囊：品牌绿 soft 底 + 绿字，与应用里的 tag-chip 同款
  if (tags.length) {
    ctx.font = tagFont
    ctx.textBaseline = 'middle'
    let tx = SHEET_X + PAD
    const ty = y - 46
    for (let i = 0; i < tags.length; i++) {
      const tw = tagWidths[i]
      if (tx + tw > SHEET_X + SHEET_W - PAD) break
      ctx.fillStyle = BRAND_SOFT
      roundRect(ctx, tx, ty, tw, 46, 23)
      ctx.fill()
      ctx.fillStyle = BRAND
      ctx.fillText(tags[i], tx + 18, ty + 24)
      tx += tw + 14
    }
    ctx.textBaseline = 'alphabetic'
  }

  // 落款：桌面上居中，「职达 CareerOS」品牌绿 + 灰色副标
  const footY = SHEET_TOP + sheetH + 52
  ctx.font = metaFont
  const w1 = ctx.measureText('职达 CareerOS').width
  const w2 = ctx.measureText(' · 本地求职陪伴').width
  let fx = (W - w1 - w2) / 2
  ctx.fillStyle = BRAND
  ctx.fillText('职达 CareerOS', fx, footY)
  fx += w1
  ctx.fillStyle = MUTED
  ctx.fillText(' · 本地求职陪伴', fx, footY)

  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), 'image/png')
  })
}
