/**
 * work_request 도메인 상수 (WRA_SCREENS_v2 · 1.2 / 1.4)
 */

/**
 * work_request.status — 프론트 내부 표준값.
 * 백엔드(API 명세서 2.2)는 DRAFT · AI_RUNNING · AI_DONE · PENDING · APPROVED · REJECTED 를 쓰며
 * api 레이어(normalize.js)에서 아래 표준값으로 매핑한다.
 */
export const STATUS = {
  DRAFT: 'DRAFT',
  RUNNING: 'RUNNING',
  DONE: 'DONE',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
}

export const STATUS_META = {
  DRAFT: { label: '작성 중', tone: 'draft' },
  RUNNING: { label: 'AI 검증중', tone: 'running' },
  DONE: { label: '결과 확인 대기', tone: 'running' },
  PENDING: { label: '승인 대기', tone: 'pending' },
  APPROVED: { label: '승인됨', tone: 'approved' },
  REJECTED: { label: '거절·보완', tone: 'rejected' },
}

/** 백엔드 status → 프론트 표준 status */
export const STATUS_FROM_API = {
  DRAFT: 'DRAFT',
  AI_RUNNING: 'RUNNING',
  AI_DONE: 'DONE',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
}

/** 프론트 표준 status → 백엔드 status (목록 필터 쿼리용) */
export const STATUS_TO_API = {
  DRAFT: 'DRAFT',
  RUNNING: 'AI_RUNNING',
  DONE: 'AI_DONE',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
}

/**
 * 제품 유형 — 프론트 내부 표준값(소문자) ↔ 백엔드 enum(API 명세서 2.3).
 * 스펙 키도 프론트(snake) ↔ 백엔드(camel) 로 다르다.
 */
export const PRODUCT_TYPE_FROM_API = {
  VALVE: 'valve',
  FITTING_TUBE: 'fitting',
  REGULATOR: 'regulator',
  FILTER: 'filter',
  ETC: 'etc',
}

export const PRODUCT_TYPE_TO_API = {
  valve: 'VALVE',
  fitting: 'FITTING_TUBE',
  regulator: 'REGULATOR',
  filter: 'FILTER',
  etc: 'ETC',
}

/** spec_json 키: 프론트(constants) → 백엔드 필수 키 */
export const SPEC_KEY_TO_API = {
  pressure_rating: 'pressureRating',
  connection_spec: 'connectionStandard',
  material: 'material',
  substance_type: 'substanceType',
  free_spec: 'freeSpec',
}

export const SPEC_KEY_FROM_API = Object.fromEntries(
  Object.entries(SPEC_KEY_TO_API).map(([fe, be]) => [be, fe]),
)

/** 에이전트 스텝 상태 */
export const STEP_STATUS = {
  WAITING: 'WAITING',
  RUNNING: 'RUNNING',
  DONE: 'DONE',
}

/** AI 에이전트 3종 */
export const AGENTS = [
  { code: 'A1', title: '규격·호환', desc: '입력 스펙 기반 규격 및 호환성 검토', icon: 'tune' },
  { code: 'A2', title: '법령·조문', desc: '적용 법령 및 조문 검토', icon: 'gavel' },
  { code: 'A3', title: '안전서류', desc: '작업허가서·위험성평가 초안 검토', icon: 'verified_user' },
]

/**
 * 제품 유형 → 동적 스펙 필드 (WRA_SCREENS_v2 · 1.4)
 * key 는 spec_json 에 저장되는 필드명
 */
export const PRODUCT_TYPES = [
  {
    value: 'valve',
    label: '밸브',
    fields: [{ key: 'pressure_rating', label: '압력 등급', placeholder: '예: 3000 psig' }],
  },
  {
    value: 'fitting',
    label: '피팅·튜브',
    fields: [
      { key: 'connection_spec', label: '연결 규격', placeholder: '예: 1/4" VCR' },
      { key: 'material', label: '재질', placeholder: '예: 316L VIM/VAR' },
    ],
  },
  {
    value: 'regulator',
    label: '레귤레이터',
    fields: [{ key: 'pressure_rating', label: '압력 등급', placeholder: '예: 0~10 bar' }],
  },
  {
    value: 'filter',
    label: '필터',
    fields: [{ key: 'substance_type', label: '물질 종류', placeholder: '예: N2 / 부식성 가스' }],
  },
  {
    value: 'etc',
    label: '기타',
    fields: [{ key: 'free_spec', label: '자유 스펙', placeholder: '커스텀 스펙 입력', textarea: true }],
  },
]

export function productTypeLabel(value) {
  return PRODUCT_TYPES.find((t) => t.value === value)?.label ?? value ?? '-'
}

export function specFieldsFor(value) {
  return PRODUCT_TYPES.find((t) => t.value === value)?.fields ?? []
}
