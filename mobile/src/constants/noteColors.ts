/**
 * 心得便签的可选底色。
 * 这些颜色只以「混色」方式参与卡片背景（color-mix 到 surface 上），
 * 所以浅色与深色主题都不需要单独配色；同时它们是允许写入库的合法值——
 * normalizeDb 之外的任何颜色一律视为脏数据丢回 null。
 * 只管心得墙卡片的底色（分类识别用），编辑器里写什么纸由 NOTE_PAPERS 管。
 */
export const NOTE_COLORS = ['#e8b64c', '#e08a7a', '#5fb894', '#6f9fdd', '#a184c9', '#d97fa6'] as const

/** 校验/归一便签底色：只接受调色板内的值，其余一律 null（跟随日程类型色）。 */
export function normalizeNoteColor(value: unknown): string | null {
  return typeof value === 'string' && (NOTE_COLORS as readonly string[]).includes(value) ? value : null
}

/** 编辑器的纸张风格：写手感的本子样式，与便签色互不相干。 */
export interface NotePaper { key: string; label: string }
export const NOTE_PAPERS = [
  { key: 'plain', label: '白纸' },
  { key: 'cream', label: '米色' },
  { key: 'kraft', label: '牛皮' },
] as const
const PAPER_KEYS: string[] = NOTE_PAPERS.map((p) => p.key)

/** 校验/归一纸张风格：只接受白名单内的 key，默认白纸。 */
export function normalizeNotePaper(value: unknown): string {
  return typeof value === 'string' && PAPER_KEYS.includes(value) ? value : 'plain'
}
