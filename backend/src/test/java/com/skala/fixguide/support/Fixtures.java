package com.skala.fixguide.support;

import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.Map;

/** 테스트 데이터 빌더 모음 */
public final class Fixtures {

    private Fixtures() {
    }

    public static WorkRequest workRequest(User requester, WorkRequestStatus status) {
        return WorkRequest.builder()
                .requester(requester)
                .equipment("펌프 P-114")
                .line("A라인")
                .substance("H2SO4")
                .operatingCondition(Map.of("temperature", "80 ℃", "pressure", "2500 psi"))
                .productName("SS-8-VCR")
                .productType(ProductType.VALVE)
                .specJson(Map.of("pressureRating", "3000 psi"))
                .status(status)
                .submittedAt(status == WorkRequestStatus.DRAFT ? null : OffsetDateTime.now())
                .build();
    }

    public static Approval reject(
            WorkRequest workRequest, User approver, String category, OffsetDateTime decidedAt) {
        return Approval.builder()
                .workRequest(workRequest)
                .approver(approver)
                .decision(ApprovalDecision.REJECT)
                .reason("거절 사유 테스트 데이터입니다.")
                .reasonCategory(category)
                .decidedAt(decidedAt)
                .build();
    }

    public static Approval approve(WorkRequest workRequest, User approver, OffsetDateTime decidedAt) {
        return Approval.builder()
                .workRequest(workRequest)
                .approver(approver)
                .decision(ApprovalDecision.APPROVE)
                .decidedAt(decidedAt)
                .build();
    }
}
