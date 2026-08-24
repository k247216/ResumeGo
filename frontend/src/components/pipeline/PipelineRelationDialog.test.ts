// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PipelineRelationDialog from './PipelineRelationDialog.vue'
import type { CareerPipeline } from '../../types/pipeline'

function pipeline(scheduleEventIds: number[] = [], interviewPlanIds: number[] = []): CareerPipeline {
  return {
    id: 7, name: '腾讯', companyName: '腾讯', roleTitle: 'Java', jobDescriptionId: null,
    resumeVersionId: null, lifecycle: 'ACTIVE', outcome: null, currentStageId: null,
    stages: [], scheduleEventIds, interviewPlanIds, archivedAt: null,
    createdAt: '2026-08-22T10:00:00', updatedAt: '2026-08-22T10:00:00',
  }
}

describe('PipelineRelationDialog', () => {
  it('shows missing schedule and interview links as unavailable and allows unlink', async () => {
    // 已绑定 id 100（日程）与 300（面试），但加载到的对象里没有它们
    const wrapper = mount(PipelineRelationDialog, {
      props: {
        pipeline: pipeline([100], [300]),
        scheduleEvents: [{ id: 1, title: '笔试' }],
        interviewPlans: [{ id: 2, jobLabel: '技术面' }],
        busy: false,
        error: '',
      },
    })
    expect(wrapper.text()).toContain('关联不可用 #100')
    expect(wrapper.text()).toContain('关联不可用 #300')

    await wrapper.get('[data-test="pipeline-relation-unlink-schedule-missing"]').trigger('click')
    expect(wrapper.emitted('toggle-schedule')).toEqual([[100, true]])
    await wrapper.get('[data-test="pipeline-relation-unlink-interview-missing"]').trigger('click')
    expect(wrapper.emitted('toggle-interview')).toEqual([[300, true]])
  })

  it('lists resolvable links and toggles them', async () => {
    const wrapper = mount(PipelineRelationDialog, {
      props: {
        pipeline: pipeline([1], [2]),
        scheduleEvents: [{ id: 1, title: '笔试' }],
        interviewPlans: [{ id: 2, jobLabel: '技术面' }],
        busy: false,
        error: '',
      },
    })
    expect(wrapper.text()).toContain('笔试')
    await wrapper.get('[data-test="pipeline-relation-unlink-schedule"]').trigger('click')
    expect(wrapper.emitted('toggle-schedule')).toEqual([[1, true]])
  })
})
