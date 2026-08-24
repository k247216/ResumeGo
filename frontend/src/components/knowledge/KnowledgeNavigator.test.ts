// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeNavigator from './KnowledgeNavigator.vue'

describe('KnowledgeNavigator', () => {
  it('shows a tag icon for each saved tag', () => {
    const wrapper = mount(KnowledgeNavigator, {
      props: {
        nodes: [], expandedIds: new Set<number>(), selectedId: null,
        activeTagId: null, treeError: '',
        tags: [{ id: 3, name: 'Java', normalizedName: 'java', createdAt: 't', updatedAt: 't' }],
      },
    })

    expect(wrapper.find('[data-test="navigator-tag-icon-3"]').exists()).toBe(true)
  })
})
