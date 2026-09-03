package com.skala.fixguide.workrequest.dto;

/** Swagger 설명 문구 공유용 */
final class WorkRequestValidatorDoc {

    static final String SPEC_JSON =
            "제품 스펙. productType 별 필수 키가 다르며 누락 시 400 SPEC_SCHEMA_MISMATCH. "
                    + "VALVE·REGULATOR → pressureRating / FITTING_TUBE → connectionStandard, material / "
                    + "FILTER → substanceType / ETC → freeSpec";

    private WorkRequestValidatorDoc() {
    }
}
