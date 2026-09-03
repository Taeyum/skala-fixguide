package com.skala.fixguide.approval.dto;

import com.skala.fixguide.approval.entity.ApprovalDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** 5.15 POST /approvals — REJECT 시 reason 필수(10자 이상) */
@Schema(description = "안전관리자 전용. PENDING 상태의 요청만 결재할 수 있다.",
        example = """
                {
                  "workRequestId": "00000000-0000-0000-0000-000000000000",
                  "decision": "APPROVE"
                }""")
public record ApprovalDecideRequest(
        @Schema(description = "결재할 요청서 id") @NotNull(message = "must not be null") UUID workRequestId,
        @NotNull(message = "must not be null") ApprovalDecision decision,
        @Schema(description = "반려 사유. REJECT 일 때 필수(10자 이상)") String reason,
        @Schema(description = "반려 사유 분류") String reasonCategory) {
}
