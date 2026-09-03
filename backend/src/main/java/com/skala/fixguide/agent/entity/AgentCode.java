package com.skala.fixguide.agent.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** API 명세서 2.4 AgentCode — AI 검증 3종 (PoC 는 전부 Mock) */
@Getter
@RequiredArgsConstructor
public enum AgentCode {
    A1("규격·호환"),
    A2("적용 법령"),
    A3("안전서류 초안");

    private final String title;
}
