package com.skala.argos.controller;

import com.skala.argos.dto.AgentDtos.PatchResultRequest;
import com.skala.argos.dto.AgentDtos.PatchResultResponse;
import com.skala.argos.dto.AgentDtos.PollResponse;
import com.skala.argos.dto.AgentDtos.StartRequest;
import com.skala.argos.dto.AgentDtos.StartResponse;
import com.skala.argos.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** API 11~13 — AI 검증 실행/폴링/결과 수정 (Mock) */
@RestController
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/agent-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartResponse start(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                               @Valid @RequestBody StartRequest request) {
        return agentService.start(userId, request);
    }

    @GetMapping("/agent-runs/{runId}")
    public PollResponse poll(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                             @PathVariable UUID runId) {
        return agentService.poll(userId, runId);
    }

    @PatchMapping("/agent-results/{id}")
    public PatchResultResponse patchResult(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                           @PathVariable UUID id,
                                           @RequestBody PatchResultRequest request) {
        return agentService.patchResult(userId, id, request);
    }
}
