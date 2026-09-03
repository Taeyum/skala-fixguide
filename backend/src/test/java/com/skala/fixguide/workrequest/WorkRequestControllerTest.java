package com.skala.fixguide.workrequest;

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
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class WorkRequestControllerTest extends IntegrationTestSupport {

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    @DisplayName("AC 2-2 · 엔지니어 목록에는 본인 요청만 나온다")
    void engineerSeesOnlyOwnRequests() throws Exception {
        User me = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        User other = createUser("김현장", "engineer2@fixguide.dev", Role.ENGINEER);

        workRequestRepository.saveAll(List.of(
                Fixtures.workRequest(me, WorkRequestStatus.DRAFT),
                Fixtures.workRequest(me, WorkRequestStatus.PENDING),
                Fixtures.workRequest(other, WorkRequestStatus.PENDING)));

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("mine", "true")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].requesterName", Matchers.everyItem(Matchers.is("이엔지"))))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    @DisplayName("AC 6-1 · status 를 콤마로 여러 개 넘기면 해당 상태만 필터링된다")
    void filterByMultipleStatuses() throws Exception {
        User me = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        workRequestRepository.saveAll(List.of(
                Fixtures.workRequest(me, WorkRequestStatus.DRAFT),
                Fixtures.workRequest(me, WorkRequestStatus.REJECTED),
                Fixtures.workRequest(me, WorkRequestStatus.APPROVED)));

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("mine", "true")
                        .param("status", "REJECTED,DRAFT")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath(
                        "$.content[*].status",
                        Matchers.containsInAnyOrder("DRAFT", "REJECTED")));
    }

    @Test
    @DisplayName("지원하지 않는 status 값이면 400 VALIDATION_FAILED")
    void unknownStatusValue() throws Exception {
        User me = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("status", "NOT_A_STATUS")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(me)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC 2-4 · 상태별 nextAction 을 서버가 계산해 내려준다")
    void nextActionIsComputedByStatus() throws Exception {
        User me = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        WorkRequest draft = workRequestRepository.save(
                Fixtures.workRequest(me, WorkRequestStatus.DRAFT));

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("mine", "true")
                        .param("status", "DRAFT")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nextAction.label").value("이어서"))
                .andExpect(jsonPath("$.content[0].nextAction.path")
                        .value("/requests/" + draft.getId() + "/edit"))
                .andExpect(jsonPath("$.content[0].statusLabel").value("작성 중"))
                .andExpect(jsonPath("$.content[0].productTypeLabel").value("밸브"));
    }

    @Test
    @DisplayName("AC 7-2 · 안전관리자는 PENDING 이후 요청만 보고, DRAFT 는 보이지 않는다")
    void safetyManagerSeesSubmittedRequestsOnly() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        User safety = createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);

        WorkRequest pending = workRequestRepository.save(
                Fixtures.workRequest(engineer, WorkRequestStatus.PENDING));
        workRequestRepository.saveAll(List.of(
                Fixtures.workRequest(engineer, WorkRequestStatus.DRAFT),
                Fixtures.workRequest(engineer, WorkRequestStatus.AI_RUNNING),
                Fixtures.workRequest(engineer, WorkRequestStatus.APPROVED)));

        mockMvc.perform(get("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(safety)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath(
                        "$.content[*].status", Matchers.containsInAnyOrder("PENDING", "APPROVED")));

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(safety)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].nextAction.path")
                        .value("/manage/requests/" + pending.getId()));
    }

    @Test
    @DisplayName("안전관리자가 mine=true 로 조회하면 403 FORBIDDEN_ROLE")
    void safetyManagerCannotUseMineFilter() throws Exception {
        User safety = createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("mine", "true")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(safety)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    @DisplayName("AC 7-5 · 승인 대기 요청이 없으면 빈 목록을 반환한다")
    void emptyListWhenNothingPending() throws Exception {
        User safety = createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);

        mockMvc.perform(get("/api/v1/work-requests")
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(safety)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }
}
