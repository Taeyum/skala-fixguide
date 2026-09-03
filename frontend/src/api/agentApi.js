import client from './client'
import { normalizeAgentRun } from './normalize'

/**
 * AI 에이전트 검증 API (API 명세서 5.11 ~ 5.13)
 */
export const agentApi = {
  /** POST /agent-runs { workRequestId } → 202 { runId, steps, ... } */
  startRun(requestId) {
    return client.post('/agent-runs', { workRequestId: requestId }).then((res) => ({
      ...res,
      data: { ...res.data, run_id: res.data?.runId ?? res.data?.run_id },
    }))
  },

  /** GET /agent-runs/{runId} → { steps:[{code,status,result}], overall_status, allDone } */
  getRun(runId) {
    return client
      .get(`/agent-runs/${runId}`)
      .then((res) => ({ ...res, data: normalizeAgentRun(res.data) }))
  },

  /**
   * PATCH /agent-results/{id} — A1·A2 는 { items }, A3 는 { documents } 전체 치환.
   * @param {{items?: any[], documents?: any[]}} body
   */
  updateResult(resultId, body) {
    return client.patch(`/agent-results/${resultId}`, body)
  },
}
