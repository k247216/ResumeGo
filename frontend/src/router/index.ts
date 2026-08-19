import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
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
