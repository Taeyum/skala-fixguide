package com.skala.argos.dto;

import com.skala.argos.domain.AgentCode;
import com.skala.argos.domain.AgentStepStatus;
import com.skala.argos.domain.RunStatus;
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
    public record StartRequest(
            @NotNull(message = "must not be null") UUID workRequestId,
            List<AgentCode> agents
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
    public record PatchResultRequest(
            List<Map<String, Object>> items,
            List<Map<String, Object>> documents
    ) {
    }

    public record PatchResultResponse(UUID agentResultId, AgentCode agentCode, boolean edited,
                                      List<Map<String, Object>> items, List<Map<String, Object>> documents,
                                      OffsetDateTime updatedAt) {
    }
}
