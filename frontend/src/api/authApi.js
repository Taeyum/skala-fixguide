import client from './client'

/**
 * 인증 API (WRA_SCREENS_v2 · API 계약 1~2)
 */
export const authApi = {
  /**
   * POST /api/v1/auth/login
   * @param {{ email: string, password: string }} payload
   * @returns 200 (+role) / 401
   */
  login(payload) {
    return client.post('/auth/login', payload)
  },

  /**
   * POST /api/v1/auth/signup
   * @param {{ name: string, email: string, password: string, role: 'engineer' | 'safety' }} payload
   * @returns 201 / 409(중복)
   */
  signup(payload) {
    return client.post('/auth/signup', payload)
  },

  /**
   * POST /api/v1/auth/logout (Bearer) → 204
   * 스토어가 토큰을 비운 뒤에도 요청이 나갈 수 있도록 토큰을 명시적으로 받는다.
   */
  logout(token) {
    return client.post('/auth/logout', null, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
  },

  /** GET /api/v1/auth/me → { userId, name, email, role } */
  me() {
    return client.get('/auth/me')
  },
}
