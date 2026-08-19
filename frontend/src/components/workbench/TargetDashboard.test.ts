import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetDashboard from './TargetDashboard.vue'

describe('TargetDashboard', () => {
  it('offers interview preparation only after job and resume are linked', async () => {
    const wrapper = mount(TargetDashboard, {
      props: {
        target: { id: 3, name: '后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: 21, archivedAt: null, createdAt: '', updatedAt: '' },
      },
    })

    const button = wrapper.get('[data-test="prepare-interview"]')
    expect(button.attributes('disabled')).toBeUndefined()
    await button.trigger('click')
    expect(wrapper.emitted('action')).toEqual([['open-interview']])
  })

  it('keeps interview preparation disabled while target materials are incomplete', () => {
    const wrapper = mount(TargetDashboard, {
      props: {
        target: { id: 3, name: '后端实习', status: 'active', jobDescriptionId: 6, resumeVersionId: null, archivedAt: null, createdAt: '', updatedAt: '' },
      },
    })

    expect(wrapper.get('[data-test="prepare-interview"]').attributes('disabled')).toBeDefined()
  })
})
