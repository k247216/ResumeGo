import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TargetDashboard, { type AgendaEventView, type DetailView, type RecentActivityItem } from './TargetDashboard.vue'

function makeEvent(overrides: Partial<AgendaEventView> = {}): AgendaEventView {
  return {
    id: '11', title: '技术面', eventType: 'interview', timeLabel: '14:30', relativeLabel: '今天', dayLabel: '',
    companyLabel: '腾讯', roleLabel: 'Java 后端实习', countdownLabel: '',
    ...overrides,
  }
}

function makeDetail(overrides: Partial<DetailView> = {}): DetailView {
  return {
    kind: 'event', note: '', companyLabel: '腾讯', roleLabel: 'Java 后端实习', targetName: '腾讯 · Java 后端实习',
    targetLinked: true, targetId: 3,
    readiness: [
      { key: 'resume', label: '简历', meta: '通用简历 · V2', subMeta: '7/15 更新', ready: true, actionLabel: '编辑', action: 'open-editor' },
      { key: 'mock', label: '模拟面试', meta: '尚未完成', subMeta: '', ready: false, actionLabel: '开始', action: 'open-interview' },
      { key: 'schedule', label: '日程', meta: '今天 14:30', subMeta: '', ready: true, actionLabel: '查看', action: 'open-schedule' },
    ],
    nextAction: { text: '面试前完成一次针对当前目标的模拟面试', button: '开始模拟面试', hint: '预计 20 分钟', action: 'open-interview' },
    ...overrides,
  }
}

function mountDashboard(props: Record<string, unknown> = {}) {
  return mount(TargetDashboard, {
    props: { agendaEvents: [], selectedEventId: null, detail: makeDetail(), recentActivity: [], ...props },
  })
}

describe('TargetDashboard', () => {
  it('renders only the three spatial regions without toolbar, page title, or help', () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })

    expect(wrapper.find('[data-test="agenda-pane"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="detail-pane"]').exists()).toBe(true)
    expect(wrapper.find('h1').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('使用帮助')
    expect(wrapper.text()).not.toContain('今日工作区')
    expect(wrapper.text()).not.toContain('通知')
    expect(wrapper.text()).not.toContain('总分')
  })

  it('groups upcoming events by day in the agenda rail with a selected navigation row', async () => {
    const events: AgendaEventView[] = [
      makeEvent({ id: '1', title: '笔试', timeLabel: '09:50', relativeLabel: '明天', companyLabel: '字节跳动', roleLabel: '后端开发实习', countdownLabel: '还有 18 小时' }),
      makeEvent({ id: '2', title: '技术二面', timeLabel: '14:30', relativeLabel: '2 天后', companyLabel: '阿里巴巴', roleLabel: '后端开发' }),
      makeEvent({ id: '3', title: 'HR 面', timeLabel: '10:00', relativeLabel: '2 天后', companyLabel: '美团', roleLabel: '前端实习' }),
    ]
    const wrapper = mountDashboard({ agendaEvents: events, selectedEventId: '1' })

    const pane = wrapper.get('[data-test="agenda-pane"]')
    expect(pane.text()).toContain('接下来')
    expect(pane.text()).toContain('3 场安排')
    const groups = wrapper.findAll('[data-test="agenda-group"]')
    expect(groups).toHaveLength(2)
    expect(groups[0].text()).toContain('明天')
    expect(groups[1].text()).toContain('2 天后')

    const rows = wrapper.findAll('[data-test="agenda-row"]')
    expect(rows).toHaveLength(3)
    expect(rows[0].find('.row-title').text()).toBe('字节跳动 · 笔试')
    expect(rows[0].find('.row-meta').text()).toBe('09:50')
    expect(rows[0].classes()).toContain('selected')
    expect(rows[1].classes()).not.toContain('selected')

    await rows[2].trigger('click')
    expect(wrapper.emitted('select-event')).toEqual([['3']])
  })

  it('shows the selected event facts directly at the top of the detail pane', () => {
    const events = [makeEvent({ id: '1', title: '笔试', timeLabel: '09:50', relativeLabel: '明天', companyLabel: '字节跳动', roleLabel: '后端开发实习', countdownLabel: '还有 18 小时' })]
    const wrapper = mountDashboard({ agendaEvents: events, selectedEventId: '1' })

    const detail = wrapper.get('[data-test="detail-pane"]')
    expect(detail.text()).toContain('明天')
    expect(wrapper.get('[data-test="detail-time"]').text()).toBe('09:50')
    expect(wrapper.get('[data-test="detail-title"]').text()).toBe('字节跳动 · 笔试')
    // 头部不再重复岗位行；岗位并入关联目标行（去重契约）
    expect(detail.text()).not.toContain('后端开发实习')
    expect(detail.text()).toContain('腾讯 · Java 后端实习')
    expect(detail.text()).toContain('还有 18 小时')
  })

  it('shows the linked target, a preparation timeline, next action with estimate, and target link for a linked event', async () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })

    const linked = wrapper.get('[data-test="detail-linked-target"]')
    expect(linked.text()).toContain('关联目标')
    // 关联目标行 = 目标公司 · 岗位，岗位不再出现在头部（去重契约）
    expect(linked.text()).toContain('腾讯 · Java 后端实习')
    expect(linked.text()).toContain('目标详情')
    expect(wrapper.get('[data-test="detail-pane"]').text()).toContain('Java 后端实习')

    const resumeRow = wrapper.get('[data-test="readiness-resume"]')
    expect(resumeRow.text()).toContain('通用简历 · V2')
    expect(resumeRow.text()).toContain('7/15 更新')
    expect(wrapper.get('[data-test="readiness-mock"]').text()).toContain('尚未完成')
    expect(wrapper.get('[data-test="readiness-schedule"]').text()).toContain('今天 14:30')
    const next = wrapper.get('[data-test="detail-next"]')
    expect(next.text()).toContain('开始模拟面试')
    expect(next.text()).toContain('预计 20 分钟')

    await resumeRow.find('.object-action').trigger('click')
    await wrapper.get('[data-test="next-button"]').trigger('click')
    await wrapper.get('[data-test="linked-target-open"]').trigger('click')
    expect(wrapper.emitted('action')).toEqual([
      ['open-editor', 3],
      ['open-interview', 3],
      ['open-target', 3],
    ])
  })

  it('does not fabricate preparation data for an event without a linked target', async () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent({ id: '12', title: '行测', companyLabel: '', roleLabel: '', countdownLabel: '还有 18 小时' })],
      selectedEventId: '12',
      detail: makeDetail({ targetLinked: false, targetId: null, companyLabel: '', roleLabel: '', targetName: '', readiness: [], nextAction: null }),
    })

    expect(wrapper.get('[data-test="detail-title"]').text()).toBe('行测')
    expect(wrapper.get('[data-test="detail-time"]').text()).toBe('14:30')
    const unlinked = wrapper.get('[data-test="detail-unlinked"]')
    expect(unlinked.text()).toContain('未关联求职目标')
    expect(unlinked.text()).toContain('关联目标')
    expect(unlinked.text()).toContain('查看日程')
    expect(wrapper.find('[data-test="readiness-resume"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="detail-next"]').exists()).toBe(false)

    await wrapper.get('[data-test="link-target"]').trigger('click')
    await wrapper.get('[data-test="view-schedule"]').trigger('click')
    expect(wrapper.emitted('action')).toEqual([
      ['link-target', 12],
      ['open-schedule'],
    ])
  })

  it('collapses into compact empty states without arrangements or targets', async () => {
    const wrapper = mountDashboard({
      detail: makeDetail({ kind: 'empty', note: '还没有求职目标', companyLabel: '', roleLabel: '', targetLinked: false, targetId: null, readiness: [], nextAction: null }),
    })

    expect(wrapper.get('[data-test="agenda-pane"]').text()).toContain('近期没有安排')
    expect(wrapper.get('[data-test="recent-activity"]').text()).toContain('还没有模拟面试记录')
    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('还没有求职目标')
    await wrapper.get('[data-test="detail-empty-action"]').trigger('click')
    expect(wrapper.emitted('action')).toEqual([['add-job']])
  })

  it('shows the target fallback detail when no arrangement is selected', () => {
    const wrapper = mountDashboard({
      detail: makeDetail({ kind: 'target', note: '当前没有选中的安排', targetLinked: false }),
    })

    expect(wrapper.get('[data-test="detail-empty"]').text()).toContain('当前没有选中的安排')
    expect(wrapper.find('[data-test="detail-identity"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="readiness-resume"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="detail-next"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="target-detail"]').exists()).toBe(true)
  })

  it('starts the split rail at 330px, clamps with arrow keys, and resets on double click', async () => {
    const wrapper = mountDashboard({ agendaEvents: [makeEvent()], selectedEventId: '11' })
    const separator = wrapper.get('[data-test="pane-separator"]')

    expect(separator.attributes('role')).toBe('separator')
    expect(separator.attributes('aria-valuenow')).toBe('330')
    expect(wrapper.get('.master-grid').attributes('style')).toContain('330px')

    // 方向键微调 8px/步：往下推到 270 夹紧
    for (let i = 0; i < 20; i += 1) {
      await separator.trigger('keydown', { key: 'ArrowLeft' })
    }
    expect(separator.attributes('aria-valuenow')).toBe('270')

    // 反向推到 420 夹紧
    for (let i = 0; i < 20; i += 1) {
      await separator.trigger('keydown', { key: 'ArrowRight' })
    }
    expect(separator.attributes('aria-valuenow')).toBe('420')

    // 双击恢复默认 330
    await separator.trigger('dblclick')
    expect(separator.attributes('aria-valuenow')).toBe('330')
    expect(wrapper.get('.master-grid').attributes('style')).toContain('330px')
  })

  it('switches to compact inline rows below 290px and restores the day milestones on reset', async () => {
    const events: AgendaEventView[] = [
      makeEvent({ id: '1', title: '笔试', timeLabel: '09:50', relativeLabel: '今天', companyLabel: '字节跳动', roleLabel: '后端开发实习', dayLabel: '' }),
      makeEvent({ id: '2', title: '技术二面', timeLabel: '14:30', relativeLabel: '2 天后', companyLabel: '阿里巴巴', roleLabel: '后端开发', dayLabel: '8月22日' }),
    ]
    const wrapper = mountDashboard({ agendaEvents: events, selectedEventId: '1' })
    expect(wrapper.find('.tl-milestone').exists()).toBe(true)

    const separator = wrapper.get('[data-test="pane-separator"]')
    for (let i = 0; i < 6; i += 1) {
      await separator.trigger('keydown', { key: 'ArrowLeft' })
    }
    expect(separator.attributes('aria-valuenow')).toBe('282')
    expect(wrapper.get('.agenda-timeline').classes()).toContain('compact')
    expect(wrapper.find('.tl-milestone').exists()).toBe(false)

    const rows = wrapper.findAll('[data-test="agenda-row"]')
    expect(rows[0].find('.row-inline-time').text()).toBe('09:50')
    expect(rows[1].find('.row-inline-time').text()).toBe('8月22日')
    expect(rows[0].find('.compact-dot').classes()).toContain('selected')
    expect(rows[1].find('.compact-dot').classes()).not.toContain('selected')

    await separator.trigger('dblclick')
    expect(wrapper.get('.agenda-timeline').classes()).not.toContain('compact')
    expect(wrapper.find('.tl-milestone').exists()).toBe(true)
  })

  it('renders the linked target as company · role without duplicating the role from the target name', () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent()],
      selectedEventId: '11',
      detail: makeDetail({ companyLabel: '腾讯', roleLabel: 'Java 后端实习', targetName: 'Java 后端实习' }),
    })
    expect(wrapper.get('[data-test="detail-linked-target"] .linked-target-copy').text()).toBe('腾讯 · Java 后端实习')
  })

  it('falls back to the target name without duplicating the role when the company is unknown', () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent({ companyLabel: '' })],
      selectedEventId: '11',
      detail: makeDetail({ companyLabel: '', roleLabel: 'Java 后端实习', targetName: 'Java 后端实习' }),
    })
    expect(wrapper.get('[data-test="detail-linked-target"] .linked-target-copy').text()).toBe('Java 后端实习')
  })

  it('appends the role only when the target name does not already contain it', () => {
    const wrapper = mountDashboard({
      agendaEvents: [makeEvent({ companyLabel: '' })],
      selectedEventId: '11',
      detail: makeDetail({ companyLabel: '', roleLabel: 'Java 后端实习', targetName: '新求职目标' }),
    })
    expect(wrapper.get('[data-test="detail-linked-target"] .linked-target-copy').text()).toBe('新求职目标 · Java 后端实习')
  })

  it('lists the latest recent activity at the bottom of the detail pane', async () => {
    const activity: RecentActivityItem[] = [
      { id: 9, dateLabel: '8月18日', companyLabel: '腾讯', title: '技术面', summary: '技术思路完整，但关键判断缺少量化依据。', targetId: 3 },
    ]
    const wrapper = mountDashboard({ recentActivity: activity })

    const block = wrapper.get('[data-test="recent-activity"]')
    expect(block.text()).toContain('8月18日')
    expect(block.text()).toContain('腾讯 · 技术面')
    expect(block.text()).toContain('技术思路完整')
    expect(block.text()).toContain('查看完整报告')
    await wrapper.get('[data-test="activity-row"] .activity-report').trigger('click')
    expect(wrapper.emitted('action')).toEqual([['open-feedback', 3]])
  })
})
