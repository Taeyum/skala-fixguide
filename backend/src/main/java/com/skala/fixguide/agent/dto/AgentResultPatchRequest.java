package com.skala.fixguide.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** 5.13 PATCH /agent-results/{id} — A1·A2 는 items, A3 는 documents. 전체 치환(PUT-like) */
@Schema(description = "A1·A2 는 items, A3 는 documents 만 보낸다. 배열 전체가 치환되며 "
        + "빠진 기존 항목은 삭제, id 없이 보낸 항목은 신규 추가된다.",
        example = """
                {
                  "items": [
                    {"itemId": "i-01", "text": "규격 적합 — 엔지니어 확인 완료"},
                    {"text": "현장 확인 결과 추가 근거"}
                  ]
                }""")
public record AgentResultPatchRequest(
        @Schema(description = "A1·A2 결과 항목. 각 항목은 itemId(선택)·text") List<Map<String, Object>> items,
        @Schema(description = "A3 문서. 각 항목은 docId(선택)·type·name·content") List<Map<String, Object>> documents) {
}
