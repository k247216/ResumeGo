// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeTrainingSetup from './KnowledgeTrainingSetup.vue'

const draft = {
  knowledgeDocumentIds: [],
  difficulty: '',
  questionCount: 5,
  focusTags: [],
  supplement: '',
}

describe('KnowledgeTrainingSetup', () => {
  it('uses real file metadata and emits a selected folder', async () => {
    const wrapper = mount(KnowledgeTrainingSetup, {
      props: {
        draft,
        documentOptions: [{ value: 7, label: 'Redis 笔记', fileType: 'MD', fileSize: '12 KB', updatedAt: '2026-08-27T10:00:00', meta: 'redis.md' }],
        categoryOptions: [{ id: 3, name: 'Java 后端', normalizedName: 'java', parentId: null, depth: 0, directDocumentCount: 1, descendantDocumentCount: 1, createdAt: 't', updatedAt: 't' }],
        selectedCategoryId: null,
      },
    })

    expect(wrapper.get('.document-size').text()).toBe('12 KB')
    await wrapper.get('[data-test="knowledge-folder-picker"]').trigger('click')
    await wrapper.get('[data-test="knowledge-folder-option-3"]').trigger('click')
    expect(wrapper.emitted('select-category')).toEqual([[3]])
  })

  it('offers opening the selected preview in the Knowledge Base', async () => {
    const wrapper = mount(KnowledgeTrainingSetup, {
      props: {
        draft: { ...draft, knowledgeDocumentIds: [7] },
        documentOptions: [{ value: 7, label: 'Redis 笔记', fileType: 'MD', fileSize: '12 KB', meta: 'redis.md' }],
        categoryOptions: [],
        selectedCategoryId: null,
        preview: { id: 7, title: 'Redis 笔记', typeLabel: 'MD', meta: 'redis.md · 12 KB', content: '# Redis' },
      },
    })
    await wrapper.get('[data-test="knowledge-open-document"]').trigger('click')
    expect(wrapper.emitted('open-knowledge-document')).toEqual([[7]])
  })

  it('allows removing a document from the selected Knowledge Base context', async () => {
    const wrapper = mount(KnowledgeTrainingSetup, {
      props: {
        draft: { ...draft, knowledgeDocumentIds: [7] },
        documentOptions: [{ value: 7, label: 'Redis 笔记', fileType: 'MD', fileSize: '12 KB', meta: 'redis.md' }],
        categoryOptions: [],
        selectedCategoryId: null,
      },
    })

    await wrapper.get('[aria-label="取消选择 Redis 笔记"]').trigger('click')
    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ knowledgeDocumentIds: [] }))
  })

  it('explains depth and exposes selectable question styles', async () => {
    const wrapper = mount(KnowledgeTrainingSetup, {
      props: { draft, documentOptions: [], categoryOptions: [], selectedCategoryId: null },
    })
    expect(wrapper.find('.setting-description').text()).toContain('识别概念和术语')
    expect(wrapper.get('[data-test="knowledge-style-结构化"]').text()).toContain('逐层拆解')
    await wrapper.get('[data-test="knowledge-style-案例型"]').trigger('click')
    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ questionStyle: '案例型' }))
  })
})
