import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'workbench',
      component: () => import('../views/workbench/WorkbenchView.vue'),
    },
    {
      path: '/home',
      name: 'home',
      redirect: { name: 'workbench' },
    },
    {
      path: '/targets',
      name: 'targets',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/editor',
      name: 'resume-editor',
      component: () => import('../views/HomeView.vue'),
      meta: { immersive: true },
    },
    {
      path: '/evidences',
      name: 'evidences',
      component: () => import('../views/EvidenceView.vue'),
    },
    {
      path: '/interview',
      name: 'interview',
      component: () => import('../views/InterviewView.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/settings/SettingsView.vue'),
    },
    {
      path: '/resumes',
      name: 'resumes',
      component: () => import('../views/ResumeView.vue'),
    },
    {
      path: '/resume-versions/:versionId/assessment',
      name: 'resume-assessment',
      component: () => import('../views/AssessmentView.vue'),
    },
    {
      path: '/jobs',
      name: 'jobs',
      component: () => import('../views/JobListView.vue'),
    },
    {
      path: '/job/create',
      name: 'job-create',
      component: () => import('../views/JobCreateView.vue'),
    },
    {
      path: '/jobs/:id',
      name: 'job-detail',
      component: () => import('../views/JobDetailView.vue'),
    },
  ],
})

export default router
