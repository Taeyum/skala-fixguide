package com.skala.fixguide.workrequest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** API 명세서 2.2 WorkRequestStatus */
@Getter
@RequiredArgsConstructor
public enum WorkRequestStatus {
    DRAFT("작성 중"),
    AI_RUNNING("AI 검증중"),
    AI_DONE("결과 확인 대기"),
    PENDING("승인 대기"),
    APPROVED("승인"),
    REJECTED("거절·보완");

    private final String label;

    /** 안전관리자에게 노출되는 범위 — 제출(PENDING) 이후 상태만 보인다. */
    public boolean visibleToSafetyManager() {
        return this == PENDING || this == APPROVED || this == REJECTED;
    }
}
