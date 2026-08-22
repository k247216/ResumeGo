// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeDetailPane from './KnowledgeDetailPane.vue'
import type { KnowledgeDocument } from '../../types/knowledge'

function doc(status: KnowledgeDocument['processingStatus']): KnowledgeDocument {
  return {
    id: 1, title: '示例文档', sourceType: 'FILE', processingStatus: status,
    sourceFile: null, createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

function mountPane(props: Record<string, unknown> = {}) {
  return mount(KnowledgeDetailPane, {
    props: {
      document: doc('COMPLETED'),
      hasDocuments: true,
      content: '',
      contentLoading: false,
      contentError: '',
      ...props,
    },
  })
}

describe('KnowledgeDetailPane', () => {
  it('shows the extracted content for a COMPLETED document', () => {
    const wrapper = mountPane({ content: '第一行\n第二行' })
    expect(wrapper.get('[data-test="knowledge-content"]').text()).toBe('第一行\n第二行')
    expect(wrapper.get('[data-test="knowledge-content"]').attributes('class')).toContain('content')
  })

  it('shows a loading state without fabricating content', () => {
    const wrapper = mountPane({ contentLoading: true })
    expect(wrapper.get('[data-test="knowledge-content-loading"]').text()).toContain('正在读取正文')
    expect(wrapper.find('[data-test="knowledge-content"]').exists()).toBe(false)
  })

  it('shows a content error with a retry action that reloads', async () => {
    const wrapper = mountPane({ contentError: '内容仍在处理中，请稍后重试' })
    expect(wrapper.get('[data-test="knowledge-content-error"]').text()).toContain('内容仍在处理中')
    await wrapper.get('[data-test="knowledge-content-retry"]').trigger('click')
    expect(wrapper.emitted('load-content')).toHaveLength(1)
  })

  it('shows the real processing state for PENDING and RUNNING without content', () => {
    for (const status of ['PENDING', 'RUNNING'] as const) {
      const wrapper = mountPane({ document: doc(status) })
      expect(wrapper.get('[data-test="knowledge-content-pending"]').text()).toContain('内容仍在处理中')
      expect(wrapper.find('[data-test="knowledge-content"]').exists()).toBe(false)
    }
  })

  it('shows failure state without pretending success', () => {
    const wrapper = mountPane({ document: doc('FAILED') })
    expect(wrapper.get('[data-test="knowledge-content-failed"]').text()).toContain('处理失败')
    expect(wrapper.find('[data-test="knowledge-content"]').exists()).toBe(false)
  })

  it('shows not-started state for NOT_STARTED notes', () => {
    const wrapper = mountPane({ document: doc('NOT_STARTED') })
    expect(wrapper.get('[data-test="knowledge-content-not-started"]').text()).toContain('尚未开始处理')
  })

  it('offers a create entry when the library is empty', async () => {
    const wrapper = mountPane({ document: null, hasDocuments: false })
    expect(wrapper.get('[data-test="knowledge-detail-empty-create"]').text()).toContain('新建笔记')
    await wrapper.get('[data-test="knowledge-detail-empty-create"]').trigger('click')
    expect(wrapper.emitted('create-note')).toHaveLength(1)
  })

  it('asks for a selection when documents exist but none is selected', () => {
    const wrapper = mountPane({ document: null, hasDocuments: true })
    expect(wrapper.text()).toContain('选择一份资料')
    expect(wrapper.find('[data-test="knowledge-detail-empty-create"]').exists()).toBe(false)
  })

  it('labels status from the real processingStatus', () => {
    const wrapper = mountPane({ document: doc('FAILED') })
    expect(wrapper.text()).toContain('失败')
  })
})
