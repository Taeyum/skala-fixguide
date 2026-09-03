package com.skala.argos.common;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ErrorResponse.FieldErrorItem> fieldErrors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message,
                        List<ErrorResponse.FieldErrorItem> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<ErrorResponse.FieldErrorItem> fieldErrors() {
        return fieldErrors;
    }
}
