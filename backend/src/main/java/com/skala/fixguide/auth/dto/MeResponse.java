package com.skala.fixguide.auth.dto;

import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.util.UUID;

/** GET /api/v1/auth/me 200 응답 (API 명세서 5.3 · 정합성 메모 #3) */
public record MeResponse(UUID userId, String name, String email, Role role) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
