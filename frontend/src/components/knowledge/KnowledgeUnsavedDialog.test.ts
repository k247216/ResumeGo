// @vitest-environment happy-dom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import KnowledgeUnsavedDialog from './KnowledgeUnsavedDialog.vue'

describe('KnowledgeUnsavedDialog', () => {
  it('offers keep, discard, and save as explicit choices', async () => {
    const wrapper = mount(KnowledgeUnsavedDialog, { props: { saving: false, error: '' } })
    await wrapper.get('[data-test="knowledge-unsaved-keep"]').trigger('click')
    await wrapper.get('[data-test="knowledge-unsaved-discard"]').trigger('click')
    await wrapper.get('[data-test="knowledge-unsaved-save"]').trigger('click')
    expect(wrapper.emitted('keep-editing')).toHaveLength(1)
    expect(wrapper.emitted('discard')).toHaveLength(1)
    expect(wrapper.emitted('save')).toHaveLength(1)
  })
})
