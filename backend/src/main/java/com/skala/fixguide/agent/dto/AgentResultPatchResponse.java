package com.skala.fixguide.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skala.fixguide.agent.entity.AgentCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentResultPatchResponse(
        UUID agentResultId,
        AgentCode agentCode,
        boolean edited,
        List<Map<String, Object>> items,
        List<Map<String, Object>> documents,
        OffsetDateTime updatedAt) {
}
