// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeDocumentList from './KnowledgeDocumentList.vue'
import type { KnowledgeDocument, KnowledgeSearchItem } from '../../types/knowledge'

function doc(id: number, status: KnowledgeDocument['processingStatus'], title = '文档 ' + id): KnowledgeDocument {
  return {
    id, title, sourceType: 'FILE', processingStatus: status, sourceFile: null,
    createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function searchItem(id: number, snippet: string, lineNumber: number | null): KnowledgeSearchItem {
  return { document: doc(id, 'COMPLETED'), matchedField: 'CONTENT', snippet, lineNumber }
}

function mountList(props: Record<string, unknown> = {}) {
  return mount(KnowledgeDocumentList, {
    props: {
      documents: [], results: [], hasSearch: false, selectedId: null,
      loading: false, errorMessage: '', collapsed: false, scopeLabel: '全部资料',
      classificationByDocumentId: {}, categoryPaths: {},
      ...props,
    },
  })
}

describe('KnowledgeDocumentList', () => {
  it('renders real document rows with statuses and retry action for FAILED', async () => {
    const wrapper = mountList({
      documents: [doc(1, 'COMPLETED', '完成笔记'), doc(2, 'FAILED', '失败文件'), doc(3, 'PENDING', '处理中')],
    })
    expect(wrapper.text()).toContain('完成笔记')
    expect(wrapper.text()).toContain('失败文件')
    expect(wrapper.text()).toContain('处理中')
    await wrapper.get('[data-test="doc-row-retry"]').trigger('click')
    expect(wrapper.emitted('retry-doc')).toEqual([[2]])
  })

  it('distinguishes Markdown, TXT, and local notes by their real source metadata', () => {
    const markdown = { ...doc(1, 'COMPLETED', 'Markdown'), sourceFile: 'notes.md' }
    const text = { ...doc(2, 'COMPLETED', 'Text'), sourceFile: 'notes.txt' }
    const note = { ...doc(3, 'COMPLETED', 'Note'), sourceType: 'NOTE' as const }
    const wrapper = mountList({ documents: [markdown, text, note] })
    expect(wrapper.text()).toContain('Markdown')
    expect(wrapper.text()).toContain('TXT')
    expect(wrapper.text()).toContain('笔记')
  })

  it('selects a row', async () => {
    const wrapper = mountList({ documents: [doc(1, 'COMPLETED')] })
    await wrapper.get('[data-test="doc-row-1"]').trigger('click')
    expect(wrapper.emitted('select')).toEqual([[1]])
  })

  it('shows search results with snippets and empty state', () => {
    const wrapper = mountList({ hasSearch: true, results: [searchItem(7, '…命中片段…', 3)] })
    expect(wrapper.text()).toContain('文档 7')
    expect(wrapper.text()).toContain('…命中片段…')

    const empty = mountList({ hasSearch: true, results: [] })
    expect(empty.get('[data-test="doc-list-search-empty"]').text()).toContain('没有匹配结果')
  })

  it('labels retained rows as previous results after a search failure', () => {
    const wrapper = mountList({
      hasSearch: true,
      results: [searchItem(7, '旧命中', 3)],
      errorMessage: '网络不可用',
    })
    expect(wrapper.get('[data-test="doc-list-search-stale"]').text()).toContain('以下为上一次结果')
    expect(wrapper.text()).toContain('旧命中')
  })

  it('collapses and restores without losing selection', async () => {
    const wrapper = mountList({ documents: [doc(1, 'COMPLETED')], selectedId: 1 })
    await wrapper.get('[data-test="doc-list-collapse"]').trigger('click')
    expect(wrapper.emitted('toggle-collapse')).toHaveLength(1)
  })
})
