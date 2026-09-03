import client from './client'
import { normalizeDashboard } from './normalize'

/**
 * 대시보드 KPI (API 명세서 5.4) — GET /dashboard/summary?role=engineer|safety
 * role 쿼리는 토큰 역할과 일치해야 한다.
 */
export const dashboardApi = {
  summary(role) {
    return client
      .get('/dashboard/summary', { params: { role } })
      .then((res) => ({ ...res, data: normalizeDashboard(res.data) }))
  },
}
