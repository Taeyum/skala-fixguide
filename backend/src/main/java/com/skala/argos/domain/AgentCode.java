package com.skala.argos.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 명세 2.4 AgentCode — AI 검증 3종 (PoC 전부 구현, Mock) */
@Getter
@RequiredArgsConstructor
public enum AgentCode {
    A1("규격·호환"),
    A2("적용 법령"),
    A3("안전서류 초안");

    private final String title;
}
