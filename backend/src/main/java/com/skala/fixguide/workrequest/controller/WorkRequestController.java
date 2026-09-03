package com.skala.fixguide.workrequest.controller;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.dto.PageResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestCreateRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestCreateResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestDetailResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestPatchRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestPatchResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestSubmitRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestSubmitResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestSummaryResponse;
import com.skala.fixguide.workrequest.service.WorkRequestCommandService;
import com.skala.fixguide.workrequest.service.WorkRequestQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WorkRequest", description = "요청 등록·목록·상세·수정·제출 (화면 WRA_E_01 ~ E_05 · S_01 · S_02)")
@RestController
@RequestMapping("/api/v1/work-requests")
@RequiredArgsConstructor
public class WorkRequestController {

    private final WorkRequestQueryService workRequestQueryService;
    private final WorkRequestCommandService workRequestCommandService;

    @Operation(summary = "요청 생성", description = "엔지니어 전용. draft=true 면 임시저장(필수 검증 생략). 201")
    @PostMapping
    public ResponseEntity<WorkRequestCreateResponse> create(
            @AuthenticationPrincipal AuthenticatedUser me,
            @RequestBody WorkRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workRequestCommandService.create(me, request));
    }

    @Operation(
            summary = "요청 목록 조회",
            description = "엔지니어는 본인 요청만, 안전관리자는 PENDING 이후 요청 전체를 조회한다.")
    @GetMapping
    public ResponseEntity<PageResponse<WorkRequestSummaryResponse>> search(
            @AuthenticationPrincipal AuthenticatedUser me,
            @RequestParam(name = "mine", defaultValue = "false") boolean mine,
            @RequestParam(name = "status", required = false) String status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(workRequestQueryService.search(me, mine, status, pageable));
    }

    @Operation(summary = "요청 상세", description = "AI 결과·승인 이력·사진 포함. 엔지니어는 본인 것만, 안전관리자는 PENDING 이후만.")
    @GetMapping("/{id}")
    public ResponseEntity<WorkRequestDetailResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return ResponseEntity.ok(workRequestQueryService.detail(me, id));
    }

    @Operation(summary = "요청 부분 수정", description = "보낸 필드만 반영. PENDING·APPROVED 에서는 409 IMMUTABLE_STATUS")
    @PatchMapping("/{id}")
    public ResponseEntity<WorkRequestPatchResponse> patch(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id,
            @RequestBody WorkRequestPatchRequest request) {
        return ResponseEntity.ok(workRequestCommandService.patch(me, id, request));
    }

    @Operation(summary = "안전관리자에게 제출", description = "AI_DONE·REJECTED 에서만. 검증 실패 시 422 SUBMIT_REQUIRED_FIELD_MISSING")
    @PatchMapping("/{id}/submit-approval")
    public ResponseEntity<WorkRequestSubmitResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkRequestSubmitRequest request) {
        return ResponseEntity.ok(workRequestCommandService.submit(me, id, request));
    }
}
