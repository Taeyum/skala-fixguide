package com.skala.fixguide.approval.service;

import com.skala.fixguide.approval.dto.ApprovalDecideRequest;
import com.skala.fixguide.approval.dto.ApprovalDecideResponse;
import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.approval.repository.ApprovalRepository;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import com.skala.fixguide.workrequest.service.WorkRequestAccessPolicy;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {

    private static final int MIN_REJECT_REASON_LENGTH = 10;

    private final ApprovalRepository approvalRepository;
    private final WorkRequestRepository workRequestRepository;
    private final UserRepository userRepository;
    private final WorkRequestAccessPolicy accessPolicy;
    private final Clock clock;

    /**
     * 5.15 POST /approvals — 승인/거절. append-only 로 새 행을 추가하고
     * 같은 트랜잭션에서 work_requests.status 를 APPROVED/REJECTED 로 갱신한다 (ERD 7장).
     * 승인에는 체크리스트 blocking 이 없고, 거절만 사유가 필수다 (화면정의서 v2.0).
     */
    public ApprovalDecideResponse decide(AuthenticatedUser me, ApprovalDecideRequest req) {
        accessPolicy.requireManager(me);
        User approver = userRepository.findById(me.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        WorkRequest wr = workRequestRepository.findById(req.workRequestId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORK_REQUEST_NOT_FOUND));
        if (wr.getStatus() == WorkRequestStatus.APPROVED || wr.getStatus() == WorkRequestStatus.REJECTED) {
            throw new ApiException(ErrorCode.ALREADY_DECIDED);
        }
        if (wr.getStatus() != WorkRequestStatus.PENDING) {
            throw new ApiException(ErrorCode.NOT_PENDING, "PENDING 상태의 요청만 처리할 수 있습니다.");
        }
        boolean reject = req.decision() == ApprovalDecision.REJECT;
        if (reject && (req.reason() == null || req.reason().strip().length() < MIN_REJECT_REASON_LENGTH)) {
            throw new ApiException(ErrorCode.REJECT_REASON_REQUIRED, "거절 사유는 10자 이상 입력해야 합니다.");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Approval approval = approvalRepository.save(Approval.builder()
                .workRequest(wr)
                .approver(approver)
                .decision(req.decision())
                .reason(reject ? req.reason() : null)
                .reasonCategory(req.reasonCategory())
                .decidedAt(now)
                .build());
        wr.decide(req.decision());

        return new ApprovalDecideResponse(approval.getId(), wr.getId(), approval.getDecision(), approval.getReason(),
                approval.getReasonCategory(), wr.getStatus(), approver.getName(), now);
    }
}
