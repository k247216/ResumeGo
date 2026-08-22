// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeClassificationPanel from './KnowledgeClassificationPanel.vue'
import type { KnowledgeCategory, KnowledgeDocumentClassification, KnowledgeTag } from '../../types/knowledge'

const category = (id: number, name: string): KnowledgeCategory => ({ id, name, normalizedName: name, createdAt: 't', updatedAt: 't' })
const tag = (id: number, name: string): KnowledgeTag => ({ id, name, normalizedName: name, createdAt: 't', updatedAt: 't' })

function mountPanel(props: Record<string, unknown> = {}) {
  return mount(KnowledgeClassificationPanel, {
    props: {
      classification: null,
      loading: false,
      saving: false,
      error: '',
      categories: [category(1, '求职'), category(3, '技能')],
      tags: [tag(2, '机器学习'), tag(4, '面试')],
      ...props,
    },
  })
}

const classification = (overrides: Partial<KnowledgeDocumentClassification> = {}): KnowledgeDocumentClassification => ({
  category: null,
  tags: [],
  ...overrides,
})

describe('KnowledgeClassificationPanel', () => {
  it('shows the current category and emits changes from the selector', async () => {
    const wrapper = mountPanel({ classification: classification({ category: category(1, '求职') }) })
    expect(wrapper.get('[data-test="classification-category"]').element).toHaveProperty('value', '1')
    await wrapper.get('[data-test="classification-category"]').setValue('3')
    expect(wrapper.emitted('set-category')).toEqual([[3]])
    await wrapper.get('[data-test="classification-category"]').setValue('')
    expect(wrapper.emitted('set-category')).toEqual([[3], [null]])
  })

  it('lists owned tags and hides already-used ones from the add menu', async () => {
    const wrapper = mountPanel({
      classification: classification({ tags: [tag(2, '机器学习')] }),
    })
    expect(wrapper.get('[data-test="classification-tag-2"]').text()).toContain('机器学习')
    const options = wrapper.get('[data-test="classification-add-tag"]').findAll('option').map((o) => o.text())
    expect(options).toEqual(['+ 添加标签', '面试'])
  })

  it('emits toggle-tag on add and remove', async () => {
    const wrapper = mountPanel({
      classification: classification({ tags: [tag(2, '机器学习')] }),
    })
    await wrapper.get('[data-test="classification-remove-tag-2"]').trigger('click')
    expect(wrapper.emitted('toggle-tag')).toEqual([[2, false]])
    await wrapper.get('[data-test="classification-add-tag"]').setValue('4')
    expect(wrapper.emitted('toggle-tag')).toEqual([[2, false], [4, true]])
  })

  it('shows loading and honest empty state', () => {
    const loading = mountPanel({ loading: true })
    expect(loading.get('[data-test="classification-loading"]').text()).toContain('正在读取关联')

    const empty = mountPanel({ classification: classification() })
    expect(empty.get('[data-test="classification-no-tags"]').text()).toContain('暂无标签')
  })

  it('keeps old state visible when a save fails and shows the error', () => {
    const wrapper = mountPanel({
      classification: classification({ category: category(1, '求职') }),
      error: '设置分类失败',
    })
    expect(wrapper.get('[data-test="classification-category"]').element).toHaveProperty('value', '1')
    expect(wrapper.get('[data-test="classification-save-error"]').text()).toContain('设置分类失败')
  })

  it('emits create-category and create-tag', async () => {
    const wrapper = mountPanel({ classification: classification() })
    await wrapper.get('[data-test="classification-new-category"]').trigger('click')
    await wrapper.get('[data-test="classification-new-tag"]').trigger('click')
    expect(wrapper.emitted('create-category')).toHaveLength(1)
    expect(wrapper.emitted('create-tag')).toHaveLength(1)
  })
})
