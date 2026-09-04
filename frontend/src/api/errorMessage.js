/**
 * 백엔드 공통 에러 응답(ErrorResponse: { code, message, fieldErrors:[{field,reason}] })을
 * 사용자에게 보여줄 한 줄 메시지로 변환한다.
 */

/** 백엔드 에러 코드 → 사용자 문구 (없으면 서버 message 를 그대로 쓴다) */
const CODE_MESSAGE = {
  VALIDATION_FAILED: '입력값을 확인해 주세요.',
  PASSWORD_MISMATCH: '비밀번호와 비밀번호 확인이 일치하지 않습니다.',
  EMAIL_ALREADY_EXISTS: '이미 가입된 이메일입니다. 로그인해 주세요.',
  INVALID_CREDENTIALS: '이메일 또는 비밀번호가 올바르지 않습니다.',
  SPEC_SCHEMA_MISMATCH: '제품 유형과 스펙 항목이 일치하지 않습니다.',
  REJECT_REASON_REQUIRED: '거절 사유는 10자 이상 입력해 주세요.',
  WORK_REQUEST_INCOMPLETE: 'AI 검증에 필요한 필수값이 누락되었습니다.',
  SUBMIT_REQUIRED_FIELD_MISSING: '제출에 필요한 항목이 누락되었습니다.',
  IMMUTABLE_STATUS: '현재 상태에서는 수정할 수 없습니다.',
  RESULT_LOCKED: '제출·승인 이후에는 결과를 수정할 수 없습니다.',
  ALREADY_DECIDED: '이미 처리된 요청입니다.',
  RUN_ALREADY_IN_PROGRESS: '이미 진행 중인 AI 검증이 있습니다.',
  PHOTO_LIMIT_EXCEEDED: '사진은 요청당 최대 5장까지 업로드할 수 있습니다.',
  FILE_TOO_LARGE: '파일 용량이 너무 큽니다. (파일당 10MB)',
  UNSUPPORTED_FILE_TYPE: '지원하지 않는 파일 형식입니다.',
}

/** 필드명 → 한글 라벨 */
const FIELD_LABEL = {
  name: '성명',
  email: '이메일',
  password: '비밀번호',
  passwordConfirm: '비밀번호 확인',
  role: '역할',
  reason: '사유',
  decision: '결정',
  workRequestId: '요청',
  sort: '정렬',
}

/**
 * @param {unknown} err  axios 에러
 * @param {string} fallback  응답 본문이 없을 때 쓸 기본 문구
 * @returns {string}
 */
export function apiErrorMessage(err, fallback = '요청 처리 중 오류가 발생했습니다.') {
  const data = err?.response?.data
  if (!data) {
    // 네트워크 단절 등 응답 자체가 없는 경우
    if (err?.code === 'ERR_NETWORK') return '서버에 연결할 수 없습니다. 백엔드 상태를 확인해 주세요.'
    return fallback
  }

  const fieldErrors = Array.isArray(data.fieldErrors) ? data.fieldErrors : []
  if (fieldErrors.length) {
    const parts = fieldErrors.map((fe) => {
      const label = FIELD_LABEL[fe.field] ?? fe.field
      return `${label}: ${fe.reason}`
    })
    return parts.join(' / ')
  }

  return CODE_MESSAGE[data.code] ?? data.message ?? fallback
}
