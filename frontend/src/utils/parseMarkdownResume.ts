import type { EducationItem, ProjectItem, ResumeContent, WorkExperienceItem } from '../types/resume'

export interface ParsedMarkdownResume {
  title: string
  content: ResumeContent
  warnings: string[]
}

/**
 * 确定性解析 Markdown 简历，不调用任何模型：
 * # 一级标题 → 简历标题；## 章节 → 简历章节；### 标题 → 条目标题；- 列表 → 条目/字段。
 * 结构无法识别时只产生 warning，不编造内容；未知章节落入 customSections 保留原文。
 */
export function parseMarkdownResume(markdown: string, fallbackTitle = '未命名简历'): ParsedMarkdownResume {
  const warnings: string[] = []
  const title = extractTitle(markdown) ?? fallbackTitle
  const content: ResumeContent = {}
  const customSections: Array<{ title: string; description: string }> = []

  const sections = splitSections(markdown)
  for (const section of sections) {
    const key = sectionKey(section.title)
    if (!key) {
      if (section.lines.length) customSections.push({ title: section.title || '未命名章节', description: section.lines.join('\n') })
      continue
    }
    if (key === 'basicInfo') {
      content.basicInfo = parseBasicInfo(section.lines)
    } else if (key === 'summary') {
      content.summary = section.lines.join('\n').trim()
    } else if (key === 'workExperience') {
      content.workExperience = normalizeItems<WorkExperienceItem>(section.lines, (raw) => raw)
    } else if (key === 'projects') {
      content.projects = normalizeItems<ProjectItem>(section.lines, (raw) => raw)
    } else if (key === 'education') {
      content.education = normalizeItems<EducationItem>(section.lines, (raw) => raw)
    } else if (key === 'skills') {
      parseSkills(section.lines, content)
    } else if (key === 'certifications') {
      content.certifications = section.lines
        .filter(isBullet)
        .map((line) => ({ name: bulletText(line) }))
        .filter((item) => item.name)
    } else if (key === 'languages') {
      content.languages = section.lines
        .filter(isBullet)
        .map((line) => {
          const [name, level] = splitPair(bulletText(line))
          return { name: name || bulletText(line), level: level || undefined }
        })
        .filter((item) => item.name)
    }
  }

  if (customSections.length) content.customSections = customSections
  const hasContent = Boolean(content.basicInfo || content.summary || content.workExperience?.length || content.projects?.length || content.skills?.length || content.education?.length || customSections.length)
  if (!hasContent) {
    warnings.push('未能识别出可用的简历结构，请确认文件使用了 Markdown 标题和列表格式。')
  }
  return { title, content, warnings }
}

function extractTitle(markdown: string): string | null {
  const match = markdown.match(/^#\s+(.+?)\s*$/m)
  return match ? match[1].trim() : null
}

interface Section {
  title: string
  lines: string[]
}

function splitSections(markdown: string): Section[] {
  const sections: Section[] = []
  let current: Section | null = null
  const body = markdown.replace(/^\uFEFF/, '')
  for (const rawLine of body.split(/\r?\n/)) {
    const line = rawLine.trimEnd()
    const heading = line.match(/^#{2,4}\s+(.+?)\s*$/)
    if (heading) {
      const level = line.indexOf(' ') // "## " → 2, "### " → 3
      if (level >= 3) {
        // ### 条目标题：并入当前章节作为子条目
        if (current) current.lines.push(`### ${heading[1].trim()}`)
        continue
      }
      if (current) sections.push(current)
      current = { title: heading[1].trim(), lines: [] }
      continue
    }
    if (line.startsWith('# ')) continue // 一级标题已作为简历标题
    if (!current) continue
    current.lines.push(line)
  }
  if (current) sections.push(current)
  return sections
}

function sectionKey(title: string): string {
  const t = title.trim().toLowerCase()
  if (/(基本信息|基础信息|个人信息|联系方式)/.test(t)) return 'basicInfo'
  if (/(个人简介|个人总结|自我评价|个人概述|职业概述|^简介$|^总结$|^概要$|summary)/.test(t)) return 'summary'
  if (/(工作经历|工作经验|实习经历|^工作$|work experience)/.test(t)) return 'workExperience'
  if (/(项目经历|项目经验|^项目$|projects?)/.test(t)) return 'projects'
  if (/(教育经历|教育背景|^教育$|education)/.test(t)) return 'education'
  if (/(专业技能|技能清单|技术栈|^技能$|^技术$|skills)/.test(t)) return 'skills'
  if (/(证书|荣誉|获奖|资质|certificates?)/.test(t)) return 'certifications'
  if (/(语言能力|^语言$|languages?)/.test(t)) return 'languages'
  return ''
}

function isBullet(line: string): boolean {
  return /^[-*•]\s+/.test(line.trim())
}

function bulletText(line: string): string {
  return line.trim().replace(/^[-*•]\s+/, '').trim()
}

function splitPair(text: string): [string | null, string | null] {
  const match = text.match(/^([^：:]+)[：:]\s*(.+)$/)
  if (!match) return [null, null]
  return [match[1].trim(), match[2].trim()]
}

function parseBasicInfo(lines: string[]): ResumeContent['basicInfo'] {
  const basic: ResumeContent['basicInfo'] = {}
  for (const line of lines) {
    if (!isBullet(line)) continue
    const [key, value] = splitPair(bulletText(line))
    if (!key || !value) continue
    if (/(姓名|名字|name)/.test(key)) basic.name = value
    else if (/(电话|手机|联系方式|phone)/.test(key)) basic.phone = value
    else if (/(邮箱|邮件|email)/.test(key)) basic.email = value
    else if (/(求职意向|应聘岗位|目标职位|target)/.test(key)) basic.targetRole = value
    else if (/(现居|所在地|城市|location)/.test(key)) basic.location = value
    else if (/(工作年限|工作经验|年限)/.test(key)) basic.yearsOfExperience = value
    else if (/(最高学历|学历|education)/.test(key)) basic.educationLevel = value
    else if (/(微信|wechat)/.test(key)) basic.wechat = value
    else if (/(个人网站|主页|website)/.test(key)) basic.website = value
  }
  return Object.keys(basic).length ? basic : undefined
}

/** 把条目解析为 { _heading, _description, _highlights, ...字段 } 的中间形态 */
function collectItems(lines: string[]): { items: Array<Record<string, unknown>>; bullets: string[] } {
  const items: Array<Record<string, unknown>> = []
  const bullets: string[] = []
  let current: Record<string, unknown> | null = null
  for (const line of lines) {
    const itemHeading = line.match(/^###\s+(.+)$/)
    if (itemHeading) {
      if (current) items.push(current)
      current = { _heading: itemHeading[1].trim() }
      continue
    }
    if (!current) {
      if (isBullet(line)) bullets.push(bulletText(line))
      continue
    }
    if (!isBullet(line)) {
      const text = line.trim()
      if (text && !current._description) current._description = text
      continue
    }
    const text = bulletText(line)
    const [key, value] = splitPair(text)
    const field = fieldName(key)
    if (field && value) {
      const existing = current[field]
      current[field] = existing ? `${existing}、${value}` : value
    } else {
      const highlights = (current._highlights ?? []) as string[]
      highlights.push(text)
      current._highlights = highlights
    }
  }
  if (current) items.push(current)
  return { items, bullets }
}

function fieldName(key: string | null): string | null {
  if (!key) return null
  if (/(公司|企业|organization)/.test(key)) return 'company'
  if (/(职位|岗位|职务|position)/.test(key)) return 'position'
  if (/(项目名称|项目名|^title$|^name$)/.test(key)) return 'title'
  if (/(学校|院校|机构|school)/.test(key)) return 'school'
  if (/(专业|方向|major)/.test(key)) return 'major'
  if (/(学历|degree)/.test(key)) return 'degree'
  if (/(时间|起止|期间|period)/.test(key)) return 'period'
  if (/(技术|栈|technolog)/.test(key)) return 'technologies'
  if (/(描述|职责|内容|description)/.test(key)) return 'description'
  return null
}

/** 归一化条目：_heading → title/name，_description → description，_highlights → highlights */
function normalizeItems<T>(lines: string[], shape: (raw: Record<string, unknown>) => T): T[] {
  const { items, bullets } = collectItems(lines)
  const normalized = items.map((raw) => {
    const { _heading, _description, _highlights, ...rest } = raw
    const result = { ...rest } as Record<string, unknown>
    if (_heading && !result.title && !result.name) result.title = _heading
    if (!result.description && _description) result.description = _description
    if (!result.highlights && (_highlights as string[])?.length) result.highlights = _highlights
    return shape(result)
  })
  if (normalized.length) return normalized
  // 纯列表章节：每行成为一个条目的描述
  return bullets.map((text) => shape({ description: text }))
}

function parseSkills(lines: string[], content: ResumeContent): void {
  const bullets: string[] = []
  const categories: Array<{ name: string; skills: string[] }> = []
  for (const line of lines) {
    if (!isBullet(line)) continue
    const text = bulletText(line)
    const [key, value] = splitPair(text)
    if (key && value) {
      const skills = value.split(/[,，、\/\s]+/).map((item) => item.trim()).filter(Boolean)
      if (skills.length) categories.push({ name: key, skills })
      continue
    }
    const plain = text.split(/[、，]/).map((item) => item.trim()).filter(Boolean)
    bullets.push(...(plain.length > 1 ? plain : [text]))
  }
  if (categories.length) content.skillCategories = categories
  if (bullets.length) content.skills = [...(content.skills ?? []), ...bullets]
}
