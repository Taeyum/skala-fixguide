package com.skala.fixguide.agent.controller;

import com.skala.fixguide.agent.dto.AgentResultPatchRequest;
import com.skala.fixguide.agent.dto.AgentResultPatchResponse;
import com.skala.fixguide.agent.dto.AgentRunPollResponse;
import com.skala.fixguide.agent.dto.AgentRunStartRequest;
import com.skala.fixguide.agent.dto.AgentRunStartResponse;
import com.skala.fixguide.agent.service.AgentService;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent", description = "AI 검증 실행 · 폴링 · 결과 수정 (화면 WRA_E_03 · WRA_E_04)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @Operation(summary = "AI 검증 3종 실행 요청", description = "엔지니어 본인 요청만. 202 를 받으면 runId 로 폴링한다.")
    @PostMapping("/agent-runs")
    public ResponseEntity<AgentRunStartResponse> start(
            @AuthenticationPrincipal AuthenticatedUser me,
            @Valid @RequestBody AgentRunStartRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(agentService.start(me, request));
    }

    @Operation(summary = "에이전트 진행 상태 폴링", description = "allDone=true 가 되면 폴링을 멈춘다.")
    @GetMapping("/agent-runs/{runId}")
    public ResponseEntity<AgentRunPollResponse> poll(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID runId) {
        return ResponseEntity.ok(agentService.poll(me, runId));
    }

    @Operation(summary = "AI 결과물 수정", description = "A1·A2 는 items, A3 는 documents 배열 전체 치환.")
    @PatchMapping("/agent-results/{id}")
    public ResponseEntity<AgentResultPatchResponse> patchResult(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id,
            @RequestBody AgentResultPatchRequest request) {
        return ResponseEntity.ok(agentService.patchResult(me, id, request));
    }
}
