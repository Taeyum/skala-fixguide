package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AgentResult;
import com.skala.fixguide.agent.entity.AgentRun;
import com.skala.fixguide.agent.repository.AgentResultRepository;
import com.skala.fixguide.agent.repository.AgentRunRepository;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.common.error.ErrorResponse.FieldError;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import com.skala.fixguide.workrequest.dto.WorkRequestCreateRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestCreateResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestPatchRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestPatchResponse;
import com.skala.fixguide.workrequest.dto.WorkRequestSubmitRequest;
import com.skala.fixguide.workrequest.dto.WorkRequestSubmitResponse;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요청 등록·수정·제출 (API 5 · 8 · 14). 조회는 {@link WorkRequestQueryService}. */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkRequestCommandService {

    private final WorkRequestRepository workRequestRepository;
    private final UserRepository userRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentResultRepository agentResultRepository;
    private final WorkRequestAccessPolicy accessPolicy;
    private final RequestNoGenerator requestNoGenerator;
    private final Clock clock;

    /** 5.5 POST /work-requests — draft=true 면 상태만 DRAFT 로 기록하고 필수 검증을 생략한다 (AC 3-6) */
    public WorkRequestCreateResponse create(AuthenticatedUser me, WorkRequestCreateRequest req) {
        accessPolicy.requireEngineer(me);
        User requester = userRepository.findById(me.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (!req.isDraft()) {
            List<FieldError> missing = WorkRequestValidator.missingRequired(req.equipment(), req.line(),
                    req.substance(), req.operatingCondition(), req.productName(), req.productType(), req.specJson());
            if (!missing.isEmpty()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "필수 항목이 누락되었습니다.", missing);
            }
            WorkRequestValidator.validateSpecSchema(req.productType(), req.specJson());
        }

        WorkRequest wr = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(requester)
                .equipment(req.equipment())
                .line(req.line())
                .substance(req.substance())
                .operatingCondition(req.operatingCondition())
                .productName(req.productName())
                .productType(req.productType())
                .specJson(req.specJson())
                .symptom(req.symptom())
                .siteMemo(req.siteMemo())
                .status(WorkRequestStatus.DRAFT)
                .build());

        return new WorkRequestCreateResponse(wr.getId(), wr.getRequestNo(), wr.getStatus(), OffsetDateTime.now(clock));
    }

    /** 5.8 PATCH /work-requests/{id} — 부분 수정. PENDING·APPROVED 에서는 409 IMMUTABLE_STATUS */
    public WorkRequestPatchResponse patch(AuthenticatedUser me, UUID workRequestId, WorkRequestPatchRequest req) {
        WorkRequest wr = getOrThrow(workRequestId);
        accessPolicy.requireOwner(me, wr);
        if (wr.getStatus().isImmutable()) {
            throw new ApiException(ErrorCode.IMMUTABLE_STATUS, "PENDING·APPROVED 상태에서는 수정할 수 없습니다.");
        }

        wr.applyPatch(req.equipment(), req.line(), req.substance(), req.operatingCondition(), req.productName(),
                req.productType(), req.specJson(), req.symptom(), req.siteMemo(), req.engineerNote());

        if (req.productType() != null || req.specJson() != null) {
            WorkRequestValidator.validateSpecSchema(wr.getProductType(), wr.getSpecJson());
        }
        return new WorkRequestPatchResponse(wr.getId(), wr.getStatus(), OffsetDateTime.now(clock));
    }

    /** 5.14 PATCH /work-requests/{id}/submit-approval — 제출 전 서버 검증 실패 시 422 */
    public WorkRequestSubmitResponse submit(AuthenticatedUser me, UUID workRequestId, WorkRequestSubmitRequest req) {
        WorkRequest wr = getOrThrow(workRequestId);
        accessPolicy.requireOwner(me, wr);

        String note = req != null && req.engineerNote() != null && !req.engineerNote().isBlank()
                ? req.engineerNote()
                : wr.getEngineerNote();

        List<FieldError> errors = new ArrayList<>();
        if (wr.getStatus() != WorkRequestStatus.AI_DONE && wr.getStatus() != WorkRequestStatus.REJECTED) {
            errors.add(new FieldError("status", "AI_DONE 또는 REJECTED 상태에서만 제출할 수 있습니다."));
        }
        if (note == null || note.isBlank()) {
            errors.add(new FieldError("engineerNote", "must not be blank"));
        }
        List<AgentResult> results = agentRunRepository.findTopByWorkRequestIdOrderByStartedAtDesc(workRequestId)
                .map(AgentRun::getId)
                .map(agentResultRepository::findByRunIdOrderByAgentCode)
                .orElse(List.of());
        if (results.size() < AgentCode.values().length) {
            errors.add(new FieldError("agentResults", "에이전트 3종(A1·A2·A3) 결과가 모두 있어야 합니다."));
        }
        results.stream()
                .filter(r -> r.getAgentCode() == AgentCode.A2)
                .findFirst()
                .ifPresent(a2 -> {
                    Object items = a2.getPayloadJson().get("items");
                    if (!(items instanceof List<?> list) || list.isEmpty()) {
                        errors.add(new FieldError("agentResults.A2", "적용 법령 항목이 1건 이상 필요합니다."));
                    }
                });
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.SUBMIT_REQUIRED_FIELD_MISSING,
                    ErrorCode.SUBMIT_REQUIRED_FIELD_MISSING.getDefaultMessage(), errors);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        wr.submit(note, now);
        return new WorkRequestSubmitResponse(wr.getId(), wr.getStatus(), now);
    }

    public WorkRequest getOrThrow(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.WORK_REQUEST_NOT_FOUND));
    }
}
