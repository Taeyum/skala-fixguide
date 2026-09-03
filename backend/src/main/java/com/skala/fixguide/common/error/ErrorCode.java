package com.skala.fixguide.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 명세서 v1.0 · 8장 "HTTP 상태 코드 · 에러 코드 정리" 를 그대로 옮긴 것.
 * 이번 브랜치에서 사용하지 않는 코드도 팀원이 이어받아 쓸 수 있도록 미리 정의해 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    SPEC_SCHEMA_MISMATCH(HttpStatus.BAD_REQUEST, "제품 유형과 스펙 항목이 일치하지 않습니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "거절 사유는 필수입니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

    // 401
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    // 명세서 8장에는 없던 코드 — 로그아웃(POST /auth/logout) 도입과 함께 추가했다.
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다. 다시 로그인해 주세요."),

    // 403
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, "해당 기능에 대한 권한이 없습니다."),
    FORBIDDEN_NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 요청만 조회할 수 있습니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    WORK_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."),
    AGENT_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 실행 이력을 찾을 수 없습니다."),

    // 409
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    RUN_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 진행 중인 AI 검증이 있습니다."),
    IMMUTABLE_STATUS(HttpStatus.CONFLICT, "현재 상태에서는 수정할 수 없습니다."),
    RESULT_LOCKED(HttpStatus.CONFLICT, "제출·승인 이후에는 결과를 수정할 수 없습니다."),
    ALREADY_DECIDED(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    NOT_PENDING(HttpStatus.CONFLICT, "승인 대기 상태의 요청이 아닙니다."),

    // 413
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 용량이 너무 큽니다."),

    // 422
    SUBMIT_REQUIRED_FIELD_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "제출에 필요한 항목이 누락되었습니다."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
