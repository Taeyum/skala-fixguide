import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

/**
 * 순수 HTTP 통신 계층.
 * - baseURL: VITE_API_BASE_URL (기본 http://localhost:8080)
 * - 요청 인터셉터: authStore 토큰을 Authorization 헤더로 주입
 * - 응답 인터셉터: 401 시 로그아웃 후 /login 이동
 */
const client = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/api/v1`,
  timeout: 10000,
})

// 개발용: 백엔드 없이 UI 확인 (VITE_USE_MOCK=true)
if (import.meta.env.VITE_USE_MOCK === 'true') {
  const { mockAdapter } = await import('./mock')
  client.defaults.adapter = mockAdapter
  console.info('[argus] mock API 활성화 — 실제 백엔드에 요청하지 않습니다.')
}

client.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      // 로그인 요청 자체의 401은 화면에서 처리하므로 리다이렉트하지 않음
      const isAuthCall = error.config?.url?.includes('/auth/')
      if (!isAuthCall && auth.token) {
        auth.logout()
        router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    }
    return Promise.reject(error)
  },
)

export default client
