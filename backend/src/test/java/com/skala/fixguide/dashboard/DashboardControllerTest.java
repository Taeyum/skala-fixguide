package com.skala.fixguide.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skala.fixguide.support.Fixtures;
import com.skala.fixguide.support.IntegrationTestSupport;
import com.skala.fixguide.support.TokenIssuer;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class DashboardControllerTest extends IntegrationTestSupport {

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    @DisplayName("AC 2-1 · 엔지니어 KPI 는 본인 요청만 상태별로 센다")
    void engineerSummaryCountsOnlyOwnRequests() throws Exception {
        User me = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        User other = createUser("김현장", "engineer2@fixguide.dev", Role.ENGINEER);

        workRequestRepository.saveAll(List.of(
                Fixtures.workRequest(me, WorkRequestStatus.DRAFT),
                Fixtures.workRequest(me, WorkRequestStatus.DRAFT),
                Fixtures.workRequest(me, WorkRequestStatus.AI_RUNNING),
                Fixtures.workRequest(me, WorkRequestStatus.PENDING),
                Fixtures.workRequest(me, WorkRequestStatus.REJECTED),
                // 다른 엔지니어 요청은 집계에서 빠져야 한다
                Fixtures.workRequest(other, WorkRequestStatus.PENDING),
                Fixtures.workRequest(other, WorkRequestStatus.DRAFT)));

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("role", "engineer")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.kpi.draft").value(2))
                .andExpect(jsonPath("$.kpi.aiRunning").value(1))
                .andExpect(jsonPath("$.kpi.pending").value(1))
                .andExpect(jsonPath("$.kpi.rejected").value(1))
                .andExpect(jsonPath("$.rejectReasonTop5").doesNotExist());
    }

    @Test
    @DisplayName("AC 7-1 / 7-3 · 안전관리자 KPI 와 거절 사유 TOP5")
    void safetySummaryWithRejectReasonRanking() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        User safety = createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);

        WorkRequest pending1 = workRequestRepository.save(
                Fixtures.workRequest(engineer, WorkRequestStatus.PENDING));
        WorkRequest pending2 = workRequestRepository.save(
                Fixtures.workRequest(engineer, WorkRequestStatus.PENDING));
        WorkRequest approved = workRequestRepository.save(
                Fixtures.workRequest(engineer, WorkRequestStatus.APPROVED));

        OffsetDateTime now = OffsetDateTime.now();
        approvalRepository.saveAll(List.of(
                Fixtures.reject(pending1, safety, "규격 부적합", now.minusMinutes(10)),
                Fixtures.reject(pending1, safety, "규격 부적합", now.minusMinutes(20)),
                Fixtures.reject(pending2, safety, "법령 미충족", now.minusMinutes(30)),
                Fixtures.approve(approved, safety, now.minusMinutes(40))));

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("role", "safety")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(safety)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SAFETY_MANAGER"))
                .andExpect(jsonPath("$.kpi.pending").value(2))
                .andExpect(jsonPath("$.kpi.processedToday").value(4))
                .andExpect(jsonPath("$.kpi.approvedThisMonth").value(1))
                .andExpect(jsonPath("$.kpi.rejectedThisMonth").value(3))
                .andExpect(jsonPath("$.rejectReasonTop5[0].category").value("규격 부적합"))
                .andExpect(jsonPath("$.rejectReasonTop5[0].count").value(2))
                .andExpect(jsonPath("$.rejectReasonTop5[1].category").value("법령 미충족"));
    }

    @Test
    @DisplayName("토큰 역할과 다른 role 로 조회하면 403 FORBIDDEN_ROLE")
    void roleMismatchIsForbidden() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("role", "safety")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(engineer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    @DisplayName("role 값이 잘못되면 400 VALIDATION_FAILED")
    void unknownRoleParam() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("role", "admin")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(engineer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary").param("role", "engineer"))
                .andExpect(status().isUnauthorized());
    }
}
