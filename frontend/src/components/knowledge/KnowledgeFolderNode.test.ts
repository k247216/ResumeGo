// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeFolderNode from './KnowledgeFolderNode.vue'
import type { KnowledgeCategoryNode } from '../../types/knowledge'

const node = (id: number, name: string, parentId: number | null): KnowledgeCategoryNode => ({
  id, name, normalizedName: name, parentId, depth: parentId == null ? 0 : 1,
  directDocumentCount: 0, descendantDocumentCount: 0, createdAt: 't', updatedAt: 't',
})

describe('KnowledgeFolderNode', () => {
  it('shows a real folder icon beside the disclosure control', () => {
    const wrapper = mount(KnowledgeFolderNode, {
      props: {
        node: node(1, '技术知识', null),
        allNodes: [node(1, '技术知识', null), node(2, 'Redis', 1)],
        expandedIds: new Set([1]),
        selectedId: null,
      },
    })

    expect(wrapper.get('[data-test="folder-icon-1"]').attributes('data-state')).toBe('open')
  })
})
