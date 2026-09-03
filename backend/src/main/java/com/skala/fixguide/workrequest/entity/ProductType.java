package com.skala.fixguide.workrequest.entity;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** API 명세서 2.3 ProductType. requiredSpecKeys 는 SPEC_SCHEMA_MISMATCH 검증에 쓰인다. */
@Getter
@RequiredArgsConstructor
public enum ProductType {
    VALVE("밸브", List.of("pressureRating")),
    FITTING_TUBE("피팅·튜브", List.of("connectionStandard", "material")),
    REGULATOR("레귤레이터", List.of("pressureRating")),
    FILTER("필터", List.of("substanceType")),
    ETC("기타", List.of("freeSpec"));

    private final String label;
    private final List<String> requiredSpecKeys;
}
