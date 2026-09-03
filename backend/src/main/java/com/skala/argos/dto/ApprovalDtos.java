package com.skala.argos.dto;

import com.skala.argos.domain.ApprovalDecision;
import com.skala.argos.domain.WorkRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API 15 (approvals) 요청·응답 DTO */
public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    /** 5.15 POST /approvals — REJECT 시 reason 필수(10자 이상) */
    @Schema(description = "안전관리자 전용. PENDING 상태의 요청만 결재할 수 있다.",
            example = """
                    {
                      "workRequestId": "00000000-0000-0000-0000-000000000000",
                      "decision": "APPROVE"
                    }""")
    public record DecideRequest(
            @Schema(description = "결재할 요청서 id") @NotNull(message = "must not be null") UUID workRequestId,
            @NotNull(message = "must not be null") ApprovalDecision decision,
            @Schema(description = "반려 사유. REJECT일 때 필수(10자 이상)") String reason,
            @Schema(description = "반려 사유 분류") String reasonCategory
    ) {
    }

    public record DecideResponse(UUID approvalId, UUID workRequestId, ApprovalDecision decision,
                                 String reason, String reasonCategory, WorkRequestStatus resultStatus,
                                 String decidedBy, OffsetDateTime decidedAt) {
    }
}
