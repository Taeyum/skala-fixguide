package com.skala.argos.dto;

import com.skala.argos.domain.ApprovalDecision;
import com.skala.argos.domain.WorkRequestStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API 15 (approvals) 요청·응답 DTO */
public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    /** 5.15 POST /approvals — REJECT 시 reason 필수(10자 이상) */
    public record DecideRequest(
            @NotNull(message = "must not be null") UUID workRequestId,
            @NotNull(message = "must not be null") ApprovalDecision decision,
            String reason,
            String reasonCategory
    ) {
    }

    public record DecideResponse(UUID approvalId, UUID workRequestId, ApprovalDecision decision,
                                 String reason, String reasonCategory, WorkRequestStatus resultStatus,
                                 String decidedBy, OffsetDateTime decidedAt) {
    }
}
