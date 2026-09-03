package com.skala.argos.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/** 명세 1.1 공통 에러 응답. fieldErrors는 입력 유효성 오류(400·422)에서만 포함 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors
) {

    public record FieldErrorItem(String field, String reason) {
    }

    public static ErrorResponse of(int status, String code, String message, String path,
                                   List<FieldErrorItem> fieldErrors) {
        return new ErrorResponse(KstTime.now(), status, code, message, path,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
