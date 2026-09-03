package com.skala.fixguide.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skala.fixguide.support.IntegrationTestSupport;
import com.skala.fixguide.support.TokenIssuer;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthControllerTest extends IntegrationTestSupport {

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    @DisplayName("AC 0-2 · 엔지니어 로그인 성공 시 토큰과 /home 리다이렉트 경로를 반환한다")
    void loginAsEngineer() throws Exception {
        createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "engineer@fixguide.dev", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.name").value("이엔지"))
                .andExpect(jsonPath("$.user.role").value("ENGINEER"))
                .andExpect(jsonPath("$.redirectPath").value("/home"));
    }

    @Test
    @DisplayName("AC 0-2 · 안전관리자는 /manage/requests 로 분기된다")
    void loginAsSafetyManager() throws Exception {
        createUser("박안전", "safety@fixguide.dev", Role.SAFETY_MANAGER);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "safety@fixguide.dev", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SAFETY_MANAGER"))
                .andExpect(jsonPath("$.redirectPath").value("/manage/requests"));
    }

    @Test
    @DisplayName("AC 0-3 · 비밀번호가 틀리면 401 INVALID_CREDENTIALS")
    void loginWithWrongPassword() throws Exception {
        createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "engineer@fixguide.dev", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    @DisplayName("AC 0-3 · 없는 계정도 계정 존재 여부를 노출하지 않고 401 로 응답한다")
    void loginWithUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@fixguide.dev", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400 VALIDATION_FAILED 와 fieldErrors 를 반환한다")
    void loginWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "not-an-email", "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("GET /auth/me · 유효한 토큰이면 사용자 정보를 반환한다")
    void meWithValidToken() throws Exception {
        User user = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, tokenIssuer.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("engineer@fixguide.dev"))
                .andExpect(jsonPath("$.role").value("ENGINEER"));
    }

    @Test
    @DisplayName("GET /auth/me · 토큰이 없으면 401 TOKEN_INVALID")
    void meWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("GET /auth/me · 위조된 토큰이면 401 TOKEN_INVALID")
    void meWithTamperedToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }
}
