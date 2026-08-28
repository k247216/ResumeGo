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
      meta: { fill: true },
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('../views/knowledge/KnowledgeLibraryView.vue'),
      meta: { fill: true },
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
      component: () => import('../views/evidence/EvidenceLibraryView.vue'),
    },
    {
      path: '/interview',
      name: 'interview',
      component: () => import('../views/InterviewView.vue'),
      meta: { fill: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/settings/SettingsView.vue'),
      meta: { fill: true },
    },
    {
      path: '/schedule',
      name: 'schedule',
      component: () => import('../views/schedule/ScheduleView.vue'),
      meta: { fill: true },
    },
    {
      path: '/resumes',
      name: 'resumes',
      component: () => import('../views/resumes/ResumeLibraryView.vue'),
      meta: { fill: true },
    },
  ],
})

export default router
