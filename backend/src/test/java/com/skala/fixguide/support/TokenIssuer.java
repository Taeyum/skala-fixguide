package com.skala.fixguide.support;

import com.skala.fixguide.auth.jwt.JwtTokenProvider;
import com.skala.fixguide.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 테스트에서 로그인 API 를 거치지 않고 토큰이 필요할 때 사용한다. */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtTokenProvider tokenProvider;

    public String bearer(User user) {
        return "Bearer " + tokenProvider.createAccessToken(user);
    }
}
