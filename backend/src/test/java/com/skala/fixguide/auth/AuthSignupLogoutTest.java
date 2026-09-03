package com.skala.fixguide.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skala.fixguide.support.IntegrationTestSupport;
import com.skala.fixguide.support.TokenIssuer;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** 회원가입(명세서 5.1)과 로그아웃(5.16) 통합 테스트 */
class AuthSignupLogoutTest extends IntegrationTestSupport {

    @Autowired
    private TokenIssuer tokenIssuer;

    private Map<String, Object> signupBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "홍길동");
        body.put("email", "hong@company.com");
        body.put("password", PASSWORD);
        body.put("passwordConfirm", PASSWORD);
        body.put("role", "ENGINEER");
        return body;
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ---------- 회원가입 ----------

    @Test
    @DisplayName("AC 1-4 · 회원가입에 성공하면 201 과 생성된 사용자 정보를 반환한다")
    void signupSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(signupBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@company.com"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("가입한 계정으로 곧바로 로그인할 수 있다 — 비밀번호가 해시로 저장된다")
    void signupThenLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(signupBody())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "hong@company.com", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.redirectPath").value("/home"));
    }

    @Test
    @DisplayName("AC 1-3 · 비밀번호와 확인이 다르면 400 PASSWORD_MISMATCH")
    void signupPasswordMismatch() throws Exception {
        Map<String, Object> body = signupBody();
        body.put("passwordConfirm", "Different!23");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("AC 1-5 · 이미 가입된 이메일이면 409 EMAIL_ALREADY_EXISTS")
    void signupDuplicateEmail() throws Exception {
        createUser("기존회원", "hong@company.com", Role.ENGINEER);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(signupBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("AC 1-2 · 역할을 고르지 않으면 400 VALIDATION_FAILED 와 필드 오류를 반환한다")
    void signupWithoutRole() throws Exception {
        Map<String, Object> body = signupBody();
        body.put("role", null);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='role')]").exists());
    }

    @Test
    @DisplayName("정의되지 않은 역할 값은 500 이 아니라 400 VALIDATION_FAILED 로 떨어진다")
    void signupWithUnknownRole() throws Exception {
        Map<String, Object> body = signupBody();
        body.put("role", "ADMIN");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC 1-2 · 비밀번호가 규칙(8자 이상 영문+숫자+특수문자)에 못 미치면 400")
    void signupWeakPassword() throws Exception {
        Map<String, Object> body = signupBody();
        body.put("password", "abcdefgh");
        body.put("passwordConfirm", "abcdefgh");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists());
    }

    // ---------- 로그아웃 ----------

    @Test
    @DisplayName("로그아웃하면 204 를 반환하고, 같은 토큰은 즉시 401 TOKEN_REVOKED 가 된다")
    void logoutRevokesTokenImmediately() throws Exception {
        User user = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        String bearer = tokenIssuer.bearer(user);

        // 로그아웃 전에는 정상 조회된다
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));
    }

    @Test
    @DisplayName("로그아웃은 그 토큰 하나만 무효화한다 — 다른 기기 토큰은 살아 있다")
    void logoutDoesNotAffectOtherTokens() throws Exception {
        User user = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        String phone = tokenIssuer.bearer(user);
        String desktop = tokenIssuer.bearer(user);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, phone))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, phone))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, desktop))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃 후 다시 로그인하면 새 토큰으로 정상 이용된다")
    void loginAgainAfterLogout() throws Exception {
        User user = createUser("이엔지", "engineer@fixguide.dev", Role.ENGINEER);
        String oldBearer = tokenIssuer.bearer(user);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, oldBearer))
                .andExpect(status().isNoContent());

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "engineer@fixguide.dev", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String newToken = objectMapper.readTree(response).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("engineer@fixguide.dev"));
    }

    @Test
    @DisplayName("토큰 없이 로그아웃하면 401")
    void logoutWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }
}
