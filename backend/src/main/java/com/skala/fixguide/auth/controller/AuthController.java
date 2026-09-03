package com.skala.fixguide.auth.controller;

import com.skala.fixguide.auth.dto.LoginRequest;
import com.skala.fixguide.auth.dto.LoginResponse;
import com.skala.fixguide.auth.dto.MeResponse;
import com.skala.fixguide.auth.dto.SignupRequest;
import com.skala.fixguide.auth.dto.SignupResponse;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 · 로그인 · 로그아웃 · 내 정보 (화면 WRA_C_00 · C_01)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "역할(ENGINEER · SAFETY_MANAGER) 선택이 필수다.")
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "성공 시 accessToken 과 역할별 redirectPath 를 반환한다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 액세스 토큰을 만료 시각까지 블랙리스트에 등록해 즉시 무효화한다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser me) {
        authService.logout(me);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 정보 조회", description = "새로고침·직접 URL 진입 시 역할 확인용.")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser me) {
        return ResponseEntity.ok(authService.me(me.userId()));
    }
}
