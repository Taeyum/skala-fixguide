package com.skala.fixguide.agent.dto;

import com.skala.fixguide.agent.entity.AgentCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** 5.11 POST /agent-runs — 프론트는 workRequestId 만 보낸다 (AI 입력 원칙) */
@Schema(example = """
        {"workRequestId": "00000000-0000-0000-0000-000000000000"}""")
public record AgentRunStartRequest(
        @Schema(description = "AI 를 돌릴 요청서 id")
        @NotNull(message = "must not be null") UUID workRequestId,
        @Schema(description = "실행할 에이전트. 비우면 A1·A2·A3 전체") List<AgentCode> agents) {
}
