package com.skala.fixguide.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skala.fixguide.user.entity.Role;
import java.util.List;

/**
 * GET /api/v1/dashboard/summary 응답 (API 명세서 5.4).
 * kpi 는 역할에 따라 EngineerKpi / SafetyKpi 중 하나가 들어간다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardSummaryResponse(Role role, Object kpi, List<RejectReasonCount> rejectReasonTop5) {

    /** 엔지니어 메인(E_01) KPI — v2.0 에서 "평균 승인 소요시간"은 제외되었다. */
    public record EngineerKpi(long draft, long aiRunning, long pending, long rejected) {
    }

    /** 안전관리자 메인(S_01) KPI */
    public record SafetyKpi(
            long pending, long processedToday, long approvedThisMonth, long rejectedThisMonth) {
    }

    public static DashboardSummaryResponse engineer(EngineerKpi kpi) {
        return new DashboardSummaryResponse(Role.ENGINEER, kpi, null);
    }

    public static DashboardSummaryResponse safety(SafetyKpi kpi, List<RejectReasonCount> top5) {
        return new DashboardSummaryResponse(Role.SAFETY_MANAGER, kpi, top5);
    }
}
