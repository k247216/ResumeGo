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
      component: () => import('../views/targets/TargetListView.vue'),
    },
    {
      path: '/editor',
      name: 'resume-editor',
      component: () => import('../views/resumes/ResumeEditorView.vue'),
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
      component: () => import('../views/resumes/ResumeLibraryView.vue'),
    },
  ],
})

export default router
