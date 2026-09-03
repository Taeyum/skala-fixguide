package com.skala.fixguide.workrequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.RunStatus;
import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 5.7 GET /work-requests/{id} — AI 결과·승인 이력 포함 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkRequestDetailResponse(
        UUID workRequestId,
        String requestNo,
        WorkRequestStatus status,
        String statusLabel,
        RequesterView requester,
        String equipment,
        String line,
        String substance,
        Map<String, Object> operatingCondition,
        String productName,
        ProductType productType,
        String productTypeLabel,
        Map<String, Object> specJson,
        String symptom,
        String siteMemo,
        String engineerNote,
        List<PhotoResponse> photos,
        AgentRunView agentRun,
        ApprovalView approval,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime submittedAt) {

    public record RequesterView(UUID userId, String name) {
    }

    public record AgentRunView(UUID runId, RunStatus status, List<AgentResultView> results) {
    }

    /** A1·A2 는 items, A3 는 documents 만 채워진다 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AgentResultView(
            UUID agentResultId,
            AgentCode agentCode,
            String title,
            boolean editable,
            boolean edited,
            List<Map<String, Object>> items,
            List<Map<String, Object>> documents) {
    }

    public record ApprovalView(
            ApprovalDecision decision,
            String reason,
            String reasonCategory,
            String decidedBy,
            OffsetDateTime decidedAt) {

        public static ApprovalView from(Approval approval) {
            return new ApprovalView(
                    approval.getDecision(),
                    approval.getReason(),
                    approval.getReasonCategory(),
                    approval.getApprover().getName(),
                    approval.getDecidedAt());
        }
    }

    public static WorkRequestDetailResponse of(
            WorkRequest wr, List<PhotoResponse> photos, AgentRunView agentRun, ApprovalView approval) {
        return new WorkRequestDetailResponse(
                wr.getId(),
                wr.getRequestNo(),
                wr.getStatus(),
                wr.getStatus().getLabel(),
                new RequesterView(wr.getRequester().getId(), wr.getRequester().getName()),
                wr.getEquipment(),
                wr.getLine(),
                wr.getSubstance(),
                wr.getOperatingCondition(),
                wr.getProductName(),
                wr.getProductType(),
                wr.getProductType() == null ? null : wr.getProductType().getLabel(),
                wr.getSpecJson(),
                wr.getSymptom(),
                wr.getSiteMemo(),
                wr.getEngineerNote(),
                photos,
                agentRun,
                approval,
                wr.getCreatedAt(),
                wr.getUpdatedAt(),
                wr.getSubmittedAt());
    }
}
