package com.skala.argos.controller;

import com.skala.argos.dto.ApprovalDtos.DecideRequest;
import com.skala.argos.dto.ApprovalDtos.DecideResponse;
import com.skala.argos.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** API 15 — 승인/거절 (SAFETY_MANAGER 전용) */
@RestController
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    public DecideResponse decide(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                 @Valid @RequestBody DecideRequest request) {
        return approvalService.decide(userId, request);
    }
}
