// @vitest-environment happy-dom
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ScheduleEventDialog from './ScheduleEventDialog.vue'
import type { ScheduleEvent } from '../../types/schedule'

const editingEvent: ScheduleEvent = {
  id: 5,
  title: '腾讯技术面',
  eventType: 'interview',
  startTime: '2026-08-25T14:00:00',
  endTime: '2026-08-25T15:30:00',
  notes: '带上作品集',
  jobDescriptionId: 6,
  createdAt: '',
  updatedAt: '',
}

const jobs = [
  { id: 6, label: '腾讯 · 后端' },
  { id: 9, label: '字节 · 前端' },
]

function mountDialog(props: Record<string, unknown> = {}) {
  return mount(ScheduleEventDialog, {
    props: {
      open: true,
      editing: null,
      submitting: false,
      errorMessage: '',
      jobs,
      defaultDate: new Date(2026, 7, 25, 10, 0),
      ...props,
    },
  })
}

function inputValue(wrapper: ReturnType<typeof mountDialog>, selector: string): string {
  return (wrapper.get(selector).element as HTMLInputElement).value
}

describe('ScheduleEventDialog', () => {
  it('prefills every field from the edited event', async () => {
    const wrapper = mountDialog({ editing: editingEvent })
    await flushPromises()

    expect(inputValue(wrapper, '[data-test="event-title"]')).toBe('腾讯技术面')
    expect(inputValue(wrapper, '[data-test="event-date"]')).toBe('2026-08-25')
    expect(inputValue(wrapper, '[data-test="event-time"]')).toBe('14:00')
    expect(inputValue(wrapper, '[data-test="event-end-time"]')).toBe('15:30')
    expect(inputValue(wrapper, '[data-test="event-job"]')).toBe('6')
    expect(inputValue(wrapper, '[data-test="event-notes"]')).toBe('带上作品集')
  })

  it('defaults a new event to the provided default date', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    expect(inputValue(wrapper, '[data-test="event-date"]')).toBe('2026-08-25')
    expect(inputValue(wrapper, '[data-test="event-time"]')).toBe('10:00')
    expect(inputValue(wrapper, '[data-test="event-end-time"]')).toBe('')
    expect(inputValue(wrapper, '[data-test="event-job"]')).toBe('')
  })

  it('emits the full payload including endTime and job association', async () => {
    const wrapper = mountDialog({ editing: editingEvent })
    await flushPromises()

    await wrapper.find('form').trigger('submit')

    const [payload] = wrapper.emitted('save')!.at(-1)!
    expect(payload).toEqual({
      title: '腾讯技术面',
      eventType: 'interview',
      startTime: '2026-08-25T14:00:00',
      endTime: '2026-08-25T15:30:00',
      notes: '带上作品集',
      jobDescriptionId: 6,
    })
  })

  it('clears the association when 不关联 is selected', async () => {
    const wrapper = mountDialog({ editing: editingEvent })
    await flushPromises()

    await wrapper.get('[data-test="event-job"]').setValue('')
    await wrapper.find('form').trigger('submit')

    const [payload] = wrapper.emitted('save')!.at(-1)!
    expect((payload as { jobDescriptionId: number | null }).jobDescriptionId).toBeNull()
  })

  it('blocks submission while endTime is not after startTime', async () => {
    const wrapper = mountDialog({ editing: editingEvent })
    await flushPromises()

    await wrapper.get('[data-test="event-end-time"]').setValue('14:00')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.text()).toContain('结束时间需晚于开始时间')

    await wrapper.get('[data-test="event-end-time"]').setValue('16:00')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('save')).toHaveLength(1)
    const [payload] = wrapper.emitted('save')!.at(-1)!
    expect((payload as { endTime: string | null }).endTime).toBe('2026-08-25T16:00:00')
  })

  it('renders job options and disables actions while submitting', async () => {
    const wrapper = mountDialog({ editing: editingEvent, submitting: true })
    await flushPromises()

    const options = wrapper.findAll('[data-test="event-job"] option')
    expect(options.map((option) => option.text())).toEqual(['不关联', '腾讯 · 后端', '字节 · 前端'])

    const save = wrapper.get('[data-test="event-save"]')
    expect(save.attributes("disabled")).toBeDefined()
    expect(save.text()).toBe('保存中…')
    expect(wrapper.get('[data-test="event-delete"]').attributes("disabled")).toBeDefined()

    const idle = mountDialog({ editing: editingEvent })
    await flushPromises()
    await idle.get('[data-test="event-delete"]').trigger('click')
    expect(idle.emitted('delete')).toHaveLength(1)
  })
})
