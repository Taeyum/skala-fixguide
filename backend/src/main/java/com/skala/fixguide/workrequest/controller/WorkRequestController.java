package com.skala.fixguide.workrequest.controller;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.dto.PageResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestSummaryResponse;
import com.skala.fixguide.workrequest.service.WorkRequestQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WorkRequest", description = "요청 목록 (화면 WRA_E_01 · WRA_E_05 · WRA_S_01)")
@RestController
@RequestMapping("/api/v1/work-requests")
@RequiredArgsConstructor
public class WorkRequestController {

    private final WorkRequestQueryService workRequestQueryService;

    @Operation(
            summary = "요청 목록 조회",
            description = "엔지니어는 본인 요청만, 안전관리자는 PENDING 이후 요청 전체를 조회한다.")
    @GetMapping
    public ResponseEntity<PageResponse<WorkRequestSummaryResponse>> search(
            @AuthenticationPrincipal AuthenticatedUser me,
            @RequestParam(name = "mine", defaultValue = "false") boolean mine,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(workRequestQueryService.search(me, mine, status, pageable));
    }
}
