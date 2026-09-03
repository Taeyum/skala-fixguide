package com.skala.argos.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 명세 2.2 WorkRequestStatus — 요청 상태머신 */
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

    /** PENDING·APPROVED 에서는 요청·AI 결과 수정 불가 (명세 5.8·5.13) */
    public boolean immutable() {
        return this == PENDING || this == APPROVED;
    }

    /** SAFETY_MANAGER 는 PENDING 이상만 조회 가능 (명세 1장 권한) */
    public boolean visibleToManager() {
        return this == PENDING || this == APPROVED || this == REJECTED;
    }
}
