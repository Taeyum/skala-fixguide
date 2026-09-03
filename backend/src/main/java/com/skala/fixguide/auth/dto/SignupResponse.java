package com.skala.fixguide.auth.dto;

import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.time.OffsetDateTime;
import java.util.UUID;

/** POST /api/v1/auth/signup 201 응답 (API 명세서 5.1) */
public record SignupResponse(
        UUID userId, String name, String email, Role role, OffsetDateTime createdAt) {

    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
