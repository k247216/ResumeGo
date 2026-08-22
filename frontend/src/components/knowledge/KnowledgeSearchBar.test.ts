// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeSearchBar from './KnowledgeSearchBar.vue'
import type { KnowledgeCategory, KnowledgeTag } from '../../types/knowledge'

const category: KnowledgeCategory = { id: 1, name: '求职', normalizedName: '求职', createdAt: 't', updatedAt: 't' }
const tag: KnowledgeTag = { id: 2, name: '机器学习', normalizedName: '机器学习', createdAt: 't', updatedAt: 't' }

function mountBar(props: Record<string, unknown> = {}) {
  return mount(KnowledgeSearchBar, {
    props: {
      query: '',
      categoryId: null,
      tagId: null,
      categories: [category],
      tags: [tag],
      ...props,
    },
  })
}

describe('KnowledgeSearchBar', () => {
  it('forwards typed queries to update-query', async () => {
    const wrapper = mountBar()
    await wrapper.get('[data-test="knowledge-search-input"]').setValue('笔记')
    expect(wrapper.emitted('update-query')).toEqual([['笔记']])
  })

  it('emits null category when cleared and id when selected', async () => {
    const wrapper = mountBar({ categoryId: 1 })
    await wrapper.get('[data-test="knowledge-search-category"]').setValue('')
    expect(wrapper.emitted('update-category')).toEqual([[null]])

    await wrapper.get('[data-test="knowledge-search-category"]').setValue('1')
    expect(wrapper.emitted('update-category')).toEqual([[null], [1]])
  })

  it('emits null tag when cleared and id when selected', async () => {
    const wrapper = mountBar()
    await wrapper.get('[data-test="knowledge-search-tag"]').setValue('')
    expect(wrapper.emitted('update-tag')).toEqual([[null]])
    await wrapper.get('[data-test="knowledge-search-tag"]').setValue('2')
    expect(wrapper.emitted('update-tag')).toEqual([[null], [2]])
  })
})
