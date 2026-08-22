// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeSearchResults from './KnowledgeSearchResults.vue'
import type { KnowledgeSearchItem } from '../../types/knowledge'

function item(id: number, matchedField: 'TITLE' | 'CONTENT', snippet: string, lineNumber: number | null): KnowledgeSearchItem {
  return {
    document: { id, title: '文档 ' + id, sourceType: 'FILE', processingStatus: 'COMPLETED', sourceFile: null, createdAt: 't', updatedAt: 't' },
    matchedField,
    snippet,
    lineNumber,
  }
}

function mountResults(props: Record<string, unknown> = {}) {
  return mount(KnowledgeSearchResults, {
    props: {
      results: [],
      selectedId: null,
      loading: false,
      errorMessage: '',
      ...props,
    },
  })
}

describe('KnowledgeSearchResults', () => {
  it('shows the real snippet and line number for content hits', () => {
    const wrapper = mountResults({
      results: [item(7, 'CONTENT', '…项目经历 TensorFlow 部署…', 3)],
    })
    expect(wrapper.text()).toContain('文档 7')
    expect(wrapper.text()).toContain('正文命中')
    expect(wrapper.text()).toContain('第 3 行')
    expect(wrapper.text()).toContain('…项目经历 TensorFlow 部署…')
  })

  it('labels title hits and omits the line number', () => {
    const wrapper = mountResults({ results: [item(8, 'TITLE', 'TensorFlow 学习笔记', null)] })
    expect(wrapper.text()).toContain('标题命中')
    expect(wrapper.text()).not.toContain('第')
  })

  it('selects a document on click', async () => {
    const wrapper = mountResults({ results: [item(7, 'TITLE', 'x', null)] })
    await wrapper.get('[data-test="knowledge-search-item-7"]').trigger('click')
    expect(wrapper.emitted('select')).toEqual([[7]])
  })

  it('shows loading, error, and empty states locally', async () => {
    expect(mountResults({ loading: true }).text()).toContain('正在搜索')

    const error = mountResults({ errorMessage: '网络错误' })
    expect(error.text()).toContain('搜索失败')
    await error.get('[data-test="knowledge-search-retry"]').trigger('click')
    expect(error.emitted('retry')).toHaveLength(1)

    expect(mountResults({}).get('[data-test="knowledge-search-empty"]').text()).toContain('没有匹配结果')
  })

  it('shows a stale banner above previous results when the latest search fails', () => {
    const wrapper = mountResults({
      results: [item(7, 'TITLE', '上一次结果', null)],
      errorMessage: '搜索失败',
    })
    const banner = wrapper.get('[data-test="knowledge-search-error"]')
    expect(banner.text()).toContain('搜索失败')
    expect(banner.text()).toContain('以下为上一次结果')
    // 旧结果仍可见，但明确标注非当前结果
    expect(wrapper.text()).toContain('上一次结果')
  })
})
