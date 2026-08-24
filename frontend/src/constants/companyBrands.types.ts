export interface CompanyMark {
  letter: string
  color: string
  lightText: boolean
  /** 打包的真实 SVG 图标地址；缺失时用字母标识 */
  icon?: string
  /** 图标为单色 SVG 时着色（未提供 icon 即字母模式时不用） */
  iconColor?: string
}
