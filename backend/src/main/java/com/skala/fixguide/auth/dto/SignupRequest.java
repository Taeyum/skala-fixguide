package com.skala.fixguide.auth.dto;

import com.skala.fixguide.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/signup 요청 (API 명세서 5.1)
 *
 * <p>passwordConfirm 불일치는 여기서 검증하지 않는다. 명세서가 별도 코드
 * {@code PASSWORD_MISMATCH} 를 요구하므로 서비스 계층에서 처리한다.
 */
public record SignupRequest(
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 20, message = "size must be between 2 and 20")
        String name,

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        String email,

        @NotBlank(message = "must not be blank")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "must be at least 8 characters with letter, digit and special character")
        String password,

        @NotBlank(message = "must not be blank") String passwordConfirm,

        @NotNull(message = "must not be null") Role role) {
}
