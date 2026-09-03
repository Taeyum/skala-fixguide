package com.skala.fixguide.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API 명세서 1.1 공통 에러 응답 포맷. fieldErrors 는 400·422 에서만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                OffsetDateTime.now(), errorCode.getStatus().value(), errorCode.name(), message, path, null);
    }

    public static ErrorResponse of(
            ErrorCode errorCode, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                OffsetDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.name(),
                message,
                path,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
