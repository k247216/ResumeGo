import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import DesktopShell from './DesktopShell.vue'

describe('DesktopShell', () => {
  it('renders stable local-workbench navigation without web marketplace controls', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'workbench', component: { template: '<div />' } },
        { path: '/targets', name: 'targets', component: { template: '<div />' } },
        { path: '/resumes', name: 'resumes', component: { template: '<div />' } },
        { path: '/evidences', name: 'evidences', component: { template: '<div />' } },
        { path: '/settings', name: 'settings', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(DesktopShell, {
      global: { plugins: [router] },
      slots: { default: '<main data-test="route-content">内容</main>' },
    })

    expect(wrapper.text()).toContain('ResumeGo')
    expect(wrapper.text()).toContain('数据保存在此设备')
    for (const label of ['工作台', '求职目标', '简历', '能力证据', '设置']) {
      expect(wrapper.text()).toContain(label)
    }
    expect(wrapper.get('[data-test="route-content"]').text()).toBe('内容')
    expect(wrapper.text()).not.toContain('岗位探索')
    expect(wrapper.find('[aria-label="通知"]').exists()).toBe(false)
    expect(wrapper.find('[aria-label="资源中心"]').exists()).toBe(false)
  })
})
