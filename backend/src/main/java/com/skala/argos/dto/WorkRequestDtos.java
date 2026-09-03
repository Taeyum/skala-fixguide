package com.skala.argos.dto;

import com.skala.argos.domain.ProductType;
import com.skala.argos.domain.WorkRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API 5~10·14 (work-requests) 요청·응답 DTO — 필드명은 명세 5장 그대로 */
public final class WorkRequestDtos {

    private WorkRequestDtos() {
    }

    /** 5.5 POST /work-requests — draft=true면 모든 필드 선택 (AC 3-6) */
    public record CreateRequest(
            String equipment,
            String line,
            String substance,
            Map<String, Object> operatingCondition,
            String productName,
            ProductType productType,
            Map<String, Object> specJson,
            String symptom,
            String siteMemo,
            Boolean draft
    ) {
        public boolean isDraft() {
            return Boolean.TRUE.equals(draft);
        }
    }

    public record CreateResponse(UUID workRequestId, String requestNo, WorkRequestStatus status,
                                 OffsetDateTime createdAt) {
    }

    /** 5.8 PATCH /work-requests/{id} — 변경할 필드만 전송 (부분 수정) */
    public record PatchRequest(
            String equipment,
            String line,
            String substance,
            Map<String, Object> operatingCondition,
            String productName,
            ProductType productType,
            Map<String, Object> specJson,
            String symptom,
            String siteMemo,
            String engineerNote
    ) {
    }

    public record PatchResponse(UUID workRequestId, WorkRequestStatus status, OffsetDateTime updatedAt) {
    }

    /** 5.14 PATCH /work-requests/{id}/submit-approval */
    public record SubmitRequest(String engineerNote) {
    }

    public record SubmitResponse(UUID workRequestId, WorkRequestStatus status, OffsetDateTime submittedAt) {
    }

    /** 5.6 — 상태별 화면 이동은 서버가 계산해 내려준다 */
    public record NextAction(String label, String path) {
    }

    public record SummaryItem(
            UUID workRequestId,
            String requestNo,
            String equipment,
            String productName,
            ProductType productType,
            String productTypeLabel,
            WorkRequestStatus status,
            String statusLabel,
            String requesterName,
            OffsetDateTime createdAt,
            OffsetDateTime submittedAt,
            NextAction nextAction
    ) {
    }

    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }

    public record PageResponse(List<SummaryItem> content, PageInfo page) {
    }

    public record RequesterView(UUID userId, String name) {
    }

    public record ApprovalView(String decision, String reason, String reasonCategory,
                               String decidedBy, OffsetDateTime decidedAt) {
    }

    public record PhotoView(UUID photoId, String fileName, Integer size, String thumbnailUrl,
                            String originalUrl, OffsetDateTime uploadedAt) {
    }

    public record PhotosResponse(List<PhotoView> photos) {
    }

    /** 5.7 GET /work-requests/{id} — AI 결과·승인 이력 포함 */
    public record Detail(
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
            List<PhotoView> photos,
            Map<String, Object> agentRun,
            ApprovalView approval,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime submittedAt
    ) {
    }
}
