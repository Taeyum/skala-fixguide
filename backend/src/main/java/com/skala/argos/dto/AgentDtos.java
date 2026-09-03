package com.skala.argos.dto;

import com.skala.argos.domain.AgentCode;
import com.skala.argos.domain.AgentStepStatus;
import com.skala.argos.domain.RunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API 11~13 (agent-runs · agent-results) 요청·응답 DTO */
public final class AgentDtos {

    private AgentDtos() {
    }

    /** 5.11 POST /agent-runs — 프론트는 workRequestId만 보낸다 (AI 입력 원칙) */
    @Schema(example = """
            {"workRequestId": "00000000-0000-0000-0000-000000000000"}""")
    public record StartRequest(
            @Schema(description = "AI를 돌릴 요청서 id") @NotNull(message = "must not be null") UUID workRequestId,
            @Schema(description = "실행할 에이전트. 비우면 A1·A2·A3 전체") List<AgentCode> agents
    ) {
    }

    public record StepView(
            AgentCode agentCode,
            String title,
            AgentStepStatus status,
            String message,
            UUID agentResultId,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
    }

    public record StartResponse(UUID runId, UUID workRequestId, RunStatus status,
                                List<StepView> steps, int pollIntervalMs) {
    }

    /** 5.12 GET /agent-runs/{runId} — allDone: true 시점에 프론트가 폴링 중단 (AC 4-3) */
    public record PollResponse(UUID runId, UUID workRequestId, RunStatus status, OffsetDateTime startedAt,
                               List<StepView> steps, boolean allDone, int pollIntervalMs) {
    }

    /** 5.13 PATCH /agent-results/{id} — A1·A2는 items, A3는 documents. 전체 치환(PUT-like) */
    @Schema(description = "A1·A2는 items, A3는 documents만 보낸다. 배열 전체가 치환되며 "
            + "빠진 기존 항목은 삭제, id 없이 보낸 항목은 신규 추가된다.",
            example = """
                    {
                      "items": [
                        {"itemId": "i-01", "text": "규격 적합 — 엔지니어 확인 완료"},
                        {"text": "현장 확인 결과 추가 근거"}
                      ]
                    }""")
    public record PatchResultRequest(
            @Schema(description = "A1·A2 결과 항목. 각 항목은 itemId(선택)·text") List<Map<String, Object>> items,
            @Schema(description = "A3 문서. 각 항목은 docId(선택)·type·name·content") List<Map<String, Object>> documents
    ) {
    }

    public record PatchResultResponse(UUID agentResultId, AgentCode agentCode, boolean edited,
                                      List<Map<String, Object>> items, List<Map<String, Object>> documents,
                                      OffsetDateTime updatedAt) {
    }
}
