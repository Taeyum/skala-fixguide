package com.skala.argos.domain;

import lombok.Getter;

import java.util.List;

/** 명세 2.3 ProductType — specJson 은 유형별 필수 키가 다른 자유 객체 */
@Getter
public enum ProductType {
    VALVE("밸브", List.of("pressureRating")),
    FITTING_TUBE("피팅·튜브", List.of("connectionStandard", "material")),
    REGULATOR("레귤레이터", List.of("pressureRating")),
    FILTER("필터", List.of("substanceType")),
    ETC("기타", List.of("freeSpec"));

    private final String label;
    private final List<String> requiredSpecKeys;

    ProductType(String label, List<String> requiredSpecKeys) {
        this.label = label;
        this.requiredSpecKeys = requiredSpecKeys;
    }
}
