import client from './client'

/**
 * 승인/거절 API (API 명세서 5.15) — POST /approvals
 */
export const approvalApi = {
  /**
   * @param {string} requestId work_request id
   * @param {'APPROVE'|'REJECT'} decision
   * @param {string} [reason] REJECT 시 필수(10자 이상)
   * @param {string} [reasonCategory] 반려 사유 분류
   */
  decide(requestId, decision, reason, reasonCategory) {
    return client.post('/approvals', {
      workRequestId: requestId,
      decision,
      reason: reason || undefined,
      reasonCategory: reasonCategory || undefined,
    })
  },
}
