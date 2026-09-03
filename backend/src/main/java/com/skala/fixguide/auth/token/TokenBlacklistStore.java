package com.skala.fixguide.auth.token;

import java.time.Duration;

/**
 * 로그아웃된 액세스 토큰의 jti 를 원래 만료 시점까지 보관한다.
 *
 * <p>JWT 는 stateless 라 서버가 토큰을 회수할 방법이 없다. 로그아웃을 즉시 반영하려면
 * "이 토큰은 더 이상 유효하지 않다"는 사실을 어딘가에 남기고 매 요청 확인해야 하는데,
 * 그 저장소를 추상화한 것이다.
 *
 * <p>운영은 {@link RedisTokenBlacklistStore}, 로컬·테스트는 {@link InMemoryTokenBlacklistStore}
 * 가 주입된다. 선택은 {@code app.auth.token-blacklist.type} 설정으로 한다.
 */
public interface TokenBlacklistStore {

    /**
     * 토큰을 무효 처리한다.
     *
     * @param tokenId JWT 의 jti 클레임
     * @param ttl 원래 만료까지 남은 시간. 이 시간이 지나면 어차피 토큰이 만료되므로 함께 폐기한다.
     */
    void blacklist(String tokenId, Duration ttl);

    boolean isBlacklisted(String tokenId);
}
