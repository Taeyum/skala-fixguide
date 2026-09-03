package com.skala.fixguide.workrequest.dto;

import com.skala.fixguide.workrequest.entity.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/** 5.5 POST /work-requests — draft=true 면 모든 필드 선택 (AC 3-6) */
@Schema(description = "부품 교체 요청 생성. draft=true 면 필수 검증을 건너뛰고 임시저장한다.",
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
public record WorkRequestCreateRequest(
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
        @Schema(description = "true 면 임시저장(필수 검증 생략)", example = "false") Boolean draft) {

    public boolean isDraft() {
        return Boolean.TRUE.equals(draft);
    }
}
