package com.skala.fixguide.agent.dto;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AgentStep;
import com.skala.fixguide.agent.entity.AgentStepStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AgentStepResponse(
        AgentCode agentCode,
        String title,
        AgentStepStatus status,
        String message,
        UUID agentResultId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {

    public static AgentStepResponse from(AgentStep step, Map<AgentCode, UUID> resultIds) {
        return new AgentStepResponse(
                step.getAgentCode(),
                step.getAgentCode().getTitle(),
                step.getStatus(),
                step.getMessage(),
                resultIds.get(step.getAgentCode()),
                step.getStartedAt(),
                step.getFinishedAt());
    }
}
