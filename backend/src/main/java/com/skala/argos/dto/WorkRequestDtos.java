package com.skala.argos.dto;

import com.skala.argos.domain.ProductType;
import com.skala.argos.domain.WorkRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API 5~10·14 (work-requests) 요청·응답 DTO — 필드명은 명세 5장 그대로 */
public final class WorkRequestDtos {

    private WorkRequestDtos() {
    }

    /** specJson은 productType별 필수 키가 다르다 — ProductType.requiredSpecKeys 와 동일 내용 */
    private static final String SPEC_JSON_DESC =
            "제품 스펙. productType별 필수 키가 다르며 누락 시 400 SPEC_SCHEMA_MISMATCH. "
                    + "VALVE·REGULATOR → pressureRating / FITTING_TUBE → connectionStandard, material / "
                    + "FILTER → substanceType / ETC → freeSpec";

    /** 5.5 POST /work-requests — draft=true면 모든 필드 선택 (AC 3-6) */
    @Schema(description = "부품 교체 요청 생성. draft=true면 필수 검증을 건너뛰고 임시저장한다.",
            example = """
                    {
                      "equipment": "펌프 P-114",
                      "line": "L3",
                      "substance": "HF",
                      "operatingCondition": {"pressure": "2500 psi", "temperature": "80 ℃"},
                      "productName": "SS-8-VCR",
                      "productType": "VALVE",
                      "specJson": {"pressureRating": "3000 psi"},
                      "symptom": "밸브 시트 누설",
                      "siteMemo": "야간 정비 시간대 작업 요망",
                      "draft": false
                    }""")
    public record CreateRequest(
            @Schema(description = "설비명", example = "펌프 P-114") String equipment,
            @Schema(description = "라인", example = "L3") String line,
            @Schema(description = "취급 물질", example = "HF") String substance,
            @Schema(description = "운전 조건. 키는 자유 형식 (예: pressure, temperature, flowRate)")
            Map<String, Object> operatingCondition,
            @Schema(description = "품명", example = "SS-8-VCR") String productName,
            ProductType productType,
            @Schema(description = SPEC_JSON_DESC) Map<String, Object> specJson,
            @Schema(description = "증상", example = "밸브 시트 누설") String symptom,
            @Schema(description = "현장 메모") String siteMemo,
            @Schema(description = "true면 임시저장(필수 검증 생략)", example = "false") Boolean draft
    ) {
        public boolean isDraft() {
            return Boolean.TRUE.equals(draft);
        }
    }

    public record CreateResponse(UUID workRequestId, String requestNo, WorkRequestStatus status,
                                 OffsetDateTime createdAt) {
    }

    /** 5.8 PATCH /work-requests/{id} — 변경할 필드만 전송 (부분 수정) */
    @Schema(description = "부분 수정. 보낸 필드만 반영되며, 나머지는 지우고 보내도 된다. "
            + "specJson·productType을 보내면 스키마 재검증이 돌고, specJson은 통째로 교체된다.",
            example = """
                    {
                      "symptom": "밸브 시트 누설",
                      "engineerNote": "동일 사양 정품으로 교체 요청"
                    }""")
    public record PatchRequest(
            @Schema(description = "설비명", example = "펌프 P-114") String equipment,
            @Schema(description = "라인", example = "L3") String line,
            @Schema(description = "취급 물질", example = "HF") String substance,
            @Schema(description = "운전 조건. 키는 자유 형식 (예: pressure, temperature, flowRate)")
            Map<String, Object> operatingCondition,
            @Schema(description = "품명", example = "SS-8-VCR") String productName,
            ProductType productType,
            @Schema(description = SPEC_JSON_DESC) Map<String, Object> specJson,
            @Schema(description = "증상", example = "밸브 시트 누설") String symptom,
            @Schema(description = "현장 메모") String siteMemo,
            @Schema(description = "엔지니어 설명. 제출(submit-approval) 시 필수") String engineerNote
    ) {
    }

    public record PatchResponse(UUID workRequestId, WorkRequestStatus status, OffsetDateTime updatedAt) {
    }

    /** 5.14 PATCH /work-requests/{id}/submit-approval */
    public record SubmitRequest(
            @Schema(description = "엔지니어 설명. 비우면 기존 저장값을 쓰고, 둘 다 없으면 422",
                    example = "동일 사양 정품으로 교체 요청") String engineerNote) {
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
