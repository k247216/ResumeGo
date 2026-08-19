import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { InterviewRecord } from '../../utils/interviewRecords'
import InterviewHistoryPanel from './InterviewHistoryPanel.vue'

const activeRecord: InterviewRecord = {
  id: '21',
  title: '示例公司 · 后端工程师',
  subtitle: '技术负责人 / HR',
  sessions: [{
    sessionId: 31,
    status: 'WAITING_ANSWER',
    currentQuestionIndex: 2,
    totalQuestions: 5,
    currentQuestion: null,
    completed: false,
    perQuestionScores: [],
    personaName: '技术负责人',
    personaTitle: '面试官',
  }],
  latestSession: {
    sessionId: 31,
    status: 'WAITING_ANSWER',
    currentQuestionIndex: 2,
    totalQuestions: 5,
    currentQuestion: null,
    completed: false,
    perQuestionScores: [],
    personaName: '技术负责人',
    personaTitle: '面试官',
  },
  job: null,
  resumeLabel: '主简历 v3',
  completedCount: 0,
  totalCount: 1,
  isCompleted: false,
  isInProgress: true,
  jobDescriptionId: 8,
  resumeVersionId: 12,
}

function mountPanel(records: InterviewRecord[] = [activeRecord]) {
  return mount(InterviewHistoryPanel, {
    props: {
      records,
      filteredRecords: records,
      recentRecords: records,
      filterTabs: [
        { key: 'all', label: '全部', count: records.length },
        { key: 'completed', label: '已完成', count: 0 },
        { key: 'inProgress', label: '进行中', count: records.length },
      ],
      activeFilter: 'all',
    },
    global: {
      stubs: {
        CompanyAvatar: true,
        ElIcon: { template: '<span><slot /></span>' },
        ElProgress: { template: '<div data-test="progress" />' },
        ArrowRight: true,
        Delete: true,
        VideoPlay: true,
      },
    },
  })
}

describe('InterviewHistoryPanel', () => {
  it('shows plan-level history and emits open and delete intents', async () => {
    const wrapper = mountPanel()

    expect(wrapper.text()).toContain('示例公司 · 后端工程师')
    expect(wrapper.text()).toContain('0/1 轮')

    await wrapper.get('[data-test="history-open-21"]').trigger('click')
    await wrapper.get('[data-test="history-delete-21"]').trigger('click')

    expect(wrapper.emitted('open')).toEqual([[activeRecord]])
    expect(wrapper.emitted('delete')).toEqual([[activeRecord]])
  })

  it('emits filter changes without owning page state', async () => {
    const wrapper = mountPanel()

    await wrapper.get('[data-test="history-filter-completed"]').trigger('click')

    expect(wrapper.emitted('update:activeFilter')).toEqual([['completed']])
  })

  it('renders a useful empty state', () => {
    const wrapper = mountPanel([])

    expect(wrapper.text()).toContain('还没有面试记录')
    expect(wrapper.text()).toContain('创建一次面试')
  })
})
