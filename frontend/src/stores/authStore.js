import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/authApi'

const STORAGE_KEY = 'argus.auth'

/** 백엔드 Role enum ↔ 프론트 표준 역할값 */
const ROLE_FROM_API = { ENGINEER: 'engineer', SAFETY_MANAGER: 'safety' }
const ROLE_TO_API = { engineer: 'ENGINEER', safety: 'SAFETY_MANAGER' }
const toRole = (r) => ROLE_FROM_API[r] ?? r

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * 백엔드 응답 스키마가 확정되지 않아 흔한 형태를 모두 수용한다.
 * 토큰: accessToken | token | data.accessToken
 * 사용자: user | data.user | 최상위 필드(role, name, email)
 */
function normalizeLoginResponse(data, fallbackEmail) {
  const token = data.accessToken ?? data.token ?? data.data?.accessToken ?? data.data?.token ?? null
  const src = data.user ?? data.data?.user ?? data
  const user = {
    id: src.id ?? src.userId ?? null,
    name: src.name ?? src.username ?? '',
    email: src.email ?? fallbackEmail ?? '',
    role: toRole(src.role ?? data.role ?? null),
  }
  return { token, user, redirectPath: data.redirectPath ?? null }
}

export const useAuthStore = defineStore('auth', () => {
  const persisted = loadPersisted()
  const token = ref(persisted?.token ?? null)
  const user = ref(persisted?.user ?? null)

  const isAuthenticated = computed(() => !!token.value)
  const role = computed(() => user.value?.role ?? null)

  function persist() {
    try {
      if (token.value) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: token.value, user: user.value }))
      } else {
        localStorage.removeItem(STORAGE_KEY)
      }
    } catch {
      /* storage 불가 환경 무시 */
    }
  }

  /** 로그인 성공 후 역할별 기본 경로 */
  function homeRoute(roleArg) {
    const r = roleArg ?? role.value
    return r === 'safety' ? '/manage/requests' : '/home'
  }

  async function login({ email, password }) {
    const { data } = await authApi.login({ email, password })
    const { token: t, user: u } = normalizeLoginResponse(data, email)
    token.value = t
    user.value = u
    persist()
    return u
  }

  async function signup(payload) {
    return authApi.signup({
      name: payload.name,
      email: payload.email,
      password: payload.password,
      passwordConfirm: payload.passwordConfirm ?? payload.password,
      role: ROLE_TO_API[payload.role] ?? payload.role,
    })
  }

  function logout() {
    // 서버 블랙리스트 등록은 best-effort (실패해도 클라이언트 세션은 정리한다).
    // 인터셉터는 비동기로 돌아 토큰을 먼저 비우면 헤더가 빠지므로, 토큰을 직접 넘긴다.
    const current = token.value
    if (current) authApi.logout(current).catch(() => {})
    token.value = null
    user.value = null
    persist()
  }

  return { token, user, isAuthenticated, role, login, signup, logout, homeRoute }
})
