import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TargetListView from './TargetListView.vue'

const store = reactive({
  targets: [{ id: 1, name: '原目标', status: 'active' }, { id: 2, name: '旧目标', status: 'archived' }],
  activeTargetId: 1,
  loading: false,
  errorMessage: '',
  load: vi.fn(), retry: vi.fn(), select: vi.fn(), rename: vi.fn(), archive: vi.fn(), restore: vi.fn(),
})

vi.mock('../../stores/targets', () => ({ useTargetsStore: () => store }))
vi.mock('vue-router', async () => ({ ...(await vi.importActual<typeof import('vue-router')>('vue-router')), useRouter: () => ({ push: vi.fn() }) }))

describe('TargetListView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renames an active target inline', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: ['RouterLink'] } })
    await wrapper.get('[data-test="rename-target-1"]').trigger('click')
    await wrapper.get('[data-test="target-name-1"]').setValue('新的目标')
    await wrapper.get('[data-test="save-target-name-1"]').trigger('click')
    await flushPromises()
    expect(store.rename).toHaveBeenCalledWith(1, '新的目标')
  })

  it('offers archive for active targets and restore for archived targets', async () => {
    const wrapper = mount(TargetListView, { global: { stubs: ['RouterLink'] } })
    await wrapper.get('[data-test="archive-target-1"]').trigger('click')
    await wrapper.get('[data-test="restore-target-2"]').trigger('click')
    expect(store.archive).toHaveBeenCalledWith(1)
    expect(store.restore).toHaveBeenCalledWith(2)
  })
})
