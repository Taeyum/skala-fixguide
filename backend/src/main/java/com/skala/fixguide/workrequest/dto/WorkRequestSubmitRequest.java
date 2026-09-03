package com.skala.fixguide.workrequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 5.14 PATCH /work-requests/{id}/submit-approval */
public record WorkRequestSubmitRequest(
        @Schema(description = "엔지니어 설명. 비우면 기존 저장값을 쓰고, 둘 다 없으면 422",
                example = "동일 사양 정품으로 교체 요청") String engineerNote) {
}
