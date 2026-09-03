package com.skala.fixguide.auth.dto;

import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.util.UUID;

/** POST /api/v1/auth/login 200 응답 (API 명세서 5.2) */
public record LoginResponse(
        String accessToken, String tokenType, long expiresIn, UserSummary user, String redirectPath) {

    public record UserSummary(UUID userId, String name, Role role) {
    }

    public static LoginResponse of(String accessToken, long expiresIn, User user) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                new UserSummary(user.getId(), user.getName(), user.getRole()),
                user.getRole().getRedirectPath());
    }
}
