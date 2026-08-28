// @vitest-environment happy-dom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ExperienceSimulationSetup from './ExperienceSimulationSetup.vue'

const baseDraft = {
  questionSetId: 4,
  personaIds: [],
  followUpIntensity: '适中',
  questionCount: 10,
  focusTags: [],
  supplement: '',
}

describe('ExperienceSimulationSetup', () => {
  it('uses one Knowledge Base source instead of separate source filters', () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: { draft: baseDraft, questionSetOptions: [] },
    })

    expect(wrapper.findAll('.rail-filter')).toHaveLength(0)
    expect(wrapper.get('[data-test="experience-source-summary"]').text()).toContain('知识库资料')
  })

  it('uses the selected set size as the real question limit', async () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: {
        draft: baseDraft,
        questionSetOptions: [{ value: 4, label: '腾讯技术面经', itemCount: 12, sourceType: 'IMPORTED_EXPERIENCE', companyName: '腾讯', targetRole: 'Java 后端', companyIconKey: 'tencent', updatedAt: '2026-08-26' }],
        preview: { id: 4, title: '腾讯技术面经', typeLabel: '真实题集', meta: '12 道', content: '1. 讲讲项目' },
      },
    })

    expect(wrapper.get('.question-count-range').text()).toBe('10–12 题')
    expect(wrapper.get('.question-set-company').text()).toBe('腾讯')
    expect(wrapper.get('.question-set-role').text()).toContain('Java 后端')
    await wrapper.get('[aria-label="增加题目"]').trigger('click')
    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ questionCount: 11 }))
  })

  it('allows cancelling the selected question set', async () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: {
        draft: baseDraft,
        questionSetOptions: [{ value: 4, label: '腾讯技术面经', itemCount: 12, sourceType: 'USER_MANUAL' }],
      },
    })

    await wrapper.get('.question-set-row').trigger('click')

    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ questionSetId: null }))
    expect(wrapper.emitted('preview-question-set')?.at(-1)).toEqual([null])
  })

  it('keeps source labels concise', () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: {
        draft: baseDraft,
        questionSetOptions: [
          { value: 4, label: '手动面经', itemCount: 2, sourceType: 'USER_MANUAL' },
          { value: 5, label: '外部面经', itemCount: 2, sourceType: 'IMPORTED_EXPERIENCE' },
          { value: -7, label: '导入的 txt 面经', sourceType: 'KNOWLEDGE_DOCUMENT', knowledgeSourceType: 'FILE' },
        ],
      },
    })

    expect(wrapper.findAll('.source-tag').map((item) => item.text())).toEqual(['手动笔记', '外部导入', '外部导入'])
  })

  it('reorders selected questions with draggable cards without changing the source set', async () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: {
        draft: { ...baseDraft, questionOrder: [] },
        questionSetOptions: [{ value: 4, label: '题集', itemCount: 3, sourceType: 'USER_MANUAL' }],
        preview: { id: 4, title: '题集', typeLabel: '真实题集', meta: '手动笔记', content: '1. 第一题\n2. 第二题\n3. 第三题' },
      },
    })

    const rows = wrapper.findAll('.question-preview-row')
    expect(rows[0].attributes('draggable')).toBe('true')
    expect(wrapper.find('.question-order-actions').exists()).toBe(false)
    await rows[0].trigger('dragstart')
    await rows[2].trigger('dragover')
    await rows[2].trigger('drop')

    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({
      questionSetId: 4,
      questionOrder: [1, 2, 0],
    }))
  })

  it('offers review mode and removable focus tags without changing the source set', async () => {
    const wrapper = mount(ExperienceSimulationSetup, {
      props: {
        draft: { ...baseDraft, focusTags: ['项目还原'] },
        questionSetOptions: [{ value: 4, label: '题集', itemCount: 8, sourceType: 'USER_MANUAL' }],
      },
    })
    await wrapper.get('[data-test="experience-review-end"]').trigger('click')
    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ reviewMode: 'END_OF_SESSION' }))
    await wrapper.get('[data-test="experience-focus-remove-项目还原"]').trigger('click')
    expect(wrapper.emitted('update:draft')?.at(-1)?.[0]).toEqual(expect.objectContaining({ focusTags: [] }))
  })
})
