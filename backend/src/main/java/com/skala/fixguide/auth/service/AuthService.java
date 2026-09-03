package com.skala.fixguide.auth.service;

import com.skala.fixguide.auth.dto.LoginRequest;
import com.skala.fixguide.auth.dto.LoginResponse;
import com.skala.fixguide.auth.dto.MeResponse;
import com.skala.fixguide.auth.dto.SignupRequest;
import com.skala.fixguide.auth.dto.SignupResponse;
import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.auth.jwt.JwtTokenProvider;
import com.skala.fixguide.auth.token.TokenBlacklistStore;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistStore blacklistStore;
    private final Clock clock;

    /**
     * 회원가입 (AC 1-4 · 1-5 · 1-3).
     *
     * <p>형식 검증은 {@link SignupRequest} 의 빈 검증이 맡고, 여기서는 그것만으로는 판단할 수 없는
     * 두 가지 — 비밀번호 확인 일치와 이메일 중복 — 만 본다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build());

        return SignupResponse.from(user);
    }

    /**
     * 로그인. 이메일이 없거나 비밀번호가 틀리면 어느 쪽인지 구분하지 않고 401 INVALID_CREDENTIALS
     * 로 응답한다(계정 존재 여부 노출 방지 · AC 0-3).
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = tokenProvider.createAccessToken(user);
        return LoginResponse.of(accessToken, tokenProvider.getExpiresInSeconds(), user);
    }

    /**
     * 로그아웃. 지금 쓰고 있는 토큰의 jti 를 원래 만료 시각까지 블랙리스트에 올린다.
     *
     * <p>같은 사용자의 다른 기기 토큰은 jti 가 달라 영향받지 않는다. 이미 블랙리스트에 있는 토큰으로는
     * 여기까지 도달할 수 없으므로(필터에서 401) 중복 호출 걱정은 없다.
     */
    public void logout(AuthenticatedUser me) {
        blacklistStore.blacklist(me.tokenId(), me.remainingValidity(clock.instant()));
    }

    /** 토큰 소유자 정보. 새로고침·직접 URL 진입 시 역할별 GNB 렌더링에 사용된다. */
    public MeResponse me(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.from(user);
    }
}
