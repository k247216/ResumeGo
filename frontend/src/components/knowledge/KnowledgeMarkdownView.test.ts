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
    expect(wrapper.find('h2').text()).toBe('技术知识')
    expect(wrapper.findAll('h3')).toHaveLength(1)
    expect(wrapper.findAll('.md-list li').map((li) => li.text())).toEqual(['项目经历', 'TensorFlow 部署'])
    expect(wrapper.find('.md-para').text()).toBe('一段正文。')
    expect(wrapper.text()).not.toContain('# 技术知识')
  })

  it('renders code blocks verbatim', () => {
    const fence = String.fromCharCode(96).repeat(3)
    const wrapper = mountMd('## 示例' + NL + NL + fence + NL + 'text' + NL + fence)
    expect(wrapper.find('.md-code code').text()).toBe('text')
  })

  it('treats raw HTML as plain text (no XSS)', () => {
    const wrapper = mountMd('<img src=x onerror=alert(1)>')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.md-para').text()).toContain('<img')
  })
})