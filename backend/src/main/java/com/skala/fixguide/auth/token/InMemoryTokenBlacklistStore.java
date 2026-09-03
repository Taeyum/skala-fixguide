package com.skala.fixguide.auth.token;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * Redis 없이 단일 인스턴스에서 동작하는 폴백 구현. 로컬 실행과 테스트에서 사용한다.
 *
 * <p>인스턴스가 여러 개면 서로의 블랙리스트를 못 보고, 재시작하면 사라진다. 운영에서는
 * {@link RedisTokenBlacklistStore} 를 써야 한다.
 */
@RequiredArgsConstructor
public class InMemoryTokenBlacklistStore implements TokenBlacklistStore {

    private final Map<String, Instant> revokedUntil = new ConcurrentHashMap<>();
    private final Clock clock;

    @Override
    public void blacklist(String tokenId, Duration ttl) {
        if (tokenId == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        purgeExpired();
        revokedUntil.put(tokenId, clock.instant().plus(ttl));
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        Instant until = revokedUntil.get(tokenId);
        if (until == null) {
            return false;
        }
        if (!until.isAfter(clock.instant())) {
            revokedUntil.remove(tokenId);
            return false;
        }
        return true;
    }

    /** TTL 이 지난 항목은 조회 시점에도 지우지만, 쓰기 때 한 번 훑어 무한 증가를 막는다. */
    private void purgeExpired() {
        Instant now = clock.instant();
        revokedUntil.values().removeIf(until -> !until.isAfter(now));
    }
}
