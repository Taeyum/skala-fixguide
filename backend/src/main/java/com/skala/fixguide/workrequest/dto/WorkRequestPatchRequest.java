package com.skala.fixguide.workrequest.dto;

import com.skala.fixguide.workrequest.entity.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/** 5.8 PATCH /work-requests/{id} — 변경할 필드만 전송 (부분 수정) */
@Schema(description = "부분 수정. 보낸 필드만 반영되며 나머지는 지우고 보내도 된다. "
        + "specJson·productType 을 보내면 스키마 재검증이 돌고, specJson 은 통째로 교체된다.",
        example = """
                {
                  "symptom": "밸브 시트 누설",
                  "engineerNote": "동일 사양 정품으로 교체 요청"
                }""")
public record WorkRequestPatchRequest(
        @Schema(description = "설비명", example = "펌프 P-114") String equipment,
        @Schema(description = "라인", example = "L3") String line,
        @Schema(description = "취급 물질", example = "HF") String substance,
        @Schema(description = "운전 조건. 키는 자유 형식 (예: pressure, temperature, flowRate)")
        Map<String, Object> operatingCondition,
        @Schema(description = "품명", example = "SS-8-VCR") String productName,
        ProductType productType,
        @Schema(description = WorkRequestValidatorDoc.SPEC_JSON) Map<String, Object> specJson,
        @Schema(description = "증상", example = "밸브 시트 누설") String symptom,
        @Schema(description = "현장 메모") String siteMemo,
        @Schema(description = "엔지니어 설명. 제출(submit-approval) 시 필수") String engineerNote) {
}
