package com.skala.argos.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e, HttpServletRequest req) {
        return ResponseEntity.status(e.status())
                .body(ErrorResponse.of(e.status().value(), e.code(), e.getMessage(),
                        req.getRequestURI(), e.fieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldErrorItem> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldErrorItem(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "VALIDATION_FAILED",
                        "필수 항목이 누락되었거나 형식이 올바르지 않습니다.", req.getRequestURI(), errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e,
                                                          HttpServletRequest req) {
        return ResponseEntity.status(413)
                .body(ErrorResponse.of(413, "FILE_TOO_LARGE",
                        "파일당 최대 10MB까지 업로드할 수 있습니다.", req.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(500, "INTERNAL_ERROR",
                        "서버 오류가 발생했습니다.", req.getRequestURI(), null));
    }
}
