package com.skala.fixguide.agent.dto;

import com.skala.fixguide.agent.entity.RunStatus;
import java.util.List;
import java.util.UUID;

public record AgentRunStartResponse(
        UUID runId,
        UUID workRequestId,
        RunStatus status,
        List<AgentStepResponse> steps,
        int pollIntervalMs) {
}
