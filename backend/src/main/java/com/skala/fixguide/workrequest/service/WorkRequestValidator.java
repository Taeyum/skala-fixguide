package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.common.error.ErrorResponse.FieldError;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 요청 등록·제출·AI 실행이 공유하는 필수값·스펙 스키마 검증 */
public final class WorkRequestValidator {

    private WorkRequestValidator() {
    }

    /** 명세 5.5 Y* 필드 — draft=false 일 때만 필수 */
    public static List<FieldError> missingRequired(
            String equipment,
            String line,
            String substance,
            Map<String, Object> operatingCondition,
            String productName,
            ProductType productType,
            Map<String, Object> specJson) {
        List<FieldError> errors = new ArrayList<>();
        if (isBlank(equipment)) errors.add(new FieldError("equipment", "must not be blank"));
        if (isBlank(line)) errors.add(new FieldError("line", "must not be blank"));
        if (isBlank(substance)) errors.add(new FieldError("substance", "must not be blank"));
        if (operatingCondition == null || operatingCondition.isEmpty()) {
            errors.add(new FieldError("operatingCondition", "must not be empty"));
        }
        if (isBlank(productName)) errors.add(new FieldError("productName", "must not be blank"));
        if (productType == null) errors.add(new FieldError("productType", "must not be null"));
        if (specJson == null || specJson.isEmpty()) errors.add(new FieldError("specJson", "must not be empty"));
        return errors;
    }

    public static List<FieldError> missingRequired(WorkRequest wr) {
        return missingRequired(wr.getEquipment(), wr.getLine(), wr.getSubstance(), wr.getOperatingCondition(),
                wr.getProductName(), wr.getProductType(), wr.getSpecJson());
    }

    /** 명세 2.3 — specJson 은 productType 별 필수 키 검증. 불일치 400 SPEC_SCHEMA_MISMATCH */
    public static void validateSpecSchema(ProductType productType, Map<String, Object> specJson) {
        if (productType == null || specJson == null) {
            return;
        }
        List<FieldError> missing = productType.getRequiredSpecKeys().stream()
                .filter(key -> specJson.get(key) == null || specJson.get(key).toString().isBlank())
                .map(key -> new FieldError("specJson." + key, "must not be blank"))
                .toList();
        if (!missing.isEmpty()) {
            throw new ApiException(ErrorCode.SPEC_SCHEMA_MISMATCH,
                    "productType(" + productType + ")에 필요한 specJson 키가 누락되었습니다.", missing);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
