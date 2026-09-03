package com.skala.fixguide.auth.token;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis TTL 기반 블랙리스트. 키가 스스로 만료되므로 별도 정리 작업이 필요 없다.
 *
 * <p>주의 — 이 구현을 켜면 인증이 필요한 모든 요청이 Redis 조회 1회를 거친다. stateless JWT 의
 * 이점을 일부 반납하는 대가로 즉시 로그아웃을 얻는 구조다.
 */
@RequiredArgsConstructor
public class RedisTokenBlacklistStore implements TokenBlacklistStore {

    private static final String KEY_PREFIX = "auth:blacklist:";
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String tokenId, Duration ttl) {
        if (tokenId == null || ttl.isZero() || ttl.isNegative()) {
            // 이미 만료된 토큰은 어차피 파싱 단계에서 걸린다. 저장하지 않는다.
            return;
        }
        redisTemplate.opsForValue().set(key(tokenId), MARKER, ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
