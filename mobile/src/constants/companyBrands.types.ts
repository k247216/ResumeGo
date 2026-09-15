export interface CompanyMark {
  letter: string
  color: string
  lightText?: boolean
  /** 单色字形（已染成 currentColor 的 SVG 字符串），衬在 color 底上。 */
  glyph?: string
  /** 字形前景色覆盖。整体填充型 App 图标（如美团）需要强制白色，不走明暗规则。 */
  fg?: string
  /** 遗留字标 PNG 的 URL（真实 logo，但小尺寸下是整条字标）。 */
  icon?: string
}
