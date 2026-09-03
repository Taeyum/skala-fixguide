package com.skala.fixguide.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 명세서 2.1 Role. redirectPath 는 로그인 응답의 역할 분기(AC 0-2)에 그대로 사용된다.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    ENGINEER("엔지니어", "/home"),
    SAFETY_MANAGER("안전관리자", "/manage/requests");

    private final String label;
    private final String redirectPath;

    /** GET /dashboard/summary?role=engineer|safety 의 쿼리 값 매핑 */
    public static Role fromDashboardParam(String param) {
        if (param == null) {
            return null;
        }
        return switch (param.toLowerCase()) {
            case "engineer" -> ENGINEER;
            case "safety" -> SAFETY_MANAGER;
            default -> null;
        };
    }
}
