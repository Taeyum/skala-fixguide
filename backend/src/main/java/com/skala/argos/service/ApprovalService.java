package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.common.KstTime;
import com.skala.argos.domain.Approval;
import com.skala.argos.domain.ApprovalDecision;
import com.skala.argos.domain.User;
import com.skala.argos.domain.WorkRequest;
import com.skala.argos.domain.WorkRequestStatus;
import com.skala.argos.dto.ApprovalDtos.DecideRequest;
import com.skala.argos.dto.ApprovalDtos.DecideResponse;
import com.skala.argos.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final WorkRequestService workRequestService;
    private final UserFinder userFinder;
    private final AccessPolicy accessPolicy;

    /**
     * 5.15 POST /approvals — 승인/거절. append-only로 새 행을 추가하고
     * 같은 트랜잭션에서 work_requests.status를 APPROVED/REJECTED로 갱신 (ERD 7장).
     * 승인에는 체크리스트 blocking 없음, 거절만 사유 필수 (화면정의서 v2.0).
     */
    public DecideResponse decide(UUID userId, DecideRequest req) {
        User user = userFinder.get(userId);
        accessPolicy.requireManager(user);

        WorkRequest wr = workRequestService.getOrThrow(req.workRequestId());
        if (wr.getStatus() == WorkRequestStatus.APPROVED || wr.getStatus() == WorkRequestStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_DECIDED", "이미 처리된 요청입니다.");
        }
        if (wr.getStatus() != WorkRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_PENDING", "PENDING 상태의 요청만 처리할 수 있습니다.");
        }
        if (req.decision() == ApprovalDecision.REJECT
                && (req.reason() == null || req.reason().strip().length() < 10)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REJECT_REASON_REQUIRED",
                    "거절 사유는 10자 이상 입력해야 합니다.");
        }

        Instant now = Instant.now();
        Approval approval = new Approval();
        approval.setId(UUID.randomUUID());
        approval.setWorkRequest(wr);
        approval.setApprover(user);
        approval.setDecision(req.decision());
        approval.setReason(req.decision() == ApprovalDecision.REJECT ? req.reason() : null);
        approval.setReasonCategory(req.reasonCategory());
        approval.setDecidedAt(now);
        approvalRepository.save(approval);

        wr.setStatus(req.decision() == ApprovalDecision.APPROVE
                ? WorkRequestStatus.APPROVED : WorkRequestStatus.REJECTED);
        wr.setUpdatedAt(now);

        return new DecideResponse(approval.getId(), wr.getId(), approval.getDecision(), approval.getReason(),
                approval.getReasonCategory(), wr.getStatus(), user.getName(), KstTime.of(now));
    }
}
