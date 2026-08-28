import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it } from 'vitest'
import DesktopShell from './DesktopShell.vue'

function createShellRouter() {
  const empty = { template: '<div />' }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'workbench', component: empty },
      { path: '/targets', name: 'targets', component: empty },
      { path: '/resumes', name: 'resumes', component: empty },
      { path: '/knowledge', name: 'knowledge', component: empty },
      { path: '/interview', name: 'interview', component: empty },
      { path: '/schedule', name: 'schedule', component: empty },
      { path: '/settings', name: 'settings', component: empty },
    ],
  })
  return router
}

describe('DesktopShell', () => {
  beforeEach(() => {
    localStorage.clear()
    delete document.body.dataset.theme
  })

  it('renders the compact desktop tool rail without ability evidence as a primary entry', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(DesktopShell, {
      global: { plugins: [router] },
      slots: { default: '<main data-test="route-content">内容</main>' },
    })

    for (const label of ['工作台', '求职目标', '简历', '知识库', '模拟面试', '日程']) {
      wrapper.get(`[aria-label="${label}"]`)
    }
    wrapper.get('[aria-label="设置"]')
    wrapper.get('[aria-label="切换夜间模式"]')
    wrapper.get('[aria-label="本地用户状态"]')
    expect(wrapper.get('[data-test="route-content"]').text()).toBe('内容')
    expect(wrapper.text()).not.toContain('能力证据')
    expect(wrapper.text()).not.toContain('岗位探索')
    expect(wrapper.find('[aria-label="通知"]').exists()).toBe(false)
    expect(wrapper.find('[aria-label="资源中心"]').exists()).toBe(false)
    expect(wrapper.get('[data-nav="knowledge"]').attributes('data-icon')).toBe('Book')
  })

  it('keeps launch and elapsed-usage status exclusive to the packaged desktop shell', async () => {
    window.resumeGoDesktop = {} as typeof window.resumeGoDesktop
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    expect(wrapper.get('[data-test="desktop-usage-status"]').text()).toContain('1 次登录')
    expect(wrapper.get('[data-test="desktop-usage-status"]').text()).toContain('累计 1 分钟')
    delete window.resumeGoDesktop
  })

  it('switches the application canvas between light and dark themes', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    await wrapper.get('[aria-label="切换夜间模式"]').trigger('click')

    expect(wrapper.get('.desktop-shell').classes()).toContain('is-dark')
    expect(wrapper.get('.desktop-shell').attributes('data-theme')).toBe('dark')
    wrapper.get('[aria-label="切换日间模式"]')
  })

  it('persists the chosen theme and mirrors it on the document body', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    await wrapper.get('[aria-label="切换夜间模式"]').trigger('click')
    expect(localStorage.getItem('resumego:theme')).toBe('dark')
    expect(document.body.dataset.theme).toBe('dark')

    await wrapper.get('[aria-label="切换日间模式"]').trigger('click')
    expect(localStorage.getItem('resumego:theme')).toBe('light')
    expect(document.body.dataset.theme).toBe('light')
  })

  it('restores a persisted dark theme on startup', async () => {
    localStorage.setItem('resumego:theme', 'dark')
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    expect(wrapper.get('.desktop-shell').classes()).toContain('is-dark')
    expect(wrapper.get('.desktop-shell').attributes('data-theme')).toBe('dark')
    expect(document.body.dataset.theme).toBe('dark')
    wrapper.get('[aria-label="切换日间模式"]')
  })

  it('opens a monthly local usage view and reveals daily minutes after selecting a day', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    await wrapper.get('[aria-label="本地用户状态"]').trigger('click')

    wrapper.get('[data-test="usage-month-label"]')
    expect(wrapper.findAll('[data-test^="usage-day-"]').length).toBeGreaterThan(28)
    const day = wrapper.find('[data-test^="usage-day-"]')
    await day.trigger('mouseenter')

    expect(day.attributes('title')).toMatch(/\d+ 分钟|\d+ 小时/)
    expect(wrapper.get('[data-test="heatmap-tooltip"]').text()).toMatch(/\d+ 分钟|\d+ 小时/)
    expect(wrapper.find('[data-test="daily-usage-detail"]').exists()).toBe(false)
  })

  it('exposes month navigation and an image upload control in the profile dialog', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    await wrapper.get('[aria-label="本地用户状态"]').trigger('click')

    expect(wrapper.get('[data-test="avatar-upload"]')).toBeTruthy()
    expect(wrapper.find('.avatar-upload').text()).not.toContain('上传')
    await wrapper.get('[data-test="usage-month-prev"]').trigger('click')
    expect(wrapper.get('[data-test="usage-month-label"]').text()).not.toBe('')
    await wrapper.get('[data-test="usage-month-next"]').trigger('click')
  })

  it('stores an uploaded image as a local avatar after saving the profile', async () => {
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })
    await wrapper.get('[aria-label="本地用户状态"]').trigger('click')

    const input = wrapper.get('[data-test="avatar-upload"]')
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' })
    Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
    await input.trigger('change')
    await new Promise((resolve) => setTimeout(resolve, 50))
    await wrapper.get('[data-test="profile-save"]').trigger('click')

    expect(JSON.parse(localStorage.getItem('resumego:local-profile') ?? '{}').avatar).toMatch(/^data:image\/png;base64,/)
  })

  it('keeps the rail avatar usable when a persisted image cannot be decoded', async () => {
    localStorage.setItem('resumego:local-profile', JSON.stringify({
      name: '本地用户', avatar: 'data:image/png;base64,broken',
      createdAt: '2026-08-27T09:00:00.000Z', lastUsedAt: '2026-08-27T09:00:00.000Z',
      usageDates: [], usageMinutesByDate: {},
    }))
    const router = createShellRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(DesktopShell, { global: { plugins: [router] } })

    await wrapper.get('.desktop-user-avatar img').trigger('error')

    expect(wrapper.find('.desktop-user-avatar img').exists()).toBe(false)
    expect(wrapper.get('.desktop-user-avatar').text()).toBe('林')
  })
})
