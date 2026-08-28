import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetDashboard, { type AgendaEventView, type DetailView, type RecentActivityItem } from './TargetDashboard.vue'

function makeEvent(overrides: Partial<AgendaEventView> = {}): AgendaEventView {
  return {
    id: '11', title: '技术面', eventType: 'interview', timeLabel: '14:30', relativeLabel: '今天', dayLabel: '8月28日',
    companyLabel: '腾讯', roleLabel: 'Java 后端实习', countdownLabel: '还有 2 小时', ...overrides,
  }
}

function makeDetail(overrides: Partial<DetailView> = {}): DetailView {
  return {
    kind: 'event', note: '', companyLabel: '腾讯', roleLabel: 'Java 后端实习', targetName: '腾讯 · Java 后端实习',
    targetLinked: true, targetId: 3,
    readiness: [{ key: 'resume', label: '简历', meta: '后端开发实习简历 · V3', subMeta: '8/28 更新', ready: true, actionLabel: '编辑', action: 'open-editor' }],
    nextAction: { text: '面试前完成一次针对当前目标的模拟面试', button: '开始准备', action: 'open-interview' },
    ...overrides,
  }
}

function mountDashboard(props: Record<string, unknown> = {}) {
  return mount(TargetDashboard, { props: { agendaEvents: [], selectedEventId: null, detail: makeDetail(), recentActivity: [], ...props } })
}

describe('TargetDashboard', () => {
  it('renders the approved workspace composition without a page title or notification block', () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })
    expect(wrapper.find('[data-test="agenda-pane"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="detail-pane"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="next-interview"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('工作台')
    expect(wrapper.text()).not.toContain('通知')
    expect(wrapper.text()).not.toContain('AI 已连接')
  })

  it('shows at most three real upcoming events and selects an event from the agenda', async () => {
    const events = [
      makeEvent({ id: '1', relativeLabel: '今天', companyLabel: '腾讯' }),
      makeEvent({ id: '2', relativeLabel: '明天', companyLabel: '字节跳动' }),
      makeEvent({ id: '3', relativeLabel: '2 天后', companyLabel: '美团' }),
      makeEvent({ id: '4', relativeLabel: '3 天后', companyLabel: '阿里巴巴' }),
    ]
    const wrapper = mountDashboard({ agendaEvents: events, selectedEventId: '1' })
    expect(wrapper.findAll('[data-test="agenda-row"]')).toHaveLength(3)
    expect(wrapper.findAll('[data-test="agenda-group"]')).toHaveLength(3)
    expect(wrapper.get('[data-test="agenda-row"]').classes()).toContain('selected')
    await wrapper.findAll('[data-test="agenda-row"]')[1].trigger('click')
    expect(wrapper.emitted('select-event')).toEqual([['2']])
  })

  it('shows the bound job target name as a quiet secondary line in every agenda row', () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent({ targetName: '腾讯 Java 后端实习' })],
      selectedEventId: '11',
    })

    expect(wrapper.get('[data-test="agenda-target"]').text()).toBe('腾讯 Java 后端实习')
  })

  it('does not layer a fallback initial over an available company logo', () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent({ logoUrl: 'data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22/%3E' })], selectedEventId: '11' })

    expect(wrapper.find('.next-company-mark img').exists()).toBe(true)
    expect(wrapper.find('.next-company-mark > span').exists()).toBe(false)
  })

  it('lets an available logo keep its own colors instead of applying the fallback brand tint', () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent({ logoUrl: 'data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22/%3E', logoColor: '#00C8D2' })], selectedEventId: '11' })

    expect(wrapper.get('.next-company-mark').classes()).toContain('has-logo')
    expect(wrapper.get('.next-company-mark').attributes('style')).toBeUndefined()
  })

  it('keeps the selected interview facts, context rows and next action together', async () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })
    expect(wrapper.get('[data-test="detail-title"]').text()).toBe('腾讯 · 技术面')
    expect(wrapper.get('[data-test="detail-time"]').text()).toBe('14:30')
    expect(wrapper.get('[data-test="detail-linked-target"]').text()).toContain('腾讯 · Java 后端实习')
    expect(wrapper.get('[data-test="readiness-resume"]').text()).toContain('后端开发实习简历 · V3')
    expect(wrapper.get('[data-test="detail-next"]').text()).toContain('面试前完成一次针对当前目标的模拟面试')
    await wrapper.get('[data-test="next-button"]').trigger('click')
    expect(wrapper.emitted('action')).toContainEqual(['open-target', 3])
    await wrapper.get('[data-test="view-schedule"]').trigger('click')
    expect(wrapper.emitted('action')).toContainEqual(['open-interview-home', 3])
  })

  it('keeps a small visual breathing space between preparation actions and context rows', () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })

    expect(wrapper.get('[data-test="next-actions"]').classes()).toContain('next-actions--spaced')
    expect(wrapper.get('[data-test="detail-linked-target"]').classes()).toContain('context-rows--spaced')
  })

  it('does not fabricate target or resume context for an unlinked event', () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent({ id: '12', title: '笔试', companyLabel: '', roleLabel: '' })], selectedEventId: '12',
      detail: makeDetail({ targetLinked: false, targetId: null, companyLabel: '', roleLabel: '', targetName: '', readiness: [], nextAction: null }),
    })
    expect(wrapper.get('[data-test="detail-unlinked"]').text()).toContain('未关联求职目标')
    expect(wrapper.find('[data-test="readiness-resume"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="detail-next"]').exists()).toBe(false)
  })

  it('provides honest empty and target fallback states', async () => {
    const empty = mountDashboard({ detail: makeDetail({ kind: 'empty', note: '还没有求职目标', targetLinked: false, targetId: null, readiness: [], nextAction: null }) })
    expect(empty.get('[data-test="detail-empty"]').text()).toContain('今天没有安排')
    expect(empty.get('[data-test="detail-empty-action"]').text()).toContain('录入岗位')
    expect(empty.get('[data-test="agenda-pane"]').text()).toContain('近期没有安排')
    const target = mountDashboard({ detail: makeDetail({ kind: 'target', note: '当前没有选中的安排' }) })
    expect(target.get('[data-test="detail-empty"]').text()).toContain('还没有安排面试')
    expect(target.find('[data-test="detail-identity"]').exists()).toBe(false)
    expect(target.find('[data-test="readiness-resume"]').exists()).toBe(false)
    expect(target.find('[data-test="detail-next"]').exists()).toBe(false)
    await target.get('[data-test="detail-empty-action"]').trigger('click')
    expect(target.emitted('action')).toContainEqual(['open-schedule', undefined])
  })

  it('keeps the reference split fixed instead of exposing a draggable divider', async () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })
    const separator = wrapper.get('[data-test="pane-separator"]')
    expect(separator.attributes('aria-hidden')).toBe('true')
    expect(separator.attributes('role')).toBeUndefined()
    expect(separator.attributes('tabindex')).toBeUndefined()
    expect(wrapper.get('.master-grid').attributes('style')).toContain('520px')
    expect(wrapper.get('.agenda-timeline').classes()).not.toContain('compact')
  })

  it('renders recent feedback and routes the tool index to real modules', async () => {
    const activity: RecentActivityItem[] = [{ id: 9, dateLabel: '8月27日', companyLabel: '腾讯', title: '技术面', summary: '项目回答需要补充技术取舍。', targetId: 3 }]
    const wrapper = mountDashboard({ recentActivity: activity })
    expect(wrapper.get('[data-test="recent-activity"]').text()).toContain('项目回答需要补充技术取舍')
    await wrapper.find('.tool-item').trigger('click')
    expect(wrapper.emitted('action')).toContainEqual(['open-resumes', 3])
    await wrapper.findAll('.tool-item')[1].trigger('click')
    expect(wrapper.emitted('action')).toContainEqual(['open-knowledge', 3])
    await wrapper.findAll('.tool-item')[2].trigger('click')
    expect(wrapper.emitted('action')).toContainEqual(['open-interview-home', 3])
  })

  it('marks the direct-tool rail as bounded columns with an outer edge', () => {
    const wrapper = mountDashboard()

    expect(wrapper.get('[data-test="tool-list"]').classes()).toContain('tool-list--bounded')
    const items = wrapper.findAll('.tool-item')
    expect(items).toHaveLength(5)
    expect(items.every((item) => item.classes().includes('tool-item--centered'))).toBe(true)
  })
})
