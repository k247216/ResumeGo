// @vitest-environment happy-dom

import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeNameDialog from './KnowledgeNameDialog.vue'

describe('KnowledgeNameDialog', () => {
  it('focuses the name field when a new tag dialog opens', async () => {
    const wrapper = mount(KnowledgeNameDialog, {
      props: { kind: 'tag', submitting: false, error: '' },
      attachTo: document.body,
    })
    await nextTick()
    await nextTick()
    expect(document.activeElement).toBe(wrapper.get('[data-test="knowledge-name-input-tag"]').element)
  })
})
