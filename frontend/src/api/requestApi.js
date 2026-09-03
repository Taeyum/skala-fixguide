import client from './client'
import {
  normalizeRequestSummary,
  normalizeRequestDetail,
  normalizePhotos,
  requestPayloadToApi,
  statusToApi,
  unwrapList,
} from './normalize'
import { STATUS_TO_API } from '@/constants/workRequest'

/**
 * 부품 교체 요청 API (API 명세서 5.5 ~ 5.10, 5.14)
 * 응답은 normalize.js 에서 뷰가 기대하는 형태로 변환한다.
 */
export const requestApi = {
  /** GET /work-requests?mine=&status=&page=&size=&sort= */
  list(params = {}) {
    const q = { ...params }
    if (q.status) {
      q.status = String(q.status)
        .split(',')
        .map((s) => STATUS_TO_API[s.trim()] ?? s.trim())
        .join(',')
    }
    return client.get('/work-requests', { params: q }).then((res) => {
      const content = unwrapList(res.data).map(normalizeRequestSummary)
      return { ...res, data: { content, page: res.data?.page } }
    })
  },

  /** GET /work-requests/{id} */
  get(id) {
    return client.get(`/work-requests/${id}`).then((res) => ({
      ...res,
      data: normalizeRequestDetail(res.data),
    }))
  },

  /** POST /work-requests — 항상 draft 로 생성하고 상태 전이는 서버(제출·AI 실행)가 맡는다 */
  create(payload) {
    return client.post('/work-requests', { ...requestPayloadToApi(payload), draft: true })
  },

  /** PATCH /work-requests/{id} — 보낸 필드만 반영 */
  update(id, payload) {
    return client.patch(`/work-requests/${id}`, requestPayloadToApi(payload))
  },

  /** POST /work-requests/{id}/photos — multipart, 파트명 files (여러 장 가능) */
  uploadPhoto(id, file) {
    const fd = new FormData()
    const files = Array.isArray(file) ? file : [file]
    files.forEach((f) => fd.append('files', f))
    return client.post(`/work-requests/${id}/photos`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /** GET /work-requests/{id}/photos */
  photos(id) {
    return client
      .get(`/work-requests/${id}/photos`)
      .then((res) => ({ ...res, data: normalizePhotos(res.data) }))
  },

  /** PATCH /work-requests/{id}/submit-approval — engineerNote 선택 */
  submitApproval(id, engineerNote) {
    const body = engineerNote ? { engineerNote } : undefined
    return client.patch(`/work-requests/${id}/submit-approval`, body)
  },
}

export { statusToApi }
