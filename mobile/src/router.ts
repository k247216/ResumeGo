import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/schedule' },
    { path: '/targets', name: 'targets', component: () => import('./views/TargetsView.vue'), meta: { tab: 'targets' } },
    { path: '/schedule', name: 'schedule', component: () => import('./views/ScheduleView.vue'), meta: { tab: 'schedule' } },
    { path: '/resumes', name: 'resumes', component: () => import('./views/ResumesView.vue'), meta: { tab: 'resumes' } },
    { path: '/resumes/:id', name: 'resume-detail', component: () => import('./views/ResumeDetailView.vue') },
    { path: '/me', name: 'me', component: () => import('./views/MeView.vue'), meta: { tab: 'me' } },
  ],
})

export default router
