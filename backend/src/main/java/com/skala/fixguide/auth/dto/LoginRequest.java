package com.skala.fixguide.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/v1/auth/login 요청 (API 명세서 5.2) */
public record LoginRequest(
        @NotBlank(message = "must not be blank") @Email(message = "must be a well-formed email address")
        String email,
        @NotBlank(message = "must not be blank") String password) {
}
