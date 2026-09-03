/**
 * 백엔드(Spring, camelCase) 응답을 프론트 뷰가 기대하는 형태로 정규화한다.
 *
 * 프론트 뷰는 예전 추측 계약(snake_case + `id` + 소문자 enum)에 맞춰 작성돼 있고,
 * 실제 백엔드는 camelCase + `workRequestId` + 대문자 enum 을 쓴다.
 * 여기서 흡수하므로 뷰는 거의 수정하지 않는다.
 *
 * 정규화 함수는 "백엔드 형태가 감지되면 변환, 아니면 원본 그대로" 로 동작해
 * 개발용 목(mock, 이미 프론트 표준 형태) 응답에도 안전하다.
 */
import {
  STATUS_FROM_API,
  STATUS_TO_API,
  PRODUCT_TYPE_FROM_API,
  PRODUCT_TYPE_TO_API,
  SPEC_KEY_FROM_API,
  SPEC_KEY_TO_API,
} from '@/constants/workRequest'

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

// ── 공용 언래핑 ────────────────────────────────────────
export function unwrapList(data) {
  if (Array.isArray(data)) return data
  return data?.items ?? data?.content ?? data?.data ?? data?.results ?? []
}

export function unwrapOne(data) {
  return data?.data ?? data ?? null
}

/** snake_case / camelCase 를 함께 조회 */
export function pick(obj, ...keys) {
  if (!obj) return undefined
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null) return obj[k]
  }
  return undefined
}

// ── enum 매핑 ─────────────────────────────────────────
export const statusFromApi = (s) => STATUS_FROM_API[s] ?? s
export const productTypeFromApi = (t) => PRODUCT_TYPE_FROM_API[t] ?? t

export function specJsonFromApi(spec) {
  if (!spec || typeof spec !== 'object') return {}
  const out = {}
  for (const [k, v] of Object.entries(spec)) out[SPEC_KEY_FROM_API[k] ?? k] = v
  return out
}

/** 운전 조건: 백엔드는 Map(object), 뷰는 문자열로 다룬다 */
export function operatingConditionFromApi(oc) {
  if (oc == null) return ''
  if (typeof oc === 'string') return oc
  if (typeof oc === 'object') {
    return Object.entries(oc)
      .map(([k, v]) => `${k} ${v}`)
      .join(', ')
  }
  return String(oc)
}

/** /api/v1/files/... 상대경로에 origin 을 붙인다 (정적 서빙, 인증 불필요) */
export function fileUrl(u) {
  if (!u) return u
  return /^https?:|^data:/.test(u) ? u : `${API_ORIGIN}${u}`
}

export const statusToApi = (s) => STATUS_TO_API[s] ?? s

function specJsonToApi(spec) {
  if (!spec || typeof spec !== 'object') return undefined
  const out = {}
  for (const [k, v] of Object.entries(spec)) out[SPEC_KEY_TO_API[k] ?? k] = v
  return out
}

/**
 * 뷰 폼(snake_case, 소문자 enum) → 백엔드 생성/수정 요청 본문.
 * `status` 는 서버가 상태 전이를 관리하므로 보내지 않는다.
 */
export function requestPayloadToApi(form) {
  const body = {}
  const put = (be, val) => {
    if (val !== undefined && val !== '') body[be] = val
  }
  put('equipment', form.equipment)
  put('line', form.line)
  put('substance', form.substance)
  put('productName', form.product_name ?? form.productName)
  if (form.product_type ?? form.productType) {
    body.productType = PRODUCT_TYPE_TO_API[form.product_type ?? form.productType] ?? form.product_type
  }
  put('symptom', form.symptom)
  put('siteMemo', form.site_memo ?? form.siteMemo)
  put('engineerNote', form.engineer_note ?? form.engineerNote)
  const oc = form.operating_condition ?? form.operatingCondition
  if (oc !== undefined && oc !== '') {
    body.operatingCondition = typeof oc === 'string' ? { note: oc } : oc
  }
  const spec = specJsonToApi(form.spec_json ?? form.specJson)
  if (spec) body.specJson = spec
  if (form.draft !== undefined) body.draft = form.draft
  return body
}

// ── 도메인 정규화 ─────────────────────────────────────
function isBackendRequest(d) {
  return d && (d.workRequestId !== undefined || d.agentRun !== undefined)
}

/** 목록 항목 (WorkRequestSummaryResponse) */
export function normalizeRequestSummary(d) {
  if (!isBackendRequest(d)) return d
  return {
    ...d,
    id: d.workRequestId,
    status: statusFromApi(d.status),
    product_name: d.partName ?? d.productName,
    product_type: productTypeFromApi(d.productType),
    requester_name: d.requesterName,
    created_at: d.createdAt,
    submitted_at: d.submittedAt,
  }
}

/** 상세 (WorkRequestDetailResponse) */
export function normalizeRequestDetail(d) {
  if (!isBackendRequest(d)) return d
  const run = d.agentRun ?? null
  const results = (run?.results ?? []).map((r) => ({
    id: r.agentResultId,
    code: r.agentCode,
    edited: r.edited,
    // A1·A2 는 items, A3 는 documents. 뷰는 content_json.items 를 읽는다.
    content_json: { items: r.items ?? r.documents ?? [] },
  }))
  const approval = d.approval
    ? [
        {
          decision: d.approval.decision,
          reason: d.approval.reason,
          reason_category: d.approval.reasonCategory,
          decided_by: d.approval.decidedBy,
          decided_at: d.approval.decidedAt,
        },
      ]
    : []
  const lastReject = [...approval].reverse().find((a) => a.decision === 'REJECT')
  return {
    ...d,
    id: d.workRequestId,
    status: statusFromApi(d.status),
    product_name: d.productName,
    product_type: productTypeFromApi(d.productType),
    operating_condition: operatingConditionFromApi(d.operatingCondition),
    spec_json: specJsonFromApi(d.specJson),
    requester_name: d.requester?.name,
    engineer_note: d.engineerNote,
    created_at: d.submittedAt ?? d.createdAt,
    run_id: run?.runId ?? null,
    agent_results: results,
    approvals: approval,
    reject_reason: lastReject?.reason ?? null,
  }
}

/** 사진 목록 (PhotoListResponse) → [{ id, url, name }] */
export function normalizePhotos(data) {
  const list = data?.photos ?? unwrapList(data)
  return list.map((p) => {
    if (p.photoId === undefined && p.id !== undefined) return p // 이미 프론트 형태(mock)
    return {
      id: p.photoId ?? p.id,
      url: fileUrl(p.originalUrl ?? p.thumbnailUrl ?? p.url),
      thumbnail: fileUrl(p.thumbnailUrl ?? p.originalUrl ?? p.url),
      name: p.fileName ?? p.name,
    }
  })
}

/** 대시보드 요약 (DashboardSummaryResponse) — kpi 중첩·거절 TOP5 를 뷰 형태로 평탄화 */
export function normalizeDashboard(data) {
  const d = unwrapOne(data)
  if (!d || d.kpi === undefined) return d // 이미 평탄한 형태(mock)
  const kpi = d.kpi ?? {}
  return {
    ...kpi,
    // 엔지니어: draft/aiRunning/pending/rejected → 뷰는 draft/running/pending/rejected
    running: kpi.aiRunning ?? kpi.running ?? 0,
    // 안전관리자: processedToday/approvedThisMonth/rejectedThisMonth
    today_processed: kpi.processedToday ?? 0,
    month_approved: kpi.approvedThisMonth ?? 0,
    month_rejected: kpi.rejectedThisMonth ?? 0,
    reject_top5: (d.rejectReasonTop5 ?? []).map((r) => ({
      reason: r.category,
      count: r.count,
    })),
  }
}

/** 폴링 응답 (AgentRunPollResponse) → { steps:[{code,status,result}], overall_status, allDone } */
export function normalizeAgentRun(data) {
  const d = unwrapOne(data)
  if (!d || (d.steps === undefined && d.agent_steps === undefined)) return d
  const rawSteps = d.steps ?? d.agent_steps ?? []
  return {
    ...d,
    overall_status: d.status ?? d.overall_status ?? d.overallStatus ?? null,
    allDone: d.allDone ?? d.all_done ?? false,
    steps: rawSteps.map((s) => ({
      code: (s.agentCode ?? s.step ?? s.code ?? '').toUpperCase(),
      status: s.status,
      message: s.message,
      resultId: s.agentResultId ?? s.result_id ?? null,
      result: s.result ?? (s.message ? { summary: s.message } : null),
    })),
  }
}
