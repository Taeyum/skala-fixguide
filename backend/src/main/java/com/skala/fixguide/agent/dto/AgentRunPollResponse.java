package com.skala.fixguide.agent.dto;

import com.skala.fixguide.agent.entity.RunStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 5.12 GET /agent-runs/{runId} — allDone: true 시점에 프론트가 폴링을 멈춘다 (AC 4-3) */
public record AgentRunPollResponse(
        UUID runId,
        UUID workRequestId,
        RunStatus status,
        OffsetDateTime startedAt,
        List<AgentStepResponse> steps,
        boolean allDone,
        int pollIntervalMs) {
}
