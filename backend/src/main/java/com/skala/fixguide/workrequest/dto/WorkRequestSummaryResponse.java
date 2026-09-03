package com.skala.fixguide.workrequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/** GET /api/v1/work-requests 목록 항목 (API 명세서 5.6) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkRequestSummaryResponse(
        UUID workRequestId,
        String equipment,
        String partName,
        ProductType productType,
        String productTypeLabel,
        WorkRequestStatus status,
        String statusLabel,
        String requesterName,
        OffsetDateTime submittedAt,
        NextAction nextAction) {

    public static WorkRequestSummaryResponse from(WorkRequest entity, Role viewerRole) {
        return new WorkRequestSummaryResponse(
                entity.getId(),
                entity.getEquipment(),
                entity.getProductName(),
                entity.getProductType(),
                entity.getProductType() == null ? null : entity.getProductType().getLabel(),
                entity.getStatus(),
                entity.getStatus().getLabel(),
                entity.getRequester().getName(),
                entity.getSubmittedAt(),
                NextAction.of(viewerRole, entity.getStatus(), entity.getId()));
    }
}
