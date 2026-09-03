package com.skala.argos.service;

import com.skala.argos.common.ApiException;
import com.skala.argos.domain.User;
import com.skala.argos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 임시 사용자 식별. JWT(auth 파트) 합류 전까지 X-User-Id 헤더로 사용자를 식별한다.
 * JWT 필터가 붙으면 이 지점만 토큰 클레임 조회로 교체하면 된다.
 */
@Component
@RequiredArgsConstructor
public class UserFinder {

    private final UserRepository userRepository;

    public User get(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID",
                    "X-User-Id 헤더가 필요합니다. (JWT 적용 전 임시 식별)");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID",
                        "존재하지 않는 사용자입니다."));
    }
}
