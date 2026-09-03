package com.skala.argos.controller;

import com.skala.argos.dto.WorkRequestDtos.CreateRequest;
import com.skala.argos.dto.WorkRequestDtos.CreateResponse;
import com.skala.argos.dto.WorkRequestDtos.Detail;
import com.skala.argos.dto.WorkRequestDtos.PageResponse;
import com.skala.argos.dto.WorkRequestDtos.PatchRequest;
import com.skala.argos.dto.WorkRequestDtos.PatchResponse;
import com.skala.argos.dto.WorkRequestDtos.SubmitRequest;
import com.skala.argos.dto.WorkRequestDtos.SubmitResponse;
import com.skala.argos.service.WorkRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** API 5~8·14 — /api/v1/work-requests. 사용자 식별은 X-User-Id 헤더 (JWT 전 임시) */
@RestController
@RequestMapping("/work-requests")
@RequiredArgsConstructor
public class WorkRequestController {

    private final WorkRequestService workRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse create(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                 @RequestBody CreateRequest request) {
        return workRequestService.create(userId, request);
    }

    @GetMapping
    public PageResponse list(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                             @RequestParam(required = false) Boolean mine,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(defaultValue = "submittedAt,desc") String sort) {
        // mine 파라미터는 수신만 한다: 권한 규칙상 ENGINEER는 항상 본인 것만 조회된다 (명세 1장)
        return workRequestService.list(userId, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public Detail detail(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                         @PathVariable UUID id) {
        return workRequestService.detail(userId, id);
    }

    @PatchMapping("/{id}")
    public PatchResponse patch(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                               @PathVariable UUID id,
                               @RequestBody PatchRequest request) {
        return workRequestService.patch(userId, id, request);
    }

    @PatchMapping("/{id}/submit-approval")
    public SubmitResponse submit(@RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                 @PathVariable UUID id,
                                 @RequestBody(required = false) SubmitRequest request) {
        return workRequestService.submit(userId, id, request);
    }
}
