import { describe, expect, it } from 'vitest'
import { parseMarkdownResume } from './parseMarkdownResume'

describe('parseMarkdownResume', () => {
  const sample = `# 张伟 · 后端开发

## 基本信息
- 姓名：张伟
- 电话：13800000000
- 邮箱：zhangwei@example.com
- 求职意向：Java 后端开发

## 个人简介
五年后端开发经验，专注于分布式系统与高并发服务。

## 工作经历
### 字节跳动 · 后端工程师
- 公司：字节跳动
- 职位：后端工程师
- 时间：2021.06 - 至今
- 负责订单服务重构，支撑千万级日活
- 主导消息队列选型与落地

## 项目经历
### 订单中心重构
- 技术：Java、Kafka、Redis
- 将单机吞吐提升 3 倍

## 教育经历
### 北京理工大学 · 计算机科学与技术
- 学校：北京理工大学
- 专业：计算机科学与技术
- 学历：本科
- 时间：2015 - 2019

## 技能
- Java、Spring、MySQL、Redis
- 编程语言：Java、Python

## 证书
- CET-6
`

  it('parses title, basic info, summary and section counts deterministically', () => {
    const result = parseMarkdownResume(sample)
    expect(result.title).toBe('张伟 · 后端开发')
    expect(result.warnings).toEqual([])
    expect(result.content.basicInfo?.name).toBe('张伟')
    expect(result.content.basicInfo?.phone).toBe('13800000000')
    expect(result.content.basicInfo?.email).toBe('zhangwei@example.com')
    expect(result.content.basicInfo?.targetRole).toBe('Java 后端开发')
    expect(result.content.summary).toContain('分布式系统与高并发服务')
    expect(result.content.workExperience).toHaveLength(1)
    expect(result.content.workExperience?.[0].company).toBe('字节跳动')
    expect(result.content.workExperience?.[0].position).toBe('后端工程师')
    expect(result.content.workExperience?.[0].period).toBe('2021.06 - 至今')
    expect(result.content.workExperience?.[0].highlights).toContain('主导消息队列选型与落地')
    expect(result.content.projects?.[0].title).toBe('订单中心重构')
    expect(result.content.projects?.[0].technologies).toContain('Kafka')
    expect(result.content.education?.[0].school).toBe('北京理工大学')
    expect(result.content.education?.[0].major).toBe('计算机科学与技术')
    expect(result.content.skills).toContain('Java')
    expect(result.content.skillCategories?.[0].name).toBe('编程语言')
    expect(result.content.skillCategories?.[0].skills).toContain('Python')
    expect(result.content.certifications?.[0].name).toBe('CET-6')
  })

  it('keeps unknown sections as customSections and falls back to the default title', () => {
    const result = parseMarkdownResume(`## 兴趣爱好\n- 跑步\n- 阅读\n`)
    expect(result.title).toBe('未命名简历')
    expect(result.content.customSections?.[0].title).toBe('兴趣爱好')
    expect(result.content.customSections?.[0].description).toContain('跑步')
  })

  it('flags unparseable input without fabricating content', () => {
    const result = parseMarkdownResume('只是一段普通文字，没有任何标题结构')
    expect(result.warnings.length).toBeGreaterThan(0)
    expect(result.content.basicInfo).toBeUndefined()
    expect(result.content.workExperience).toBeUndefined()
  })

  it('merges plain bullets into item highlights when no heading exists', () => {
    const result = parseMarkdownResume(`# 简历\n\n## 项目经历\n- 校园论坛管理系统\n- 负责后端 API 开发\n`)
    expect(result.content.projects).toHaveLength(2)
    expect(result.content.projects?.[0].description).toBe('校园论坛管理系统')
  })
})
