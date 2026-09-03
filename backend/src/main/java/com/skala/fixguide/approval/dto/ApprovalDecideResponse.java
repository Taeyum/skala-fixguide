package com.skala.fixguide.approval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApprovalDecideResponse(
        UUID approvalId,
        UUID workRequestId,
        ApprovalDecision decision,
        String reason,
        String reasonCategory,
        WorkRequestStatus resultStatus,
        String decidedBy,
        OffsetDateTime decidedAt) {
}
