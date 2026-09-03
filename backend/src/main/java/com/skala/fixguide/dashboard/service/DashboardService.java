package com.skala.fixguide.dashboard.service;

import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.approval.repository.ApprovalRepository;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.dashboard.dto.DashboardSummaryResponse;
import com.skala.fixguide.dashboard.dto.RejectReasonCount;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 메인화면 KPI 집계 (AC 2-1 · 7-1 · 7-3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int REJECT_REASON_TOP_N = 5;

    private final WorkRequestRepository workRequestRepository;
    private final ApprovalRepository approvalRepository;
    private final Clock clock;

    /**
     * 역할별 대시보드 요약.
     *
     * @param roleParam 쿼리 파라미터 role (engineer|safety) — 토큰 역할과 다르면 403
     */
    public DashboardSummaryResponse summary(AuthenticatedUser me, String roleParam) {
        Role requested = Role.fromDashboardParam(roleParam);
        if (requested == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "role 은 engineer 또는 safety 여야 합니다.");
        }
        if (requested != me.role()) {
            throw new ApiException(ErrorCode.FORBIDDEN_ROLE, "토큰의 역할과 다른 대시보드는 조회할 수 없습니다.");
        }

        return requested == Role.ENGINEER ? engineerSummary(me.userId()) : safetySummary();
    }

    private DashboardSummaryResponse engineerSummary(UUID userId) {
        return DashboardSummaryResponse.engineer(new DashboardSummaryResponse.EngineerKpi(
                workRequestRepository.countByRequesterIdAndStatus(userId, WorkRequestStatus.DRAFT),
                workRequestRepository.countByRequesterIdAndStatus(userId, WorkRequestStatus.AI_RUNNING),
                workRequestRepository.countByRequesterIdAndStatus(userId, WorkRequestStatus.PENDING),
                workRequestRepository.countByRequesterIdAndStatus(userId, WorkRequestStatus.REJECTED)));
    }

    private DashboardSummaryResponse safetySummary() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime startOfMonth = LocalDate.of(now.getYear(), now.getMonth(), 1)
                .atStartOfDay()
                .atOffset(now.getOffset());

        DashboardSummaryResponse.SafetyKpi kpi = new DashboardSummaryResponse.SafetyKpi(
                workRequestRepository.countByStatus(WorkRequestStatus.PENDING),
                approvalRepository.countByDecidedAtBetween(startOfToday, now),
                approvalRepository.countByDecisionAndDecidedAtBetween(
                        ApprovalDecision.APPROVE, startOfMonth, now),
                approvalRepository.countByDecisionAndDecidedAtBetween(
                        ApprovalDecision.REJECT, startOfMonth, now));

        List<RejectReasonCount> top5 = approvalRepository.findRejectReasonRanking(
                startOfMonth, PageRequest.of(0, REJECT_REASON_TOP_N));

        return DashboardSummaryResponse.safety(kpi, top5);
    }
}
