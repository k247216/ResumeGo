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

    for (const label of ['工作台', '求职目标', '简历', '模拟面试', '日程']) {
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
})
