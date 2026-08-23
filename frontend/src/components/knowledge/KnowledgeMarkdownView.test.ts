// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeMarkdownView from './KnowledgeMarkdownView.vue'

const NL = '\n'

function mountMd(source: string) {
  return mount(KnowledgeMarkdownView, { props: { source } })
}

describe('KnowledgeMarkdownView', () => {
  it('renders headings, lists and paragraphs without raw syntax', () => {
    const wrapper = mountMd('# 技术知识' + NL + NL + '### 系统设计' + NL + '- 项目经历' + NL + '- TensorFlow 部署' + NL + NL + '一段正文。')
    expect(wrapper.find('h1').text()).toBe('技术知识')
    expect(wrapper.findAll('h3')).toHaveLength(1)
    expect(wrapper.findAll('.md-list li').map((li) => li.text())).toEqual(['项目经历', 'TensorFlow 部署'])
    expect(wrapper.find('.md-para').text()).toBe('一段正文。')
    expect(wrapper.text()).not.toContain('# 技术知识')
  })

  it('shows a language label and syntax-colored tokens in fenced code', () => {
    const fence = String.fromCharCode(96).repeat(3)
    const wrapper = mountMd(fence + 'ts' + NL + 'const x = 1 // 注释' + NL + fence)
    expect(wrapper.find('.md-code-head').text()).toBe('TypeScript')
    expect(wrapper.find('.tok-keyword').text()).toBe('const')
    expect(wrapper.find('.tok-number').text()).toBe('1')
    expect(wrapper.find('.tok-comment').text()).toBe('// 注释')
  })

  it('renders code blocks verbatim', () => {
    const fence = String.fromCharCode(96).repeat(3)
    const wrapper = mountMd('## 示例' + NL + NL + fence + NL + 'text' + NL + fence)
    expect(wrapper.find('.md-code code').text()).toBe('text')
  })

  it('renders inline bold, italic and code with real formatting', () => {
    const wrapper = mountMd('重点 **加粗** 与 *斜体* 以及 `code`。')
    expect(wrapper.find('.md-para strong').text()).toBe('加粗')
    expect(wrapper.find('.md-para em').text()).toBe('斜体')
    expect(wrapper.find('.md-inline-code').text()).toBe('code')
    expect(wrapper.text()).not.toContain('**加粗**')
  })

  it('renders tables, blockquote, hr and ordered lists', () => {
    const src = '| 标题 | 值 |' + NL + '| --- | --- |' + NL + '| A | 1 |' + NL + NL + '> 引用内容' + NL + NL + '---' + NL + NL + '1. 第一项' + NL + '2. 第二项'
    const wrapper = mountMd(src)
    expect(wrapper.findAll('.md-table th').map((t) => t.text())).toEqual(['标题', '值'])
    expect(wrapper.findAll('.md-table td').map((t) => t.text())).toEqual(['A', '1'])
    expect(wrapper.find('.md-quote').text()).toBe('引用内容')
    expect(wrapper.find('.md-hr').exists()).toBe(true)
    expect(wrapper.findAll('.md-ol li').map((li) => li.text())).toEqual(['第一项', '第二项'])
  })

  it('treats raw HTML as plain text (no XSS)', () => {
    const wrapper = mountMd('<img src=x onerror=alert(1)>')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.md-para').text()).toContain('<img')
  })

  it('does not turn syntax inside code blocks into inline formatting', () => {
    const fence = String.fromCharCode(96).repeat(3)
    const wrapper = mountMd(fence + NL + '**not bold**`code`' + NL + fence)
    expect(wrapper.find('strong').exists()).toBe(false)
    expect(wrapper.find('.md-code').text()).toBe('**not bold**`code`')
  })
})