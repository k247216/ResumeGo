// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeSourceInspector from './KnowledgeSourceInspector.vue'
import type { KnowledgeCategoryNode, KnowledgeDocument, KnowledgeDocumentClassification } from '../../types/knowledge'

const doc = (id: number, sourceType: 'NOTE' | 'FILE', status: KnowledgeDocument['processingStatus']): KnowledgeDocument => ({
  id, title: '文档 ' + id, sourceType, processingStatus: status, sourceFile: null,
  createdAt: 't', updatedAt: 't',
})

const categoryNode = (id: number, name: string): KnowledgeCategoryNode => ({
  id, name, normalizedName: name, parentId: null, depth: 0,
  directDocumentCount: 0, descendantDocumentCount: 0, createdAt: 't', updatedAt: 't',
})

const classification = (overrides: Partial<KnowledgeDocumentClassification> = {}): KnowledgeDocumentClassification => ({
  category: null, tags: [], ...overrides,
})

function mountInspector(props: Record<string, unknown> = {}) {
  return mount(KnowledgeSourceInspector, {
    props: {
      document: null, classification: null, classificationError: '',
      categories: [categoryNode(1, '求职')], tags: [{ id: 2, name: '机器学习', normalizedName: '机器学习', createdAt: 't', updatedAt: 't' }],
      saving: false, retrying: false, retryError: '', deleteError: '', sourceResultMessage: '',
      categoryPaths: {},
      ...props,
    },
  })
}

describe('KnowledgeSourceInspector', () => {
  it('emits close and only shows the current document', async () => {
    const wrapper = mountInspector({ document: doc(1, 'NOTE', 'COMPLETED') })
    await wrapper.get('[data-test="inspector-close"]').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('shows the current category and emits changes', async () => {
    const wrapper = mountInspector({
      document: doc(1, 'NOTE', 'COMPLETED'),
      classification: classification({ category: categoryNode(1, '求职') }),
    })
    expect(wrapper.get('[data-test="inspector-category"]').element).toHaveProperty('value', '1')
    await wrapper.get('[data-test="inspector-category"]').setValue('')
    expect(wrapper.emitted('set-category')).toEqual([[null]])
  })

  it('enables open/reveal only for completed FILE documents', () => {
    const note = mountInspector({ document: doc(1, 'NOTE', 'COMPLETED') })
    expect(note.find('[data-test="inspector-open-source"]').exists()).toBe(false)

    const failed = mountInspector({ document: doc(2, 'FILE', 'FAILED') })
    expect((failed.get('[data-test="inspector-open-source"]').element as HTMLButtonElement).disabled).toBe(true)

    const ok = mountInspector({ document: doc(3, 'FILE', 'COMPLETED') })
    expect((ok.get('[data-test="inspector-open-source"]').element as HTMLButtonElement).disabled).toBe(false)
  })

  it('keeps per-document errors isolated and shows retry for FAILED', async () => {
    const wrapper = mountInspector({
      document: doc(2, 'FILE', 'FAILED'),
      classificationError: '文档 A 的错误',
      retryError: '重试失败',
    })
    expect(wrapper.get('[data-test="inspector-category-error"]').text()).toContain('文档 A 的错误')
    await wrapper.get('[data-test="inspector-retry"]').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('emits delete and shows delete error', async () => {
    const wrapper = mountInspector({ document: doc(1, 'NOTE', 'COMPLETED'), deleteError: '删除失败' })
    await wrapper.get('[data-test="inspector-delete"]').trigger('click')
    expect(wrapper.emitted('delete')).toHaveLength(1)
    expect(wrapper.get('[data-test="inspector-delete-error"]').text()).toContain('删除失败')
  })
})
