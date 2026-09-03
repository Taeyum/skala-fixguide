package com.skala.fixguide.agent.service;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * AI 검증 3종 Mock (가이드: "AI 서비스는 Mock API 로 JSON 반환").
 * 실제 LLM 전환 시 이 컴포넌트만 ai_configs.provider 기반 구현으로 교체한다 (Interface First).
 */
@Component
public class MockAgentEngine {

    public String runningMessage(AgentCode code) {
        return switch (code) {
            case A1 -> "입력 스펙 기준 규격 적합성 검토 중…";
            case A2 -> "관련 조문 검색 중…";
            case A3 -> "허가서·위험성평가 생성 중…";
        };
    }

    public String doneMessage(AgentCode code, WorkRequest wr) {
        return switch (code) {
            case A1 -> "입력 스펙(" + specSummary(wr) + ") 기준 규격 적합 — 근거 2건";
            case A2 -> "적용 법령 2건 식별 — " + safe(wr.getSubstance()) + " 취급 기준";
            case A3 -> "작업허가서·위험성평가서 초안 생성 완료";
        };
    }

    /** ERD 6장 payload_json 구조: A1·A2 는 items[], A3 는 documents[] */
    public Map<String, Object> payload(AgentCode code, WorkRequest wr) {
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (code) {
            case A1 -> payload.put("items", List.of(
                    item("i-01", "규격 적합: " + specSummary(wr) + " — 운전 조건("
                            + operatingSummary(wr) + ") 충족 판단 (Mock)"),
                    item("i-02", "대체 호환: " + safe(wr.getProductName()) + " 동급 사양 확인 필요 (Mock)")));
            case A2 -> payload.put("items", List.of(
                    item("i-01", "산업안전보건기준에 관한 규칙 제38조 — " + safe(wr.getSubstance())
                            + " 취급 설비 작업계획 대상 (Mock)"),
                    item("i-02", "고압가스 안전관리법 시행규칙 — 운전 압력 기준 검토 필요 (Mock)")));
            case A3 -> payload.put("documents", List.of(
                    document("d-01", "WORK_PERMIT", "작업허가서 초안",
                            safe(wr.getLine()) + " " + safe(wr.getEquipment()) + " 부품("
                                    + safe(wr.getProductName()) + ") 교체 작업허가서 초안 본문… (Mock)"),
                    document("d-02", "RISK_ASSESSMENT", "위험성평가서 초안",
                            safe(wr.getSubstance()) + " 취급 조건에서의 교체 작업 위험성평가 초안 본문… (Mock)")));
        }
        return payload;
    }

    private Map<String, Object> item(String itemId, String text) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("itemId", itemId);
        item.put("text", text);
        item.put("edited", false);
        return item;
    }

    private Map<String, Object> document(String docId, String type, String name, String content) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("docId", docId);
        document.put("type", type);
        document.put("name", name);
        document.put("content", content);
        document.put("edited", false);
        return document;
    }

    private String specSummary(WorkRequest wr) {
        return summarize(wr.getSpecJson(), "스펙 미입력");
    }

    private String operatingSummary(WorkRequest wr) {
        return summarize(wr.getOperatingCondition(), "미입력");
    }

    private String summarize(Map<String, Object> map, String fallback) {
        if (map == null || map.isEmpty()) {
            return fallback;
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse(fallback);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "미입력" : value;
    }
}
