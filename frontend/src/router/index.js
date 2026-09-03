import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const routes = [
  {
    path: '/',
    redirect: () => {
      const auth = useAuthStore()
      return auth.isAuthenticated ? auth.homeRoute() : '/login'
    },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, layout: 'auth' },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/auth/SignupView.vue'),
    meta: { public: true, layout: 'auth' },
  },

  // ── 엔지니어 (WRA_E_*) ──────────────────────────────
  {
    path: '/home',
    name: 'engineer-home',
    component: () => import('@/views/engineer/EngineerHomeView.vue'),
    meta: { role: 'engineer', layout: 'app' },
  },
  {
    path: '/requests/new',
    name: 'request-create',
    component: () => import('@/views/engineer/RequestCreateView.vue'),
    meta: { role: 'engineer', layout: 'app' },
  },
  {
    path: '/requests/:id/run',
    name: 'agent-run',
    component: () => import('@/views/engineer/AgentRunProgressView.vue'),
    meta: { role: 'engineer', layout: 'app' },
    props: true,
  },
  {
    path: '/requests/:id/result',
    name: 'agent-result',
    component: () => import('@/views/engineer/AgentResultEditView.vue'),
    meta: { role: 'engineer', layout: 'app' },
    props: true,
  },
  {
    path: '/my/requests',
    name: 'my-requests',
    component: () => import('@/views/engineer/MyRequestListView.vue'),
    meta: { role: 'engineer', layout: 'app' },
  },

  // ── 안전관리자 (WRA_S_*) ────────────────────────────
  {
    path: '/manage/requests',
    name: 'safety-manage',
    component: () => import('@/views/safety/SafetyManageView.vue'),
    meta: { role: 'safety', layout: 'app' },
  },
  {
    path: '/manage/requests/:id',
    name: 'safety-detail',
    component: () => import('@/views/safety/SafetyDetailView.vue'),
    meta: { role: 'safety', layout: 'app' },
    props: true,
  },

  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * Navigation Guard (WRA_SCREENS_v2 · 1.1)
 * 1) 인증 검사: public 아닌 경로 + 미인증 → /login?redirect=
 * 2) 로그인 상태로 공개(auth) 경로 진입 → 역할 홈으로
 * 3) 역할 가드: meta.role 불일치 → 본인 역할 홈으로
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isAuthenticated && (to.name === 'login' || to.name === 'signup')) {
      return auth.homeRoute()
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.role && auth.role !== to.meta.role) {
    return auth.homeRoute()
  }

  return true
})

export default router
