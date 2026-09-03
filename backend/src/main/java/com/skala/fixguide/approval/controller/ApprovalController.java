package com.skala.fixguide.approval.controller;

import com.skala.fixguide.approval.dto.ApprovalDecideRequest;
import com.skala.fixguide.approval.dto.ApprovalDecideResponse;
import com.skala.fixguide.approval.service.ApprovalService;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Approval", description = "승인 · 거절 (화면 WRA_S_02)")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "승인 / 거절", description = "안전관리자 전용. REJECT 는 reason 10자 이상 필수. 201")
    @PostMapping
    public ResponseEntity<ApprovalDecideResponse> decide(
            @AuthenticationPrincipal AuthenticatedUser me,
            @Valid @RequestBody ApprovalDecideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.decide(me, request));
    }
}
