package com.skala.fixguide.workrequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.skala.fixguide.support.IntegrationTestSupport;
import com.skala.fixguide.support.TokenIssuer;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/** API 5 · 7 · 8 · 9 · 10 · 11 · 12 · 13 · 14 · 15 를 한 흐름으로 검증한다 (E_02 → E_03 → E_04 → S_02) */
class WorkRequestFlowTest extends IntegrationTestSupport {

    private static final Map<String, Object> FULL_REQUEST = Map.of(
            "equipment", "펌프 P-114",
            "line", "L3",
            "substance", "HF",
            "operatingCondition", Map.of("pressure", "2500 psi", "temperature", "80 ℃"),
            "productName", "SS-8-VCR",
            "productType", "VALVE",
            "specJson", Map.of("pressureRating", "3000 psi"),
            "symptom", "밸브 시트 누설");

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    @DisplayName("등록 → AI 실행 → 폴링 3회 → 결과 수정 → 제출 → 승인까지 상태가 명세대로 흐른다")
    void fullFlow() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        User manager = createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);
        String engineerToken = tokenIssuer.bearer(engineer);
        String managerToken = tokenIssuer.bearer(manager);

        // 5. 생성
        MvcResult created = mockMvc.perform(post("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FULL_REQUEST)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.requestNo", Matchers.startsWith("WR-")))
                .andReturn();
        String workRequestId = read(created).get("workRequestId").asText();

        // 11. AI 실행 → 202, step 3개 WAITING
        MvcResult started = mockMvc.perform(post("/api/v1/agent-runs")
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.steps", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.steps[*].status", Matchers.everyItem(Matchers.is("WAITING"))))
                .andReturn();
        String runId = read(started).get("runId").asText();

        // 같은 요청에 두 번 실행하면 409
        mockMvc.perform(post("/api/v1/agent-runs")
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RUN_ALREADY_IN_PROGRESS"));

        // 12. 폴링 — 호출마다 step 하나씩 DONE (Mock 전이)
        mockMvc.perform(get("/api/v1/agent-runs/{runId}", runId).header(HttpHeaders.AUTHORIZATION, engineerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allDone").value(false))
                .andExpect(jsonPath("$.steps[0].status").value("DONE"))
                .andExpect(jsonPath("$.steps[1].status").value("RUNNING"));
        mockMvc.perform(get("/api/v1/agent-runs/{runId}", runId).header(HttpHeaders.AUTHORIZATION, engineerToken))
                .andExpect(jsonPath("$.allDone").value(false));
        MvcResult polled = mockMvc.perform(get("/api/v1/agent-runs/{runId}", runId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allDone").value(true))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andReturn();
        String a2ResultId = read(polled).get("steps").get(1).get("agentResultId").asText();

        // 7. 상세 — AI_DONE, agentRun 결과 3건, 엔지니어는 editable
        mockMvc.perform(get("/api/v1/work-requests/{id}", workRequestId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AI_DONE"))
                .andExpect(jsonPath("$.agentRun.results", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.agentRun.results[0].editable").value(true))
                .andExpect(jsonPath("$.agentRun.results[2].documents", Matchers.hasSize(2)));

        // 13. A2 결과 수정 — 항목 하나 삭제 + 신규 추가 → edited
        mockMvc.perform(patch("/api/v1/agent-results/{id}", a2ResultId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"itemId\":\"i-01\",\"text\":\"수정된 법령\"},{\"text\":\"추가 조문\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edited").value(true))
                .andExpect(jsonPath("$.items", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.items[1].itemId").value("i-03"));

        // 14. 설명 없이 제출하면 422
        mockMvc.perform(patch("/api/v1/work-requests/{id}/submit-approval", workRequestId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SUBMIT_REQUIRED_FIELD_MISSING"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("engineerNote"));

        // 14. 설명 넣고 제출 → PENDING
        mockMvc.perform(patch("/api/v1/work-requests/{id}/submit-approval", workRequestId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"engineerNote\":\"동일 사양 정품으로 교체 요청\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 8. PENDING 에서는 수정 불가 409
        mockMvc.perform(patch("/api/v1/work-requests/{id}", workRequestId)
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptom\":\"변경\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMMUTABLE_STATUS"));

        // 15. 엔지니어가 결재하면 403
        mockMvc.perform(post("/api/v1/approvals")
                        .header(HttpHeaders.AUTHORIZATION, engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\",\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());

        // 15. 사유 10자 미만 거절은 400
        mockMvc.perform(post("/api/v1/approvals")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\",\"decision\":\"REJECT\",\"reason\":\"짧음\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REJECT_REASON_REQUIRED"));

        // 15. 승인 → APPROVED, 상세에서 approval 확인 (안전관리자 조회 시 editable=false)
        mockMvc.perform(post("/api/v1/approvals")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\",\"decision\":\"APPROVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultStatus").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("박안전"));

        mockMvc.perform(get("/api/v1/work-requests/{id}", workRequestId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approval.decision").value("APPROVE"))
                .andExpect(jsonPath("$.agentRun.results[0].editable").value(false));

        // 이미 처리된 요청은 409
        mockMvc.perform(post("/api/v1/approvals")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRequestId\":\"" + workRequestId + "\",\"decision\":\"APPROVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_DECIDED"));
    }

    @Test
    @DisplayName("AC 3-6 · draft=false 인데 필수값이 빠지면 400 과 fieldErrors, draft=true 면 통과")
    void requiredFieldsOnlyWhenNotDraft() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        String token = tokenIssuer.bearer(engineer);

        mockMvc.perform(post("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipment\":\"펌프\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", Matchers.hasItems("line", "productType", "specJson")));

        mockMvc.perform(post("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipment\":\"펌프\",\"draft\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("명세 2.3 · productType 별 specJson 필수 키가 없으면 400 SPEC_SCHEMA_MISMATCH")
    void specSchemaMismatch() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(post("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(engineer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withSpec(Map.of("material", "SUS316L")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SPEC_SCHEMA_MISMATCH"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("specJson.pressureRating"));
    }

    @Test
    @DisplayName("사진 업로드 · 목록 — 확장자 제한과 요청당 5장 제한")
    void photoUploadAndList() throws Exception {
        User engineer = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        String token = tokenIssuer.bearer(engineer);
        MvcResult created = mockMvc.perform(post("/api/v1/work-requests")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draft\":true}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = read(created).get("workRequestId").asText();

        mockMvc.perform(multipart("/api/v1/work-requests/{id}/photos", id)
                        .file(image("a.jpg"))
                        .file(image("b.png"))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.photos[0].originalUrl", Matchers.startsWith("/api/v1/files/" + id + "/")));

        mockMvc.perform(multipart("/api/v1/work-requests/{id}/photos", id)
                        .file(new MockMultipartFile("files", "doc.pdf", "application/pdf", new byte[] {1}))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));

        mockMvc.perform(multipart("/api/v1/work-requests/{id}/photos", id)
                        .file(image("c.jpg")).file(image("d.jpg")).file(image("e.jpg")).file(image("f.jpg"))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHOTO_LIMIT_EXCEEDED"));

        mockMvc.perform(get("/api/v1/work-requests/{id}/photos", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos", Matchers.hasSize(2)));
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile("files", name, MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
    }

    private static Map<String, Object> withSpec(Map<String, Object> specJson) {
        Map<String, Object> body = new java.util.HashMap<>(FULL_REQUEST);
        body.put("specJson", specJson);
        return body;
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
