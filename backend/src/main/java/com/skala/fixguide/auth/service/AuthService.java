package com.skala.fixguide.auth.service;

import com.skala.fixguide.auth.dto.LoginRequest;
import com.skala.fixguide.auth.dto.LoginResponse;
import com.skala.fixguide.auth.dto.MeResponse;
import com.skala.fixguide.auth.jwt.JwtTokenProvider;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
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

    /** 토큰 소유자 정보. 새로고침·직접 URL 진입 시 역할별 GNB 렌더링에 사용된다. */
    public MeResponse me(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.from(user);
    }
}
